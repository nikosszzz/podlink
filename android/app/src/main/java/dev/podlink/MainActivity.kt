package dev.podlink

import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.podlink.bt.L2capChannel
import dev.podlink.core.BondedCandidate
import dev.podlink.core.TraceLog
import dev.podlink.ui.components.PairedDevicePickerSheet
import dev.podlink.ui.screens.AccessibilityScreen
import dev.podlink.ui.screens.AdaptiveStrengthScreen
import dev.podlink.ui.screens.CallControlsScreen
import dev.podlink.ui.screens.DiagnosticsScreen
import dev.podlink.ui.screens.EqualizerScreen
import dev.podlink.ui.screens.HeadTrackingScreen
import dev.podlink.ui.screens.HearingAidScreen
import dev.podlink.ui.screens.HearingProtectionScreen
import dev.podlink.ui.screens.MicrophoneScreen
import dev.podlink.ui.screens.OverviewScreen
import dev.podlink.ui.screens.SettingsScreen
import dev.podlink.ui.screens.StemControlsScreen
import dev.podlink.ui.screens.TransparencyScreen
import dev.podlink.ui.screens.VersionInfoScreen
import dev.podlink.ui.theme.PodTheme
import dev.podlink.ui.theme.PodThemeTokens

private enum class AppRoute {
    OVERVIEW,
    SETTINGS,
    EQUALIZER,
    ADAPTIVE_STRENGTH,
    STEM_CONTROLS,
    CALL_CONTROLS,
    MICROPHONE,
    HEAD_TRACKING,
    ACCESSIBILITY,
    TRANSPARENCY,
    HEARING_PROTECTION,
    HEARING_AID,
    VERSION_INFO,
    DIAGNOSTICS,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as PodLinkApplication

