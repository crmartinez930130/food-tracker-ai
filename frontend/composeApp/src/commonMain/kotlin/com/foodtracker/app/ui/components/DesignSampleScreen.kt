package com.foodtracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.action_back
import food_tracker_frontend.composeapp.generated.resources.design_sample_badges
import food_tracker_frontend.composeapp.generated.resources.design_sample_bar
import food_tracker_frontend.composeapp.generated.resources.design_sample_body
import food_tracker_frontend.composeapp.generated.resources.design_sample_colors
import food_tracker_frontend.composeapp.generated.resources.design_sample_display
import food_tracker_frontend.composeapp.generated.resources.design_sample_headline
import food_tracker_frontend.composeapp.generated.resources.design_sample_loading
import food_tracker_frontend.composeapp.generated.resources.design_sample_numbers
import food_tracker_frontend.composeapp.generated.resources.design_sample_stat_average
import food_tracker_frontend.composeapp.generated.resources.design_sample_stat_streak
import food_tracker_frontend.composeapp.generated.resources.design_sample_stat_water
import food_tracker_frontend.composeapp.generated.resources.design_sample_stats
import food_tracker_frontend.composeapp.generated.resources.design_sample_title
import food_tracker_frontend.composeapp.generated.resources.design_sample_type
import food_tracker_frontend.composeapp.generated.resources.ic_arrow_back
import food_tracker_frontend.composeapp.generated.resources.macro_carbs
import food_tracker_frontend.composeapp.generated.resources.macro_fat
import food_tracker_frontend.composeapp.generated.resources.macro_protein
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Every redesign building block with sample data, for checking them in both themes (debug builds only). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun DesignSampleScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    val colors = MaterialTheme.colorScheme

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.design_sample_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = stringResource(Res.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section(Res.string.design_sample_type)
            Text(stringResource(Res.string.design_sample_display), style = MaterialTheme.typography.displaySmall)
            Text(stringResource(Res.string.design_sample_headline), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(Res.string.design_sample_body), style = MaterialTheme.typography.bodyLarge)

            Section(Res.string.design_sample_numbers)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BigNumber(value = "1240", unit = "kcal", modifier = Modifier.weight(1f))
                ShapeBadge(text = "62%")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                BigNumber("4.4", "km", style = MaterialTheme.typography.headlineLarge)
                BigNumber("12", "min", style = MaterialTheme.typography.headlineLarge)
                BigNumber("0", "m", style = MaterialTheme.typography.headlineLarge)
            }

            Section(Res.string.design_sample_badges)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ShapeBadge("87")
                ShapeBadge("100", polygon = ExpressiveShapes.sunny, containerColor = colors.tertiaryContainer)
                ShapeBadge("3", polygon = ExpressiveShapes.softBurst, containerColor = colors.secondaryContainer, size = 56.dp)
                ShapeBadge("✓", polygon = ExpressiveShapes.pentagon, containerColor = colors.primary, size = 48.dp)
            }

            Section(Res.string.design_sample_bar)
            SegmentedBar(
                listOf(
                    BarSegment(stringResource(Res.string.macro_protein), 92.0, colors.primary, "92 g"),
                    BarSegment(stringResource(Res.string.macro_fat), 54.0, colors.tertiary, "54 g"),
                    BarSegment(stringResource(Res.string.macro_carbs), 180.0, colors.secondary, "180 g"),
                ),
            )

            Section(Res.string.design_sample_stats)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(stringResource(Res.string.design_sample_stat_average), "1830", Modifier.weight(1f), unit = "kcal", containerColor = colors.primaryContainer)
                StatCard(stringResource(Res.string.design_sample_stat_streak), "5", Modifier.weight(1f), containerColor = colors.tertiaryContainer)
                StatCard(stringResource(Res.string.design_sample_stat_water), "1.8", Modifier.weight(1f), unit = "l")
            }

            Section(Res.string.design_sample_loading)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                ExpressiveLoadingIndicator()
                ExpressiveLoadingIndicator(size = 64.dp, containerColor = colors.primaryContainer, color = colors.onPrimaryContainer)
            }

            Section(Res.string.design_sample_colors)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    colors.primary, colors.primaryContainer, colors.secondary, colors.secondaryContainer,
                    colors.tertiary, colors.tertiaryContainer, colors.surface, colors.surfaceContainer,
                    colors.surfaceContainerHigh, colors.error,
                ).forEach { Swatch(it) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(title: StringResource) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun Swatch(color: Color) {
    Box(Modifier.size(40.dp).background(color, MaterialTheme.shapes.small))
}
