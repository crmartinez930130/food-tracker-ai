package com.foodtracker.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.color.DayNightColorProvider
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.foodtracker.app.R
import com.foodtracker.app.ui.settings.AppEnvironment
import com.foodtracker.app.ui.settings.PreferencesStore
import com.foodtracker.app.ui.settings.deviceLanguage
import com.foodtracker.app.ui.settings.resolveLanguage
import com.foodtracker.app.ui.theme.FoodTrackerTheme
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.widget_config_done
import food_tracker_frontend.composeapp.generated.resources.widget_config_title
import food_tracker_frontend.composeapp.generated.resources.widget_config_transparency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Which of the app's widgets a configuration is for. */
private enum class WidgetKind { TODAY, ACTIONS }

/** The kind of this app's widget [appWidgetId] is, or null when it isn't one of them. */
private fun widgetKind(context: Context, appWidgetId: Int): WidgetKind? =
    when (AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.provider) {
        ComponentName(context, TodayCaloriesWidgetReceiver::class.java) -> WidgetKind.TODAY
        ComponentName(context, QuickActionsWidgetReceiver::class.java) -> WidgetKind.ACTIONS
        else -> null
    }

/**
 * A widget's settings (home-widgets): its background transparency, previewed over the live wallpaper.
 * Opened by the launcher (exported, as it must be), so it only accepts this app's own widgets.
 */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Leaving without "Done" keeps the widget as it was (and, on a first configuration, doesn't place it).
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        val kind = widgetKind(this, appWidgetId)
        if (kind == null) {
            finish()
            return
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT),
        )
        val glanceManager = GlanceAppWidgetManager(this)
        setContent {
            val language = resolveLanguage(remember { PreferencesStore.load(this) }.language, deviceLanguage())
            AppEnvironment(language) {
                // The widget follows the system's mode, so its preview does too.
                FoodTrackerTheme(darkTheme = isSystemInDarkTheme(), transparentBackground = true) {
                    val stored by produceState<Int?>(null) {
                        val state = getAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, glanceManager.getGlanceIdBy(appWidgetId))
                        value = clampTransparency(state[BackgroundTransparencyKey] ?: 0)
                    }
                    stored?.let { initial ->
                        WidgetConfigScreen(kind, initial) { transparency ->
                            lifecycleScope.launch {
                                val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                                updateAppWidgetState(this@WidgetConfigActivity, glanceId) { it[BackgroundTransparencyKey] = transparency }
                                when (kind) {
                                    WidgetKind.TODAY -> TodayCaloriesWidget().update(this@WidgetConfigActivity, glanceId)
                                    WidgetKind.ACTIONS -> QuickActionsWidget().update(this@WidgetConfigActivity, glanceId)
                                }
                                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetConfigScreen(kind: WidgetKind, initial: Int, onDone: (Int) -> Unit) {
    var transparency by rememberSaveable { mutableIntStateOf(initial) }
    // "Today's calories" always uses its dark background; "Quick actions" the theme's.
    val background = when (kind) {
        WidgetKind.TODAY -> TodayWidgetLook.background.copy(alpha = backgroundAlpha(transparency))
        WidgetKind.ACTIONS -> previewBackground(transparency)
    }
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        // The preview sits over the wallpaper, above the settings card.
        Box(Modifier.align(Alignment.Center).padding(horizontal = 24.dp).padding(bottom = 220.dp)) {
            when (kind) {
                WidgetKind.TODAY -> TodayPreview(background)
                WidgetKind.ACTIONS -> ActionsPreview(background)
            }
        }
        Card(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(Res.string.widget_config_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.widget_config_transparency),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text("$transparency%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = transparency.toFloat(),
                    onValueChange = { transparency = snapTransparency(it) },
                    valueRange = 0f..100f,
                    steps = 100 / TRANSPARENCY_STEP - 1,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onDone(transparency) }, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(Res.string.widget_config_done))
                }
            }
        }
    }
}

/** The widget's own background color (glance-material3's widgetBackground) with the chosen transparency. */
@Composable
private fun previewBackground(transparency: Int): Color {
    val provider = WidgetColors.widgetBackground
    val dark = isSystemInDarkTheme()
    val opaque = if (provider is DayNightColorProvider) {
        if (dark) provider.night else provider.day
    } else {
        provider.getColor(LocalContext.current)
    }
    return opaque.copy(alpha = backgroundAlpha(transparency))
}

/** A Compose replica of "Today's calories" with today's numbers, to preview the background. */
@Composable
private fun TodayPreview(background: Color) {
    val context = LocalContext.current
    val content by produceState<Pair<TodayWidgetState, TodayTexts>?>(null) {
        val state = todayWidgetStates(context).first()
        value = state to todayTexts(context, state)
    }
    val look = TodayWidgetLook
    Column(
        Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        val (state, texts) = content ?: return@Column
        val over = state.calorieProgress.isOver
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_widget_apple), contentDescription = null, tint = look.text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(texts.title, color = look.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${state.calories}", color = look.text, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text("kcal", color = look.secondaryText, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
        }
        Row {
            Text("${texts.target} · ", color = look.secondaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(texts.leftOrOver, color = if (over) look.over else look.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { state.calorieProgress.fraction },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (over) look.over else look.progress,
            trackColor = look.track,
            drawStopIndicator = {},
            gapSize = 0.dp,
        )
        Spacer(Modifier.height(8.dp))
        // The app's typeface is wider than the widget's system one: one line, cut with an ellipsis if needed.
        Text(
            texts.macros,
            color = look.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text("${texts.waterTitle} ${texts.water}", color = look.secondaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp)
    }
}

/** A Compose replica of "Quick actions", to preview the background. */
@Composable
private fun ActionsPreview(background: Color) {
    val context = LocalContext.current
    val texts by produceState<ActionTexts?>(null) {
        value = actionTexts(context, todayWidgetStates(context).first().glassMl)
    }
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .width(180.dp)
            .background(background, RoundedCornerShape(24.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val labels = texts ?: return@Column
        PreviewButton(labels.glass, R.drawable.ic_widget_water, colors.primary, colors.onPrimary)
        PreviewButton(labels.logMeal, R.drawable.ic_widget_camera, colors.secondaryContainer, colors.onSecondaryContainer)
    }
}

@Composable
private fun PreviewButton(text: String, icon: Int, container: Color, content: Color) {
    Button(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, maxLines = 2)
    }
}
