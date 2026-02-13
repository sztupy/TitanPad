package scot.raven.titanpad.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import scot.raven.titanpad.accessibility.AppAccessibilityService
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.cursor.domain.IconAlignment
import scot.raven.titanpad.settings.domain.AppListType
import scot.raven.titanpad.settings.domain.Defaults
import scot.raven.titanpad.settings.domain.UsageConfig
import scot.raven.titanpad.settings.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import scot.raven.titanpad.cursor.domain.FuncButtonMap
import scot.raven.titanpad.cursor.domain.InputType
import scot.raven.titanpad.settings.domain.ApplicationSettings
import scot.raven.titanpad.settings.domain.ScrollConfig

/**
 * Bridges settings with UI.
 */
class SettingsState(
    private val settingsRepository: SettingsRepository,
    configId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _validationErrors = MutableStateFlow<List<String>>(emptyList())
    val validationErrors : StateFlow<List<String>> = _validationErrors.asStateFlow()

    private var toastFunction: ((String) -> Unit)? = null

    fun setToastFunction(toastFn: (String) -> Unit) {
        toastFunction = toastFn
    }

    fun showToast(message: String) {
        toastFunction?.invoke(message)
    }

    init {
        loadSettings(configId)
    }

    private fun loadSettings(configId: String) {
        viewModelScope.launch {
            try {
                settingsRepository.getSettings().collect { applicationSettings ->
                    var settings = applicationSettings.additionalConfigs.find{it.configId == configId}
                    if (settings == null)
                        settings = applicationSettings.defaultConfig

                    _uiState.update { currentState ->
                        currentState.copy(
                            configId = settings.configId,
                            configName = settings.configName,
                            activationDuration = settings.activationDuration,
                            showGestureVisualization = settings.showGestureVisualization,
                            visualSize = settings.visualSize,
                            cursorSize = settings.cursorSize,
                            cursorAccelerationStart = settings.cursorAccelerationStart,
                            cursorAccelerationDuration = settings.cursorAccelerationDuration,
                            cursorActivationKey = settings.cursorActivationKey,
                            touchPadMainInputType = settings.touchPadMainInputType,
                            touchPadLeftInputType = settings.touchPadLeftInputType,
                            touchPadRightInputType = settings.touchPadRightInputType,
                            backScreenInputType = settings.backScreenInputType,
                            touchpadDisableTopRow = settings.touchpadDisableTopRow,
                            touchpadSplitInput = settings.touchpadSplitInput,
                            touchpadSplitPosition = settings.touchpadSplitPosition,
                            touchpadSplitRightInput = settings.touchpadSplitRightInput,
                            touchpadSplitRightPosition = settings.touchpadSplitRightPosition,
                            mouseTapToClick = settings.mouseTapToClick,
                            mouseDoubleTapToHold = settings.mouseDoubleTapToHold,
                            mouseTwoFingerToHold = settings.mouseTwoFingerToHold,
                            mouseTapMaxDuration = settings.mouseTapMaxDuration,
                            softwareMouseSensitivity = settings.softwareMouseSensitivity,
                            softwareMouseExponential = settings.softwareMouseExponential,
                            twoFingerSensitivity = settings.twoFingerSensitivity,
                            func1ButtonMap = settings.func1ButtonMap,
                            func2ButtonMap = settings.func2ButtonMap,
                            allowPassthrough = settings.allowPassthrough,
                            hideOnKeyboardOpen = settings.hideOnKeyboardOpen,
                            hideOnLauncherOpen = settings.hideOnLauncherOpen,
                            hideOnLockScreen = settings.hideOnLockScreen,
                            roundedCursorCorners = settings.roundedCursorCorners,
                            usePhysicalSize = settings.usePhysicalSize,
                            standardCursorHex = settings.standardCursorHex,
                            standardCursorMatchBorder = settings.standardCursorMatchBorder,
                            cursorImagePath = settings.cursorImagePath,
                            clickableImagePath = settings.clickableImagePath,
                            scrollToggleImagePath = settings.scrollToggleImagePath,
                            useCustomCursorIcon = settings.useCustomCursorIcon,
                            cursorImageAlignment = settings.cursorImageAlignment,
                            clickableImageAlignment = settings.clickableImageAlignment,
                            scrollToggleImageAlignment = settings.scrollToggleImageAlignment,
                            autoEnableApps = settings.autoEnableApps,
                            autoHideApps = settings.autoHideApps,
                            clickableApps = settings.clickableApps,
                            showNotification = settings.showNotification,
                            autoEnableListType = settings.autoEnableListType,
                            autoDisableActivity = settings.autoDisableActivity,
                            autoEnableIfOnOnly = settings.autoEnableIfOnOnly,
                            applicationListType = settings.applicationListType,
                            clickableListType = settings.clickableListType,
                            checkClickable = settings.checkClickable,
                            disableTouchscreen = settings.disableTouchscreen,

                            scrollSettings = settings.scrollSettings,

                            defaultConfigName = applicationSettings.defaultConfig.configName,
                            alwaysRemapFuncKeys = applicationSettings.alwaysRemapFuncKeys,
                            alwaysRemapFuncKeysCompat = applicationSettings.alwaysRemapFuncKeysCompat,
                            configList = applicationSettings.additionalConfigs.associate { it.configId to it.configName },
                        )
                    }
                }
            } catch (error: Exception) {
                Logger.e("Failed to load settings", error)
                _uiState.update {
                    it.copy(
                        showError = true,
                        errorMessage = "Failed to load settings"
                    )
                }
            }
        }
    }

    private fun updateSettings(settingsUpdater: (UsageConfig) -> UsageConfig) {
        viewModelScope.launch {
            val currentSettings = createConfigSettingsFromUiState()
            val updatedSettings = settingsUpdater(currentSettings)

            val result = settingsRepository.validateAndUpdateSettings(currentSettings.configId, updatedSettings)

            if (result.isValid) {
                _validationErrors.value = emptyList()
                _uiState.update { it.copy(showInvalidSettingError = false) }
            } else {
                _validationErrors.value = result.errors
                _uiState.update { it.copy(showInvalidSettingError = true) }
            }
        }
    }

    private fun updateGlobalSettings(settingsUpdater: (ApplicationSettings) -> ApplicationSettings) {
        viewModelScope.launch {
            val currentSettings = settingsRepository.getSettings().first()
            val updatedSettings = settingsUpdater(currentSettings)

            val result = settingsRepository.validateAndUpdateSettings(updatedSettings)

            if (result.isValid) {
                _validationErrors.value = emptyList()
                _uiState.update { it.copy(showInvalidSettingError = false) }
            } else {
                _validationErrors.value = result.errors
                _uiState.update { it.copy(showInvalidSettingError = true) }
            }
        }
    }

    private fun createConfigSettingsFromUiState(): UsageConfig {
        return UsageConfig(
            configId = _uiState.value.configId,
            configName = _uiState.value.configName,
            activationDuration = _uiState.value.activationDuration,
            showGestureVisualization = _uiState.value.showGestureVisualization,
            visualSize = _uiState.value.visualSize,
            cursorSize = _uiState.value.cursorSize,
            cursorAccelerationStart = _uiState.value.cursorAccelerationStart,
            cursorAccelerationDuration = _uiState.value.cursorAccelerationDuration,
            cursorActivationKey = _uiState.value.cursorActivationKey,
            touchPadMainInputType = _uiState.value.touchPadMainInputType,
            touchPadLeftInputType = _uiState.value.touchPadLeftInputType,
            touchPadRightInputType = _uiState.value.touchPadRightInputType,
            backScreenInputType = _uiState.value.backScreenInputType,
            touchpadDisableTopRow = _uiState.value.touchpadDisableTopRow,
            touchpadSplitInput = _uiState.value.touchpadSplitInput,
            touchpadSplitPosition = _uiState.value.touchpadSplitPosition,
            touchpadSplitRightInput = _uiState.value.touchpadSplitRightInput,
            touchpadSplitRightPosition = _uiState.value.touchpadSplitRightPosition,
            mouseTapToClick = _uiState.value.mouseTapToClick,
            mouseDoubleTapToHold = _uiState.value.mouseDoubleTapToHold,
            mouseTwoFingerToHold = _uiState.value.mouseTwoFingerToHold,
            mouseTapMaxDuration = _uiState.value.mouseTapMaxDuration,
            softwareMouseSensitivity = _uiState.value.softwareMouseSensitivity,
            softwareMouseExponential = _uiState.value.softwareMouseExponential,
            twoFingerSensitivity = _uiState.value.twoFingerSensitivity,
            func1ButtonMap = _uiState.value.func1ButtonMap,
            func2ButtonMap = _uiState.value.func2ButtonMap,
            allowPassthrough = _uiState.value.allowPassthrough,
            hideOnKeyboardOpen = _uiState.value.hideOnKeyboardOpen,
            hideOnLauncherOpen = _uiState.value.hideOnLauncherOpen,
            hideOnLockScreen = _uiState.value.hideOnLockScreen,
            roundedCursorCorners = _uiState.value.roundedCursorCorners,
            usePhysicalSize = _uiState.value.usePhysicalSize,
            standardCursorHex = _uiState.value.standardCursorHex,
            standardCursorMatchBorder = _uiState.value.standardCursorMatchBorder,
            cursorImagePath = _uiState.value.cursorImagePath,
            clickableImagePath = _uiState.value.clickableImagePath,
            scrollToggleImagePath = _uiState.value.scrollToggleImagePath,
            useCustomCursorIcon = _uiState.value.useCustomCursorIcon,
            cursorImageAlignment = _uiState.value.cursorImageAlignment,
            clickableImageAlignment = _uiState.value.clickableImageAlignment,
            scrollToggleImageAlignment = _uiState.value.scrollToggleImageAlignment,
            autoEnableApps = _uiState.value.autoEnableApps,
            autoHideApps = _uiState.value.autoHideApps,
            clickableApps = _uiState.value.clickableApps,
            showNotification = _uiState.value.showNotification,
            autoEnableListType = _uiState.value.autoEnableListType,
            autoDisableActivity = _uiState.value.autoDisableActivity,
            autoEnableIfOnOnly = _uiState.value.autoEnableIfOnOnly,
            applicationListType = _uiState.value.applicationListType,
            clickableListType = _uiState.value.clickableListType,
            checkClickable = _uiState.value.checkClickable,
            disableTouchscreen = _uiState.value.disableTouchscreen,

            scrollSettings = _uiState.value.scrollSettings,
        )
    }

    fun <T> updatePreference(value: T, updater: (UsageConfig, T) -> UsageConfig) {
        updateSettings { settings -> updater(settings, value) }
    }

    fun <T> updateGlobalPreference(value: T, updater: (ApplicationSettings, T) -> ApplicationSettings) {
        updateGlobalSettings { settings -> updater(settings, value) }
    }

    fun updateAccessibilityServiceStatus(isEnabled: Boolean) {
        _uiState.update { it.copy(isAccessibilityServiceEnabled = isEnabled) }
    }

    fun updateCursorActivationKey(keyCode: Int) {
        updateSettings { it.copy(cursorActivationKey = keyCode) }
    }

    fun requestHideAllOverlays() {
        val serviceInstance = AppAccessibilityService.getInstance()
        serviceInstance?.forceHideAllOverlays(false)
    }

    fun updateAllowPassthrough(allow: Boolean) {
        updateSettings { it.copy(allowPassthrough = allow) }
    }

    fun updateDisableTouchscreen(disable: Boolean) {
        updateSettings { it.copy(disableTouchscreen = disable) }
    }

    fun addConfig(configId: String) {
        updateGlobalSettings { settings ->
            Logger.d("NEW CONFIG")
            val newConfig = UsageConfig(
                configId = configId,
                configName = "Config #${settings.additionalConfigs.size + 2}"
            )

            Logger.d(newConfig.toString())
            settings.copy(
                additionalConfigs = settings.additionalConfigs + newConfig
            )
        }
    }

    fun deleteConfig(configId: String) {
        updateGlobalSettings { settings ->
            settings.copy(
                additionalConfigs = settings.additionalConfigs.filter { it.configId != configId }
            )
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val configId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsState::class.java)) {
                return SettingsState(settingsRepository, configId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class SettingsUiState(
    val isAccessibilityServiceEnabled: Boolean = false,
    val showInvalidSettingError: Boolean = false,
    val showError: Boolean = false,
    val errorMessage: String = "",
    val configList: Map<String,String> = HashMap(),
    val defaultConfigName: String = Defaults.Settings.DEFAULT_CONFIG_NAME,
    val alwaysRemapFuncKeys: Boolean = Defaults.Settings.ALWAYS_REMAP_FUNC_KEYS,
    val alwaysRemapFuncKeysCompat: Boolean = Defaults.Settings.ALWAYS_REMAP_FUNC_KEYS_COMPAT,

    val configId: String = Defaults.Settings.DEFAULT_CONFIG_ID,
    val configName: String = Defaults.Settings.DEFAULT_CONFIG_NAME,
    val activationDuration: Long = Defaults.Settings.ACTIVATION_DURATION,
    val showGestureVisualization: Boolean = Defaults.Settings.SHOW_GESTURE_VISUAL,
    val visualSize: Int = Defaults.Settings.VISUAL_SIZE,
    val cursorSize: Int = Defaults.Settings.CURSOR_SIZE,
    val cursorAccelerationStart: Long = Defaults.Settings.CURSOR_ACCELERATION_START,
    val cursorAccelerationDuration: Long = Defaults.Settings.CURSOR_ACCELERATION_DURATION,
    val touchPadMainInputType: InputType = Defaults.Settings.TOUCHPAD_MAIN_INPUT,
    val touchPadLeftInputType: InputType = Defaults.Settings.TOUCHPAD_LEFT_INPUT,
    val touchPadRightInputType: InputType = Defaults.Settings.TOUCHPAD_RIGHT_INPUT,
    val backScreenInputType: InputType = Defaults.Settings.BACK_SCREEN_INPUT,
    val touchpadDisableTopRow: Boolean = Defaults.Settings.TOUCHPAD_DISABLE_TOP_ROW,
    val touchpadSplitInput: Boolean = Defaults.Settings.TOUCHPAD_SPLIT_INPUT,
    val touchpadSplitPosition: Int = Defaults.Settings.TOUCHPAD_SPLIT_POSITION,
    val touchpadSplitRightInput: Boolean = Defaults.Settings.TOUCHPAD_SPLIT_RIGHT_INPUT,
    val touchpadSplitRightPosition: Int = Defaults.Settings.TOUCHPAD_SPLIT_RIGHT_POSITION,
    val mouseTapToClick: Boolean = Defaults.Settings.MOUSE_TAP_TO_CLICK,
    val mouseDoubleTapToHold: Boolean = Defaults.Settings.MOUSE_DOUBLE_TAP_HOLD,
    val mouseTwoFingerToHold: Boolean = Defaults.Settings.MOUSE_TWO_FINGER_HOLD,
    val mouseTapMaxDuration: Int = Defaults.Settings.MOUSE_TAP_MAX_DURATION,
    val softwareMouseSensitivity: Int = Defaults.Settings.SOFTWARE_MOUSE_SENSITIVITY,
    val softwareMouseExponential: Boolean = Defaults.Settings.SOFTWARE_MOUSE_EXPONENTIAL,
    val twoFingerSensitivity: Int = Defaults.Settings.TWO_FINGER_SENSITIVITY,
    val func1ButtonMap: FuncButtonMap = Defaults.Settings.FUNC_1_BUTTON_MAP,
    val func2ButtonMap: FuncButtonMap = Defaults.Settings.FUNC_2_BUTTON_MAP,
    val cursorActivationKey: Int = Defaults.Settings.CURSOR_ACTIVATION_KEY,
    val allowPassthrough: Boolean = Defaults.Settings.ALLOW_PASSTHROUGH,
    val hideOnKeyboardOpen: Boolean = Defaults.Settings.HIDE_ON_KEYBOARD_OPEN,
    val hideOnLauncherOpen: Boolean = Defaults.Settings.HIDE_ON_LAUNCHER_OPEN,
    val hideOnLockScreen: Boolean = Defaults.Settings.HIDE_ON_LOCK_SCREEN,
    val roundedCursorCorners: Boolean = Defaults.Settings.ROUNDED_CURSOR_CORNERS,
    val usePhysicalSize: Boolean = Defaults.Settings.USE_PHYSICAL_SIZE,
    val standardCursorHex: String = Defaults.Settings.STANDARD_CURSOR_HEX,
    val standardCursorMatchBorder: Boolean = Defaults.Settings.STANDARD_CURSOR_MATCH_BORDER,
    val cursorImagePath: String? = Defaults.Settings.CURSOR_IMAGE_PATH,
    val clickableImagePath: String? = Defaults.Settings.CLICKABLE_IMAGE_PATH,
    val scrollToggleImagePath: String? = Defaults.Settings.SCROLL_TOGGLE_IMAGE_PATH,
    val useCustomCursorIcon: Boolean = Defaults.Settings.USE_CUSTOM_CURSOR_ICON,
    val cursorImageAlignment: IconAlignment = Defaults.Settings.CURSOR_IMAGE_ALIGNMENT,
    val clickableImageAlignment: IconAlignment = Defaults.Settings.CLICKABLE_IMAGE_ALIGNMENT,
    val scrollToggleImageAlignment: IconAlignment = Defaults.Settings.SCROLL_TOGGLE_IMAGE_ALIGNMENT,
    val autoEnableApps: Set<String> = Defaults.Settings.AUTO_ENABLE_APPS,
    val autoHideApps: Set<String> = Defaults.Settings.AUTO_HIDE_APPS,
    val clickableApps: Set<String> = Defaults.Settings.CLICKABLE_APPS,
    val showNotification: Boolean = Defaults.Settings.SHOW_NOTIFICATION,
    val autoEnableListType: AppListType = Defaults.Settings.AUTO_ENABLE_LIST_TYPE,
    val autoEnableIfOnOnly: Boolean = Defaults.Settings.AUTO_ENABLE_IF_ON_ONLY,
    val autoDisableActivity: String = Defaults.Settings.AUTO_DISABLE_ACTIVITY,
    val applicationListType: AppListType = Defaults.Settings.APPLICATION_LIST_TYPE,
    val clickableListType: AppListType = Defaults.Settings.CLICKABLE_LIST_TYPE,
    val checkClickable: Boolean = Defaults.Settings.CHECK_CLICKABLE,
    val disableTouchscreen: Boolean = Defaults.Settings.DISABLE_TOUCHSCREEN,

    val scrollSettings: List<ScrollConfig> = Defaults.Settings.SCROLL_SETTINGS,
)