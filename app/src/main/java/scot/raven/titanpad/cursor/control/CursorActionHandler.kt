package scot.raven.titanpad.cursor.control

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import scot.raven.titanpad.core.control.CoreManager.ChannelMessage
import scot.raven.titanpad.core.control.ModeCoordinator
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.gesture.api.GestureManager
import scot.raven.titanpad.settings.domain.UsageConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import scot.raven.titanpad.accessibility.AppAccessibilityService.Companion.BROADCAST_CURSOR_ACTIVATED
import scot.raven.titanpad.accessibility.AppAccessibilityService.Companion.BROADCAST_CURSOR_ACTIVATED_EXTRA_KEY
import scot.raven.titanpad.settings.domain.ApplicationSettings
import kotlin.reflect.KProperty

/**
 * Handles key events for the standard cursor mode.
 */
class CursorActionHandler(
    private val service: AccessibilityService,
    private val cursorStateManager: CursorStateManager,
    private val gestureManager: GestureManager,
    private val settingsFlow: StateFlow<ApplicationSettings>,
    private val backgroundScope: CoroutineScope,
    private val modeCoordinator: ModeCoordinator,
) {
    private var activationKeyPressStartTime: Long = -1
    private var isActivationKeyPressed: Boolean = false
    private var wasActivated: Boolean = false
    private var currentZoomDirection: Boolean? = null
    private var activationJob: Job? = null
    private var continuousGestureJob: Job? = null
    private var movementJob: Job? = null
    private var slowScrollJob: Job? = null

    private var lastScrollTime: Long? = null

    private fun cancelActivationJob() {
        activationJob?.cancel()
        activationJob = null
    }

    private fun cancelContinuousGesture() {
        currentZoomDirection = null
        continuousGestureJob?.cancel()
        continuousGestureJob = null
        lastScrollTime = null
    }

    private fun cancelMovementJob() {
        movementJob?.cancel()
        movementJob = null
    }

    fun cleanup() {
        cancelActivationJob()
        cancelContinuousGesture()
        cancelMovementJob()
        slowScrollJob?.cancel()
    }

    fun handleKeyEvent(event: KeyEvent?, channel: Channel<ChannelMessage>): Boolean {
        cursorStateManager.updateClickable(false)
        val settings = settingsFlow.value

        try {
            if (event == null) return false

            val activateKeys = buildSet {
                add(settings.getActiveConfig().cursorActivationKey)
            }

            if (event.keyCode in activateKeys) {
                return handleActivationKey(event)
            }
            return false
        } catch (e: Exception) {
            Logger.e("Error processing cursor key event", e)
            cancelContinuousGesture()
            return false
        }
    }

    private fun handleActivationKey(event: KeyEvent): Boolean {
        cancelContinuousGesture()
        val settings = settingsFlow.value

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                cancelActivationJob()

                activationKeyPressStartTime = System.currentTimeMillis()
                isActivationKeyPressed = true
                wasActivated = false

                activationJob = backgroundScope.launch {
                    delay(settings.getActiveConfig().activationDuration)
                    if (isActivationKeyPressed) {
                        if (modeCoordinator.requestActivation(ModeCoordinator.OverlayMode.CURSOR)) {
                            cursorStateManager.toggleCursorVisibility()
                            wasActivated = cursorStateManager.isCursorVisible()

                            if (!wasActivated) {
                                modeCoordinator.deactivate(ModeCoordinator.OverlayMode.CURSOR, false)
                                gestureManager.setGestureReady(true)

                                val intent = Intent(BROADCAST_CURSOR_ACTIVATED)
                                intent.setPackage(service.packageName)
                                intent.putExtra(BROADCAST_CURSOR_ACTIVATED_EXTRA_KEY, "")
                                service.sendBroadcast(intent)
                            } else {
                                val intent = Intent(BROADCAST_CURSOR_ACTIVATED)
                                intent.setPackage(service.packageName)
                                intent.putExtra(BROADCAST_CURSOR_ACTIVATED_EXTRA_KEY, "default")
                                service.sendBroadcast(intent)
                            }
                        }
                    }
                }

                // Do not intercept if cursor not visible yet
                return cursorStateManager.isCursorVisible()
            }

            KeyEvent.ACTION_UP -> {
                isActivationKeyPressed = false
                cancelActivationJob()

                // Do not intercept if cursor just activated
                if (wasActivated) {
                    wasActivated = false
                    return false
                }

                if (cursorStateManager.isCursorVisible()) {
                    cursorStateManager.hideCursor()
                    return true
                }
                return false
            }

            else -> return false
        }
    }
}