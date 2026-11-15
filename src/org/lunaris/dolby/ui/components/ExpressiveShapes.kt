/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toPath
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class MorphShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = morph.toPath(progress = progress, path = Path())
        val matrix = Matrix().apply {
            scale(size.width, size.height)
        }
        path.transform(matrix)
        val center = Offset(size.width / 2f, size.height / 2f)
        val pathCenter = path.getBounds().center
        path.translate(center - pathCenter)
        return Outline.Generic(path)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object DolbyExpressiveShapes {
    fun profilePolygonFor(profileId: Int): RoundedPolygon = when (profileId) {
        0 -> MaterialShapes.Sunny
        1 -> MaterialShapes.Clover4Leaf
        2 -> MaterialShapes.Cookie9Sided
        3 -> MaterialShapes.Boom
        4 -> MaterialShapes.Pill
        5 -> MaterialShapes.SoftBurst
        else -> MaterialShapes.PuffyDiamond
    }
}
