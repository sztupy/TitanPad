package scot.raven.titanpad.settings.ui.setup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import scot.raven.titanpad.settings.ui.NoteItem
import scot.raven.titanpad.settings.ui.PreferenceCategory
import scot.raven.titanpad.settings.ui.SettingsState
import scot.raven.titanpad.settings.ui.SimplePreferenceItem
import scot.raven.titanpad.settings.ui.SliderPreferenceItem
import scot.raven.titanpad.settings.ui.SwitchPreferenceItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.cursor.domain.FuncButtonMap
import scot.raven.titanpad.cursor.domain.InputType
import scot.raven.titanpad.settings.ui.DropdownPreferenceItem
import scot.raven.titanpad.settings.ui.InputSelectorItem
import scot.raven.titanpad.settings.ui.TextFieldDialog
import scot.raven.titanpad.settings.ui.rememberDocumentCreateLauncher
import scot.raven.titanpad.settings.ui.rememberDocumentLoaderLauncher
import scot.raven.titanpad.settings.ui.startActivity

/**
 * Standard cursor settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageConfigurationScreen(
    settingsState: SettingsState,
    onNavigateToDebugOptions: () -> Unit,
    onNavigateToAutoHideSettings: () -> Unit,
    onNavigateToSoftwareEmulationSettings: () -> Unit,
    onNavigateToScrollSettings: (Int) -> () -> Unit,
    onNavigateToWheelSettings: (Int) -> () -> Unit,
    onNavigateToActivationSettings: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val uiState by settingsState.uiState.collectAsState()
    var showNameChangeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Input configuration - ${uiState.configName}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            PreferenceCategory(title = "Configuration") {
                SimplePreferenceItem(
                    title = "Configuration ID",
                    subtitle = uiState.configId,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("TitanPad Config ID", uiState.configId)
                        clipboard.setPrimaryClip(clip)

                        settingsState.showToast("TitanPad Configuration ID copied to Clipboard")
                    }
                )

                SimplePreferenceItem(
                    title = "Configuration Name",
                    subtitle = uiState.configName,
                    onClick = { showNameChangeDialog = true }
                )

                if (showNameChangeDialog) {
                    TextFieldDialog(
                        title = "Configuration Name",
                        initialValue = uiState.configName,
                        subtitle = "Set configuration name",
                        label = "Name",
                        onUpdate = { configName ->
                            settingsState.updatePreference(configName) { settings, v ->
                                settings.copy(configName = v)
                            }
                        },
                        onDismiss = { showNameChangeDialog = false }
                    )
                }
            }

            PreferenceCategory(title = "Activation") {
                SimplePreferenceItem(
                    title = "Automated Activation Setup",
                    subtitle = "Set up ways to enable and disable this config automatically",
                    onClick = onNavigateToActivationSettings
                )
            }

            PreferenceCategory(title = "Inputs") {
                NoteItem(
                    "All hardware emulation features require a Shizuku version that has working MTK phone support. The latest official Shizuku version v13.6.0 will NOT work.",
                    Icons.Default.Warning,
                    "Warning"
                )
                InputSelectorItem(
                    title = "Trackpad behavior",
                    selectedInputType = uiState.touchPadMainInputType,
                    onOptionSelected = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(touchPadMainInputType = v)
                        }
                    }
                )
                SwitchPreferenceItem(
                    title = "Disable top row touch",
                    subtitle = if (uiState.touchpadDisableTopRow) "Touching the top row will not trigger touch events" else "The entire keyboard is used for touch events",
                    checked = uiState.touchpadDisableTopRow,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(touchpadDisableTopRow = v)
                        }
                    },
                )
                SwitchPreferenceItem(
                    title = "Separate left side",
                    subtitle = if (uiState.touchpadSplitInput) "Use different configuration for the left side" else "Use same configuration as the centre",
                    checked = uiState.touchpadSplitInput,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(touchpadSplitInput = v)
                        }
                    },
                )
                if (uiState.touchpadSplitInput) {
                    InputSelectorItem(
                        title = "Trackpad left side behavior",
                        selectedInputType = uiState.touchPadLeftInputType,
                        onOptionSelected = { value ->
                            settingsState.updatePreference(value) { settings, v ->
                                settings.copy(touchPadLeftInputType = v)
                            }
                        },
                    )

                    SliderPreferenceItem(
                        title = "TouchPad split location",
                        value = uiState.touchpadSplitPosition.toFloat(),
                        valueRange = 0f .. 100f,
                        valueText = "${uiState.touchpadSplitPosition}%",
                        onValueChange = { value ->
                            settingsState.updatePreference(value) { settings, v ->
                                settings.copy(touchpadSplitPosition = v.toInt())
                            }
                        },
                        steps = 19,
                    )
                }

                SwitchPreferenceItem(
                    title = "Separate right side",
                    subtitle = if (uiState.touchpadSplitRightInput) "Use different configuration for the right side" else "Use same configuration as the centre",
                    checked = uiState.touchpadSplitRightInput,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(touchpadSplitRightInput = v)
                        }
                    },
                )
                if (uiState.touchpadSplitRightInput) {
                    InputSelectorItem(
                        title = "Trackpad right side behavior",
                        selectedInputType = uiState.touchPadRightInputType,
                        onOptionSelected = { value ->
                            settingsState.updatePreference(value) { settings, v ->
                                settings.copy(touchPadRightInputType = v)
                            }
                        },
                    )

                    SliderPreferenceItem(
                        title = "TouchPad right split location",
                        value = uiState.touchpadSplitRightPosition.toFloat(),
                        valueRange = 0f .. 100f,
                        valueText = "${uiState.touchpadSplitRightPosition}%",
                        onValueChange = { value ->
                            settingsState.updatePreference(value) { settings, v ->
                                settings.copy(touchpadSplitRightPosition = v.toInt())
                            }
                        },
                        steps = 19,
                    )
                }

                InputSelectorItem(
                    title = "Back screen behavior",
                    selectedInputType = uiState.backScreenInputType,
                    onOptionSelected = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(backScreenInputType = v)
                        }
                    }
                )
            }

            val combinedInputTypes = (setOf(
                uiState.touchPadMainInputType,
                uiState.backScreenInputType
            ) + (if (uiState.touchpadSplitInput) uiState.touchPadLeftInputType else uiState.touchPadMainInputType)) +
                (if (uiState.touchpadSplitRightInput) uiState.touchPadRightInputType else uiState.touchPadMainInputType)

            if (combinedInputTypes.contains(InputType.HARDWARE_MOUSE) || combinedInputTypes.contains(InputType.SOFTWARE_MOUSE)) {
                PreferenceCategory(title = "Mouse settings") {
                    SwitchPreferenceItem(
                        title = "Tap To Click",
                        subtitle = "Convert single taps to click events",
                        checked = uiState.mouseTapToClick,
                        onCheckedChange = { value ->
                            settingsState.updatePreference(value) { settings, v ->
                                settings.copy(mouseTapToClick = v)
                            }
                        },
                    )

                    if (uiState.mouseTapToClick) {
                        SwitchPreferenceItem(
                            title = "Double Tap To Drag",
                            subtitle = "Convert double taps to drag and hold events",
                            checked = uiState.mouseDoubleTapToHold,
                            onCheckedChange = { value ->
                                settingsState.updatePreference(value) { settings, v ->
                                    settings.copy(mouseDoubleTapToHold = v)
                                }
                            },
                        )

                        SliderPreferenceItem(
                            title = "Tap Click Sensitivity",
                            value = uiState.mouseTapMaxDuration.toFloat(),
                            valueRange = 25f..300f,
                            valueText = "${uiState.mouseTapMaxDuration}ms",
                            onValueChange = { value ->
                                settingsState.updatePreference(value) { settings, v ->
                                    settings.copy(mouseTapMaxDuration = v.toInt())
                                }
                            },
                            steps = 10,
                        )
                    }

                    SwitchPreferenceItem(
                        title = "Two Finger Touch Clicks",
                        subtitle = "Convert multi touch taps to click / drag / hold events",
                        checked = uiState.mouseTwoFingerToHold,
                        onCheckedChange = { value ->
                            settingsState.updatePreference(value) { settings, v ->
                                settings.copy(mouseTwoFingerToHold = v)
                            }
                        },
                    )

                    if (uiState.mouseTwoFingerToHold) {
                        SliderPreferenceItem(
                            title = "Multi-Touch Sensitivity",
                            value = uiState.twoFingerSensitivity.toFloat(),
                            valueRange = 5f..13f,
                            valueText = "${uiState.twoFingerSensitivity}",
                            onValueChange = { value ->
                                settingsState.updatePreference(value) { settings, v ->
                                    settings.copy(twoFingerSensitivity = v.toInt())
                                }
                            },
                            steps = 7,
                        )
                    }
                }
            }

            if (combinedInputTypes.contains(InputType.HARDWARE_SCROLL) || combinedInputTypes.contains(InputType.SOFTWARE_SCROLL)) {
                PreferenceCategory(title = "Scroll settings") {
                    if (listOf(InputType.HARDWARE_SCROLL, InputType.SOFTWARE_SCROLL).contains(uiState.touchPadMainInputType)) {
                        SimplePreferenceItem(
                            title = "Main TouchPad scroll",
                            subtitle = "Change scroll options for main touchpad",
                            onClick = onNavigateToScrollSettings(0)
                        )
                    }

                    if (uiState.touchpadSplitInput && listOf(InputType.HARDWARE_SCROLL, InputType.SOFTWARE_SCROLL).contains(uiState.touchPadLeftInputType)) {
                        SimplePreferenceItem(
                            title = "Left TouchPad scroll",
                            subtitle = "Change scroll options for left side of touchpad",
                            onClick = onNavigateToScrollSettings(1)
                        )
                    }

                    if (uiState.touchpadSplitRightInput && listOf(InputType.HARDWARE_SCROLL, InputType.SOFTWARE_SCROLL).contains(uiState.touchPadRightInputType)) {
                        SimplePreferenceItem(
                            title = "Right TouchPad scroll",
                            subtitle = "Change scroll options for right side of touchpad",
                            onClick = onNavigateToScrollSettings(2)
                        )
                    }

                    if (listOf(InputType.HARDWARE_SCROLL, InputType.SOFTWARE_SCROLL).contains(uiState.backScreenInputType)) {
                        SimplePreferenceItem(
                            title = "Back screen scroll",
                            subtitle = "Change scroll options for back screen",
                            onClick = onNavigateToScrollSettings(3)
                        )
                    }
                }
            }

            if (combinedInputTypes.contains(InputType.HARDWARE_WHEEL)) {
                PreferenceCategory(title = "Wheel settings") {
                    if (listOf(InputType.HARDWARE_WHEEL).contains(uiState.touchPadMainInputType)) {
                        SimplePreferenceItem(
                            title = "Main TouchPad wheel",
                            subtitle = "Change wheel options for main touchpad",
                            onClick = onNavigateToWheelSettings(0)
                        )
                    }

                    if (uiState.touchpadSplitInput && listOf(InputType.HARDWARE_WHEEL).contains(uiState.touchPadLeftInputType)) {
                        SimplePreferenceItem(
                            title = "Left TouchPad wheel",
                            subtitle = "Change wheel options for left side of touchpad",
                            onClick = onNavigateToWheelSettings(1)
                        )
                    }

                    if (uiState.touchpadSplitRightInput && listOf(InputType.HARDWARE_WHEEL).contains(uiState.touchPadRightInputType)) {
                        SimplePreferenceItem(
                            title = "Right TouchPad wheel",
                            subtitle = "Change wheel options for right side of touchpad",
                            onClick = onNavigateToWheelSettings(2)
                        )
                    }

                    if (listOf(InputType.HARDWARE_WHEEL).contains(uiState.backScreenInputType)) {
                        SimplePreferenceItem(
                            title = "Back screen wheel",
                            subtitle = "Change wheel options for back screen",
                            onClick = onNavigateToWheelSettings(3)
                        )
                    }
                }
            }

            PreferenceCategory(title = "Software Emulation") {
                SimplePreferenceItem(
                    title = "Software Emulation Setup",
                    subtitle = "Change software emulated input settings",
                    onClick = onNavigateToSoftwareEmulationSettings
                )
            }

            PreferenceCategory(title = "Hardware Emulation") {
                SimplePreferenceItem(
                    title = "Cursor Size",
                    subtitle = "Found under 'Display' -> 'Colour and Motion' -> 'Large mouse cursor'",
                    onClick = {
                        if (!startActivity($$"com.android.settings/.Settings$ColorAndMotionActivity")) {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    }
                )

                SimplePreferenceItem(
                    title = "Sensitivity settings",
                    subtitle = "Found under 'System' -> 'Keyboard' -> 'Pointer Speed'",
                    onClick = {
                        if (!startActivity($$"com.android.settings/.Settings$KeyboardSettingsActivity")) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                )

                SimplePreferenceItem(
                    title = "Show visualization",
                    subtitle = "Found under 'Developer Options' -> 'Input' -> 'Show taps'",
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                    }
                )

                DropdownPreferenceItem(
                    title = "Func 1 (top left) button usage",
                    subtitle =
                        when (uiState.func1ButtonMap) {
                            FuncButtonMap.OFF -> "None"
                            FuncButtonMap.MOUSE_LEFT_CLICK -> "Left click"
                            FuncButtonMap.MOUSE_RIGHT_CLICK -> "Right click"
                            FuncButtonMap.MOUSE_MIDDLE_CLICK -> "Middle click"
                        },
                    selectedOption = uiState.func1ButtonMap,
                    options =
                        listOf(
                            FuncButtonMap.OFF to "None",
                            FuncButtonMap.MOUSE_LEFT_CLICK to "Left click",
                            FuncButtonMap.MOUSE_RIGHT_CLICK to "Right click",
                            FuncButtonMap.MOUSE_MIDDLE_CLICK to "Middle click"
                        ),
                    onOptionSelected = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(func1ButtonMap = v)
                        }
                    },
                )

                DropdownPreferenceItem(
                    title = "Func 2 (bottom left) button usage",
                    subtitle =
                        when (uiState.func2ButtonMap) {
                            FuncButtonMap.OFF -> "None"
                            FuncButtonMap.MOUSE_LEFT_CLICK -> "Left click"
                            FuncButtonMap.MOUSE_RIGHT_CLICK -> "Right click"
                            FuncButtonMap.MOUSE_MIDDLE_CLICK -> "Middle click"
                        },
                    selectedOption = uiState.func2ButtonMap,
                    options =
                        listOf(
                            FuncButtonMap.OFF to "None",
                            FuncButtonMap.MOUSE_LEFT_CLICK to "Left click",
                            FuncButtonMap.MOUSE_RIGHT_CLICK to "Right click",
                            FuncButtonMap.MOUSE_MIDDLE_CLICK to "Middle click"
                        ),
                    onOptionSelected = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(func2ButtonMap = v)
                        }
                    },
                )
            }

            PreferenceCategory(title = "Behavior") {
                SimplePreferenceItem(
                    title = "Set Up Auto-Hide Options",
                    subtitle = "Automatically hide, but not disable the config on various events",
                    onClick = onNavigateToAutoHideSettings
                )

                if (uiState.activationDuration == 0L) {
                    NoteItem(
                        title = "Activation keys will be fully intercepted",
                        icon = Icons.Default.Warning,
                        contentDescription = "Warning",
                        color = Color(0xFFFFF4E6),
                    )
                }

                SwitchPreferenceItem(
                    title = "Show Notification Icon",
                    subtitle = "Show icon when config is activated",
                    checked = uiState.showNotification,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(showNotification = v)
                        }
                    },
                )
            }

            PreferenceCategory(title = "Advanced") {
                SimplePreferenceItem(
                    title = "Developer Options",
                    subtitle = "Additional configurable features",
                    onClick = onNavigateToDebugOptions
                )
            }

            PreferenceCategory(title = "Backup and Restore") {
                val backupLauncher = rememberDocumentCreateLauncher(
                    settingsState = settingsState,
                    coroutineScope = coroutineScope,
                    context = LocalContext.current,
                    fileName = "titanpad-config",
                    inputCallback = { callback ->
                        coroutineScope.launch {
                            val currentSettings =
                                TitanPad.getInstance().settingsRepository.getSettings().first()

                            val currentId = uiState.configId
                            val selectedConfig = currentSettings.additionalConfigs.find{it.configId == currentId}?:currentSettings.defaultConfig

                            val result = TitanPad.getInstance().settingsRepository.exportSettings(currentId,selectedConfig)

                            callback(result)
                        }
                    }
                )

                val restoreLauncher = rememberDocumentLoaderLauncher(
                    settingsState = settingsState,
                    coroutineScope = coroutineScope,
                    context = LocalContext.current,
                    outputCallback = { jsonData ->
                        coroutineScope.launch {
                            try {
                                if (TitanPad.getInstance().settingsRepository.importSettings(uiState.configId, jsonData)) {
                                    settingsState.showToast("Backup restored!")
                                } else {
                                    settingsState.showToast("Could not restore backup!")
                                }
                            } catch (_: Exception) {
                                settingsState.showToast("Could not restore backup!")
                            }
                        }
                    }
                )

                val backupLauncherNoApp = rememberDocumentCreateLauncher(
                    settingsState = settingsState,
                    coroutineScope = coroutineScope,
                    context = LocalContext.current,
                    fileName = "titanpad-config-no-app",
                    inputCallback = { callback ->
                        coroutineScope.launch {
                            val currentSettings =
                                TitanPad.getInstance().settingsRepository.getSettings().first()

                            val currentId = uiState.configId
                            val selectedConfig = currentSettings.additionalConfigs.find{it.configId == currentId}?:currentSettings.defaultConfig

                            val result = TitanPad.getInstance().settingsRepository.exportSettingsWithoutAppData(currentId,selectedConfig)

                            callback(result)
                        }
                    }
                )

                val restoreLauncherNoApp = rememberDocumentLoaderLauncher(
                    settingsState = settingsState,
                    coroutineScope = coroutineScope,
                    context = LocalContext.current,
                    outputCallback = { jsonData ->
                        coroutineScope.launch {
                            try {
                                val currentSettings =
                                    TitanPad.getInstance().settingsRepository.getSettings().first()

                                val currentId = uiState.configId
                                val selectedConfig = currentSettings.additionalConfigs.find{it.configId == currentId}?:currentSettings.defaultConfig

                                if (TitanPad.getInstance().settingsRepository.importSettingsWithoutAppData(uiState.configId, jsonData, selectedConfig)) {
                                    settingsState.showToast("Backup restored!")
                                } else {
                                    settingsState.showToast("Could not restore backup!")
                                }

                            } catch (_: Exception) {
                                settingsState.showToast("Could not restore backup!")
                            }
                        }
                    }
                )

                SimplePreferenceItem(
                    title = "Backup Configuration",
                    subtitle = "Save your config to a file",
                    onClick = { backupLauncher() }
                )

                SimplePreferenceItem(
                    title = "Restore Configuration",
                    subtitle = "Load an existing backup, replacing the current config",
                    onClick = { restoreLauncher() }
                )

                SimplePreferenceItem(
                    title = "Backup Configuration without App and Activation data",
                    subtitle = "Save your config to a file except any AppList and Activation settings",
                    onClick = { backupLauncherNoApp() }
                )

                SimplePreferenceItem(
                    title = "Restore Configuration without App and Activation data",
                    subtitle = "Load an existing backup, replacing the current config except the Activation setup and any AppLists",
                    onClick = { restoreLauncherNoApp() }
                )
            }
        }
    }
}