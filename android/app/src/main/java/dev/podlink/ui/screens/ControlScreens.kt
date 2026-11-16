@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.podlink.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.podlink.R
import dev.podlink.core.ListeningMode
import dev.podlink.core.PodFeature
import dev.podlink.core.PodUiState
import dev.podlink.core.StemAction
import dev.podlink.core.StemPress
import dev.podlink.core.StemSide
import dev.podlink.ui.components.ExpressiveLazyScaffold
import dev.podlink.ui.components.SegmentedActionRow
import dev.podlink.ui.components.SegmentedCardGroup
import dev.podlink.ui.components.SegmentedSelectableRow
import dev.podlink.ui.components.sectionGap
import dev.podlink.ui.components.sectionHeader
import dev.podlink.ui.components.segmentedShape
import dev.podlink.ui.theme.PodThemeTokens

private data class CycleModeInfo(
    val mode: ListeningMode,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descRes: Int,
)

private val CycleModes = listOf(
    CycleModeInfo(ListeningMode.OFF, R.string.off, R.string.listening_mode_off_description),
    CycleModeInfo(ListeningMode.TRANSPARENCY, R.string.transparency, R.string.listening_mode_transparency_description),
    CycleModeInfo(ListeningMode.ADAPTIVE, R.string.adaptive, R.string.listening_mode_adaptive_description),
    CycleModeInfo(ListeningMode.NOISE_CANCELLATION, R.string.noise_cancellation, R.string.listening_mode_noise_cancellation_description),
)

@StringRes
private fun StemPress.titleRes(): Int = when (this) {
    StemPress.SINGLE_PRESS -> R.string.remake_single_press
    StemPress.DOUBLE_PRESS -> R.string.remake_double_press
    StemPress.TRIPLE_PRESS -> R.string.remake_triple_press
    StemPress.LONG_PRESS -> R.string.remake_long_press
}

