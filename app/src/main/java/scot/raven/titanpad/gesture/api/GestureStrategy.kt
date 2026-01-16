package scot.raven.titanpad.gesture.api

/**
 * Strategy interface for gesture implementations.
 */
interface GestureStrategy {
    suspend fun startTap(x: Float, y: Float, completionListener: GestureCompletionListener? = null): Boolean

    suspend fun dragTap(fromX: Float, fromY: Float, toX: Float, toY: Float, completionListener: GestureCompletionListener? = null): Boolean

    suspend fun endTap(finalX: Float, finalY: Float, completionListener: GestureCompletionListener? = null): Boolean

    fun cancelTap(completionListener: GestureCompletionListener?): Boolean
}