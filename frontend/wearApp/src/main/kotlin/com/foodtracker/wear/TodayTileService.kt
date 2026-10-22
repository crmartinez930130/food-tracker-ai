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
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

/** The "Today" tile (wear-app): a calories ring with the calories, what's left and the water. */
class TodayTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        SuspendToFutureAdapter.launchFuture(Dispatchers.IO) {
            WatchRefresh.requestIfDue(this@TodayTileService)
            val context = this@TodayTileService.localized()
            val settings = WatchSettingsStore.load(this@TodayTileService)
            val texts = settings?.let { todayStates(this@TodayTileService, it).first() }?.let { it to watchTexts(context, it) }
            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setFreshnessIntervalMillis(FRESHNESS_MILLIS)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(tileLayout(context, requestParams.deviceConfiguration, texts)))
                .build()
        }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        SuspendToFutureAdapter.launchFuture(Dispatchers.Default) {
            ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
        }

    private fun tileLayout(
        context: Context,
        device: DeviceParameters,
        content: Pair<com.foodtracker.app.widget.TodayWidgetState, WatchTexts>?,
    ): LayoutElementBuilders.LayoutElement = materialScope(context, device) {
        primaryLayout(
            titleSlot = { text((content?.second?.title ?: context.getString(R.string.today_title)).layoutString) },
            mainSlot = {
                if (content == null) {
                    text(context.getString(R.string.not_configured).layoutString, maxLines = 3)
                } else {
                    caloriesRing(content.first.calorieProgress.fraction, "${content.first.calories}", content.second.leftOrOver)
                }
            },
            bottomSlot = content?.let { (_, texts) -> { text((texts.note ?: texts.water).layoutString, maxLines = 2, typography = Typography.BODY_SMALL) } },
            onClick = openApp(context),
        )
    }

    private fun MaterialScope.caloriesRing(fraction: Float, calories: String, left: String): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .addContent(circularProgressIndicator(staticProgress = fraction))
            .addContent(
                LayoutElementBuilders.Column.Builder()
                    .addContent(text(calories.layoutString, typography = Typography.NUMERAL_MEDIUM))
                    .addContent(text(left.layoutString, typography = Typography.LABEL_SMALL))
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
