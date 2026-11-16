@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.podlink.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.podlink.R
import dev.podlink.core.CompanionSnapshot
import dev.podlink.core.NotificationStyle
import dev.podlink.core.PodUiState
import dev.podlink.core.ShizukuBridgeStatus
import dev.podlink.ui.components.ExpressiveLazyScaffold
import dev.podlink.ui.components.SegmentedActionRow
import dev.podlink.ui.components.SegmentedSelectableRow
import dev.podlink.ui.components.SegmentedSliderRow
import dev.podlink.ui.components.SegmentedSwitchRow
import dev.podlink.ui.components.sectionGap
import dev.podlink.ui.components.sectionHeader
import dev.podlink.ui.components.segmentedShape
import dev.podlink.ui.theme.PodThemeTokens
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: PodUiState,
    companion: CompanionSnapshot,
    notificationsGranted: Boolean,
    canPostPromoted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenLiveNotificationSettings: () -> Unit,
    onSetShowBatteryNotification: (Boolean) -> Unit,
    onSetNotificationStyle: (NotificationStyle) -> Unit,
    onSetIncludeCaseInNotification: (Boolean) -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onSetShizukuBatterySync: (Boolean) -> Unit,
    onSetConversationAwarenessDuckVolume: (Boolean) -> Unit,
    onSetConversationAwarenessVolumePercent: (Int) -> Unit,
    onSetConversationAwarenessRelativeVolume: (Boolean) -> Unit,
    onSetConversationAwarenessPauseMusic: (Boolean) -> Unit,
    onSetShowPhoneBatteryInWidget: (Boolean) -> Unit,
    onSetQsClickBehaviorDialog: (Boolean) -> Unit,
    onSetUseAlternateHeadTrackingPackets: (Boolean) -> Unit,
    onSetDisconnectWhenNotWearing: (Boolean) -> Unit,
    onOpenDevicePicker: () -> Unit,
    onDisassociate: (Int) -> Unit,
    onOpenDiagnostics: () -> Unit,
    onResetToDefaults: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }

    val prefs = state.prefs
    val supportsLiveChip = Build.VERSION.SDK_INT >= 36
    val showLiveWarning = supportsLiveChip &&
        prefs.showBatteryNotification &&
        notificationsGranted &&
        prefs.notificationStyle == NotificationStyle.LIVE_CHIP &&
        !canPostPromoted
    val showNotifPermCta = prefs.showBatteryNotification && !notificationsGranted

    val notifRows = 2 +
        (if (supportsLiveChip) 2 else 0) +
        (if (showNotifPermCta) 1 else 0) +
        (if (showLiveWarning) 1 else 0)

    ExpressiveLazyScaffold(
        title = stringResource(R.string.remake_app_settings_title),
        subtitle = stringResource(R.string.remake_app_settings_subtitle),
        onBack = onBack,
    ) {
        sectionHeader("hdr_notif", R.string.remake_notifications_section)

        var nIdx = 0
        if (showNotifPermCta) {
            val i = nIdx++
            item(key = "notif_perm", contentType = "action") {
                SegmentedActionRow(
                    index = i,
                    count = notifRows,
                    title = stringResource(R.string.remake_grant_notif_cta),
                    subtitle = stringResource(R.string.permissions_required),
                    leadingIcon = Icons.Rounded.NotificationsActive,
                    onClick = onRequestNotificationPermission,
                )
            }
        }

        val toggleIdx = nIdx++
        item(key = "notif_enable", contentType = "switch") {
            SegmentedSwitchRow(
                index = toggleIdx,
                count = notifRows,
                title = stringResource(R.string.remake_battery_notif_title),
                subtitle = stringResource(R.string.remake_battery_notif_desc),
                checked = prefs.showBatteryNotification,
                onCheckedChange = onSetShowBatteryNotification,
            )
        }

        if (supportsLiveChip) {
            val cardStyleIdx = nIdx++
            item(key = "notif_style_card", contentType = "radio") {
                SegmentedSelectableRow(
                    index = cardStyleIdx,
                    count = notifRows,
                    title = stringResource(R.string.remake_notif_style_card),
                    subtitle = stringResource(R.string.remake_notif_style_card_desc),
                    selected = prefs.notificationStyle == NotificationStyle.DEVICE_CARD,
                    enabled = prefs.showBatteryNotification,
                    onClick = { onSetNotificationStyle(NotificationStyle.DEVICE_CARD) },
                )
            }

            val liveStyleIdx = nIdx++
            item(key = "notif_style_live", contentType = "radio") {
                SegmentedSelectableRow(
                    index = liveStyleIdx,
                    count = notifRows,
                    title = stringResource(R.string.remake_notif_style_live),
                    subtitle = stringResource(R.string.remake_notif_style_live_desc),
                    selected = prefs.notificationStyle == NotificationStyle.LIVE_CHIP,
                    enabled = prefs.showBatteryNotification,
                    onClick = { onSetNotificationStyle(NotificationStyle.LIVE_CHIP) },
                )
            }

            if (showLiveWarning) {
                val i = nIdx++
                item(key = "notif_live_perm", contentType = "action") {
                    SegmentedActionRow(
                        index = i,
                        count = notifRows,
                        title = stringResource(R.string.remake_live_notif_allow_cta),
                        subtitle = stringResource(R.string.remake_live_notif_allow_desc),
                        leadingIcon = Icons.Rounded.NotificationsActive,
                        onClick = onOpenLiveNotificationSettings,
                    )
                }
            }
        }

        val caseIdx = nIdx
        item(key = "notif_case", contentType = "switch") {
            SegmentedSwitchRow(
                index = caseIdx,
                count = notifRows,
                title = stringResource(R.string.remake_battery_notif_case_title),
                subtitle = stringResource(R.string.remake_battery_notif_case_desc),
                checked = prefs.includeCaseInNotification,
                enabled = prefs.showBatteryNotification,
                onCheckedChange = onSetIncludeCaseInNotification,
            )
        }

        sectionGap("gap_shizuku")
        sectionHeader("hdr_shizuku", R.string.remake_shizuku_section)

        item(key = "shizuku_status", contentType = "shizuku_status") {
            ShizukuStatusRow(
                index = 0,
                count = 2,
                status = state.shizukuStatus,
                lastSyncNote = state.shizukuLastSyncNote,
                onAuthorize = onRequestShizukuPermission,
            )
        }
        item(key = "shizuku_toggle", contentType = "switch") {
            SegmentedSwitchRow(
                index = 1,
                count = 2,
                title = stringResource(R.string.remake_shizuku_battery_title),
                subtitle = stringResource(R.string.remake_shizuku_battery_desc),
                leadingIcon = Icons.Rounded.BatteryStd,
                checked = prefs.shizukuBatterySync,
                enabled = state.shizukuStatus != ShizukuBridgeStatus.NOT_INSTALLED,
                onCheckedChange = { enabled ->
                    onSetShizukuBatterySync(enabled)
                    if (enabled && state.shizukuStatus == ShizukuBridgeStatus.PERMISSION_REQUIRED) {
                        onRequestShizukuPermission()
                    }
                },
            )
        }

        sectionGap("gap_ca")
        sectionHeader("hdr_ca", R.string.conversation_awareness)

        item(key = "ca_duck", contentType = "switch") {
            SegmentedSwitchRow(
                index = 0,
                count = 4,
                title = stringResource(R.string.remake_duck_volume_title),
                subtitle = stringResource(R.string.remake_duck_volume_desc),
                checked = prefs.conversationAwarenessDuckVolume,
                onCheckedChange = onSetConversationAwarenessDuckVolume,
            )
        }
        item(key = "ca_vol", contentType = "slider") {
            SegmentedSliderRow(
                index = 1,
                count = 4,
                title = stringResource(R.string.conversation_awareness_volume),
                valueLabel = "${prefs.conversationAwarenessVolumePercent}%",
                value = prefs.conversationAwarenessVolumePercent.toFloat(),
                onValueChange = { onSetConversationAwarenessVolumePercent(it.roundToInt()) },
                valueRange = 10f..85f,
                steps = 14,
                enabled = prefs.conversationAwarenessDuckVolume,
                startLabel = "10%",
                endLabel = "85%",
            )
        }
        item(key = "ca_relative", contentType = "switch") {
            SegmentedSwitchRow(
                index = 2,
                count = 4,
                title = stringResource(R.string.relative_conversation_awareness_volume),
                subtitle = stringResource(R.string.relative_conversation_awareness_volume_description),
                checked = prefs.conversationAwarenessRelativeVolume,
                enabled = prefs.conversationAwarenessDuckVolume,
                onCheckedChange = onSetConversationAwarenessRelativeVolume,
            )
        }
        item(key = "ca_pause", contentType = "switch") {
            SegmentedSwitchRow(
                index = 3,
                count = 4,
                title = stringResource(R.string.conversation_awareness_pause_music),
                subtitle = stringResource(R.string.conversation_awareness_pause_music_description),
                checked = prefs.conversationAwarenessPauseMusic,
                onCheckedChange = onSetConversationAwarenessPauseMusic,
            )
        }

        sectionGap("gap_widget")
        sectionHeader("hdr_widget", R.string.widget)

        item(key = "widget_phone", contentType = "switch") {
            SegmentedSwitchRow(
                index = 0,
                count = 2,
                title = stringResource(R.string.show_phone_battery_in_widget),
                subtitle = stringResource(R.string.show_phone_battery_in_widget_description),
                checked = prefs.showPhoneBatteryInWidget,
                onCheckedChange = onSetShowPhoneBatteryInWidget,
            )
        }
        item(key = "qs_dialog", contentType = "switch") {
            SegmentedSwitchRow(
                index = 1,
                count = 2,
                title = stringResource(R.string.qs_click_behavior_title),
                subtitle = stringResource(
                    if (prefs.qsClickBehaviorDialog) R.string.qs_click_behavior_dialog_desc else R.string.qs_click_behavior_cycle_desc,
                ),
                checked = prefs.qsClickBehaviorDialog,
                onCheckedChange = onSetQsClickBehaviorDialog,
            )
        }

        sectionGap("gap_adv")
        sectionHeader("hdr_adv", R.string.advanced_options)

        item(key = "adv_alt_ht", contentType = "switch") {
            SegmentedSwitchRow(
                index = 0,
                count = 2,
                title = stringResource(R.string.use_alternate_head_tracking_packets),
                subtitle = stringResource(R.string.use_alternate_head_tracking_packets_description),
                checked = prefs.useAlternateHeadTrackingPackets,
                onCheckedChange = onSetUseAlternateHeadTrackingPackets,
            )
        }
        item(key = "adv_disconnect", contentType = "switch") {
            SegmentedSwitchRow(
                index = 1,
                count = 2,
                title = stringResource(R.string.disconnect_when_not_wearing),
                subtitle = stringResource(R.string.disconnect_when_not_wearing_description),
                checked = prefs.disconnectWhenNotWearing,
                onCheckedChange = onSetDisconnectWhenNotWearing,
            )
        }

        sectionGap("gap_links")
        sectionHeader("hdr_links", R.string.remake_companion_linked_title)

        val links = companion.links
        val totalLinkRows = links.size + 1
        itemsIndexed(
            items = links,
            key = { _, link -> "link_${link.id}" },
            contentType = { _, _ -> "action" },
        ) { idx, link ->
            SegmentedActionRow(
                index = idx,
                count = totalLinkRows,
                title = link.displayName,
                subtitle = link.macAddress,
                trailingText = stringResource(R.string.remake_companion_unlink),
                leadingIcon = Icons.Rounded.LinkOff,
                onClick = { onDisassociate(link.id) },
            )
        }
        item(key = "link_add", contentType = "action") {
            SegmentedActionRow(
                index = links.size,
                count = totalLinkRows,
                title = stringResource(R.string.remake_companion_link_another),
                leadingIcon = Icons.Rounded.AddLink,
                onClick = onOpenDevicePicker,
            )
        }

        sectionGap("gap_diag")
        sectionHeader("hdr_diag", R.string.troubleshooting)

        item(key = "diag_logs", contentType = "action") {
            SegmentedActionRow(
                index = 0,
                count = 2,
                title = stringResource(R.string.remake_diagnostics_title),
                subtitle = stringResource(R.string.remake_diagnostics_subtitle),
                leadingIcon = Icons.Rounded.BugReport,
                onClick = onOpenDiagnostics,
            )
        }
        item(key = "diag_reset", contentType = "action") {
            SegmentedActionRow(
                index = 1,
                count = 2,
                title = stringResource(R.string.remake_reset_defaults_title),
                subtitle = stringResource(R.string.remake_reset_defaults_desc),
                leadingIcon = Icons.Rounded.RestartAlt,
                onClick = { confirmReset = true },
            )
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = {
                Text(
                    text = stringResource(R.string.remake_reset_defaults_title),
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                )
            },
            text = { Text(text = stringResource(R.string.remake_reset_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmReset = false
                        onResetToDefaults()
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmReset = false },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(R.string.remake_cancel))
                }
            },
        )
    }
}

