package scot.raven.titanpad.core.constants

object CursorConstants {
    // Size
    const val MIN_SIZE = 1
    const val MAX_SIZE = 10
    const val DEFAULT_SIZE = 5
    const val SIZE_MULTIPLIER = 8f

    // Appearance
    const val STANDARD_CURSOR_HEX = "FFFFFF"

    private const val POLLING_RATE = 60
    const val POLLING_DURATION_MS = 1000f / POLLING_RATE
    const val OPACITY = 0.7f
}