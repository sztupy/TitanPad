package scot.raven.titanpad.core.control

import scot.raven.titanpad.core.logs.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ensures only one overlay is active at a time.
 */
class ModeCoordinator {
    enum class OverlayMode {
        OFF,
        HIDDEN,
        ON
    }

    private val _activeMode = MutableStateFlow(OverlayMode.OFF)
    val activeMode: StateFlow<OverlayMode> = _activeMode.asStateFlow()

    fun requestActivation(mode: OverlayMode): Boolean {
        val currentMode = _activeMode.value
        val newMode = if (currentMode == mode) OverlayMode.OFF else mode
        _activeMode.value = newMode
        Logger.d("Overlay mode changed: $currentMode -> $newMode")
        return true
    }

    fun deactivate(mode: OverlayMode, fromAutoHide: Boolean) {
        if (_activeMode.value == mode) {
            _activeMode.value = if (fromAutoHide) OverlayMode.HIDDEN else OverlayMode.OFF
            Logger.d("Overlay mode deactivated: $mode")
        }
    }
}