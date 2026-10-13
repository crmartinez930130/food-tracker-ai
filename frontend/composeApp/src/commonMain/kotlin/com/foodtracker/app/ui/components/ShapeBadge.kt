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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).background(containerColor, shape),
    ) {
        Text(text, style = textStyle, color = contentColor, maxLines = 1)
    }
}
