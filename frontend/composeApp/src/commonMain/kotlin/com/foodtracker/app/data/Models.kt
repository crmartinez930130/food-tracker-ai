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
)

@Serializable
data class SaveMealRequest(
    val items: List<MealItem>,
    val notes: String? = null,
    val occurredAt: String? = null,
    val mealType: MealType? = null,
)

@Serializable
data class UpdateMealRequest(
    val items: List<MealItem>? = null,
    val notes: String? = null,
    val occurredAt: String? = null,
    val mealType: MealType? = null,
)

@Serializable
data class NutritionTotals(
    val totalCalories: Double,
    val totalProteinGrams: Double,
    val totalFatGrams: Double,
    val totalCarbsGrams: Double,
)

/** One day of `GET /api/summary/range`: that local day's summed totals (zeros if nothing was logged). */
@Serializable
data class DailyTotals(
    val date: String,
    val totalCalories: Double,
    val totalProteinGrams: Double,
    val totalFatGrams: Double,
    val totalCarbsGrams: Double,
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
    /** True when the user has never saved a goal and these are the backend's built-in defaults. */
    val isDefault: Boolean,
)

@Serializable
data class UpdateGoalsRequest(
    val targetCalories: Double,
    val targetProteinGrams: Double,
    val targetFatGrams: Double,
    val targetCarbsGrams: Double,
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
