package dev.podlink.core

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.companion.AssociationInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import androidx.compose.runtime.Stable
import androidx.core.content.ContextCompat
import dev.podlink.bt.AacpLink
import dev.podlink.bt.AttLink
import dev.podlink.bt.CompanionBridge
import dev.podlink.bt.L2capChannel
import dev.podlink.system.BatteryNotifier
import dev.podlink.system.HeadMotionEngine
import dev.podlink.system.MediaBridge
import dev.podlink.system.ShizukuReporter
import dev.podlink.system.WidgetSync
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

object PodStateReducer {
    fun reduce(state: PodUiState, packet: AacpPacket): PodUiState = when (packet) {
        AacpPacket.HandshakeAck -> state
        is AacpPacket.BatteryInfo -> state.copy(battery = PodBattery.from(packet.metrics))
        is AacpPacket.EarDetection -> state.copy(wear = packet.wear)
        is AacpPacket.ConversationAwareness -> {
            val speaking = packet.status == 0x08.toByte() || packet.status == 0x09.toByte()
            state.copy(speakingDetected = speaking)
        }
        is AacpPacket.Information -> {
            val meta = packet.metadata
            state.copy(
                metadata = meta,
                deviceName = meta.name.ifBlank { state.deviceName },
                modelNumber = meta.modelNumber.ifBlank { state.modelNumber },
            )
        }
        is AacpPacket.ControlCommand -> reduceControl(state, packet)
        is AacpPacket.CustomEqUpdate -> state.copy(customEq = packet.curve, customEqReceived = true)
        is AacpPacket.Accommodation -> state.copy(
            accommodations = Accommodations(
                onMedia = packet.onMedia,
                onPhone = packet.onPhone,
                bands = packet.bands,
            ),
        )
        is AacpPacket.AudioSourceUpdate -> state.copy(audioSource = packet.source)
        is AacpPacket.ConnectedDevices -> state.copy(connectedPeers = packet.peers)
        is AacpPacket.ProximityKeys,
        is AacpPacket.StemEvent,
        is AacpPacket.SmartRouting,
        is AacpPacket.HeadTrackingSample,
        is AacpPacket.EqRaw,
        is AacpPacket.Unknown,
        -> state
    }

    private fun reduceControl(state: PodUiState, cmd: AacpPacket.ControlCommand): PodUiState {
        val v0 = cmd.value.getOrNull(0) ?: return state
        return when (cmd.id) {
            ControlId.MIC_MODE -> (v0.toInt() and 0xFF).let { mode ->
                if (mode in 0..2) state.copy(micMode = mode) else state
            }
            ControlId.EAR_DETECTION_CONFIG -> state.copy(earDetectionEnabled = v0 == 0x01.toByte())
            ControlId.LISTENING_MODE -> ListeningMode.fromWire(v0)?.let {
                state.copy(listeningMode = it, listeningModeKnown = true)
            } ?: state
            ControlId.LISTENING_MODE_CONFIGS -> state.copy(listeningModeMask = v0.toInt() and 0xFF)
            ControlId.ONE_BUD_ANC_MODE -> state.copy(singleBudAncEnabled = v0 == 0x01.toByte())
            ControlId.CHIME_VOLUME -> state.copy(chimeVolume = (v0.toInt() and 0xFF).coerceIn(0, 100))
            ControlId.AUTOMATIC_CONNECTION_CONFIG -> state.copy(automaticConnectionEnabled = v0 == 0x01.toByte())
            ControlId.DOUBLE_CLICK_INTERVAL -> state.copy(doubleClickInterval = (v0.toInt() and 0xFF).coerceIn(0, 2))
            ControlId.CLICK_HOLD_INTERVAL -> state.copy(clickHoldInterval = (v0.toInt() and 0xFF).coerceIn(0, 2))
            ControlId.VOLUME_SWIPE_INTERVAL -> state.copy(volumeSwipeInterval = (v0.toInt() and 0xFF).coerceIn(0, 2))
            ControlId.CALL_MANAGEMENT_CONFIG -> {
                val flipped = cmd.value.getOrNull(1) == 0x02.toByte() || v0 == 0x02.toByte()
                state.copy(callManagementFlipped = flipped)
            }
            ControlId.VOLUME_SWIPE_MODE -> state.copy(volumeSwipeEnabled = v0 == 0x01.toByte())
            ControlId.ADAPTIVE_VOLUME_CONFIG -> state.copy(personalizedVolumeEnabled = v0 == 0x01.toByte())
            ControlId.CONVERSATION_DETECT_CONFIG -> state.copy(conversationAwarenessEnabled = v0 == 0x01.toByte())
            ControlId.AUTO_ANC_STRENGTH -> {
                val raw = v0.toInt() and 0xFF
                state.copy(adaptiveAncStrength = (100 - raw).coerceIn(0, 100))
            }
            ControlId.SLEEP_DETECTION_CONFIG -> state.copy(sleepDetectionEnabled = v0 == 0x01.toByte())
            ControlId.DYNAMIC_END_OF_CHARGE -> state.copy(optimizedChargeLimitEnabled = v0 == 0x01.toByte())
            ControlId.PPE_TOGGLE_CONFIG -> state.copy(ppeEnabled = v0 == 0x01.toByte())
            ControlId.HEARING_AID -> state.copy(hearingAidEnrolled = v0 == 0x01.toByte())
            ControlId.HEARING_ASSIST_CONFIG -> state.copy(hearingAidEnabled = v0 == 0x01.toByte())
            ControlId.IN_CASE_TONE_CONFIG -> state.copy(inCaseToneEnabled = v0 == 0x01.toByte())
            ControlId.ALLOW_OFF_OPTION -> state.copy(allowOffOption = v0 == 0x01.toByte())
            else -> state
        }
    }
}

