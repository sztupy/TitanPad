package scot.raven.titanpad.settings.ui.activation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import scot.raven.titanpad.core.constants.ApplicationConstants
import scot.raven.titanpad.core.ui.KeyCaptureOverlay
import scot.raven.titanpad.settings.domain.AppListType
import scot.raven.titanpad.settings.domain.UsageConfig
import scot.raven.titanpad.settings.ui.DropdownPreferenceItem
import scot.raven.titanpad.settings.ui.PreferenceCategory
import scot.raven.titanpad.settings.ui.SetKeyPreferenceItem
import scot.raven.titanpad.settings.ui.SettingsState
import scot.raven.titanpad.settings.ui.SimplePreferenceItem
import scot.raven.titanpad.settings.ui.SliderPreferenceItem
import scot.raven.titanpad.settings.ui.SwitchPreferenceItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivationScreen(
    settingsState: SettingsState,
    onNavigateToEnableAppsScreen: () -> Unit,
    onNavigateToAutoHideSettings: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val uiState by settingsState.uiState.collectAsState()
    var showCursorKeyCaptureOverlay by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activation settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            PreferenceCategory(title = "Keyboard activation") {
                SetKeyPreferenceItem(
                    title = "Set Activation Key",
                    currentKeyCode = uiState.cursorActivationKey,
                    onCaptureKey = {
                        settingsState.requestHideAllOverlays()
                        showCursorKeyCaptureOverlay = true
                    },
                )

                SimplePreferenceItem(
                    title = "Clear Activation Key",
                    subtitle = "Removes activation key",
                    onClick = {
                        settingsState.updateCursorActivationKey(UsageConfig.KEY_NONE)
                    },
                )

                if (showCursorKeyCaptureOverlay) {
                    KeyCaptureOverlay(
                        onKeySelected = { settingsState.updateCursorActivationKey(it) },
                        onDismiss = { showCursorKeyCaptureOverlay = false },
                        showToast = { message -> settingsState.showToast(message) },
                    )
                }

                SliderPreferenceItem(
                    title = "Activation Keypress Minimum Duration",
                    value = uiState.activationDuration.toFloat(),
                    valueRange = ApplicationConstants.MIN_ACTIVATION_HOLD_DURATION.toFloat()..ApplicationConstants.MAX_ACTIVATION_HOLD_DURATION.toFloat(),
                    valueText = "${uiState.activationDuration} ms",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(activationDuration = v.toLong())
                        }
                    },
                    steps = 4,
                )
            }

            PreferenceCategory(title = "App based activation") {
                DropdownPreferenceItem(
                    title = "Activation AppList Behaviour",
                    subtitle =
                        when (uiState.autoEnableListType) {
                            AppListType.ALLOW_LIST -> "Auto enable config for applications on this list"
                            AppListType.DENY_LIST -> "Auto enable config for every other application not on this list"
                        },
                    selectedOption = uiState.autoEnableListType,
                    options =
                        listOf(
                            AppListType.ALLOW_LIST to "Allow list",
                            AppListType.DENY_LIST to "Ignore list"
                        ),
                    onOptionSelected = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(autoEnableListType = v)
                        }
                    }
                )

                SimplePreferenceItem(
                    title = "Select Applications for Activation",
                    subtitle = "${if (uiState.autoEnableListType == AppListType.ALLOW_LIST) "Auto Enable" else "Ignore"} in specific apps",
                    onClick = onNavigateToEnableAppsScreen,
                )

                SwitchPreferenceItem(
                    title = "Only enable if TitanPad already running",
                    subtitle = if (uiState.autoEnableIfOnOnly) "Only switch if any other config is already enabled" else "Start this config even if TitanPad is not yet active",
                    checked = uiState.autoEnableIfOnOnly,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(autoEnableIfOnOnly = v)
                        }
                    },
                )

                DropdownPreferenceItem(
                    title = "Action when switching away",
                    subtitle =
                        when (uiState.autoDisableActivity) {
                            in uiState.configList.keys -> "Switch to ${uiState.configList.getValue(uiState.autoDisableActivity)}"
                            "default" -> "Switch to default settings"
                            "disable" -> "Disable config"
                            else -> "Keep active"
                        },
                    selectedOption = uiState.autoDisableActivity,
                    options =
                        listOf(
                            "" to "Keep active",
                            "disable" to "Disable config",
                            "default" to "Switch to default",
                        ) +
                        uiState.configList.map{ e -> e.key to "Switch to ${e.value}" },
                    onOptionSelected = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(autoDisableActivity = v)
                        }
                    },
                )
            }

            PreferenceCategory(title = "Auto hide settings") {
                SimplePreferenceItem(
                    title = "Set Up Auto-Hide Options",
                    subtitle = "Automatically hide, but not disable the config on various events",
                    onClick = onNavigateToAutoHideSettings
                )
            }
        }
    }
}