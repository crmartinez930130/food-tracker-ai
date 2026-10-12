package com.foodtracker.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.createHttpClient
import com.foodtracker.app.data.defaultBackendBaseUrl
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
import food_tracker_frontend.composeapp.generated.resources.settings_server
import food_tracker_frontend.composeapp.generated.resources.settings_server_connected
import food_tracker_frontend.composeapp.generated.resources.settings_server_failed
import food_tracker_frontend.composeapp.generated.resources.settings_server_field
import food_tracker_frontend.composeapp.generated.resources.settings_server_hint
import food_tracker_frontend.composeapp.generated.resources.settings_server_invalid
import food_tracker_frontend.composeapp.generated.resources.settings_server_restore
import food_tracker_frontend.composeapp.generated.resources.settings_server_save
import food_tracker_frontend.composeapp.generated.resources.settings_server_saved
import food_tracker_frontend.composeapp.generated.resources.settings_server_test
import food_tracker_frontend.composeapp.generated.resources.settings_server_testing
import food_tracker_frontend.composeapp.generated.resources.settings_server_timeout
import food_tracker_frontend.composeapp.generated.resources.settings_theme
import food_tracker_frontend.composeapp.generated.resources.settings_theme_dark
import food_tracker_frontend.composeapp.generated.resources.settings_theme_light
import food_tracker_frontend.composeapp.generated.resources.settings_theme_system
import food_tracker_frontend.composeapp.generated.resources.settings_title
import food_tracker_frontend.composeapp.generated.resources.unknown_error
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
            Spacer(Modifier.height(16.dp))
            ServerSection(
                preferences = preferences,
                onSave = { url -> onPreferencesChange(preferences.copy(serverUrl = url)) },
            )
        }
    }
}

/** What the Server section last reported. */
private sealed interface ServerStatus {
    data object Invalid : ServerStatus
    data object Saved : ServerStatus
    data object Testing : ServerStatus
    data object Connected : ServerStatus
    data object Timeout : ServerStatus
    data class Failed(val detail: String?) : ServerStatus
}

/** How long "Test connection" waits before reporting the server as unreachable. */
private const val CONNECTION_TEST_TIMEOUT_MS = 5_000L

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServerSection(preferences: AppPreferences, onSave: (String?) -> Unit) {
    var text by remember(preferences.serverUrl) { mutableStateOf(effectiveServerUrl(preferences)) }
    var status by remember { mutableStateOf<ServerStatus?>(null) }
    val scope = rememberCoroutineScope()

    Text(stringResource(Res.string.settings_server), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Text(
        stringResource(Res.string.settings_server_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            status = null
        },
        label = { Text(stringResource(Res.string.settings_server_field)) },
        singleLine = true,
        isError = status == ServerStatus.Invalid,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth(),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        Button(onClick = {
            val url = normalizeServerUrl(text)
            if (url == null) {
                status = ServerStatus.Invalid
            } else {
                text = url
                onSave(url)
                status = ServerStatus.Saved
            }
        }) { Text(stringResource(Res.string.settings_server_save)) }
        OutlinedButton(
            onClick = {
                val url = normalizeServerUrl(text)
                if (url == null) {
                    status = ServerStatus.Invalid
                    return@OutlinedButton
                }
                status = ServerStatus.Testing
                scope.launch { status = testConnection(url) }
            },
            enabled = status != ServerStatus.Testing,
        ) { Text(stringResource(Res.string.settings_server_test)) }
        TextButton(onClick = {
            onSave(null)
            text = defaultBackendBaseUrl()
            status = null
        }) { Text(stringResource(Res.string.settings_server_restore)) }
    }
    status?.let { current ->
        val isProblem = current == ServerStatus.Invalid || current == ServerStatus.Timeout || current is ServerStatus.Failed
        Text(
            when (current) {
                ServerStatus.Invalid -> stringResource(Res.string.settings_server_invalid)
                ServerStatus.Saved -> stringResource(Res.string.settings_server_saved)
                ServerStatus.Testing -> stringResource(Res.string.settings_server_testing)
                ServerStatus.Connected -> stringResource(Res.string.settings_server_connected)
                ServerStatus.Timeout -> stringResource(Res.string.settings_server_timeout)
                is ServerStatus.Failed ->
                    stringResource(Res.string.settings_server_failed, current.detail ?: stringResource(Res.string.unknown_error))
            },
            color = if (isProblem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Tries the typed (not necessarily saved) address with a throwaway client. */
private suspend fun testConnection(url: String): ServerStatus {
    // Timeouts at the engine level too: a blocking connect to a host that drops packets (firewall,
    // sleeping PC) may not react to coroutine cancellation, which is all withTimeoutOrNull does.
    val httpClient = createHttpClient().config {
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECTION_TEST_TIMEOUT_MS
            socketTimeoutMillis = CONNECTION_TEST_TIMEOUT_MS
            requestTimeoutMillis = CONNECTION_TEST_TIMEOUT_MS
        }
    }
    return try {
        withTimeoutOrNull(CONNECTION_TEST_TIMEOUT_MS) {
            FoodTrackerApiClient(httpClient) { url }.getGoals()
            ServerStatus.Connected
        } ?: ServerStatus.Timeout
    } catch (e: HttpRequestTimeoutException) {
        ServerStatus.Timeout
    } catch (e: ConnectTimeoutException) {
        ServerStatus.Timeout
    } catch (e: SocketTimeoutException) {
        ServerStatus.Timeout
    } catch (e: Exception) {
        ServerStatus.Failed(e.message)
    } finally {
        httpClient.close()
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
