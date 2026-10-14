package com.foodtracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

/** A short value inside an expressive shape, like Greenwave's "87" score cookie. */
@Composable
fun ShapeBadge(
    text: String,
    modifier: Modifier = Modifier,
    polygon: RoundedPolygon = ExpressiveShapes.cookie9,
    size: Dp = 64.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
    textStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
) {
    val shape = remember(polygon) { PolygonShape(polygon) }
    ShapeBadge(text, shape, modifier, size, containerColor, contentColor, textStyle)
}

/** The calorie badge's two shapes, cookie (in progress) and sunburst (goal reached). */
private val goalMorph = Morph(ExpressiveShapes.cookie9, ExpressiveShapes.sunny)

/**
 * A [ShapeBadge] between the cookie and the sunburst: [morphProgress] 0 is the cookie, 1 the
 * sunburst (see badgeShapeFor); animate it for the goal-reached morph.
 */
@Composable
fun MorphingShapeBadge(
    text: String,
    morphProgress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
    textStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
) {
    ShapeBadge(text, MorphShape(goalMorph, morphProgress), modifier, size, containerColor, contentColor, textStyle)
}

@Composable
private fun ShapeBadge(
    text: String,
    shape: Shape,
    modifier: Modifier,
    size: Dp,
    containerColor: Color,
    contentColor: Color,
    textStyle: TextStyle,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).background(containerColor, shape),
    ) {
        Text(text, style = textStyle, color = contentColor, maxLines = 1)
    }
}
