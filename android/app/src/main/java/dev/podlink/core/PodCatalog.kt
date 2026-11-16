package dev.podlink.core

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import dev.podlink.R

enum class PodFeature {
    LISTENING_MODE,
    CONVERSATION_AWARENESS,
    STEM_CONFIG,
    HEAD_GESTURES,
    LOUD_SOUND_REDUCTION,
    PPE,
    SLEEP_DETECTION,
    HEARING_AID,
    ADAPTIVE_AUDIO,
    ADAPTIVE_VOLUME,
    SWIPE_FOR_VOLUME,
    HRM,
}

@Immutable
data class PodModelSpec(
    val modelNumbers: Set<String>,
    val name: String,
    @param:DrawableRes val budsRes: Int,
    @param:DrawableRes val leftRes: Int,
    @param:DrawableRes val rightRes: Int,
    @param:DrawableRes val caseRes: Int,
    val features: Set<PodFeature>,
)

object PodCatalog {
    val models: List<PodModelSpec> = listOf(
        PodModelSpec(
            modelNumbers = setOf("A1523", "A1722"),
            name = "AirPods 1",
            budsRes = R.drawable.airpods_1_buds,
            leftRes = R.drawable.airpods_1_left,
            rightRes = R.drawable.airpods_1_right,
            caseRes = R.drawable.airpods_1_case,
            features = emptySet(),
        ),
        PodModelSpec(
            modelNumbers = setOf("A2032", "A2031"),
            name = "AirPods 2",
            budsRes = R.drawable.airpods_2_buds,
            leftRes = R.drawable.airpods_2_left,
            rightRes = R.drawable.airpods_2_right,
            caseRes = R.drawable.airpods_2_case,
            features = emptySet(),
        ),
        PodModelSpec(
            modelNumbers = setOf("A2565", "A2564"),
            name = "AirPods 3",
            budsRes = R.drawable.airpods_3_buds,
            leftRes = R.drawable.airpods_3_left,
            rightRes = R.drawable.airpods_3_right,
            caseRes = R.drawable.airpods_3_case,
            features = setOf(PodFeature.HEAD_GESTURES),
        ),
        PodModelSpec(
            modelNumbers = setOf("A3053", "A3050", "A3054"),
            name = "AirPods 4",
            budsRes = R.drawable.airpods_4_buds,
            leftRes = R.drawable.airpods_4_left,
            rightRes = R.drawable.airpods_4_right,
            caseRes = R.drawable.airpods_4_case,
            features = setOf(PodFeature.HEAD_GESTURES, PodFeature.SLEEP_DETECTION, PodFeature.ADAPTIVE_VOLUME),
        ),
        PodModelSpec(
            modelNumbers = setOf("A3056", "A3055", "A3057"),
            name = "AirPods 4 (ANC)",
            budsRes = R.drawable.airpods_4_anc_buds,
            leftRes = R.drawable.airpods_4_left,
            rightRes = R.drawable.airpods_4_right,
            caseRes = R.drawable.airpods_4_case,
            features = setOf(
                PodFeature.LISTENING_MODE, PodFeature.CONVERSATION_AWARENESS, PodFeature.HEAD_GESTURES,
                PodFeature.ADAPTIVE_AUDIO, PodFeature.SLEEP_DETECTION, PodFeature.ADAPTIVE_VOLUME, PodFeature.STEM_CONFIG,
            ),
        ),
        PodModelSpec(
            modelNumbers = setOf("A3531", "A3532", "A3533"),
            name = "AirPods 5",
            budsRes = R.drawable.airpods_5_buds,
            leftRes = R.drawable.airpods_5_left,
            rightRes = R.drawable.airpods_5_right,
            caseRes = R.drawable.airpods_5_case,
            features = setOf(
                PodFeature.LISTENING_MODE, PodFeature.CONVERSATION_AWARENESS, PodFeature.HEAD_GESTURES,
                PodFeature.ADAPTIVE_AUDIO, PodFeature.SLEEP_DETECTION, PodFeature.ADAPTIVE_VOLUME, PodFeature.STEM_CONFIG,
            ),
        ),
        PodModelSpec(
            modelNumbers = setOf("A3439", "A3440", "A3441"),
            name = "AirPods 5 with Wireless Charging Case",
            budsRes = R.drawable.airpods_5_wireless_buds,
            leftRes = R.drawable.airpods_5_left,
            rightRes = R.drawable.airpods_5_right,
            caseRes = R.drawable.airpods_5_case,
            features = setOf(
                PodFeature.LISTENING_MODE, PodFeature.CONVERSATION_AWARENESS, PodFeature.HEAD_GESTURES,
                PodFeature.ADAPTIVE_AUDIO, PodFeature.SLEEP_DETECTION, PodFeature.ADAPTIVE_VOLUME,
                PodFeature.STEM_CONFIG, PodFeature.SWIPE_FOR_VOLUME,
            ),
        ),
        PodModelSpec(
            modelNumbers = setOf("A2084", "A2083"),
            name = "AirPods Pro 1",
            budsRes = R.drawable.airpods_pro_1_buds,
            leftRes = R.drawable.airpods_pro_1_left,
            rightRes = R.drawable.airpods_pro_1_right,
            caseRes = R.drawable.airpods_pro_1_case,
            features = setOf(PodFeature.LISTENING_MODE),
        ),
        PodModelSpec(
            modelNumbers = setOf("A2931", "A2699", "A2698"),
            name = "AirPods Pro 2 with Magsafe Charging Case (Lightning)",
            budsRes = R.drawable.airpods_pro_2_buds,
            leftRes = R.drawable.airpods_pro_2_left,
            rightRes = R.drawable.airpods_pro_2_right,
            caseRes = R.drawable.airpods_pro_2_case,
            features = setOf(
                PodFeature.LISTENING_MODE, PodFeature.CONVERSATION_AWARENESS, PodFeature.STEM_CONFIG,
                PodFeature.LOUD_SOUND_REDUCTION, PodFeature.SLEEP_DETECTION, PodFeature.HEARING_AID,
                PodFeature.ADAPTIVE_AUDIO, PodFeature.ADAPTIVE_VOLUME, PodFeature.SWIPE_FOR_VOLUME, PodFeature.HEAD_GESTURES,
            ),
        ),
        PodModelSpec(
            modelNumbers = setOf("A3047", "A3048", "A3049"),
            name = "AirPods Pro 2 with Magsafe Charging Case (USB-C)",
            budsRes = R.drawable.airpods_pro_2_buds,
            leftRes = R.drawable.airpods_pro_2_left,
            rightRes = R.drawable.airpods_pro_2_right,
            caseRes = R.drawable.airpods_pro_2_case,
            features = setOf(
                PodFeature.LISTENING_MODE, PodFeature.CONVERSATION_AWARENESS, PodFeature.STEM_CONFIG,
                PodFeature.LOUD_SOUND_REDUCTION, PodFeature.SLEEP_DETECTION, PodFeature.HEARING_AID,
                PodFeature.ADAPTIVE_AUDIO, PodFeature.ADAPTIVE_VOLUME, PodFeature.SWIPE_FOR_VOLUME, PodFeature.HEAD_GESTURES,
            ),
        ),
        PodModelSpec(
            modelNumbers = setOf("A3063", "A3064", "A3065"),
            name = "AirPods Pro 3",
            budsRes = R.drawable.airpods_pro_3_buds,
            leftRes = R.drawable.airpods_pro_3_left,
            rightRes = R.drawable.airpods_pro_3_right,
            caseRes = R.drawable.airpods_pro_3_case,
            features = setOf(
                PodFeature.LISTENING_MODE, PodFeature.CONVERSATION_AWARENESS, PodFeature.HEAD_GESTURES,
                PodFeature.STEM_CONFIG, PodFeature.LOUD_SOUND_REDUCTION, PodFeature.PPE,
                PodFeature.SLEEP_DETECTION, PodFeature.HEARING_AID, PodFeature.ADAPTIVE_AUDIO,
                PodFeature.ADAPTIVE_VOLUME, PodFeature.SWIPE_FOR_VOLUME, PodFeature.HRM,
            ),
        ),
    )

    private val byNumber: Map<String, PodModelSpec> = buildMap {
        models.forEach { spec -> spec.modelNumbers.forEach { put(it, spec) } }
    }

    fun findByModelNumber(modelNumber: String): PodModelSpec? = byNumber[modelNumber]
}
