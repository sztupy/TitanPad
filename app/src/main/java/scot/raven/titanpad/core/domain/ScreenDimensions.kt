package scot.raven.titanpad.core.domain

import kotlin.math.sqrt

/**
 * Handles screen size calculations.
 */
data class ScreenDimensions(
    val width: Int,
    val height: Int
) {

    fun center(): Pair<Float, Float> {
        return Pair(width / 2f, height / 2f)
    }

    fun constrainToBounds(x: Float, y: Float): Pair<Float, Float> {
        return Pair(
            x.coerceIn(0f, width.toFloat()),
            y.coerceIn(0f, height.toFloat())
        )
    }

    fun getScreenScaleFactor(): Float {
        return sqrt(width * height * 1.0f) / 1000f
    }
}