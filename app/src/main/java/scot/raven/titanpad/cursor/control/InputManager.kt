package scot.raven.titanpad.cursor.control

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import scot.raven.titanpad.BuildConfig
import scot.raven.titanpad.core.control.HidService
import scot.raven.titanpad.core.control.IHidService
import scot.raven.titanpad.core.control.ModeCoordinator
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.gesture.api.GestureManager
import scot.raven.titanpad.settings.domain.ApplicationSettings
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Listens to trackpad events via Shizuku and triggers callbacks on swipe.
 * Keeps gesture logic isolated so the IME can stay lean and only react to events.
 */
class InputManager(
    private val isEnabled: () -> Boolean,
    private val cursorStateManager: CursorStateManager,
    private val gestureManager: GestureManager,
    private val modeCoordinator: ModeCoordinator,
    private val scope: CoroutineScope,
    private val settingsFlow: StateFlow<ApplicationSettings>,
    private val trackpadEventDevice: String = DEFAULT_TRACKPAD_EVENT_DEVICE,
    private val backScreenEventDevice: String = DEFAULT_BACK_SCREEN_EVENT_DEVICE,
    private val topButtonEventDevice: String = DEFAULT_LEFT_TOP_EVENT_DEVICE,
    private val bottomButtonEventDevice: String = DEFAULT_LEFT_BOTTOM_EVENT_DEVICE,
    private val logTag: String = DEFAULT_LOG_TAG,
) {

    private var getTrackpadEventJob: Job? = null
    private var getBackScreenEventJob: Job? = null
    private var getTopButtonEventJob: Job? = null
    private var getBottomButtonEventJob: Job? = null
    private var hidService: IHidService? = null
    private var keyboardInputHandler = KeyInputHandler(
        settingsFlow = settingsFlow,
        modeCoordinator = modeCoordinator
    )
    private var trackpadInputHandler = TouchInputHandler(
        cursorStateManager = cursorStateManager,
        gestureManager = gestureManager,
        settingsFlow = settingsFlow,
        scope = scope,
        modeCoordinator = modeCoordinator,
        backScreenMode = false
    )
    private var backScreenInputHandler = TouchInputHandler(
        cursorStateManager = cursorStateManager,
        gestureManager = gestureManager,
        settingsFlow = settingsFlow,
        scope = scope,
        modeCoordinator = modeCoordinator,
        backScreenMode = true
    )

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
        getBackScreenEventJob?.cancel()
        getBottomButtonEventJob?.cancel()
        getTopButtonEventJob?.cancel()

        Log.d(DEBUG_TAG, "start() launching getevent coroutine...")
        getTrackpadEventJob = eventParser(trackpadEventDevice, trackpadInputHandler)
        getBackScreenEventJob = eventParser(backScreenEventDevice, backScreenInputHandler)
        getBottomButtonEventJob = eventParser(topButtonEventDevice, keyboardInputHandler)
        getTopButtonEventJob = eventParser(bottomButtonEventDevice, keyboardInputHandler)
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
            hidService=null
            trackpadInputHandler.setHidService(null)
            keyboardInputHandler.setHidService(null)
            backScreenInputHandler.setHidService(null)
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

    fun eventParser(eventDevice: String, inputHandler: InputHandler): Job {
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
                        inputHandler.parseInput(line)
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

    private val userServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder?) {
            val res = StringBuilder()
            res.append("onServiceConnected: ").append(componentName.className).append('\n')
            if (binder != null && binder.pingBinder()) {
                val service: IHidService = IHidService.Stub.asInterface(binder)
                try {
                    hidService = service
                    trackpadInputHandler.setHidService(service)
                    keyboardInputHandler.setHidService(service)
                    backScreenInputHandler.setHidService(service)
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
            trackpadInputHandler.setHidService(null)
            keyboardInputHandler.setHidService(null)
            backScreenInputHandler.setHidService(null)
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
        // Titan 2 event list
//        const val DEFAULT_VOLUME_EVENT_DEVICE = "/dev/input/event0" // volume up and down
        const val DEFAULT_LEFT_TOP_EVENT_DEVICE = "/dev/input/event1" // left top button is event 00f9; Power button is also here
//        const val DEFAULT_MAIN_SCREEN_EVENT_DEVICE = "/dev/input/event2" // main screen touch
        const val DEFAULT_LEFT_BOTTOM_EVENT_DEVICE = "/dev/input/event3" // left bottom button is event 00fa
//        // event4 would be the non-existent headphone jack sensor
        const val DEFAULT_BACK_SCREEN_EVENT_DEVICE = "/dev/input/event5" // back screen touch
//        const val DEFAULT_KEYBOARD_EVENT_DEVICE = "/dev/input/event7" // keyboard buttons
        const val DEFAULT_TRACKPAD_EVENT_DEVICE = "/dev/input/event6" // keyboard touch
        // event8 has an unknown purpose

        const val DEFAULT_LOG_TAG = "PastieraIME"
        private const val DEBUG_TAG = "InputManager"
    }
}




