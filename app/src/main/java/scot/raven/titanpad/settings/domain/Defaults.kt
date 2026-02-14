package scot.raven.titanpad.settings.domain

import scot.raven.titanpad.core.constants.ApplicationConstants
import scot.raven.titanpad.core.constants.CursorConstants
import scot.raven.titanpad.core.constants.GestureConstants
import scot.raven.titanpad.cursor.domain.FuncButtonMap
import scot.raven.titanpad.cursor.domain.IconAlignment
import scot.raven.titanpad.cursor.domain.InputType
import scot.raven.titanpad.settings.domain.UsageConfig.Companion.KEY_NONE

/**
 * Contains default values that can be modified by the user.
 */

// Maybe reference constants file directly
object Defaults {
    object Settings {
        const val DEFAULT_CONFIG_ID = "default"
        const val DEFAULT_CONFIG_NAME = "Main Config"
        const val ACTIVATION_DURATION = ApplicationConstants.DEFAULT_ACTIVATION_HOLD_DURATION
        const val SHOW_GESTURE_VISUAL = GestureConstants.SHOW_GESTURE_VISUAL
        const val VISUAL_SIZE = GestureConstants.DEFAULT_SIZE
        const val CURSOR_SIZE = CursorConstants.DEFAULT_SIZE
        const val CURSOR_ACCELERATION_START = GestureConstants.DEFAULT_ACCELERATION_START
        const val CURSOR_ACCELERATION_DURATION = GestureConstants.DEFAULT_ACCELERATION_DURATION
        const val CURSOR_ACTIVATION_KEY = KEY_NONE
        const val ALLOW_PASSTHROUGH = GestureConstants.ALLOW_PASSTHROUGH
        val TOUCHPAD_MAIN_INPUT = InputType.HARDWARE_MOUSE
        val TOUCHPAD_LEFT_INPUT = InputType.HARDWARE_SCROLL
        val TOUCHPAD_RIGHT_INPUT = InputType.HARDWARE_SCROLL
        val BACK_SCREEN_INPUT = InputType.OFF
        const val TOUCHPAD_DISABLE_TOP_ROW = true
        const val TOUCHPAD_SPLIT_INPUT = false
        const val TOUCHPAD_SPLIT_POSITION = 25
        const val TOUCHPAD_SPLIT_RIGHT_INPUT = false
        const val TOUCHPAD_SPLIT_RIGHT_POSITION = 75
        val SCROLL_SETTINGS = listOf(
            // central trackpad
            ScrollConfig(
                topCropRegion = 25,
                bottomCropRegion = 25,
                leftCropRegion = 0,
                rightCropRegion = 0,
                touchSensitivity = 9,
                scrollOnlyVertically = false
            ),
            // left trackpad
            ScrollConfig(
                topCropRegion = 25,
                bottomCropRegion = 25,
                leftCropRegion = 20,
                rightCropRegion = 50,
                touchSensitivity = 9,
                scrollOnlyVertically = true
            ),
            // right trackpad
            ScrollConfig(
                topCropRegion = 25,
                bottomCropRegion = 25,
                leftCropRegion = 50,
                rightCropRegion = 20,
                touchSensitivity = 9,
                scrollOnlyVertically = true
            ),
            // back screen
            ScrollConfig(
                topCropRegion = 35,
                bottomCropRegion = 35,
                leftCropRegion = 35,
                rightCropRegion = 35,
                touchSensitivity = 9,
                scrollOnlyVertically = false
            )
        )

        val WHEEL_SETTINGS = listOf(
            // central trackpad
            WheelConfig(
                touchSensitivity = 9,
                speed = 5,
                scrollOnlyVertically = false,
                momentum = false
            ),
            // left trackpad
            WheelConfig(
                touchSensitivity = 9,
                speed = 5,
                scrollOnlyVertically = true,
                momentum = false
            ),
            // right trackpad
            WheelConfig(
                touchSensitivity = 9,
                speed = 5,
                scrollOnlyVertically = true,
                momentum = false
            ),
            // back screen
            WheelConfig(
                touchSensitivity = 9,
                speed = 5,
                scrollOnlyVertically = false,
                momentum = false
            )
        )

        const val MOUSE_TAP_TO_CLICK = true
        const val MOUSE_DOUBLE_TAP_HOLD = true
        const val MOUSE_TWO_FINGER_HOLD = false
        const val MOUSE_TAP_MAX_DURATION = 100
        const val SOFTWARE_MOUSE_SENSITIVITY = 4
        const val SOFTWARE_MOUSE_EXPONENTIAL = true
        const val TWO_FINGER_SENSITIVITY = 8
        val FUNC_1_BUTTON_MAP = FuncButtonMap.OFF
        val FUNC_2_BUTTON_MAP = FuncButtonMap.OFF
        const val HIDE_ON_KEYBOARD_OPEN = true
        const val HIDE_ON_LAUNCHER_OPEN = false
        const val HIDE_ON_LOCK_SCREEN = false
        const val ROUNDED_CURSOR_CORNERS = true
        const val USE_PHYSICAL_SIZE = true
        const val STANDARD_CURSOR_HEX = CursorConstants.STANDARD_CURSOR_HEX
        const val STANDARD_CURSOR_MATCH_BORDER = false
        val CURSOR_IMAGE_PATH = null
        val CLICKABLE_IMAGE_PATH = null
        val SCROLL_TOGGLE_IMAGE_PATH = null
        const val USE_CUSTOM_CURSOR_ICON = false
        val CURSOR_IMAGE_ALIGNMENT = IconAlignment.TOP_LEFT
        val CLICKABLE_IMAGE_ALIGNMENT = IconAlignment.TOP_LEFT
        val SCROLL_TOGGLE_IMAGE_ALIGNMENT = IconAlignment.TOP_LEFT
        val AUTO_ENABLE_APPS = emptySet<String>()
        val AUTO_HIDE_APPS = emptySet<String>()
        val CLICKABLE_APPS = emptySet<String>()
        const val SHOW_NOTIFICATION = false
        val AUTO_ENABLE_LIST_TYPE = AppListType.ALLOW_LIST
        val APPLICATION_LIST_TYPE = AppListType.DENY_LIST
        val CLICKABLE_LIST_TYPE = AppListType.DENY_LIST
        const val AUTO_ENABLE_IF_ON_ONLY = true
        const val AUTO_DISABLE_ACTIVITY = ""
        const val CHECK_CLICKABLE = false
        const val DISABLE_TOUCHSCREEN = false
        const val ALWAYS_REMAP_FUNC_KEYS = true
        const val ALWAYS_REMAP_FUNC_KEYS_COMPAT = false
        const val RUN_AFTER_BOOT = ""
    }
}
