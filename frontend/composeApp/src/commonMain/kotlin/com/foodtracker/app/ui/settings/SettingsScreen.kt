package com.foodtracker.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.action_back
import food_tracker_frontend.composeapp.generated.resources.ic_arrow_back
import food_tracker_frontend.composeapp.generated.resources.ic_chevron_right
import food_tracker_frontend.composeapp.generated.resources.settings_language
import food_tracker_frontend.composeapp.generated.resources.settings_language_english
import food_tracker_frontend.composeapp.generated.resources.settings_language_spanish
import food_tracker_frontend.composeapp.generated.resources.settings_language_system
import food_tracker_frontend.composeapp.generated.resources.settings_reminders
import food_tracker_frontend.composeapp.generated.resources.settings_reminders_hint
import food_tracker_frontend.composeapp.generated.resources.settings_theme
import food_tracker_frontend.composeapp.generated.resources.settings_theme_dark
import food_tracker_frontend.composeapp.generated.resources.settings_theme_light
import food_tracker_frontend.composeapp.generated.resources.settings_theme_system
import food_tracker_frontend.composeapp.generated.resources.settings_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val LANGUAGE_LABELS = listOf(
    LanguagePreference.SYSTEM to Res.string.settings_language_system,
    LanguagePreference.SPANISH to Res.string.settings_language_spanish,
    LanguagePreference.ENGLISH to Res.string.settings_language_english,
)

private val THEME_LABELS = listOf(
    ThemePreference.SYSTEM to Res.string.settings_theme_system,
    ThemePreference.LIGHT to Res.string.settings_theme_light,
    ThemePreference.DARK to Res.string.settings_theme_dark,
)

/** Options: language, theme and the way into Reminders. Every choice applies (and persists) on tap. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    onPreferencesChange: (AppPreferences) -> Unit,
    onOpenReminders: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
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
            ChoiceGroup(
                title = Res.string.settings_language,
                options = LANGUAGE_LABELS,
                selected = preferences.language,
                onSelect = { onPreferencesChange(preferences.copy(language = it)) },
            )
            Spacer(Modifier.height(16.dp))
            ChoiceGroup(
                title = Res.string.settings_theme,
                options = THEME_LABELS,
                selected = preferences.theme,
                onSelect = { onPreferencesChange(preferences.copy(theme = it)) },
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenReminders).padding(vertical = 16.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.settings_reminders), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(Res.string.settings_reminders_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(painterResource(Res.drawable.ic_chevron_right), contentDescription = null)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun <T> ChoiceGroup(
    title: StringResource,
    options: List<Pair<T, StringResource>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Column(Modifier.selectableGroup()) {
        for ((value, label) in options) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = value == selected, onClick = { onSelect(value) }, role = Role.RadioButton)
                    .padding(vertical = 8.dp),
            ) {
                // The row handles the click, so the button itself stays non-interactive for accessibility.
                RadioButton(selected = value == selected, onClick = null)
                Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}
