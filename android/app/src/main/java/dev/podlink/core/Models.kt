package dev.podlink.core

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import dev.podlink.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
data class BatteryMetric(
    val slot: BatterySlot,
    val level: Int,
    val state: BatteryState,
) {
    val isAvailable: Boolean
        get() = state != BatteryState.DISCONNECTED && level in 0..100

    val isCharging: Boolean
        get() = state == BatteryState.CHARGING || state == BatteryState.OPTIMIZED_CHARGING
}

@Immutable
data class PodBattery(
    val left: BatteryMetric? = null,
    val right: BatteryMetric? = null,
    val case: BatteryMetric? = null,
    val headphone: BatteryMetric? = null,
) {
    enum class Layout {
        BOTH_BUDS,
        LEFT_ONLY,
        RIGHT_ONLY,
        HEADPHONE,
        CASE_ONLY,
        NONE,
    }

    val activeLayout: Layout
        get() {
            val l = left?.isAvailable == true
            val r = right?.isAvailable == true
            return when {
                l && r -> Layout.BOTH_BUDS
                l -> Layout.LEFT_ONLY
                r -> Layout.RIGHT_ONLY
                headphone?.isAvailable == true -> Layout.HEADPHONE
                case?.isAvailable == true -> Layout.CASE_ONLY
                else -> Layout.NONE
            }
        }

    val hasAnyAvailable: Boolean
        get() = left?.isAvailable == true ||
            right?.isAvailable == true ||
            case?.isAvailable == true ||
            headphone?.isAvailable == true

    fun formatCompact(includeCase: Boolean = true): String? {
        val parts = buildList {
            left?.takeIf { it.isAvailable }?.let { add(labelLevel("L", it)) }
            right?.takeIf { it.isAvailable }?.let { add(labelLevel("R", it)) }
            if (includeCase) {
                case?.takeIf { it.isAvailable }?.let { add(labelLevel("Case", it)) }
            }
            if (isEmpty()) {
                headphone?.takeIf { it.isAvailable }?.let { add(labelLevel("Pods", it)) }
            }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" | ")
    }

    fun formatNotificationText(
        includeCase: Boolean = true,
        singleLeftLabel: String = "Left AirPod",
        singleRightLabel: String = "Right AirPod",
        caseLabel: String = "Case",
        chargingLabel: String = "Charging",
        headphoneLabel: String = "Pods",
    ): String? = when (activeLayout) {
        Layout.BOTH_BUDS -> formatCompact(includeCase = includeCase)
        Layout.LEFT_ONLY -> singleSummary(singleLeftLabel, left!!, includeCase, caseLabel, chargingLabel)
        Layout.RIGHT_ONLY -> singleSummary(singleRightLabel, right!!, includeCase, caseLabel, chargingLabel)
        Layout.HEADPHONE -> headphone?.takeIf { it.isAvailable }?.let { labelLevel(headphoneLabel, it) }
        Layout.CASE_ONLY -> if (includeCase) case?.takeIf { it.isAvailable }?.let { labelLevel(caseLabel, it) } else null
        Layout.NONE -> null
    }

    private fun singleSummary(
        label: String,
        metric: BatteryMetric,
        includeCase: Boolean,
        caseLabel: String,
        chargingLabel: String,
    ): String {
        val base = if (metric.isCharging) "$label · ${metric.level}% ($chargingLabel)" else "$label · ${metric.level}%"
        val caseMetric = if (includeCase) case?.takeIf { it.isAvailable } else null
        return if (caseMetric != null) "$base | ${labelLevel(caseLabel, caseMetric)}" else base
    }

    fun formatChipText(): String? {
        val l = left?.takeIf { it.isAvailable }
        val r = right?.takeIf { it.isAvailable }
        return when {
            l != null && r != null -> "${l.level}|${r.level}"
            l != null -> "L:${l.level}%"
            r != null -> "R:${r.level}%"
            else -> headphone?.takeIf { it.isAvailable }?.let { "${it.level}%" }
                ?: case?.takeIf { it.isAvailable }?.let { "C:${it.level}%" }
        }
    }

    private fun labelLevel(prefix: String, metric: BatteryMetric): String =
        if (metric.isCharging) "$prefix: ${metric.level}%⚡" else "$prefix: ${metric.level}%"

    companion object {
        fun from(metrics: List<BatteryMetric>): PodBattery {
            var left: BatteryMetric? = null
            var right: BatteryMetric? = null
            var case: BatteryMetric? = null
            var hp: BatteryMetric? = null
            for (m in metrics) {
                when (m.slot) {
                    BatterySlot.LEFT -> left = m
                    BatterySlot.RIGHT -> right = m
                    BatterySlot.CASE -> case = m
                    BatterySlot.HEADPHONE -> hp = m
                }
            }
            return PodBattery(left, right, case, hp)
        }
    }
}

