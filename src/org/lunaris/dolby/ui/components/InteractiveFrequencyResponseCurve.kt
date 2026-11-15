/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package org.lunaris.dolby.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.lunaris.dolby.domain.models.BandGain
import org.lunaris.dolby.utils.HapticFeedbackHelper
import org.lunaris.dolby.utils.rememberHapticFeedback

@Composable
fun InteractiveFrequencyResponseCurve(
    bandGains: List<BandGain>,
    onBandGainChange: (index: Int, newGain: Int) -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isEditable: Boolean = true
) {
    val motion = MaterialTheme.motionScheme
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceContainerHighest = MaterialTheme.colorScheme.surfaceContainerHighest

    val plotInkColor = if (isActive) onPrimaryContainerColor else onSurfaceVariantColor
    val curveColor = if (isActive) primaryColor else secondaryColor
    
    val backgroundColor = if (isActive) {
        primaryContainerColor
    } else {
        surfaceContainerHighest
    }

    val borderColor = if (isActive) {
        primaryColor
    } else if (!isEditable) {
        errorColor
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    
    val borderWidth = if (isActive) 2.dp else 1.dp
    
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var controlPoints by remember { mutableStateOf(bandGains.map { it.gain }) }
    var lastHapticStep by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(bandGains) {
        if (draggedIndex == null) {
            controlPoints = bandGains.map { it.gain }
        }
    }
    
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.large)
                .background(backgroundColor)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = MaterialTheme.shapes.large
                )
                .pointerInput(isEditable) {
                    if (isEditable) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val width = size.width
                                val height = size.height
                                val stepX = width / (bandGains.size - 1).toFloat()
                                
                                var closestIndex = -1
                                var closestDistance = Float.MAX_VALUE
                                
                                bandGains.forEachIndexed { index, _ ->
                                    val x = index * stepX
                                    val normalizedGain = (controlPoints[index] / 150f).coerceIn(-1f, 1f)
                                    val y = height / 2 - (normalizedGain * height / 2 * 0.85f)
                                    
                                    val distance = kotlin.math.sqrt(
                                        (offset.x - x) * (offset.x - x) + 
                                        (offset.y - y) * (offset.y - y)
                                    )
                                    
                                    if (distance < closestDistance && distance < 120f) {
                                        closestDistance = distance
                                        closestIndex = index
                                    }
                                }
                                
                                if (closestIndex != -1) {
                                    draggedIndex = closestIndex
                                    lastHapticStep = controlPoints[closestIndex] / 5
                                    scope.launch {
                                        haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                draggedIndex?.let { index ->
                                    val height = size.height
                                    val centerY = height / 2
                                    val y = change.position.y
                                    val normalizedGain = ((centerY - y) / (height / 2 * 0.85f)).coerceIn(-1f, 1f)
                                    val newGain = (normalizedGain * 150).toInt().coerceIn(-150, 150)
                                    
                                    if (controlPoints[index] != newGain) {
                                        val step = newGain / 5
                                        if (step != lastHapticStep) {
                                            scope.launch {
                                                if (newGain in -2..2) {
                                                    haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                                                } else {
                                                    haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.TEXTURE_TICK)
                                                }
                                            }
                                            lastHapticStep = step
                                        }
                                        controlPoints = controlPoints.toMutableList().apply {
                                            this[index] = newGain
                                        }
                                    }
                                    change.consume()
                                }
                            },
                            onDragEnd = {
                                draggedIndex?.let { index ->
                                    onBandGainChange(index, controlPoints[index])
                                    scope.launch {
                                        haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.TICK)
                                    }
                                }
                                draggedIndex = null
                            },
                            onDragCancel = {
                                draggedIndex = null
                            }
                        )
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2
            val gridColor = plotInkColor.copy(alpha = 0.18f)
            val gridVerticalColor = plotInkColor.copy(alpha = 0.12f)

            drawLine(
                color = plotInkColor.copy(alpha = 0.5f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 2.5f
            )

            for (i in 1..4) {
                val y = (height / 5) * i
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }
            
            val stepX = width / (bandGains.size - 1)
            bandGains.forEachIndexed { index, _ ->
                val x = index * stepX
                drawLine(
                    color = gridVerticalColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
            }
            
            if (bandGains.isNotEmpty() && controlPoints.isNotEmpty()) {
                val path = Path()
                
                controlPoints.forEachIndexed { index, gain ->
                    val x = index * stepX
                    val normalizedGain = (gain / 150f).coerceIn(-1f, 1f)
                    val y = centerY - (normalizedGain * centerY * 0.85f)
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        val prevX = (index - 1) * stepX
                        val prevGain = controlPoints[index - 1]
                        val prevNormalizedGain = (prevGain / 150f).coerceIn(-1f, 1f)
                        val prevY = centerY - (prevNormalizedGain * centerY * 0.85f)
                        
                        val cpX1 = prevX + stepX * 0.4f
                        val cpY1 = prevY
                        val cpX2 = x - stepX * 0.4f
                        val cpY2 = y
                        
                        path.cubicTo(cpX1, cpY1, cpX2, cpY2, x, y)
                    }
                }
                
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            curveColor.copy(alpha = if (isActive) 0.38f else 0.22f),
                            curveColor.copy(alpha = 0.02f)
                        )
                    )
                )

                drawPath(
                    path = path,
                    color = curveColor,
                    style = Stroke(
                        width = if (isActive) 5.5f else 4.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                
                controlPoints.forEachIndexed { index, gain ->
                    val x = index * stepX
                    val normalizedGain = (gain / 150f).coerceIn(-1f, 1f)
                    val y = centerY - (normalizedGain * centerY * 0.85f)
                    
                    val isBeingDragged = draggedIndex == index
                    
                    if (isBeingDragged) {
                        drawCircle(
                            color = curveColor.copy(alpha = 0.22f),
                            radius = 28f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = curveColor.copy(alpha = 0.45f),
                            radius = 18f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = backgroundColor,
                            radius = 12f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = curveColor,
                            radius = 8f,
                            center = Offset(x, y)
                        )
                    } else {
                        drawCircle(
                            color = curveColor.copy(alpha = 0.2f),
                            radius = 11f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = backgroundColor,
                            radius = 8f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = curveColor,
                            radius = 5.5f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            bandGains.forEach { bandGain ->
                Text(
                    text = if (bandGain.frequency >= 1000) {
                        "${bandGain.frequency / 1000}k"
                    } else {
                        "${bandGain.frequency}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = plotInkColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "+15",
                style = MaterialTheme.typography.labelSmallEmphasized,
                color = plotInkColor,
                fontSize = 10.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Text(
                text = "0",
                style = MaterialTheme.typography.labelSmallEmphasized,
                color = plotInkColor,
                fontSize = 10.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Text(
                text = "-15",
                style = MaterialTheme.typography.labelSmallEmphasized,
                color = plotInkColor,
                fontSize = 10.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
        
        AnimatedVisibility(
            visible = draggedIndex != null,
            enter = fadeIn(motion.fastEffectsSpec()) + scaleIn(motion.fastSpatialSpec()),
            exit = fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastSpatialSpec()),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        ) {
            draggedIndex?.let { index ->
                val gain = controlPoints.getOrNull(index) ?: 0
                val gainDb = gain / 10f
                val tooltipBg by animateColorAsState(
                    targetValue = when {
                        gain > 0 -> MaterialTheme.colorScheme.primary
                        gain < 0 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    animationSpec = motion.fastEffectsSpec(),
                    label = "tooltip_bg"
                )
                val tooltipTextColor by animateColorAsState(
                    targetValue = when {
                        gain != 0 -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    animationSpec = motion.fastEffectsSpec(),
                    label = "tooltip_text_color"
                )
                Surface(
                    shape = CircleShape,
                    color = tooltipBg,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "${if (gainDb > 0) "+" else ""}%.1f dB".format(gainDb),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        color = tooltipTextColor,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
