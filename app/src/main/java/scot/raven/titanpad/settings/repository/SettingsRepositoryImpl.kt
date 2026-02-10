package scot.raven.titanpad.settings.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.settings.domain.UsageConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import scot.raven.titanpad.settings.domain.ApplicationSettings
import kotlin.collections.map
import kotlin.collections.toSet

/**
 * Persists user settings.
 */
class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    companion object {
        private val ALWAYS_REMAP_FUNC_KEYS = booleanPreferencesKey("always_remap_func_keys")
        private val LAST_ACTIVE_SETTING = stringPreferencesKey("last_active_setting")
        private val ADDITIONAL_CONFIG_KEYS = stringSetPreferencesKey("additional_config_keys")
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

    override fun getSettings(): Flow<ApplicationSettings> {
        return dataStore.data
            .catch { exception ->
                Logger.e("Error reading settings: ${exception.message}", exception)
                emit(emptyPreferences())
            }
            .map { preferences ->
                val additionalConfigKeys = preferences[ADDITIONAL_CONFIG_KEYS] ?: HashSet()

                val settings = ApplicationSettings(
                    defaultConfig = usageConfigPreferenceLoader("default", preferences),
                    lastActiveSetting = preferences[LAST_ACTIVE_SETTING] ?: "default",
                    alwaysRemapFuncKeys = preferences[ALWAYS_REMAP_FUNC_KEYS] ?: ApplicationSettings.DEFAULT.alwaysRemapFuncKeys,
                    additionalConfigs = additionalConfigKeys.map { usageConfigPreferenceLoader(it, preferences) }
                )

                settings
            }
    }

    override suspend fun setActiveKey(configId: String) {
        try {
            dataStore.edit { preferences ->
                preferences[LAST_ACTIVE_SETTING] = configId
            }
        } catch (e: Exception) {
            Logger.e("Error updating settings", e)
        }
    }
    override suspend fun updateSettings(settings: ApplicationSettings) {
        try {
            dataStore.edit { preferences ->
                preferences[ALWAYS_REMAP_FUNC_KEYS] = settings.alwaysRemapFuncKeys
                preferences[ADDITIONAL_CONFIG_KEYS] = settings.additionalConfigs.map{it.configId}.toSet()

                usageConfigPreferenceWriter("default", preferences,settings.defaultConfig)
                settings.additionalConfigs.forEach { usageConfigPreferenceWriter(it.configId, preferences, it) }
            }
        } catch (e: Exception) {
            Logger.e("Error updating settings", e)
        }
    }

    override suspend fun updateSettings(configId: String, usageConfig: UsageConfig) {
        try {
            dataStore.edit { preferences ->
                usageConfigPreferenceWriter(configId, preferences, usageConfig)
            }
        } catch (e: Exception) {
            Logger.e("Error updating settings", e)
        }
    }

    override suspend fun validateAndUpdateSettings(configId: String, usageConfig: UsageConfig): ApplicationSettings.ValidationResult {
        try {
            val validationResult = usageConfig.validate()

            val settingsToSave =
                if (validationResult.isValid) {
                    usageConfig
                } else {
                    Logger.w("Saving sanitized settings due to validation errors: ${validationResult.errors}")
                    usageConfig.sanitized()
                }

            updateSettings(configId,settingsToSave)

            return validationResult
        } catch (e: Exception) {
            Logger.e("Error updating settings", e)
            return ApplicationSettings.ValidationResult(
                isValid = false,
                errors = listOf("Error updating settings: ${e.message}"),
            )
        }
    }

