package dev.podlink.system

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.hamondev.shevery.api.SheveryPrivilege
import dev.podlink.BuildConfig
import dev.podlink.core.PodUiState
import dev.podlink.core.ShizukuBridgeStatus
import dev.podlink.core.TraceLog
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShizukuReporter(context: Context) {
    private val appContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()

    private val _status = MutableStateFlow(ShizukuBridgeStatus.NOT_INSTALLED)
    val status: StateFlow<ShizukuBridgeStatus> = _status.asStateFlow()

    private val _lastSyncNote = MutableStateFlow<String?>(null)
    val lastSyncNote: StateFlow<String?> = _lastSyncNote.asStateFlow()

    @Volatile private var bridge: IShizukuBatteryBridge? = null
    @Volatile private var bound = false
    @Volatile private var lastSignature: String? = null
    @Volatile private var pendingState: PodUiState? = null

    private val userServiceArgs = SheveryPrivilege.UserServiceArgs(
        ComponentName(appContext.packageName, ShizukuBatteryService::class.java.name),
    )
        .daemon(false)
        .processNameSuffix("shevery_battery")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bridge = IShizukuBatteryBridge.Stub.asInterface(service)
            bound = bridge != null
            TraceLog.record(TAG, "Shevery UserService connected (bound=$bound)")
            pendingState?.let(::dispatchAsync)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bridge = null
            bound = false
            TraceLog.record(TAG, "Shevery UserService disconnected")
        }

        override fun onBindingDied(name: ComponentName?) {
            bridge = null
            bound = false
            TraceLog.record(TAG, "Shevery UserService binding died")
        }

        override fun onNullBinding(name: ComponentName?) {
            bridge = null
            bound = false
            TraceLog.record(TAG, "Shevery UserService null binding")
        }
    }

    private val receivedListener = SheveryPrivilege.OnBinderReceivedListener {
        refreshStatus()
        pendingState?.let(::sync)
    }

    private val deadListener = SheveryPrivilege.OnBinderDeadListener {
        bridge = null
        bound = false
        refreshStatus()
    }

    private val permissionListener = SheveryPrivilege.OnPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQ_SHEVERY) {
            refreshStatus()
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                pendingState?.let(::sync)
            }
        }
    }

    init {
        runAsync {
            runCatching {
                SheveryPrivilege.connectFast(appContext)
                SheveryPrivilege.addBinderReceivedListenerSticky(receivedListener)
                SheveryPrivilege.addBinderDeadListener(deadListener)
                SheveryPrivilege.addPermissionResultListener(permissionListener)
            }
            _status.value = computeStatus()
        }
    }

    fun refreshStatus() {
        runAsync {
            _status.value = computeStatus()
        }
    }

    fun requestPermission() {
        runAsync {
            val current = computeStatus()
            _status.value = current
            if (current == ShizukuBridgeStatus.PERMISSION_REQUIRED) {
                runCatching { SheveryPrivilege.requestPermission(REQ_SHEVERY) }
                    .onFailure { err -> TraceLog.record(TAG, "Shevery permission request failed: ${err.message}") }
            }
        }
    }

    fun sync(state: PodUiState) {
        if (!state.prefs.shizukuBatterySync || !state.isConnected || state.deviceAddress.isNullOrBlank()) {
            pendingState = null
            lastSignature = null
            runAsync { unbindIfNeeded() }
            return
        }
        pendingState = state
        runAsync {
            val currentStatus = computeStatus()
            _status.value = currentStatus
            if (currentStatus != ShizukuBridgeStatus.READY) return@runAsync
            if (!bound || bridge == null) {
                ensureBound()
                return@runAsync
            }
            pushOnExecutor(state)
        }
    }

    fun release() {
        runAsync { unbindIfNeeded() }
        runCatching {
            SheveryPrivilege.removeBinderReceivedListener(receivedListener)
            SheveryPrivilege.removeBinderDeadListener(deadListener)
            SheveryPrivilege.removePermissionResultListener(permissionListener)
        }
        runCatching { executor.shutdown() }
    }

    private fun runAsync(block: () -> Unit) {
        if (executor.isShutdown) return
        runCatching { executor.execute(block) }
    }

    private fun ensureBound() {
        if (bound) return
        bound = true
        runCatching {
            SheveryPrivilege.bindUserService(userServiceArgs, serviceConnection)
        }.onFailure { err ->
            bound = false
            _lastSyncNote.value = err.message
            TraceLog.record(TAG, "Shevery bindUserService failed: ${err.message}")
        }
    }

    private fun unbindIfNeeded() {
        if (!bound && bridge == null) return
        runCatching { SheveryPrivilege.unbindUserService(userServiceArgs, serviceConnection, true) }
        bridge = null
        bound = false
    }

    private fun dispatchAsync(state: PodUiState) {
        runAsync { pushOnExecutor(state) }
    }

    private fun pushOnExecutor(state: PodUiState) {
        val mac = state.deviceAddress ?: return
        val bat = state.battery
        if (!bat.hasAnyAvailable) return
        val left = bat.left?.takeIf { it.isAvailable }
        val right = bat.right?.takeIf { it.isAvailable }
        val case = bat.case?.takeIf { it.isAvailable }
        val hp = bat.headphone?.takeIf { it.isAvailable }
        val modelName = state.matchedModel?.name ?: state.displayName

        val key = "$mac|${left?.level}:${left?.isCharging}|${right?.level}:${right?.isCharging}|${case?.level}:${case?.isCharging}|${hp?.level}:${hp?.isCharging}|$modelName"
        if (key == lastSignature) return

        val pkg = appContext.packageName
        val model = state.matchedModel
        val mainUri = resUri(pkg, model?.caseRes ?: model?.budsRes)
        val leftUri = resUri(pkg, model?.leftRes)
        val rightUri = resUri(pkg, model?.rightRes)
        val caseUri = resUri(pkg, model?.caseRes)

        val activeBridge = bridge ?: return
        val result = try {
            activeBridge.pushBatteryState(
                mac,
                left?.level ?: -1,
                left?.isCharging == true,
                right?.level ?: -1,
                right?.isCharging == true,
                case?.level ?: -1,
                case?.isCharging == true,
                hp?.level ?: -1,
                hp?.isCharging == true,
                modelName,
                mainUri,
                leftUri,
                rightUri,
                caseUri,
                pkg,
            ).also {
                lastSignature = key
            }
        } catch (err: Exception) {
            if (err is android.os.RemoteException) {
                bridge = null
                bound = false
            }
            lastSignature = null
            "IPC error: ${err.message}"
        }
        _lastSyncNote.value = result
        TraceLog.record(TAG, "Push -> $result")
    }

    private fun resUri(pkg: String, resId: Int?): String = if (resId != null) {
        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://$pkg/$resId"
    } else {
        ""
    }

    private fun computeStatus(): ShizukuBridgeStatus {
        val installed = runCatching {
            appContext.packageManager.getPackageInfo(SHEVERY_PKG, 0)
            true
        }.getOrDefault(false)
        if (!installed) return ShizukuBridgeStatus.NOT_INSTALLED
        val alive = runCatching {
            if (!SheveryPrivilege.pingBinder()) {
                SheveryPrivilege.connectFast(appContext)
            }
            SheveryPrivilege.pingBinder()
        }.getOrDefault(false)
        if (!alive) return ShizukuBridgeStatus.NOT_RUNNING
        val granted = runCatching { SheveryPrivilege.isGranted() }.getOrDefault(false)
        return if (granted) ShizukuBridgeStatus.READY else ShizukuBridgeStatus.PERMISSION_REQUIRED
    }

    private companion object {
        const val TAG = "SheveryReporter"
        const val SHEVERY_PKG = "com.hamondev.shevery"
        const val REQ_SHEVERY = 4102
    }
}
