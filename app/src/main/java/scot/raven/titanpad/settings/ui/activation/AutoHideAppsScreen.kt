package scot.raven.titanpad.settings.ui.activation

import androidx.compose.runtime.Composable
import scot.raven.titanpad.settings.ui.AppListScreen
import scot.raven.titanpad.settings.ui.SettingsState
import scot.raven.titanpad.settings.ui.SettingsUiState

@Composable
fun AutoHideAppsScreen(
    settingsState: SettingsState,
    onNavigateBack: () -> Unit
) {
    AppListScreen(settingsState, {it: SettingsUiState -> it.autoHideApps}, {settings, v -> settings.copy(autoHideApps = v)}, onNavigateBack)
}