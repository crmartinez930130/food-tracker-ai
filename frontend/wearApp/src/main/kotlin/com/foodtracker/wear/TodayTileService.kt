package com.foodtracker.wear

import android.content.Context
import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.circularProgressIndicator
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.loadAction
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.foodtracker.app.widget.TodayWidgetState
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

/**
 * The "Today" tile (wear-app): a calories ring with the calories and what's left, the water, and a "+ glass"
 * edge button that logs a glass without opening the app, in Food Tracker's colors.
 */
class TodayTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        SuspendToFutureAdapter.launchFuture(Dispatchers.IO) {
            val service = this@TodayTileService
            // A tap on "+ glass" reloads the tile carrying its id: log the glass first, then draw the new total.
            val now = System.currentTimeMillis()
            if (isNewTileClick(requestParams.currentState.lastClickableId, TileClicks.lastHandled(service), now)) {
                TileClicks.handled(service, now)
                logWatchGlass(service)
            } else {
                WatchRefresh.requestIfDue(service)
            }
            val context = service.localized()
            val content = WatchSettingsStore.load(service)
                ?.let { todayStates(service, it).first() }
                ?.let { it to watchTexts(context, it) }
            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setFreshnessIntervalMillis(FRESHNESS_MILLIS)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(tileLayout(context, requestParams.deviceConfiguration, content)))
                .build()
        }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        SuspendToFutureAdapter.launchFuture(Dispatchers.Default) {
            ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
        }

    private fun tileLayout(
        context: Context,
        device: DeviceParameters,
        content: Pair<TodayWidgetState, WatchTexts>?,
    ): LayoutElementBuilders.LayoutElement =
        materialScope(context, device, allowDynamicTheme = false, defaultColorScheme = WatchColors.tile) {
            primaryLayout(
                titleSlot = { text((content?.second?.title ?: context.getString(R.string.today_title)).layoutString) },
                mainSlot = {
                    if (content == null) {
                        text(context.getString(R.string.not_configured).layoutString, maxLines = 3)
                    } else {
                        caloriesRing(content.first, content.second)
                    }
                },
                bottomSlot = content?.let { (state, _) ->
                    {
                        textEdgeButton(onClick = clickable(id = ADD_GLASS_CLICK, action = loadAction())) {
                            text(context.getString(R.string.add_glass, state.glassMl).layoutString)
                        }
                    }
                },
                onClick = openApp(context),
            )
        }

    /** The ring with the calories and what's left inside, and the water (or how old the data is) below. */
    private fun MaterialScope.caloriesRing(state: TodayWidgetState, texts: WatchTexts): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .addContent(circularProgressIndicator(staticProgress = state.calorieProgress.fraction))
            .addContent(
                LayoutElementBuilders.Column.Builder()
                    .addContent(text("${state.calories}".layoutString, typography = Typography.NUMERAL_MEDIUM))
                    .addContent(text(texts.leftOrOver.layoutString, typography = Typography.LABEL_SMALL))
                    .addContent(text((texts.note ?: texts.water).layoutString, typography = Typography.BODY_EXTRA_SMALL, maxLines = 2))
                    .build(),
            )
            .build()

    private fun openApp(context: Context): ModifiersBuilders.Clickable =
        ModifiersBuilders.Clickable.Builder()
            .setId("open_app")
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(context.packageName)
                            .setClassName(WatchActivity::class.java.name)
                            .build(),
                    )
                    .build(),
            )
            .build()

    private companion object {
        const val RESOURCES_VERSION = "1"
        const val FRESHNESS_MILLIS = 30 * 60 * 1000L
    }
}

/** When the tile last handled a "+ glass" tap (see [isNewTileClick]). */
private object TileClicks {
    private const val PREFS = "watch_tile"
    private const val KEY = "last_glass_tap"

    fun lastHandled(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY, 0L)

    fun handled(context: Context, at: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY, at).commit()
    }
}