@Stable
class DeviceCoordinator(
    context: Context,
    private val prefs: PrefsStore,
    private val companion: CompanionBridge,
    private val media: MediaBridge,
    private val notifier: BatteryNotifier,
    private val shizuku: ShizukuReporter,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val systemExecutor = Executors.newSingleThreadExecutor()
    private val adapter: BluetoothAdapter? =
        appContext.getSystemService(BluetoothManager::class.java)?.adapter

    private val _state = MutableStateFlow(
        PodUiState(
            deviceName = prefs.state.value.cachedDeviceName,
            modelNumber = prefs.state.value.cachedModelNumber,
            listeningModeMask = prefs.state.value.cachedListeningModeMask,
            allowOffOption = prefs.state.value.cachedAllowOffOption,
            bluetoothGranted = hasBtPermission(),
            links = companion.state.value.links,
            prefs = prefs.state.value,
            shizukuStatus = shizuku.status.value,
            shizukuLastSyncNote = shizuku.lastSyncNote.value,
        ),
    )
    val state: StateFlow<PodUiState> = _state.asStateFlow()

    val companionState: StateFlow<CompanionSnapshot> = companion.state

    val motionEngine = HeadMotionEngine(
        scope = scope,
        startStream = { startHeadTracking() },
        stopStream = { stopHeadTracking() },
    )
    val motionState: StateFlow<MotionUiState> = motionEngine.motion
    val detectingGesture: StateFlow<Boolean> = motionEngine.detecting

    private var activeLink: AacpLink? = null
    private var activeAtt: AttLink? = null
    private var activeMac: String? = null
    private var lastSyncAt = 0L

    private var sliderJob: Job? = null
    private var accommJob: Job? = null
    private var eqJob: Job? = null
    private var attJob: Job? = null
    private var bleRetryJob: Job? = null
    private var caseCloseJob: Job? = null
    private var stemSyncPosted = false

    private var btReceiverRegistered = false
    private var a2dpConnectedForActive = false
    private var hfpConnectedForActive = false
    private var sawAnyAudioProfileForActive = false
    private var sawBothBudsInSession = false
    private var pendingBatteryOnClose: PodBattery? = null
    private var pendingWearOnClose: WearSnapshot? = null

    private val btStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
            val mac = device.address ?: return
            if (!activeMac.equals(mac, ignoreCase = true)) return
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    TraceLog.record(TAG, "ACL_DISCONNECTED for $mac")
                    onDeviceDisconnected(mac)
                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                    handleAudioProfileChanged(mac, isA2dp = true, profileState = state)
                }
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                    handleAudioProfileChanged(mac, isA2dp = false, profileState = state)
                }
            }
        }
    }

    init {
        if (hasBtPermission()) {
            companion.refreshAssociations()
            companion.ensureObservingAll()
        }
        scope.launch {
            prefs.state.collectLatest { userPrefs ->
                updateState {
                    copy(
                        prefs = userPrefs,
                        deviceName = deviceName.ifBlank { userPrefs.cachedDeviceName },
                        modelNumber = modelNumber.ifBlank { userPrefs.cachedModelNumber },
                        listeningModeMask = if (isConnected) listeningModeMask else userPrefs.cachedListeningModeMask,
                        allowOffOption = if (isConnected) allowOffOption else userPrefs.cachedAllowOffOption,
                    )
                }
            }
        }
        scope.launch {
            companion.state.collectLatest { snap ->
                updateState { copy(links = snap.links) }
            }
        }
        scope.launch {
            shizuku.status.collectLatest { status ->
                updateState { copy(shizukuStatus = status) }
            }
        }
        scope.launch {
            shizuku.lastSyncNote.collectLatest { note ->
                updateState { copy(shizukuLastSyncNote = note) }
            }
        }
    }

    private fun hasBtPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun ensureBtGranted(): Boolean {
        val granted = hasBtPermission()
        if (_state.value.bluetoothGranted != granted) {
            updateState { copy(bluetoothGranted = granted) }
        }
        return granted
    }

    fun onPermissionsUpdated(granted: Boolean) {
        val resolved = granted && hasBtPermission()
        val changed = _state.value.bluetoothGranted != resolved
        updateState { copy(bluetoothGranted = resolved) }
        if (resolved) {
            companion.refreshAssociations()
            companion.ensureObservingAll()
            if (changed || activeLink == null) {
                syncIfIdle(userInitiated = false)
            }
        } else if (activeLink != null || activeMac != null || _state.value.phase != ConnectionPhase.DISCONNECTED) {
            resetDisconnectedState()
        }
    }

    fun onPresenceConnected(device: BluetoothDevice?) {
        bleRetryJob?.cancel()
        if (!ensureBtGranted()) return
        if (device != null) {
            onDeviceConnected(device)
        } else {
            syncIfIdle(userInitiated = false)
        }
    }

    fun onBleAppeared() {
        if (!ensureBtGranted() || activeLink != null) return
        syncIfIdle(userInitiated = false)
        bleRetryJob?.cancel()
        bleRetryJob = scope.launch {
            val delays = longArrayOf(500L, 1200L, 2000L)
            for (waitMs in delays) {
                delay(waitMs)
                if (activeLink != null) break
                companion.findConnectedPods { device, _ ->
                    if (device != null && activeLink == null) {
                        onDeviceConnected(device)
                    }
                }
            }
        }
    }

    fun onBleDisappeared(mac: String) {
        if (!activeMac.equals(mac, ignoreCase = true)) return
        val snap = _state.value
        if (snap.earDetectionEnabled && !snap.wear.anyOutOfCase) {
            TraceLog.record(TAG, "BLE disappeared for $mac with buds in case -> closing session")
            resetDisconnectedState()
            companion.disconnectAudioProfiles(mac)
            return
        }
        companion.queryDeviceAudioStates(mac) { a2dpUp, hfpUp ->
            if (!activeMac.equals(mac, ignoreCase = true)) return@queryDeviceAudioStates
            if (a2dpUp || hfpUp) {
                TraceLog.record(TAG, "BLE disappeared for $mac while audio still connected -> keeping session")
            } else {
                onDeviceDisconnected(mac)
            }
        }
    }

    fun refreshNotifications() {
        val snap = _state.value
        runSystemSync { notifier.update(snap) }
    }

    fun refreshShizukuStatus() {
        shizuku.refreshStatus()
    }

    fun requestShizukuPermission() {
        shizuku.requestPermission()
    }

    fun syncIfIdle(userInitiated: Boolean) {
        val current = _state.value
        val link = activeLink
        if (link != null) {
            val now = System.currentTimeMillis()
            if (userInitiated || (current.isConnected && now - lastSyncAt > SYNC_THROTTLE_MS)) {
                lastSyncAt = now
                link.requestNotifications()
            }
            return
        }
        if (!ensureBtGranted()) {
            if (userInitiated) {
                updateState { copy(statusHint = "Grant Bluetooth permission to sync connected AirPods") }
            }
            return
        }
        val now = System.currentTimeMillis()
        if (!userInitiated && now - lastSyncAt < SYNC_THROTTLE_MS) return
        lastSyncAt = now

        TraceLog.record(TAG, "Checking connected audio profiles (manual=$userInitiated)")
        companion.findConnectedPods { device, reason ->
            if (device != null) {
                onDeviceConnected(device)
            } else {
                TraceLog.record(TAG, "Idle sync: ${reason ?: "no matching audio profile"}")
                if (userInitiated) {
                    updateState {
                        copy(
                            statusHint = reason ?: "No linked AirPods connected to audio profile",
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun onDeviceConnected(device: BluetoothDevice) {
        val mac = device.address ?: return
        val btAdapter = adapter ?: return
        if (!ensureBtGranted()) return
        bleRetryJob?.cancel()
        if (activeMac.equals(mac, ignoreCase = true) && activeLink != null) {
            TraceLog.record(TAG, "Already tracking $mac -> refreshing notifications")
            activeLink?.requestNotifications()
            return
        }

        closeLinks()
        L2capChannel.ensureExempted()
        activeMac = mac
        stemSyncPosted = false
        registerBtStateReceiver()
        companion.queryDeviceAudioStates(mac) { a2dpUp, hfpUp ->
            if (!activeMac.equals(mac, ignoreCase = true)) return@queryDeviceAudioStates
            a2dpConnectedForActive = a2dpConnectedForActive || a2dpUp
            hfpConnectedForActive = hfpConnectedForActive || hfpUp
            if (a2dpConnectedForActive || hfpConnectedForActive) {
                sawAnyAudioProfileForActive = true
            }
        }

        val btName = runCatching { device.name }.getOrNull().orEmpty()
        updateState {
            copy(
                phase = ConnectionPhase.CONNECTING,
                deviceAddress = mac,
                deviceName = btName.ifBlank { deviceName.ifBlank { prefs.cachedDeviceName } },
                errorMessage = null,
                statusHint = null,
                hiddenApiBlocked = false,
            )
        }

        val link = AacpLink(
            deviceAddress = mac,
            adapter = btAdapter,
            device = device,
            onConnected = {
                scope.launch {
                    updateState {
                        copy(
                            phase = ConnectionPhase.CONNECTED,
                            errorMessage = null,
                            statusHint = null,
                        )
                    }
                    syncStemConfigToFirmware()
                }
            },
            onPacket = { packet -> handlePacket(mac, packet) },
            onDisconnected = { error, hiddenBlocked ->
                scope.launch {
                    if (!activeMac.equals(mac, ignoreCase = true)) return@launch
                    resetDisconnectedState(
                        phase = if (error != null) ConnectionPhase.ERROR else ConnectionPhase.DISCONNECTED,
                        errorMessage = error?.message?.takeIf { it.isNotBlank() },
                        hiddenApiBlocked = hiddenBlocked,
                    )
                }
            },
        )
        activeLink = link
        link.start()

        val att = AttLink(
            adapter = btAdapter,
            device = device,
            onNotify = { handle, data -> handleAttData(handle, data) },
            onAvailability = { available ->
                scope.launch {
                    if (activeMac.equals(mac, ignoreCase = true)) {
                        updateState { copy(attAvailable = available) }
                    }
                }
            },
        )
        activeAtt = att
        att.start()
    }

    fun onDeviceDisconnected(mac: String) {
        if (!activeMac.equals(mac, ignoreCase = true)) return
        TraceLog.record(TAG, "Presence disconnected for $mac")
        resetDisconnectedState()
    }

    fun disconnectNow() {
        val mac = activeMac ?: _state.value.deviceAddress
        resetDisconnectedState()
        if (!mac.isNullOrBlank()) {
            companion.disconnectAudioProfiles(mac)
        }
    }

    fun setListeningMode(mode: ListeningMode): Boolean {
        val current = _state.value
        if (!current.supportsListeningMode) return false
        updateState { copy(listeningMode = mode, listeningModeKnown = true) }
        return sendPacket(AacpCodec.listeningMode(mode))
    }

    fun cycleListeningMode(): Boolean {
        val current = _state.value
        if (!current.supportsListeningMode) return false
        val next = ListeningMode.nextInCycle(current.listeningMode, current.listeningModeMask, current.allowOffOption)
        return setListeningMode(next)
    }

    fun toggleCycleModeBit(mode: ListeningMode) {
        val curMask = _state.value.listeningModeMask
        val toggled = curMask xor mode.cycleMask
        if (ListeningMode.entries.count { (toggled and it.cycleMask) != 0 } < 2) return
        updateState { copy(listeningModeMask = toggled) }
        prefs.cacheListeningModeMask(toggled)
        sendPacket(AacpCodec.control(ControlId.LISTENING_MODE_CONFIGS, toggled.toByte()))
    }

    fun setStemAction(press: StemPress, isLeft: Boolean, action: StemAction) {
        prefs.setStemAction(press, isLeft, action)
        syncStemConfigToFirmware()
    }

    fun setCallManagementFlipped(flipped: Boolean) {
        updateState { copy(callManagementFlipped = flipped) }
        sendPacket(AacpCodec.callManagement(flipped))
    }

    fun setMicMode(mode: Int) {
        val clamped = mode.coerceIn(0, 2)
        updateState { copy(micMode = clamped) }
        sendPacket(AacpCodec.control(ControlId.MIC_MODE, clamped.toByte()))
    }

    fun setCustomEq(curve: EqCurve) {
        updateState { copy(customEq = curve, customEqReceived = true) }
        eqJob?.cancel()
        eqJob = scope.launch {
            delay(80)
            sendPacket(AacpCodec.customEq(curve))
        }
    }

    fun setAdaptiveAncStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 100)
        updateState { copy(adaptiveAncStrength = clamped) }
        sliderJob?.cancel()
        sliderJob = scope.launch {
            delay(80)
            sendPacket(AacpCodec.adaptiveStrength(clamped))
        }
    }

    fun setPersonalizedVolume(enabled: Boolean) {
        updateState { copy(personalizedVolumeEnabled = enabled) }
        sendPacket(AacpCodec.control(ControlId.ADAPTIVE_VOLUME_CONFIG, enabled))
    }

    fun setConversationAwareness(enabled: Boolean) {
        updateState { copy(conversationAwarenessEnabled = enabled) }
        sendPacket(AacpCodec.control(ControlId.CONVERSATION_DETECT_CONFIG, enabled))
    }

    fun setLoudSoundReduction(enabled: Boolean) {
        updateState { copy(loudSoundReductionEnabled = enabled) }
        scope.launch(Dispatchers.IO) {
            activeAtt?.writeChar(AttHandle.LOUD_SOUND_REDUCTION, byteArrayOf(if (enabled) 0x01 else 0x00, 0x00))
        }
    }

    fun setEarDetection(enabled: Boolean) {
        updateState { copy(earDetectionEnabled = enabled) }
        sendPacket(AacpCodec.control(ControlId.EAR_DETECTION_CONFIG, enabled))
        if (!enabled) sendPacket(AacpCodec.requestNotifications(earDetection = false))
    }

    fun setAutoPauseMedia(enabled: Boolean) = prefs.setAutoPauseResumeOnEarDetection(enabled)

    fun setAutomaticConnection(enabled: Boolean) {
        updateState { copy(automaticConnectionEnabled = enabled) }
        sendPacket(AacpCodec.control(ControlId.AUTOMATIC_CONNECTION_CONFIG, enabled))
    }

    fun setHeadGesturesEnabled(enabled: Boolean) = prefs.setHeadGesturesEnabled(enabled)
    fun setUseAlternateHeadTrackingPackets(enabled: Boolean) = prefs.setUseAlternateHeadTrackingPackets(enabled)
    fun setDisconnectWhenNotWearing(enabled: Boolean) = prefs.setDisconnectWhenNotWearing(enabled)
    fun setShowPhoneBatteryInWidget(enabled: Boolean) = prefs.setShowPhoneBatteryInWidget(enabled)
    fun setQsClickBehaviorDialog(dialog: Boolean) = prefs.setQsClickBehaviorDialog(dialog)
    fun setShowBatteryNotification(enabled: Boolean) = prefs.setShowBatteryNotification(enabled)
    fun setNotificationStyle(style: NotificationStyle) = prefs.setNotificationStyle(style)
    fun setIncludeCaseInNotification(enabled: Boolean) = prefs.setIncludeCaseInNotification(enabled)
    fun setConversationAwarenessDuckVolume(enabled: Boolean) = prefs.setConversationAwarenessDuckVolume(enabled)
    fun setConversationAwarenessPauseMusic(enabled: Boolean) = prefs.setConversationAwarenessPauseMusic(enabled)
    fun setConversationAwarenessVolumePercent(percent: Int) = prefs.setConversationAwarenessVolumePercent(percent)
    fun setConversationAwarenessRelativeVolume(enabled: Boolean) = prefs.setConversationAwarenessRelativeVolume(enabled)
    fun setShizukuBatterySync(enabled: Boolean) = prefs.setShizukuBatterySync(enabled)

    fun setSleepDetection(enabled: Boolean) {
        updateState { copy(sleepDetectionEnabled = enabled) }
        sendPacket(AacpCodec.control(ControlId.SLEEP_DETECTION_CONFIG, enabled))
    }

    fun setOptimizedChargeLimit(enabled: Boolean) {
        updateState { copy(optimizedChargeLimitEnabled = enabled) }
        sendPacket(AacpCodec.control(ControlId.DYNAMIC_END_OF_CHARGE, enabled))
    }

    fun setAllowOffOption(allow: Boolean) {
        updateState { copy(allowOffOption = allow) }
        prefs.cacheAllowOffOption(allow)
        sendPacket(AacpCodec.control(ControlId.ALLOW_OFF_OPTION, allow))
    }

    fun setInCaseTone(enabled: Boolean) {
        updateState { copy(inCaseToneEnabled = enabled) }
        sendPacket(AacpCodec.control(ControlId.IN_CASE_TONE_CONFIG, enabled))
    }

    fun setSingleBudAnc(enabled: Boolean) {
        updateState { copy(singleBudAncEnabled = enabled) }
        sendPacket(AacpCodec.control(ControlId.ONE_BUD_ANC_MODE, enabled))
    }

    fun setVolumeSwipeEnabled(enabled: Boolean) {
        updateState { copy(volumeSwipeEnabled = enabled) }
        sendPacket(AacpCodec.control(ControlId.VOLUME_SWIPE_MODE, enabled))
    }

    fun setChimeVolume(volume: Int) {
        val clamped = volume.coerceIn(0, 100)
        updateState { copy(chimeVolume = clamped) }
        sliderJob?.cancel()
        sliderJob = scope.launch {
            delay(80)
            sendPacket(AacpCodec.chimeVolume(clamped))
        }
    }

    fun setDoubleClickInterval(interval: Int) {
        val v = interval.coerceIn(0, 2)
        updateState { copy(doubleClickInterval = v) }
        sendPacket(AacpCodec.control(ControlId.DOUBLE_CLICK_INTERVAL, v.toByte()))
    }

    fun setClickHoldInterval(interval: Int) {
        val v = interval.coerceIn(0, 2)
        updateState { copy(clickHoldInterval = v) }
        sendPacket(AacpCodec.control(ControlId.CLICK_HOLD_INTERVAL, v.toByte()))
    }

    fun setVolumeSwipeInterval(interval: Int) {
        val v = interval.coerceIn(0, 2)
        updateState { copy(volumeSwipeInterval = v) }
        sendPacket(AacpCodec.control(ControlId.VOLUME_SWIPE_INTERVAL, v.toByte()))
    }

    fun setAccommodations(accommodations: Accommodations) {
        updateState { copy(accommodations = accommodations) }
        accommJob?.cancel()
        accommJob = scope.launch {
            delay(80)
            sendPacket(
                AacpCodec.accommodation(
                    bands = accommodations.bands,
                    onPhone = accommodations.onPhone,
                    onMedia = accommodations.onMedia,
                ),
            )
        }
    }

    fun setPpeEnabled(enabled: Boolean) {
        updateState { copy(ppeEnabled = enabled) }
        sendPacket(AacpCodec.control(ControlId.PPE_TOGGLE_CONFIG, enabled))
    }

    fun setHearingAidEnrolled(enrolled: Boolean) {
        updateState { copy(hearingAidEnrolled = enrolled) }
        sendPacket(AacpCodec.control(ControlId.HEARING_AID, byteArrayOf(0x01, if (enrolled) 0x01 else 0x02)))
    }

    fun setHearingAidEnabled(enabled: Boolean) {
        updateState { copy(hearingAidEnabled = enabled) }
        AacpCodec.hearingAidFrames(enabled).forEach(::sendPacket)
    }

    fun updateTransparency(tuning: TransparencyTuning) {
        updateState { copy(transparency = tuning) }
        attJob?.cancel()
        attJob = scope.launch(Dispatchers.IO) {
            delay(80)
            activeAtt?.writeChar(AttHandle.TRANSPARENCY, tuning.toAttBytes())
        }
    }

    fun updateHearingAid(tuning: HearingAidTuning) {
        val bytes = tuning.toUpdatedAttBytes() ?: return
        updateState { copy(hearingAid = tuning) }
        attJob?.cancel()
        attJob = scope.launch(Dispatchers.IO) {
            delay(80)
            activeAtt?.writeChar(AttHandle.HEARING_AID, bytes)
        }
    }

    fun startHeadTracking(): Boolean {
        val sent = sendPacket(AacpCodec.startHeadTracking(_state.value.prefs.useAlternateHeadTrackingPackets))
        if (sent) updateState { copy(headTrackingActive = true) }
        return sent
    }

    fun stopHeadTracking(): Boolean {
        val sent = sendPacket(AacpCodec.stopHeadTracking(_state.value.prefs.useAlternateHeadTrackingPackets))
        updateState { copy(headTrackingActive = false) }
        return sent
    }

    fun resetHeadOrientation() = motionEngine.resetOrientation()

    fun startGestureTest() {
        updateState { copy(headGestureTesting = true, lastDetectedGesture = null) }
        motionEngine.startGestureTest(keepStreamingAfter = _state.value.headTrackingActive) { nodded ->
            updateState { copy(headGestureTesting = false, lastDetectedGesture = nodded) }
        }
    }

    fun stopGestureTest() {
        motionEngine.stopGestureTest(keepStreaming = _state.value.headTrackingActive)
        updateState { copy(headGestureTesting = false) }
    }

    fun renamePods(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (trimmed.toByteArray(Charsets.UTF_8).size > 255) {
            updateState { copy(statusHint = "Name must be at most 255 bytes") }
            return
        }
        updateState { copy(deviceName = trimmed) }
        prefs.cacheDeviceMetadata(
            name = trimmed,
            modelNumber = "",
            serialNumber = "",
            leftSerial = "",
            rightSerial = "",
            firmwareVersion = "",
        )
        sendPacket(AacpCodec.rename(trimmed))
    }

    fun resetToDefaults() {
        prefs.resetControlDefaults()
        val defaults = EqCurve()
        updateState {
            copy(
                customEq = defaults,
                micMode = 0,
                doubleClickInterval = 0,
                clickHoldInterval = 0,
                volumeSwipeInterval = 1,
                volumeSwipeEnabled = true,
                callManagementFlipped = false,
                singleBudAncEnabled = false,
                allowOffOption = false,
                listeningModeMask = AacpSpec.DEFAULT_MODE_MASK,
            )
        }
        prefs.cacheListeningModeMask(AacpSpec.DEFAULT_MODE_MASK)
        prefs.cacheAllowOffOption(false)
        if (_state.value.isConnected) {
            sendPacket(AacpCodec.stemConfig(single = false, doublePress = false, triple = false, longPress = false))
            sendPacket(AacpCodec.customEq(defaults))
            sendPacket(AacpCodec.control(ControlId.MIC_MODE, 0.toByte()))
            sendPacket(AacpCodec.control(ControlId.DOUBLE_CLICK_INTERVAL, 0.toByte()))
            sendPacket(AacpCodec.control(ControlId.CLICK_HOLD_INTERVAL, 0.toByte()))
            sendPacket(AacpCodec.control(ControlId.VOLUME_SWIPE_INTERVAL, 1.toByte()))
            sendPacket(AacpCodec.control(ControlId.VOLUME_SWIPE_MODE, true))
            sendPacket(AacpCodec.callManagement(flipped = false))
            sendPacket(AacpCodec.control(ControlId.ONE_BUD_ANC_MODE, false))
            sendPacket(AacpCodec.control(ControlId.ALLOW_OFF_OPTION, false))
            sendPacket(AacpCodec.control(ControlId.LISTENING_MODE_CONFIGS, AacpSpec.DEFAULT_MODE_MASK.toByte()))
        }
        updateState { copy(statusHint = "Settings reset to defaults") }
    }

    fun listUnassociatedBonded(): List<BondedCandidate> = companion.listUnassociatedBonded()

    fun associateMac(macAddress: String, onChooser: (IntentSender) -> Unit, onError: (String) -> Unit) {
        companion.associateMac(macAddress, onChooser, onError)
    }

    fun startDiscovery(showAll: Boolean, onChooser: (IntentSender) -> Unit, onError: (String) -> Unit) {
        companion.startDiscovery(showAll, onChooser, onError)
    }

    fun handleAssociationResult(info: AssociationInfo?) {
        companion.handleAssociationResult(info)
        syncIfIdle(userInitiated = false)
    }

    fun disassociate(id: Int) {
        val removedMac = _state.value.links.firstOrNull { it.id == id }?.macAddress
            ?: companion.resolveByAssociationId(id)?.address
        companion.disassociate(id)
        if (removedMac != null && activeMac.equals(removedMac, ignoreCase = true)) {
            resetDisconnectedState()
        }
    }

    fun disassociateAll() {
        companion.disassociateAll()
        if (activeMac != null || activeLink != null || activeAtt != null || _state.value.phase != ConnectionPhase.DISCONNECTED) {
            resetDisconnectedState()
        } else {
            closeLinks()
        }
    }

    fun clearStatusHint() = updateState { copy(statusHint = null) }

    fun release() {
        closeLinks()
        motionEngine.stopGestureTest(keepStreaming = false)
        scope.cancel()
        shizuku.release()
        runCatching { systemExecutor.shutdown() }
    }

    private fun handlePacket(sourceMac: String, packet: AacpPacket) {
        if (packet is AacpPacket.HeadTrackingSample) {
            motionEngine.onSample(packet.sample)
            return
        }
        scope.launch {
            if (!activeMac.equals(sourceMac, ignoreCase = true)) return@launch
            val prev = _state.value
            when (packet) {
                is AacpPacket.StemEvent -> handleStemEvent(packet)
                is AacpPacket.BatteryInfo -> {
                    val nextBattery = PodBattery.from(packet.metrics)
                    if (nextBattery.activeLayout == PodBattery.Layout.BOTH_BUDS) {
                        sawBothBudsInSession = true
                        cancelCaseCloseCheck()
                    } else if (shouldDeferOnCaseClose(prev, nextBattery, pendingWearOnClose ?: prev.wear)) {
                        pendingBatteryOnClose = nextBattery
                        scheduleCaseCloseCheck(sourceMac)
                        return@launch
                    }
                }
                is AacpPacket.EarDetection -> {
                    val userPrefs = prev.prefs
                    media.onWearChanged(prev.wear, packet.wear, userPrefs.autoPauseResumeOnEarDetection)
                    if (userPrefs.disconnectWhenNotWearing && prev.wear.anyInEar && !packet.wear.anyInEar) {
                        companion.disconnectAudioProfiles(sourceMac)
                    }
                    if (packet.wear.anyOutOfCase) {
                        flushPendingCloseState(packet)
                        return@launch
                    }
                    if (sawBothBudsInSession && prev.earDetectionEnabled &&
                        (packet.wear.primary == WearState.DISCONNECTED || packet.wear.secondary == WearState.DISCONNECTED)
                    ) {
                        pendingWearOnClose = packet.wear
                        scheduleCaseCloseCheck(sourceMac)
                        return@launch
                    }
                    if (pendingBatteryOnClose == null &&
                        prev.battery.activeLayout == PodBattery.Layout.BOTH_BUDS &&
                        packet.wear.primary != WearState.DISCONNECTED &&
                        packet.wear.secondary != WearState.DISCONNECTED
                    ) {
                        cancelCaseCloseCheck()
                    }
                }
                is AacpPacket.ConversationAwareness -> {
                    val speaking = packet.status == 0x08.toByte() || packet.status == 0x09.toByte()
                    if (speaking != prev.speakingDetected) {
                        val p = prev.prefs
                        media.onSpeakingChanged(
                            speaking = speaking,
                            duckVolume = p.conversationAwarenessDuckVolume,
                            pauseMusic = p.conversationAwarenessPauseMusic,
                            targetPercent = p.conversationAwarenessVolumePercent,
                            relative = p.conversationAwarenessRelativeVolume,
                        )
                    }
                }
                is AacpPacket.Information -> {
                    val m = packet.metadata
                    prefs.cacheDeviceMetadata(
                        name = m.name,
                        modelNumber = m.modelNumber,
                        serialNumber = m.serialNumber,
                        leftSerial = m.leftSerial,
                        rightSerial = m.rightSerial,
                        firmwareVersion = m.version1,
                    )
                }
                is AacpPacket.ControlCommand -> when (packet.id) {
                    ControlId.LISTENING_MODE_CONFIGS ->
                        packet.value.getOrNull(0)?.let { prefs.cacheListeningModeMask(it.toInt() and 0xFF) }
                    ControlId.ALLOW_OFF_OPTION ->
                        packet.value.getOrNull(0)?.let { prefs.cacheAllowOffOption(it == 0x01.toByte()) }
                    else -> Unit
                }
                else -> Unit
            }
            updateState { PodStateReducer.reduce(this, packet) }
        }
    }

    private fun handleStemEvent(event: AacpPacket.StemEvent) {
        val action = _state.value.prefs.stemMapping.actionFor(event.press, event.side)
        media.triggerAction(action) { cycleListeningMode() }
    }

    private fun handleAttData(handle: Byte, data: ByteArray) {
        scope.launch {
            when (handle) {
                AttHandle.TRANSPARENCY.code -> TransparencyTuning.parse(data)?.let { parsed ->
                    updateState { copy(transparency = parsed, attAvailable = true) }
                }
                AttHandle.LOUD_SOUND_REDUCTION.code -> {
                    val enabled = data.getOrNull(0) == 0x01.toByte()
                    updateState { copy(loudSoundReductionEnabled = enabled, attAvailable = true) }
                }
                AttHandle.HEARING_AID.code -> HearingAidTuning.parse(data)?.let { parsed ->
                    updateState { copy(hearingAid = parsed, attAvailable = true) }
                }
            }
        }
    }

    private fun syncStemConfigToFirmware() {
        val mapping = _state.value.prefs.stemMapping
        val single = mapping.isCustomized(StemPress.SINGLE_PRESS)
        val dbl = mapping.isCustomized(StemPress.DOUBLE_PRESS)
        val triple = mapping.isCustomized(StemPress.TRIPLE_PRESS)
        val long = mapping.isCustomized(StemPress.LONG_PRESS)
        if (single || dbl || triple || long || stemSyncPosted) {
            stemSyncPosted = single || dbl || triple || long
            sendPacket(AacpCodec.stemConfig(single, dbl, triple, long))
        }
    }

    private fun registerBtStateReceiver() {
        if (btReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        runCatching {
            ContextCompat.registerReceiver(appContext, btStateReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
            btReceiverRegistered = true
        }
    }

    private fun unregisterBtStateReceiver() {
        if (!btReceiverRegistered) return
        btReceiverRegistered = false
        runCatching { appContext.unregisterReceiver(btStateReceiver) }
    }

    private fun handleAudioProfileChanged(mac: String, isA2dp: Boolean, profileState: Int) {
        if (!activeMac.equals(mac, ignoreCase = true)) return
        val connected = profileState == BluetoothProfile.STATE_CONNECTED
        if (isA2dp) a2dpConnectedForActive = connected else hfpConnectedForActive = connected
        if (connected) {
            sawAnyAudioProfileForActive = true
            return
        }
        if (profileState == BluetoothProfile.STATE_DISCONNECTED &&
            sawAnyAudioProfileForActive &&
            !a2dpConnectedForActive &&
            !hfpConnectedForActive
        ) {
            TraceLog.record(TAG, "Audio profiles disconnected for $mac -> closing session")
            resetDisconnectedState()
        }
    }

    private fun shouldDeferOnCaseClose(
        prev: PodUiState,
        nextBattery: PodBattery,
        wear: WearSnapshot,
    ): Boolean = sawBothBudsInSession &&
        prev.earDetectionEnabled &&
        !wear.anyOutOfCase &&
        nextBattery.activeLayout != PodBattery.Layout.BOTH_BUDS

    private fun scheduleCaseCloseCheck(mac: String) {
        if (caseCloseJob?.isActive == true) return
        caseCloseJob = scope.launch {
            delay(CASE_CLOSE_CONFIRM_MS)
            if (!activeMac.equals(mac, ignoreCase = true)) return@launch
            val snap = _state.value
            if (snap.earDetectionEnabled && !snap.wear.anyOutOfCase) {
                TraceLog.record(TAG, "Case closed with buds inside ($mac) -> closing session")
                resetDisconnectedState()
                companion.disconnectAudioProfiles(mac)
            } else {
                flushPendingCloseState()
            }
        }
    }

    private fun flushPendingCloseState(packetToReduce: AacpPacket? = null) {
        val flushedBattery = pendingBatteryOnClose
        val flushedWear = pendingWearOnClose
        cancelCaseCloseCheck()
        updateState {
            var next = this
            if (flushedBattery != null) next = next.copy(battery = flushedBattery)
            if (flushedWear != null) next = next.copy(wear = flushedWear)
            if (packetToReduce != null) PodStateReducer.reduce(next, packetToReduce) else next
        }
    }

    private fun cancelCaseCloseCheck() {
        caseCloseJob?.cancel()
        caseCloseJob = null
        pendingBatteryOnClose = null
        pendingWearOnClose = null
    }

    private fun sendPacket(packet: ByteArray): Boolean = activeLink?.send(packet) == true

    private fun closeLinks() {
        unregisterBtStateReceiver()
        bleRetryJob?.cancel()
        bleRetryJob = null
        cancelCaseCloseCheck()
        sliderJob?.cancel()
        sliderJob = null
        accommJob?.cancel()
        accommJob = null
        eqJob?.cancel()
        eqJob = null
        attJob?.cancel()
        attJob = null
        activeMac = null
        a2dpConnectedForActive = false
        hfpConnectedForActive = false
        sawAnyAudioProfileForActive = false
        sawBothBudsInSession = false
        activeLink?.close()
        activeLink = null
        activeAtt?.close()
        activeAtt = null
    }

    private fun resetDisconnectedState(
        phase: ConnectionPhase = ConnectionPhase.DISCONNECTED,
        errorMessage: String? = null,
        statusHint: String? = null,
        hiddenApiBlocked: Boolean = false,
    ) {
        closeLinks()
        media.resetSession()
        motionEngine.stopGestureTest(keepStreaming = false)
        updateState {
            copy(
                phase = phase,
                battery = PodBattery(),
                wear = WearSnapshot(),
                listeningModeKnown = false,
                speakingDetected = false,
                attAvailable = false,
                transparency = null,
                hearingAid = null,
                connectedPeers = emptyList(),
                audioSource = null,
                headTrackingActive = false,
                headGestureTesting = false,
                errorMessage = errorMessage,
                statusHint = statusHint,
                hiddenApiBlocked = hiddenApiBlocked,
            )
        }
    }

    private fun updateState(transform: PodUiState.() -> PodUiState) {
        var prevSnapshot: PodUiState? = null
        val nextSnapshot = _state.updateAndGet { current ->
            prevSnapshot = current
            current.transform()
        }
        val prev = prevSnapshot ?: return
        if (prev == nextSnapshot) return

        val needNotifier = prev.phase != nextSnapshot.phase ||
            prev.battery != nextSnapshot.battery ||
            prev.listeningMode != nextSnapshot.listeningMode ||
            prev.supportsListeningMode != nextSnapshot.supportsListeningMode ||
            prev.displayName != nextSnapshot.displayName ||
            prev.matchedModel != nextSnapshot.matchedModel ||
            prev.prefs.showBatteryNotification != nextSnapshot.prefs.showBatteryNotification ||
            prev.prefs.notificationStyle != nextSnapshot.prefs.notificationStyle ||
            prev.prefs.includeCaseInNotification != nextSnapshot.prefs.includeCaseInNotification

        val needWidgets = prev.phase != nextSnapshot.phase ||
            prev.battery != nextSnapshot.battery ||
            prev.listeningMode != nextSnapshot.listeningMode ||
            prev.allowOffOption != nextSnapshot.allowOffOption ||
            prev.prefs.showPhoneBatteryInWidget != nextSnapshot.prefs.showPhoneBatteryInWidget

        if (needNotifier || needWidgets) {
            runSystemSync {
                if (needNotifier) notifier.update(nextSnapshot)
                if (needWidgets) WidgetSync.updateAll(appContext, nextSnapshot)
            }
        }

        if (prev.phase != nextSnapshot.phase ||
            prev.deviceAddress != nextSnapshot.deviceAddress ||
            prev.battery != nextSnapshot.battery ||
            prev.matchedModel != nextSnapshot.matchedModel ||
            prev.displayName != nextSnapshot.displayName ||
            prev.prefs.shizukuBatterySync != nextSnapshot.prefs.shizukuBatterySync ||
            prev.shizukuStatus != nextSnapshot.shizukuStatus
        ) {
            shizuku.sync(nextSnapshot)
        }
    }

    private fun runSystemSync(block: () -> Unit) {
        if (systemExecutor.isShutdown) return
        runCatching { systemExecutor.execute(block) }
    }

    private companion object {
        const val TAG = "DeviceCoordinator"
        const val SYNC_THROTTLE_MS = 2000L
        const val CASE_CLOSE_CONFIRM_MS = 650L
    }
}
