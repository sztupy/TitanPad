package scot.raven.titanpad.core.util

import android.view.KeyEvent

object KeyCodeUtil {
    fun keyCodeToString(keyCode: Int): String {
        return when (keyCode) {
            10183 -> "KEYCODE_FUNC1"
            10184 -> "KEYCODE_FUNC2"
            in 10000..Int.MAX_VALUE -> "SCANCODE_${keyCode-10000}"
            else -> KeyEvent.keyCodeToString(
                keyCode
            )
        }
    }
}