@Immutable
data class WearSnapshot(
    val primary: WearState = WearState.DISCONNECTED,
    val secondary: WearState = WearState.DISCONNECTED,
) {
    val primaryInEar: Boolean get() = primary == WearState.IN_EAR
    val secondaryInEar: Boolean get() = secondary == WearState.IN_EAR
    val inEarCount: Int get() = (if (primaryInEar) 1 else 0) + (if (secondaryInEar) 1 else 0)
    val anyInEar: Boolean get() = primaryInEar || secondaryInEar
    val anyOutOfCase: Boolean
        get() = primary.isOutOfCase || secondary.isOutOfCase
}

private val WearState.isOutOfCase: Boolean
    get() = this == WearState.IN_EAR || this == WearState.OUT_OF_EAR

@Immutable
data class DeviceMetadata(
    val name: String = "",
    val modelNumber: String = "",
    val manufacturer: String = "",
    val serialNumber: String = "",
    val version1: String = "",
    val version2: String = "",
    val hardwareRevision: String = "",
    val updaterId: String = "",
    val leftSerial: String = "",
    val rightSerial: String = "",
    val version3: String = "",
)

@Immutable
data class AudioRoute(val mac: String, val type: AudioRouteType)

@Immutable
data class PeerDevice(val mac: String, val info1: Byte, val info2: Byte, val type: String? = null)

@Immutable
data class EqCurve(
    val state: Int = RECOMMENDED,
    val low: Int = NEUTRAL,
    val mid: Int = NEUTRAL,
    val high: Int = NEUTRAL,
) {
    init {
        require(low in 0..100) { "low must be in 0..100, got $low" }
        require(mid in 0..100) { "mid must be in 0..100, got $mid" }
        require(high in 0..100) { "high must be in 0..100, got $high" }
    }

    val isEnabled: Boolean get() = state == CUSTOM

    companion object {
        const val RECOMMENDED = 1
        const val CUSTOM = 2
        const val NEUTRAL = 50
    }
}

@Immutable
data class Accommodations(
    val onMedia: Boolean = false,
    val onPhone: Boolean = false,
    val bands: FloatArray = FloatArray(8),
) {
    override fun equals(other: Any?): Boolean =
        other is Accommodations && onMedia == other.onMedia && onPhone == other.onPhone && bands.contentEquals(other.bands)

    override fun hashCode(): Int {
        var r = onMedia.hashCode()
        r = 31 * r + onPhone.hashCode()
        r = 31 * r + bands.contentHashCode()
        return r
    }
}

@Immutable
data class MotionSample(
    val o1: Int,
    val o2: Int,
    val o3: Int,
    val horizontalAccel: Float,
    val verticalAccel: Float,
)

@Immutable
data class MotionUiState(
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val verticalAccel: Float = 0f,
    val horizontalAccel: Float = 0f,
)

enum class StemAction(@param:StringRes val labelRes: Int) {
    PLAY_PAUSE(R.string.remake_play_pause),
    PREVIOUS_TRACK(R.string.remake_previous_track),
    NEXT_TRACK(R.string.remake_next_track),
    DIGITAL_ASSISTANT(R.string.digital_assistant),
    CYCLE_NOISE_CONTROL(R.string.remake_cycle_noise_control_modes);

    companion object {
        fun fromStored(name: String?, fallback: StemAction): StemAction =
            entries.find { it.name == name } ?: fallback

        fun defaultFor(press: StemPress): StemAction = when (press) {
            StemPress.SINGLE_PRESS -> PLAY_PAUSE
            StemPress.DOUBLE_PRESS -> NEXT_TRACK
            StemPress.TRIPLE_PRESS -> PREVIOUS_TRACK
            StemPress.LONG_PRESS -> CYCLE_NOISE_CONTROL
        }
    }
}

