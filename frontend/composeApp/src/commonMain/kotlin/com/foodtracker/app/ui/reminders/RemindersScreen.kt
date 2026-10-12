package com.foodtracker.app.ui.reminders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.MealType
import com.foodtracker.app.ui.displayLabel
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.ic_arrow_back
import org.jetbrains.compose.resources.painterResource

/** Opt-in daily reminders per meal. Every change is saved and (re)scheduled immediately. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun RemindersScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val platform = rememberReminderPlatform()
    var settings by remember { mutableStateOf(platform.load()) }
    // Shown after a denied request, or when reminders are on but the user has since blocked notifications.
    var notificationsBlocked by remember {
        mutableStateOf(settings.reminders.any { it.enabled } && !platform.canNotify())
    }
    var editingTimeOf by remember { mutableStateOf<MealType?>(null) }

    fun update(reminder: MealReminder) {
        settings = settings.with(reminder)
        platform.save(settings)
    }

    fun setEnabled(reminder: MealReminder, enabled: Boolean) {
        if (!enabled) {
            update(reminder.copy(enabled = false))
            return
        }
        // Opting in is the moment to ask for notification permission; denied means it stays off.
        platform.requestNotifications { allowed ->
            notificationsBlocked = !allowed
            if (allowed) update(reminder.copy(enabled = true))
        }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Reminders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Text(
                "Optional daily nudges to log a meal. They're off until you turn them on.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            for (meal in REMINDER_MEALS) {
                val reminder = settings.reminderFor(meal)
                ReminderRow(
                    reminder = reminder,
                    onEnabledChange = { setEnabled(reminder, it) },
                    onEditTime = { editingTimeOf = meal },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            if (notificationsBlocked) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Notifications are blocked for Food Tracker. Allow them in system settings to get reminders.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { platform.openNotificationSettings() }) { Text("Open settings") }
            }
        }
    }

    editingTimeOf?.let { meal ->
        val reminder = settings.reminderFor(meal)
        ReminderTimeDialog(
            title = "${meal.displayLabel()} reminder",
            initialHour = reminder.hour,
            initialMinute = reminder.minute,
            onDismiss = { editingTimeOf = null },
            onConfirm = { hour, minute ->
                editingTimeOf = null
                update(reminder.copy(hour = hour, minute = minute))
            },
        )
    }
}

@Composable
private fun ReminderRow(
    reminder: MealReminder,
    onEnabledChange: (Boolean) -> Unit,
    onEditTime: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(reminder.meal.displayLabel(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onEditTime) {
            Text(timeLabel(reminder.hour, reminder.minute), style = MaterialTheme.typography.titleMedium)
        }
        Switch(checked = reminder.enabled, onCheckedChange = onEnabledChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
