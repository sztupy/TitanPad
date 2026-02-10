package scot.raven.titanpad.core.util

import android.view.Surface

object OrientationUtil {
    enum class Orientation {
        PORTRAIT,
        LANDSCAPE_RIGHT,
        PORTRAIT_UPSIDE_DOWN,
        LANDSCAPE_LEFT
    }

    fun getOrientationFromRotation(rotation: Int): Orientation {
        return when (rotation) {
            Surface.ROTATION_0 -> Orientation.PORTRAIT
            Surface.ROTATION_90 -> Orientation.LANDSCAPE_LEFT
            Surface.ROTATION_180 -> Orientation.PORTRAIT_UPSIDE_DOWN
            Surface.ROTATION_270 -> Orientation.LANDSCAPE_RIGHT
            else -> Orientation.PORTRAIT
        }
    }
}