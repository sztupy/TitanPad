package scot.raven.titanpad.settings.ui

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import scot.raven.titanpad.core.util.VersionUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugOptionsScreen(
    settingsState: SettingsState,
    onNavigateBack: () -> Unit
) {
    val uiState by settingsState.uiState.collectAsState()
    var showPassthroughDialog by remember { mutableStateOf(false) }
    var showTouchscreenDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Options") },
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
//            PreferenceCategory(title = "Logging") {
//                SwitchPreferenceItem(
//                    title = "Collect Logs",
//                    subtitle = "Logs will be written to the log screen",
//                    checked = uiState.collectLogs,
//                    onCheckedChange = { value ->
//                        settingsState.updatePreference(value) { settings, v ->
//                            settings.copy(collectLogs = v)
//                        }
//                    }
//                )
//                SimplePreferenceItem(
//                    title = "Log Screen",
//                    subtitle = "View application logs",
//                    onClick = onNavigateToLogScreen,
//                )
//            }

            PreferenceCategory(title = "Gestures") {
                SwitchPreferenceItem(
                    title = "Overlapping Gestures",
                    subtitle = "Allow manual scrolls and zooms to overlap",
                    checked = uiState.allowOverlappingGestures,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(allowOverlappingGestures = v)
                        }
                    },
                )
            }

            PreferenceCategory(title = "Display") {
                SwitchPreferenceItem(
                    title = "Use Physical Size",
                    subtitle = "Overlay cursor over the entire screen, including any navigation bars",
                    checked = uiState.usePhysicalSize,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(usePhysicalSize = v)
                        }
                    },
                )
            }

            PreferenceCategory(title = "Experimental") {
                SwitchPreferenceItem(
                    title = "Disable Touchscreen",
                    subtitle = "Disables the touchscreen except during button presses",
                    checked = uiState.disableTouchscreen,
                    onCheckedChange = { newValue ->
                        if (newValue && !uiState.disableTouchscreen) {
                            showTouchscreenDialog = true
                        } else {
                            settingsState.updateDisableTouchscreen(newValue)
                        }
                    },
                )

                SwitchPreferenceItem(
                    title = "Allow Passthrough",
                    subtitle = "Disable key press interception",
                    checked = uiState.allowPassthrough,
                    onCheckedChange = { newValue ->
                        if (newValue && !uiState.allowPassthrough) {
                            showPassthroughDialog = true
                        } else {
                            settingsState.updateAllowPassthrough(newValue)
                        }
                    },
                )
            }

            if (showPassthroughDialog) {
                AlertDialog(
                    onDismissRequest = { showPassthroughDialog = false },
                    title = { Text("Allow Passthrough") },
                    text = {
                        Text("All button presses will be forwarded to the underlying app. This may fix numpad backlight issues but cause unintended behavior with the underlying application.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                settingsState.updateAllowPassthrough(true)
                                showPassthroughDialog = false
                            }
                        ) {
                            Text("Enable")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showPassthroughDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showTouchscreenDialog) {
                AlertDialog(
                    onDismissRequest = { showTouchscreenDialog = false },
                    title = { Text("Disable Touchscreen") },
                    text = {
                        Text("While the cursor is activated, the touchscreen will be disabled except during button presses to allow the cursor to dispatch gestures. Also, this may introduce a small amount of input lag.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                settingsState.updateDisableTouchscreen(true)
                                showTouchscreenDialog = false
                            }
                        ) {
                            Text("Disable")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showTouchscreenDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}