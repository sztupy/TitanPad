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
import scot.raven.titanpad.core.evdev.IEvdevReaderService
import scot.raven.titanpad.core.control.ModeCoordinator
import scot.raven.titanpad.core.evdev.EvdevDevice
import scot.raven.titanpad.core.evdev.EvdevReaderService
import scot.raven.titanpad.core.evdev.IEvdevDevice
import scot.raven.titanpad.core.evdev.IEventCallback
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.gesture.api.GestureManager
import scot.raven.titanpad.settings.domain.ApplicationSettings
import kotlin.String

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

    private var inputReaderService: IEvdevReaderService? = null

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

        scope.launch(Dispatchers.IO) {
            Log.i(DEBUG_TAG, "hidService starting")
            try {
                bindHidUserService()
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
            unbindHidUserService()
        }

        scope.launch(Dispatchers.IO) {
            Log.i(DEBUG_TAG, "inputReaderService starting")
            try {
                bindInputReaderUserService()
                Log.i(DEBUG_TAG, "inputReaderService bound")

                while (isActive && inputReaderService == null) {
                    Log.i(DEBUG_TAG, "waiting for inputReaderService to become active")
                    delay(1000)
                }

                val deviceList: List<IBinder>? = inputReaderService?.devices() as List<IBinder>?
                try {
                    if (deviceList != null) {
                        Log.i(DEBUG_TAG, "inputReaderService devicelist: ${deviceList.size}")
                        for (deviceBinder in deviceList) {
                            val device = IEvdevDevice.Stub.asInterface(deviceBinder)
                            when (device.path) {
                                trackpadEventDevice -> getTrackpadEventJob = eventParser(device, trackpadInputHandler)
                                backScreenEventDevice -> getBackScreenEventJob = eventParser(device, backScreenInputHandler)
                                topButtonEventDevice -> getTopButtonEventJob = eventParser(device, keyboardInputHandler)
                                bottomButtonEventDevice -> getBottomButtonEventJob = eventParser(device, keyboardInputHandler)
                                else -> device.close()
                            }
                        }
                    }

                    while (isActive) {
                        delay(100)
                    }
                } finally {
                    if (deviceList != null) {
                        for (deviceBinder in deviceList) {
                            val device = IEvdevDevice.Stub.asInterface(deviceBinder)
                            device.close()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(DEBUG_TAG, "inputReaderService: ${e.message}", e)
                Log.e(logTag, "Trackpad inputReaderService failed", e)
            }
            Log.i(DEBUG_TAG, "inputReaderService stopping")
            inputReaderService?.exit()
            inputReaderService=null
            unbindInputReaderUserService()
        }
    }

    fun eventParser(eventDevice: IEvdevDevice, inputHandler: IEventCallback): Job {
        return scope.launch(Dispatchers.IO) {
          try {
                Log.d(DEBUG_TAG, "eventparser reader loop starts for ${eventDevice.name} ${eventDevice.path}")
                  while (isActive) {
                      eventDevice.events(1000, inputHandler)
                  }

                  Log.d(DEBUG_TAG, "getevent reader loop ended for ${eventDevice.name} ${eventDevice.path}")
              } catch (e: Exception) {
                  Log.e(DEBUG_TAG, "getevent coroutine FAILED: ${e.message}", e)
              }
        }
    }

    fun stop() {
        Log.d(DEBUG_TAG, "stop() called - had active job: ${getTrackpadEventJob != null}")
        getTrackpadEventJob?.cancel()
        getBackScreenEventJob?.cancel()
        getBottomButtonEventJob?.cancel()
        getTopButtonEventJob?.cancel()
        getTrackpadEventJob = null
        getBackScreenEventJob = null
        getBottomButtonEventJob = null
        getTopButtonEventJob = null
        Log.d(logTag, "Trackpad gesture detection stopped")
    }

    /**
     * Returns true if the detector is currently running (has an active getevent job).
     */
    fun isRunning(): Boolean {
        return getTrackpadEventJob != null && getTrackpadEventJob?.isActive == true
    }

    private val userHidServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder?) {
            val res = StringBuilder()
            res.append("onHidServiceConnected: ").append(componentName.className).append('\n')
            if (binder != null && binder.pingBinder()) {
                val service: IHidService = IHidService.Stub.asInterface(binder)
                try {
                    hidService = service
                    if (hidService != null) {
                        trackpadInputHandler.setHidService(service)
                        keyboardInputHandler.setHidService(service)
                        backScreenInputHandler.setHidService(service)
                    } else {
                        Logger.e("Did not get Hid Service")
                    }
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
            Logger.i("onHidServiceDisconnected: " + '\n' + componentName.className)
        }
    }

    private val userHidServiceArgs: UserServiceArgs = UserServiceArgs(
        ComponentName(
            BuildConfig.APPLICATION_ID,
            HidService::class.java.name
        )
    )
        .daemon(false)
        .processNameSuffix("service")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private fun bindHidUserService() {
        val res = StringBuilder()
        try {
            if (Shizuku.getVersion() < 10) {
                res.append("requires Shizuku API 10")
            } else {
                Shizuku.bindUserService(userHidServiceArgs, userHidServiceConnection)
            }
        } catch (tr: Throwable) {
            tr.printStackTrace()
            res.append(tr.toString())
        }
        Logger.i(res.toString().trim { it <= ' ' })
    }

    private fun unbindHidUserService() {
        val res = StringBuilder()
        try {
            if (Shizuku.getVersion() < 10) {
                res.append("requires Shizuku API 10")
            } else {
                Shizuku.unbindUserService(userHidServiceArgs, userHidServiceConnection, true)
            }
        } catch (tr: Throwable) {
            tr.printStackTrace()
            res.append(tr.toString())
        }
        Logger.i(res.toString().trim { it <= ' ' })
    }

    private val userInputReaderServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder?) {
            val res = StringBuilder()
            res.append("onInputReaderServiceConnected: ").append(componentName.className).append('\n')
            if (binder != null && binder.pingBinder()) {
                val service: IEvdevReaderService = IEvdevReaderService.Stub.asInterface(binder)
                try {
                    inputReaderService = service
                    if (inputReaderService != null) {
                        inputReaderService?.init()
                    } else {
                        Logger.e("Did not get Input Reader Service")
                    }
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
            Logger.i("onInputServiceDisconnected: " + '\n' + componentName.className)
        }
    }

    private val userInputReaderServiceArgs: UserServiceArgs = UserServiceArgs(
        ComponentName(
            BuildConfig.APPLICATION_ID,
            EvdevReaderService::class.java.name
        )
    )
        .daemon(false)
        .processNameSuffix("service")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private fun bindInputReaderUserService() {
        val res = StringBuilder()
        try {
            if (Shizuku.getVersion() < 10) {
                res.append("requires Shizuku API 10")
            } else {
                Shizuku.bindUserService(userInputReaderServiceArgs, userInputReaderServiceConnection)
            }
        } catch (tr: Throwable) {
            tr.printStackTrace()
            res.append(tr.toString())
        }
        Logger.i(res.toString().trim { it <= ' ' })
    }

    private fun unbindInputReaderUserService() {
        val res = StringBuilder()
        try {
            if (Shizuku.getVersion() < 10) {
                res.append("requires Shizuku API 10")
            } else {
                Shizuku.unbindUserService(userInputReaderServiceArgs, userInputReaderServiceConnection, true)
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
//        const val DEFAULT_KEYBOARD_EVENT_DEVICE = "/dev/input/event6" // keyboard buttons
        const val DEFAULT_TRACKPAD_EVENT_DEVICE = "/dev/input/event7" // keyboard touch
        // event8 has an unknown purpose

        const val DEFAULT_LOG_TAG = "PastieraIME"
        private const val DEBUG_TAG = "InputManager"
    }
}




