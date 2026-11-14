/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package org.lunaris.dolby.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SpatialAudio
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.lunaris.dolby.R
import org.lunaris.dolby.ui.components.DolbyPageHeading
import org.lunaris.dolby.ui.components.DolbyTopAppBar
import org.lunaris.dolby.ui.components.squishable
import org.lunaris.dolby.utils.HapticFeedbackHelper
import org.lunaris.dolby.utils.rememberHapticFeedback
import org.lunaris.dolby.viper.effect.EffectState
import org.lunaris.dolby.viper.ui.screens.debug.DebugLogDialog
import org.lunaris.dolby.viper.ui.screens.device.DeviceDialog
import org.lunaris.dolby.viper.ui.screens.main.*
import org.lunaris.dolby.viper.ui.components.ConfirmDialog
import org.lunaris.dolby.viper.ui.screens.preset.PresetDialog
import org.lunaris.dolby.viper.ui.screens.settings.ExcludedAppsDialog
import org.lunaris.dolby.viper.ui.screens.settings.SettingsDialog
import org.lunaris.dolby.viper.ui.screens.status.DriverStatusDialog
import org.lunaris.dolby.viper.ui.screens.main.DriverStatus
import org.lunaris.dolby.viper.ui.viewmodel.ViperViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ModernV4AScreen(
    viewModel: ViperViewModel,
    navController: NavController,
    bottomPadding: Dp = 0.dp,
) {
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.saveSettingsOnBackground()
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val presets by viewModel.presetList.collectAsStateWithLifecycle()
    val deviceSettings by viewModel.deviceSettingsList.collectAsStateWithLifecycle()
    val driverStatus by viewModel.driverStatus.collectAsStateWithLifecycle()
    val autoStart by viewModel.autoStartEnabled.collectAsStateWithLifecycle()
    val globalMode by viewModel.globalModeEnabled.collectAsStateWithLifecycle()
    val aidlMode by viewModel.aidlModeEnabled.collectAsStateWithLifecycle()
    val debugMode by viewModel.debugModeEnabled.collectAsStateWithLifecycle()
    val excludedApps by viewModel.excludedApps.collectAsStateWithLifecycle()

    var showPresetDialog by remember { mutableStateOf(false) }
    var showResetAllConfirm by remember(state.activeDeviceId) { mutableStateOf(false) }
    var showDriverStatusDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDebugLog by remember { mutableStateOf(false) }
    var showDeviceDialog by remember { mutableStateOf(false) }
    var showExcludedAppsDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val showTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    val context = LocalContext.current
    val appVersionName =
        remember {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
            } catch (_: Exception) {
                ""
            }
        }

    val clearAllProgressStr = stringResource(R.string.preset_clear_all_progress)
    val clearedStr = stringResource(R.string.preset_cleared)

    if (showExcludedAppsDialog) {
        ExcludedAppsDialog(
            excludedApps = excludedApps,
            onToggle = viewModel::setAppExcluded,
            loadInstalledApps = viewModel::loadInstalledApps,
            onDismiss = { showExcludedAppsDialog = false },
        )
    }

    if (showPresetDialog) {
        PresetDialog(
            presets = presets,
            onSave = viewModel::savePreset,
            onLoad = { id ->
                viewModel.loadPreset(id)
                showPresetDialog = false
            },
            onDelete = viewModel::deletePreset,
            onRename = viewModel::renamePreset,
            onUpdate = viewModel::updatePreset,
            onClearAll = {
                viewModel.clearAllPresets(
                    notificationTitle = clearAllProgressStr,
                    successStr = clearedStr,
                ) { count ->
                    Toast.makeText(context, "$clearedStr: $count", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showPresetDialog = false },
        )
    }

    if (showResetAllConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.reset_all_effects_title),
            body = stringResource(R.string.reset_all_effects_confirm, state.activeDeviceName),
            confirmLabel = stringResource(R.string.action_reset),
            destructive = true,
            onConfirm = {
                viewModel.resetAllEffects()
                showResetAllConfirm = false
            },
            onDismiss = { showResetAllConfirm = false },
        )
    }

    if (showDriverStatusDialog) {
        LaunchedEffect(Unit) {
            while (true) {
                viewModel.queryDriverStatus()
                delay(500.milliseconds)
            }
        }
        DriverStatusDialog(
            driverStatus = driverStatus,
            onDismiss = { showDriverStatusDialog = false },
        )
    }

    if (showDebugLog) {
        DebugLogDialog(
            onDisableDebug = {
                viewModel.disableDebugMode()
                showDebugLog = false
            },
            onDismiss = { showDebugLog = false },
        )
    }

    if (showDeviceDialog) {
        DeviceDialog(
            devices = deviceSettings,
            activeDeviceId = state.activeDeviceId,
            onRename = viewModel::renameDevice,
            onLoad = viewModel::loadDevicePreset,
            onUpdate = viewModel::saveDevicePreset,
            onDelete = viewModel::deleteDeviceSettings,
            onDismiss = { showDeviceDialog = false },
        )
    }

    val importSuccessStr = stringResource(R.string.import_success)
    val importFailedStr = stringResource(R.string.import_failed)
    val importPresetStr = stringResource(R.string.settings_import_preset)
    val importPresetLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris ->
            if (uris.isNotEmpty()) {
                viewModel.importPresetFiles(uris, notificationTitle = importPresetStr, successStr = importSuccessStr) { success ->
                    val msg = if (success) importSuccessStr else importFailedStr
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

    val importKernelStr = stringResource(R.string.settings_import_kernel)
    val importKernelLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris ->
            if (uris.isNotEmpty()) {
                viewModel.importKernels(uris, notificationTitle = importKernelStr, successStr = importSuccessStr) { success ->
                    val msg = if (success) importSuccessStr else importFailedStr
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

    val importVdcStr = stringResource(R.string.settings_import_vdc)
    val importVdcLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris ->
            if (uris.isNotEmpty()) {
                viewModel.importVdcs(uris, notificationTitle = importVdcStr, successStr = importSuccessStr) { success ->
                    val msg = if (success) importSuccessStr else importFailedStr
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

    if (showSettingsDialog) {
        LaunchedEffect(Unit) { viewModel.queryDriverStatus() }
        SettingsDialog(
            autoStartEnabled = autoStart,
            globalModeEnabled = globalMode,
            aidlModeActive = aidlMode,
            debugModeEnabled = debugMode,
            onGlobalModeChanged = viewModel::toggleGlobalMode,
            onOpenExcludedApps = { showExcludedAppsDialog = true },
            driverStatus = driverStatus,
            appVersionName = appVersionName,
            onAutoStartChanged = viewModel::toggleAutoStart,
            onImportPreset = { importPresetLauncher.launch(arrayOf("application/json")) },
            onImportKernel = {
                importKernelLauncher.launch(
                    arrayOf(
                        "audio/*",
                        "application/octet-stream",
                        "*/*",
                    ),
                )
            },
            onDebugUnlocked = viewModel::enableDebugMode,
            onImportVdc = { importVdcLauncher.launch(arrayOf("*/*")) },
            onDismiss = { showSettingsDialog = false },
        )
    }

    Scaffold(
        topBar = {
            DolbyTopAppBar(
                title = stringResource(R.string.v4a_title),
                showTitle = showTitle,
                actions = {
                    if (debugMode) {
                        IconButton(onClick = { showDebugLog = true }) {
                            Icon(
                                Icons.Default.BugReport,
                                contentDescription = stringResource(R.string.debug_log_title),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    IconButton(onClick = { showDeviceDialog = true }) {
                        Icon(
                            Icons.Filled.SpeakerGroup,
                            contentDescription = stringResource(R.string.menu_devices),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { showDriverStatusDialog = true }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.menu_driver_status),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { showPresetDialog = true }) {
                        Icon(
                            Icons.Default.LibraryMusic,
                            contentDescription = stringResource(R.string.menu_presets),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = { showResetAllConfirm = true },
                        enabled = state.activeDeviceId.isNotEmpty(),
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = stringResource(R.string.action_reset_all_effects),
                            tint = if (state.activeDeviceId.isNotEmpty()) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.menu_settings),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { paddingValues ->
        val targetAlpha = if (state.masterEnable) 1f else 0.45f
        val alpha by animateFloatAsState(
            targetValue = targetAlpha,
            animationSpec = tween(durationMillis = 200),
            label = "effectListAlpha",
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = listState,
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 12.dp,
                end = 20.dp,
                bottom = 12.dp + bottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                DolbyPageHeading(
                    title = stringResource(R.string.v4a_title),
                    subtitle = stringResource(R.string.v4a_subtitle),
                )
            }

            item {
                V4AMainCard(
                    enabled = state.masterEnable,
                    activeDeviceName = state.activeDeviceName,
                    driverStatus = driverStatus,
                    onEnabledChange = { viewModel.setMasterEnabled(it) },
                )
            }

            // Virtualized effect section items
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { MasterLimiterRows(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { PlaybackGainSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { LUFSTargetingSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { MultibandCompressorSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { FetCompressorSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { DdcSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { SpectrumExtensionSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { EqualizerSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { DynamicEqSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { ConvolverSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { FieldSurroundSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { DiffSurroundSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { StereoImagerSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { HeadphoneSurroundSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { ReverberationSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { DynamicSystemSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { TubeSimulatorSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { PsychoacousticBassSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { ViperBassSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { ViperBassMonoSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { ViperClaritySection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { AuditoryProtectionSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { AnalogXSection(state, viewModel) } }
            item { Box(Modifier.graphicsLayer { this.alpha = alpha }) { SpeakerOptSection(state, viewModel) } }
        }
    }
}

@Composable
fun V4AMainCard(
    enabled: Boolean,
    activeDeviceName: String,
    driverStatus: DriverStatus,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()

    val containerColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(durationMillis = 200),
        label = "v4a_master_container",
    )
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .squishable(),
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier
                .toggleable(
                    value = enabled,
                    role = Role.Switch,
                    onValueChange = {
                        scope.launch {
                            haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                        }
                        onEnabledChange(it)
                    },
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_qs_master),
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.onPrimary
                            else contentColor,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.v4a_title),
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        color = contentColor,
                    )

                    // Active audio device display
                    if (activeDeviceName.isNotEmpty()) {
                        val deviceIndicatorColor = if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(deviceIndicatorColor)
                            }
                            Text(
                                text = activeDeviceName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (enabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }

                    // Driver status text
                    Text(
                        text = if (driverStatus.installed) {
                            stringResource(R.string.v4a_driver_connected)
                        } else {
                            stringResource(R.string.v4a_driver_disconnected)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (driverStatus.installed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(if (enabled) R.string.dolby_on else R.string.dolby_off),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = contentColor,
                )

                Switch(
                    checked = enabled,
                    onCheckedChange = null,
                    thumbContent = {
                        Icon(
                            if (enabled) Icons.Rounded.Check else Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }
    }
}
