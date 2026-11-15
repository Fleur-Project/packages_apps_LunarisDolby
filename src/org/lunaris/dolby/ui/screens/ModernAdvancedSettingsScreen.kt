/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package org.lunaris.dolby.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import org.lunaris.dolby.R
import org.lunaris.dolby.domain.models.DolbyUiState
import org.lunaris.dolby.ui.components.*
import org.lunaris.dolby.ui.viewmodel.DolbyViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModernAdvancedSettingsScreen(
    viewModel: DolbyViewModel,
    navController: NavController,
    bottomPadding: Dp = 0.dp,
) {
    val uiState by viewModel.uiState.collectAsState()

    val listState = rememberLazyListState()
    val showTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    Scaffold(
        topBar = {
            DolbyTopAppBar(
                title = stringResource(R.string.dolby_category_adv_settings),
                showTitle = showTitle || uiState !is DolbyUiState.Success,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is DolbyUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is DolbyUiState.Success -> {
                    ModernAdvancedSettingsContent(
                        state = state,
                        listState = listState,
                        viewModel = viewModel,
                        navController = navController,
                        bottomPadding = bottomPadding,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                is DolbyUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernAdvancedSettingsContent(
    state: DolbyUiState.Success,
    listState: LazyListState,
    viewModel: DolbyViewModel,
    navController: NavController,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp + bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            DolbyPageHeading(
                title = stringResource(R.string.dolby_category_adv_settings),
                subtitle = stringResource(R.string.dolby_advanced_summary),
            )
        }

        if (state.settings.enabled) {
            item {
                ModernSettingsCard(
                    title = stringResource(R.string.dolby_category_settings),
                    icon = Icons.Default.Tune,
                    badgePolygon = MaterialShapes.Cookie9Sided
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        EnhancerCard(
                            title = stringResource(R.string.dolby_bass_enhancer),
                            subtitle = stringResource(R.string.dolby_bass_enhancer_summary),
                            icon = Icons.Default.MusicNote,
                            checked = state.profileSettings.bassLevel > 0,
                            onCheckedChange = { enabled ->
                                if (enabled && state.profileSettings.bassLevel == 0) {
                                    viewModel.setBassLevel(50)
                                } else if (!enabled) {
                                    viewModel.setBassLevel(0)
                                }
                            }
                        ) {
                            ModernSettingSelector(
                                title = stringResource(R.string.dolby_bass_curve),
                                currentValue = state.profileSettings.bassCurve,
                                entries = R.array.dolby_bass_curve_entries,
                                values = R.array.dolby_bass_curve_values,
                                onValueChange = { viewModel.setBassCurve(it) },
                                icon = Icons.Default.Equalizer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ModernSettingSlider(
                                title = stringResource(R.string.dolby_bass_level),
                                value = state.profileSettings.bassLevel,
                                onValueChange = { viewModel.setBassLevel(it.toInt()) },
                                valueRange = 0f..100f,
                                steps = 19,
                                valueLabel = { "$it%" }
                            )
                        }

                        EnhancerCard(
                            title = stringResource(R.string.dolby_mid_enhancer),
                            subtitle = stringResource(R.string.dolby_mid_enhancer_summary),
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            checked = state.profileSettings.midLevel > 0,
                            onCheckedChange = { enabled ->
                                if (enabled && state.profileSettings.midLevel == 0) {
                                    viewModel.setMidLevel(40)
                                } else if (!enabled) {
                                    viewModel.setMidLevel(0)
                                }
                            }
                        ) {
                            ModernSettingSlider(
                                title = stringResource(R.string.dolby_mid_level),
                                value = state.profileSettings.midLevel,
                                onValueChange = { viewModel.setMidLevel(it.toInt()) },
                                valueRange = 0f..100f,
                                steps = 19,
                                valueLabel = { "$it%" }
                            )
                        }

                        EnhancerCard(
                            title = stringResource(R.string.dolby_treble_enhancer),
                            subtitle = stringResource(R.string.dolby_treble_enhancer_summary),
                            icon = Icons.Default.GraphicEq,
                            checked = state.profileSettings.trebleLevel > 0,
                            onCheckedChange = { enabled ->
                                if (enabled && state.profileSettings.trebleLevel == 0) {
                                    viewModel.setTrebleLevel(30)
                                } else if (!enabled) {
                                    viewModel.setTrebleLevel(0)
                                }
                            }
                        ) {
                            ModernSettingSlider(
                                title = stringResource(R.string.dolby_treble_level),
                                value = state.profileSettings.trebleLevel,
                                onValueChange = { viewModel.setTrebleLevel(it.toInt()) },
                                valueRange = 0f..100f,
                                steps = 19,
                                valueLabel = { "$it%" }
                            )
                        }
                    }
                }
            }
            
            item {
                ModernSettingsCard(
                    title = stringResource(R.string.dolby_volume_leveler),
                    icon = Icons.AutoMirrored.Filled.VolumeDown,
                    badgePolygon = MaterialShapes.Sunny
                ) {
                    ModernSettingSwitch(
                        title = stringResource(R.string.dolby_volume_leveler),
                        subtitle = stringResource(R.string.dolby_volume_leveler_summary),
                        checked = state.settings.volumeLevelerEnabled,
                        onCheckedChange = { viewModel.setVolumeLeveler(it) },
                        icon = Icons.Default.BarChart
                    )
                }
            }
            
            if (state.settings.currentProfile != 0) {
                item {
                    ModernSettingsCard(
                        title = stringResource(R.string.dolby_adv_surround_virtualizer),
                        icon = Icons.Default.Headphones,
                        badgePolygon = MaterialShapes.Clover4Leaf
                    ) {
                        if (state.isOnSpeaker) {
                            ModernSettingSwitch(
                                title = stringResource(R.string.dolby_spk_virtualizer),
                                subtitle = stringResource(R.string.dolby_spk_virtualizer_summary),
                                checked = state.profileSettings.speakerVirtualizerEnabled,
                                onCheckedChange = { viewModel.setSpeakerVirtualizer(it) },
                                icon = Icons.Default.Speaker
                            )
                        } else {
                            EnhancerCard(
                                title = stringResource(R.string.dolby_hp_virtualizer),
                                subtitle = stringResource(R.string.dolby_hp_virtualizer_summary),
                                icon = Icons.Default.Headphones,
                                checked = state.profileSettings.headphoneVirtualizerEnabled,
                                onCheckedChange = { viewModel.setHeadphoneVirtualizer(it) }
                            ) {
                                ModernSettingSlider(
                                    title = stringResource(R.string.dolby_hp_virtualizer_dolby_strength),
                                    value = state.profileSettings.stereoWideningAmount,
                                    onValueChange = { viewModel.setStereoWidening(it.toInt()) },
                                    valueRange = 4f..64f,
                                    steps = 59,
                                    valueLabel = { "$it" }
                                )
                            }
                        }
                    }
                }
                
                item {
                    ModernSettingsCard(
                        title = stringResource(R.string.dolby_adv_dialogue_enhancement),
                        icon = Icons.Default.RecordVoiceOver,
                        badgePolygon = MaterialShapes.Pill
                    ) {
                        EnhancerCard(
                            title = stringResource(R.string.dolby_dialogue_enhancer),
                            subtitle = stringResource(R.string.dolby_dialogue_enhancer_summary),
                            icon = Icons.Default.RecordVoiceOver,
                            checked = state.profileSettings.dialogueEnhancerEnabled,
                            onCheckedChange = { viewModel.setDialogueEnhancer(it) }
                        ) {
                            val levelFormat = stringResource(R.string.dolby_adv_level_format)
                            ModernSettingSlider(
                                title = stringResource(R.string.dolby_dialogue_enhancer_dolby_strength),
                                value = state.profileSettings.dialogueEnhancerAmount,
                                onValueChange = { viewModel.setDialogueEnhancerAmount(it.toInt()) },
                                valueRange = 1f..12f,
                                steps = 10,
                                valueLabel = { levelFormat.format(it) }
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val infoMorph = remember {
                            Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided)
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(MorphShape(infoMorph, 1f))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.dolby_off),
                                style = MaterialTheme.typography.titleMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.dolby_adv_settings_footer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancerCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val motion = MaterialTheme.motionScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            ModernSettingSwitch(
                title = title,
                subtitle = subtitle,
                checked = checked,
                onCheckedChange = onCheckedChange,
                icon = icon
            )
            AnimatedVisibility(
                visible = checked,
                enter = expandVertically(animationSpec = motion.defaultSpatialSpec()) + fadeIn(animationSpec = motion.fastEffectsSpec()),
                exit = shrinkVertically(animationSpec = motion.defaultSpatialSpec()) + fadeOut(animationSpec = motion.fastEffectsSpec())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    content()
                }
            }
        }
    }
}
