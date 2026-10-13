package com.foodtracker.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.foodtracker.app.ui.theme.FoodTrackerTheme

/** A large figure with a small unit after it, baseline-aligned: "1.240 kcal", "4.4 km". */
@Composable
fun BigNumber(
    value: String,
    unit: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = FoodTrackerTheme.typeTokens.emphasizedNumber,
    unitStyle: TextStyle = FoodTrackerTheme.typeTokens.numberUnit,
    color: Color = LocalContentColor.current,
    unitColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(value, style = style, color = color, maxLines = 1, modifier = Modifier.alignByBaseline())
        if (unit != null) {
            Text(
                unit,
                style = unitStyle,
                color = unitColor,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.alignByBaseline().padding(start = 4.dp),
            )
        }
    }
}
