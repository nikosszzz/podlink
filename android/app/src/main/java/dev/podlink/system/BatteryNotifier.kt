package dev.podlink.system

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import dev.podlink.MainActivity
import dev.podlink.R
import dev.podlink.core.BatteryMetric
import dev.podlink.core.ListeningMode
import dev.podlink.core.NotificationStyle
import dev.podlink.core.PodBattery
import dev.podlink.core.PodUiState
import dev.podlink.core.TraceLog

class BatteryNotifier(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(NotificationManager::class.java)
    private var lastSignature: String? = null
    private var localeTag: String? = null
    @Volatile var latestNotification: Notification? = null
        private set

    private var lblLeft = ""
    private var lblRight = ""
    private var lblSingleLeft = ""
    private var lblSingleRight = ""
    private var lblCase = ""
    private var lblBattery = ""
    private var lblCharging = ""

    private val openAppIntent: PendingIntent by lazy(LazyThreadSafetyMode.NONE) {
        PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    init {
        manager?.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.remake_battery_notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = appContext.getString(R.string.remake_battery_notif_channel_desc)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager?.createNotificationChannel(channel)
    }

    fun canPostPromoted(): Boolean =
        Build.VERSION.SDK_INT >= 36 && manager?.canPostPromotedNotifications() == true

    fun update(state: PodUiState) {
        if (!state.isConnected || !state.prefs.showBatteryNotification) {
            cancel()
            return
        }
        if (appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            cancel()
            return
        }

        ensureLabels()
        val includeCase = state.prefs.includeCaseInNotification
        val style = if (state.prefs.notificationStyle == NotificationStyle.LIVE_CHIP && Build.VERSION.SDK_INT >= 36) {
            NotificationStyle.LIVE_CHIP
        } else {
            NotificationStyle.DEVICE_CARD
        }
        val summaryText = state.battery.formatNotificationText(
            includeCase = includeCase,
            singleLeftLabel = lblSingleLeft,
            singleRightLabel = lblSingleRight,
            caseLabel = lblCase,
            chargingLabel = lblCharging,
        ) ?: run {
            cancel()
            return
        }

        val modeText = if (state.supportsListeningMode) {
            when (state.listeningMode) {
                ListeningMode.OFF -> appContext.getString(R.string.off)
                ListeningMode.TRANSPARENCY -> appContext.getString(R.string.transparency)
                ListeningMode.ADAPTIVE -> appContext.getString(R.string.adaptive)
                ListeningMode.NOISE_CANCELLATION -> appContext.getString(R.string.noise_cancellation)
            }
        } else {
            null
        }
        val title = state.displayName
        val layout = state.battery.activeLayout
        val smallIcon = when (layout) {
            PodBattery.Layout.LEFT_ONLY -> R.drawable.airpods_pro_left_notification
            PodBattery.Layout.RIGHT_ONLY -> R.drawable.airpods_pro_right_notification
            PodBattery.Layout.CASE_ONLY -> R.drawable.airpods_pro_case_notification
            else -> R.drawable.airpods
        }
        val modelResKey = state.matchedModel?.modelNumbers?.firstOrNull().orEmpty()
        val chipText = state.battery.formatChipText()
        val promotedAllowed = canPostPromoted()
        val signature = "${style.name}|$title|$modelResKey|$smallIcon|$summaryText|${modeText.orEmpty()}|$chipText|$promotedAllowed"
        if (signature == lastSignature) return

        val notification = when (style) {
            NotificationStyle.DEVICE_CARD -> buildDeviceCardNotification(
                state = state,
                title = title,
                summaryText = summaryText,
                modeText = modeText,
                smallIcon = smallIcon,
                includeCase = includeCase,
            )
            NotificationStyle.LIVE_CHIP -> if (Build.VERSION.SDK_INT >= 36) {
                buildLiveChipNotification(
                    state = state,
                    title = title,
                    summaryText = summaryText,
                    modeText = modeText,
                    smallIcon = smallIcon,
                    chipText = chipText,
                    includeCase = includeCase,
                )
            } else {
                buildDeviceCardNotification(
                    state = state,
                    title = title,
                    summaryText = summaryText,
                    modeText = modeText,
                    smallIcon = smallIcon,
                    includeCase = includeCase,
                )
            }
        }

        latestNotification = notification
        runCatching {
            val firstPost = lastSignature == null
            manager?.notify(NOTIFICATION_ID, notification)
            PodSessionService.ensureStarted(appContext)
            lastSignature = signature
            if (firstPost) {
                val flags = manager?.activeNotifications?.firstOrNull { it.id == NOTIFICATION_ID }?.notification?.flags ?: 0
                val promotedActive = Build.VERSION.SDK_INT >= 36 && (flags and Notification.FLAG_PROMOTED_ONGOING) != 0
                TraceLog.record("Notifier", "Posted ${style.name}: '$summaryText' promoted=$promotedActive")
            }
        }.onFailure { err ->
            TraceLog.record("Notifier", "Notify failed: ${err.message}")
        }
    }

    fun cancel() {
        latestNotification = null
        PodSessionService.stop(appContext)
        if (lastSignature != null) {
            runCatching { manager?.cancel(NOTIFICATION_ID) }
            lastSignature = null
        }
    }

    fun currentOrFallbackNotification(): Notification =
        latestNotification ?: Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.airpods)
            .setContentTitle(appContext.getString(R.string.app_name))
            .setContentText(appContext.getString(R.string.remake_status_connected))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent)
            .build()

    private fun buildDeviceCardNotification(
        state: PodUiState,
        title: String,
        summaryText: String,
        modeText: String?,
        smallIcon: Int,
        includeCase: Boolean,
    ): Notification {
        val model = state.matchedModel
        val leftImg = model?.leftRes ?: R.drawable.airpods_pro_2_left
        val rightImg = model?.rightRes ?: R.drawable.airpods_pro_2_right
        val caseImg = model?.caseRes ?: R.drawable.airpods_pro_2_case
        val budsImg = model?.budsRes ?: R.drawable.airpods_pro_2_buds

        val bat = state.battery
        val leftMetric = bat.left?.takeIf { it.isAvailable }
        val rightMetric = bat.right?.takeIf { it.isAvailable }
        val caseMetric = if (includeCase) bat.case?.takeIf { it.isAvailable } else null
        val headsetMetric = bat.headphone?.takeIf { it.isAvailable }

        val compact = RemoteViews(appContext.packageName, R.layout.notification_device_compact).apply {
            val line = if (modeText != null) "$summaryText · $modeText" else summaryText
            setTextViewText(R.id.notif_compact_subtitle, line)
        }

        val expanded = RemoteViews(appContext.packageName, R.layout.notification_device_card).apply {
            setTextViewText(R.id.notif_device_title, title)
            if (modeText != null) {
                setViewVisibility(R.id.notif_device_subtitle, View.VISIBLE)
                setTextViewText(R.id.notif_device_subtitle, modeText)
            } else {
                setViewVisibility(R.id.notif_device_subtitle, View.GONE)
            }

            if (leftMetric == null && rightMetric == null && headsetMetric != null) {
                bindCardSlot(
                    slotId = R.id.notif_left_slot,
                    imageId = R.id.notif_left_image,
                    iconId = R.id.notif_left_battery_icon,
                    percentId = R.id.notif_left_percent,
                    artRes = budsImg,
                    metric = headsetMetric,
                )
                setViewVisibility(R.id.notif_case_slot, View.GONE)
                setViewVisibility(R.id.notif_right_slot, View.GONE)
            } else {
                bindCardSlot(
                    slotId = R.id.notif_left_slot,
                    imageId = R.id.notif_left_image,
                    iconId = R.id.notif_left_battery_icon,
                    percentId = R.id.notif_left_percent,
                    artRes = leftImg,
                    metric = leftMetric,
                )
                bindCardSlot(
                    slotId = R.id.notif_case_slot,
                    imageId = R.id.notif_case_image,
                    iconId = R.id.notif_case_battery_icon,
                    percentId = R.id.notif_case_percent,
                    artRes = caseImg,
                    metric = caseMetric,
                )
                bindCardSlot(
                    slotId = R.id.notif_right_slot,
                    imageId = R.id.notif_right_image,
                    iconId = R.id.notif_right_battery_icon,
                    percentId = R.id.notif_right_percent,
                    artRes = rightImg,
                    metric = rightMetric,
                )
            }
        }

        return Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setColor(appContext.getColor(android.R.color.system_accent1_600))
            .setContentTitle(title)
            .setContentText(summaryText)
            .setSubText("Devices")
            .setStyle(Notification.DecoratedCustomViewStyle())
            .setCustomContentView(compact)
            .setCustomBigContentView(expanded)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent)
            .build()
    }

    private fun RemoteViews.bindCardSlot(
        slotId: Int,
        imageId: Int,
        iconId: Int,
        percentId: Int,
        artRes: Int,
        metric: BatteryMetric?,
    ) {
        if (metric == null) {
            setViewVisibility(slotId, View.GONE)
            return
        }
        setViewVisibility(slotId, View.VISIBLE)
        setImageViewResource(imageId, artRes)
        setImageViewResource(
            iconId,
            if (metric.isCharging) R.drawable.ic_notif_battery_charging else R.drawable.ic_notif_battery,
        )
        setInt(
            iconId,
            "setColorFilter",
            appContext.getColor(if (metric.isCharging) R.color.notif_charging_tint else R.color.notif_secondary_text),
        )
        setTextViewText(percentId, "${metric.level}%")
    }

    @RequiresApi(36)
    @android.annotation.SuppressLint("InlinedApi")
    private fun buildLiveChipNotification(
        state: PodUiState,
        title: String,
        summaryText: String,
        modeText: String?,
        smallIcon: Int,
        chipText: String?,
        includeCase: Boolean,
    ): Notification {
        val builder = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(summaryText)
            .apply { if (modeText != null) setSubText(modeText) }
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent)

        if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
            builder.setRequestPromotedOngoing(true)
        } else {
            builder.addExtras(Bundle().apply { putBoolean(Notification.EXTRA_REQUEST_PROMOTED_ONGOING, true) })
        }
        if (!chipText.isNullOrBlank()) builder.setShortCriticalText(chipText)

        if (Build.VERSION.SDK_INT >= 37) {
            buildMetricStyle(state.battery, includeCase)?.let { builder.setStyle(it) }
        } else {
            val bigText = if (modeText != null) "$summaryText · $modeText" else summaryText
            builder.setStyle(Notification.BigTextStyle().bigText(bigText))
        }
        return builder.build()
    }

    private fun ensureLabels() {
        val tag = appContext.resources.configuration.locales.toLanguageTags()
        if (tag != localeTag) {
            lblLeft = appContext.getString(R.string.remake_metric_left)
            lblRight = appContext.getString(R.string.remake_metric_right)
            lblSingleLeft = appContext.getString(R.string.remake_single_left_airpod)
            lblSingleRight = appContext.getString(R.string.remake_single_right_airpod)
            lblCase = appContext.getString(R.string.remake_metric_case)
            lblBattery = appContext.getString(R.string.remake_metric_battery)
            lblCharging = appContext.getString(R.string.remake_charging)
            localeTag = tag
        }
    }

    @RequiresApi(37)
    private fun buildMetricStyle(bat: PodBattery, includeCase: Boolean): Notification.MetricStyle? {
        ensureLabels()
        val layout = bat.activeLayout
        val leftLbl = if (layout == PodBattery.Layout.LEFT_ONLY) lblSingleLeft else lblLeft
        val rightLbl = if (layout == PodBattery.Layout.RIGHT_ONLY) lblSingleRight else lblRight
        val items = buildList {
            bat.left?.takeIf { it.isAvailable }?.let { add(leftLbl to it) }
            bat.right?.takeIf { it.isAvailable }?.let { add(rightLbl to it) }
            if (includeCase && size < 3) bat.case?.takeIf { it.isAvailable }?.let { add(lblCase to it) }
            if (isEmpty()) bat.headphone?.takeIf { it.isAvailable }?.let { add(lblBattery to it) }
        }
        if (items.isEmpty()) return null
        val metrics = items.map { (lbl, m) -> toMetric(lbl, m) }
        val crit = items.indices.minByOrNull { items[it].second.level } ?: 0
        return Notification.MetricStyle().setMetrics(metrics).setCriticalMetric(crit)
    }

    @RequiresApi(37)
    private fun toMetric(label: String, m: BatteryMetric): Notification.Metric {
        val text = if (m.isCharging) "$label ⚡" else label
        val style = when {
            m.isCharging -> Notification.SEMANTIC_STYLE_INFO
            m.level <= 15 -> Notification.SEMANTIC_STYLE_DANGER
            m.level <= 25 -> Notification.SEMANTIC_STYLE_CAUTION
            else -> Notification.SEMANTIC_STYLE_SAFE
        }
        return Notification.Metric(Notification.Metric.FixedInt(m.level, "%"), text, style)
    }

    companion object {
        private const val LEGACY_CHANNEL_ID = "airpods_battery_status"
        private const val CHANNEL_ID = "airpods_battery_live"
        const val NOTIFICATION_ID = 1001
    }
}
