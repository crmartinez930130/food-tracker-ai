package com.foodtracker.wear

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.foodtracker.app.widget.TodayWidgetState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** The watch app (wear-app): today's calories, macros and water, from the watch's copy and the backend. */
class WatchActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.localized())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Settings the phone published while the app wasn't listening; new ones trigger a refresh themselves.
        lifecycleScope.launch { loadPublishedSettings(this@WatchActivity) }
        setContent {
            MaterialTheme(colorScheme = WatchColors.compose) {
                TodayScreen()
            }
        }
    }
}

@Composable
private fun TodayScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val settings by remember { WatchSettingsStore.flow(context) }.collectAsState()
    val states = remember(settings) { settings?.let { todayStates(context, it) } ?: flowOf(null) }
    val state by states.collectAsState(initial = null)
    // Today again whenever the app opens with settings, or they change (e.g. a new token).
    LaunchedEffect(settings) { if (settings != null) refreshToday(context) }
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    AppScaffold {
        val today = state
        // The calories ring hugs the round screen behind the list, so it stays put while scrolling.
        if (today != null) CaloriesRing(today)
        ScreenScaffold(
            scrollState = listState,
            edgeButton = {
                EdgeButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        scope.launch { logWatchGlass(context) }
                    },
                    enabled = settings != null,
                ) {
                    Text(context.getString(R.string.add_glass, today?.glassMl ?: 250))
                }
            },
        ) { contentPadding ->
            // Kept inside the calories ring: cards edge to edge would run into it and past the round border.
            TransformingLazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.padding(horizontal = 18.dp)) {
                when {
                    settings == null -> item { Message(context.getString(R.string.not_configured)) }
                    today != null -> {
                        val texts = watchTexts(context, today)
                        item {
                            ListHeader(
                                transformation = SurfaceTransformation(transformationSpec),
                                modifier = Modifier.transformedHeight(this, transformationSpec),
                            ) { Text(texts.title) }
                        }
                        item {
                            InfoCard(Modifier.transformedHeight(this, transformationSpec), SurfaceTransformation(transformationSpec)) {
                                CaloriesContent(today, texts)
                            }
                        }
                        item {
                            InfoCard(Modifier.transformedHeight(this, transformationSpec), SurfaceTransformation(transformationSpec)) {
                                MacroLine(texts.protein)
                                MacroLine(texts.fat)
                                MacroLine(texts.carbs)
                            }
                        }
                        item {
                            InfoCard(Modifier.transformedHeight(this, transformationSpec), SurfaceTransformation(transformationSpec)) {
                                Text(texts.water, fontSize = 13.sp)
                                Spacer(Modifier.height(6.dp))
                                ProgressBar(today.waterFraction, MaterialTheme.colorScheme.tertiary)
                            }
                        }
                        texts.note?.let { note -> item { Note(note) } }
                    }
                }
            }
        }
    }
}

/** A read-only card: Material 3's card surface and list motion, with nothing to tap. */
@Composable
private fun InfoCard(modifier: Modifier, transformation: SurfaceTransformation, content: @Composable () -> Unit) {
    Card(
        onClick = {},
        enabled = false,
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
        // Less side padding than the default: on a round watch every dp of line width counts.
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

@Composable
private fun CaloriesContent(state: TodayWidgetState, texts: WatchTexts) {
    val colors = MaterialTheme.colorScheme
    val over = state.calorieProgress.isOver
    Row(verticalAlignment = Alignment.Bottom) {
        Text("${state.calories}", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
        Spacer(Modifier.width(4.dp))
        Text("kcal", color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
    }
    Text(texts.target, color = colors.onSurfaceVariant, fontSize = 13.sp)
    Text(texts.leftOrOver, color = if (over) colors.error else colors.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
}

/**
 * The calories ring around the round screen, drawn directly: Wear's CircularProgressIndicator (like its linear
 * one) showed only its track here, on the Galaxy Watch6 as on the emulator, so a plain arc it is.
 */
@Composable
private fun CaloriesRing(state: TodayWidgetState) {
    val colors = MaterialTheme.colorScheme
    val track = colors.surfaceContainer
    val progress = if (state.calorieProgress.isOver) colors.error else colors.primary
    val fraction = state.calorieProgress.fraction
    Canvas(Modifier.fillMaxSize().padding(4.dp)) {
        val stroke = 6.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
        if (fraction > 0f) {
            drawArc(
                progress,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/** One macro line, centered, so "Carbohidratos …" still reads well if it wraps on a small watch. */
@Composable
private fun MacroLine(text: String) {
    Text(text, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun Message(text: String) {
    Text(text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp))
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A plain rounded bar: [fraction] (0..1) of the width in [color] over the track. */
@Composable
private fun ProgressBar(fraction: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(CircleShape).background(color))
    }
}
