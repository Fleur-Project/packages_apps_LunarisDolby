/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package org.lunaris.dolby.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import kotlinx.coroutines.launch
import org.lunaris.dolby.R
import org.lunaris.dolby.utils.HapticFeedbackHelper
import org.lunaris.dolby.utils.rememberHapticFeedback

@Composable
fun ModernIeqSelector(
    currentPreset: Int,
    onPresetChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = stringArrayResource(R.array.dolby_ieq_entries)
    val values = stringArrayResource(R.array.dolby_ieq_values)
    val options = entries.mapIndexedNotNull { index, title ->
        values.getOrNull(index)?.toIntOrNull()?.let { it to title }
    }
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    val select: (Int) -> Unit = { value ->
        if (value != currentPreset) {
            scope.launch { haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK) }
            onPresetChange(value)
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.dolby_ieq),
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val readableWidth = maxWidth / LocalDensity.current.fontScale
            val columns = when {
                readableWidth >= 560.dp -> 4
                readableWidth >= 240.dp -> 2
                else -> 1
            }
            Column(
                Modifier.fillMaxWidth().selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                options.chunked(columns).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        row.forEachIndexed { index, (value, title) ->
                            key(value) {
                                val interactionSource = remember { MutableInteractionSource() }
                                val selected = currentPreset == value
                                val isFirst = if (columns == 1) value == options.first().first else index == 0
                                val isLast = if (columns == 1) value == options.last().first else index == row.lastIndex
                                IeqOption(
                                    title = title,
                                    icon = when (value) {
                                        0 -> Icons.Rounded.PowerOff
                                        1 -> Icons.Rounded.GraphicEq
                                        2 -> Icons.Rounded.Balance
                                        3 -> Icons.Rounded.Whatshot
                                        else -> Icons.Rounded.GraphicEq
                                    },
                                    selected = selected,
                                    isFirst = isFirst,
                                    isLast = isLast,
                                    interactionSource = interactionSource,
                                    onClick = { select(value) },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Text(
            text = stringResource(when (currentPreset) {
                1 -> R.string.dolby_ieq_detailed_summary
                2 -> R.string.dolby_ieq_balanced_summary
                3 -> R.string.dolby_ieq_warm_summary
                else -> R.string.dolby_ieq_off_summary
            }),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IeqOption(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = motion.fastSpatialSpec(),
        label = "ieq_option_press",
    )
    
    val topStart by animateDpAsState(
        targetValue = if (selected) 28.dp else if (isFirst) 24.dp else 8.dp,
        animationSpec = motion.fastSpatialSpec(),
        label = "ieq_corner_ts",
    )
    val bottomStart by animateDpAsState(
        targetValue = if (selected) 28.dp else if (isFirst) 24.dp else 8.dp,
        animationSpec = motion.fastSpatialSpec(),
        label = "ieq_corner_bs",
    )
    val topEnd by animateDpAsState(
        targetValue = if (selected) 28.dp else if (isLast) 24.dp else 8.dp,
        animationSpec = motion.fastSpatialSpec(),
        label = "ieq_corner_te",
    )
    val bottomEnd by animateDpAsState(
        targetValue = if (selected) 28.dp else if (isLast) 24.dp else 8.dp,
        animationSpec = motion.fastSpatialSpec(),
        label = "ieq_corner_be",
    )
    val shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)

    val container by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else colors.surfaceContainerLow,
        animationSpec = motion.fastEffectsSpec(),
        label = "ieq_option_container",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        animationSpec = motion.fastEffectsSpec(),
        label = "ieq_option_content",
    )

    val badgeMorph = remember {
        Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided)
    }
    val badgeProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = motion.fastSpatialSpec(),
        label = "ieq_badge_morph",
    )
    val badgeColor by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else colors.surfaceContainerHigh,
        animationSpec = motion.fastEffectsSpec(),
        label = "ieq_badge_bg",
    )
    val badgeContentColor by animateColorAsState(
        targetValue = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
        animationSpec = motion.fastEffectsSpec(),
        label = "ieq_badge_tint",
    )

    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { role = Role.RadioButton },
        shape = shape,
        color = container,
        contentColor = content,
        interactionSource = interactionSource,
    ) {
        Column(
            Modifier.heightIn(min = 120.dp).padding(horizontal = 14.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MorphShape(badgeMorph, badgeProgress))
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(
                    targetState = selected,
                    animationSpec = motion.fastEffectsSpec(),
                    label = "ieq_icon_crossfade",
                ) { isSelected ->
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.Check else icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = badgeContentColor,
                    )
                }
            }
            Text(
                text = title,
                style = if (selected) MaterialTheme.typography.titleMediumEmphasized
                    else MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
