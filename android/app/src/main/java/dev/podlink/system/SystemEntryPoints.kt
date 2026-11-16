package dev.podlink.system

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import dev.podlink.MainActivity
import dev.podlink.PodLinkApplication
import dev.podlink.QuickPanelActivity
import dev.podlink.R
import dev.podlink.core.BatteryMetric
import dev.podlink.core.ListeningMode
import dev.podlink.core.PodUiState
import dev.podlink.core.TraceLog
import dev.podlink.core.labelRes
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PodSessionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        queuedStart.set(false)
        val app = application as? PodLinkApplication
        val notification = app?.notifier?.currentOrFallbackNotification()
        if (notification != null) {
            runCatching {
                startForeground(
                    BatteryNotifier.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                )
                running.set(true)
                if (!app.coordinator.state.value.isConnected) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    running.set(false)
                    stopSelf()
                }
            }.onFailure { err ->
                TraceLog.record("SessionService", "startForeground skipped: ${err.message}")
                running.set(false)
                stopSelf()
            }
        } else {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running.set(false)
        queuedStart.set(false)
        super.onDestroy()
    }

    companion object {
        private val running = AtomicBoolean(false)
        private val queuedStart = AtomicBoolean(false)

        fun ensureStarted(context: Context) {
            if (running.get() || !queuedStart.compareAndSet(false, true)) return
            runCatching {
                ContextCompat.startForegroundService(
                    context.applicationContext,
                    Intent(context.applicationContext, PodSessionService::class.java),
                )
            }.onFailure { queuedStart.set(false) }
        }

        fun stop(context: Context) {
            val wasActive = running.getAndSet(false)
            val wasQueued = queuedStart.getAndSet(false)
            if (!wasActive && !wasQueued) return
            runCatching {
                context.applicationContext.stopService(
                    Intent(context.applicationContext, PodSessionService::class.java),
                )
            }
        }
    }
}

class SystemWakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext as? PodLinkApplication ?: return
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            TraceLog.record("WakeReceiver", "Restoring CDM observation ($action)")
            app.coordinator.onPermissionsUpdated(true)
        }
    }
}

class QuickSettingsTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collectJob: Job? = null
    private var lastSignature: String? = null

    override fun onStartListening() {
        super.onStartListening()
        lastSignature = null
        val app = application as? PodLinkApplication ?: return
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            app.coordinator.onPermissionsUpdated(true)
        }
        render(app.coordinator.state.value)
        collectJob?.cancel()
        collectJob = scope.launch { app.coordinator.state.collect(::render) }
    }

    override fun onStopListening() {
        collectJob?.cancel()
        collectJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val app = application as? PodLinkApplication ?: return
        val state = app.coordinator.state.value
        if (!state.isConnected) {
            TraceLog.record("QsTile", "Clicked disconnected -> syncing")
            app.coordinator.syncIfIdle(userInitiated = true)
            return
        }
        if (state.prefs.qsClickBehaviorDialog || !state.supportsListeningMode) {
            val intent = Intent(this, QuickPanelActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            TileServiceCompat.startActivityAndCollapse(
                this,
                PendingIntentActivityWrapper(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT,
                    false,
                ),
            )
        } else {
            val ok = app.coordinator.cycleListeningMode()
            TraceLog.record("QsTile", "Cycled listening mode (sent=$ok)")
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun render(state: PodUiState) {
        val tile = qsTile ?: return
        val newState: Int
        val label: String
        val subtitle: String
        if (!state.isConnected) {
            newState = Tile.STATE_UNAVAILABLE
            label = getString(R.string.listening_mode)
            subtitle = getString(R.string.airpods_not_connected)
        } else {
            val compact = state.battery.formatCompact(includeCase = false) ?: state.battery.formatCompact(includeCase = true)
            if (!state.supportsListeningMode) {
                newState = Tile.STATE_ACTIVE
                label = state.displayName
                subtitle = compact ?: getString(R.string.remake_status_connected)
            } else {
                val modeName = getString(state.listeningMode.labelRes)
                newState = if (state.listeningMode == ListeningMode.OFF) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
                label = state.displayName
                subtitle = if (compact != null) "$modeName · $compact" else modeName
            }
        }
        val sig = "$newState|$label|$subtitle"
        if (sig == lastSignature) return
        lastSignature = sig
        tile.state = newState
        tile.label = label
        tile.subtitle = subtitle
        tile.updateTile()
    }
}

class BatteryAppWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val coord = (context.applicationContext as? PodLinkApplication)?.coordinator ?: return
        WidgetSync.updateBatteryWidgets(context, coord.state.value)
    }
}

class ListeningModeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val coord = (context.applicationContext as? PodLinkApplication)?.coordinator ?: return
        WidgetSync.updateModeWidgets(context, coord.state.value)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetSync.ACTION_SET_LISTENING_MODE) {
            val wire = intent.getIntExtra(WidgetSync.EXTRA_LISTENING_MODE, ListeningMode.OFF.wireValue.toInt()).toByte()
            val mode = ListeningMode.fromWire(wire) ?: return
            (context.applicationContext as? PodLinkApplication)?.coordinator?.setListeningMode(mode)
        }
    }
}

