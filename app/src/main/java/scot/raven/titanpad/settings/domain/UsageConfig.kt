package scot.raven.titanpad.settings.domain

import scot.raven.titanpad.core.constants.ApplicationConstants
import scot.raven.titanpad.core.constants.CursorConstants
import scot.raven.titanpad.cursor.domain.IconAlignment

/**
 * Represents default user preferences.
 */
data class UsageConfig(
    val configId: String = Defaults.Settings.DEFAULT_CONFIG_ID,
    val configName: String = Defaults.Settings.DEFAULT_CONFIG_NAME,
    val activationDuration: Long = Defaults.Settings.ACTIVATION_DURATION,
    val showGestureVisualization: Boolean = Defaults.Settings.SHOW_GESTURE_VISUAL,
    val visualSize: Int = Defaults.Settings.VISUAL_SIZE,
    val cursorSize: Int = Defaults.Settings.CURSOR_SIZE,
    val cursorAccelerationStart: Long = Defaults.Settings.CURSOR_ACCELERATION_START,
    val cursorAccelerationDuration: Long = Defaults.Settings.CURSOR_ACCELERATION_DURATION,
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
    val autoHideApps: Set<String> = Defaults.Settings.AUTO_HIDE_APPS,
    val clickableApps: Set<String> = Defaults.Settings.CLICKABLE_APPS,
    val showNotification: Boolean = Defaults.Settings.SHOW_NOTIFICATION,
    val applicationListType: AppListType = Defaults.Settings.APPLICATION_LIST_TYPE,
    val clickableListType: AppListType = Defaults.Settings.CLICKABLE_LIST_TYPE,
    val checkClickable: Boolean = Defaults.Settings.CHECK_CLICKABLE,
    val disableTouchscreen: Boolean = Defaults.Settings.DISABLE_TOUCHSCREEN
) {
    companion object {
        val DEFAULT = UsageConfig()
        const val KEY_NONE = ApplicationConstants.OVERLAY_DISABLED
        val RESTRICTED_KEYS = emptySet<Int>()

        fun randomId() : String {
            val allowedChars = ('a'..'z')
            return (1..10).map {allowedChars.random()}.joinToString("")
        }
    }

    private fun isValidRemappableKey(keyCode: Int): Boolean {
        if (keyCode == KEY_NONE) return true
        return keyCode !in RESTRICTED_KEYS
    }

    fun validate(): ValidationResult {
        val errors =
            buildList {
                if (!isValidRemappableKey(cursorActivationKey)) {
                    add("Invalid cursor activation key: $cursorActivationKey")
                }
            }

        return ValidationResult(errors.isEmpty(), errors)
    }

    fun sanitized(): UsageConfig {
        return copy(
            cursorSize = cursorSize.coerceIn(CursorConstants.MIN_SIZE, CursorConstants.MAX_SIZE),
            cursorActivationKey = if (isValidRemappableKey(cursorActivationKey)) cursorActivationKey else KEY_NONE,
        )
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
    )
}
