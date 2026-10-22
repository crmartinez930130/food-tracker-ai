package com.foodtracker.wear

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
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
        lifecycleScope.launch {
            loadPublishedSettings(this@WatchActivity)
            refreshToday(this@WatchActivity)
        }
        setContent {
            MaterialTheme {
                TodayScreen()
            }
        }
    }
}

@Composable
private fun TodayScreen() {
    val context = LocalContext.current
    val settings by remember { WatchSettingsStore.flow(context) }.collectAsState()
    val states = remember(settings) { settings?.let { todayStates(context, it) } ?: flowOf(null) }
    val state by states.collectAsState(initial = null)
    // New settings (e.g. a new token) refresh at once.
    LaunchedEffect(settings) { if (settings != null) refreshToday(context) }
    val listState = rememberTransformingLazyColumnState()
    AppScaffold {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
                val today = state
                when {
                    settings == null -> item { Message(context.getString(R.string.not_configured)) }
                    today != null -> {
                        val texts = watchTexts(context, today)
                        // One item per block, so the list opens at the top and scrolls block by block.
                        item { CaloriesBlock(today, texts) }
                        item { MacrosBlock(texts) }
                        item { WaterBlock(today, texts) }
                        texts.note?.let { note -> item { Note(note) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp))
}

@Composable
private fun CaloriesBlock(state: TodayWidgetState, texts: WatchTexts) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(texts.title, color = colors.primary, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${state.calories}", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text("kcal", color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
        }
        Text(texts.target, color = colors.onSurfaceVariant, fontSize = 13.sp)
        Text(
            texts.leftOrOver,
            color = if (state.calorieProgress.isOver) colors.error else colors.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(6.dp))
        ProgressBar(state.calorieProgress.fraction, if (state.calorieProgress.isOver) colors.error else colors.primary)
    }
}

@Composable
private fun MacrosBlock(texts: WatchTexts) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(texts.protein, fontSize = 13.sp)
        Text(texts.fat, fontSize = 13.sp)
        Text(texts.carbs, fontSize = 13.sp)
    }
}

@Composable
private fun WaterBlock(state: TodayWidgetState, texts: WatchTexts) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(texts.water, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        ProgressBar(state.waterFraction, MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun Note(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
