package scot.raven.titanpad.gesture.api

import android.os.Build
import androidx.compose.ui.geometry.Offset
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.core.shizuku.ShizukuConnection
import scot.raven.titanpad.core.util.VersionUtil
import scot.raven.titanpad.gesture.ui.GesturePath
import scot.raven.titanpad.gesture.ui.GestureType
import scot.raven.titanpad.gesture.ui.endStationaryGesture
import scot.raven.titanpad.gesture.ui.showStationaryGesture
import scot.raven.titanpad.settings.domain.OverlaySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

/**
 * Translates inputs from either cursor mode into gestures, using Shizuku if necessary.
 */
class GestureManager(
    private val defaultStrategy: GestureStrategy,
    private val settingsFlow: StateFlow<OverlaySettings>,
    private val serviceScope: CoroutineScope
) {
    private val _gesturePaths = MutableStateFlow<List<GesturePath>>(emptyList())
    val gesturePaths: StateFlow<List<GesturePath>> = _gesturePaths.asStateFlow()

    private var currentStrategy: GestureStrategy = defaultStrategy
    private var shizukuObserverJob: Job? = null

    private val _isReady = MutableStateFlow(true)
    private var currentTapVisual: String = ""

    fun setGestureReady(ready: Boolean) {
        _isReady.value = ready
    }

    fun getGestureReady(): Boolean {
        return _isReady.value
    }

    init {
        evaluateStrategy()

        shizukuObserverJob = ShizukuConnection.observeStatus { status ->
            Logger.d("Shizuku status changed to: $status, re-evaluating gesture strategy")
            serviceScope.launch {
                delay(1000)
                evaluateStrategy()
            }
        }

        serviceScope.launch {
            settingsFlow.collect {
                Logger.d("Settings changed, re-evaluating gesture strategy")
                evaluateStrategy()
            }
        }
    }

    private fun evaluateStrategy() {
        Logger.d("Using standard gesture strategy")
        currentStrategy = defaultStrategy
    }

    private var shouldShowGestures = false

    private val completionListener = object : GestureCompletionListener {
        override fun onGestureCompleted(success: Boolean) {
            setGestureReady(true)
        }
    }

    suspend fun startTap(x: Float, y: Float): Boolean {
        try {
            Logger.d("Starting tap gesture at ($x, $y)")
            if (!getGestureReady()) return false
            setGestureReady(false)
            if (shouldShowGestures) {
                visualizeTap(x, y)
            }

            return currentStrategy.startTap(x, y, completionListener)
        } catch (e: Exception) {
            Logger.e("Error starting tap gesture", e)
            cancelTap()
            return false
        }
    }

    private fun Float.equalToDecimalPlaces(other: Float, decimalPlaces: Int): Boolean {
        val epsilon = 0.1f.pow(decimalPlaces)
        return abs(this - other) < epsilon
    }

    suspend fun dragTap(fromX: Float, fromY: Float, toX: Float, toY: Float): Boolean {
        if (VersionUtil.belowVersion(Build.VERSION_CODES.O)) {
            return true
        }
        if (shouldShowGestures) {
            // Need to fix visualization lag; remove drag visualization for now
            endVisualizeTap()
            // visualizeTap(toX, toY)
        }
        if (!getGestureReady()) return false
        if (fromX.equalToDecimalPlaces(toX, 4) && fromY.equalToDecimalPlaces(toY, 4)) {
            return false
        }

        setGestureReady(false)

        try {
            Logger.d("Dragging from ($fromX, $fromY) to ($toX, $toY)")
            return currentStrategy.dragTap(fromX, fromY, toX, toY, completionListener)
        } catch (e: Exception) {
            Logger.e("Error during drag tap", e)
            cancelTap()
            return false
        }
    }

    suspend fun endTap(x: Float, y: Float): Boolean {
        if (VersionUtil.belowVersion(Build.VERSION_CODES.O)) {
            return true
        }
        if (!getGestureReady()) return false
        setGestureReady(false)
        if (shouldShowGestures) {
            endVisualizeTap()
        }

        try {
            Logger.d("Ending tap at ($x, $y)")
            return currentStrategy.endTap(x, y, completionListener)
        } catch (e: Exception) {
            Logger.e("Error ending tap gesture", e)
            cancelTap()
            return false
        }
    }

    private fun cancelTap(): Boolean {
        if (VersionUtil.belowVersion(Build.VERSION_CODES.O)) {
            return true
        }

        try {
            Logger.d("Cancelling tap gesture")
            return currentStrategy.cancelTap(completionListener)
        } catch (e: Exception) {
            Logger.e("Error cancelling tap gesture", e)
            return false
        }
    }

    private fun visualizeTap(x: Float, y: Float) {
        _gesturePaths.value = emptyList()
        currentTapVisual = "tap_${System.currentTimeMillis()}"
        showStationaryGesture(
            gestureId = currentTapVisual,
            position = Offset(x, y),
            type = GestureType.TAP,
            pathsFlow = _gesturePaths
        )
    }

    private fun endVisualizeTap() {
        endStationaryGesture(
            gestureId = currentTapVisual,
            pathsFlow = _gesturePaths
        )
    }

    fun updateGestureVisibility(showGestures: Boolean) {
        shouldShowGestures = showGestures
    }

    fun cleanup() {
        shizukuObserverJob?.cancel()
        shizukuObserverJob = null
    }
}