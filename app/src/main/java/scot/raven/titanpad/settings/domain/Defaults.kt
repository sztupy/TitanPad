package scot.raven.titanpad.settings.domain

import android.view.KeyEvent
import scot.raven.titanpad.core.constants.ApplicationConstants
import scot.raven.titanpad.core.constants.CursorConstants
import scot.raven.titanpad.core.constants.GestureConstants
import scot.raven.titanpad.core.domain.GestureStyle
import scot.raven.titanpad.cursor.domain.IconAlignment

/**
 * Contains default values that can be modified by the user.
 */

// Maybe reference constants file directly
object Defaults {
    object Settings {
        const val ACTIVATION_DURATION = ApplicationConstants.DEFAULT_ACTIVATION_HOLD_DURATION
        const val USE_NATURAL_SCROLLING = GestureConstants.USE_NATURAL_SCROLLING
        const val SHOW_GESTURE_VISUAL = GestureConstants.SHOW_GESTURE_VISUAL
        const val VISUAL_SIZE = GestureConstants.DEFAULT_SIZE
        const val CURSOR_SIZE = CursorConstants.DEFAULT_SIZE
        const val CURSOR_ACCELERATION_START = GestureConstants.DEFAULT_ACCELERATION_START
        const val CURSOR_ACCELERATION_DURATION = GestureConstants.DEFAULT_ACCELERATION_DURATION
        const val CURSOR_ACTIVATION_KEY = KeyEvent.KEYCODE_STAR
        val GESTURE_STYLE = GestureStyle.FIXED
        const val ALLOW_PASSTHROUGH = GestureConstants.ALLOW_PASSTHROUGH
        const val ENABLE_SHIZUKU_INTEGRATION = true
        const val HIDE_ON_KEYBOARD_OPEN = false
        const val HIDE_ON_LAUNCHER_OPEN = false
        const val HIDE_ON_LOCK_SCREEN = false
        const val ROUNDED_CURSOR_CORNERS = true
        const val USE_PHYSICAL_SIZE = true
        const val STANDARD_CURSOR_HEX = CursorConstants.STANDARD_CURSOR_HEX
        const val STANDARD_CURSOR_MATCH_BORDER = false
        const val ALLOW_OVERLAPPING_GESTURES = false
        val CURSOR_IMAGE_PATH = null
        val CLICKABLE_IMAGE_PATH = null
        val SCROLL_TOGGLE_IMAGE_PATH = null
        const val USE_CUSTOM_CURSOR_ICON = false
        val CURSOR_IMAGE_ALIGNMENT = IconAlignment.TOP_LEFT
        val CLICKABLE_IMAGE_ALIGNMENT = IconAlignment.TOP_LEFT
        val SCROLL_TOGGLE_IMAGE_ALIGNMENT = IconAlignment.TOP_LEFT
        const val COLLECT_LOGS = false
        val AUTO_HIDE_APPS = emptySet<String>()
        val CLICKABLE_APPS = emptySet<String>()
        const val SHOW_NOTIFICATION = false
        val APPLICATION_LIST_TYPE = AppListType.DENY_LIST
        val CLICKABLE_LIST_TYPE = AppListType.DENY_LIST
        const val CHECK_CLICKABLE = false
        const val KEEP_CURRENT_GRID_TRANSPARENT = true
        const val DISABLE_TOUCHSCREEN = false
        const val TOUCH_WIDTH_THRESHOLD = 8
        const val CLICK_DURATION = 100L
        const val SCROLL_AREA_TOP_PERCENT = 10F
        const val SCROLL_AREA_BOTTOM_PERCENT = 10F
        const val SCROLL_AREA_RIGHT_PERCENT = 10F
        const val SCROLL_AREA_LEFT_PERCENT = 10F
        const val SCROLL_AREA_ENABLED = false
        const val SCROLL_MULTITOUCH_ENABLED = true
    }
}
