package scot.raven.titanpad.cursor.domain

enum class TouchZone {
    LEFT,
    CENTER,
    RIGHT;

    companion object {
        fun fromPosition(
            x: Float,
            width: Float,
            splitLeftPercent: Int,
            splitRightPercent: Int,
            leftEnabled: Boolean = true,
            rightEnabled: Boolean = true
        ): TouchZone {
            val leftBoundary = width * (splitLeftPercent / 100f)
            val rightBoundary = width * (1f - splitRightPercent / 100f)

            val rawZone = when {
                x < leftBoundary -> LEFT
                x < rightBoundary -> CENTER
                else -> RIGHT
            }

            return when (rawZone) {
                LEFT -> if (leftEnabled) LEFT else CENTER
                RIGHT -> if (rightEnabled) RIGHT else CENTER
                CENTER -> CENTER
            }
        }
    }
}

