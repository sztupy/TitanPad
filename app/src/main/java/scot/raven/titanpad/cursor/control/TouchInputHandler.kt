package scot.raven.titanpad.cursor.control

import android.util.Log
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import scot.raven.titanpad.accessibility.AppAccessibilityService
import scot.raven.titanpad.core.control.IHidService
import scot.raven.titanpad.gesture.api.GestureManager
import scot.raven.titanpad.settings.domain.ApplicationSettings

class TouchInputHandler(
    private val cursorStateManager: CursorStateManager,
    private val gestureManager: GestureManager,
    private val settingsFlow: StateFlow<ApplicationSettings>,
    private val scope: CoroutineScope
) : InputHandler {
    private var hidService: IHidService? = null
    private var touchDown = false
    private var startX = 0
    private var startY = 0
    private var centerX = 0
    private var centerY = 0
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

    fun setHidService(service: IHidService?) {
        hidService = service
    }

    override fun parseInput(line: String) {
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
                            centerX = newX
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
                            centerY = newY
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

            //hidService?.setMousePosition(deltaX.toInt(), deltaY.toInt(), 0)

            //hidService?.tapScreen(currentX, currentY*2)

            //hidService?.setJoystick(currentX - centerX, currentY - centerY)

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

            //hidService?.tapRelease()
            //hidService?.setJoystick(0,0)

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

    companion object {
        private const val DEBUG_TAG = "InputManager"
    }
}