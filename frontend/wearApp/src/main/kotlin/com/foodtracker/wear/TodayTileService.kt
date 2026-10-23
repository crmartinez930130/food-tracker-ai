package com.foodtracker.wear

import android.content.Context
import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.ProgressIndicatorColors
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.circularProgressIndicator
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.loadAction
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.foodtracker.app.widget.TodayWidgetState
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

/**
 * The "Today" tile (wear-app), in the style of Samsung's own tiles: a title and a rounded panel with two circles
 * (calories ring, water ring), in Food Tracker's colors. A tap on the water circle logs a glass without opening
 * the app; a tap elsewhere on the panel opens it.
 */
class TodayTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        SuspendToFutureAdapter.launchFuture(Dispatchers.IO) {
            val service = this@TodayTileService
            // A tap on the water circle reloads the tile carrying its id: log the glass first, then draw the new total.
            val clicked = requestParams.currentState.lastClickableId
            if (isNewTileClick(clicked, TileClicks.lastHandled(service))) {
                TileClicks.handled(service, clicked)
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
            ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .addIdToImageMapping(
                    WATER_ICON,
                    ResourceBuilders.ImageResource.Builder()
                        .setAndroidResourceByResId(
                            ResourceBuilders.AndroidImageResourceByResId.Builder().setResourceId(R.drawable.ic_tile_water).build(),
                        )
                        .build(),
                )
                .build()
        }

    private fun tileLayout(
        context: Context,
        device: DeviceParameters,
        content: Pair<TodayWidgetState, WatchTexts>?,
    ): LayoutElementBuilders.LayoutElement =
        materialScope(context, device, allowDynamicTheme = false, defaultColorScheme = WatchColors.tile) {
            // Laid out by hand, like Samsung's own tiles: the title, then the panel centered on the round screen.
            // (primaryLayout's reserved title and edge-button room left the panel too little height.)
            val title = text((content?.second?.title ?: context.getString(R.string.today_title)).layoutString, typography = Typography.TITLE_SMALL)
            val body = if (content == null) {
                text(context.getString(R.string.not_configured).layoutString, maxLines = 3)
            } else {
                panel(context, device, content.first)
            }
            val column = LayoutElementBuilders.Column.Builder()
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(title)
                .addContent(spacer(6f))
                .addContent(body)
            content?.second?.note?.let { note ->
                column.addContent(spacer(4f)).addContent(text(note.layoutString, typography = Typography.BODY_EXTRA_SMALL, maxLines = 2))
            }
            LayoutElementBuilders.Box.Builder()
                .setWidth(expand())
                .setHeight(expand())
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                .addContent(column.build())
                .build()
        }

