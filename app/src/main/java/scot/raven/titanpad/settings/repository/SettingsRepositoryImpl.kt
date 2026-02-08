package scot.raven.titanpad.settings.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.settings.domain.OverlaySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Persists user settings.
 */
class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    companion object {
        private val ACTIVATION_DURATION = longPreferencesKey("activation_duration")
        private val SHOW_GESTURE_VISUAL = booleanPreferencesKey("show_gesture_visual")
        private val VISUAL_SIZE = intPreferencesKey("visual_size")
        private val CURSOR_SIZE = intPreferencesKey("cursor_size")
        private val CURSOR_ACCELERATION_START = longPreferencesKey("cursor_acceleration_start")
        private val CURSOR_ACCELERATION_DURATION = longPreferencesKey("cursor_acceleration_duration")
        private val CURSOR_ACTIVATION_KEY = intPreferencesKey("cursor_activation_key")
        private val ALLOW_PASSTHROUGH = booleanPreferencesKey("allow_passthrough")
        private val HIDE_ON_KEYBOARD_OPEN = booleanPreferencesKey("hide_on_keyboard_open")
        private val HIDE_ON_LAUNCHER_OPEN = booleanPreferencesKey("hide_on_launcher_open")
        private val HIDE_ON_LOCK_SCREEN = booleanPreferencesKey("hide_on_lock_screen")
        private val ROUNDED_CURSOR_CORNERS = booleanPreferencesKey("rounded_cursor_corners")
        private val USE_PHYSICAL_SIZE = booleanPreferencesKey("use_physical_size")
        private val STANDARD_CURSOR_HEX = stringPreferencesKey("standard_cursor_hex")
        private val STANDARD_CURSOR_MATCH_BORDER = booleanPreferencesKey("standard_cursor_match_border")
        private val CURSOR_IMAGE_PATH = stringPreferencesKey("cursor_image_path")
        private val CLICKABLE_IMAGE_PATH = stringPreferencesKey("clickable_image_path")
        private val SCROLL_TOGGLE_IMAGE_PATH = stringPreferencesKey("scroll_toggle_image_path")
        private val USE_CUSTOM_CURSOR_ICON = booleanPreferencesKey("use_custom_cursor_icon")
        private val CURSOR_IMAGE_ALIGNMENT = stringPreferencesKey("cursor_image_alignment")
        private val CLICKABLE_IMAGE_ALIGNMENT = stringPreferencesKey("clickable_image_alignment")
        private val SCROLL_TOGGLE_IMAGE_ALIGNMENT = stringPreferencesKey("scroll_toggle_image_alignment")
        private val AUTO_HIDE_APPS = stringPreferencesKey("auto_hide_apps")
        private val CLICKABLE_APPS = stringPreferencesKey("clickable_apps")
        private val SHOW_NOTIFICATION = booleanPreferencesKey("show_notification")
        private val APPLICATION_LIST_TYPE = stringPreferencesKey("application_list_type")
        private val CLICKABLE_LIST_TYPE = stringPreferencesKey("clickable_list_type")
        private val CHECK_CLICKABLE = booleanPreferencesKey("check_clickable")
        private val DISABLE_TOUCHSCREEN = booleanPreferencesKey("disable_touchscreen")
    }

    private inline fun <reified T : Enum<T>> getEnumPreference(
        preferences: Preferences,
        preferencesKey: Preferences.Key<String>,
        defaultValue: T,
        logTag: String
    ): T {
        val valueStr = preferences[preferencesKey]
        return if (valueStr != null) {
            try {
                enumValueOf<T>(valueStr)
            } catch (e: Exception) {
                Logger.w("Invalid $logTag value: $valueStr", e)
                defaultValue
            }
        } else {
            defaultValue
        }
    }

    override fun getSettings(): Flow<OverlaySettings> {
        return dataStore.data
            .catch { exception ->
                Logger.e("Error reading settings: ${exception.message}", exception)
                emit(emptyPreferences())
            }
            .map { preferences ->
                val cursorImageAlignment = getEnumPreference(
                    preferences,
                    CURSOR_IMAGE_ALIGNMENT,
                    OverlaySettings.DEFAULT.cursorImageAlignment,
                    "cursor image alignment"
                )

                val clickableImageAlignment = getEnumPreference(
                    preferences,
                    CLICKABLE_IMAGE_ALIGNMENT,
                    OverlaySettings.DEFAULT.clickableImageAlignment,
                    "clickable image alignment"
                )

                val scrollToggleImageAlignment = getEnumPreference(
                    preferences,
                    SCROLL_TOGGLE_IMAGE_ALIGNMENT,
                    OverlaySettings.DEFAULT.scrollToggleImageAlignment,
                    "scroll toggle image alignment"
                )

                val autoHideAppsString = preferences[AUTO_HIDE_APPS] ?: ""
                val autoHideApps = if (autoHideAppsString.isBlank()) {
                    emptySet()
                } else {
                    autoHideAppsString.split(",").toSet()
                }

                val applicationListType = getEnumPreference(
                    preferences,
                    APPLICATION_LIST_TYPE,
                    OverlaySettings.DEFAULT.applicationListType,
                    "application list type"
                )

                val clickableAppsString = preferences[CLICKABLE_APPS] ?: ""
                val clickableApps = if (clickableAppsString.isBlank()) {
                    emptySet()
                } else {
                    clickableAppsString.split(",").toSet()
                }

                val clickableListType = getEnumPreference(
                    preferences,
                    CLICKABLE_LIST_TYPE,
                    OverlaySettings.DEFAULT.clickableListType,
                    "clickable list type"
                )

                val settings = OverlaySettings(
                    activationDuration = preferences[ACTIVATION_DURATION]
                        ?: OverlaySettings.DEFAULT.activationDuration,
                    showGestureVisualization = preferences[SHOW_GESTURE_VISUAL]
                        ?: OverlaySettings.DEFAULT.showGestureVisualization,
                    visualSize = preferences[VISUAL_SIZE] ?: OverlaySettings.DEFAULT.visualSize,
                    cursorSize = preferences[CURSOR_SIZE] ?: OverlaySettings.DEFAULT.cursorSize,
                    cursorAccelerationStart = preferences[CURSOR_ACCELERATION_START]
                        ?: OverlaySettings.DEFAULT.cursorAccelerationStart,
                    cursorAccelerationDuration = preferences[CURSOR_ACCELERATION_DURATION]
                        ?: OverlaySettings.DEFAULT.cursorAccelerationDuration,
                    cursorActivationKey = preferences[CURSOR_ACTIVATION_KEY]
                        ?: OverlaySettings.DEFAULT.cursorActivationKey,
                    allowPassthrough = preferences[ALLOW_PASSTHROUGH]
                        ?: OverlaySettings.DEFAULT.allowPassthrough,
                    hideOnKeyboardOpen = preferences[HIDE_ON_KEYBOARD_OPEN]
                        ?: OverlaySettings.DEFAULT.hideOnKeyboardOpen,
                    hideOnLauncherOpen = preferences[HIDE_ON_LAUNCHER_OPEN]
                        ?: OverlaySettings.DEFAULT.hideOnLauncherOpen,
                    hideOnLockScreen = preferences[HIDE_ON_LOCK_SCREEN]
                        ?: OverlaySettings.DEFAULT.hideOnLockScreen,
                    roundedCursorCorners = preferences[ROUNDED_CURSOR_CORNERS]
                        ?: OverlaySettings.DEFAULT.roundedCursorCorners,
                    usePhysicalSize = preferences[USE_PHYSICAL_SIZE]
                        ?: OverlaySettings.DEFAULT.usePhysicalSize,
                    standardCursorHex = preferences[STANDARD_CURSOR_HEX]
                        ?: OverlaySettings.DEFAULT.standardCursorHex,
                    standardCursorMatchBorder = preferences[STANDARD_CURSOR_MATCH_BORDER]
                        ?: OverlaySettings.DEFAULT.standardCursorMatchBorder,
                    cursorImagePath = preferences[CURSOR_IMAGE_PATH]
                        ?: OverlaySettings.DEFAULT.cursorImagePath,
                    clickableImagePath = preferences[CLICKABLE_IMAGE_PATH]
                        ?: OverlaySettings.DEFAULT.clickableImagePath,
                    scrollToggleImagePath = preferences[SCROLL_TOGGLE_IMAGE_PATH]
                        ?: OverlaySettings.DEFAULT.scrollToggleImagePath,
                    useCustomCursorIcon = preferences[USE_CUSTOM_CURSOR_ICON]
                        ?: OverlaySettings.DEFAULT.useCustomCursorIcon,
                    cursorImageAlignment = cursorImageAlignment,
                    clickableImageAlignment = clickableImageAlignment,
                    scrollToggleImageAlignment = scrollToggleImageAlignment,
                    autoHideApps = autoHideApps,
                    clickableApps = clickableApps,
                    showNotification = preferences[SHOW_NOTIFICATION]
                        ?: OverlaySettings.DEFAULT.showNotification,
                    applicationListType = applicationListType,
                    clickableListType = clickableListType,
                    checkClickable = preferences[CHECK_CLICKABLE]
                        ?: OverlaySettings.DEFAULT.checkClickable,
                    disableTouchscreen = preferences[DISABLE_TOUCHSCREEN] ?: OverlaySettings.DEFAULT.disableTouchscreen
                )

                settings
            }
    }

    override suspend fun updateSettings(settings: OverlaySettings) {
        try {
            dataStore.edit { preferences ->
                preferences[ACTIVATION_DURATION] = settings.activationDuration
                preferences[SHOW_GESTURE_VISUAL] = settings.showGestureVisualization
                preferences[VISUAL_SIZE] = settings.visualSize
                preferences[CURSOR_SIZE] = settings.cursorSize
                preferences[CURSOR_ACCELERATION_START] = settings.cursorAccelerationStart
                preferences[CURSOR_ACCELERATION_DURATION] = settings.cursorAccelerationDuration
                preferences[CURSOR_ACTIVATION_KEY] = settings.cursorActivationKey
                preferences[ALLOW_PASSTHROUGH] = settings.allowPassthrough
                preferences[HIDE_ON_KEYBOARD_OPEN] = settings.hideOnKeyboardOpen
                preferences[HIDE_ON_LAUNCHER_OPEN] = settings.hideOnLauncherOpen
                preferences[HIDE_ON_LOCK_SCREEN] = settings.hideOnLockScreen
                preferences[ROUNDED_CURSOR_CORNERS] = settings.roundedCursorCorners
                preferences[USE_PHYSICAL_SIZE] = settings.usePhysicalSize
                preferences[STANDARD_CURSOR_HEX] = settings.standardCursorHex
                preferences[STANDARD_CURSOR_MATCH_BORDER] = settings.standardCursorMatchBorder
                preferences[USE_CUSTOM_CURSOR_ICON] = settings.useCustomCursorIcon
                preferences[CURSOR_IMAGE_ALIGNMENT] = settings.cursorImageAlignment.name
                preferences[CLICKABLE_IMAGE_ALIGNMENT] = settings.clickableImageAlignment.name
                preferences[SCROLL_TOGGLE_IMAGE_ALIGNMENT] = settings.scrollToggleImageAlignment.name
                preferences[AUTO_HIDE_APPS] = settings.autoHideApps.joinToString(",")
                preferences[CLICKABLE_APPS] = settings.clickableApps.joinToString(",")
                preferences[SHOW_NOTIFICATION] = settings.showNotification
                preferences[APPLICATION_LIST_TYPE] = settings.applicationListType.name
                preferences[CLICKABLE_LIST_TYPE] = settings.clickableListType.name
                preferences[CHECK_CLICKABLE] = settings.checkClickable
                preferences[DISABLE_TOUCHSCREEN] = settings.disableTouchscreen

                if (settings.cursorImagePath != null) {
                    preferences[CURSOR_IMAGE_PATH] = settings.cursorImagePath
                } else {
                    preferences.remove(CURSOR_IMAGE_PATH)
                }

                if (settings.clickableImagePath != null) {
                    preferences[CLICKABLE_IMAGE_PATH] = settings.clickableImagePath
                } else {
                    preferences.remove(CLICKABLE_IMAGE_PATH)
                }

                if (settings.scrollToggleImagePath != null) {
                    preferences[SCROLL_TOGGLE_IMAGE_PATH] = settings.scrollToggleImagePath
                } else {
                    preferences.remove(SCROLL_TOGGLE_IMAGE_PATH)
                }
            }
        } catch (e: Exception) {
            Logger.e("Error updating settings", e)
        }
    }

    override suspend fun validateAndUpdateSettings(settings: OverlaySettings): OverlaySettings.ValidationResult {
        try {
            val validationResult = settings.validate()

            val settingsToSave =
                if (validationResult.isValid) {
                    settings
                } else {
                    Logger.w("Saving sanitized settings due to validation errors: ${validationResult.errors}")
                    settings.sanitized()
                }

            updateSettings(settingsToSave)

            return validationResult
        } catch (e: Exception) {
            Logger.e("Error updating settings", e)
            return OverlaySettings.ValidationResult(
                isValid = false,
                errors = listOf("Error updating settings: ${e.message}"),
            )
        }
    }
}