    override suspend fun validateAndUpdateSettings(settings: ApplicationSettings): ApplicationSettings.ValidationResult {
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
            return ApplicationSettings.ValidationResult(
                isValid = false,
                errors = listOf("Error updating settings: ${e.message}"),
            )
        }
    }

    fun usageConfigPreferenceLoader(configId: String, preferences: Preferences) : UsageConfig {
        val CONFIG_NAME = stringPreferencesKey("config_name__$configId")
        val ACTIVATION_DURATION = longPreferencesKey("activation_duration__$configId")
        val SHOW_GESTURE_VISUAL = booleanPreferencesKey("show_gesture_visual__$configId")
        val VISUAL_SIZE = intPreferencesKey("visual_size__$configId")
        val CURSOR_SIZE = intPreferencesKey("cursor_size__$configId")
        val CURSOR_ACCELERATION_START = longPreferencesKey("cursor_acceleration_start__$configId")
        val CURSOR_ACCELERATION_DURATION = longPreferencesKey("cursor_acceleration_duration__$configId")
        val CURSOR_ACTIVATION_KEY = intPreferencesKey("cursor_activation_key__$configId")
        val ALLOW_PASSTHROUGH = booleanPreferencesKey("allow_passthrough__$configId")
        val HIDE_ON_KEYBOARD_OPEN = booleanPreferencesKey("hide_on_keyboard_open__$configId")
        val HIDE_ON_LAUNCHER_OPEN = booleanPreferencesKey("hide_on_launcher_open__$configId")
        val HIDE_ON_LOCK_SCREEN = booleanPreferencesKey("hide_on_lock_screen__$configId")
        val ROUNDED_CURSOR_CORNERS = booleanPreferencesKey("rounded_cursor_corners__$configId")
        val USE_PHYSICAL_SIZE = booleanPreferencesKey("use_physical_size__$configId")
        val STANDARD_CURSOR_HEX = stringPreferencesKey("standard_cursor_hex__$configId")
        val STANDARD_CURSOR_MATCH_BORDER = booleanPreferencesKey("standard_cursor_match_border__$configId")
        val CURSOR_IMAGE_PATH = stringPreferencesKey("cursor_image_path__$configId")
        val CLICKABLE_IMAGE_PATH = stringPreferencesKey("clickable_image_path__$configId")
        val SCROLL_TOGGLE_IMAGE_PATH = stringPreferencesKey("scroll_toggle_image_path__$configId")
        val USE_CUSTOM_CURSOR_ICON = booleanPreferencesKey("use_custom_cursor_icon__$configId")
        val CURSOR_IMAGE_ALIGNMENT = stringPreferencesKey("cursor_image_alignment__$configId")
        val CLICKABLE_IMAGE_ALIGNMENT = stringPreferencesKey("clickable_image_alignment__$configId")
        val SCROLL_TOGGLE_IMAGE_ALIGNMENT = stringPreferencesKey("scroll_toggle_image_alignment__$configId")
        val AUTO_HIDE_APPS = stringPreferencesKey("auto_hide_apps__$configId")
        val CLICKABLE_APPS = stringPreferencesKey("clickable_apps__$configId")
        val SHOW_NOTIFICATION = booleanPreferencesKey("show_notification__$configId")
        val APPLICATION_LIST_TYPE = stringPreferencesKey("application_list_type__$configId")
        val CLICKABLE_LIST_TYPE = stringPreferencesKey("clickable_list_type__$configId")
        val CHECK_CLICKABLE = booleanPreferencesKey("check_clickable__$configId")
        val DISABLE_TOUCHSCREEN = booleanPreferencesKey("disable_touchscreen__$configId")

        val cursorImageAlignment = getEnumPreference(
            preferences,
            CURSOR_IMAGE_ALIGNMENT,
            UsageConfig.DEFAULT.cursorImageAlignment,
            "cursor image alignment"
        )

        val clickableImageAlignment = getEnumPreference(
            preferences,
            CLICKABLE_IMAGE_ALIGNMENT,
            UsageConfig.DEFAULT.clickableImageAlignment,
            "clickable image alignment"
        )

        val scrollToggleImageAlignment = getEnumPreference(
            preferences,
            SCROLL_TOGGLE_IMAGE_ALIGNMENT,
            UsageConfig.DEFAULT.scrollToggleImageAlignment,
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
            UsageConfig.DEFAULT.applicationListType,
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
            UsageConfig.DEFAULT.clickableListType,
            "clickable list type"
        )

        return UsageConfig(
            configId = configId,
            configName = preferences[CONFIG_NAME] ?: UsageConfig.DEFAULT.configName,
            activationDuration = preferences[ACTIVATION_DURATION]
                ?: UsageConfig.DEFAULT.activationDuration,
            showGestureVisualization = preferences[SHOW_GESTURE_VISUAL]
                ?: UsageConfig.DEFAULT.showGestureVisualization,
            visualSize = preferences[VISUAL_SIZE] ?: UsageConfig.DEFAULT.visualSize,
            cursorSize = preferences[CURSOR_SIZE] ?: UsageConfig.DEFAULT.cursorSize,
            cursorAccelerationStart = preferences[CURSOR_ACCELERATION_START]
                ?: UsageConfig.DEFAULT.cursorAccelerationStart,
            cursorAccelerationDuration = preferences[CURSOR_ACCELERATION_DURATION]
                ?: UsageConfig.DEFAULT.cursorAccelerationDuration,
            cursorActivationKey = preferences[CURSOR_ACTIVATION_KEY]
                ?: UsageConfig.DEFAULT.cursorActivationKey,
            allowPassthrough = preferences[ALLOW_PASSTHROUGH]
                ?: UsageConfig.DEFAULT.allowPassthrough,
            hideOnKeyboardOpen = preferences[HIDE_ON_KEYBOARD_OPEN]
                ?: UsageConfig.DEFAULT.hideOnKeyboardOpen,
            hideOnLauncherOpen = preferences[HIDE_ON_LAUNCHER_OPEN]
                ?: UsageConfig.DEFAULT.hideOnLauncherOpen,
            hideOnLockScreen = preferences[HIDE_ON_LOCK_SCREEN]
                ?: UsageConfig.DEFAULT.hideOnLockScreen,
            roundedCursorCorners = preferences[ROUNDED_CURSOR_CORNERS]
                ?: UsageConfig.DEFAULT.roundedCursorCorners,
            usePhysicalSize = preferences[USE_PHYSICAL_SIZE]
                ?: UsageConfig.DEFAULT.usePhysicalSize,
            standardCursorHex = preferences[STANDARD_CURSOR_HEX]
                ?: UsageConfig.DEFAULT.standardCursorHex,
            standardCursorMatchBorder = preferences[STANDARD_CURSOR_MATCH_BORDER]
                ?: UsageConfig.DEFAULT.standardCursorMatchBorder,
            cursorImagePath = preferences[CURSOR_IMAGE_PATH]
                ?: UsageConfig.DEFAULT.cursorImagePath,
            clickableImagePath = preferences[CLICKABLE_IMAGE_PATH]
                ?: UsageConfig.DEFAULT.clickableImagePath,
            scrollToggleImagePath = preferences[SCROLL_TOGGLE_IMAGE_PATH]
                ?: UsageConfig.DEFAULT.scrollToggleImagePath,
            useCustomCursorIcon = preferences[USE_CUSTOM_CURSOR_ICON]
                ?: UsageConfig.DEFAULT.useCustomCursorIcon,
            cursorImageAlignment = cursorImageAlignment,
            clickableImageAlignment = clickableImageAlignment,
            scrollToggleImageAlignment = scrollToggleImageAlignment,
            autoHideApps = autoHideApps,
            clickableApps = clickableApps,
            showNotification = preferences[SHOW_NOTIFICATION]
                ?: UsageConfig.DEFAULT.showNotification,
            applicationListType = applicationListType,
            clickableListType = clickableListType,
            checkClickable = preferences[CHECK_CLICKABLE]
                ?: UsageConfig.DEFAULT.checkClickable,
            disableTouchscreen = preferences[DISABLE_TOUCHSCREEN] ?: UsageConfig.DEFAULT.disableTouchscreen,
        )
    }

    fun usageConfigPreferenceWriter(configId: String, preferences: MutablePreferences, settings: UsageConfig) {
        val CONFIG_NAME = stringPreferencesKey("config_name__$configId")
        val ACTIVATION_DURATION = longPreferencesKey("activation_duration__$configId")
        val SHOW_GESTURE_VISUAL = booleanPreferencesKey("show_gesture_visual__$configId")
        val VISUAL_SIZE = intPreferencesKey("visual_size__$configId")
        val CURSOR_SIZE = intPreferencesKey("cursor_size__$configId")
        val CURSOR_ACCELERATION_START = longPreferencesKey("cursor_acceleration_start__$configId")
        val CURSOR_ACCELERATION_DURATION = longPreferencesKey("cursor_acceleration_duration__$configId")
        val CURSOR_ACTIVATION_KEY = intPreferencesKey("cursor_activation_key__$configId")
        val ALLOW_PASSTHROUGH = booleanPreferencesKey("allow_passthrough__$configId")
        val HIDE_ON_KEYBOARD_OPEN = booleanPreferencesKey("hide_on_keyboard_open__$configId")
        val HIDE_ON_LAUNCHER_OPEN = booleanPreferencesKey("hide_on_launcher_open__$configId")
        val HIDE_ON_LOCK_SCREEN = booleanPreferencesKey("hide_on_lock_screen__$configId")
        val ROUNDED_CURSOR_CORNERS = booleanPreferencesKey("rounded_cursor_corners__$configId")
        val USE_PHYSICAL_SIZE = booleanPreferencesKey("use_physical_size__$configId")
        val STANDARD_CURSOR_HEX = stringPreferencesKey("standard_cursor_hex__$configId")
        val STANDARD_CURSOR_MATCH_BORDER = booleanPreferencesKey("standard_cursor_match_border__$configId")
        val CURSOR_IMAGE_PATH = stringPreferencesKey("cursor_image_path__$configId")
        val CLICKABLE_IMAGE_PATH = stringPreferencesKey("clickable_image_path__$configId")
        val SCROLL_TOGGLE_IMAGE_PATH = stringPreferencesKey("scroll_toggle_image_path__$configId")
        val USE_CUSTOM_CURSOR_ICON = booleanPreferencesKey("use_custom_cursor_icon__$configId")
        val CURSOR_IMAGE_ALIGNMENT = stringPreferencesKey("cursor_image_alignment__$configId")
        val CLICKABLE_IMAGE_ALIGNMENT = stringPreferencesKey("clickable_image_alignment__$configId")
        val SCROLL_TOGGLE_IMAGE_ALIGNMENT = stringPreferencesKey("scroll_toggle_image_alignment__$configId")
        val AUTO_HIDE_APPS = stringPreferencesKey("auto_hide_apps__$configId")
        val CLICKABLE_APPS = stringPreferencesKey("clickable_apps__$configId")
        val SHOW_NOTIFICATION = booleanPreferencesKey("show_notification__$configId")
        val APPLICATION_LIST_TYPE = stringPreferencesKey("application_list_type__$configId")
        val CLICKABLE_LIST_TYPE = stringPreferencesKey("clickable_list_type__$configId")
        val CHECK_CLICKABLE = booleanPreferencesKey("check_clickable__$configId")
        val DISABLE_TOUCHSCREEN = booleanPreferencesKey("disable_touchscreen__$configId")

        preferences[CONFIG_NAME] = settings.configName
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
}
