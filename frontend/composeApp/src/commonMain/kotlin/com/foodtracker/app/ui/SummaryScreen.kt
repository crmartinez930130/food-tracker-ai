package com.foodtracker.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.NutritionTotals
import com.foodtracker.app.data.WeeklySummary
import com.foodtracker.app.util.plusDays
import com.foodtracker.app.util.todayDateString
import kotlin.math.roundToInt

private enum class SummaryMode { DAILY, WEEKLY }

@Composable
fun SummaryScreen(apiClient: FoodTrackerApiClient, modifier: Modifier = Modifier) {
    var mode by rememberSaveable { mutableStateOf(SummaryMode.DAILY) }
    var date by rememberSaveable { mutableStateOf(todayDateString()) }
    var dailyTotals by remember { mutableStateOf<NutritionTotals?>(null) }
    var weekly by remember { mutableStateOf<WeeklySummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mode, date) {
        isLoading = true
        errorMessage = null
        try {
            when (mode) {
                SummaryMode.DAILY -> dailyTotals = apiClient.dailySummary(date)
                SummaryMode.WEEKLY -> weekly = apiClient.weeklySummary(date)
            }
        } catch (e: Exception) {
            errorMessage = "Couldn't load summary: ${e.message ?: "unknown error"}"
        } finally {
            isLoading = false
        }
    }

    val stepDays = if (mode == SummaryMode.DAILY) 1 else 7

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row {
            TextButton(onClick = { mode = SummaryMode.DAILY }) {
                Text(if (mode == SummaryMode.DAILY) "[Daily]" else "Daily")
            }
            TextButton(onClick = { mode = SummaryMode.WEEKLY }) {
                Text(if (mode == SummaryMode.WEEKLY) "[Weekly]" else "Weekly")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { date = date.plusDays(-stepDays) }) { Text("<") }
            Text(
                if (mode == SummaryMode.DAILY) date else "Week of $date",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = { date = date.plusDays(stepDays) }) { Text(">") }
        }

        Spacer(Modifier.height(16.dp))

        when {
            isLoading -> CircularProgressIndicator()
            errorMessage != null -> Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
            mode == SummaryMode.DAILY -> dailyTotals?.let { TotalsCard("Totals for $date", it) }
            else -> weekly?.let { summary ->
                TotalsCard("Total for the week", summary.total)
                Spacer(Modifier.height(16.dp))
                TotalsCard("Average per day", summary.averagePerDay)
            }
        }
    }
}

@Composable
private fun TotalsCard(title: String, totals: NutritionTotals, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text("${totals.totalCalories.roundToInt()} kcal")
        Text("Protein: ${totals.totalProteinGrams.roundToInt()} g")
        Text("Fat: ${totals.totalFatGrams.roundToInt()} g")
        Text("Carbs: ${totals.totalCarbsGrams.roundToInt()} g")
    }
}
