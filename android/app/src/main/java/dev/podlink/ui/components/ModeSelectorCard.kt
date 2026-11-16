@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.podlink.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.podlink.R
import dev.podlink.core.ListeningMode
import dev.podlink.core.labelRes
import dev.podlink.ui.theme.PodThemeTokens

private data class ModeOption(
    val mode: ListeningMode,
    @param:DrawableRes val iconRes: Int?,
)

private val AllModeOptions = listOf(
    ModeOption(ListeningMode.OFF, null),
    ModeOption(ListeningMode.TRANSPARENCY, R.drawable.transparency),
    ModeOption(ListeningMode.ADAPTIVE, R.drawable.adaptive),
    ModeOption(ListeningMode.NOISE_CANCELLATION, R.drawable.noise_cancellation),
)

@Composable
fun ModeSelectorCard(
    selectedMode: ListeningMode,
    showOffOption: Boolean,
    showAdaptiveOption: Boolean,
    enabled: Boolean,
    onModeSelected: (ListeningMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = remember(showOffOption, showAdaptiveOption) {
        AllModeOptions.filter { option ->
            when (option.mode) {
                ListeningMode.OFF -> showOffOption
                ListeningMode.ADAPTIVE -> showAdaptiveOption
                else -> true
            }
        }
    }
    val buttonColors = ToggleButtonDefaults.colors(
        containerColor = PodThemeTokens.cardInsetContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = PodThemeTokens.cardContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.listening_mode),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                options.forEachIndexed { idx, opt ->
                    val checked = selectedMode == opt.mode
                    val shapes = when {
                        options.size == 1 -> ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.MinHeight)
                        idx == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        idx == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                    ToggleButton(
                        checked = checked,
                        onCheckedChange = { if (it || !checked) onModeSelected(opt.mode) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        shapes = shapes,
                        colors = buttonColors,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 6.dp),
                        ) {
                            if (opt.iconRes != null) {
                                Image(
                                    painter = painterResource(opt.iconRes),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.tint(LocalContentColor.current),
                                    modifier = Modifier.size(22.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Text(
                                text = stringResource(opt.mode.labelRes),
                                style = MaterialTheme.typography.labelSmallEmphasized,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
