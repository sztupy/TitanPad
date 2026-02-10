package scot.raven.titanpad.accessibility

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.core.constants.ApplicationConstants
import scot.raven.titanpad.core.control.CoreManager
import scot.raven.titanpad.core.control.ModeCoordinator
import scot.raven.titanpad.core.control.OverlayManager
import scot.raven.titanpad.core.domain.OrientationHandler
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.settings.domain.AppListType
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import scot.raven.titanpad.settings.ui.SettingsActivity.Companion.CONFIG_ID_EXTRA

/**
 * Receives key events, displays overlays, and performs gestures.
 */
@SuppressLint("AccessibilityPolicy")
class AppAccessibilityService : AccessibilityService(), LifecycleOwner,
    SavedStateRegistryOwner {
    private var windowManager: WindowManager? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var serviceJob: Job
    private lateinit var backgroundScope: CoroutineScope
    private lateinit var mainScope: CoroutineScope

    private val coroutineExceptionHandler =
        CoroutineExceptionHandler { _, exception ->
            Logger.e("Coroutine error in shizuku", exception)
        }

    private lateinit var coreManager: CoreManager
    private lateinit var overlayManager: OverlayManager
    private lateinit var orientationHandler: OrientationHandler
    private lateinit var modeCoordinator: ModeCoordinator

    private var lastOverlayType: ModeCoordinator.OverlayMode = ModeCoordinator.OverlayMode.OFF

    private val _keysPressed = MutableStateFlow(0)
    private val keysPressed: StateFlow<Int> = _keysPressed.asStateFlow()
    private val _layoutApplied = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1
    )

    val layoutApplied = _layoutApplied.asSharedFlow()

    private var lastKeyboardState = false
    private var lastLockScreenState = false
    private var lastApp: String? = null
    private var lastAppState = false
    private var lastStateChanged = false
    private var autoHideJob: Job? = null
    private val keyguardManager by lazy { getSystemService(KEYGUARD_SERVICE) as KeyguardManager }
    private val imm by lazy { getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager }
    private val windowHeightMethod by lazy { InputMethodManager::class.java.getMethod("getInputMethodWindowVisibleHeight")}

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var configId = intent.getStringExtra(CONFIG_ID_EXTRA)
            if (configId==null)
                configId = "default"

            when (intent.action) {
                ACTION_ACTIVATE_CURSOR -> {
                    backgroundScope.launch {
                        coreManager.activateCursorMode(true, configId)
                    }
                }
                ACTION_DEACTIVATE_CURSOR -> {
                    backgroundScope.launch {
                        coreManager.deactivateCursorMode(true)
                    }
                }
            }
        }
    }

    companion object {
        private var instance: AppAccessibilityService? = null

        fun getInstance(): AppAccessibilityService? {
            return instance
        }

        const val ACTION_ACTIVATE_CURSOR = "scot.raven.titanpad.ACTION_ACTIVATE_CURSOR"
        const val ACTION_DEACTIVATE_CURSOR = "scot.raven.titanpad.ACTION_DEACTIVATE_CURSOR"
        const val BROADCAST_CURSOR_ACTIVATED = "scot.raven.titanpad.BROADCAST_CURSOR_ACTIVATED"

        fun activateStandardCursor(context: Context, configId: String) {
            val intent = Intent(ACTION_ACTIVATE_CURSOR)
            intent.setPackage(context.packageName)
            intent.putExtra(CONFIG_ID_EXTRA, configId)
            context.sendBroadcast(intent, null)
        }

        fun deactivateStandardCursor(context: Context, configId: String) {
            val intent = Intent(ACTION_DEACTIVATE_CURSOR)
            intent.setPackage(context.packageName)
            intent.putExtra(CONFIG_ID_EXTRA, configId)
            context.sendBroadcast(intent, null)
        }
    }

    private fun _isNodeClickable(node: AccessibilityNodeInfo?, pos: Offset?): Boolean {
        if (node == null || pos == null) return false

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val x = pos.x.toInt()
        val y = pos.y.toInt()

        if (!bounds.contains(x, y)) {
            return false
        }

        val isClickable = node.isClickable ||
                node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
        if (isClickable) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (_isNodeClickable(child, pos)) {
                return true
            }
        }

        return false
    }

    fun isNodeClickable(pos: Offset?): Boolean {
        return _isNodeClickable(rootInActiveWindow, pos)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        try {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

            modeCoordinator = ModeCoordinator()

            serviceJob = SupervisorJob()
            backgroundScope = CoroutineScope(Dispatchers.Default + serviceJob + coroutineExceptionHandler)
            mainScope = CoroutineScope(Dispatchers.Main + serviceJob + coroutineExceptionHandler)

            val settingsFlow = TitanPad.getInstance().getSettingsFlow()

            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            orientationHandler = OrientationHandler(context = this, settingsFlow = settingsFlow)

            coreManager = CoreManager(
                service = this,
                settingsFlow = settingsFlow,
                orientationHandler = orientationHandler,
                backgroundScope = backgroundScope,
                keysPressed = _keysPressed,
                layoutApplied = layoutApplied,
                modeCoordinator = modeCoordinator
            )
            coreManager.initialize()

            overlayManager = OverlayManager(
                context = this,
                backgroundScope = backgroundScope,
                mainScope = mainScope,
                windowManager = windowManager!!,
                settingsFlow = settingsFlow,
                orientationHandler = orientationHandler,
                coreManager = coreManager,
                lifecycleOwner = this,
                savedStateRegistryOwner = this,
                keysPressed = keysPressed,
                _layoutApplied = _layoutApplied
            )
            overlayManager.initialize()

            val filter = IntentFilter().apply {
                addAction(ACTION_ACTIVATE_CURSOR)
                addAction(ACTION_DEACTIVATE_CURSOR)
            }
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)

            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            Logger.i("Overlay accessibility service connected")
        } catch (e: Exception) {
            Logger.e("Error initializing service", e)
            if (!::serviceJob.isInitialized) {
                serviceJob = SupervisorJob()
            }
        }
    }

    private fun autoHideCursor() {
        val currentOverlay = modeCoordinator.activeMode.value
        val cursorOff = currentOverlay == ModeCoordinator.OverlayMode.OFF
        val cursorAlreadyHidden = currentOverlay == ModeCoordinator.OverlayMode.AUTOHIDDEN
        if (cursorOff || cursorAlreadyHidden)
            return

        lastOverlayType = currentOverlay
        if (lastOverlayType == ModeCoordinator.OverlayMode.CURSOR) {
            coreManager.cursorStateManager.cursorState.value?.let { cursor ->
                coreManager.cursorStateManager.setLastCursorPosition(Offset(cursor.position.x, cursor.position.y))
            }
        }

        Logger.d("Hiding cursor overlay")
        forceHideAllOverlays(true)
    }

    private fun attemptCursorRestore() {
        val currentOverlay = modeCoordinator.activeMode.value
        if (currentOverlay != ModeCoordinator.OverlayMode.AUTOHIDDEN)
            return

        Logger.d("Restoring cursor overlay")
        val settings = TitanPad.getInstance().getSettingsFlow().value
        val cursorMapped = settings.getActiveConfig().cursorActivationKey != ApplicationConstants.OVERLAY_DISABLED
        (lastOverlayType == ModeCoordinator.OverlayMode.CURSOR) && !cursorMapped

        // If no previous overlay type, default to any mapped cursor
        if (lastOverlayType == ModeCoordinator.OverlayMode.OFF) {
            lastOverlayType = when {
                cursorMapped -> ModeCoordinator.OverlayMode.CURSOR
                else -> ModeCoordinator.OverlayMode.OFF
            }
        }

        when (lastOverlayType) {
            ModeCoordinator.OverlayMode.CURSOR -> {
                coreManager.activateCursorMode(configId = settings.getActiveConfig().configId)
            }

            else -> {}
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val settings = TitanPad.getInstance().getSettingsFlow().value

        event.let {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOWS_CHANGED, AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    if (settings.getActiveConfig().hideOnKeyboardOpen) {
                        checkKeyboardVisibility()
                    }

                    if (settings.getActiveConfig().hideOnLockScreen) {
                        checkLockScreenVisibility()
                    }

                    checkAppVisibility()
                }

                else -> {}
            }
        }

        if (lastStateChanged) {
            onAutoHideConditionChanged(lastKeyboardState || lastLockScreenState || lastAppState)
        }
        lastStateChanged = false
    }

    private fun checkKeyboardVisibility() {
        try {
            val isKeyboardVisible = isImeWindowPresent() ||
                    windowHeightMethod.invoke(imm) as Int > 0
            if (isKeyboardVisible != lastKeyboardState) {
                Logger.d("Autohide by keyboard: $lastKeyboardState")
                lastKeyboardState = isKeyboardVisible
                lastStateChanged = true
            }
        } catch (e: Exception) {
            Logger.e("Error checking keyboard visibility", e)
        }
    }

    private fun isImeWindowPresent(): Boolean {
        for (window in windows) {
            if (window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                return true
            }
        }
        return false
    }

    private fun checkAppVisibility() {
        try {
            val (app, appState) = shouldHideInCurrentApp()
            if (app != null && app != lastApp) {
                Logger.d("Autohide by current app: $appState")
                lastApp = app
                lastAppState = appState
                lastStateChanged = true
            }
        } catch (e: Exception) {
            Logger.e("Error checking app visibility", e)
        }
    }

    private fun shouldHideInCurrentApp(): Pair<String?, Boolean> {
        val settings = TitanPad.getInstance().getSettingsFlow().value
        val appWindow = windows
            ?.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.root != null }
        val appName = appWindow?.root?.packageName?.toString()

        if (settings.getActiveConfig().autoHideApps.isEmpty()) return Pair(appName, settings.getActiveConfig().applicationListType == AppListType.ALLOW_LIST)

        if (appWindow != null && appName in settings.getActiveConfig().autoHideApps) {
            return Pair(appName, settings.getActiveConfig().applicationListType == AppListType.DENY_LIST)
        }

        return Pair(appName, settings.getActiveConfig().applicationListType == AppListType.ALLOW_LIST)
    }

    fun showClickableInCurrentApp(): Boolean {
        val settings = TitanPad.getInstance().getSettingsFlow().value
        val showByDefault = settings.getActiveConfig().clickableListType == AppListType.DENY_LIST

        if (settings.getActiveConfig().clickableApps.isEmpty()) return showByDefault

        for (window in windows) {
            val pkg = window.root?.packageName?.toString()
            if (pkg != null && pkg in settings.getActiveConfig().clickableApps) {
                return !showByDefault
            }
        }
        return showByDefault
    }

    private fun checkLockScreenVisibility() {
        try {
            val isLockScreenVisible = keyguardManager.isKeyguardLocked
            if (isLockScreenVisible != lastLockScreenState) {
                Logger.d("Autohide by lockscreen: $lastLockScreenState")
                lastLockScreenState = isLockScreenVisible
                lastStateChanged = true
            }
        } catch (e: Exception) {
            Logger.e("Error checking lock screen visibility", e)
        }
    }

    private fun onAutoHideConditionChanged(shouldHide: Boolean) {
        autoHideJob?.cancel()
        autoHideJob = mainScope.launch {
            delay(100L)
            if (shouldHide) {
                autoHideCursor()
            } else {
                attemptCursorRestore()
            }
        }
    }

    // Required by AccessibilityService interface
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        return try {
            coreManager.handleKeyEvent(event)
        } catch (e: Exception) {
            Logger.e("Error processing key event", e)
            false
        }
    }

    fun forceHideAllOverlays(fromAutoHide: Boolean) {
        coreManager.forceHideAllOverlays(fromAutoHide)
        overlayManager.updateOverlayUI()
    }

    override fun onDestroy() {
        instance = null
        try {
            if (::backgroundScope.isInitialized) {
                backgroundScope.cancel("AppAccessibilityService destroyed")
            }

            if (::mainScope.isInitialized) {
                mainScope.cancel("AppAccessibilityService destroyed")
            }

            if (::coreManager.isInitialized) {
                coreManager.cleanup()
            }

            if (::overlayManager.isInitialized) {
                overlayManager.cleanup()
            }

            if (::orientationHandler.isInitialized) {
                orientationHandler.cleanup()
            }

            try {
                unregisterReceiver(receiver)
            } catch (e: Exception) {
                Logger.e("Error unregistering receiver", e)
            }

            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

            Logger.i("Overlay accessibility shizuku destroyed")
        } catch (e: Exception) {
            Logger.e("Error during shizuku cleanup", e)
        } finally {
            super.onDestroy()
        }
    }
}