@Immutable
data class StemMapping(
    val leftSingle: StemAction = StemAction.PLAY_PAUSE,
    val rightSingle: StemAction = StemAction.PLAY_PAUSE,
    val leftDouble: StemAction = StemAction.NEXT_TRACK,
    val rightDouble: StemAction = StemAction.NEXT_TRACK,
    val leftTriple: StemAction = StemAction.PREVIOUS_TRACK,
    val rightTriple: StemAction = StemAction.PREVIOUS_TRACK,
    val leftLong: StemAction = StemAction.CYCLE_NOISE_CONTROL,
    val rightLong: StemAction = StemAction.CYCLE_NOISE_CONTROL,
) {
    fun actionFor(press: StemPress, side: StemSide): StemAction = when (press) {
        StemPress.SINGLE_PRESS -> if (side == StemSide.LEFT) leftSingle else rightSingle
        StemPress.DOUBLE_PRESS -> if (side == StemSide.LEFT) leftDouble else rightDouble
        StemPress.TRIPLE_PRESS -> if (side == StemSide.LEFT) leftTriple else rightTriple
        StemPress.LONG_PRESS -> if (side == StemSide.LEFT) leftLong else rightLong
    }

    fun isCustomized(press: StemPress): Boolean {
        val d = StemAction.defaultFor(press)
        return actionFor(press, StemSide.LEFT) != d || actionFor(press, StemSide.RIGHT) != d
    }

    fun updated(press: StemPress, isLeft: Boolean, action: StemAction): StemMapping = when (press) {
        StemPress.SINGLE_PRESS -> if (isLeft) copy(leftSingle = action) else copy(rightSingle = action)
        StemPress.DOUBLE_PRESS -> if (isLeft) copy(leftDouble = action) else copy(rightDouble = action)
        StemPress.TRIPLE_PRESS -> if (isLeft) copy(leftTriple = action) else copy(rightTriple = action)
        StemPress.LONG_PRESS -> if (isLeft) copy(leftLong = action) else copy(rightLong = action)
    }
}

@Immutable
data class CompanionLinkSummary(
    val id: Int,
    val macAddress: String,
    val displayName: String,
)

@Immutable
data class CompanionSnapshot(
    val links: List<CompanionLinkSummary> = emptyList(),
    val observingPresence: Boolean = false,
)

@Immutable
data class BondedCandidate(
    val name: String,
    val macAddress: String,
    val hasPodUuid: Boolean,
    val isAudioDevice: Boolean,
) {
    val isLikelyPods: Boolean
        get() = hasPodUuid || name.contains("AirPods", ignoreCase = true)
}

enum class ConnectionPhase {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

enum class ShizukuBridgeStatus {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_REQUIRED,
    READY,
}

enum class NotificationStyle {
    DEVICE_CARD,
    LIVE_CHIP;

