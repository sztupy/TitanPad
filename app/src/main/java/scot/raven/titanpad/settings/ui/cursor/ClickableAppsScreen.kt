package scot.raven.titanpad.settings.ui.cursor

import androidx.compose.runtime.Composable
import scot.raven.titanpad.settings.ui.AppListScreen
import scot.raven.titanpad.settings.ui.SettingsState
import scot.raven.titanpad.settings.ui.SettingsUiState

@Composable
fun ClickableAppsScreen(
    settingsState: SettingsState,
    onNavigateBack: () -> Unit
) {
    AppListScreen(settingsState, {it: SettingsUiState -> it.clickableApps}, {settings, v -> settings.copy(clickableApps = v)}, onNavigateBack)
}