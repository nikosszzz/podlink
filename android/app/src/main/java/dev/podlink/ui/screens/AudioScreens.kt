@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.podlink.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.podlink.R
import dev.podlink.core.Accommodations
import dev.podlink.core.EqCurve
import dev.podlink.core.PodUiState
import dev.podlink.ui.components.ExpressiveLazyScaffold
import dev.podlink.ui.components.SegmentedSliderRow
import dev.podlink.ui.components.SegmentedSwitchRow
import dev.podlink.ui.components.sectionGap
import dev.podlink.ui.components.sectionHeader
import dev.podlink.ui.theme.PodThemeTokens
import kotlin.math.roundToInt

private data class EqPreset(val name: String, val low: Int, val mid: Int, val high: Int)

private val EqPresets = listOf(
    EqPreset("Flat", 50, 50, 50),
    EqPreset("Bass", 78, 52, 46),
    EqPreset("Vocal", 42, 74, 58),
    EqPreset("Treble", 46, 52, 78),
    EqPreset("Warm", 68, 58, 40),
)

@Composable
fun EqualizerScreen(
    state: PodUiState,
    onSetCustomEq: (EqCurve) -> Unit,
    onSetAccommodations: (Accommodations) -> Unit,
    onBack: () -> Unit,
) {
    val curve = state.customEq
    val accomm = state.accommodations

    ExpressiveLazyScaffold(
        title = stringResource(R.string.equalizer),
        subtitle = stringResource(R.string.remake_equalizer_subtitle),
        onBack = onBack,
    ) {
        item(key = "eq_curve_card", contentType = "card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = PodThemeTokens.cardContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.remake_equalizer_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    ) {
                        ToggleButton(
                            checked = !curve.isEnabled,
                            onCheckedChange = {
                                if (it) onSetCustomEq(curve.copy(state = EqCurve.RECOMMENDED))
                            },
                            modifier = Modifier.weight(1f),
                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        ) {
                            Text(text = stringResource(R.string.recommended))
                        }
                        ToggleButton(
                            checked = curve.isEnabled,
                            onCheckedChange = {
                                if (it) onSetCustomEq(curve.copy(state = EqCurve.CUSTOM))
                            },
                            modifier = Modifier.weight(1f),
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        ) {
                            Text(text = stringResource(R.string.custom))
                        }
                    }

                    EqCurvePreview(
                        low = if (curve.isEnabled) curve.low else EqCurve.NEUTRAL,
                        mid = if (curve.isEnabled) curve.mid else EqCurve.NEUTRAL,
                        high = if (curve.isEnabled) curve.high else EqCurve.NEUTRAL,
                        active = curve.isEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        EqPresets.forEachIndexed { idx, preset ->
                            val matches = curve.isEnabled &&
                                curve.low == preset.low &&
                                curve.mid == preset.mid &&
                                curve.high == preset.high
                            val shapes = when (idx) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                EqPresets.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                            ToggleButton(
                                checked = matches,
                                onCheckedChange = {
                                    onSetCustomEq(
                                        EqCurve(
                                            state = EqCurve.CUSTOM,
                                            low = preset.low,
                                            mid = preset.mid,
                                            high = preset.high,
                                        ),
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shapes = shapes,
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.labelSmallEmphasized,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        sectionGap("gap_bands")
        sectionHeader("hdr_bands", R.string.custom)

        item(key = "band_low", contentType = "slider") {
            SegmentedSliderRow(
                index = 0,
                count = 3,
                title = stringResource(R.string.eq_low),
                valueLabel = formatEqDb(curve.low),
                value = curve.low.toFloat(),
                onValueChange = {
                    onSetCustomEq(curve.copy(state = EqCurve.CUSTOM, low = it.roundToInt().coerceIn(0, 100)))
                },
                valueRange = 0f..100f,
                enabled = curve.isEnabled,
                startLabel = "-6 dB",
                centerLabel = "0 dB",
                endLabel = "+6 dB",
            )
        }
        item(key = "band_mid", contentType = "slider") {
            SegmentedSliderRow(
                index = 1,
                count = 3,
                title = stringResource(R.string.eq_mid),
                valueLabel = formatEqDb(curve.mid),
                value = curve.mid.toFloat(),
                onValueChange = {
                    onSetCustomEq(curve.copy(state = EqCurve.CUSTOM, mid = it.roundToInt().coerceIn(0, 100)))
                },
                valueRange = 0f..100f,
                enabled = curve.isEnabled,
                startLabel = "-6 dB",
                centerLabel = "0 dB",
                endLabel = "+6 dB",
            )
        }
        item(key = "band_high", contentType = "slider") {
            SegmentedSliderRow(
                index = 2,
                count = 3,
                title = stringResource(R.string.eq_high),
                valueLabel = formatEqDb(curve.high),
                value = curve.high.toFloat(),
                onValueChange = {
                    onSetCustomEq(curve.copy(state = EqCurve.CUSTOM, high = it.roundToInt().coerceIn(0, 100)))
                },
                valueRange = 0f..100f,
                enabled = curve.isEnabled,
                startLabel = "-6 dB",
                centerLabel = "0 dB",
                endLabel = "+6 dB",
            )
        }

        sectionGap("gap_accomm")
        sectionHeader("hdr_accomm", R.string.remake_headphone_accommodation)

        val bandCount = accomm.bands.size
        val accommCount = 2 + bandCount
        item(key = "accomm_media", contentType = "switch") {
            SegmentedSwitchRow(
                index = 0,
                count = accommCount,
                title = stringResource(R.string.adjust_media),
                subtitle = stringResource(R.string.apply_eq_to),
                checked = accomm.onMedia,
                onCheckedChange = { onSetAccommodations(accomm.copy(onMedia = it)) },
            )
        }
        item(key = "accomm_phone", contentType = "switch") {
            SegmentedSwitchRow(
                index = 1,
                count = accommCount,
                title = stringResource(R.string.adjust_calls),
                checked = accomm.onPhone,
                onCheckedChange = { onSetAccommodations(accomm.copy(onPhone = it)) },
            )
        }

        val accommLabels = listOf("250 Hz", "500 Hz", "1 kHz", "2 kHz", "3 kHz", "4 kHz", "6 kHz", "8 kHz")
        for (idx in 0 until bandCount) {
            item(key = "accomm_band_$idx", contentType = "slider") {
                val freq = accommLabels.getOrElse(idx) { stringResource(R.string.band_label, idx + 1) }
                val value = accomm.bands[idx]
                SegmentedSliderRow(
                    index = 2 + idx,
                    count = accommCount,
                    title = freq,
                    valueLabel = "${value.roundToInt()} dB",
                    value = value,
                    onValueChange = { updated ->
                        val next = accomm.bands.copyOf()
                        next[idx] = updated
                        onSetAccommodations(accomm.copy(bands = next))
                    },
                    valueRange = 0f..50f,
                    startLabel = "0 dB",
                    endLabel = "+50 dB",
                )
            }
        }

        sectionGap("gap_eq_reset")
        item(key = "eq_reset", contentType = "button") {
            OutlinedButton(
                onClick = {
                    onSetCustomEq(EqCurve())
                    onSetAccommodations(Accommodations())
                },
                modifier = Modifier.fillMaxWidth(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.reset))
            }
        }
    }
}

private fun formatEqDb(value: Int): String {
    val db = ((value - 50) / 50f) * 6f
    return if (db >= 0f) "+${(db * 10).roundToInt() / 10f} dB" else "${(db * 10).roundToInt() / 10f} dB"
}

@Composable
private fun EqCurvePreview(
    low: Int,
    mid: Int,
    high: Int,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val lineColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val density = LocalDensity.current
    val fillBrush = remember(lineColor) {
        Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.24f), lineColor.copy(alpha = 0.0f)))
    }
    val lineStroke = remember(density) {
        Stroke(width = with(density) { 3.dp.toPx() }, cap = StrokeCap.Round)
    }
    val curve = remember { Path() }
    val fill = remember { Path() }

    Canvas(modifier = modifier.padding(vertical = 4.dp)) {
        val w = size.width
        val h = size.height
        val midY = h / 2f
        drawLine(
            color = gridColor,
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.dp.toPx(),
        )

        val p0Y = h - (low / 100f) * h
        val p1Y = h - (mid / 100f) * h
        val p2Y = h - (high / 100f) * h

        curve.reset()
        curve.moveTo(0f, p0Y)
        curve.cubicTo(w * 0.25f, p0Y, w * 0.25f, p1Y, w * 0.5f, p1Y)
        curve.cubicTo(w * 0.75f, p1Y, w * 0.75f, p2Y, w, p2Y)

        fill.reset()
        fill.addPath(curve)
        fill.lineTo(w, h)
        fill.lineTo(0f, h)
        fill.close()

        drawPath(path = fill, brush = fillBrush)
        drawPath(path = curve, color = lineColor, style = lineStroke)
    }
}

@Composable
fun AdaptiveStrengthScreen(
    state: PodUiState,
    onSetAdaptiveAncStrength: (Int) -> Unit,
    onBack: () -> Unit,
) {
    ExpressiveLazyScaffold(
        title = stringResource(R.string.adaptive_audio),
        subtitle = stringResource(R.string.adaptive_audio_description),
        onBack = onBack,
    ) {
        item(key = "adaptive_slider", contentType = "slider") {
            SegmentedSliderRow(
                index = 0,
                count = 1,
                title = stringResource(R.string.customize_adaptive_audio),
                valueLabel = "${state.adaptiveAncStrength}%",
                value = state.adaptiveAncStrength.toFloat(),
                onValueChange = { onSetAdaptiveAncStrength(it.roundToInt()) },
                valueRange = 0f..100f,
                startLabel = stringResource(R.string.remake_adaptive_less_noise),
                centerLabel = stringResource(R.string.remake_adaptive_default),
                endLabel = stringResource(R.string.remake_adaptive_more_noise),
            )
        }
    }
}
