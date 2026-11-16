@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.podlink.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.podlink.R
import dev.podlink.core.CompanionSnapshot
import dev.podlink.core.ConnectionPhase
import dev.podlink.core.ListeningMode
import dev.podlink.core.PodFeature
import dev.podlink.core.PodUiState
import dev.podlink.ui.components.BatteryOverviewCard
import dev.podlink.ui.components.CompanionCard
import dev.podlink.ui.components.DisconnectedStandbyCard
import dev.podlink.ui.components.ErrorNoticeCard
import dev.podlink.ui.components.ExpressiveLazyScaffold
import dev.podlink.ui.components.ModeSelectorCard
import dev.podlink.ui.components.PermissionNoticeCard
import dev.podlink.ui.components.SegmentedActionRow
import dev.podlink.ui.components.SegmentedKeyValueRow
import dev.podlink.ui.components.SegmentedSwitchRow
import dev.podlink.ui.components.sectionGap
import dev.podlink.ui.components.sectionHeader
import dev.podlink.ui.theme.PodThemeTokens

@Composable
fun OverviewScreen(
    state: PodUiState,
    companion: CompanionSnapshot,
    isAssociating: Boolean,
    onGrantPermissions: () -> Unit,
    onOpenDevicePicker: () -> Unit,
    onDisassociateAll: () -> Unit,
    onSyncNow: () -> Unit,
    onDisconnect: () -> Unit,
    onSelectListeningMode: (ListeningMode) -> Unit,
    onRenamePods: (String) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenAdaptiveStrength: () -> Unit,
    onOpenStemControls: () -> Unit,
    onOpenCallControls: () -> Unit,
    onOpenMicrophone: () -> Unit,
    onOpenHeadTracking: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenHearingProtection: () -> Unit,
    onOpenHearingAid: () -> Unit,
    onOpenVersionInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetPersonalizedVolume: (Boolean) -> Unit,
    onSetConversationAwareness: (Boolean) -> Unit,
    onSetLoudSoundReduction: (Boolean) -> Unit,
    onSetEarDetection: (Boolean) -> Unit,
    onSetAutoPauseMedia: (Boolean) -> Unit,
    onSetAutomaticConnection: (Boolean) -> Unit,
    onSetSleepDetection: (Boolean) -> Unit,
    onSetOptimizedChargeLimit: (Boolean) -> Unit,
    onSetAllowOffOption: (Boolean) -> Unit,
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    val features = state.capabilities
    val hasBattery = state.battery.hasAnyAvailable
    val micLabel = when (state.micMode) {
        1 -> stringResource(R.string.microphone_always_right)
        2 -> stringResource(R.string.microphone_always_left)
        else -> stringResource(R.string.microphone_automatic)
    }

    ExpressiveLazyScaffold(
        title = stringResource(R.string.app_name),
        subtitle = null,
        actions = {
            if (state.bluetoothGranted && !state.isConnected && state.phase != ConnectionPhase.CONNECTING) {
                FilledTonalIconButton(
                    onClick = onSyncNow,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.remake_sync_now),
                    )
                }
            }
            FilledTonalIconButton(
                onClick = onOpenSettings,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.settings),
                )
            }
        },
    ) {
        if (!state.bluetoothGranted) {
            item(key = "perm_notice", contentType = "banner") {
                PermissionNoticeCard(onGrantPermissions = onGrantPermissions)
            }
            sectionGap("gap_perm")
        }

        if (state.bluetoothGranted && companion.links.isEmpty()) {
            item(key = "companion_setup", contentType = "companion") {
                CompanionCard(
                    links = companion.links,
                    isAssociating = isAssociating,
                    onOpenDevicePicker = onOpenDevicePicker,
                    onDisassociateAll = onDisassociateAll,
                )
            }
            sectionGap("gap_companion")
        }

        if (state.errorMessage != null || state.hiddenApiBlocked) {
            item(key = "error_banner", contentType = "banner") {
                ErrorNoticeCard(
                    message = state.errorMessage,
                    hiddenApiBlocked = state.hiddenApiBlocked,
                    onRetry = onSyncNow,
                )
            }
            sectionGap("gap_err")
        }

        item(key = "hero", contentType = "hero") {
            PodsHeroCard(state = state)
        }

        if (hasBattery) {
            sectionGap("gap_hero_bat")
            item(key = "battery", contentType = "battery") {
                BatteryOverviewCard(
                    battery = state.battery,
                    wear = state.wear,
                    matchedModel = state.matchedModel,
                    isConnected = state.isConnected,
                )
            }
        }

        if (!state.isConnected &&
            state.errorMessage == null &&
            !state.hiddenApiBlocked &&
            state.bluetoothGranted &&
            companion.links.isNotEmpty()
        ) {
            sectionGap("gap_standby")
            item(key = "standby", contentType = "standby") {
                DisconnectedStandbyCard(
                    isConnecting = state.phase == ConnectionPhase.CONNECTING,
                    statusHint = state.statusHint,
                    onConnectNow = onSyncNow,
                )
            }
        }

        if (state.isConnected) {
            if (state.supportsListeningMode) {
                sectionGap("gap_mode")
                item(key = "listening_mode", contentType = "listening_mode") {
                    ModeSelectorCard(
                        selectedMode = state.listeningMode,
                        showOffOption = state.allowOffOption,
                        showAdaptiveOption = PodFeature.ADAPTIVE_AUDIO in features,
                        enabled = state.isConnected,
                        onModeSelected = onSelectListeningMode,
                    )
                }
            }

            if (state.speakingDetected) {
                sectionGap("gap_ca_live")
                item(key = "speaking_banner", contentType = "status_pill") {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.remake_speaking_now),
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }

            sectionGap("gap_name")
            item(key = "name_row", contentType = "action") {
                SegmentedActionRow(
                    index = 0,
                    count = 1,
                    title = stringResource(R.string.name),
                    trailingText = state.displayName,
                    leadingIcon = Icons.Rounded.Edit,
                    onClick = { showRenameDialog = true },
                )
            }

            val hasPpe = PodFeature.PPE in features
            val hasHearingAid = PodFeature.HEARING_AID in features
            if (hasPpe || hasHearingAid) {
                sectionGap("gap_hh")
                sectionHeader("hdr_hh", R.string.hearing_health)
                val hhCount = (if (hasPpe) 1 else 0) + (if (hasHearingAid) 1 else 0)
                var hhIdx = 0
                if (hasPpe) {
                    val i = hhIdx++
                    item(key = "hh_ppe", contentType = "action") {
                        SegmentedActionRow(
                            index = i,
                            count = hhCount,
                            title = stringResource(R.string.hearing_protection),
                            leadingIcon = Icons.Rounded.Shield,
                            onClick = onOpenHearingProtection,
                        )
                    }
                }
                if (hasHearingAid) {
                    val i = hhIdx
                    item(key = "hh_aid", contentType = "action") {
                        SegmentedActionRow(
                            index = i,
                            count = hhCount,
                            title = stringResource(R.string.hearing_aid),
                            leadingIcon = Icons.Rounded.Hearing,
                            onClick = onOpenHearingAid,
                        )
                    }
                }
            }

            sectionGap("gap_controls")
            sectionHeader("hdr_controls", R.string.customizations)
            val controlsCount = if (state.supportsStemConfig) 3 else 2
            var cIdx = 0
            if (state.supportsStemConfig) {
                val i = cIdx++
                item(key = "ctrl_stem", contentType = "action") {
                    SegmentedActionRow(
                        index = i,
                        count = controlsCount,
                        title = stringResource(R.string.remake_stem_controls_title),
                        subtitle = stringResource(R.string.remake_stem_controls_subtitle),
                        leadingIcon = Icons.Rounded.TouchApp,
                        onClick = onOpenStemControls,
                    )
                }
            }
            val callIdx = cIdx++
            item(key = "ctrl_call", contentType = "action") {
                SegmentedActionRow(
                    index = callIdx,
                    count = controlsCount,
                    title = stringResource(R.string.call_controls),
                    subtitle = stringResource(R.string.remake_call_controls_subtitle),
                    leadingIcon = Icons.Rounded.Call,
                    onClick = onOpenCallControls,
                )
            }
            val micIdx = cIdx
            item(key = "ctrl_mic", contentType = "action") {
                SegmentedActionRow(
                    index = micIdx,
                    count = controlsCount,
                    title = stringResource(R.string.microphone),
                    trailingText = micLabel,
                    leadingIcon = Icons.Rounded.Mic,
                    onClick = onOpenMicrophone,
                )
            }

            sectionGap("gap_audio")
            sectionHeader("hdr_audio", R.string.audio)
            val hasAdaptive = PodFeature.ADAPTIVE_AUDIO in features
            val hasPersonalizedVol = PodFeature.ADAPTIVE_VOLUME in features
            val hasConvAwareness = PodFeature.CONVERSATION_AWARENESS in features
            val hasLoudSound = state.attAvailable && PodFeature.LOUD_SOUND_REDUCTION in features
            val audioCount = 1 +
                (if (hasAdaptive) 1 else 0) +
                (if (hasPersonalizedVol) 1 else 0) +
                (if (hasConvAwareness) 1 else 0) +
                (if (hasLoudSound) 1 else 0)
            var aIdx = 0

            val eqIdx = aIdx++
            item(key = "aud_eq", contentType = "action") {
                SegmentedActionRow(
                    index = eqIdx,
                    count = audioCount,
                    title = stringResource(R.string.equalizer),
                    subtitle = stringResource(R.string.remake_equalizer_subtitle),
                    leadingIcon = Icons.Rounded.GraphicEq,
                    onClick = onOpenEqualizer,
                )
            }
            if (hasAdaptive) {
                val i = aIdx++
                item(key = "aud_adaptive", contentType = "action") {
                    SegmentedActionRow(
                        index = i,
                        count = audioCount,
                        title = stringResource(R.string.adaptive_audio),
                        trailingText = "${state.adaptiveAncStrength}%",
                        leadingIcon = Icons.Rounded.Tune,
                        onClick = onOpenAdaptiveStrength,
                    )
                }
            }
            if (hasPersonalizedVol) {
                val i = aIdx++
                item(key = "aud_pvol", contentType = "switch") {
                    SegmentedSwitchRow(
                        index = i,
                        count = audioCount,
                        title = stringResource(R.string.personalized_volume),
                        subtitle = stringResource(R.string.personalized_volume_description),
                        leadingIcon = Icons.AutoMirrored.Rounded.VolumeUp,
                        checked = state.personalizedVolumeEnabled,
                        onCheckedChange = onSetPersonalizedVolume,
                    )
                }
            }
            if (hasConvAwareness) {
                val i = aIdx++
                item(key = "aud_ca", contentType = "switch") {
                    SegmentedSwitchRow(
                        index = i,
                        count = audioCount,
                        title = stringResource(R.string.conversation_awareness),
                        subtitle = stringResource(R.string.conversation_awareness_description),
                        leadingIcon = Icons.Rounded.RecordVoiceOver,
                        checked = state.conversationAwarenessEnabled,
                        onCheckedChange = onSetConversationAwareness,
                    )
                }
            }
            if (hasLoudSound) {
                val i = aIdx
                item(key = "aud_loud", contentType = "switch") {
                    SegmentedSwitchRow(
                        index = i,
                        count = audioCount,
                        title = stringResource(R.string.loud_sound_reduction),
                        subtitle = stringResource(R.string.loud_sound_reduction_description),
                        leadingIcon = Icons.AutoMirrored.Rounded.VolumeOff,
                        checked = state.loudSoundReductionEnabled,
                        onCheckedChange = onSetLoudSoundReduction,
                    )
                }
            }

            sectionGap("gap_conn")
            item(key = "conn_ear", contentType = "switch") {
                SegmentedSwitchRow(
                    index = 0,
                    count = 3,
                    title = stringResource(R.string.ear_detection),
                    subtitle = stringResource(R.string.ear_detection_description),
                    leadingIcon = Icons.Rounded.Sensors,
                    checked = state.earDetectionEnabled,
                    onCheckedChange = onSetEarDetection,
                )
            }
            item(key = "conn_pause", contentType = "switch") {
                SegmentedSwitchRow(
                    index = 1,
                    count = 3,
                    title = stringResource(R.string.remake_auto_pause_media_title),
                    subtitle = stringResource(R.string.remake_auto_pause_media_desc),
                    leadingIcon = Icons.Rounded.PauseCircleOutline,
                    checked = state.prefs.autoPauseResumeOnEarDetection,
                    enabled = state.earDetectionEnabled,
                    onCheckedChange = onSetAutoPauseMedia,
                )
            }
            item(key = "conn_auto", contentType = "switch") {
                SegmentedSwitchRow(
                    index = 2,
                    count = 3,
                    title = stringResource(R.string.automatically_connect),
                    subtitle = stringResource(R.string.automatically_connect_description),
                    leadingIcon = Icons.Rounded.BluetoothConnected,
                    checked = state.automaticConnectionEnabled,
                    onCheckedChange = onSetAutomaticConnection,
                )
            }

            sectionGap("gap_smart")
            val hasHeadGestures = PodFeature.HEAD_GESTURES in features
            val hasSleepDetection = PodFeature.SLEEP_DETECTION in features
            val hasOffOption = state.supportsListeningMode
            val smartCount = 2 +
                (if (hasHeadGestures) 1 else 0) +
                (if (hasSleepDetection) 1 else 0) +
                (if (hasOffOption) 1 else 0)
            var sIdx = 0

            if (hasHeadGestures) {
                val i = sIdx++
                item(key = "smart_head", contentType = "action") {
                    SegmentedActionRow(
                        index = i,
                        count = smartCount,
                        title = stringResource(R.string.head_gestures),
                        subtitle = stringResource(R.string.head_gestures_details),
                        trailingText = stringResource(
                            if (state.prefs.headGesturesEnabled) R.string.on else R.string.off,
                        ),
                        leadingIcon = Icons.Rounded.Face,
                        onClick = onOpenHeadTracking,
                    )
                }
            }
            if (hasSleepDetection) {
                val i = sIdx++
                item(key = "smart_sleep", contentType = "switch") {
                    SegmentedSwitchRow(
                        index = i,
                        count = smartCount,
                        title = stringResource(R.string.sleep_detection),
                        leadingIcon = Icons.Rounded.Bedtime,
                        checked = state.sleepDetectionEnabled,
                        onCheckedChange = onSetSleepDetection,
                    )
                }
            }
            val optIdx = sIdx++
            item(key = "smart_charge", contentType = "switch") {
                SegmentedSwitchRow(
                    index = optIdx,
                    count = smartCount,
                    title = stringResource(R.string.optimized_charging),
                    subtitle = stringResource(R.string.optimized_charging_description),
                    leadingIcon = Icons.Rounded.BatteryChargingFull,
                    checked = state.optimizedChargeLimitEnabled,
                    onCheckedChange = onSetOptimizedChargeLimit,
                )
            }
            val accIdx = sIdx++
            item(key = "smart_a11y", contentType = "action") {
                SegmentedActionRow(
                    index = accIdx,
                    count = smartCount,
                    title = stringResource(R.string.accessibility),
                    leadingIcon = Icons.Rounded.AccessibilityNew,
                    onClick = onOpenAccessibility,
                )
            }
            if (hasOffOption) {
                val i = sIdx
                item(key = "smart_off", contentType = "switch") {
                    SegmentedSwitchRow(
                        index = i,
                        count = smartCount,
                        title = stringResource(R.string.off_listening_mode),
                        subtitle = stringResource(R.string.off_listening_mode_description),
                        leadingIcon = Icons.Rounded.Block,
                        checked = state.allowOffOption,
                        onCheckedChange = onSetAllowOffOption,
                    )
                }
            }

            sectionGap("gap_about")
            sectionHeader("hdr_about", R.string.about)
            val hasModel = state.effectiveModelNumber.isNotBlank()
            val hasFw = state.effectiveFirmware.isNotBlank()
            val aboutCount = 1 + (if (hasModel) 1 else 0) + (if (hasFw) 1 else 0)
            var abIdx = 0
            if (hasModel) {
                val i = abIdx++
                item(key = "about_model", contentType = "kv") {
                    SegmentedKeyValueRow(
                        index = i,
                        count = aboutCount,
                        title = stringResource(R.string.model_name),
                        value = state.matchedModel?.name ?: state.effectiveModelNumber,
                        leadingIcon = Icons.Rounded.Headphones,
                    )
                }
            }
            if (hasFw) {
                val i = abIdx++
                item(key = "about_fw", contentType = "kv") {
                    SegmentedKeyValueRow(
                        index = i,
                        count = aboutCount,
                        title = stringResource(R.string.version),
                        value = state.effectiveFirmware,
                        leadingIcon = Icons.Rounded.Memory,
                    )
                }
            }
            val verIdx = abIdx
            item(key = "about_version", contentType = "action") {
                SegmentedActionRow(
                    index = verIdx,
                    count = aboutCount,
                    title = stringResource(R.string.device_info),
                    leadingIcon = Icons.Rounded.Info,
                    onClick = onOpenVersionInfo,
                )
            }

            sectionGap("gap_disconnect")
            item(key = "disconnect_btn", contentType = "button") {
                FilledTonalButton(
                    onClick = onDisconnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BluetoothDisabled,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(
                        text = stringResource(R.string.disconnect),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        RenamePodsDialog(
            currentName = state.displayName,
            onDismiss = { showRenameDialog = false },
            onSave = { updated ->
                onRenamePods(updated)
                showRenameDialog = false
            },
        )
    }
}

@Composable
private fun PodsHeroCard(state: PodUiState) {
    val budsRes = state.matchedModel?.budsRes ?: R.drawable.airpods_pro_2_buds
    val caseRes = state.matchedModel?.caseRes ?: R.drawable.airpods_pro_2_case

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = PodThemeTokens.cardContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(budsRes),
                    contentDescription = state.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(88.dp),
                )
                if (state.battery.case?.isAvailable == true || state.matchedModel != null) {
                    Image(
                        painter = painterResource(caseRes),
                        contentDescription = stringResource(R.string.case_alt),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(82.dp),
                    )
                }
            }

            val badgeText = when (state.phase) {
                ConnectionPhase.CONNECTED -> {
                    val m = state.matchedModel?.name ?: state.effectiveModelNumber.ifBlank { "AirPods" }
                    "$m · ${stringResource(R.string.remake_status_connected)}"
                }
                ConnectionPhase.CONNECTING -> stringResource(R.string.remake_status_connecting)
                ConnectionPhase.ERROR -> stringResource(R.string.remake_status_error)
                ConnectionPhase.DISCONNECTED -> state.matchedModel?.name ?: stringResource(R.string.remake_status_dormant)
            }
            val badgeContainer = when (state.phase) {
                ConnectionPhase.CONNECTED -> MaterialTheme.colorScheme.secondaryContainer
                ConnectionPhase.CONNECTING -> MaterialTheme.colorScheme.tertiaryContainer
                ConnectionPhase.ERROR -> MaterialTheme.colorScheme.errorContainer
                ConnectionPhase.DISCONNECTED -> PodThemeTokens.cardInsetContainer
            }
            val badgeContent = when (state.phase) {
                ConnectionPhase.CONNECTED -> MaterialTheme.colorScheme.onSecondaryContainer
                ConnectionPhase.CONNECTING -> MaterialTheme.colorScheme.onTertiaryContainer
                ConnectionPhase.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                ConnectionPhase.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                shape = CircleShape,
                color = badgeContainer,
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = badgeContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun RenamePodsDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember(currentName) { mutableStateOf(currentName) }
    val valid = text.trim().isNotEmpty() && text.trim().toByteArray(Charsets.UTF_8).size <= 255

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.remake_rename_title),
                style = MaterialTheme.typography.headlineSmallEmphasized,
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                label = { Text(text = stringResource(R.string.remake_rename_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(text) },
                enabled = valid,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(text = stringResource(R.string.remake_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(text = stringResource(R.string.remake_cancel))
            }
        },
    )
}
