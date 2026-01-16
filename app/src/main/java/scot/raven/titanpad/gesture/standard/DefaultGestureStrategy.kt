package scot.raven.titanpad.gesture.standard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import scot.raven.titanpad.core.constants.GestureConstants
import scot.raven.titanpad.core.domain.GestureStyle
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.gesture.api.GestureCompletionListener
import scot.raven.titanpad.gesture.api.GestureStrategy
import scot.raven.titanpad.settings.domain.OverlaySettings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implements gestures using the AccessibilityService API.
 */
class DefaultGestureStrategy(
    private val service: AccessibilityService,
    private val settingsFlow: StateFlow<OverlaySettings>
) : GestureStrategy {

    private val scrollPath = Path()
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

    // Callbacks to pause for fixed gesture style
    private fun completeGestureCallback(completionListener: GestureCompletionListener?): AccessibilityService.GestureResultCallback {
        return object : AccessibilityService.GestureResultCallback () {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                completionListener?.onGestureCompleted(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                completionListener?.onGestureCompleted(true)
            }
        }
    }

    private fun pauseGestureCallback(
        stroke: GestureDescription.StrokeDescription,
        endX: Float,
        endY: Float,
        willContinue: Boolean,
        completionListener: GestureCompletionListener?
    ): AccessibilityService.GestureResultCallback {
        return object : AccessibilityService.GestureResultCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onCompleted(gestureDescription: GestureDescription?) {
                val settings = settingsFlow.value
                if (willContinue) {
                    val pausePath = Path().apply {
                        when (settings.gestureStyle) {
                            GestureStyle.FIXED -> moveTo(endX, endY)
                            GestureStyle.FIXED_2 -> moveTo(endX / 2, endY / 2)
                            else -> moveTo(endX, endY)
                        }
                    }

                    val pauseStrokeDescription = stroke.continueStroke(
                        pausePath,
                        0,
                        GestureConstants.GESTURE_PAUSE,
                        false
                    )

                    val pauseGesture = GestureDescription.Builder()
                        .addStroke(pauseStrokeDescription)
                        .build()

                    service.dispatchGesture(pauseGesture, completeGestureCallback(completionListener), null)
                } else {
                    completionListener?.onGestureCompleted(true)
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                completionListener?.onGestureCompleted(true)
            }
        }
    }

    private fun pauseGestureCallback(
        stroke1: GestureDescription.StrokeDescription,
        stroke2: GestureDescription.StrokeDescription,
        endX1: Float, endY1: Float,
        endX2: Float, endY2: Float,
        willContinue: Boolean,
        completionListener: GestureCompletionListener?
    ): AccessibilityService.GestureResultCallback {
        return object : AccessibilityService.GestureResultCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (willContinue) {
                    val finger1PausePath = Path().apply {
                        moveTo(endX1, endY1)
                    }

                    val finger2PausePath = Path().apply {
                        moveTo(endX2, endY2)
                    }

                    val stroke1Pause = stroke1.continueStroke(
                        finger1PausePath,
                        0,
                        GestureConstants.GESTURE_PAUSE,
                        false
                    )

                    val stroke2Pause = stroke2.continueStroke(
                        finger2PausePath,
                        0,
                        GestureConstants.GESTURE_PAUSE,
                        false
                    )

                    val pauseGesture = GestureDescription.Builder()
                        .addStroke(stroke1Pause)
                        .addStroke(stroke2Pause)
                        .build()

                    service.dispatchGesture(pauseGesture, completeGestureCallback(completionListener), null)
                } else {
                    completionListener?.onGestureCompleted(true)
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                completionListener?.onGestureCompleted(true)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
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