        setContent {
            PodTheme {
                PodAppRoot(app = app)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PodAppRoot(app: PodLinkApplication) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coordinator = app.coordinator

    val uiState by coordinator.state.collectAsStateWithLifecycle()
    val companionState by coordinator.companionState.collectAsStateWithLifecycle()

    val backStack = remember { mutableStateListOf(AppRoute.OVERVIEW) }
    val currentRoute = backStack.last()
    var navigatingBack by remember { mutableStateOf(false) }

    var notificationsGranted by remember { mutableStateOf(hasNotifPermission(context)) }
    var canPostPromoted by remember { mutableStateOf(app.notifier.canPostPromoted()) }
    var isAssociating by remember { mutableStateOf(false) }
    var showDevicePicker by remember { mutableStateOf(false) }
    var bondedCandidates by remember { mutableStateOf<List<BondedCandidate>>(emptyList()) }

    val navigate: (AppRoute) -> Unit = remember(backStack) {
        { target ->
            if (backStack.lastOrNull() != target) {
                navigatingBack = false
                backStack.add(target)
            }
        }
    }
    val goBack: () -> Unit = remember(backStack) {
        {
            if (backStack.size > 1) {
                navigatingBack = true
                backStack.removeAt(backStack.lastIndex)
            }
        }
    }

    val openEqualizer = remember(navigate) { { navigate(AppRoute.EQUALIZER) } }
    val openAdaptiveStrength = remember(navigate) { { navigate(AppRoute.ADAPTIVE_STRENGTH) } }
    val openStemControls = remember(navigate) { { navigate(AppRoute.STEM_CONTROLS) } }
    val openCallControls = remember(navigate) { { navigate(AppRoute.CALL_CONTROLS) } }
    val openMicrophone = remember(navigate) { { navigate(AppRoute.MICROPHONE) } }
    val openHeadTracking = remember(navigate) { { navigate(AppRoute.HEAD_TRACKING) } }
    val openAccessibility = remember(navigate) { { navigate(AppRoute.ACCESSIBILITY) } }
    val openHearingProtection = remember(navigate) { { navigate(AppRoute.HEARING_PROTECTION) } }
    val openHearingAid = remember(navigate) { { navigate(AppRoute.HEARING_AID) } }
    val openVersionInfo = remember(navigate) { { navigate(AppRoute.VERSION_INFO) } }
    val openSettings = remember(navigate) { { navigate(AppRoute.SETTINGS) } }
    val openTransparency = remember(navigate) { { navigate(AppRoute.TRANSPARENCY) } }
    val openDiagnostics = remember(navigate) { { navigate(AppRoute.DIAGNOSTICS) } }

    val syncNow = remember(coordinator) { { coordinator.syncIfIdle(userInitiated = true) } }
    val selectListeningMode: (dev.podlink.core.ListeningMode) -> Unit = remember(coordinator) {
        { mode -> coordinator.setListeningMode(mode) }
    }
    val startHeadTracking = remember(coordinator) {
        {
            coordinator.startHeadTracking()
            Unit
        }
    }
    val stopHeadTracking = remember(coordinator) {
        {
            coordinator.stopHeadTracking()
            Unit
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        goBack()
    }

    val btPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = hasBtPermissions(context) ||
            (grants[Manifest.permission.BLUETOOTH_CONNECT] == true)
        coordinator.onPermissionsUpdated(granted)
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsGranted = granted || hasNotifPermission(context)
        canPostPromoted = app.notifier.canPostPromoted()
        coordinator.refreshNotifications()
    }

    val cdmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        isAssociating = false
        val info: AssociationInfo? = result.data?.getParcelableExtra(
            CompanionDeviceManager.EXTRA_ASSOCIATION,
            AssociationInfo::class.java,
        )
        coordinator.handleAssociationResult(info)
    }

    val openPicker = remember(coordinator) {
        {
            bondedCandidates = coordinator.listUnassociatedBonded()
            showDevicePicker = true
        }
    }

    val openLiveSettings = remember(context) {
        {
            val intent = Intent("android.settings.MANAGE_APP_PROMOTED_NOTIFICATIONS").apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            runCatching { context.startActivity(intent) }.onFailure {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }
            Unit
        }
    }

    val requestPermissions = remember(btPermissionsLauncher) {
        {
            btPermissionsLauncher.launch(requiredBtPermissions())
        }
    }

    val requestNotifPerm = remember(notifPermissionLauncher) {
        {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(uiState.statusHint) {
        val hint = uiState.statusHint ?: return@LaunchedEffect
        Toast.makeText(context, hint, Toast.LENGTH_SHORT).show()
        coordinator.clearStatusHint()
    }

    DisposableEffect(lifecycleOwner, coordinator) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action ?: return
                if (action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED ||
                    action == BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED
                ) {
                    val stateExtra = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                    if (stateExtra == BluetoothProfile.STATE_CONNECTED) {
                        coordinator.syncIfIdle(userInitiated = false)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val btOk = hasBtPermissions(context)
                notificationsGranted = hasNotifPermission(context)
                canPostPromoted = app.notifier.canPostPromoted()
                coordinator.onPermissionsUpdated(btOk)
                coordinator.refreshShizukuStatus()
                coordinator.refreshNotifications()
                if (btOk) coordinator.syncIfIdle(userInitiated = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    val fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val slideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PodThemeTokens.pageContainer),
    ) {
        AnimatedContent(
            targetState = currentRoute,
            transitionSpec = {
                val reverse = navigatingBack
                val enter = slideInHorizontally(slideSpec) { fullWidth ->
                    if (reverse) -fullWidth / 5 else fullWidth / 4
                } + fadeIn(fadeInSpec)
                val exit = slideOutHorizontally(slideSpec) { fullWidth ->
                    if (reverse) fullWidth / 4 else -fullWidth / 5
                } + fadeOut(fadeOutSpec)
                (enter togetherWith exit).using(SizeTransform(clip = false))
            },
            label = "route_transition",
        ) { route ->
            when (route) {
                AppRoute.OVERVIEW -> OverviewScreen(
                    state = uiState,
                    companion = companionState,
                    isAssociating = isAssociating,
                    onGrantPermissions = requestPermissions,
                    onOpenDevicePicker = openPicker,
                    onDisassociateAll = coordinator::disassociateAll,
                    onSyncNow = syncNow,
                    onDisconnect = coordinator::disconnectNow,
                    onSelectListeningMode = selectListeningMode,
                    onRenamePods = coordinator::renamePods,
                    onOpenEqualizer = openEqualizer,
                    onOpenAdaptiveStrength = openAdaptiveStrength,
                    onOpenStemControls = openStemControls,
                    onOpenCallControls = openCallControls,
                    onOpenMicrophone = openMicrophone,
                    onOpenHeadTracking = openHeadTracking,
                    onOpenAccessibility = openAccessibility,
                    onOpenHearingProtection = openHearingProtection,
                    onOpenHearingAid = openHearingAid,
                    onOpenVersionInfo = openVersionInfo,
                    onOpenSettings = openSettings,
                    onSetPersonalizedVolume = coordinator::setPersonalizedVolume,
                    onSetConversationAwareness = coordinator::setConversationAwareness,
                    onSetLoudSoundReduction = coordinator::setLoudSoundReduction,
                    onSetEarDetection = coordinator::setEarDetection,
                    onSetAutoPauseMedia = coordinator::setAutoPauseMedia,
                    onSetAutomaticConnection = coordinator::setAutomaticConnection,
                    onSetSleepDetection = coordinator::setSleepDetection,
                    onSetOptimizedChargeLimit = coordinator::setOptimizedChargeLimit,
                    onSetAllowOffOption = coordinator::setAllowOffOption,
                )

                AppRoute.SETTINGS -> SettingsScreen(
                    state = uiState,
                    companion = companionState,
                    notificationsGranted = notificationsGranted,
                    canPostPromoted = canPostPromoted,
                    onRequestNotificationPermission = requestNotifPerm,
                    onOpenLiveNotificationSettings = openLiveSettings,
                    onSetShowBatteryNotification = coordinator::setShowBatteryNotification,
                    onSetNotificationStyle = coordinator::setNotificationStyle,
                    onSetIncludeCaseInNotification = coordinator::setIncludeCaseInNotification,
                    onRequestShizukuPermission = coordinator::requestShizukuPermission,
                    onSetShizukuBatterySync = coordinator::setShizukuBatterySync,
                    onSetConversationAwarenessDuckVolume = coordinator::setConversationAwarenessDuckVolume,
                    onSetConversationAwarenessVolumePercent = coordinator::setConversationAwarenessVolumePercent,
                    onSetConversationAwarenessRelativeVolume = coordinator::setConversationAwarenessRelativeVolume,
                    onSetConversationAwarenessPauseMusic = coordinator::setConversationAwarenessPauseMusic,
                    onSetShowPhoneBatteryInWidget = coordinator::setShowPhoneBatteryInWidget,
                    onSetQsClickBehaviorDialog = coordinator::setQsClickBehaviorDialog,
                    onSetUseAlternateHeadTrackingPackets = coordinator::setUseAlternateHeadTrackingPackets,
                    onSetDisconnectWhenNotWearing = coordinator::setDisconnectWhenNotWearing,
                    onOpenDevicePicker = openPicker,
                    onDisassociate = coordinator::disassociate,
                    onOpenDiagnostics = openDiagnostics,
                    onResetToDefaults = coordinator::resetToDefaults,
                    onBack = goBack,
                )

                AppRoute.EQUALIZER -> EqualizerScreen(
                    state = uiState,
                    onSetCustomEq = coordinator::setCustomEq,
                    onSetAccommodations = coordinator::setAccommodations,
                    onBack = goBack,
                )

                AppRoute.ADAPTIVE_STRENGTH -> AdaptiveStrengthScreen(
                    state = uiState,
                    onSetAdaptiveAncStrength = coordinator::setAdaptiveAncStrength,
                    onBack = goBack,
                )

                AppRoute.STEM_CONTROLS -> StemControlsScreen(
                    state = uiState,
                    onToggleCycleBit = coordinator::toggleCycleModeBit,
                    onSetStemAction = coordinator::setStemAction,
                    onBack = goBack,
                )

                AppRoute.CALL_CONTROLS -> CallControlsScreen(
                    state = uiState,
                    onSetCallManagementFlipped = coordinator::setCallManagementFlipped,
                    onBack = goBack,
                )

                AppRoute.MICROPHONE -> MicrophoneScreen(
                    state = uiState,
                    onSetMicMode = coordinator::setMicMode,
                    onBack = goBack,
                )

                AppRoute.HEAD_TRACKING -> HeadTrackingScreen(
                    state = uiState,
                    motionFlow = coordinator.motionState,
                    onSetHeadGesturesEnabled = coordinator::setHeadGesturesEnabled,
                    onStartHeadTracking = startHeadTracking,
                    onStopHeadTracking = stopHeadTracking,
                    onResetOrientation = coordinator::resetHeadOrientation,
                    onStartGestureTest = coordinator::startGestureTest,
                    onStopGestureTest = coordinator::stopGestureTest,
                    onBack = goBack,
                )

                AppRoute.ACCESSIBILITY -> AccessibilityScreen(
                    state = uiState,
                    onSetDoubleClickInterval = coordinator::setDoubleClickInterval,
                    onSetClickHoldInterval = coordinator::setClickHoldInterval,
                    onSetVolumeSwipeEnabled = coordinator::setVolumeSwipeEnabled,
                    onSetVolumeSwipeInterval = coordinator::setVolumeSwipeInterval,
                    onSetChimeVolume = coordinator::setChimeVolume,
                    onSetInCaseTone = coordinator::setInCaseTone,
                    onSetSingleBudAnc = coordinator::setSingleBudAnc,
                    onSetPersonalizedVolume = coordinator::setPersonalizedVolume,
                    onSetConversationAwareness = coordinator::setConversationAwareness,
                    onOpenTransparency = openTransparency,
                    onOpenHearingProtection = openHearingProtection,
                    onOpenHearingAid = openHearingAid,
                    onBack = goBack,
                )

                AppRoute.TRANSPARENCY -> TransparencyScreen(
                    state = uiState,
                    onUpdateTransparency = coordinator::updateTransparency,
                    onBack = goBack,
                )

                AppRoute.HEARING_PROTECTION -> HearingProtectionScreen(
                    state = uiState,
                    onSetPpeEnabled = coordinator::setPpeEnabled,
                    onSetLoudSoundReduction = coordinator::setLoudSoundReduction,
                    onBack = goBack,
                )

                AppRoute.HEARING_AID -> HearingAidScreen(
                    state = uiState,
                    onSetHearingAidEnrolled = coordinator::setHearingAidEnrolled,
                    onSetHearingAidEnabled = coordinator::setHearingAidEnabled,
                    onUpdateHearingAid = coordinator::updateHearingAid,
                    onBack = goBack,
                )

                AppRoute.VERSION_INFO -> VersionInfoScreen(
                    state = uiState,
                    onBack = goBack,
                )

                AppRoute.DIAGNOSTICS -> {
                    LaunchedEffect(Unit) { TraceLog.sync() }
                    val logs by TraceLog.entries.collectAsStateWithLifecycle()
                    val copiedText = stringResource(R.string.remake_diagnostics_copied)
                    val copyDiagnostics = remember(context, coordinator, copiedText) {
                        {
                            val report = TraceLog.formatReport(
                                state = coordinator.state.value,
                                companion = coordinator.companionState.value,
                                hiddenApiReady = L2capChannel.isAccessible(),
                            )
                            context.getSystemService(ClipboardManager::class.java)
                                ?.setPrimaryClip(ClipData.newPlainText("Podlink Diagnostics", report))
                            Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                        }
                    }

                    DiagnosticsScreen(
                        logs = logs,
                        companion = companionState,
                        hiddenApiReady = L2capChannel.isAccessible(),
                        onCopyDiagnostics = copyDiagnostics,
                        onClearDiagnostics = TraceLog::clear,
                        onBack = goBack,
                    )
                }
            }
        }
    }

    if (showDevicePicker) {
        PairedDevicePickerSheet(
            candidates = bondedCandidates,
            isAssociating = isAssociating,
            onDismiss = { showDevicePicker = false },
            onSelectBonded = { candidate ->
                showDevicePicker = false
                isAssociating = true
                coordinator.associateMac(
                    macAddress = candidate.macAddress,
                    onChooser = { sender -> cdmLauncher.launch(IntentSenderRequest.Builder(sender).build()) },
                    onError = { err ->
                        isAssociating = false
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    },
                )
            },
            onScanAirPods = {
                showDevicePicker = false
                isAssociating = true
                coordinator.startDiscovery(
                    showAll = false,
                    onChooser = { sender -> cdmLauncher.launch(IntentSenderRequest.Builder(sender).build()) },
                    onError = { err ->
                        isAssociating = false
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    },
                )
            },
            onScanAll = {
                showDevicePicker = false
                isAssociating = true
                coordinator.startDiscovery(
                    showAll = true,
                    onChooser = { sender -> cdmLauncher.launch(IntentSenderRequest.Builder(sender).build()) },
                    onError = { err ->
                        isAssociating = false
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    },
                )
            },
        )
    }
}

private fun requiredBtPermissions(): Array<String> = arrayOf(
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.POST_NOTIFICATIONS,
)

private fun hasBtPermissions(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

private fun hasNotifPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
