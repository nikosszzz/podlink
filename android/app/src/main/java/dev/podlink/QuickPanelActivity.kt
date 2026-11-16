@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.podlink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.podlink.core.PodFeature
import dev.podlink.ui.components.ModeSelectorCard
import dev.podlink.ui.components.SegmentedSwitchRow
import dev.podlink.ui.theme.PodTheme
import dev.podlink.ui.theme.PodThemeTokens
import dev.podlink.ui.theme.tabular

class QuickPanelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val coordinator = (application as PodLinkApplication).coordinator

        setContent {
            PodTheme {
                val state by coordinator.state.collectAsStateWithLifecycle()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = ::finish,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                            ),
                        shape = MaterialTheme.shapes.extraLargeIncreased,
                        colors = CardDefaults.cardColors(containerColor = PodThemeTokens.pageContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = state.displayName,
                                style = MaterialTheme.typography.titleLargeEmphasized,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val batterySummary = state.battery.formatCompact(includeCase = true)
                                ?: if (!state.isConnected) stringResource(R.string.airpods_not_connected) else null
                            batterySummary?.let { summary ->
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.bodyMedium.tabular,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (state.supportsListeningMode) {
                                ModeSelectorCard(
                                    selectedMode = state.listeningMode,
                                    showOffOption = state.allowOffOption,
                                    showAdaptiveOption = PodFeature.ADAPTIVE_AUDIO in state.capabilities,
                                    enabled = state.isConnected,
                                    onModeSelected = { mode -> coordinator.setListeningMode(mode) },
                                )
                            }

                            if (PodFeature.CONVERSATION_AWARENESS in state.capabilities) {
                                SegmentedSwitchRow(
                                    index = 0,
                                    count = 1,
                                    title = stringResource(R.string.conversation_awareness),
                                    checked = state.conversationAwarenessEnabled,
                                    enabled = state.isConnected,
                                    onCheckedChange = coordinator::setConversationAwareness,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
