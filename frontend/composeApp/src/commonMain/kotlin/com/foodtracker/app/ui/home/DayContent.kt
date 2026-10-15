package com.foodtracker.app.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.foodtracker.app.data.DailyGoal
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.LogWaterRequest
import com.foodtracker.app.data.MealAnalysis
import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.MealType
import com.foodtracker.app.data.WaterDay
import com.foodtracker.app.data.cache.CachedDay
import com.foodtracker.app.data.cache.CaptureQueueRepository
import com.foodtracker.app.data.cache.CaptureRow
import com.foodtracker.app.data.cache.DayRepository
import com.foodtracker.app.data.cache.OutboxRepository
import com.foodtracker.app.data.cache.OutboxRow
import com.foodtracker.app.data.cache.RefreshResult
import com.foodtracker.app.data.cache.SaveOutcome
import com.foodtracker.app.ui.LoadingAware
import com.foodtracker.app.ui.MealTypeIcon
import com.foodtracker.app.ui.ReviewCapture
import com.foodtracker.app.ui.components.BadgeShape
import com.foodtracker.app.ui.components.BarSegment
import com.foodtracker.app.ui.components.BigNumber
import com.foodtracker.app.ui.components.EmptyIllustration
import com.foodtracker.app.ui.components.IllustrationKind
import com.foodtracker.app.ui.components.LocalReduceMotion
import com.foodtracker.app.ui.components.MorphingShapeBadge
import com.foodtracker.app.ui.components.SegmentedBar
import com.foodtracker.app.ui.components.animatedCount
import com.foodtracker.app.ui.components.badgeShapeFor
import com.foodtracker.app.ui.components.pressBounce
import com.foodtracker.app.ui.displayLabelOrOther
import com.foodtracker.app.ui.errorText
import com.foodtracker.app.ui.loadingPresentation
import com.foodtracker.app.ui.shortDate
import com.foodtracker.app.util.currentInstantIso
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.action_delete
import food_tracker_frontend.composeapp.generated.resources.add_to_favorites
import food_tracker_frontend.composeapp.generated.resources.calorie_ring_left
import food_tracker_frontend.composeapp.generated.resources.calorie_ring_of_target
import food_tracker_frontend.composeapp.generated.resources.calorie_ring_over
import food_tracker_frontend.composeapp.generated.resources.day_calories_title
import food_tracker_frontend.composeapp.generated.resources.default_goal_hint
import food_tracker_frontend.composeapp.generated.resources.edit_daily_goal
import food_tracker_frontend.composeapp.generated.resources.error_delete_meal
import food_tracker_frontend.composeapp.generated.resources.error_load_day
import food_tracker_frontend.composeapp.generated.resources.error_update_favorite
import food_tracker_frontend.composeapp.generated.resources.error_update_water
import food_tracker_frontend.composeapp.generated.resources.home_calories_title
import food_tracker_frontend.composeapp.generated.resources.home_empty_day
import food_tracker_frontend.composeapp.generated.resources.ic_delete
import food_tracker_frontend.composeapp.generated.resources.ic_edit
import food_tracker_frontend.composeapp.generated.resources.ic_star
import food_tracker_frontend.composeapp.generated.resources.ic_star_filled
import food_tracker_frontend.composeapp.generated.resources.macro_carbs
import food_tracker_frontend.composeapp.generated.resources.macro_fat
import food_tracker_frontend.composeapp.generated.resources.macro_progress_grams
import food_tracker_frontend.composeapp.generated.resources.macro_protein
import food_tracker_frontend.composeapp.generated.resources.macros_short
import food_tracker_frontend.composeapp.generated.resources.meal_pending
import food_tracker_frontend.composeapp.generated.resources.no_items
import food_tracker_frontend.composeapp.generated.resources.offline_notice_other_day
import food_tracker_frontend.composeapp.generated.resources.offline_notice_today
import food_tracker_frontend.composeapp.generated.resources.outbox_discard
import food_tracker_frontend.composeapp.generated.resources.outbox_rejected
import food_tracker_frontend.composeapp.generated.resources.outbox_retry
import food_tracker_frontend.composeapp.generated.resources.remove_from_favorites
import food_tracker_frontend.composeapp.generated.resources.unit_kcal
import food_tracker_frontend.composeapp.generated.resources.unit_ml
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Breakfast/lunch/dinner/snack first, in that order; untagged entries ("Other") last. */
private val MEAL_TYPE_SECTION_ORDER: List<MealType?> =
    listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK, null)