@Composable
private fun ShizukuStatusRow(
    index: Int,
    count: Int,
    status: ShizukuBridgeStatus,
    lastSyncNote: String?,
    onAuthorize: () -> Unit,
) {
    val shape = segmentedShape(index, count)
    val statusLabel = when (status) {
        ShizukuBridgeStatus.READY -> stringResource(R.string.remake_shizuku_status_ready)
        ShizukuBridgeStatus.PERMISSION_REQUIRED -> stringResource(R.string.remake_shizuku_status_perm)
        ShizukuBridgeStatus.NOT_RUNNING -> stringResource(R.string.remake_shizuku_status_stopped)
        ShizukuBridgeStatus.NOT_INSTALLED -> stringResource(R.string.remake_shizuku_status_missing)
    }
    val containerColor = when (status) {
        ShizukuBridgeStatus.READY -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ShizukuBridgeStatus.PERMISSION_REQUIRED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        else -> PodThemeTokens.rowContainer
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                tint = when (status) {
                    ShizukuBridgeStatus.READY -> MaterialTheme.colorScheme.primary
                    ShizukuBridgeStatus.PERMISSION_REQUIRED -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!lastSyncNote.isNullOrBlank()) {
                    Text(
                        text = lastSyncNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when (status) {
                ShizukuBridgeStatus.PERMISSION_REQUIRED -> {
                    FilledTonalButton(
                        onClick = onAuthorize,
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(text = stringResource(R.string.remake_shizuku_grant_cta))
                    }
                }
                ShizukuBridgeStatus.NOT_RUNNING -> {
                    val context = LocalContext.current
                    FilledTonalButton(
                        onClick = {
                            runCatching {
                                context.packageManager
                                    .getLaunchIntentForPackage("com.hamondev.shevery")
                                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    ?.let(context::startActivity)
                            }
                        },
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(text = stringResource(R.string.remake_shizuku_open_cta))
                    }
                }
                ShizukuBridgeStatus.READY,
                ShizukuBridgeStatus.NOT_INSTALLED,
                -> Unit
            }
        }
    }
}
