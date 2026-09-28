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
)

@Serializable
data class UpdateMealRequest(
    val items: List<MealItem>? = null,
    val notes: String? = null,
    val occurredAt: String? = null,
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
