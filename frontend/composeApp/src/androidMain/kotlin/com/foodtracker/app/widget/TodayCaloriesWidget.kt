package com.foodtracker.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.foodtracker.app.MainActivity
import com.foodtracker.app.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * "Today's calories" (home-widgets): today's calories against the goal, and, when wide enough, the
 * macros and the water. Read-only; a tap opens Home.
 */
class TodayCaloriesWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(NARROW, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val contents = todayWidgetStates(context).map { it to todayTexts(context, it) }
        val initial = contents.first()
        WidgetUpdates.scheduleMidnight(context)
        provideContent {
            val (state, texts) = contents.collectAsState(initial).value
            GlanceTheme(colors = WidgetColors) {
                TodayContent(state, texts)
            }
        }
    }

    companion object {
        private val NARROW = DpSize(110.dp, 110.dp)
        internal val WIDE = DpSize(250.dp, 110.dp)
    }
}

@Composable
private fun TodayContent(state: TodayWidgetState, texts: TodayTexts) {
    val look = TodayWidgetLook
    val wide = LocalSize.current.width >= TodayCaloriesWidget.WIDE.width
    val over = state.calorieProgress.isOver
    val main = ColorProvider(look.text)
    val secondary = ColorProvider(look.secondaryText)
    Column(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(widgetBackground(fixed = look.background))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                ImageProvider(R.drawable.ic_widget_apple),
                contentDescription = null,
                modifier = GlanceModifier.size(18.dp),
                colorFilter = ColorFilter.tint(main),
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                texts.title,
                style = TextStyle(color = main, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            if (wide && texts.note != null) Note(texts.note)
        }
        Spacer(GlanceModifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${state.calories}",
                style = TextStyle(color = main, fontSize = 36.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(4.dp))
            Text(
                "kcal",
                style = TextStyle(color = secondary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.padding(bottom = 6.dp),
            )
        }
        // Over the goal the excess stays red (the dark scheme's light one), still bold.
        val leftStyle = TextStyle(color = if (over) ColorProvider(look.over) else main, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        val targetStyle = TextStyle(color = secondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        if (wide) {
            Row {
                Text("${texts.target} · ", style = targetStyle, maxLines = 1)
                Text(texts.leftOrOver, style = leftStyle, maxLines = 1)
            }
        } else {
            Text(texts.target, style = targetStyle, maxLines = 1)
            Text(texts.leftOrOver, style = leftStyle, maxLines = 1)
        }
        Spacer(GlanceModifier.height(8.dp))
        LinearProgressIndicator(
            progress = state.calorieProgress.fraction,
            modifier = GlanceModifier.fillMaxWidth().height(8.dp),
            color = ColorProvider(if (over) look.over else look.progress),
            backgroundColor = ColorProvider(look.track),
        )
        if (wide) {
            Spacer(GlanceModifier.height(8.dp))
            Text(texts.macros, style = TextStyle(color = secondary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            Spacer(GlanceModifier.height(6.dp))
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${texts.waterTitle} ${texts.water}",
                    style = TextStyle(color = secondary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.width(8.dp))
                LinearProgressIndicator(
                    progress = state.waterFraction,
                    modifier = GlanceModifier.defaultWeight().height(6.dp),
                    color = ColorProvider(look.water),
                    backgroundColor = ColorProvider(look.track),
                )
            }
        } else if (texts.note != null) {
            Spacer(GlanceModifier.height(6.dp))
            Note(texts.note)
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = TextStyle(color = ColorProvider(TodayWidgetLook.secondaryText), fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 2)
}

class TodayCaloriesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayCaloriesWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdates.onWidgetsChanged(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdates.onWidgetsChanged(context)
    }
}
