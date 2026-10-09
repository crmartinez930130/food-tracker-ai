package com.foodtracker.app.data

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
