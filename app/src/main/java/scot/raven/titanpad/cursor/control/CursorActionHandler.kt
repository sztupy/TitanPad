package scot.raven.titanpad.cursor.control

import android.view.KeyEvent
import scot.raven.titanpad.core.control.CoreManager.ChannelMessage
import scot.raven.titanpad.core.control.ModeCoordinator
import scot.raven.titanpad.core.domain.ScrollDirection
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.cursor.domain.CursorDirection
import scot.raven.titanpad.gesture.api.GestureManager
import scot.raven.titanpad.settings.domain.OverlaySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Handles key events for the standard cursor mode.
 */
class CursorActionHandler(
    private val cursorStateManager: CursorStateManager,
    private val gestureManager: GestureManager,
    private val settingsFlow: StateFlow<OverlaySettings>,
    private val backgroundScope: CoroutineScope,
    private val modeCoordinator: ModeCoordinator,
) {
    private var activationKeyPressStartTime: Long = -1
    private var isActivationKeyPressed: Boolean = false
    private var wasActivated: Boolean = false
    private var currentScrollDirection: ScrollDirection? = null
    private var currentZoomDirection: Boolean? = null
    private var activationJob: Job? = null
    private var continuousGestureJob: Job? = null
    private var movementJob: Job? = null
    private var slowScrollJob: Job? = null

    private val activeDirections = mutableSetOf<CursorDirection>()
    private var lastScrollTime: Long? = null

    private fun cancelActivationJob() {
        activationJob?.cancel()
        activationJob = null
    }

    private fun cancelContinuousGesture() {
        currentScrollDirection = null
        currentZoomDirection = null
        continuousGestureJob?.cancel()
        continuousGestureJob = null
        lastScrollTime = null
    }

    private fun cancelMovementJob() {
        movementJob?.cancel()
        movementJob = null
        activeDirections.clear()
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
                add(settings.cursorActivationKey)
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
                    delay(settings.activationDuration)
                    if (isActivationKeyPressed) {
                        if (modeCoordinator.requestActivation(ModeCoordinator.OverlayMode.CURSOR)) {
                            cursorStateManager.toggleCursorVisibility()
                            wasActivated = cursorStateManager.isCursorVisible()

                            if (!wasActivated) {
                                modeCoordinator.deactivate(ModeCoordinator.OverlayMode.CURSOR, false)
                                gestureManager.setGestureReady(true)
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