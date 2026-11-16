package dev.podlink.bt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.companion.CompanionDeviceService
import android.companion.DevicePresenceEvent
import android.companion.ObservingDevicePresenceRequest
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import dev.podlink.PodLinkApplication
import dev.podlink.core.AacpSpec
import dev.podlink.core.BondedCandidate
import dev.podlink.core.CompanionLinkSummary
import dev.podlink.core.CompanionSnapshot
import dev.podlink.core.TraceLog
import java.util.Locale
import java.util.regex.Pattern
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CompanionBridge(context: Context) {
    private val appContext = context.applicationContext
    private val cdm = appContext.getSystemService(CompanionDeviceManager::class.java)
    private val btManager = appContext.getSystemService(BluetoothManager::class.java)

    private val _state = MutableStateFlow(CompanionSnapshot())
    val state: StateFlow<CompanionSnapshot> = _state.asStateFlow()

    fun refreshAssociations(): List<CompanionLinkSummary> {
        val list = cdm?.myAssociations.orEmpty().mapNotNull { it.toSummary() }
        _state.value = _state.value.copy(links = list)
        return list
    }

    @SuppressLint("MissingPermission")
    fun listUnassociatedBonded(): List<BondedCandidate> {
        val adapter = btManager?.adapter ?: return emptyList()
        val linkedMacs = cdm?.myAssociations
            ?.mapNotNull { it.deviceMacAddress?.toString()?.uppercase(Locale.US) }
            ?.toSet()
            .orEmpty()
        val podUuid = ParcelUuid.fromString(AacpSpec.SERVICE_UUID)
        return runCatching {
            adapter.bondedDevices.orEmpty()
                .filter { it.address.uppercase(Locale.US) !in linkedMacs }
                .map { device ->
                    val major = device.bluetoothClass?.majorDeviceClass
                    val hasUuid = device.uuids?.contains(podUuid) == true
                    val isAudio = hasUuid ||
                        major == BluetoothClass.Device.Major.AUDIO_VIDEO ||
                        major == BluetoothClass.Device.Major.WEARABLE
                    BondedCandidate(
                        name = device.name?.takeIf { it.isNotBlank() } ?: device.address,
                        macAddress = device.address.uppercase(Locale.US),
                        hasPodUuid = hasUuid,
                        isAudioDevice = isAudio,
                    )
                }
                .sortedWith(
                    compareByDescending<BondedCandidate> { it.isLikelyPods }
                        .thenByDescending { it.isAudioDevice }
                        .thenBy { it.name.lowercase(Locale.US) },
                )
        }.getOrDefault(emptyList())
    }

    fun associateMac(macAddress: String, onChooser: (IntentSender) -> Unit, onError: (String) -> Unit) {
        val normalized = macAddress.uppercase(Locale.US)
        val req = AssociationRequest.Builder()
            .setSingleDevice(true)
            .addDeviceFilter(BluetoothDeviceFilter.Builder().setAddress(normalized).build())
            .build()
        launchAssociation(req, "bonded MAC ($normalized)", onChooser, onError)
    }

    fun startDiscovery(showAll: Boolean, onChooser: (IntentSender) -> Unit, onError: (String) -> Unit) {
        val filter = if (showAll) {
            BluetoothDeviceFilter.Builder().build()
        } else {
            BluetoothDeviceFilter.Builder().setNamePattern(Pattern.compile(".*AirPods.*", Pattern.CASE_INSENSITIVE)).build()
        }
        val req = AssociationRequest.Builder().setSingleDevice(false).addDeviceFilter(filter).build()
        launchAssociation(req, "discovery (all=$showAll)", onChooser, onError)
    }

    private fun launchAssociation(
        request: AssociationRequest,
        desc: String,
        onChooser: (IntentSender) -> Unit,
        onError: (String) -> Unit,
    ) {
        val manager = cdm ?: return onError("CompanionDeviceManager unavailable")
        TraceLog.record(TAG, "CDM associate $desc")
        manager.associate(
            request,
            appContext.mainExecutor,
            object : CompanionDeviceManager.Callback() {
                override fun onAssociationPending(intentSender: IntentSender) = onChooser(intentSender)
                override fun onAssociationCreated(info: AssociationInfo) {
                    observePresence(info)
                    refreshAssociations()
                }
                override fun onFailure(error: CharSequence?) {
                    val msg = error?.toString().orEmpty().ifBlank { "Association canceled" }
                    TraceLog.record(TAG, "CDM fail: $msg")
                    onError(msg)
                }
            },
        )
    }

    fun handleAssociationResult(info: AssociationInfo?) {
        if (info != null) observePresence(info) else ensureObservingAll()
        refreshAssociations()
    }

    fun ensureObservingAll(): Boolean {
        val all = cdm?.myAssociations.orEmpty()
        var observing = false
        for (info in all) if (observePresence(info)) observing = true
        refreshAssociations()
        _state.value = _state.value.copy(observingPresence = observing)
        return observing
    }

    fun disassociate(id: Int) {
        val manager = cdm ?: return
        val mac = manager.myAssociations.firstOrNull { it.id == id }?.deviceMacAddress?.toString()
        stopObservingPresence(manager, id, mac)
        runCatching { manager.disassociate(id) }
        refreshAssociations()
    }

    fun disassociateAll() {
        val manager = cdm ?: return
        for (info in manager.myAssociations) {
            stopObservingPresence(manager, info.id, info.deviceMacAddress?.toString())
            runCatching { manager.disassociate(info.id) }
        }
        _state.value = CompanionSnapshot()
    }

    @SuppressLint("MissingPermission")
    fun disconnectAudioProfiles(mac: String) {
        val adapter = btManager?.adapter ?: return
        val device = findBondedByMac(mac) ?: return
        for (profileType in listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET)) {
            runCatching {
                adapter.getProfileProxy(
                    appContext,
                    object : BluetoothProfile.ServiceListener {
                        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                            runCatching {
                                proxy.javaClass.getMethod("disconnect", BluetoothDevice::class.java).invoke(proxy, device)
                            }
                            adapter.closeProfileProxy(profile, proxy)
                        }
                        override fun onServiceDisconnected(profile: Int) = Unit
                    },
                    profileType,
                )
            }
        }
    }

    fun resolveByAssociationId(id: Int): BluetoothDevice? {
        val mac = cdm?.myAssociations?.firstOrNull { it.id == id }?.deviceMacAddress?.toString() ?: return null
        return findBondedByMac(mac)
    }

    @SuppressLint("MissingPermission")
    fun findBondedByMac(mac: String): BluetoothDevice? {
        val adapter = btManager?.adapter ?: return null
        val target = mac.uppercase(Locale.US)
        return runCatching {
            adapter.bondedDevices?.firstOrNull { it.address.equals(target, ignoreCase = true) }
                ?: adapter.getRemoteDevice(target)
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    fun queryDeviceAudioStates(mac: String, onResult: (a2dpConnected: Boolean, hfpConnected: Boolean) -> Unit) {
        val adapter = btManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            onResult(false, false)
            return
        }
        val a2dpUp = adapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothAdapter.STATE_CONNECTED
        val hfpUp = adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothAdapter.STATE_CONNECTED
        if (!a2dpUp && !hfpUp) {
            onResult(false, false)
            return
        }
        val target = mac.uppercase(Locale.US)
        isDeviceConnectedOnProfile(adapter, BluetoothProfile.A2DP, a2dpUp, target) { a2dpHit ->
            isDeviceConnectedOnProfile(adapter, BluetoothProfile.HEADSET, hfpUp, target) { hfpHit ->
                onResult(a2dpHit, hfpHit)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun isDeviceConnectedOnProfile(
        adapter: BluetoothAdapter,
        profile: Int,
        enabled: Boolean,
        targetMac: String,
        onResult: (Boolean) -> Unit,
    ) {
        if (!enabled) {
            onResult(false)
            return
        }
        val requested = runCatching {
            adapter.getProfileProxy(
                appContext,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(p: Int, proxy: BluetoothProfile) {
                        val hit = runCatching {
                            proxy.connectedDevices.any { it.address.equals(targetMac, ignoreCase = true) }
                        }.getOrDefault(false)
                        adapter.closeProfileProxy(p, proxy)
                        onResult(hit)
                    }
                    override fun onServiceDisconnected(p: Int) = Unit
                },
                profile,
            )
        }.getOrDefault(false)
        if (!requested) onResult(false)
    }

    @SuppressLint("MissingPermission")
    fun findConnectedPods(onResult: (BluetoothDevice?, String?) -> Unit) {
        val adapter = btManager?.adapter ?: return onResult(null, "Bluetooth adapter unavailable")
        if (!adapter.isEnabled) return onResult(null, "Bluetooth is turned off")
        val a2dp = adapter.getProfileConnectionState(BluetoothProfile.A2DP)
        val hfp = adapter.getProfileConnectionState(BluetoothProfile.HEADSET)
        if (a2dp != BluetoothProfile.STATE_CONNECTED && hfp != BluetoothProfile.STATE_CONNECTED) {
            return onResult(null, "No Bluetooth audio connected (A2DP=$a2dp, HFP=$hfp)")
        }

        val linked = cdm?.myAssociations?.mapNotNull { it.deviceMacAddress?.toString()?.uppercase(Locale.US) }?.toSet().orEmpty()
        val podUuid = ParcelUuid.fromString(AacpSpec.SERVICE_UUID)
        val candidates = runCatching {
            adapter.bondedDevices.orEmpty()
                .filter {
                    it.address.uppercase(Locale.US) in linked ||
                        it.uuids?.contains(podUuid) == true ||
                        it.name?.contains("AirPods", ignoreCase = true) == true
                }
                .map { it.address.uppercase(Locale.US) }
                .toSet()
        }.getOrDefault(emptySet())
        if (candidates.isEmpty()) {
            return onResult(null, "Connected audio did not match a linked/AirPods device")
        }

        val profiles = buildList {
            if (a2dp == BluetoothProfile.STATE_CONNECTED) add(BluetoothProfile.A2DP)
            if (hfp == BluetoothProfile.STATE_CONNECTED) add(BluetoothProfile.HEADSET)
        }
        queryProfiles(adapter, profiles, 0, candidates, mutableListOf(), onResult)
    }

    @SuppressLint("MissingPermission")
    private fun queryProfiles(
        adapter: BluetoothAdapter,
        profiles: List<Int>,
        index: Int,
        candidates: Set<String>,
        seenNames: MutableList<String>,
        onResult: (BluetoothDevice?, String?) -> Unit,
    ) {
        if (index >= profiles.size) {
            onResult(null, "Connected devices (${seenNames.distinct().joinToString().ifBlank { "none" }}) did not match")
            return
        }
        val accepted = adapter.getProfileProxy(
            appContext,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    val devs = runCatching { proxy.connectedDevices }.getOrDefault(emptyList())
                    devs.forEach { d -> seenNames += runCatching { d.name }.getOrNull().orEmpty().ifBlank { d.address } }
                    val match = devs.firstOrNull { it.address.uppercase(Locale.US) in candidates }
                    adapter.closeProfileProxy(profile, proxy)
                    if (match != null) onResult(match, null)
                    else queryProfiles(adapter, profiles, index + 1, candidates, seenNames, onResult)
                }
                override fun onServiceDisconnected(profile: Int) = Unit
            },
            profiles[index],
        )
        if (!accepted) queryProfiles(adapter, profiles, index + 1, candidates, seenNames, onResult)
    }

    private fun observePresence(info: AssociationInfo): Boolean {
        val manager = cdm ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= 36) {
                val req = ObservingDevicePresenceRequest.Builder().setAssociationId(info.id).build()
                manager.startObservingDevicePresence(req)
            } else {
                val mac = info.deviceMacAddress?.toString() ?: return false
                @Suppress("DEPRECATION")
                manager.startObservingDevicePresence(mac)
            }
            true
        }.onFailure { err -> TraceLog.record(TAG, "Observe fail id=${info.id}: ${err.message}") }.getOrDefault(false)
    }

    private fun stopObservingPresence(manager: CompanionDeviceManager, id: Int, mac: String?) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 36) {
                manager.stopObservingDevicePresence(ObservingDevicePresenceRequest.Builder().setAssociationId(id).build())
            } else if (!mac.isNullOrBlank()) {
                @Suppress("DEPRECATION")
                manager.stopObservingDevicePresence(mac)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun AssociationInfo.toSummary(): CompanionLinkSummary? {
        val mac = deviceMacAddress?.toString()?.uppercase(Locale.US) ?: return null
        val bondedName = findBondedByMac(mac)?.let { runCatching { it.name }.getOrNull() }
        val name = displayName?.toString()?.takeIf { it.isNotBlank() } ?: bondedName ?: mac
        return CompanionLinkSummary(id, mac, name)
    }

    private companion object {
        const val TAG = "CompanionBridge"
    }
}

