@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.podlink.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.podlink.R
import dev.podlink.core.BatteryMetric
import dev.podlink.core.PodBattery
import dev.podlink.core.PodModelSpec
import dev.podlink.core.WearSnapshot
import dev.podlink.core.WearState
import dev.podlink.ui.theme.PodThemeTokens
import dev.podlink.ui.theme.tabular

@Composable
fun BatteryOverviewCard(
    battery: PodBattery,
    wear: WearSnapshot,
    matchedModel: PodModelSpec?,
    isConnected: Boolean,
    modifier: Modifier = Modifier,
) {
    val leftRes = matchedModel?.leftRes ?: R.drawable.airpods_pro_2_left
    val rightRes = matchedModel?.rightRes ?: R.drawable.airpods_pro_2_right
    val caseRes = matchedModel?.caseRes ?: R.drawable.airpods_pro_2_case
    val headphoneRes = matchedModel?.budsRes ?: R.drawable.airpods_pro_2_buds

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = PodThemeTokens.cardContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        when {
            battery.headphone?.isAvailable == true &&
                battery.left?.isAvailable != true &&
                battery.right?.isAvailable != true -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PodBatterySlot(
                        label = stringResource(R.string.remake_headphone),
                        metric = battery.headphone,
                        wearState = wear.primary,
                        iconRes = headphoneRes,
                        isConnected = isConnected,
                    )
                }
            }
            battery.left?.isAvailable == true && battery.right?.isAvailable != true ||
                battery.right?.isAvailable == true && battery.left?.isAvailable != true -> {
                val leftOnly = battery.left?.isAvailable == true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top,
                ) {
                    PodBatterySlot(
                        label = stringResource(if (leftOnly) R.string.left else R.string.right),
                        metric = if (leftOnly) battery.left else battery.right,
                        wearState = wear.primary,
                        iconRes = if (leftOnly) leftRes else rightRes,
                        isConnected = isConnected,
                    )
                    if (battery.case?.isAvailable == true) {
                        PodBatterySlot(
                            label = stringResource(R.string.remake_metric_case),
                            metric = battery.case,
                            wearState = null,
                            iconRes = caseRes,
                            isConnected = isConnected,
                        )
                    }
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top,
                ) {
                    PodBatterySlot(
                        label = stringResource(R.string.left),
                        metric = battery.left,
                        wearState = wear.primary,
                        iconRes = leftRes,
                        isConnected = isConnected,
                    )
                    PodBatterySlot(
                        label = stringResource(R.string.right),
                        metric = battery.right,
                        wearState = wear.secondary,
                        iconRes = rightRes,
                        isConnected = isConnected,
                    )
                    if (battery.case?.isAvailable == true || (battery.left == null && battery.right == null)) {
                        PodBatterySlot(
                            label = stringResource(R.string.remake_metric_case),
                            metric = battery.case,
                            wearState = null,
                            iconRes = caseRes,
                            isConnected = isConnected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodBatterySlot(
    label: String,
    metric: BatteryMetric?,
    wearState: WearState?,
    @DrawableRes iconRes: Int,
    isConnected: Boolean,
) {
    val activeMetric = metric?.takeIf { it.isAvailable }
    val available = activeMetric != null
    val level = activeMetric?.level?.coerceIn(0, 100) ?: 0
    val isCharging = activeMetric?.isCharging == true
    val isWiggling = isCharging && level < 100

    val targetProgress = when {
        !available -> 0f
        isWiggling -> (level / 100f).coerceAtLeast(0.05f)
        else -> level / 100f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "battery_progress_$label",
    )

    val ringColor = when {
        !available || !isConnected -> MaterialTheme.colorScheme.outlineVariant
        isCharging -> MaterialTheme.colorScheme.tertiary
        level <= 20 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    val density = LocalDensity.current
    val ringStroke = remember(density) {
        Stroke(width = with(density) { 4.5.dp.toPx() }, cap = StrokeCap.Round)
    }
    val trackStroke = remember(density) {
        Stroke(width = with(density) { 4.dp.toPx() }, cap = StrokeCap.Round)
    }

    val amplitudeProvider = remember(available, isWiggling) {
        { progress: Float ->
            when {
                !available || progress <= 0f -> 0f
                isWiggling -> 1f
                progress >= 0.999f -> 0f
                else -> 0.7f
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(82.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                color = ringColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                stroke = ringStroke,
                trackStroke = trackStroke,
                amplitude = amplitudeProvider,
                wavelength = WavyProgressIndicatorDefaults.CircularWavelength,
                waveSpeed = if (isWiggling) WavyProgressIndicatorDefaults.CircularWavelength else 0.dp,
            )

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(PodThemeTokens.cardInsetContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(34.dp),
                )
            }

            if (isCharging) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = stringResource(R.string.remake_charging),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        Text(
            text = if (available) "$level%" else stringResource(R.string.remake_unavailable),
            style = MaterialTheme.typography.titleMediumEmphasized.tabular,
            color = if (available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        val statusBadge = when (wearState) {
            WearState.IN_EAR -> stringResource(R.string.remake_in_ear)
            WearState.IN_CASE -> stringResource(R.string.remake_in_case)
            WearState.OUT_OF_EAR -> stringResource(R.string.remake_out_of_ear)
            else -> null
        }
        if (statusBadge != null && available) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (wearState == WearState.IN_EAR) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            ) {
                Text(
                    text = statusBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (wearState == WearState.IN_EAR) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
fun DisconnectedStandbyCard(
    isConnecting: Boolean,
    statusHint: String?,
    onConnectNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = PodThemeTokens.cardContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (isConnecting) {
                ContainedLoadingIndicator(modifier = Modifier.size(40.dp))
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(
                        if (isConnecting) R.string.remake_status_connecting else R.string.remake_status_dormant,
                    ),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = statusHint ?: stringResource(R.string.remake_diagnostics_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!isConnecting) {
                Button(
                    onClick = onConnectNow,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(R.string.remake_sync_now))
                }
            }
        }
    }
}
