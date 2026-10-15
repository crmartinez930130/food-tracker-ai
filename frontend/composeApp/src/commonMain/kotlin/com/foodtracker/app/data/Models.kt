package com.foodtracker.app.data

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Confidence {
    @SerialName("low") LOW,
    @SerialName("medium") MEDIUM,
    @SerialName("high") HIGH,
}

@Serializable
enum class MealType {
    @SerialName("breakfast") BREAKFAST,
    @SerialName("lunch") LUNCH,
    @SerialName("dinner") DINNER,
    @SerialName("snack") SNACK,
}

@Serializable
data class MealItem(
    val name: String,
    val estimatedGrams: Double,
    val confidence: Confidence,
    val calories: Double,
    val proteinGrams: Double,
    val fatGrams: Double,
    val carbsGrams: Double,
)

@Serializable
data class MealAnalysis(
    val items: List<MealItem>,
    val totalCalories: Double,
    val totalProteinGrams: Double,
    val totalFatGrams: Double,
    val totalCarbsGrams: Double,
    val notes: String? = null,
)

@Serializable
data class MealEntry(
    val id: Int,
    val occurredAt: String,
    val createdAt: String,
    val notes: String? = null,
    val mealType: MealType? = null,
    val isFavorite: Boolean = false,
    val items: List<MealItem>,
    val totalCalories: Double,
    val totalProteinGrams: Double,
    val totalFatGrams: Double,
    val totalCarbsGrams: Double,
    /** The id the app generated when it saved this meal (null for meals saved without one). */
    val clientId: String? = null,
)

/** [clientId] identifies this save across retries: the backend stores the meal once (offline-queue). */
@Serializable
data class SaveMealRequest(
    val items: List<MealItem>,
    val notes: String? = null,
    val occurredAt: String? = null,
    val mealType: MealType? = null,
    val clientId: String? = null,
)

/**
 * Partial update: only the fields that are set are sent, and the backend keeps the rest. Absent
 * fields are omitted rather than sent as null (the client encodes defaults), so this can't clear a
 * field by sending null - nothing in the app needs that yet.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UpdateMealRequest(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val items: List<MealItem>? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val notes: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val occurredAt: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val mealType: MealType? = null,
)

@Serializable
data class NutritionTotals(
    val totalCalories: Double,
    val totalProteinGrams: Double,
    val totalFatGrams: Double,
    val totalCarbsGrams: Double,
)

/**
 * One day of `GET /api/summary/range`: that local day's summed totals, how many meals it has and
 * its water (zeros if nothing was logged).
 */
@Serializable
data class DailyTotals(
    val date: String,
    val totalCalories: Double,
    val totalProteinGrams: Double,
    val totalFatGrams: Double,
    val totalCarbsGrams: Double,
    val mealCount: Int = 0,
    val waterMl: Int = 0,
)

@Serializable
data class WeeklySummary(
    val total: NutritionTotals,
    val averagePerDay: NutritionTotals,
)

@Serializable
data class DailyGoal(
    val targetCalories: Double,
    val targetProteinGrams: Double,
    val targetFatGrams: Double,
    val targetCarbsGrams: Double,
    /** Daily water target, in ml. */
    val targetWaterMl: Int,
    /** How much one "add a glass" tap logs, in ml. */
    val waterGlassMl: Int,
    /** True when the user has never saved a goal and these are the backend's built-in defaults. */
    val isDefault: Boolean,
)

@Serializable
data class UpdateGoalsRequest(
    val targetCalories: Double,
    val targetProteinGrams: Double,
    val targetFatGrams: Double,
    val targetCarbsGrams: Double,
    val targetWaterMl: Int,
    val waterGlassMl: Int,
)

/**
 * Body for toggling a favorite. Deliberately not [UpdateMealRequest]: the client serializes with
 * `encodeDefaults`, so its null fields would be sent too and clear the meal's notes and type.
 */
@Serializable
data class FavoriteUpdateRequest(val isFavorite: Boolean)

/** Starting data for the review screen when re-logging a saved meal: same foods, no new analysis. */
fun MealEntry.toReviewAnalysis(): MealAnalysis = MealAnalysis(
    items = items,
    totalCalories = totalCalories,
    totalProteinGrams = totalProteinGrams,
    totalFatGrams = totalFatGrams,
    totalCarbsGrams = totalCarbsGrams,
    notes = notes,
)

/** One weighing; [day] is the user's local calendar day (YYYY-MM-DD). */
@Serializable
data class WeightEntry(
    val id: Int,
    val weightKg: Double,
    val day: String,
    val createdAt: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class LogWeightRequest(
    val weightKg: Double,
    /** Omitted (not sent as null) when absent, so the backend defaults it to today. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val day: String? = null,
)

/** One water entry; it counts toward the local calendar day of [loggedAt] (UTC ISO timestamp). */
@Serializable
data class WaterEntry(
    val id: Int,
    val amountMl: Int,
    val loggedAt: String,
    /** The id the app generated when it logged this glass (null for entries logged without one). */
    val clientId: String? = null,
)

/** The water logged on one local day ([date], YYYY-MM-DD), oldest entry first. */
@Serializable
data class WaterDay(
    val date: String,
    val totalMl: Int,
    val entries: List<WaterEntry>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class LogWaterRequest(
    val amountMl: Int,
    /** Omitted (not sent as null) when absent, so the backend logs it now. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val loggedAt: String? = null,
    /** Identifies this glass across retries, like [SaveMealRequest.clientId]; omitted when absent. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val clientId: String? = null,
)

/** The local day (YYYY-MM-DD) of the earliest logged meal, or null when there are none. */
@Serializable
data class FirstMealDate(val date: String?)

/** A portion size the user can state when analyzing a meal; [apiValue] is what the backend expects. */
enum class Portion(val apiValue: String) { SMALL("small"), MEDIUM("medium"), LARGE("large") }
