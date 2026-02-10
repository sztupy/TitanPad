package scot.raven.titanpad.core.control

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.core.domain.OrientationHandler
import scot.raven.titanpad.core.domain.ScreenDimensions
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.core.notification.NotificationManager
import scot.raven.titanpad.cursor.control.CursorActivator
import scot.raven.titanpad.cursor.control.CursorStateManager
import scot.raven.titanpad.cursor.control.InputManager
import scot.raven.titanpad.gesture.api.GestureManager
import scot.raven.titanpad.gesture.standard.DefaultGestureStrategy
import scot.raven.titanpad.gesture.ui.GesturePath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import scot.raven.titanpad.accessibility.AppAccessibilityService.Companion.BROADCAST_CURSOR_ACTIVATED
import scot.raven.titanpad.cursor.domain.InputType
import scot.raven.titanpad.settings.domain.ApplicationSettings
import scot.raven.titanpad.settings.ui.SettingsActivity.Companion.CONFIG_ID_EXTRA

/**
 * Manages standard cursor modes.
 */
class CoreManager(
    private val service: AccessibilityService,
    private val settingsFlow: StateFlow<ApplicationSettings>,
    private val modeCoordinator: ModeCoordinator,
    private val orientationHandler: OrientationHandler,
    private val backgroundScope: CoroutineScope,
    private val keysPressed: MutableStateFlow<Int>,
    private val layoutApplied: SharedFlow<Unit>
) {
    private lateinit var gestureManager: GestureManager
    lateinit var cursorStateManager: CursorStateManager
    private lateinit var cursorActivator: CursorActivator
    private lateinit var notificationManager: NotificationManager
    private lateinit var inputManager: InputManager

    private val screenDimensionsFlow = orientationHandler.screenDimensions

    data class ChannelMessage(
        val event: KeyEvent,
        val handler: suspend (KeyEvent) -> Unit
    )

    private val channel = Channel<ChannelMessage>(Channel.UNLIMITED)
    init {
        backgroundScope.launch(Dispatchers.Default) {
            for (msg in channel) {
                processMessage(msg)
            }
        }
    }
    private suspend fun processMessage(msg: ChannelMessage) = coroutineScope {
        val settings = settingsFlow.value

        if (msg.event.action == KeyEvent.ACTION_DOWN) {
            keysPressed.value += 1
            if (keysPressed.value == 1 && settings.getActiveConfig().disableTouchscreen) {
                layoutApplied.first()
            }
        }

        msg.handler(msg.event)

        if (msg.event.action == KeyEvent.ACTION_UP) {
            keysPressed.value -= 1
            if (keysPressed.value == 0 && settings.getActiveConfig().disableTouchscreen) {
                layoutApplied.first()
            }
        }
    }

    fun initialize() {
        try {
            Logger.i("Initializing CoreManager")

            notificationManager = NotificationManager(service)

            val defaultStrategy = DefaultGestureStrategy(service)

            gestureManager = GestureManager(
                defaultStrategy,
                settingsFlow,
                backgroundScope
            )

            // Cursor components
            cursorStateManager = CursorStateManager(
                screenDimensionsFlow
            )
            cursorActivator = CursorActivator(
                service,
                cursorStateManager,
                gestureManager,
                settingsFlow,
                backgroundScope,
                modeCoordinator,
            )

            // Listen for orientation changes
            orientationHandler.screenDimensions
                .onEach { newDimensions ->
                    onScreenDimensionsChanged(newDimensions)
                }
                .launchIn(backgroundScope)

            // Listen for mode changes to update notification
            modeCoordinator.activeMode
                .onEach { mode ->
                    updateNotification(mode)
                    updateCursorMode()
                }
                .launchIn(backgroundScope)

            settingsFlow
                .onEach {
                    updateNotification(modeCoordinator.activeMode.value)
                    updateCursorMode()
                }
                .launchIn(backgroundScope)

            inputManager = InputManager(
                isEnabled = { true },
                scope = backgroundScope,
                cursorStateManager = cursorStateManager,
                gestureManager = gestureManager,
                settingsFlow = settingsFlow,
                modeCoordinator = modeCoordinator
            )

            TitanPad.getInstance().setTrackpadActionHandler(inputManager)
            inputManager.start()

            Logger.i("CoreManager initialization complete")
        } catch (e: Exception) {
            Logger.e("Error initializing CoreManager", e)
            throw e
        }
    }

    private fun updateCursorMode() {
        val currentMode = modeCoordinator.activeMode.value
        val currentSettings = settingsFlow.value.getActiveConfig()

        if (currentMode != ModeCoordinator.OverlayMode.ON) {
            cursorStateManager.hideCursor()
            return
        }

        val combinedInputTypes = setOf(
            currentSettings.touchPadMainInputType,
            currentSettings.backScreenInputType
        ) + if (currentSettings.touchpadSplitInput) currentSettings.touchPadLeftInputType else currentSettings.touchPadMainInputType

        val shouldDisplayCursor = combinedInputTypes.contains(InputType.SOFTWARE_MOUSE)
        if (!shouldDisplayCursor) {
            cursorStateManager.hideCursor()
        }

        if (shouldDisplayCursor && !cursorStateManager.isCursorVisible())
            cursorStateManager.setCursorVisibiliy(true)
    }

    private fun onScreenDimensionsChanged(newDimensions: ScreenDimensions) {
        try {
            if (cursorStateManager.isCursorVisible()) {
                val (centerX, centerY) = newDimensions.center()
                cursorStateManager.updatePosition(Offset(centerX, centerY))
            }
        } catch (e: Exception) {
            Logger.e("Error handling screen dimensions change", e)
        }
    }

    fun activateCursorMode(keymapToggle: Boolean = false, configId: String): Boolean {
        try {
            Logger.d("Activating cursor mode")

            val settings = settingsFlow.value

            if (settings.getActiveConfig().configId != configId && modeCoordinator.activeMode.value != ModeCoordinator.OverlayMode.OFF) {
                modeCoordinator.requestActivation(ModeCoordinator.OverlayMode.OFF)
            }

            backgroundScope.launch {
                TitanPad.getInstance().settingsRepository.setActiveKey(configId)
            }

            if ((!cursorStateManager.isCursorVisible() || keymapToggle) && modeCoordinator.requestActivation(
                    ModeCoordinator.OverlayMode.ON
                )) {
                cursorStateManager.setCursorVisibiliy(modeCoordinator.activeMode.value == ModeCoordinator.OverlayMode.ON)

                val intent = Intent(BROADCAST_CURSOR_ACTIVATED)
                intent.setPackage(service.packageName)
                intent.putExtra(CONFIG_ID_EXTRA, configId)
                service.sendBroadcast(intent)

                return cursorStateManager.isCursorVisible()
            }
            return false
        } catch (e: Exception) {
            Logger.e("Error activating cursor mode", e)
            return false
        }
    }

    fun deactivateCursorMode(keymapToggle: Boolean = false) : Boolean {
        try {
            Logger.d("Deactivating cursor mode")
            if ((cursorStateManager.isCursorVisible() || keymapToggle) && modeCoordinator.requestActivation(
                    ModeCoordinator.OverlayMode.OFF
                )) {
                cursorStateManager.setCursorVisibiliy(false)

                val intent = Intent(BROADCAST_CURSOR_ACTIVATED)
                intent.setPackage(service.packageName)
                intent.putExtra(CONFIG_ID_EXTRA, "")
                service.sendBroadcast(intent)

                return !cursorStateManager.isCursorVisible()
            }
            return false
        } catch (e: Exception) {
            Logger.e("Error activating cursor mode", e)
            return false
        }
    }

    fun handleKeyEvent(event: KeyEvent?): Boolean {
        Logger.d("Key event: $event")
        val settings = settingsFlow.value

        try {
            val eventHandled = cursorActivator.handleKeyEvent(event, channel)

            if (settings.getActiveConfig().allowPassthrough) {
                Logger.d("Allowing key event to pass through")
            }

            return !settings.getActiveConfig().allowPassthrough && eventHandled
        } catch (e: Exception) {
            Logger.e("Error processing key event", e)
            return false
        }
    }

    private fun updateNotification(mode: ModeCoordinator.OverlayMode) {
        val settings = settingsFlow.value

        try {
            if (settings.getActiveConfig().showNotification) {
                when (mode) {
                    ModeCoordinator.OverlayMode.OFF -> {
                        notificationManager.hideNotification()
                    }

                    else -> {
                        notificationManager.showNotification(mode)
                    }
                }
            } else {
                notificationManager.hideNotification()
            }
        } catch (e: Exception) {
            Logger.e("Error updating notification", e)
        }
    }

    // Invoked when setting activation key
    fun forceHideAllOverlays(fromAutoHide: Boolean) {
        Logger.d("Force hiding all overlays")

        try {
            if (cursorStateManager.isCursorVisible()) {
                cursorStateManager.hideCursor()
            }

            modeCoordinator.deactivate(ModeCoordinator.OverlayMode.ON, fromAutoHide)

            cursorActivator.cleanup()
        } catch (e: Exception) {
            Logger.e("Error force hiding overlays", e)
        }
    }

    fun updateGestureVisualization(showGestures: Boolean) {
        gestureManager.updateGestureVisibility(showGestures)
    }

    fun getGesturePaths(): StateFlow<List<GesturePath>> {
        return gestureManager.gesturePaths
    }

    fun cleanup() {
        cursorActivator.cleanup()
        gestureManager.cleanup()
    }
}