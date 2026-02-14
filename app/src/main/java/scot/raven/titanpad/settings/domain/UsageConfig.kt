package scot.raven.titanpad.settings.domain

import kotlinx.serialization.Serializable
import scot.raven.titanpad.BuildConfig
import scot.raven.titanpad.core.constants.CursorConstants
import scot.raven.titanpad.cursor.domain.FuncButtonMap
import scot.raven.titanpad.cursor.domain.IconAlignment
import scot.raven.titanpad.cursor.domain.InputType

/**
 * Represents default user preferences.
 */
@Serializable
data class UsageConfig(
    val className: String = "UsageConfig",
    val versionCode: Int = BuildConfig.VERSION_CODE,
    val configId: String = Defaults.Settings.DEFAULT_CONFIG_ID,
    val configName: String = Defaults.Settings.DEFAULT_CONFIG_NAME,
    val activationDuration: Long = Defaults.Settings.ACTIVATION_DURATION,
    val showGestureVisualization: Boolean = Defaults.Settings.SHOW_GESTURE_VISUAL,
    val visualSize: Int = Defaults.Settings.VISUAL_SIZE,
    val cursorSize: Int = Defaults.Settings.CURSOR_SIZE,
    val cursorAccelerationStart: Long = Defaults.Settings.CURSOR_ACCELERATION_START,
    val cursorAccelerationDuration: Long = Defaults.Settings.CURSOR_ACCELERATION_DURATION,
    val cursorActivationKey: Int = Defaults.Settings.CURSOR_ACTIVATION_KEY,
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
    val wheelSettings: List<WheelConfig> = Defaults.Settings.WHEEL_SETTINGS,
) {
    companion object {
        val DEFAULT = UsageConfig()

        const val SCROLL_SETTING_COUNT = 4
        const val KEY_NONE = -999

        fun randomId() : String {
            val allowedChars = ('a'..'z')
            return (1..10).map {allowedChars.random()}.joinToString("")
        }
    }

    fun validate(): ApplicationSettings.ValidationResult {
        return ApplicationSettings.ValidationResult(true, ArrayList())
    }

    fun sanitized(): UsageConfig {
        return copy(
            cursorSize = cursorSize.coerceIn(CursorConstants.MIN_SIZE, CursorConstants.MAX_SIZE),
            cursorActivationKey = cursorActivationKey,
        )
    }

    fun withoutAppConfig(existingConfig: UsageConfig? = null): UsageConfig {
        if (existingConfig == null) {
            return copy(
                applicationListType = Defaults.Settings.APPLICATION_LIST_TYPE,
                autoEnableListType = Defaults.Settings.AUTO_ENABLE_LIST_TYPE,
                clickableListType = Defaults.Settings.CLICKABLE_LIST_TYPE,
                autoEnableApps = Defaults.Settings.AUTO_ENABLE_APPS,
                autoHideApps = Defaults.Settings.AUTO_HIDE_APPS,
                clickableApps = Defaults.Settings.CLICKABLE_APPS,
                autoEnableIfOnOnly = Defaults.Settings.AUTO_ENABLE_IF_ON_ONLY,
                autoDisableActivity = Defaults.Settings.AUTO_DISABLE_ACTIVITY,
                cursorActivationKey = Defaults.Settings.CURSOR_ACTIVATION_KEY,
                activationDuration = Defaults.Settings.ACTIVATION_DURATION
            )
        } else {
            return copy(
                applicationListType = existingConfig.applicationListType,
                autoEnableListType = existingConfig.autoEnableListType,
                clickableListType = existingConfig.clickableListType,
                autoEnableApps = existingConfig.autoEnableApps,
                autoHideApps = existingConfig.autoHideApps,
                clickableApps = existingConfig.clickableApps,
                autoEnableIfOnOnly = existingConfig.autoEnableIfOnOnly,
                autoDisableActivity = existingConfig.autoDisableActivity,
                cursorActivationKey = existingConfig.cursorActivationKey,
                activationDuration = existingConfig.activationDuration
            )
        }
    }
}

@Serializable
data class ScrollConfig(
    val topCropRegion: Int = 25,
    val bottomCropRegion: Int = 25,
    val leftCropRegion: Int = 25,
    val rightCropRegion: Int = 25,
    val touchSensitivity: Int = 5,
    val scrollOnlyVertically: Boolean = false,
)

@Serializable
data class WheelConfig(
    val speed: Int = 5,
    val touchSensitivity: Int = 5,
    val scrollOnlyVertically: Boolean = false,
    val momentum: Boolean = false
)