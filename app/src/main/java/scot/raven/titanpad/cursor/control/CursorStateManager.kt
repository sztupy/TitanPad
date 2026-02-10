package scot.raven.titanpad.cursor.control

import androidx.compose.ui.geometry.Offset
import scot.raven.titanpad.core.domain.ScreenDimensions
import scot.raven.titanpad.cursor.domain.CursorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the cursor state, including position, visibility and mode.
 */
class CursorStateManager(
    private val dimensionsFlow: StateFlow<ScreenDimensions>
) {
    private val _cursorState = MutableStateFlow<CursorState?>(null)
    val cursorState: StateFlow<CursorState?> = _cursorState.asStateFlow()
    private var _lastCursorPosition: Offset? = null

    fun isCursorVisible(): Boolean = _cursorState.value != null

    fun toggleCursorVisibility() {
        if (_cursorState.value == null) {
            showCursor()
        } else {
            hideCursor()
        }
    }

    fun setCursorVisibiliy(state: Boolean) {
        if (state) {
            showCursor()
        } else {
            hideCursor()
        }
    }

    private fun showCursor() {
        val dimensions = dimensionsFlow.value
        val (centerX, centerY) = dimensions.center()

        // If manually activated in text field, still restore at last position
        val position = if (_lastCursorPosition != null) {
            _lastCursorPosition!!
        } else {
            Offset(centerX, centerY)
        }
        _lastCursorPosition = null

        val newCursor = CursorState(
            position = position,
            isVisible = true,
            inScrollMode = false
        )

        updateCursor(newCursor)
    }

    fun hideCursor() {
        updateCursor(null)
    }

    fun updatePosition(position: Offset): CursorState? {
        _cursorState.update { currentState ->
            currentState?.copy(position = position)
        }

        return _cursorState.value
    }

    fun setLastCursorPosition (pos: Offset?) {
        _lastCursorPosition = pos
    }

    fun updateClickable(clickable: Boolean) {
        _cursorState.update { currentState ->
            currentState?.copy(clickable = clickable)
        }
    }

    private fun updateCursor(cursor: CursorState?) {
        _cursorState.value = cursor
    }

    fun applyMovement(delta: Offset): Offset {
        val dimensions = dimensionsFlow.value
        val currentState = _cursorState.value ?: return Offset.Zero

        val currentPosition = currentState.position
        val newX = currentPosition.x + delta.x
        val newY = currentPosition.y + delta.y

        return dimensions.constrainToBounds(newX, newY).let { (x, y) -> Offset(x, y) }
    }
}