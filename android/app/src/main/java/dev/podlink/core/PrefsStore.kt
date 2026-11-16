package dev.podlink.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class UserPrefs(
    val autoPauseResumeOnEarDetection: Boolean = true,
    val showBatteryNotification: Boolean = true,
    val notificationStyle: NotificationStyle = NotificationStyle.DEVICE_CARD,
    val includeCaseInNotification: Boolean = true,
    val shizukuBatterySync: Boolean = false,
    val conversationAwarenessDuckVolume: Boolean = true,
    val conversationAwarenessPauseMusic: Boolean = false,
    val conversationAwarenessVolumePercent: Int = 25,
    val conversationAwarenessRelativeVolume: Boolean = false,
    val headGesturesEnabled: Boolean = false,
    val useAlternateHeadTrackingPackets: Boolean = true,
    val disconnectWhenNotWearing: Boolean = false,
    val showPhoneBatteryInWidget: Boolean = false,
    val qsClickBehaviorDialog: Boolean = false,
    val stemMapping: StemMapping = StemMapping(),
    val cachedDeviceName: String = "",
    val cachedModelNumber: String = "",
    val cachedSerialNumber: String = "",
    val cachedLeftSerial: String = "",
    val cachedRightSerial: String = "",
    val cachedFirmwareVersion: String = "",
    val cachedListeningModeMask: Int = AacpSpec.DEFAULT_MODE_MASK,
    val cachedAllowOffOption: Boolean = false,
)

class PrefsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<UserPrefs> = _state.asStateFlow()

    fun setAutoPauseResumeOnEarDetection(enabled: Boolean) = update(KEY_AUTO_PAUSE_EAR, enabled) {
        copy(autoPauseResumeOnEarDetection = enabled)
    }

    fun setShowBatteryNotification(enabled: Boolean) = update(KEY_SHOW_BATTERY_NOTIF, enabled) {
        copy(showBatteryNotification = enabled)
    }

    fun setNotificationStyle(style: NotificationStyle) {
        prefs.edit { putString(KEY_NOTIF_STYLE, style.name) }
        _state.value = _state.value.copy(notificationStyle = style)
    }

    fun setIncludeCaseInNotification(enabled: Boolean) = update(KEY_INCLUDE_CASE_NOTIF, enabled) {
        copy(includeCaseInNotification = enabled)
    }

    fun setShizukuBatterySync(enabled: Boolean) = update(KEY_SHIZUKU_BATTERY, enabled) {
        copy(shizukuBatterySync = enabled)
    }

    fun setConversationAwarenessDuckVolume(enabled: Boolean) = update(KEY_CA_DUCK_VOLUME, enabled) {
        copy(conversationAwarenessDuckVolume = enabled)
    }

    fun setConversationAwarenessPauseMusic(enabled: Boolean) = update(KEY_CA_PAUSE_MUSIC, enabled) {
        copy(conversationAwarenessPauseMusic = enabled)
    }

    fun setConversationAwarenessVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(10, 85)
        prefs.edit { putInt(KEY_CA_VOLUME_PERCENT, clamped) }
        _state.value = _state.value.copy(conversationAwarenessVolumePercent = clamped)
    }

    fun setConversationAwarenessRelativeVolume(enabled: Boolean) = update(KEY_CA_RELATIVE_VOLUME, enabled) {
        copy(conversationAwarenessRelativeVolume = enabled)
    }

    fun setHeadGesturesEnabled(enabled: Boolean) = update(KEY_HEAD_GESTURES, enabled) {
        copy(headGesturesEnabled = enabled)
    }

    fun setUseAlternateHeadTrackingPackets(enabled: Boolean) = update(KEY_ALT_HEAD_TRACKING, enabled) {
        copy(useAlternateHeadTrackingPackets = enabled)
    }

    fun setDisconnectWhenNotWearing(enabled: Boolean) = update(KEY_DISCONNECT_NOT_WEARING, enabled) {
        copy(disconnectWhenNotWearing = enabled)
    }

    fun setShowPhoneBatteryInWidget(enabled: Boolean) = update(KEY_SHOW_PHONE_WIDGET, enabled) {
        copy(showPhoneBatteryInWidget = enabled)
    }

    fun setQsClickBehaviorDialog(openDialog: Boolean) = update(KEY_QS_DIALOG, openDialog) {
        copy(qsClickBehaviorDialog = openDialog)
    }

    fun setStemAction(press: StemPress, isLeft: Boolean, action: StemAction) {
        val current = _state.value
        val next = current.stemMapping.updated(press, isLeft, action)
        if (next == current.stemMapping) return
        prefs.edit { putString(stemKey(press, isLeft), action.name) }
        _state.value = current.copy(stemMapping = next)
    }

    fun cacheDeviceMetadata(
        name: String,
        modelNumber: String,
        serialNumber: String,
        leftSerial: String,
        rightSerial: String,
        firmwareVersion: String,
    ) {
        val prev = _state.value
        val n = name.ifBlank { prev.cachedDeviceName }
        val m = modelNumber.ifBlank { prev.cachedModelNumber }
        val s = serialNumber.ifBlank { prev.cachedSerialNumber }
        val ls = leftSerial.ifBlank { prev.cachedLeftSerial }
        val rs = rightSerial.ifBlank { prev.cachedRightSerial }
        val fw = firmwareVersion.ifBlank { prev.cachedFirmwareVersion }
        if (n == prev.cachedDeviceName && m == prev.cachedModelNumber && s == prev.cachedSerialNumber &&
            ls == prev.cachedLeftSerial && rs == prev.cachedRightSerial && fw == prev.cachedFirmwareVersion
        ) {
            return
        }
        prefs.edit {
            if (n != prev.cachedDeviceName) putString(KEY_CACHED_NAME, n)
            if (m != prev.cachedModelNumber) putString(KEY_CACHED_MODEL, m)
            if (s != prev.cachedSerialNumber) putString(KEY_CACHED_SERIAL, s)
            if (ls != prev.cachedLeftSerial) putString(KEY_CACHED_LEFT_SERIAL, ls)
            if (rs != prev.cachedRightSerial) putString(KEY_CACHED_RIGHT_SERIAL, rs)
            if (fw != prev.cachedFirmwareVersion) putString(KEY_CACHED_FIRMWARE, fw)
        }
        _state.value = prev.copy(
            cachedDeviceName = n,
            cachedModelNumber = m,
            cachedSerialNumber = s,
            cachedLeftSerial = ls,
            cachedRightSerial = rs,
            cachedFirmwareVersion = fw,
        )
    }

    fun cacheListeningModeMask(mask: Int) {
        val prev = _state.value
        if (prev.cachedListeningModeMask == mask) return
        prefs.edit { putInt(KEY_LISTENING_MASK, mask) }
        _state.value = prev.copy(cachedListeningModeMask = mask)
    }

    fun cacheAllowOffOption(allowOff: Boolean) {
        if (_state.value.cachedAllowOffOption == allowOff) return
        update(KEY_ALLOW_OFF, allowOff) { copy(cachedAllowOffOption = allowOff) }
    }

    fun resetControlDefaults() {
        val prev = _state.value
        val defaults = StemMapping()
        if (prev.stemMapping == defaults && !prev.headGesturesEnabled) return
        prefs.edit {
            remove(KEY_L_SINGLE)
            remove(KEY_R_SINGLE)
            remove(KEY_L_DOUBLE)
            remove(KEY_R_DOUBLE)
            remove(KEY_L_TRIPLE)
            remove(KEY_R_TRIPLE)
            remove(KEY_L_LONG)
            remove(KEY_R_LONG)
            putBoolean(KEY_HEAD_GESTURES, false)
        }
        _state.value = prev.copy(stemMapping = defaults, headGesturesEnabled = false)
    }

    private inline fun update(key: String, value: Boolean, transform: UserPrefs.() -> UserPrefs) {
        prefs.edit { putBoolean(key, value) }
        _state.value = _state.value.transform()
    }

    private fun stemKey(press: StemPress, isLeft: Boolean): String = when (press) {
        StemPress.SINGLE_PRESS -> if (isLeft) KEY_L_SINGLE else KEY_R_SINGLE
        StemPress.DOUBLE_PRESS -> if (isLeft) KEY_L_DOUBLE else KEY_R_DOUBLE
        StemPress.TRIPLE_PRESS -> if (isLeft) KEY_L_TRIPLE else KEY_R_TRIPLE
        StemPress.LONG_PRESS -> if (isLeft) KEY_L_LONG else KEY_R_LONG
    }

    private fun load(): UserPrefs = UserPrefs(
        autoPauseResumeOnEarDetection = prefs.getBoolean(KEY_AUTO_PAUSE_EAR, true),
        showBatteryNotification = prefs.getBoolean(KEY_SHOW_BATTERY_NOTIF, true),
        notificationStyle = NotificationStyle.fromStored(prefs.getString(KEY_NOTIF_STYLE, null)),
        includeCaseInNotification = prefs.getBoolean(KEY_INCLUDE_CASE_NOTIF, true),
        shizukuBatterySync = prefs.getBoolean(KEY_SHIZUKU_BATTERY, false),
        conversationAwarenessDuckVolume = prefs.getBoolean(KEY_CA_DUCK_VOLUME, true),
        conversationAwarenessPauseMusic = prefs.getBoolean(KEY_CA_PAUSE_MUSIC, false),
        conversationAwarenessVolumePercent = prefs.getInt(KEY_CA_VOLUME_PERCENT, 25).coerceIn(10, 85),
        conversationAwarenessRelativeVolume = prefs.getBoolean(KEY_CA_RELATIVE_VOLUME, false),
        headGesturesEnabled = prefs.getBoolean(KEY_HEAD_GESTURES, false),
        useAlternateHeadTrackingPackets = prefs.getBoolean(KEY_ALT_HEAD_TRACKING, true),
        disconnectWhenNotWearing = prefs.getBoolean(KEY_DISCONNECT_NOT_WEARING, false),
        showPhoneBatteryInWidget = prefs.getBoolean(KEY_SHOW_PHONE_WIDGET, false),
        qsClickBehaviorDialog = prefs.getBoolean(KEY_QS_DIALOG, false),
        stemMapping = StemMapping(
            leftSingle = StemAction.fromStored(prefs.getString(KEY_L_SINGLE, null), StemAction.PLAY_PAUSE),
            rightSingle = StemAction.fromStored(prefs.getString(KEY_R_SINGLE, null), StemAction.PLAY_PAUSE),
            leftDouble = StemAction.fromStored(prefs.getString(KEY_L_DOUBLE, null), StemAction.NEXT_TRACK),
            rightDouble = StemAction.fromStored(prefs.getString(KEY_R_DOUBLE, null), StemAction.NEXT_TRACK),
            leftTriple = StemAction.fromStored(prefs.getString(KEY_L_TRIPLE, null), StemAction.PREVIOUS_TRACK),
            rightTriple = StemAction.fromStored(prefs.getString(KEY_R_TRIPLE, null), StemAction.PREVIOUS_TRACK),
            leftLong = StemAction.fromStored(prefs.getString(KEY_L_LONG, null), StemAction.CYCLE_NOISE_CONTROL),
            rightLong = StemAction.fromStored(prefs.getString(KEY_R_LONG, null), StemAction.CYCLE_NOISE_CONTROL),
        ),
        cachedDeviceName = prefs.getString(KEY_CACHED_NAME, "").orEmpty(),
        cachedModelNumber = prefs.getString(KEY_CACHED_MODEL, "").orEmpty(),
        cachedSerialNumber = prefs.getString(KEY_CACHED_SERIAL, "").orEmpty(),
        cachedLeftSerial = prefs.getString(KEY_CACHED_LEFT_SERIAL, "").orEmpty(),
        cachedRightSerial = prefs.getString(KEY_CACHED_RIGHT_SERIAL, "").orEmpty(),
        cachedFirmwareVersion = prefs.getString(KEY_CACHED_FIRMWARE, "").orEmpty(),
        cachedListeningModeMask = prefs.getInt(KEY_LISTENING_MASK, AacpSpec.DEFAULT_MODE_MASK),
        cachedAllowOffOption = prefs.getBoolean(KEY_ALLOW_OFF, false),
    )

    private companion object {
        const val PREFS_NAME = "podlink_prefs"
        const val KEY_AUTO_PAUSE_EAR = "auto_pause_resume_ear"
        const val KEY_SHOW_BATTERY_NOTIF = "show_battery_notification"
        const val KEY_NOTIF_STYLE = "notification_style"
        const val KEY_INCLUDE_CASE_NOTIF = "include_case_notification"
        const val KEY_SHIZUKU_BATTERY = "shizuku_battery_sync"
        const val KEY_CA_DUCK_VOLUME = "ca_duck_volume"
        const val KEY_CA_PAUSE_MUSIC = "ca_pause_music"
        const val KEY_CA_VOLUME_PERCENT = "ca_volume_percent"
        const val KEY_CA_RELATIVE_VOLUME = "ca_relative_volume"
        const val KEY_HEAD_GESTURES = "head_gestures_enabled"
        const val KEY_ALT_HEAD_TRACKING = "alt_head_tracking_packets"
        const val KEY_DISCONNECT_NOT_WEARING = "disconnect_when_not_wearing"
        const val KEY_SHOW_PHONE_WIDGET = "show_phone_battery_widget"
        const val KEY_QS_DIALOG = "qs_click_behavior_dialog"
        const val KEY_L_SINGLE = "left_single_press"
        const val KEY_R_SINGLE = "right_single_press"
        const val KEY_L_DOUBLE = "left_double_press"
        const val KEY_R_DOUBLE = "right_double_press"
        const val KEY_L_TRIPLE = "left_triple_press"
        const val KEY_R_TRIPLE = "right_triple_press"
        const val KEY_L_LONG = "left_long_press"
        const val KEY_R_LONG = "right_long_press"
        const val KEY_CACHED_NAME = "cached_device_name"
        const val KEY_CACHED_MODEL = "cached_model_number"
        const val KEY_CACHED_SERIAL = "cached_serial_number"
        const val KEY_CACHED_LEFT_SERIAL = "cached_left_serial_number"
        const val KEY_CACHED_RIGHT_SERIAL = "cached_right_serial_number"
        const val KEY_CACHED_FIRMWARE = "cached_firmware_version"
        const val KEY_LISTENING_MASK = "listening_mode_mask"
        const val KEY_ALLOW_OFF = "allow_off_option"
    }
}