object WidgetSync {
    const val ACTION_SET_LISTENING_MODE = "dev.podlink.action.SET_LISTENING_MODE"
    const val EXTRA_LISTENING_MODE = "listening_mode"

    @Volatile private var openAppPi: PendingIntent? = null
    private val modeIntents = HashMap<ListeningMode, PendingIntent>(4)

    fun updateAll(context: Context, state: PodUiState) {
        updateBatteryWidgets(context, state)
        updateModeWidgets(context, state)
    }

    fun updateBatteryWidgets(context: Context, state: PodUiState) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(context, BatteryAppWidget::class.java))
        if (ids.isEmpty()) return

        val appCtx = context.applicationContext
        val launchPi = openAppPi ?: PendingIntent.getActivity(
            appCtx,
            0,
            Intent(appCtx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ).also { openAppPi = it }

        val views = RemoteViews(context.packageName, R.layout.battery_widget).apply {
            setOnClickPendingIntent(android.R.id.background, launchPi)
            bindPod(R.id.left_battery_widget, R.id.left_battery_progress, R.id.left_charging_icon, state.battery.left ?: state.battery.headphone)
            bindPod(R.id.right_battery_widget, R.id.right_battery_progress, R.id.right_charging_icon, state.battery.right)
            bindPod(R.id.case_battery_widget, R.id.case_battery_progress, R.id.case_charging_icon, state.battery.case)

            val showPhone = state.prefs.showPhoneBatteryInWidget
            setViewVisibility(R.id.phone_battery_widget_container, if (showPhone) View.VISIBLE else View.GONE)
            if (showPhone) {
                val bm = context.getSystemService(BatteryManager::class.java)
                val lvl = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 0
                val chg = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
                setTextViewText(R.id.phone_battery_widget, "$lvl%")
                setProgressBar(R.id.phone_battery_progress, 100, lvl, false)
                setViewVisibility(R.id.phone_charging_icon, if (chg) View.VISIBLE else View.GONE)
            }
        }
        manager.updateAppWidget(ids, views)
    }

    fun updateModeWidgets(context: Context, state: PodUiState) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(context, ListeningModeWidget::class.java))
        if (ids.isEmpty()) return

        val mode = state.listeningMode
        val allowOff = state.allowOffOption
        val views = RemoteViews(context.packageName, R.layout.listening_mode_widget).apply {
            setOnClickPendingIntent(R.id.widget_off_button, modeIntent(context, ListeningMode.OFF))
            setOnClickPendingIntent(R.id.widget_transparency_button, modeIntent(context, ListeningMode.TRANSPARENCY))
            setOnClickPendingIntent(R.id.widget_adaptive_button, modeIntent(context, ListeningMode.ADAPTIVE))
            setOnClickPendingIntent(R.id.widget_anc_button, modeIntent(context, ListeningMode.NOISE_CANCELLATION))

            setInt(
                R.id.widget_off_button,
                "setBackgroundResource",
                if (mode == ListeningMode.OFF) R.drawable.widget_button_checked_shape_start else R.drawable.widget_button_shape_start,
            )
            setInt(
                R.id.widget_transparency_button,
                "setBackgroundResource",
                if (mode == ListeningMode.TRANSPARENCY) {
                    if (allowOff) R.drawable.widget_button_checked_shape_middle else R.drawable.widget_button_checked_shape_start
                } else {
                    if (allowOff) R.drawable.widget_button_shape_middle else R.drawable.widget_button_shape_start
                },
            )
            setInt(
                R.id.widget_adaptive_button,
                "setBackgroundResource",
                if (mode == ListeningMode.ADAPTIVE) R.drawable.widget_button_checked_shape_middle else R.drawable.widget_button_shape_middle,
            )
            setInt(
                R.id.widget_anc_button,
                "setBackgroundResource",
                if (mode == ListeningMode.NOISE_CANCELLATION) R.drawable.widget_button_checked_shape_end else R.drawable.widget_button_shape_end,
            )
            setViewVisibility(R.id.widget_off_button, if (allowOff) View.VISIBLE else View.GONE)
            setViewLayoutMargin(
                R.id.widget_transparency_button,
                RemoteViews.MARGIN_START,
                if (allowOff) 2f else 12f,
                TypedValue.COMPLEX_UNIT_DIP,
            )
        }
        manager.updateAppWidget(ids, views)
    }

    private fun RemoteViews.bindPod(textId: Int, progressId: Int, iconId: Int, metric: BatteryMetric?) {
        val avail = metric?.takeIf { it.isAvailable }
        setTextViewText(textId, avail?.let { "${it.level}%" } ?: "--")
        setProgressBar(progressId, 100, avail?.level ?: 0, false)
        setViewVisibility(iconId, if (avail?.isCharging == true) View.VISIBLE else View.GONE)
    }

    private fun modeIntent(context: Context, mode: ListeningMode): PendingIntent = synchronized(modeIntents) {
        modeIntents.getOrPut(mode) {
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, ListeningModeWidget::class.java).apply {
                action = ACTION_SET_LISTENING_MODE
                putExtra(EXTRA_LISTENING_MODE, mode.wireValue.toInt())
            }
            PendingIntent.getBroadcast(
                appCtx,
                mode.wireValue.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