class PodPresenceService : CompanionDeviceService() {
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        if (Build.VERSION.SDK_INT >= 36) return
        val app = application as? PodLinkApplication ?: return
        val mac = associationInfo.deviceMacAddress?.toString()
        val device = app.companion.resolveByAssociationId(associationInfo.id)
            ?: mac?.let { app.companion.findBondedByMac(it) }
        TraceLog.record(TAG, "DEVICE_APPEARED assoc=${associationInfo.id} mac=$mac")
        app.coordinator.onPresenceConnected(device)
        if (device == null) app.coordinator.onBleAppeared()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        if (Build.VERSION.SDK_INT >= 36) return
        val app = application as? PodLinkApplication ?: return
        val mac = associationInfo.deviceMacAddress?.toString()
            ?: app.companion.resolveByAssociationId(associationInfo.id)?.address
            ?: return
        TraceLog.record(TAG, "DEVICE_DISAPPEARED assoc=${associationInfo.id} mac=$mac")
        app.coordinator.onBleDisappeared(mac)
    }

    @SuppressLint("MissingPermission")
    override fun onDevicePresenceEvent(event: DevicePresenceEvent) {
        if (Build.VERSION.SDK_INT < 36) return
        val app = application as? PodLinkApplication ?: return
        val device = app.companion.resolveByAssociationId(event.associationId)
        val mac = device?.address
        when (event.event) {
            DevicePresenceEvent.EVENT_BT_CONNECTED -> {
                TraceLog.record(TAG, "BT_CONNECTED assoc=${event.associationId} mac=$mac")
                app.coordinator.onPresenceConnected(device)
            }
            DevicePresenceEvent.EVENT_BLE_APPEARED -> {
                TraceLog.record(TAG, "BLE_APPEARED assoc=${event.associationId} mac=$mac")
                app.coordinator.onBleAppeared()
            }
            DevicePresenceEvent.EVENT_BLE_DISAPPEARED -> {
                TraceLog.record(TAG, "BLE_DISAPPEARED assoc=${event.associationId} mac=$mac")
                if (mac != null) app.coordinator.onBleDisappeared(mac)
            }
            DevicePresenceEvent.EVENT_BT_DISCONNECTED -> {
                TraceLog.record(TAG, "BT_DISCONNECTED assoc=${event.associationId} mac=$mac")
                if (mac != null) app.coordinator.onDeviceDisconnected(mac)
            }
            else -> Log.d(TAG, "Presence event=${event.event}")
        }
    }

    private companion object {
        const val TAG = "PodPresenceService"
    }
}
