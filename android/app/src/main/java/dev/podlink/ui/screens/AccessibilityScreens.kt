@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.podlink.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.podlink.R
import dev.podlink.core.HearingAidTuning
import dev.podlink.core.MotionUiState
import dev.podlink.core.PodFeature
import dev.podlink.core.PodUiState
import dev.podlink.core.TransparencyTuning
import dev.podlink.ui.components.ExpressiveLazyScaffold
import dev.podlink.ui.components.SegmentedActionRow
import dev.podlink.ui.components.SegmentedDiscreteStepRow
import dev.podlink.ui.components.SegmentedSliderRow
import dev.podlink.ui.components.SegmentedSwitchRow
import dev.podlink.ui.components.sectionGap
import dev.podlink.ui.components.sectionHeader
import dev.podlink.ui.theme.PodThemeTokens
import dev.podlink.ui.theme.tabular
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AccessibilityScreen(
    state: PodUiState,
    onSetDoubleClickInterval: (Int) -> Unit,
    onSetClickHoldInterval: (Int) -> Unit,
    onSetVolumeSwipeEnabled: (Boolean) -> Unit,
    onSetVolumeSwipeInterval: (Int) -> Unit,
    onSetChimeVolume: (Int) -> Unit,
    onSetInCaseTone: (Boolean) -> Unit,
    onSetSingleBudAnc: (Boolean) -> Unit,
    onSetPersonalizedVolume: (Boolean) -> Unit,
    onSetConversationAwareness: (Boolean) -> Unit,
    onOpenTransparency: () -> Unit,
    onOpenHearingProtection: () -> Unit,
    onOpenHearingAid: () -> Unit,
    onBack: () -> Unit,
) {
    val speedLabels = listOf(
        stringResource(R.string.default_option),
        stringResource(R.string.slower),
        stringResource(R.string.slowest),
    )
    val holdLabels = listOf(
        stringResource(R.string.default_option),
        stringResource(R.string.longer),
        stringResource(R.string.longest),
    )
    val features = state.capabilities
    val hasVolumeSwipe = PodFeature.SWIPE_FOR_VOLUME in features
    val hasPpe = PodFeature.PPE in features
    val hasHearingAid = PodFeature.HEARING_AID in features

    ExpressiveLazyScaffold(
        title = stringResource(R.string.accessibility),
        subtitle = stringResource(R.string.customizations),
        onBack = onBack,
    ) {
        sectionHeader("hdr_press", R.string.press_and_hold_airpods)

        item(key = "press_speed", contentType = "slider") {
            SegmentedDiscreteStepRow(
                index = 0,
                count = 2,
                title = stringResource(R.string.press_speed),
                subtitle = stringResource(R.string.press_speed_description),
                selectedStep = state.doubleClickInterval,
                stepLabels = speedLabels,
                onStepSelected = onSetDoubleClickInterval,
            )
        }
        item(key = "hold_dur", contentType = "slider") {
            SegmentedDiscreteStepRow(
                index = 1,
                count = 2,
                title = stringResource(R.string.press_and_hold_duration),
                subtitle = stringResource(R.string.press_and_hold_duration_description),
                selectedStep = state.clickHoldInterval,
                stepLabels = holdLabels,
                onStepSelected = onSetClickHoldInterval,
            )
        }

        if (hasVolumeSwipe) {
            sectionGap("gap_vol")
            sectionHeader("hdr_vol", R.string.volume_control)
            item(key = "swipe_toggle", contentType = "switch") {
                SegmentedSwitchRow(
                    index = 0,
                    count = 2,
                    title = stringResource(R.string.volume_control),
                    subtitle = stringResource(R.string.volume_control_description),
                    checked = state.volumeSwipeEnabled,
                    onCheckedChange = onSetVolumeSwipeEnabled,
                )
            }
            item(key = "swipe_speed", contentType = "slider") {
                SegmentedDiscreteStepRow(
                    index = 1,
                    count = 2,
                    title = stringResource(R.string.volume_swipe_speed),
                    subtitle = stringResource(R.string.volume_swipe_speed_description),
                    selectedStep = state.volumeSwipeInterval,
                    stepLabels = holdLabels,
                    enabled = state.volumeSwipeEnabled,
                    onStepSelected = onSetVolumeSwipeInterval,
                )
            }
        }

        sectionGap("gap_tones")
        sectionHeader("hdr_tones", R.string.tone_volume)
        item(key = "chime_vol", contentType = "slider") {
            SegmentedSliderRow(
                index = 0,
                count = 3,
                title = stringResource(R.string.tone_volume),
                valueLabel = "${state.chimeVolume}%",
                value = state.chimeVolume.toFloat(),
                onValueChange = { onSetChimeVolume(it.roundToInt()) },
                valueRange = 0f..100f,
                startLabel = "0%",
                endLabel = "100%",
            )
        }
        item(key = "case_tone", contentType = "switch") {
            SegmentedSwitchRow(
                index = 1,
                count = 3,
                title = stringResource(R.string.remake_in_case_tone),
                subtitle = stringResource(R.string.remake_in_case_tone_desc),
                checked = state.inCaseToneEnabled,
                onCheckedChange = onSetInCaseTone,
            )
        }
        item(key = "single_anc", contentType = "switch") {
            SegmentedSwitchRow(
                index = 2,
                count = 3,
                title = stringResource(R.string.noise_cancellation_single_airpod),
                subtitle = stringResource(R.string.noise_cancellation_single_airpod_description),
                checked = state.singleBudAncEnabled,
                onCheckedChange = onSetSingleBudAnc,
            )
        }

        sectionGap("gap_a11y_audio")
        sectionHeader("hdr_a11y_audio", R.string.hearing_health)
        val hhRows = 3 + (if (hasPpe) 1 else 0) + (if (hasHearingAid) 1 else 0)
        var rIdx = 0

        val pvolIdx = rIdx++
        item(key = "a11y_pvol", contentType = "switch") {
            SegmentedSwitchRow(
                index = pvolIdx,
                count = hhRows,
                title = stringResource(R.string.personalized_volume),
                subtitle = stringResource(R.string.personalized_volume_description),
                leadingIcon = Icons.AutoMirrored.Rounded.VolumeUp,
                checked = state.personalizedVolumeEnabled,
                onCheckedChange = onSetPersonalizedVolume,
            )
        }
        val caIdx = rIdx++
        item(key = "a11y_ca", contentType = "switch") {
            SegmentedSwitchRow(
                index = caIdx,
                count = hhRows,
                title = stringResource(R.string.conversation_awareness),
                subtitle = stringResource(R.string.conversation_awareness_description),
                leadingIcon = Icons.Rounded.RecordVoiceOver,
                checked = state.conversationAwarenessEnabled,
                onCheckedChange = onSetConversationAwareness,
            )
        }
        val transpIdx = rIdx++
        item(key = "a11y_transp", contentType = "action") {
            SegmentedActionRow(
                index = transpIdx,
                count = hhRows,
                title = stringResource(R.string.transparency_mode),
                subtitle = stringResource(R.string.customize_transparency_mode_description),
                leadingIcon = Icons.Rounded.Tune,
                enabled = state.attAvailable,
                onClick = onOpenTransparency,
            )
        }
        if (hasPpe) {
            val i = rIdx++
            item(key = "a11y_ppe", contentType = "action") {
                SegmentedActionRow(
                    index = i,
                    count = hhRows,
                    title = stringResource(R.string.hearing_protection),
                    leadingIcon = Icons.Rounded.Shield,
                    onClick = onOpenHearingProtection,
                )
            }
        }
        if (hasHearingAid) {
            val i = rIdx
            item(key = "a11y_aid", contentType = "action") {
                SegmentedActionRow(
                    index = i,
                    count = hhRows,
                    title = stringResource(R.string.hearing_aid),
                    leadingIcon = Icons.Rounded.Hearing,
                    onClick = onOpenHearingAid,
                )
            }
        }

        if (!state.attAvailable) {
            sectionGap("gap_att_warn")
            item(key = "att_warn", contentType = "notice") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = PodThemeTokens.cardInsetContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.remake_att_unavailable_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun TransparencyScreen(
    state: PodUiState,
    onUpdateTransparency: (TransparencyTuning) -> Unit,
    onBack: () -> Unit,
) {
    val t = state.transparency
    ExpressiveLazyScaffold(
        title = stringResource(R.string.customize_transparency_mode),
        subtitle = stringResource(R.string.transparency_mode),
        onBack = onBack,
    ) {
        if (t == null) {
            item(key = "transp_unavailable", contentType = "notice") {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = PodThemeTokens.cardContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.remake_att_unavailable_notice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
            return@ExpressiveLazyScaffold
        }

        val hasOwnVoice = t.ownVoiceAmplification != null
        val rows = 6 + (if (hasOwnVoice) 1 else 0)
        item(key = "tr_enable", contentType = "switch") {
            SegmentedSwitchRow(
                index = 0,
                count = rows,
                title = stringResource(R.string.customize_transparency_mode),
                checked = t.enabled,
                onCheckedChange = { onUpdateTransparency(t.copy(enabled = it)) },
            )
        }
        item(key = "tr_amp", contentType = "slider") {
            SegmentedSliderRow(
                index = 1,
                count = rows,
                title = stringResource(R.string.amplification),
                valueLabel = "${((t.netAmplification + 1f) * 50f).roundToInt()}%",
                value = t.netAmplification,
                onValueChange = { amp ->
                    val halfBal = t.balance / 2f
                    onUpdateTransparency(
                        t.copy(
                            netAmplification = amp,
                            leftAmplification = (amp - halfBal).coerceIn(-1f, 1f),
                            rightAmplification = (amp + halfBal).coerceIn(-1f, 1f),
                        ),
                    )
                },
                valueRange = -1f..1f,
                enabled = t.enabled,
                startLabel = stringResource(R.string.less),
                endLabel = stringResource(R.string.more),
            )
        }
        item(key = "tr_bal", contentType = "slider") {
            SegmentedSliderRow(
                index = 2,
                count = rows,
                title = stringResource(R.string.balance),
                valueLabel = "${(t.balance * 100f).roundToInt()}",
                value = t.balance,
                onValueChange = { bal ->
                    val halfBal = bal / 2f
                    onUpdateTransparency(
                        t.copy(
                            balance = bal,
                            leftAmplification = (t.netAmplification - halfBal).coerceIn(-1f, 1f),
                            rightAmplification = (t.netAmplification + halfBal).coerceIn(-1f, 1f),
                        ),
                    )
                },
                valueRange = -1f..1f,
                enabled = t.enabled,
                startLabel = stringResource(R.string.left),
                endLabel = stringResource(R.string.right),
            )
        }
        item(key = "tr_tone", contentType = "slider") {
            SegmentedSliderRow(
                index = 3,
                count = rows,
                title = stringResource(R.string.tone),
                valueLabel = "${((t.leftTone + 1f) * 50f).roundToInt()}%",
                value = t.leftTone,
                onValueChange = { tone ->
                    onUpdateTransparency(t.copy(leftTone = tone, rightTone = tone))
                },
                valueRange = -1f..1f,
                enabled = t.enabled,
                startLabel = stringResource(R.string.darker),
                endLabel = stringResource(R.string.brighter),
            )
        }
        item(key = "tr_anr", contentType = "slider") {
            SegmentedSliderRow(
                index = 4,
                count = rows,
                title = stringResource(R.string.ambient_noise_reduction),
                valueLabel = "${(t.leftAmbientNoiseReduction * 100f).roundToInt()}%",
                value = t.leftAmbientNoiseReduction,
                onValueChange = { anr ->
                    onUpdateTransparency(t.copy(leftAmbientNoiseReduction = anr, rightAmbientNoiseReduction = anr))
                },
                valueRange = 0f..1f,
                enabled = t.enabled,
            )
        }
        item(key = "tr_boost", contentType = "switch") {
            SegmentedSwitchRow(
                index = 5,
                count = rows,
                title = stringResource(R.string.conversation_boost),
                subtitle = stringResource(R.string.conversation_boost_description),
                checked = t.leftConversationBoost && t.rightConversationBoost,
                enabled = t.enabled,
                onCheckedChange = { boost ->
                    onUpdateTransparency(t.copy(leftConversationBoost = boost, rightConversationBoost = boost))
                },
            )
        }
        if (hasOwnVoice) {
            item(key = "tr_own_voice", contentType = "slider") {
                SegmentedSliderRow(
                    index = 6,
                    count = rows,
                    title = stringResource(R.string.remake_own_voice_amplification),
                    valueLabel = "${(t.ownVoiceAmplification * 100f).roundToInt()}%",
                    value = t.ownVoiceAmplification,
                    onValueChange = { ova -> onUpdateTransparency(t.copy(ownVoiceAmplification = ova)) },
                    valueRange = 0f..1f,
                    enabled = t.enabled,
                )
            }
        }
    }
}

@Composable
fun HearingProtectionScreen(
    state: PodUiState,
    onSetPpeEnabled: (Boolean) -> Unit,
    onSetLoudSoundReduction: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val hasLoud = state.attAvailable && PodFeature.LOUD_SOUND_REDUCTION in state.capabilities
    val count = if (hasLoud) 2 else 1

    ExpressiveLazyScaffold(
        title = stringResource(R.string.hearing_protection),
        subtitle = stringResource(R.string.hearing_health),
        onBack = onBack,
    ) {
        item(key = "ppe_toggle", contentType = "switch") {
            SegmentedSwitchRow(
                index = 0,
                count = count,
                title = stringResource(R.string.hearing_protection),
                subtitle = stringResource(R.string.environmental_noise),
                checked = state.ppeEnabled,
                onCheckedChange = onSetPpeEnabled,
            )
        }
        if (hasLoud) {
            item(key = "ppe_lsr", contentType = "switch") {
                SegmentedSwitchRow(
                    index = 1,
                    count = count,
                    title = stringResource(R.string.loud_sound_reduction),
                    subtitle = stringResource(R.string.loud_sound_reduction_description),
                    checked = state.loudSoundReductionEnabled,
                    onCheckedChange = onSetLoudSoundReduction,
                )
            }
        }
    }
}

@Composable
fun HearingAidScreen(
    state: PodUiState,
    onSetHearingAidEnrolled: (Boolean) -> Unit,
    onSetHearingAidEnabled: (Boolean) -> Unit,
    onUpdateHearingAid: (HearingAidTuning) -> Unit,
    onBack: () -> Unit,
) {
    val tuning = state.hearingAid
    val hasTuning = state.attAvailable && tuning != null
    val total = if (hasTuning) 6 else 2

    ExpressiveLazyScaffold(
        title = stringResource(R.string.hearing_aid),
        subtitle = stringResource(R.string.hearing_aid_description),
        onBack = onBack,
    ) {
        item(key = "ha_enrolled", contentType = "switch") {
            SegmentedSwitchRow(
                index = 0,
                count = total,
                title = stringResource(R.string.hearing_test),
                checked = state.hearingAidEnrolled,
                onCheckedChange = onSetHearingAidEnrolled,
            )
        }
        item(key = "ha_enabled", contentType = "switch") {
            SegmentedSwitchRow(
                index = 1,
                count = total,
                title = stringResource(R.string.hearing_aid),
                subtitle = stringResource(R.string.hearing_aid_description),
                checked = state.hearingAidEnabled,
                enabled = state.hearingAidEnrolled,
                onCheckedChange = onSetHearingAidEnabled,
            )
        }
        if (hasTuning) {
            val t = tuning
            item(key = "ha_amp", contentType = "slider") {
                SegmentedSliderRow(
                    index = 2,
                    count = total,
                    title = stringResource(R.string.amplification),
                    valueLabel = "${((t.netAmplification + 1f) * 50f).roundToInt()}%",
                    value = t.netAmplification,
                    onValueChange = { amp ->
                        val hb = t.balance / 2f
                        onUpdateHearingAid(
                            t.copy(
                                netAmplification = amp,
                                leftAmplification = (amp - hb).coerceIn(-1f, 1f),
                                rightAmplification = (amp + hb).coerceIn(-1f, 1f),
                            ),
                        )
                    },
                    valueRange = -1f..1f,
                )
            }
            item(key = "ha_bal", contentType = "slider") {
                SegmentedSliderRow(
                    index = 3,
                    count = total,
                    title = stringResource(R.string.balance),
                    valueLabel = "${(t.balance * 100f).roundToInt()}",
                    value = t.balance,
                    onValueChange = { bal ->
                        val hb = bal / 2f
                        onUpdateHearingAid(
                            t.copy(
                                balance = bal,
                                leftAmplification = (t.netAmplification - hb).coerceIn(-1f, 1f),
                                rightAmplification = (t.netAmplification + hb).coerceIn(-1f, 1f),
                            ),
                        )
                    },
                    valueRange = -1f..1f,
                    startLabel = stringResource(R.string.left),
                    endLabel = stringResource(R.string.right),
                )
            }
            item(key = "ha_tone", contentType = "slider") {
                SegmentedSliderRow(
                    index = 4,
                    count = total,
                    title = stringResource(R.string.tone),
                    valueLabel = "${((t.leftTone + 1f) * 50f).roundToInt()}%",
                    value = t.leftTone,
                    onValueChange = { tone -> onUpdateHearingAid(t.copy(leftTone = tone, rightTone = tone)) },
                    valueRange = -1f..1f,
                )
            }
            item(key = "ha_boost", contentType = "switch") {
                SegmentedSwitchRow(
                    index = 5,
                    count = total,
                    title = stringResource(R.string.conversation_boost),
                    checked = t.leftConversationBoost && t.rightConversationBoost,
                    onCheckedChange = { b -> onUpdateHearingAid(t.copy(leftConversationBoost = b, rightConversationBoost = b)) },
                )
            }
        }
    }
}

@Composable
fun HeadTrackingScreen(
    state: PodUiState,
    motionFlow: StateFlow<MotionUiState>,
    onSetHeadGesturesEnabled: (Boolean) -> Unit,
    onStartHeadTracking: () -> Unit,
    onStopHeadTracking: () -> Unit,
    onResetOrientation: () -> Unit,
    onStartGestureTest: () -> Unit,
    onStopGestureTest: () -> Unit,
    onBack: () -> Unit,
) {
    val activeTracking by rememberUpdatedState(state.headTrackingActive)
    val activeGestureTest by rememberUpdatedState(state.headGestureTesting)
    val stopTrackingCallback by rememberUpdatedState(onStopHeadTracking)
    val stopGestureCallback by rememberUpdatedState(onStopGestureTest)
    DisposableEffect(Unit) {
        onDispose {
            if (activeGestureTest) stopGestureCallback()
            if (activeTracking) stopTrackingCallback()
        }
    }

    val motion by motionFlow.collectAsStateWithLifecycle()

    ExpressiveLazyScaffold(
        title = stringResource(R.string.head_gestures),
        subtitle = stringResource(R.string.head_tracking),
        onBack = onBack,
    ) {
        item(key = "ht_toggle", contentType = "switch") {
            SegmentedSwitchRow(
                index = 0,
                count = 1,
                title = stringResource(R.string.head_gestures),
                subtitle = stringResource(R.string.head_gestures_details),
                leadingIcon = Icons.Rounded.AccessibilityNew,
                checked = state.prefs.headGesturesEnabled,
                onCheckedChange = onSetHeadGesturesEnabled,
            )
        }

        sectionGap("gap_ht_live")
        item(key = "ht_card", contentType = "ht_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = PodThemeTokens.cardContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.remake_head_velocity),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                    )

                    MotionRadar(
                        motion = motion,
                        active = state.headTrackingActive || state.headGestureTesting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${stringResource(R.string.remake_head_horizontal)}: ${motion.yaw.roundToInt()}°",
                            style = MaterialTheme.typography.labelLarge.tabular,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${stringResource(R.string.remake_head_vertical)}: ${motion.pitch.roundToInt()}°",
                            style = MaterialTheme.typography.labelLarge.tabular,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    state.lastDetectedGesture?.let { nodded ->
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(
                                    if (nodded) R.string.remake_head_detected_yes else R.string.remake_head_detected_no,
                                ),
                                style = MaterialTheme.typography.labelLargeEmphasized,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = if (state.headGestureTesting) onStopGestureTest else onStartGestureTest,
                            modifier = Modifier.weight(1f),
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Icon(
                                imageVector = if (state.headGestureTesting) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                            Text(
                                text = stringResource(
                                    if (state.headGestureTesting) R.string.remake_head_test_stop else R.string.remake_head_test_start,
                                ),
                            )
                        }

                        FilledTonalButton(
                            onClick = if (state.headTrackingActive) onStopHeadTracking else onStartHeadTracking,
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(
                                text = stringResource(
                                    if (state.headTrackingActive) R.string.remake_head_test_stop else R.string.head_tracking,
                                ),
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onResetOrientation,
                        modifier = Modifier.fillMaxWidth(),
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                        Text(text = stringResource(R.string.reset))
                    }
                }
            }
        }
    }
}

@Composable
private fun MotionRadar(
    motion: MotionUiState,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val ringColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Surface(
        shape = MaterialTheme.shapes.large,
        color = PodThemeTokens.cardInsetContainer,
        modifier = modifier,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawLine(ringColor, Offset(cx, 12f), Offset(cx, size.height - 12f), strokeWidth = 1.dp.toPx())
            drawLine(ringColor, Offset(12f, cy), Offset(size.width - 12f, cy), strokeWidth = 1.dp.toPx())

            val normX = (motion.yaw / 45f).coerceIn(-1f, 1f)
            val normY = (-motion.pitch / 45f).coerceIn(-1f, 1f)
            val dotX = cx + normX * (cx - 18.dp.toPx())
            val dotY = cy + normY * (cy - 18.dp.toPx())
            drawCircle(
                color = dotColor.copy(alpha = 0.22f),
                radius = 18.dp.toPx(),
                center = Offset(dotX, dotY),
            )
            drawCircle(
                color = dotColor,
                radius = 9.dp.toPx(),
                center = Offset(dotX, dotY),
            )
        }
    }
}