private data class DayTotals(val calories: Double, val protein: Double, val fat: Double, val carbs: Double)

/** [errorText] for use in composition: null for the moment it takes to resolve the strings. */
@Composable
private fun produceErrorText(message: StringResource, error: Exception): String? {
    val text by produceState<String?>(null, message, error) { value = errorText(message, error) }
    return text
}

/** "No connection · data from 14:32", with the date too when the copy isn't from [today]. */
@Composable
private fun offlineNotice(refreshedAt: Long, today: LocalDate): String {
    val age = cacheAge(refreshedAt, today)
    return if (age.date == null) {
        stringResource(Res.string.offline_notice_today, age.time)
    } else {
        stringResource(Res.string.offline_notice_other_day, shortDate(age.date), age.time)
    }
}

/** A queued meal or glass the backend refused, with its error and what the user can do about it. */
@Composable
private fun RejectedEntryCard(entry: RejectedEntry, onRetry: () -> Unit, onDiscard: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val what = entry.meal?.items?.joinToString(", ") { it.name }?.ifBlank { null }
        ?: entry.meal?.let { stringResource(Res.string.no_items) }
        ?: stringResource(Res.string.unit_ml, entry.water?.amountMl ?: 0)
    val reason = produceErrorText(Res.string.outbox_rejected, entry.error).orEmpty()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.errorContainer, contentColor = colors.onErrorContainer),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(what, style = MaterialTheme.typography.titleSmall)
            Text(reason, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDiscard) { Text(stringResource(Res.string.outbox_discard)) }
                TextButton(onClick = onRetry) { Text(stringResource(Res.string.outbox_retry)) }
            }
        }
    }
}

/** Summed from the same meals the list shows, so the totals and the list can never disagree. */
private fun List<MealEntry>.dayTotals() = DayTotals(
    calories = sumOf { it.totalCalories },
    protein = sumOf { it.totalProteinGrams },
    fat = sumOf { it.totalFatGrams },
    carbs = sumOf { it.totalCarbsGrams },
)

/**
 * One day's calories, macros, water and meals, with every action on them (favorite, delete, log
 * or undo water). Home shows it for today; it takes the day so other screens can show past days.
 *
 * The day comes from [dayRepository]: its cached copy at once, then the backend's (offline-cache).
 * Without a cached copy it loads as before, dimming the previous day meanwhile.
 */
