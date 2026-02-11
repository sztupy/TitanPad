package scot.raven.titanpad.cursor.control

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import scot.raven.titanpad.core.control.CoreManager.ChannelMessage
import scot.raven.titanpad.core.control.ModeCoordinator
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.gesture.api.GestureManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.accessibility.AppAccessibilityService.Companion.BROADCAST_CURSOR_ACTIVATED
import scot.raven.titanpad.settings.domain.ApplicationSettings
import scot.raven.titanpad.settings.domain.UsageConfig
import scot.raven.titanpad.settings.ui.SettingsActivity.Companion.CONFIG_ID_EXTRA

/**
 * Handles key events for the standard cursor mode.
 */
class CursorActivator(
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

            val activateKeys = (settings.additionalConfigs + settings.defaultConfig).map {
                it.cursorActivationKey
            }.filter { it in 0..<10000 }

            val specialKeys = (settings.additionalConfigs + settings.defaultConfig).map {
                it.cursorActivationKey
            }.filter { it > 10000 }

            if (event.keyCode in activateKeys || (event.scanCode + 10000) in specialKeys) {
                var foundSettings = (settings.additionalConfigs + settings.defaultConfig).find {
                    it.cursorActivationKey > 0 && (event.keyCode == it.cursorActivationKey || event.scanCode == it.cursorActivationKey + 10000)
                }
                if (foundSettings == null)
                    foundSettings = settings.getActiveConfig()

                return handleActivationKey(event, foundSettings, foundSettings.configId == settings.getActiveConfig().configId)
            }
            return false
        } catch (e: Exception) {
            Logger.e("Error processing cursor key event", e)
            cancelContinuousGesture()
            return false
        }
    }

    private fun handleActivationKey(event: KeyEvent, config: UsageConfig, isSame: Boolean): Boolean {
        cancelContinuousGesture()

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                cancelActivationJob()

                activationKeyPressStartTime = System.currentTimeMillis()
                isActivationKeyPressed = true
                wasActivated = false

                activationJob = backgroundScope.launch {
                    delay(config.activationDuration)
                    if (isActivationKeyPressed) {
                        Logger.d("Switching config ${config.configId}")
                        if (!isSame) {
                            Logger.d("Disabling old config")
                            modeCoordinator.requestActivation(ModeCoordinator.OverlayMode.OFF)
                            backgroundScope.launch {
                                TitanPad.getInstance().settingsRepository.setActiveKey(config.configId)
                            }
                        }

                        if (modeCoordinator.requestActivation(ModeCoordinator.OverlayMode.ON)) {
                            Logger.d("Switching config")
                            if (modeCoordinator.activeMode.value == ModeCoordinator.OverlayMode.OFF) {
                                Logger.d("Switching config OFF")
                                gestureManager.setGestureReady(true)

                                val intent = Intent(BROADCAST_CURSOR_ACTIVATED)
                                intent.setPackage(service.packageName)
                                intent.putExtra(CONFIG_ID_EXTRA, "")
                                service.sendBroadcast(intent)
                            } else {
                                Logger.d("Switching config ON")
                                val intent = Intent(BROADCAST_CURSOR_ACTIVATED)
                                intent.setPackage(service.packageName)
                                intent.putExtra(CONFIG_ID_EXTRA, config.configId)
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