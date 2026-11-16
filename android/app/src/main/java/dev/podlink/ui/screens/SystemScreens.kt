package dev.podlink.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.podlink.R
import dev.podlink.core.CompanionSnapshot
import dev.podlink.core.PodUiState
import dev.podlink.ui.components.DiagnosticsCard
import dev.podlink.ui.components.ExpressiveLazyScaffold
import dev.podlink.ui.components.SegmentedKeyValueRow
import dev.podlink.ui.components.sectionGap
import dev.podlink.ui.components.sectionHeader

@Composable
fun VersionInfoScreen(
    state: PodUiState,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val meta = state.metadata
    val serials = state.effectiveSerials
    val copiedMsg = stringResource(R.string.remake_serial_copied)

    fun copySerial(value: String) {
        if (value.isBlank()) return
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("serial", value))
        Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
    }

    val coreRows = buildList {
        add(stringResource(R.string.model_name) to (state.matchedModel?.name ?: state.displayName))
        state.effectiveModelNumber.takeIf { it.isNotBlank() }?.let { add(stringResource(R.string.model_number) to it) }
        meta?.manufacturer?.takeIf { it.isNotBlank() }?.let { add(stringResource(R.string.manufacturer) to it) }
        serials.getOrNull(0)?.takeIf { it.isNotBlank() }?.let { add(stringResource(R.string.serial_number) to it) }
        serials.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { add("${stringResource(R.string.left)} ${stringResource(R.string.serial_number)}" to it) }
        serials.getOrNull(2)?.takeIf { it.isNotBlank() }?.let { add("${stringResource(R.string.right)} ${stringResource(R.string.serial_number)}" to it) }
        meta?.hardwareRevision?.takeIf { it.isNotBlank() }?.let { add(stringResource(R.string.remake_hardware_revision) to it) }
        state.effectiveFirmware.takeIf { it.isNotBlank() }?.let { add(stringResource(R.string.remake_firmware_ver_1) to it) }
        meta?.version2?.takeIf { it.isNotBlank() }?.let { add(stringResource(R.string.remake_firmware_ver_2) to it) }
        meta?.updaterId?.takeIf { it.isNotBlank() }?.let { add(stringResource(R.string.remake_firmware_ver_3) to it) }
    }
    val audioSourceTitle = stringResource(R.string.remake_active_audio_source)
    val serialTitle = stringResource(R.string.serial_number)

    ExpressiveLazyScaffold(
        title = stringResource(R.string.device_info),
        subtitle = state.displayName,
        onBack = onBack,
    ) {
        itemsIndexed(
            items = coreRows,
            key = { _, pair -> "kv_${pair.first}" },
            contentType = { _, _ -> "kv" },
        ) { idx, (label, value) ->
            SegmentedKeyValueRow(
                index = idx,
                count = coreRows.size,
                title = label,
                value = value,
                onClick = if (label.contains(serialTitle)) {
                    { copySerial(value) }
                } else {
                    null
                },
            )
        }

        if (state.audioSource != null || state.connectedPeers.isNotEmpty()) {
            sectionGap("gap_routing")
            sectionHeader("hdr_routing", R.string.remake_connected_peers)

            val routingRows = buildList {
                state.audioSource?.let { src ->
                    add(audioSourceTitle to "${src.mac} (${src.type.name})")
                }
                state.connectedPeers.forEachIndexed { i, peer ->
                    add("Peer ${i + 1}" to "${peer.type ?: "Device"} (${peer.mac})")
                }
            }
            itemsIndexed(
                items = routingRows,
                key = { idx, pair -> "route_${idx}_${pair.first}" },
                contentType = { _, _ -> "kv" },
            ) { idx, (label, value) ->
                SegmentedKeyValueRow(
                    index = idx,
                    count = routingRows.size,
                    title = label,
                    value = value,
                )
            }
        }
    }
}

@Composable
fun DiagnosticsScreen(
    logs: List<String>,
    companion: CompanionSnapshot,
    hiddenApiReady: Boolean,
    onCopyDiagnostics: () -> Unit,
    onClearDiagnostics: () -> Unit,
    onBack: () -> Unit,
) {
    ExpressiveLazyScaffold(
        title = stringResource(R.string.remake_diagnostics_title),
        subtitle = stringResource(R.string.remake_diagnostics_subtitle),
        onBack = onBack,
    ) {
        item(key = "diag_card", contentType = "diag_card") {
            DiagnosticsCard(
                logs = logs,
                companion = companion,
                hiddenApiReady = hiddenApiReady,
                initiallyExpanded = true,
                onCopyDiagnostics = onCopyDiagnostics,
                onClearDiagnostics = onClearDiagnostics,
            )
        }
    }
}
