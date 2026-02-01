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
    private var touchDown = false
    private var startX = 0
    private var startY = 0
    private var currentX = 0
    private var currentY = 0
    private var dragStartX = 0.0f
    private var dragStartY = 0.0f
    private var width = 0
    private var height = 0
    private var startPosSet = false
    private var startTime: Long = 0
    private var endTime: Long = 0

    private var numFingers = 0
    private var startGesture = false

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
        when {
            line.contains("BTN_TOUCH") && line.contains("DOWN") -> {
                touchDown = true
                startPosSet = false
                startTime = System.nanoTime()
            }

            line.contains("BTN_TOUCH") && line.contains("UP") -> {
                if (touchDown) {
                    endTime = System.nanoTime()
                }
                touchDown = false
                startPosSet = false
            }

            line.contains("ABS_MT_POSITION_X") -> {
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size >= 3) {
                    val hexValue = parts.last()
                    val newX = hexValue.toIntOrNull(16)
                    if (newX != null) {
                        currentX = newX
                        if (touchDown && !startPosSet) {
                            startX = newX
                        }
                    }
                }
            }

            line.contains("ABS_MT_POSITION_Y") -> {
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size >= 3) {
                    val hexValue = parts.last()
                    val newY = hexValue.toIntOrNull(16)
                    if (newY != null) {
                        currentY = newY
                        if (touchDown && !startPosSet) {
                            startY = newY
                        }
                    }
                }
            }

            line.contains("ABS_MT_TOUCH_MAJOR") -> {
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size >= 3) {
                    val hexValue = parts.last()
                    val newWidth = hexValue.toIntOrNull(16)
                    if (newWidth != null) {
                        width = newWidth
                    }
                }
            }

            line.contains("ABS_MT_TOUCH_MAJOR") -> {
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size >= 3) {
                    val hexValue = parts.last()
                    val newHeight = hexValue.toIntOrNull(16)
                    if (newHeight != null) {
                        height = newHeight
                    }
                }
            }

            line.contains("SYN_REPORT") -> {
                if (touchDown && !startPosSet) {
                    numFingers = if (width <= settingsFlow.value.touchWidthThreshold) 1 else 2
                    startPosSet = true
                    startGesture = true
                }
                detectGesture()
            }
        }
    }

    private fun detectGesture() {
        if (touchDown && startPosSet) {
            val deltaX = currentX - startX + 0.0f
            val deltaY = currentY - startY + 0.0f
            Log.d(DEBUG_TAG, "X: ${deltaX}, Y: ${deltaY}, W: ${width}, NF: ${numFingers}, DSX: ${dragStartX} DSY: ${dragStartY}")

            if (numFingers <= 1) {
                val newPosition = cursorStateManager.applyMovement(Offset(deltaX, deltaY))
                cursorStateManager.updatePosition(newPosition)
                startX = currentX
                startY = currentY
                if (width >= settingsFlow.value.touchWidthThreshold) {
                    numFingers = 2
                    startGesture = true
                }
            } else {
                if (gestureManager.getGestureReady()) {
                    if (cursorStateManager.cursorState.value != null) {
                        val value = cursorStateManager.cursorState.value!!
                        val position = value.position

                        val deltaX = (currentX - startX + 0.0f) * 2
                        val deltaY = (currentY - startY + 0.0f) * 2
                        startX = currentX
                        startY = currentY

                        if (startGesture) {
                            startGesture = false
                            dragStartX = position.x
                            dragStartY = position.y

                            val fromX = dragStartX
                            val fromY = dragStartY

                            scope.launch {
                                gestureManager.startTap(fromX, fromY)
                            }
                        } else {
                            val fromX = dragStartX
                            val fromY = dragStartY

                            scope.launch {
                                gestureManager.dragTap(
                                    fromX,
                                    fromY,
                                    fromX + deltaX,
                                    fromY + deltaY
                                )
                            }
                            dragStartX += deltaX
                            dragStartY += deltaY
                        }
                    }
                }
            }
        }

        if (!touchDown && !startPosSet) {
            val durationMs = (endTime - startTime) / 1_000_000.0
            if (durationMs < 100 || numFingers > 1) {
                if (cursorStateManager.cursorState.value != null) {
                    val value = cursorStateManager.cursorState.value!!
                    val position = value.position
                    Log.d(DEBUG_TAG, "CLICK ${durationMs} X: ${position.x}, Y: ${position.y}, DX: ${dragStartX}, DY: ${dragStartY}")

                    val fromX = dragStartX
                    val fromY = dragStartY
                    var oldFingers = numFingers

                    scope.launch {
                        if (oldFingers<=1) {
                            gestureManager.startTap(position.x, position.y)
                            gestureManager.endTap(position.x, position.y)
                        } else {
                            gestureManager.endTap(-1f, -1f)
                        }
                    }
                }
            }
            numFingers = 0
        }
    }

    companion object {
        const val DEFAULT_TRACKPAD_MAX_X = 1440
        const val DEFAULT_SWIPE_UP_THRESHOLD = 300
        const val DEFAULT_MIN_VELOCITY_THRESHOLD = 2.0  // pixels per millisecond (e.g., 1.0 px/ms = 1000 px/s)
        const val DEFAULT_EVENT_DEVICE = "/dev/input/event7"
        const val DEFAULT_LOG_TAG = "PastieraIME"
        private const val DEBUG_TAG = "TrackpadDebug"
    }
}




