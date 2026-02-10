package scot.raven.titanpad.settings.ui.cursor

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
import scot.raven.titanpad.core.constants.CursorConstants
import scot.raven.titanpad.core.constants.GestureConstants
import scot.raven.titanpad.settings.domain.AppListType
import scot.raven.titanpad.settings.ui.ColorPickerDialog
import scot.raven.titanpad.settings.ui.DropdownPreferenceItem
import scot.raven.titanpad.settings.ui.PreferenceCategory
import scot.raven.titanpad.settings.ui.SettingsState
import scot.raven.titanpad.settings.ui.SimplePreferenceItem
import scot.raven.titanpad.settings.ui.SliderPreferenceItem
import scot.raven.titanpad.settings.ui.SwitchPreferenceItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftwareEmulationSettingsScreen(
    settingsState: SettingsState,
    onNavigateBack: () -> Unit,
    onNavigateToClickableAppsScreen: () -> Unit,
    onNavigateToCursorIcon: () -> Unit,
    onNavigateToLocationClickableIcon: () -> Unit
) {
    val uiState by settingsState.uiState.collectAsState()
    var showColorPickerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Software Emulation Settings") },
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
            PreferenceCategory(title = "Sensitivity") {
                SliderPreferenceItem(
                    title = "Mouse Sensitivity",
                    value = uiState.softwareMouseSensitivity.toFloat(),
                    valueRange = 1f..9f,
                    valueText = "${uiState.softwareMouseSensitivity}",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(softwareMouseSensitivity = v.toInt())
                        }
                    },
                    steps = 7,
                )

                SwitchPreferenceItem(
                    title = "Exponential sensitivity",
                    subtitle = if (uiState.softwareMouseExponential) "Mouse moves quicker on quicker swipes" else "Mouse moves the same regardless of swipe speed",
                    checked = uiState.softwareMouseExponential,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(softwareMouseExponential = v)
                        }
                    },
                )
            }

            PreferenceCategory(title = "Adaptive") {
                SwitchPreferenceItem(
                    title = "Show Location Clickable",
                    subtitle = "Attempt to indicate if current cursor location is clickable",
                    checked = uiState.checkClickable,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(checkClickable = v)
                        }
                    },
                )

                DropdownPreferenceItem(
                    title = "Application List Type",
                    subtitle =
                        when (uiState.clickableListType) {
                            AppListType.ALLOW_LIST -> "Show clickable locations only for selected apps"
                            AppListType.DENY_LIST -> "Do not show clickable locations for selected apps"
                        },
                    selectedOption = uiState.clickableListType,
                    options =
                        listOf(
                            AppListType.ALLOW_LIST to "Allow",
                            AppListType.DENY_LIST to "Deny"
                        ),
                    onOptionSelected = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(clickableListType = v)
                        }
                    },
                    enabled = uiState.checkClickable
                )

                SimplePreferenceItem(
                    title = "Select Applications",
                    subtitle = "${if (uiState.clickableListType == AppListType.ALLOW_LIST) "Show" else "Ignore"} in specific apps",
                    onClick = onNavigateToClickableAppsScreen,
                    enabled = uiState.checkClickable
                )
            }

            PreferenceCategory(title = "Appearance") {
                SliderPreferenceItem(
                    title = "Cursor Size",
                    value = uiState.cursorSize.toFloat(),
                    valueRange = CursorConstants.MIN_SIZE.toFloat()..CursorConstants.MAX_SIZE.toFloat(),
                    valueText = uiState.cursorSize.toString(),
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(cursorSize = v.toInt())
                        }
                    },
                    steps = 8,
                )

                SwitchPreferenceItem(
                    title = "Smooth Cursor Corners",
                    subtitle = "Round out the corners of the cursor",
                    checked = uiState.roundedCursorCorners,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(roundedCursorCorners = v)
                        }
                    },
                )

                SimplePreferenceItem(
                    title = "Cursor Color",
                    subtitle = "Current RGB hex value: #${uiState.standardCursorHex}",
                    onClick = { showColorPickerDialog = true }
                )

                SwitchPreferenceItem(
                    title = "Match Border to Body",
                    subtitle = "Replace black border and match cursor body color",
                    checked = uiState.standardCursorMatchBorder,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(standardCursorMatchBorder = v)
                        }
                    },
                )

                if (showColorPickerDialog) {
                    ColorPickerDialog(
                        initialColorHex = uiState.standardCursorHex,
                        onColorSelected = { newColorHex ->
                            settingsState.updatePreference(newColorHex) { settings, v ->
                                settings.copy(standardCursorHex = v)
                            }
                        },
                        onDismiss = { showColorPickerDialog = false },
                        title = "Cursor Color"
                    )
                }
            }

            PreferenceCategory(title = "Custom Icon") {
                SwitchPreferenceItem(
                    title = "Custom Cursor Icons",
                    subtitle = "Replace the default cursor icon with an image or gif",
                    checked = uiState.useCustomCursorIcon,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(useCustomCursorIcon = v)
                        }
                    },
                )

                SimplePreferenceItem(
                    title = "Cursor Icon",
                    subtitle = when {
                        uiState.cursorImagePath == null -> "No icon set, falling back to default icon"
                        else -> "Update icon"
                    },
                    onClick = onNavigateToCursorIcon,
                    enabled = uiState.useCustomCursorIcon
                )

                SimplePreferenceItem(
                    title = "Location Clickable Icon",
                    subtitle = when {
                        !uiState.checkClickable -> "Only applicable if \"Show Location Clickable\" is enabled"
                        uiState.clickableImagePath == null -> "Select icon, otherwise falling back to base custom icon"
                        else -> "Update icon"
                    },
                    onClick = onNavigateToLocationClickableIcon,
                    enabled = uiState.useCustomCursorIcon && uiState.checkClickable,
                )
            }

            PreferenceCategory(title = "Gestures") {
                SwitchPreferenceItem(
                    title = "Gesture Visualization",
                    subtitle = "Show gestures on screen",
                    checked = uiState.showGestureVisualization,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(showGestureVisualization = v)
                        }
                    },
                )

                SliderPreferenceItem(
                    title = "Gesture Visualization Size",
                    value = uiState.visualSize.toFloat(),
                    valueRange = GestureConstants.MIN_SIZE.toFloat()..GestureConstants.MAX_SIZE.toFloat(),
                    valueText = uiState.visualSize.toString(),
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(visualSize = v.toInt())
                        }
                    },
                    steps = 8,
                    enabled = uiState.showGestureVisualization
                )
            }
        }
    }
}