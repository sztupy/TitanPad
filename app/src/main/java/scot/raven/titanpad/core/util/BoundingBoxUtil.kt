package scot.raven.titanpad.core.util

object BoundingBoxUtil {
    fun screenBox(screenWidth: Float, screenHeight: Float, left: Float, top: Float, right: Float, bottom: Float) : Box {
        val x = screenWidth * left / 100f
        val y = screenHeight * top / 100f

        val width =
            screenWidth - screenWidth * (right + left).coerceAtMost(
                95f
            ) / 100f
        val height =
            screenHeight - screenHeight * (top + bottom).coerceAtMost(
                95f
            ) / 100f

        return Box(x,y, width, height)
    }

    fun coerceInto(screenWidth: Float, screenHeight: Float, left: Float, top: Float, right: Float, bottom: Float, screenX: Float, screenY: Float) : Box {
        val boundingBox = screenBox(screenWidth, screenHeight, left, top, right, bottom)

        val x = (boundingBox.x + screenX / screenWidth * boundingBox.width).coerceIn(boundingBox.x, boundingBox.x + boundingBox.width)
        val y = (boundingBox.y + screenY / screenHeight * boundingBox.height).coerceIn(boundingBox.y, boundingBox.y + boundingBox.height)

        return Box(x,y,0f,0f)
    }

    data class Box(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )
}