    /**
     * The rounded panel with two circles: the calories ring (a tap opens the app, as on the rest of the panel) and
     * the water ring, which logs a glass when tapped.
     */
    private fun MaterialScope.panel(context: Context, device: DeviceParameters, state: TodayWidgetState): LayoutElementBuilders.LayoutElement {
        // Sized from the screen, so the two circles fit any round watch.
        val cell = (device.screenWidthDp * 0.34f).coerceIn(52f, 88f)
        val gap = cell * 0.1f
        val over = state.calorieProgress.isOver
        val calories = ringCircle(
            size = cell,
            progress = state.calorieProgress.fraction,
            indicator = if (over) WatchColors.ERROR else WatchColors.PRIMARY,
            track = WatchColors.PRIMARY_CONTAINER,
            onClick = null,
            value = text("${state.calories}".layoutString, typography = Typography.LABEL_LARGE),
            label = text(context.getString(R.string.tile_kcal).layoutString, typography = Typography.BODY_EXTRA_SMALL),
        )
        val water = ringCircle(
            size = cell,
            progress = state.waterFraction,
            indicator = WatchColors.TERTIARY,
            track = WatchColors.TERTIARY_CONTAINER,
            onClick = clickable(id = glassClickId(System.currentTimeMillis()), action = loadAction()),
            value = text("${state.waterMl}".layoutString, typography = Typography.LABEL_LARGE),
            // "+ (drop)": the tap adds a glass of the goal's size.
            label = LayoutElementBuilders.Row.Builder()
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                .addContent(text(context.getString(R.string.tile_water_add).layoutString, typography = Typography.BODY_EXTRA_SMALL))
                .addContent(waterIcon(cell * 0.16f, WatchColors.TERTIARY))
                .build(),
        )
        return LayoutElementBuilders.Box.Builder()
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(WatchColors.SURFACE_CONTAINER.toInt()))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(cell * 0.6f)).build())
                            .build(),
                    )
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setStart(dp(gap * 2.5f)).setEnd(dp(gap * 2.5f)).setTop(dp(gap * 1.5f)).setBottom(dp(gap * 1.5f))
                            .build(),
                    )
                    .setClickable(openApp(context))
                    .build(),
            )
            .addContent(row(gap, calories, water))
            .build()
    }

    /** A circle with a progress ring around it, a value and a label inside, and an optional click of its own. */
    private fun MaterialScope.ringCircle(
        size: Float,
        progress: Float,
        indicator: Long,
        track: Long,
        onClick: ModifiersBuilders.Clickable?,
        value: LayoutElementBuilders.LayoutElement,
        label: LayoutElementBuilders.LayoutElement,
    ): LayoutElementBuilders.LayoutElement =
        circle(size, WatchColors.SURFACE_CONTAINER_HIGH, onClick) {
            LayoutElementBuilders.Box.Builder()
                .setWidth(dp(size))
                .setHeight(dp(size))
                .addContent(
                    circularProgressIndicator(
                        staticProgress = progress,
                        colors = ProgressIndicatorColors(indicatorColor = indicator.toInt().argb, trackColor = track.toInt().argb),
                        strokeWidth = size * 0.07f,
                        size = dp(size),
                    ),
                )
                .addContent(column(value, label))
                .build()
        }

    /** A filled circle of [size] dp with [content] centered, and an optional click. */
    private fun circle(
        size: Float,
        background: Long,
        onClick: ModifiersBuilders.Clickable?,
        content: () -> LayoutElementBuilders.LayoutElement,
    ): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .setWidth(dp(size))
            .setHeight(dp(size))
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(background.toInt()))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(size / 2)).build())
                            .build(),
                    )
                    .apply { onClick?.let { setClickable(it) } }
                    .build(),
            )
            .addContent(content())
            .build()

    private fun waterIcon(size: Float, tint: Long): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Image.Builder()
            .setResourceId(WATER_ICON)
            .setWidth(dp(size))
            .setHeight(dp(size))
            .setColorFilter(LayoutElementBuilders.ColorFilter.Builder().setTint(argb(tint.toInt())).build())
            .build()

    private fun column(vararg items: LayoutElementBuilders.LayoutElement): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .apply { items.forEach { addContent(it) } }
            .build()

    private fun row(gap: Float, first: LayoutElementBuilders.LayoutElement, second: LayoutElementBuilders.LayoutElement): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Row.Builder()
            .addContent(first)
            .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(gap)).build())
            .addContent(second)
            .build()

    private fun spacer(height: Float): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Spacer.Builder().setHeight(dp(height)).build()

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
        // "2": the tile now declares the water-drop image, so cached resources must be fetched again.
        const val RESOURCES_VERSION = "2"
        const val FRESHNESS_MILLIS = 30 * 60 * 1000L
        const val WATER_ICON = "water"
    }
}

/** The id of the last "+ glass" tap the tile handled (see [isNewTileClick]). */
private object TileClicks {
    private const val PREFS = "watch_tile"
    private const val KEY = "last_glass_tap_id"

    fun lastHandled(context: Context): String? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)

    fun handled(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, id).commit()
    }
}
