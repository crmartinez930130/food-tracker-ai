package com.foodtracker.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.foodtracker.app.MainActivity
import com.foodtracker.app.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** "Quick actions" (home-widgets): a glass of water through the offline queue, and a shortcut to Capture. */
class QuickActionsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(NARROW, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // The glass button shows the goal's glass size, so it follows Goals.
        val texts = todayWidgetStates(context).map { it.glassMl }.distinctUntilChanged().map { actionTexts(context, it) }
        val initial = texts.first()
        val openCapture = actionStartActivity(
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_CAPTURE, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        provideContent {
            val current = texts.collectAsState(initial).value
            GlanceTheme(colors = WidgetColors) {
                ActionsContent(current, logGlass = actionRunCallback<LogGlassAction>(), openCapture = openCapture)
            }
        }
    }

    companion object {
        private val NARROW = DpSize(110.dp, 60.dp)
        internal val WIDE = DpSize(250.dp, 60.dp)
    }
}

@Composable
private fun ActionsContent(texts: ActionTexts, logGlass: Action, openCapture: Action) {
    val colors = GlanceTheme.colors
    val wide = LocalSize.current.width >= QuickActionsWidget.WIDE.width
    val glassColors = ButtonDefaults.buttonColors(backgroundColor = colors.primary, contentColor = colors.onPrimary)
    val mealColors = ButtonDefaults.buttonColors(backgroundColor = colors.secondaryContainer, contentColor = colors.onSecondaryContainer)
    val modifier = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .cornerRadius(24.dp)
        .background(colors.widgetBackground)
        .padding(12.dp)
    if (wide) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            FilledButton(texts.glass, logGlass, GlanceModifier.defaultWeight(), icon = ImageProvider(R.drawable.ic_widget_water), colors = glassColors)
            Spacer(GlanceModifier.width(8.dp))
            FilledButton(texts.logMeal, openCapture, GlanceModifier.defaultWeight(), icon = ImageProvider(R.drawable.ic_widget_camera), colors = mealColors)
        }
    } else {
        Column(modifier, verticalAlignment = Alignment.CenterVertically) {
            FilledButton(texts.glass, logGlass, GlanceModifier.fillMaxWidth(), icon = ImageProvider(R.drawable.ic_widget_water), colors = glassColors)
            Spacer(GlanceModifier.height(8.dp))
            FilledButton(
                texts.logMeal,
                openCapture,
                GlanceModifier.fillMaxWidth(),
                icon = ImageProvider(R.drawable.ic_widget_camera),
                colors = mealColors,
                maxLines = 2,
            )
        }
    }
}

/** The glass button: logs one glass of the goal's size for today (see [logWidgetGlass]). */
class LogGlassAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        logWidgetGlass(context)
    }
}

class QuickActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickActionsWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdates.onWidgetsChanged(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdates.onWidgetsChanged(context)
    }
}