    companion object {
        fun fromStored(name: String?): NotificationStyle =
            entries.find { it.name == name } ?: DEVICE_CARD
    }
}

@Immutable
data class PodUiState(
    val phase: ConnectionPhase = ConnectionPhase.DISCONNECTED,
    val deviceAddress: String? = null,
    val deviceName: String = "",
    val modelNumber: String = "",
    val metadata: DeviceMetadata? = null,
    val battery: PodBattery = PodBattery(),
    val wear: WearSnapshot = WearSnapshot(),
    val listeningMode: ListeningMode = ListeningMode.NOISE_CANCELLATION,
    val listeningModeKnown: Boolean = false,
    val listeningModeMask: Int = AacpSpec.DEFAULT_MODE_MASK,
    val allowOffOption: Boolean = false,
    val earDetectionEnabled: Boolean = true,
    val conversationAwarenessEnabled: Boolean = false,
    val speakingDetected: Boolean = false,
    val personalizedVolumeEnabled: Boolean = false,
    val volumeSwipeEnabled: Boolean = true,
    val singleBudAncEnabled: Boolean = false,
    val customEq: EqCurve = EqCurve(),
    val customEqReceived: Boolean = false,
    val accommodations: Accommodations = Accommodations(),
    val adaptiveAncStrength: Int = 50,
    val chimeVolume: Int = 75,
    val micMode: Int = 0,
    val callManagementFlipped: Boolean = false,
    val doubleClickInterval: Int = 0,
    val clickHoldInterval: Int = 0,
    val volumeSwipeInterval: Int = 1,
    val sleepDetectionEnabled: Boolean = false,
    val automaticConnectionEnabled: Boolean = true,
    val optimizedChargeLimitEnabled: Boolean = false,
    val ppeEnabled: Boolean = false,
    val hearingAidEnrolled: Boolean = false,
    val hearingAidEnabled: Boolean = false,
    val inCaseToneEnabled: Boolean = true,
    val connectedPeers: List<PeerDevice> = emptyList(),
    val audioSource: AudioRoute? = null,
    val attAvailable: Boolean = false,
    val transparency: TransparencyTuning? = null,
    val loudSoundReductionEnabled: Boolean = false,
    val hearingAid: HearingAidTuning? = null,
    val headTrackingActive: Boolean = false,
    val headGestureTesting: Boolean = false,
    val lastDetectedGesture: Boolean? = null,
    val bluetoothGranted: Boolean = false,
    val links: List<CompanionLinkSummary> = emptyList(),
    val prefs: UserPrefs = UserPrefs(),
    val shizukuStatus: ShizukuBridgeStatus = ShizukuBridgeStatus.NOT_INSTALLED,
    val shizukuLastSyncNote: String? = null,
    val errorMessage: String? = null,
    val statusHint: String? = null,
    val hiddenApiBlocked: Boolean = false,
) {
    val isConnected: Boolean get() = phase == ConnectionPhase.CONNECTED
    val effectiveModelNumber: String get() = modelNumber.ifBlank { prefs.cachedModelNumber }
    val matchedModel: PodModelSpec?
        get() = effectiveModelNumber.takeIf { it.isNotBlank() }?.let(PodCatalog::findByModelNumber)
    val capabilities: Set<PodFeature>
        get() = matchedModel?.features ?: emptySet()
    val supportsListeningMode: Boolean
        get() = matchedModel?.let { PodFeature.LISTENING_MODE in it.features } ?: listeningModeKnown
    val supportsStemConfig: Boolean
        get() = matchedModel?.let { PodFeature.STEM_CONFIG in it.features } ?: false
    val displayName: String
        get() = deviceName.ifBlank { prefs.cachedDeviceName.ifBlank { matchedModel?.name ?: "AirPods" } }
    val effectiveSerials: List<String>
        get() = listOf(
            metadata?.serialNumber.orEmpty().ifBlank { prefs.cachedSerialNumber },
            metadata?.leftSerial.orEmpty().ifBlank { prefs.cachedLeftSerial },
            metadata?.rightSerial.orEmpty().ifBlank { prefs.cachedRightSerial },
        )
    val effectiveFirmware: String
        get() = metadata?.version1.orEmpty().ifBlank { prefs.cachedFirmwareVersion }
}

object TraceLog {
    private const val MAX_ENTRIES = 120
    private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS", Locale.US)
    private val lock = Any()
    private val buffer = ArrayDeque<String>(MAX_ENTRIES)
    private val _entries = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())
    val entries: kotlinx.coroutines.flow.StateFlow<List<String>> = _entries

    fun record(tag: String, message: String) {
        val formatted = "${LocalTime.now().format(FORMATTER)} [$tag] $message"
        val push = _entries.subscriptionCount.value > 0
        val snapshot = synchronized(lock) {
            if (buffer.size >= MAX_ENTRIES) buffer.removeFirst()
            buffer.addLast(formatted)
            if (push) buffer.toList() else null
        }
        if (snapshot != null) _entries.value = snapshot
    }

    fun sync() {
        _entries.value = synchronized(lock) { buffer.toList() }
    }

    fun clear() {
        synchronized(lock) { buffer.clear() }
        _entries.value = emptyList()
    }

    fun formatReport(
        state: PodUiState,
        companion: CompanionSnapshot,
        hiddenApiReady: Boolean,
    ): String {
        val snapshot = synchronized(lock) { buffer.toList() }
        return buildString {
            appendLine("=== Podlink Diagnostics ===")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE}, SDK ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Phase: ${state.phase}, HiddenApiReady: $hiddenApiReady, HiddenApiBlocked: ${state.hiddenApiBlocked}")
            appendLine("PermissionsGranted: ${state.bluetoothGranted}, Shizuku: ${state.shizukuStatus} (${state.shizukuLastSyncNote ?: "-"})")
            appendLine("CompanionObserving: ${companion.observingPresence}, Links: ${companion.links.size}")
            companion.links.forEach { appendLine("  Link id=${it.id} name='${it.displayName}' mac=${it.macAddress}") }
            state.deviceAddress?.let { appendLine("TargetMAC: $it") }
            if (state.modelNumber.isNotBlank()) {
                appendLine("Model: ${state.matchedModel?.name ?: state.displayName} (${state.modelNumber})")
            }
            state.errorMessage?.let { appendLine("Error: $it") }
            state.statusHint?.let { appendLine("Hint: $it") }
            appendLine("--- Events (${snapshot.size}) ---")
            if (snapshot.isEmpty()) appendLine("(none)") else snapshot.forEach(::appendLine)
        }.trimEnd()
    }
}
