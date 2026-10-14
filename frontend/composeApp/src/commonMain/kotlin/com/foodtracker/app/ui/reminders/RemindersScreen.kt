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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.foodtracker.app.data.MealType
import com.foodtracker.app.ui.MealTypeIcon
import com.foodtracker.app.ui.displayLabel
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.action_back
import food_tracker_frontend.composeapp.generated.resources.action_cancel
import food_tracker_frontend.composeapp.generated.resources.action_ok
import food_tracker_frontend.composeapp.generated.resources.ic_arrow_back
import food_tracker_frontend.composeapp.generated.resources.reminders_blocked
import food_tracker_frontend.composeapp.generated.resources.reminders_intro
import food_tracker_frontend.composeapp.generated.resources.reminders_open_settings
import food_tracker_frontend.composeapp.generated.resources.reminders_time_dialog_breakfast
import food_tracker_frontend.composeapp.generated.resources.reminders_time_dialog_dinner
import food_tracker_frontend.composeapp.generated.resources.reminders_time_dialog_lunch
import food_tracker_frontend.composeapp.generated.resources.reminders_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Opt-in daily reminders per meal. Every change is saved and (re)scheduled immediately. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun RemindersScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val platform = rememberReminderPlatform()
    var settings by remember { mutableStateOf(platform.load()) }
    // Shown after a denied request, or when reminders are on but notifications aren't allowed.
    var notificationsBlocked by remember { mutableStateOf(false) }
    // Re-checked on every resume (incl. coming back from system settings), not just on entry.
    LifecycleResumeEffect(Unit) {
        notificationsBlocked = if (platform.canNotify()) {
            false
        } else {
            // Keep a denial's message even while every reminder is off.
            notificationsBlocked || settings.reminders.any { it.enabled }
        }
        onPauseOrDispose {}
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
                title = { Text(stringResource(Res.string.reminders_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = stringResource(Res.string.action_back))
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Text(
                stringResource(Res.string.reminders_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            for (meal in REMINDER_MEALS) {
                val reminder = settings.reminderFor(meal)
                ReminderCard(
                    reminder = reminder,
                    onEnabledChange = { setEnabled(reminder, it) },
                    onEditTime = { editingTimeOf = meal },
                )
                Spacer(Modifier.height(12.dp))
            }

            if (notificationsBlocked) {
                Spacer(Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(Res.string.reminders_blocked), style = MaterialTheme.typography.bodyMedium)
                        FilledTonalButton(
                            onClick = { platform.openNotificationSettings() },
                            modifier = Modifier.padding(top = 12.dp),
                        ) { Text(stringResource(Res.string.reminders_open_settings)) }
                    }
                }
            }
        }
    }

    editingTimeOf?.let { meal ->
        val reminder = settings.reminderFor(meal)
        ReminderTimeDialog(
            title = stringResource(
                when (meal) {
                    MealType.BREAKFAST -> Res.string.reminders_time_dialog_breakfast
                    MealType.DINNER -> Res.string.reminders_time_dialog_dinner
                    else -> Res.string.reminders_time_dialog_lunch
                },
            ),
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

/** One meal's reminder on its own card: icon, name, the time (tap to change) and its switch. */
@Composable
private fun ReminderCard(
    reminder: MealReminder,
    onEnabledChange: (Boolean) -> Unit,
    onEditTime: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
            MealTypeIcon(reminder.meal)
            Text(
                reminder.meal.displayLabel(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 16.dp),
            )
            TextButton(onClick = onEditTime) {
                Text(
                    timeLabel(reminder.hour, reminder.minute),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (reminder.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = reminder.enabled, onCheckedChange = onEnabledChange, modifier = Modifier.padding(start = 4.dp))
        }
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
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text(stringResource(Res.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
