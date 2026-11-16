@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.podlink.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.podlink.R
import dev.podlink.core.BondedCandidate
import dev.podlink.core.CompanionLinkSummary
import dev.podlink.core.CompanionSnapshot
import dev.podlink.ui.theme.PodThemeTokens

@Composable
fun PermissionNoticeCard(
    onGrantPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.remake_grant_bluetooth_title),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = stringResource(R.string.remake_grant_bluetooth_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(
                onClick = onGrantPermissions,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(text = stringResource(R.string.remake_grant_permissions_cta))
            }
        }
    }
}

@Composable
fun ErrorNoticeCard(
    message: String?,
    hiddenApiBlocked: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = stringResource(R.string.remake_status_error),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            val detail = when {
                hiddenApiBlocked -> stringResource(R.string.remake_hidden_api_blocked_hint)
                !message.isNullOrBlank() -> message
                else -> stringResource(R.string.remake_diagnostics_empty)
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            FilledTonalButton(
                onClick = onRetry,
                shapes = ButtonDefaults.shapes(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text(text = stringResource(R.string.remake_retry_connect))
            }
        }
    }
}

@Composable
fun CompanionCard(
    links: List<CompanionLinkSummary>,
    isAssociating: Boolean,
    onOpenDevicePicker: () -> Unit,
    onDisassociateAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = PodThemeTokens.cardContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isAssociating) {
                    ContainedLoadingIndicator(modifier = Modifier.size(38.dp))
                } else {
                    Surface(
                        shape = CircleShape,
                        color = if (links.isNotEmpty()) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Link,
                                contentDescription = null,
                                tint = if (links.isNotEmpty()) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.remake_companion_title),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
            }

            if (links.isEmpty()) {
                Text(
                    text = stringResource(R.string.remake_companion_unpaired_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onOpenDevicePicker,
                    enabled = !isAssociating,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(R.string.remake_pair_airpods_cta))
                }
            } else {
                Text(
                    text = stringResource(
                        R.string.remake_companion_paired_desc,
                        links.joinToString { "${it.displayName} (${it.macAddress})" },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDisassociateAll,
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LinkOff,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                        Text(text = stringResource(R.string.remake_unlink_device))
                    }
                    FilledTonalButton(
                        onClick = onOpenDevicePicker,
                        enabled = !isAssociating,
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(text = stringResource(R.string.remake_show_all_bt_cta))
                    }
                }
            }
        }
    }
}

@Composable
fun PairedDevicePickerSheet(
    candidates: List<BondedCandidate>,
    isAssociating: Boolean,
    onDismiss: () -> Unit,
    onSelectBonded: (BondedCandidate) -> Unit,
    onScanAirPods: () -> Unit,
    onScanAll: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Expanded,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                text = stringResource(R.string.remake_paired_picker_title),
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
            Text(
                text = stringResource(R.string.remake_paired_picker_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (candidates.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = PodThemeTokens.cardInsetContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.remake_paired_picker_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(PodThemeTokens.segmentGap),
                ) {
                    itemsIndexed(candidates, key = { _, d -> d.macAddress }) { index, dev ->
                        val shape = segmentedShape(index, candidates.size)
                        Surface(
                            onClick = { onSelectBonded(dev) },
                            enabled = !isAssociating,
                            modifier = Modifier.fillMaxWidth(),
                            shape = shape,
                            color = if (dev.isLikelyPods) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            } else {
                                PodThemeTokens.rowContainer
                            },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = if (dev.isLikelyPods) Icons.Rounded.Headphones else Icons.Rounded.Devices,
                                    contentDescription = null,
                                    tint = if (dev.isLikelyPods) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dev.name,
                                        style = MaterialTheme.typography.bodyLargeEmphasized,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = if (dev.hasPodUuid) {
                                            "${dev.macAddress} · ${stringResource(R.string.remake_airpods_uuid_badge)}"
                                        } else {
                                            dev.macAddress
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onScanAirPods,
                    enabled = !isAssociating,
                    modifier = Modifier.fillMaxWidth(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(R.string.remake_scan_nearby_airpods))
                }
                OutlinedButton(
                    onClick = onScanAll,
                    enabled = !isAssociating,
                    modifier = Modifier.fillMaxWidth(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(R.string.remake_scan_all_nearby))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagnosticsCard(
    logs: List<String>,
    companion: CompanionSnapshot,
    hiddenApiReady: Boolean,
    onCopyDiagnostics: () -> Unit,
    onClearDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
) {
    var expanded by remember(initiallyExpanded) { mutableStateOf(initiallyExpanded) }

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.remake_diagnostics_title),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                    )
                    Text(
                        text = stringResource(R.string.remake_diagnostics_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AssistChip(
                    onClick = { expanded = !expanded },
                    label = { Text(text = "SDK ${Build.VERSION.SDK_INT}") },
                )
                AssistChip(
                    onClick = { expanded = !expanded },
                    label = { Text(text = if (hiddenApiReady) "HiddenAPI: OK" else "HiddenAPI: Blocked") },
                )
                AssistChip(
                    onClick = { expanded = !expanded },
                    label = { Text(text = if (companion.observingPresence) "CDM: Observing" else "CDM: Idle") },
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val vScroll = rememberScrollState()
                    val hScroll = rememberScrollState()
                    val formattedLogs = remember(logs) {
                        if (logs.isEmpty()) "" else logs.joinToString("\n")
                    }
                    LaunchedEffect(logs.size) {
                        if (logs.isNotEmpty()) vScroll.animateScrollTo(vScroll.maxValue)
                    }

                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = PodThemeTokens.cardInsetContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 84.dp, max = 260.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(vScroll)
                                .horizontalScroll(hScroll)
                                .padding(12.dp),
                        ) {
                            SelectionContainer {
                                Text(
                                    text = if (formattedLogs.isEmpty()) {
                                        stringResource(R.string.remake_diagnostics_empty)
                                    } else {
                                        formattedLogs
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                    ),
                                    color = if (logs.isEmpty()) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onClearDiagnostics,
                            enabled = logs.isNotEmpty(),
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                            Text(text = stringResource(R.string.remake_clear_diagnostics))
                        }
                        FilledTonalButton(
                            onClick = onCopyDiagnostics,
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                            Text(text = stringResource(R.string.remake_copy_diagnostics))
                        }
                    }
                }
            }
        }
    }
}
