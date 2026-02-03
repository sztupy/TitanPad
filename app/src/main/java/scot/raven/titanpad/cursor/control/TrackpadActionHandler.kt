package scot.raven.titanpad.cursor.control

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.StateFlow
import scot.raven.titanpad.gesture.api.GestureManager
import rikka.shizuku.Shizuku
import scot.raven.titanpad.settings.domain.OverlaySettings
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Listens to trackpad events via Shizuku and triggers callbacks on swipe.
 * Keeps gesture logic isolated so the IME can stay lean and only react to events.
 */
class TrackpadActionHandler(
    private val isEnabled: () -> Boolean,
    private val cursorStateManager: CursorStateManager,
    private val gestureManager: GestureManager,
    private val scope: CoroutineScope,
    private val eventDevice: String = DEFAULT_EVENT_DEVICE,
    private val swipeUpThreshold: Int = DEFAULT_SWIPE_UP_THRESHOLD,
    private val logTag: String = DEFAULT_LOG_TAG,
    private val shizukuPing: () -> Boolean = {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    },
    private val settingsFlow: StateFlow<OverlaySettings>
    ) {

    private var geteventJob: Job? = null
    private var dragStartX = 0.0f
    private var dragStartY = 0.0f
    private var startGesture = false
    private var state = TouchState()

    fun start() {
        // Guard: if already running, do nothing
        if (isRunning()) {
            Log.d(DEBUG_TAG, "start() SKIPPED: detector already running")
            return
        }

        val enabled = isEnabled()
        Log.d(DEBUG_TAG, "start() called - isEnabled=$enabled, swipeUpThreshold=$swipeUpThreshold, eventDevice=$eventDevice")

        if (!enabled) {
            Log.d(DEBUG_TAG, "start() ABORTED: gestures disabled in settings")
            Log.d(logTag, "Trackpad gestures disabled in settings")
            return
        }

        val shizukuRunning = try { Shizuku.pingBinder() } catch (e: Exception) { false }
        val shizukuAuthorized = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) { false }
        val shizukuAvailable = shizukuRunning && shizukuAuthorized
        Log.d(DEBUG_TAG, "start() Shizuku status: running=$shizukuRunning, authorized=$shizukuAuthorized, available=$shizukuAvailable")

        if (!shizukuAvailable) {
            val reason = when {
                !shizukuRunning -> "Shizuku not running"
                !shizukuAuthorized -> "App not authorized in Shizuku"
                else -> "Unknown"
            }
            Log.d(DEBUG_TAG, "start() ABORTED: $reason")
            Log.w(logTag, "Shizuku not available ($reason), trackpad gesture detection disabled")
            return
        }

        geteventJob?.cancel()
        Log.d(DEBUG_TAG, "start() launching getevent coroutine...")
        geteventJob = scope.launch(Dispatchers.IO) {
            try {
                Log.d(DEBUG_TAG, "getevent coroutine started, getting Shizuku.newProcess method...")
                val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                newProcessMethod.isAccessible = true

                Log.d(DEBUG_TAG, "Invoking Shizuku.newProcess for getevent -l $eventDevice")
                val process = newProcessMethod.invoke(
                    null,
                    arrayOf("getevent", "-l", eventDevice),
                    null,
                    null
                ) as Process

                Log.d(DEBUG_TAG, "getevent process started successfully, reading events...")
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        parseTrackpadEvent(line)
                    }
                }
                Log.d(DEBUG_TAG, "getevent reader loop ended")
            } catch (e: Exception) {
                Log.e(DEBUG_TAG, "getevent coroutine FAILED: ${e.message}", e)
                Log.e(logTag, "Trackpad getevent failed", e)
            }
        }
        Log.d(DEBUG_TAG, "start() completed - getevent job launched")
        Log.d(logTag, "Trackpad gesture detection started")
    }

    fun stop() {
        Log.d(DEBUG_TAG, "stop() called - had active job: ${geteventJob != null}")
        geteventJob?.cancel()
        geteventJob = null
        Log.d(logTag, "Trackpad gesture detection stopped")
    }

    /**
     * Returns true if the detector is currently running (has an active getevent job).
     */
    fun isRunning(): Boolean {
        return geteventJob != null && geteventJob?.isActive == true
    }

    private fun parseTrackpadEvent(line: String) {
        fun parseValue(): Int? =
            line.trim().split(Regex("\\s+")).lastOrNull()?.toIntOrNull(16)

        when {

            line.contains("BTN_TOUCH") && line.contains("DOWN") -> {
                state.isDown = true
                state.startPosSet = false
                state.startTime = System.nanoTime()
            }

            line.contains("BTN_TOUCH") && line.contains("UP") -> {
                if (state.isDown) {
                    state.endTime = System.nanoTime()
                }
                state.startPosSet = false
                state.isDown = false
            }

            line.contains("ABS_MT_POSITION_X") -> {
                val value = parseValue()

                if (value != null) {
                    if (state.isDown && !state.startPosSet) {
                        state.startX = value
                    }
                    state.currentX = value
                }
            }

            line.contains("ABS_MT_POSITION_Y") -> {
                val value = parseValue()

                if (value != null) {
                    if (state.isDown && !state.startPosSet) {
                        state.startY = value
                    }
                    state.currentY = value
                }
            }

            line.contains("ABS_MT_TOUCH_MAJOR") -> {
                val value = parseValue()

                if (value != null) {
                    state.width = value
                }
            }

            line.contains("ABS_MT_TOUCH_MINOR") -> {
                val value = parseValue()

                if (value != null) {
                    state.height = value
                }
            }

            line.contains("SYN_REPORT") -> {
                if (state.isDown && !state.startPosSet) {
                    state.startPosSet = true
                    startGesture = true
                }

                detectGesture()
            }
        }
    }

    private fun detectGesture() {
        val settings = settingsFlow.value
        val numFingers = if (state.width <= settings.touchWidthThreshold) 1 else 2

        if (state.isDown && state.startPosSet) {
            val dx = (state.currentX - state.startX).toFloat()
            val dy = (state.currentY - state.startY).toFloat()
            state.startX = state.currentX
            state.startY = state.currentY
            val inScrollArea = inScrollArea()
            val multitouchScroll = settings.scrollMultitouchEnabled && numFingers > 1
            val isScroll = inScrollArea || multitouchScroll

            if (gestureManager.getGestureReady() && isScroll) {
                if (startGesture) {
                    startGesture()
                } else {
                    scroll(dx, dy)
                }
            } else if (!isScroll) {
                moveCursor(dx, dy)
            }
        }

        if (!state.isDown && !state.startPosSet) {
            val durationMs = (state.endTime - state.startTime) / 1_000_000.0
            if (durationMs < settingsFlow.value.clickDuration) {
                click()
            } else {
                endGesture()
            }
        }
    }

    private fun moveCursor(dx: Float, dy: Float) {
        val newPos = cursorStateManager.applyMovement(
            Offset(dx, dy)
        )
        cursorStateManager.updatePosition(newPos)

        state.startX = state.currentX
        state.startY = state.currentY
    }

    private fun startGesture() {
        startGesture = false
        val pos = cursorStateManager.cursorState.value?.position ?: return
        dragStartX = pos.x
        dragStartY = pos.y
        scope.launch {
            gestureManager.startTap(dragStartX, dragStartY)
        }
    }

    private fun scroll(dx: Float, dy: Float) {
        val fromX = dragStartX
        val fromY = dragStartY
        val toX = fromX + dx * 2
        val toY = fromY + dy * 2

        scope.launch {
            gestureManager.dragTap(fromX, fromY, toX, toY)
        }

        dragStartX = toX
        dragStartY = toY
    }

    private fun click() {
        val pos = cursorStateManager.cursorState.value?.position ?: return
        scope.launch {
            gestureManager.startTap(pos.x, pos.y)
            gestureManager.endTap(pos.x, pos.y)
        }
    }

    private fun endGesture() {
        scope.launch {
            gestureManager.endTap(dragStartX, dragStartY)
        }
    }

    private fun inScrollArea(): Boolean {
        val settings = settingsFlow.value

        if (!settings.scrollAreaEnabled) {
            return false
        }

        val left = DEFAULT_TRACKPAD_MAX_X * (settings.scrollAreaLeftPercent / 100.0)
        val right = DEFAULT_TRACKPAD_MAX_X - DEFAULT_TRACKPAD_MAX_X * (settings.scrollAreaRightPercent / 100.0)
        val top = DEFAULT_TRACKPAD_MAX_Y * (settings.scrollAreaTopPercent / 100.0)
        val bottom = DEFAULT_TRACKPAD_MAX_Y - DEFAULT_TRACKPAD_MAX_Y * (settings.scrollAreaBottomPercent / 100.0)

        return state.currentX <= left ||
                state.currentX >= right ||
                state.currentY <= top ||
                state.currentY >= bottom
    }

    companion object {
        const val DEFAULT_TRACKPAD_MAX_X = 1440
        const val DEFAULT_TRACKPAD_MAX_Y = 720
        const val DEFAULT_SWIPE_UP_THRESHOLD = 300
        const val DEFAULT_MIN_VELOCITY_THRESHOLD = 2.0  // pixels per millisecond (e.g., 1.0 px/ms = 1000 px/s)
        const val DEFAULT_EVENT_DEVICE = "/dev/input/event7"
        const val DEFAULT_LOG_TAG = "PastieraIME"
        private const val DEBUG_TAG = "TrackpadDebug"
    }
}

data class TouchState(
    var isDown: Boolean = false,
    var startPosSet: Boolean = false,
    var startTime: Long = 0L,
    var endTime: Long = 0L,

    var startX: Int = 0,
    var startY: Int = 0,
    var currentX: Int = 0,
    var currentY: Int = 0,

    var width: Int = 0,
    var height: Int = 0,
)
