package com.foodtracker.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.ic_check
import org.jetbrains.compose.resources.painterResource

private val InnerCorner = CornerSize(8.dp)
private val FullCorner = CornerSize(50)

private fun CornerKind.size() = if (this == CornerKind.FULL) FullCorner else InnerCorner

/**
 * Single-choice buttons joined into one group (M3 Expressive "connected" style): round outer
 * edges, small inner ones, and the selected button a filled pill, with a check when [showCheck].
 * [selectedIndex] -1 means nothing is selected. Our own because material3's ButtonGroup is
 * internal in Compose Multiplatform 1.9.
 */
@Composable
fun ConnectedButtonGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showCheck: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.fillMaxWidth().height(48.dp).selectableGroup(),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val corners = connectedCorners(index, options.size, selected)
            val interaction = remember { MutableInteractionSource() }
            Surface(
                shape = RoundedCornerShape(
                    topStart = corners.topStart.size(),
                    topEnd = corners.topEnd.size(),
                    bottomEnd = corners.bottomEnd.size(),
                    bottomStart = corners.bottomStart.size(),
                ),
                color = if (selected) colors.primary else colors.secondaryContainer,
                contentColor = if (selected) colors.onPrimary else colors.onSecondaryContainer,
                selected = selected,
                onClick = { onSelect(index) },
                enabled = enabled,
                interactionSource = interaction,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .pressBounce(interaction)
                    .semantics { role = Role.RadioButton },
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    if (selected && showCheck) {
                        Icon(painterResource(Res.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 2.dp))
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(start = if (selected && showCheck) 6.dp else 0.dp),
                    )
                }
            }
        }
    }
}
