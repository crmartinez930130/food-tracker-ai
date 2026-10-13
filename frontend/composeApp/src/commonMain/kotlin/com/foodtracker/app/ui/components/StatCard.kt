package com.foodtracker.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A figure with its label on a tinted container, for stat grids (Dollop / Heliotrope style). */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor),
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            BigNumber(
                value = value,
                unit = unit,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                unitStyle = MaterialTheme.typography.labelMedium,
                unitColor = LocalContentColor.current,
            )
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
