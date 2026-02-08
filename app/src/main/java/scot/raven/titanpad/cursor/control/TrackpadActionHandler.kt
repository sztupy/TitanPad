package scot.raven.titanpad.cursor.control

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import scot.raven.titanpad.BuildConfig
import scot.raven.titanpad.accessibility.AppAccessibilityService
import scot.raven.titanpad.core.control.HidService
import scot.raven.titanpad.core.control.IHidService
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.gesture.api.GestureManager
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
    private val trackpadEventDevice: String = DEFAULT_TRACKPAD_EVENT_DEVICE,
    private val topButtonEventDevice: String = DEFAULT_LEFT_TOP_EVENT_DEVICE,
    private val bottomButtonEventDevice: String = DEFAULT_LEFT_BOTTOM_EVENT_DEVICE,
    private val swipeUpThreshold: Int = DEFAULT_SWIPE_UP_THRESHOLD,
    private val logTag: String = DEFAULT_LOG_TAG,
) {

    private var getTrackpadEventJob: Job? = null
    private var getTopButtonEventJob: Job? = null
    private var getBottomButtonEventJob: Job? = null
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

    private var hidService: IHidService? = null

    fun start() {
        // Guard: if already running, do nothing
        if (isRunning()) {
            Log.d(DEBUG_TAG, "start() SKIPPED: detector already running")
            return
        }

        val enabled = isEnabled()
        Log.d(DEBUG_TAG, "start() called - isEnabled=$enabled, swipeUpThreshold=$swipeUpThreshold")

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

        getTrackpadEventJob?.cancel()
        getBottomButtonEventJob?.cancel()
        getTopButtonEventJob?.cancel()
        Log.d(DEBUG_TAG, "start() launching getevent coroutine...")
        getTrackpadEventJob = eventParser(trackpadEventDevice, ::parseTrackpadEvent)
        getBottomButtonEventJob = eventParser(topButtonEventDevice, ::parseKeyboardEvent)
        getTopButtonEventJob = eventParser(bottomButtonEventDevice, ::parseKeyboardEvent)
        Log.d(DEBUG_TAG, "start() completed - getevent job launched")
        Log.d(logTag, "Trackpad gesture detection started")

        scope.launch(Dispatchers.IO) {
            Log.i(DEBUG_TAG, "hidService starting")
            try {
                bindUserService()
                while (isActive) {
                    delay(100)
                }
            } catch (e: Exception) {
                Log.e(DEBUG_TAG, "hidService: ${e.message}", e)
                Log.e(logTag, "Trackpad hidService failed", e)
            }
            Log.i(DEBUG_TAG, "hidService stopping")
            hidService?.exit()
            unbindUserService()
        }
    }

    fun stop() {
        Log.d(DEBUG_TAG, "stop() called - had active job: ${getTrackpadEventJob != null}")
        getTrackpadEventJob?.cancel()
        getBottomButtonEventJob?.cancel()
        getTopButtonEventJob?.cancel()
        getTrackpadEventJob = null
        getBottomButtonEventJob = null
        getTopButtonEventJob = null
        Log.d(logTag, "Trackpad gesture detection stopped")
    }

    fun eventParser(eventDevice: String, callback: (String) -> Unit): Job {
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
                        callback(line)
                    }
                }
                Log.d(DEBUG_TAG, "getevent reader loop ended")
            } catch (e: Exception) {
                Log.e(DEBUG_TAG, "getevent coroutine FAILED: ${e.message}", e)
                Log.e(logTag, "Trackpad getevent failed", e)
            }
        }
    }

    /**
     * Returns true if the detector is currently running (has an active getevent job).
     */
    fun isRunning(): Boolean {
        return getTrackpadEventJob != null && getTrackpadEventJob?.isActive == true
    }

    private fun parseKeyboardEvent(line: String) {
        when {
            line.contains("EV_KEY") && line.contains("DOWN") -> {
                val parts = line.trim().split(Regex("\\s+"))
                when(parts[1]) {
                    "00f9" -> {
                        Logger.d("Func1 key down")
                        hidService?.keyDown(0x44)
                    }
                    "00fa" -> {
                        Logger.d("Func2 key down")
                        hidService?.keyDown(0x45)
                    }
                }
            }

            line.contains("EV_KEY") && line.contains("UP") -> {
                val parts = line.trim().split(Regex("\\s+"))
                when(parts[1]) {
                    "00f9" -> {
                        Logger.d("Func1 key up")
                        hidService?.keyUp(0x44)
                    }
                    "00fa" -> {
                        Logger.d("Func2 key up")
                        hidService?.keyUp(0x45)
                    }
                }
            }
        }
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

            line.contains("ABS_MT_TOUCH_MINOR") -> {
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
                    numFingers = if (width <= 8) 1 else 2
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

            hidService?.setMousePosition(deltaX.toInt(), deltaY.toInt(), 0)

            if (numFingers <= 1) {
                val newPosition = cursorStateManager.applyMovement(Offset(deltaX, deltaY))
                cursorStateManager.updatePosition(newPosition)
                startX = currentX
                startY = currentY
                if (width >= 10) {
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
            val service = AppAccessibilityService.getInstance()
            val clickable = service?.isNodeClickable(cursorStateManager.cursorState.value?.position) == true && service.showClickableInCurrentApp()
            cursorStateManager.updateClickable(clickable)

            val durationMs = (endTime - startTime) / 1_000_000.0
            if (durationMs < 100 || numFingers > 1) {
                Log.d(DEBUG_TAG, "CLICK")
//                hidService?.setMousePosition(0,0,1)
//                hidService?.setMousePosition(0,0,0)

                if (cursorStateManager.cursorState.value != null) {
                    val value = cursorStateManager.cursorState.value!!
                    val position = value.position
                    Log.d(DEBUG_TAG, "CLICK $durationMs X: ${position.x}, Y: ${position.y}, DX: $dragStartX, DY: $dragStartY")

                    dragStartX
                    dragStartY
                    val oldFingers = numFingers

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

    private val userServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder?) {
            val res = StringBuilder()
            res.append("onServiceConnected: ").append(componentName.className).append('\n')
            if (binder != null && binder.pingBinder()) {
                val service: IHidService = IHidService.Stub.asInterface(binder)
                try {
                    hidService = service
                } catch (e: RemoteException) {
                    e.printStackTrace()
                    res.append(Log.getStackTraceString(e))
                }
            } else {
                res.append("invalid binder for ").append(componentName).append(" received")
            }
            Logger.i(res.toString())
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            hidService = null
            Logger.i("onServiceDisconnected: " + '\n' + componentName.className)
        }
    }

    private val userServiceArgs: UserServiceArgs = UserServiceArgs(
        ComponentName(
            BuildConfig.APPLICATION_ID,
            HidService::class.java.name
        )
    )
        .daemon(false)
        .processNameSuffix("service")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private fun bindUserService() {
        val res = StringBuilder()
        try {
            if (Shizuku.getVersion() < 10) {
                res.append("requires Shizuku API 10")
            } else {
                Shizuku.bindUserService(userServiceArgs, userServiceConnection)
            }
        } catch (tr: Throwable) {
            tr.printStackTrace()
            res.append(tr.toString())
        }
        Logger.i(res.toString().trim { it <= ' ' })
    }

    private fun unbindUserService() {
        val res = StringBuilder()
        try {
            if (Shizuku.getVersion() < 10) {
                res.append("requires Shizuku API 10")
            } else {
                Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
            }
        } catch (tr: Throwable) {
            tr.printStackTrace()
            res.append(tr.toString())
        }
        Logger.i(res.toString().trim { it <= ' ' })
    }

    companion object {
        const val DEFAULT_SWIPE_UP_THRESHOLD = 300

        // Titan 2 event list
//        const val DEFAULT_VOLUME_EVENT_DEVICE = "/dev/input/event0" // volume up and down
        const val DEFAULT_LEFT_TOP_EVENT_DEVICE = "/dev/input/event1" // left top button is event 00f9; Power button is also here
//        const val DEFAULT_MAIN_SCREEN_EVENT_DEVICE = "/dev/input/event2" // main screen touch
        const val DEFAULT_LEFT_BOTTOM_EVENT_DEVICE = "/dev/input/event3" // left bottom button is event 00fa
//        // event4 would be the non-existent headphone jack sensor
//        const val DEFAULT_BACK_SCREEN_EVENT_DEVICE = "/dev/input/event5" // back screen touch
//        const val DEFAULT_KEYBOARD_EVENT_DEVICE = "/dev/input/event6" // keyboard buttons
        const val DEFAULT_TRACKPAD_EVENT_DEVICE = "/dev/input/event7" // keyboard touch
        // event8 has an unknown purpose

        const val DEFAULT_LOG_TAG = "PastieraIME"
        private const val DEBUG_TAG = "TrackpadDebug"
    }
}




