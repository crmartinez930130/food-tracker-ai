package com.foodtracker.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.ic_chevron_left
import food_tracker_frontend.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

/** Weekly trends. The single-day view lives on Home. */
@Composable
fun SummaryScreen(apiClient: FoodTrackerApiClient, modifier: Modifier = Modifier) {
    // The week shown ends today by default: the 7 days starting 6 days ago.
    var startDate by rememberSaveable { mutableStateOf(todayDateString().plusDays(-6)) }
    var weekly by remember { mutableStateOf<WeeklySummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(startDate) {
        isLoading = true
        errorMessage = null
        try {
            weekly = apiClient.weeklySummary(startDate)
        } catch (e: Exception) {
            errorMessage = "Couldn't load summary: ${e.message ?: "unknown error"}"
        } finally {
            isLoading = false
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { startDate = startDate.plusDays(-7) }) {
                Icon(painterResource(Res.drawable.ic_chevron_left), contentDescription = "Previous week")
            }
            Text(
                "${startDate} – ${startDate.plusDays(6)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { startDate = startDate.plusDays(7) }) {
                Icon(painterResource(Res.drawable.ic_chevron_right), contentDescription = "Next week")
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            isLoading -> CircularProgressIndicator()
            errorMessage != null -> Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
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
