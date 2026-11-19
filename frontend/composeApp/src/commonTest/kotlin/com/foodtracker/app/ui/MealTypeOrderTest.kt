package com.foodtracker.app.ui

import com.foodtracker.app.data.MealType
import com.foodtracker.app.ui.home.MEAL_TYPE_SECTION_ORDER
import kotlin.test.Test
import kotlin.test.assertEquals

class MealTypeOrderTest {

    @Test
    fun `types are shown in the day's order, pre-workout first`() {
        assertEquals(
            listOf(MealType.PRE_WORKOUT, MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK),
            MEAL_TYPE_DISPLAY_ORDER,
        )
    }

    @Test
    fun `every type is shown exactly once`() {
        assertEquals(MealType.entries.toSet(), MEAL_TYPE_DISPLAY_ORDER.toSet())
        assertEquals(MealType.entries.size, MEAL_TYPE_DISPLAY_ORDER.size)
    }

    @Test
    fun `Home groups follow the display order, with untagged meals last`() {
        assertEquals(MEAL_TYPE_DISPLAY_ORDER + null, MEAL_TYPE_SECTION_ORDER)
    }

    @Test
    fun `existing types keep their ordinals, which reminders use as ids`() {
        assertEquals(
            listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK, MealType.PRE_WORKOUT),
            MealType.entries,
        )
    }

    @Test
    fun `the suggested type follows the time of the meal`() {
        val expected = mapOf(
            0 to MealType.BREAKFAST,
            6 to MealType.BREAKFAST,
            7 to MealType.PRE_WORKOUT,
            8 to MealType.PRE_WORKOUT,
            9 to MealType.BREAKFAST,
            10 to MealType.BREAKFAST,
            11 to MealType.LUNCH,
            14 to MealType.LUNCH,
            15 to MealType.SNACK,
            18 to MealType.SNACK,
            19 to MealType.DINNER,
            23 to MealType.DINNER,
        )
        expected.forEach { (hour, type) -> assertEquals(type, suggestedMealType(hour), "hour $hour") }
    }
}
