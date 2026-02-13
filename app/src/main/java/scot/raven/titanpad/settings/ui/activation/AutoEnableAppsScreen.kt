package scot.raven.titanpad.settings.ui.activation

import androidx.compose.runtime.Composable
import scot.raven.titanpad.settings.ui.AppListScreen
import scot.raven.titanpad.settings.ui.SettingsState
import scot.raven.titanpad.settings.ui.SettingsUiState

@Composable
fun AutoEnableAppsScreen(
    settingsState: SettingsState,
    onNavigateBack: () -> Unit
) {
    AppListScreen(settingsState, {it: SettingsUiState -> it.autoEnableApps}, {settings, v -> settings.copy(autoEnableApps = v)}, onNavigateBack)
}