@Composable
fun DayContent(
    apiClient: FoodTrackerApiClient,
    dayRepository: DayRepository,
    outbox: OutboxRepository,
    captureRepository: CaptureQueueRepository,
    onReviewCapture: (MealAnalysis, ReviewCapture) -> Unit,
    date: String,
    today: String,
    onEditGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // What's on screen and which day it belongs to: the previous day stays (dimmed) until the new one is known.
    var shown by remember { mutableStateOf<CachedDay?>(null) }
    var shownDate by remember { mutableStateOf<String?>(null) }
    // Whether [date]'s cached copy has been read yet (a few milliseconds), so the spinner doesn't flash first.
    var cacheRead by remember(date) { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(true) }
    // The last refresh's outcome for [date]; null while one runs or after a success.
    var refreshProblem by remember(date) { mutableStateOf<RefreshResult?>(null) }
    var actionError by remember(date) { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var waterError by remember { mutableStateOf<String?>(null) }
    // The day's queued entries (offline-queue), shown as pending or rejected on top of the cached day.
    var outboxRows by remember(date) { mutableStateOf<List<OutboxRow>>(emptyList()) }
    // The day's captures kept for analysis (offline-queue); they never count in the totals.
    var captures by remember(date) { mutableStateOf<List<CaptureRow>>(emptyList()) }
    // Serializes water taps, so each refresh reflects every earlier log/undo (taps can be rapid).
    val waterMutex = remember { Mutex() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(date) {
        dayRepository.observe(date).collect { cached ->
            cacheRead = true
            if (cached != null) {
                shown = cached
                shownDate = date
            } else if (shownDate == date) {
                // Dropped from the cache (e.g. it no longer decodes): load it again.
                shown = null
                shownDate = null
            }
        }
    }

    LaunchedEffect(date) {
        outbox.observeDay(date).collect { outboxRows = it }
    }

    LaunchedEffect(date) {
        captureRepository.observeDay(date).collect { captures = it }
    }

    LaunchedEffect(date, reloadKey) {
        isRefreshing = true
        val result = dayRepository.refresh(date)
        refreshProblem = result.takeUnless { it == RefreshResult.Success }
        isRefreshing = false
    }

    // Back in the foreground: the day may have changed elsewhere. The first resume is the opening itself.
    var resumedBefore by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        if (resumedBefore) reloadKey++ else resumedBefore = true
        onPauseOrDispose {}
    }

    fun toggleFavorite(meal: MealEntry) {
        scope.launch {
            try {
                apiClient.setFavorite(meal.id, !meal.isFavorite)
                actionError = null
                reloadKey++
            } catch (e: Exception) {
                actionError = errorText(Res.string.error_update_favorite, e)
            }
        }
    }

    fun deleteMeal(id: Int) {
        scope.launch {
            try {
                apiClient.deleteMeal(id)
                actionError = null
                reloadKey++
            } catch (e: Exception) {
                actionError = errorText(Res.string.error_delete_meal, e)
            }
        }
    }

    // The refresh updates the cache, and with it the water shown for that day.
    fun changeWater(action: suspend (day: String) -> Unit) {
        val day = date
        scope.launch {
            waterMutex.withLock {
                try {
                    action(day)
                    waterError = null
                } catch (e: Exception) {
                    waterError = errorText(Res.string.error_update_water, e)
                    return@withLock
                }
                dayRepository.refresh(day)
            }
        }
    }

    fun retryRejected(id: Long) {
        scope.launch {
            outbox.retry(id)
            reloadKey++
        }
    }

    fun discardRejected(id: Long) {
        scope.launch { outbox.discard(id) }
    }

    fun reviewCapture(capture: CaptureRow) {
        val analysis = captureRepository.analysisOf(capture) ?: return
        onReviewCapture(analysis, ReviewCapture(capture.id, capture.capturedAt, capture.clientId))
    }

    val problem = refreshProblem
    val loadError = when (problem) {
        is RefreshResult.Failed -> produceErrorText(Res.string.error_load_day, problem.error)
        is RefreshResult.Offline -> produceErrorText(Res.string.error_load_day, problem.cause)
        else -> null
    }
    val cachedDay = shown
    when {
        // This day isn't cached and couldn't be loaded: the error alone, as without a cache.
        shownDate != date && problem != null && !isRefreshing -> Text(
            loadError.orEmpty(),
            color = MaterialTheme.colorScheme.error,
            modifier = modifier.padding(horizontal = 16.dp),
        )
        // Reading the cache takes a few milliseconds; nothing is better than a flashing spinner.
        cachedDay == null && !cacheRead -> Box(modifier)
        // Dims the previous day while a day that isn't cached loads, instead of showing it unmarked.
        else -> LoadingAware(loadingPresentation(isLoading = shownDate != date, hasContent = cachedDay != null), modifier) {
            if (cachedDay != null) {
                // The previous day (still dimmed) shows without this day's queue.
                val shownDay = mergePending(cachedDay.meals, cachedDay.water, if (shownDate == date) outboxRows else emptyList())
                val meals = shownDay.meals
                val water = shownDay.water
                val currentGoal = cachedDay.goal
                val notice = when {
                    shownDate != date -> null
                    problem is RefreshResult.Offline -> offlineNotice(cachedDay.refreshedAt, LocalDate.parse(today))
                    problem is RefreshResult.Failed -> loadError
                    else -> null
                }
                val totals = meals.dayTotals()
                val grouped = meals.groupBy { it.mealType }
                Column(Modifier.fillMaxSize()) {
                    // Outside the list: inserted above its first item, it would land just off screen.
                    for (text in listOfNotNull(notice, actionError)) {
                        Text(
                            text,
                            color = if (text == notice && problem is RefreshResult.Offline) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item(key = "calories") {
                            CaloriesCard(totals = totals, goal = currentGoal, isToday = date == today, onEditGoal = onEditGoal)
                        }
                        item(key = "water") {
                            Column {
                                WaterCard(
                                    water = water,
                                    targetMl = currentGoal.targetWaterMl,
                                    glassMl = currentGoal.waterGlassMl,
                                    onAddGlass = {
                                        changeWater { day ->
                                            // Today's glass carries the tap's time, so a queued one keeps it.
                                            val loggedAt = loggedAtFor(day, today) ?: currentInstantIso()
                                            val outcome = outbox.logWater(LogWaterRequest(currentGoal.waterGlassMl, loggedAt), day)
                                            if (outcome is SaveOutcome.Rejected) throw outcome.error
                                        }
                                    },
                                    onUndo = {
                                        // A queued glass goes first, on the phone. Otherwise the newest stored one, picked
                                        // under the lock from fresh data, so rapid undos each remove a different entry.
                                        changeWater { day ->
                                            if (!outbox.undoPendingWater(day)) {
                                                apiClient.getWater(day).entries.lastOrNull()?.let { apiClient.deleteWater(it.id) }
                                            }
                                        }
                                    },
                                )
                                waterError?.let {
                                    Text(
                                        it,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                        if (shownDate == date) {
                            for (capture in captures) {
                                item(key = "capture-${capture.id}") {
                                    CaptureCard(
                                        capture = capture,
                                        loadPhoto = { captureRepository.photoOf(capture) },
                                        onReview = { reviewCapture(capture) },
                                        onRetry = { scope.launch { captureRepository.retry(capture.id) } },
                                        onDiscard = { scope.launch { captureRepository.discard(capture.id) } },
                                    )
                                }
                            }
                        }
                        for (rejected in shownDay.rejected) {
                            item(key = "rejected-${rejected.id}") {
                                RejectedEntryCard(
                                    entry = rejected,
                                    onRetry = { retryRejected(rejected.id) },
                                    onDiscard = { discardRejected(rejected.id) },
                                )
                            }
                        }
                        if (meals.isEmpty()) {
                            item(key = "empty") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    EmptyIllustration(IllustrationKind.PLATE)
                                    Text(
                                        stringResource(Res.string.home_empty_day),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 12.dp),
                                    )
                                }
                            }
                        }
                        for (type in MEAL_TYPE_SECTION_ORDER) {
                            val mealsOfType = grouped[type] ?: continue
                            item(key = "group-${type?.name ?: "other"}") {
                                MealGroupCard(
                                    type = type,
                                    meals = mealsOfType,
                                    pendingMealIds = shownDay.pendingMealIds,
                                    onToggleFavorite = ::toggleFavorite,
                                    onDelete = { deleteMeal(it.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The day's calories as a big figure with the goal percentage badge, plus the macro bar. */
@Composable
private fun CaloriesCard(totals: DayTotals, goal: DailyGoal, isToday: Boolean, onEditGoal: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val progress = progressOf(totals.calories, goal.targetCalories)
    // The figure counts up; the badge turns into the sunburst the moment the count passes the goal.
    val shownCalories = animatedCount(totals.calories.roundToInt())
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer, contentColor = colors.onSurface),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(if (isToday) Res.string.home_calories_title else Res.string.day_calories_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    BigNumber(value = "$shownCalories", unit = "kcal")
                    Row {
                        Text(
                            stringResource(Res.string.calorie_ring_of_target, goal.targetCalories.roundToInt()) + " · ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                        Text(
                            if (progress.isOver) {
                                stringResource(Res.string.calorie_ring_over, progress.excess.roundToInt())
                            } else {
                                stringResource(Res.string.calorie_ring_left, progress.remaining.roundToInt())
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (progress.isOver) colors.error else colors.primary,
                        )
                    }
                }
                // Text and shape both follow the counted figure, so they reach the goal together.
                val percent = goalPercent(shownCalories.toDouble(), goal.targetCalories)
                // Springs from the cookie to the sunburst as the counted figure reaches the goal (and back).
                val reduceMotion = LocalReduceMotion.current
                val morph by animateFloatAsState(
                    targetValue = if (badgeShapeFor(percent) == BadgeShape.GOAL_REACHED) 1f else 0f,
                    animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow),
                    label = "badgeMorph",
                )
                val badgeInteraction = remember { MutableInteractionSource() }
                MorphingShapeBadge(
                    text = percent?.let { "$it%" } ?: "–",
                    morphProgress = morph,
                    size = 80.dp,
                    containerColor = if (progress.isOver) colors.errorContainer else colors.primaryContainer,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .pressBounce(badgeInteraction)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = badgeInteraction,
                            indication = ripple(),
                            onClickLabel = stringResource(Res.string.edit_daily_goal),
                            onClick = onEditGoal,
                        ),
                )
            }
            if (goal.isDefault) {
                AssistChip(
                    onClick = onEditGoal,
                    label = { Text(stringResource(Res.string.default_goal_hint)) },
                    leadingIcon = { Icon(painterResource(Res.drawable.ic_edit), contentDescription = null) },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            MacroBar(totals, goal, Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun MacroBar(totals: DayTotals, goal: DailyGoal, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    @Composable
    fun segment(label: String, consumed: Double, target: Double, color: Color) = BarSegment(
        label = label,
        value = consumed,
        color = color,
        detail = stringResource(Res.string.macro_progress_grams, consumed.roundToInt(), target.roundToInt()),
        detailColor = if (progressOf(consumed, target).isOver) colors.error else null,
    )
    SegmentedBar(
        segments = listOf(
            segment(stringResource(Res.string.macro_protein), totals.protein, goal.targetProteinGrams, colors.primary),
            segment(stringResource(Res.string.macro_fat), totals.fat, goal.targetFatGrams, colors.tertiary),
            segment(stringResource(Res.string.macro_carbs), totals.carbs, goal.targetCarbsGrams, colors.secondary),
        ),
        modifier = modifier,
    )
}

@Composable
private fun MealGroupCard(
    type: MealType?,
    meals: List<MealEntry>,
    pendingMealIds: Set<Int>,
    onToggleFavorite: (MealEntry) -> Unit,
    onDelete: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow, contentColor = colors.onSurface),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                MealTypeIcon(type)
                Text(
                    type.displayLabelOrOther(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
                Text(
                    stringResource(Res.string.unit_kcal, meals.sumOf { it.totalCalories }.roundToInt()),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
            }
            meals.forEachIndexed { index, meal ->
                if (index > 0) HorizontalDivider(color = colors.outlineVariant)
                MealRow(
                    meal,
                    isPending = meal.id in pendingMealIds,
                    onToggleFavorite = { onToggleFavorite(meal) },
                    onDelete = { onDelete(meal) },
                )
            }
        }
    }
}

@Composable
private fun MealRow(
    meal: MealEntry,
    isPending: Boolean,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                meal.items.joinToString(", ") { it.name }.ifBlank { stringResource(Res.string.no_items) },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(
                    Res.string.macros_short,
                    meal.totalCalories.roundToInt(),
                    meal.totalProteinGrams.roundToInt(),
                    meal.totalFatGrams.roundToInt(),
                    meal.totalCarbsGrams.roundToInt(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Not stored yet: nothing to favorite or delete on the backend (offline-queue).
        if (isPending) {
            Text(
                stringResource(Res.string.meal_pending),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            return@Row
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                painterResource(if (meal.isFavorite) Res.drawable.ic_star_filled else Res.drawable.ic_star),
                contentDescription = stringResource(if (meal.isFavorite) Res.string.remove_from_favorites else Res.string.add_to_favorites),
                tint = if (meal.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                painterResource(Res.drawable.ic_delete),
                contentDescription = stringResource(Res.string.action_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
