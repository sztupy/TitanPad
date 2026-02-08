package scot.raven.titanpad.gesture.standard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.gesture.api.GestureCompletionListener
import scot.raven.titanpad.gesture.api.GestureStrategy
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implements gestures using the AccessibilityService API.
 */
class DefaultGestureStrategy(
    private val service: AccessibilityService
) : GestureStrategy {
    private val tapPath = Path()
    private var activeStroke: GestureDescription.StrokeDescription? = null

    private suspend fun AccessibilityService.dispatchGestureAwait(
        gesture: GestureDescription,
        _onCompleted: (() -> Unit)? = null,
        _onCancelled: (() -> Unit)? = null
    ): Boolean =
        suspendCancellableCoroutine { cont ->
            val handler = Handler(Looper.getMainLooper())
            dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    _onCompleted?.invoke()
                    cont.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    _onCancelled?.invoke()
                    cont.resume(false)
                }
            }, handler)

            cont.invokeOnCancellation {
                // Optional: cancel gesture if coroutine is cancelled
                // Not all versions support cancelling gestures, but can try:
                // dispatchGesture(gesture, null, handler) ?
            }
        }

    override suspend fun startTap(x: Float, y: Float, completionListener: GestureCompletionListener?): Boolean {
        try {
            Logger.d("DefaultGestureStrategy: starting tap at ($x, $y)")

            tapPath.reset()
            tapPath.moveTo(x, y)

            activeStroke = GestureDescription.StrokeDescription(
                tapPath,
                0,
                1,
                true // Hold gesture
            )

            val gesture = GestureDescription.Builder()
                .addStroke(activeStroke!!)
                .build()

            service.dispatchGestureAwait(
                gesture,
                {
                    Logger.d("DefaultGestureStrategy: start tap completed successfully")
                    completionListener?.onGestureCompleted(true)
                },
                {
                    Logger.d("DefaultGestureStrategy: start tap was cancelled")
                    completionListener?.onGestureCompleted(true)
                }
            )

            return true

        } catch (e: Exception) {
            cancelTap(completionListener)
            Logger.e("Error starting tap", e)
            return false
        }
    }

    override suspend fun dragTap(fromX: Float, fromY: Float, toX: Float, toY: Float, completionListener: GestureCompletionListener?): Boolean {
        try {
            Logger.d("DefaultGestureStrategy: dragging from ($fromX, $fromY) to ($toX, $toY)")

            if (activeStroke == null) {
                Logger.d("Cannot continue drag: no active long press")
                cancelTap(completionListener)
                return false
            }

            tapPath.reset()
            tapPath.moveTo(fromX, fromY)
            tapPath.lineTo(toX, toY)

            val continuedStroke = activeStroke!!.continueStroke(
                tapPath,
                0, // Continue immediately
                1,
                true
            )

            activeStroke = continuedStroke

            val gesture = GestureDescription.Builder()
                .addStroke(continuedStroke)
                .build()

            service.dispatchGestureAwait(
                gesture,
                {
                    Logger.d("DefaultGestureStrategy: drag completed successfully")
                    completionListener?.onGestureCompleted(true)
                },
                {
                    Logger.d("DefaultGestureStrategy: drag was cancelled")
                    completionListener?.onGestureCompleted(true)
                }
            )

            return true

        } catch (e: Exception) {
            cancelTap(completionListener)
            Logger.e("Error continuing drag", e)
            return false
        }
    }

    override suspend fun endTap(finalX: Float, finalY: Float, completionListener: GestureCompletionListener?): Boolean {
        try {
            Logger.d("DefaultGestureStrategy: ending tap at ($finalX, $finalY)")

            if (activeStroke == null) {
                Logger.d("Cannot end tap: no active tap operation")
                cancelTap(completionListener)
                return false
            }

            tapPath.reset()
            tapPath.moveTo(finalX, finalY)

            val finalStroke = activeStroke!!.continueStroke(
                tapPath,
                0,
                1,
                false
            )

            val gesture = GestureDescription.Builder()
                .addStroke(finalStroke)
                .build()

            service.dispatchGestureAwait(
                gesture,
                {
                    Logger.d("DefaultGestureStrategy: end tap completed successfully")
                    completionListener?.onGestureCompleted(true)
                },
                {
                    Logger.d("DefaultGestureStrategy: end tap was cancelled")
                    completionListener?.onGestureCompleted(true)
                }
            )

            return true

        } catch (e: Exception) {
            cancelTap(completionListener)
            Logger.e("Error ending tap", e)
            return false
        }
    }

    override fun cancelTap(completionListener: GestureCompletionListener?): Boolean {
        completionListener?.onGestureCompleted(true)

        if (activeStroke == null) {
            return false
        }

        activeStroke = null
        Logger.d("DefaultGestureStrategy: tap operation cancelled")
        return true
    }
}