package scot.raven.titanpad.cursor.control

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
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
    private val logTag: String = DEFAULT_LOG_TAG,
    private val settingsFlow: StateFlow<OverlaySettings>
    ) {

    private var geteventJobs = mutableListOf<Job>()
    private var clickJob: Job? = null
    private var dragStartX = 0.0f
    private var dragStartY = 0.0f
    private var gesturePhase = GesturePhase.ENDED
    private var touchState = TouchState()
    private var forceScroll  = false

    fun start() {
        // Guard: if already running, do nothing
        if (isRunning()) {
            Log.d(DEBUG_TAG, "start() SKIPPED: detector already running")
            return
        }

        val enabled = isEnabled()
        Log.d(DEBUG_TAG, "start() called - isEnabled=$enabled")

        if (!enabled) {
            Log.d(DEBUG_TAG, "start() ABORTED: gestures disabled in settings")
            Log.d(logTag, "Trackpad gestures disabled in settings")
            return
        }

        val shizukuRunning = try { Shizuku.pingBinder() } catch (_: Exception) { false }
        val shizukuAuthorized = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) { false }
        val shizukuAvailable = shizukuRunning && shizukuAuthorized
        Log.d(DEBUG_TAG, "start() Shizuku status: running=$shizukuRunning, authorized=$shizukuAuthorized, available=$shizukuAvailable")

        if (!shizukuAvailable) {
            Log.d(DEBUG_TAG, "start() ABORTED")
            Log.w(logTag, "Shizuku not available, trackpad gesture detection disabled")
            return
        }

        geteventJobs.forEach { it.cancel() }
        geteventJobs.clear()

        Log.d(DEBUG_TAG, "start() launching getevent coroutines...")
        geteventJobs += launchGeteventJob(TOUCHPAD_EVENT_DEVICE)
        geteventJobs += launchGeteventJob(SUB_TOUCH_EVENT_DEVICE)

        Log.d(DEBUG_TAG, "start() completed - getevent jobs launched")
        Log.d(logTag, "Trackpad gesture detection started")
    }

    fun stop() {
        Log.d(DEBUG_TAG, "stop() called - had active jobs: ${geteventJobs.size}")
        geteventJobs.forEach { it.cancel() }
        geteventJobs.clear()
        Log.d(logTag, "Trackpad gesture detection stopped")
    }

    /**
     * Returns true if the detector is currently running (has an active getevent job).
     */
    fun isRunning(): Boolean {
        return geteventJobs.any { it.isActive }
    }

    private fun launchGeteventJob(eventDevice: String): Job {
        return scope.launch(Dispatchers.IO) {
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
                        touchState.device = when (eventDevice) {
                            TOUCHPAD_EVENT_DEVICE -> EventDevice.TOUCHPAD
                            SUB_TOUCH_EVENT_DEVICE -> EventDevice.SUB_TOUCH
                            else -> touchState.device
                        }
                        parseTrackpadEvent(line)
                    }
                }
                Log.d(DEBUG_TAG, "getevent -l $eventDevice reader loop ended")
            } catch (e: Exception) {
                Log.e(DEBUG_TAG, "getevent coroutine FAILED: ${e.message}", e)
                Log.e(logTag, "getevent -l $eventDevice failed", e)
            }
        }
    }

    private fun parseTrackpadEvent(line: String) {
        if (touchState.device == EventDevice.SUB_TOUCH && !settingsFlow.value.subTouchEnabled) {
            return
        }

        fun parseValue(): Int? =
            line.trim().split(Regex("\\s+")).lastOrNull()?.toIntOrNull(16)

        when {

            line.contains("BTN_TOUCH") && line.contains("DOWN") -> {
                touchState.isDown = true
                touchState.startPosSet = false
                touchState.startTime = System.nanoTime()
            }

            line.contains("BTN_TOUCH") && line.contains("UP") -> {
                if (touchState.isDown) {
                    touchState.endTime = System.nanoTime()
                }
                touchState.startPosSet = false
                touchState.isDown = false
                touchState.device = EventDevice.NONE
            }

            line.contains("ABS_MT_POSITION_X") -> {
                val value = parseValue()

                if (value != null) {
                    if (touchState.isDown && !touchState.startPosSet) {
                        touchState.startX = value
                    }
                    touchState.currentX = value
                }
            }

            line.contains("ABS_MT_POSITION_Y") -> {
                val value = parseValue()

                if (value != null) {
                    if (touchState.isDown && !touchState.startPosSet) {
                        touchState.startY = value
                    }
                    touchState.currentY = value
                }
            }

            line.contains("ABS_MT_TOUCH_MAJOR") -> {
                val value = parseValue()

                if (value != null) {
                    touchState.width = value
                }
            }

            line.contains("ABS_MT_TOUCH_MINOR") -> {
                val value = parseValue()

                if (value != null) {
                    touchState.height = value
                }
            }

            line.contains("SYN_REPORT") -> {
                if (touchState.isDown && !touchState.startPosSet) {
                    touchState.startPosSet = true
                    gesturePhase = GesturePhase.PENDING
                }

                detectGesture()
            }
        }
    }

    private fun detectGesture() {
        val settings = settingsFlow.value
        val numFingers = if (touchState.width <= settings.touchWidthThreshold) 1 else 2

        if (touchState.isDown && touchState.startPosSet && gestureManager.getGestureReady()) {
            val dx = (touchState.currentX - touchState.startX).toFloat()
            val dy = (touchState.currentY - touchState.startY).toFloat()
            touchState.startX = touchState.currentX
            touchState.startY = touchState.currentY
            val inScrollArea = inScrollArea()
            val multitouchScroll = settings.scrollMultitouchEnabled && numFingers > 1
            val isScroll = inScrollArea || multitouchScroll || forceScroll

            if (isScroll) {
                if (gesturePhase == GesturePhase.PENDING) {
                    startGesture()
                } else {
                    scroll(dx, dy)
                }
            } else {
                moveCursor(dx, dy)
            }
        }

        if (!touchState.isDown && !touchState.startPosSet) {
            val durationMs = (touchState.endTime - touchState.startTime) / 1_000_000.0
            val isClick = durationMs < settings.clickDuration

            if (isClick) {
                if (isSecondTap()) {
                    doubleTap()
                } else if (settings.activateScrollByDoubleTap) {
                    // delay only when there is need for it
                    clickJob = scope.launch {
                        delay(250)
                        clickJob = null
                        click()
                    }
                } else {
                    click()
                }
            }
            else if (gesturePhase == GesturePhase.ACTIVE || gesturePhase == GesturePhase.STARTED) {
                endGesture()
            }
        }
    }

    private fun isSecondTap(): Boolean {
        return clickJob != null
    }

    private fun doubleTap() {
        clickJob?.cancel()
        clickJob = null
        forceScroll = !forceScroll && settingsFlow.value.activateScrollByDoubleTap
    }

    private fun moveCursor(dx: Float, dy: Float) {
        val settings = settingsFlow.value
        val scaledDx = dx * settings.horizontalCursorSensitivity
        val scaledDy = dy * settings.verticalCursorSensitivity
        val newPos = cursorStateManager.applyMovement(
            Offset(scaledDx, scaledDy)
        )
        cursorStateManager.updatePosition(newPos)

        touchState.startX = touchState.currentX
        touchState.startY = touchState.currentY
    }

    private fun startGesture() {
        gesturePhase = GesturePhase.STARTED
        val pos = cursorStateManager.cursorState.value?.position ?: return
        dragStartX = pos.x
        dragStartY = pos.y
        scope.launch {
            gestureManager.startTap(dragStartX, dragStartY)
        }
    }

    private fun scroll(dx: Float, dy: Float) {
        gesturePhase = GesturePhase.ACTIVE
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
        gesturePhase = GesturePhase.ENDED
        val pos = cursorStateManager.cursorState.value?.position ?: return
        scope.launch {
            gestureManager.startTap(pos.x, pos.y)
            gestureManager.endTap(pos.x, pos.y)
        }
    }

    private fun endGesture() {
        gesturePhase = GesturePhase.ENDED
        scope.launch {
            gestureManager.endTap(dragStartX, dragStartY)
        }
    }

    private fun inScrollArea(): Boolean {
        val settings = settingsFlow.value

        if (!settings.scrollAreaEnabled) {
            return false
        }

        if (touchState.device == EventDevice.SUB_TOUCH) {
            return false
        }

        val left = TOUCHPAD_MAX_X * (settings.scrollAreaLeftPercent / 100.0)
        val right = TOUCHPAD_MAX_X - TOUCHPAD_MAX_X * (settings.scrollAreaRightPercent / 100.0)
        val top = TOUCHPAD_MAX_Y * (settings.scrollAreaTopPercent / 100.0)
        val bottom = TOUCHPAD_MAX_Y - TOUCHPAD_MAX_Y * (settings.scrollAreaBottomPercent / 100.0)

        return touchState.currentX <= left ||
                touchState.currentX >= right ||
                touchState.currentY <= top ||
                touchState.currentY >= bottom
    }

    companion object {
        const val TOUCHPAD_MAX_X = 1440
        const val TOUCHPAD_MAX_Y = 720
        const val TOUCHPAD_EVENT_DEVICE = "/dev/input/event7"
        const val SUB_TOUCH_EVENT_DEVICE = "/dev/input/event5"
        const val DEFAULT_LOG_TAG = "Trackpad"
        private const val DEBUG_TAG = "TrackpadDebug"
    }
}

data class TouchState(
    var device: EventDevice = EventDevice.NONE,
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

enum class GesturePhase {
    PENDING,
    STARTED,
    ACTIVE,
    ENDED
}

enum class EventDevice {
    NONE,
    TOUCHPAD,
    SUB_TOUCH,
}