@Composable
fun StemControlsScreen(
    state: PodUiState,
    onToggleCycleBit: (ListeningMode) -> Unit,
    onSetStemAction: (StemPress, Boolean, StemAction) -> Unit,
    onBack: () -> Unit,
) {
    var selectedSide by remember { mutableStateOf(StemSide.LEFT) }
    var editingPress by remember { mutableStateOf<StemPress?>(null) }

    val mask = state.listeningModeMask
    val selectedCycleCount = ListeningMode.entries.count { (mask and it.cycleMask) != 0 }
    val supportsAdaptive = PodFeature.ADAPTIVE_AUDIO in state.capabilities
    val visibleCycleModes = CycleModes.filter { it.mode != ListeningMode.ADAPTIVE || supportsAdaptive }
    val mapping = state.prefs.stemMapping

    ExpressiveLazyScaffold(
        title = stringResource(R.string.remake_stem_controls_title),
        subtitle = stringResource(R.string.remake_stem_controls_subtitle),
        onBack = onBack,
    ) {
        sectionHeader("hdr_cycle", R.string.remake_long_press_cycles_title)

        visibleCycleModes.forEachIndexed { idx, info ->
            val checked = (mask and info.mode.cycleMask) != 0
            val canToggle = !checked || selectedCycleCount > 2
            item(key = "cycle_${info.mode.name}", contentType = "checkbox") {
                val shape = segmentedShape(idx, visibleCycleModes.size)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    color = PodThemeTokens.rowContainer,
                    onClick = { if (canToggle) onToggleCycleBit(info.mode) },
                    enabled = canToggle,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stringResource(info.titleRes),
                                style = MaterialTheme.typography.bodyLargeEmphasized,
                            )
                            Text(
                                text = stringResource(info.descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null,
                            enabled = canToggle,
                        )
                    }
                }
            }
        }

        item(key = "cycle_hint", contentType = "hint") {
            Text(
                text = stringResource(R.string.remake_long_press_min_two),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }

        sectionGap("gap_stem_actions")
        sectionHeader("hdr_stem_actions", R.string.remake_stem_actions_title)

        item(key = "bud_picker", contentType = "bud_picker") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                ToggleButton(
                    checked = selectedSide == StemSide.LEFT,
                    onCheckedChange = { if (it) selectedSide = StemSide.LEFT },
                    modifier = Modifier.weight(1f),
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                ) {
                    Text(text = stringResource(R.string.left))
                }
                ToggleButton(
                    checked = selectedSide == StemSide.RIGHT,
                    onCheckedChange = { if (it) selectedSide = StemSide.RIGHT },
                    modifier = Modifier.weight(1f),
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                ) {
                    Text(text = stringResource(R.string.right))
                }
            }
        }

        val presses = StemPress.entries
        presses.forEachIndexed { idx, press ->
            item(key = "stem_${press.name}", contentType = "action") {
                val currentAction = mapping.actionFor(press, selectedSide)
                SegmentedActionRow(
                    index = idx,
                    count = presses.size,
                    title = stringResource(press.titleRes()),
                    trailingText = stringResource(currentAction.labelRes),
                    onClick = { editingPress = press },
                )
            }
        }
    }

    editingPress?.let { targetPress ->
        val sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Expanded,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
        val currentAction = mapping.actionFor(targetPress, selectedSide)
        ModalBottomSheet(
            onDismissRequest = { editingPress = null },
            sheetState = sheetState,
            containerColor = PodThemeTokens.cardContainer,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "${stringResource(if (selectedSide == StemSide.LEFT) R.string.left else R.string.right)} · ${stringResource(targetPress.titleRes())}",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                )
                SegmentedCardGroup {
                    StemAction.entries.forEachIndexed { idx, action ->
                        SegmentedSelectableRow(
                            index = idx,
                            count = StemAction.entries.size,
                            title = stringResource(action.labelRes),
                            selected = action == currentAction,
                            onClick = {
                                onSetStemAction(targetPress, selectedSide == StemSide.LEFT, action)
                                editingPress = null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallControlsScreen(
    state: PodUiState,
    onSetCallManagementFlipped: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val flipped = state.callManagementFlipped
    val muteHeader = "${stringResource(R.string.mute_unmute)} / ${stringResource(R.string.hang_up)}"

    ExpressiveLazyScaffold(
        title = stringResource(R.string.call_controls),
        subtitle = stringResource(R.string.remake_call_controls_subtitle),
        onBack = onBack,
    ) {
        sectionHeader("hdr_answer", R.string.answer_call)
        item(key = "answer_normal", contentType = "radio") {
            SegmentedSelectableRow(
                index = 0,
                count = 2,
                title = stringResource(R.string.press_once),
                selected = !flipped,
                onClick = { onSetCallManagementFlipped(false) },
            )
        }
        item(key = "answer_flipped", contentType = "radio") {
            SegmentedSelectableRow(
                index = 1,
                count = 2,
                title = stringResource(R.string.press_twice),
                selected = flipped,
                onClick = { onSetCallManagementFlipped(true) },
            )
        }

        sectionGap("gap_mute")
        sectionHeader("hdr_mute", muteHeader)
        item(key = "mute_summary", contentType = "radio") {
            SegmentedSelectableRow(
                index = 0,
                count = 2,
                title = "${stringResource(R.string.mute_unmute)}: ${stringResource(if (flipped) R.string.press_twice else R.string.press_once)}",
                subtitle = "${stringResource(R.string.hang_up)}: ${stringResource(if (flipped) R.string.press_once else R.string.press_twice)}",
                selected = !flipped,
                onClick = { onSetCallManagementFlipped(false) },
            )
        }
        item(key = "hangup_summary", contentType = "radio") {
            SegmentedSelectableRow(
                index = 1,
                count = 2,
                title = "${stringResource(R.string.mute_unmute)}: ${stringResource(R.string.press_twice)}",
                subtitle = "${stringResource(R.string.hang_up)}: ${stringResource(R.string.press_once)}",
                selected = flipped,
                onClick = { onSetCallManagementFlipped(true) },
            )
        }
    }
}

@Composable
fun MicrophoneScreen(
    state: PodUiState,
    onSetMicMode: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val options = listOf(
        0 to R.string.microphone_automatic,
        1 to R.string.microphone_always_right,
        2 to R.string.microphone_always_left,
    )

    ExpressiveLazyScaffold(
        title = stringResource(R.string.microphone),
        subtitle = stringResource(R.string.microphone_mode),
        onBack = onBack,
    ) {
        options.forEachIndexed { idx, (mode, titleRes) ->
            item(key = "mic_$mode", contentType = "radio") {
                SegmentedSelectableRow(
                    index = idx,
                    count = options.size,
                    title = stringResource(titleRes),
                    selected = state.micMode == mode,
                    onClick = { onSetMicMode(mode) },
                )
            }
        }
    }
}
