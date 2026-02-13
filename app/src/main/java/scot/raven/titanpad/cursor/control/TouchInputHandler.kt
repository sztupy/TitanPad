package scot.raven.titanpad.cursor.control

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import scot.raven.titanpad.accessibility.AppAccessibilityService
import scot.raven.titanpad.core.control.IHidService
import scot.raven.titanpad.core.control.ModeCoordinator
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.core.util.BoundingBoxUtil
import scot.raven.titanpad.cursor.domain.InputType
import scot.raven.titanpad.gesture.api.GestureManager
import scot.raven.titanpad.settings.domain.ApplicationSettings
import scot.raven.titanpad.settings.domain.ScrollConfig
import scot.raven.titanpad.settings.domain.UsageConfig
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sign

class TouchInputHandler(
    private val cursorStateManager: CursorStateManager,
    private val gestureManager: GestureManager,
    private val settingsFlow: StateFlow<ApplicationSettings>,
    private val scope: CoroutineScope,
    private val modeCoordinator: ModeCoordinator,
    private val backScreenMode: Boolean
) : InputHandler {
    private var hidService: IHidService? = null
    private var touchDown = false
    private var lastPositionX = 0
    private var lastPositionY = 0
    private var startPositionX = 0
    private var startPositionY = 0
    private var currentX = 0
    private var currentY = 0
    private var width = 0
    private var height = 0
    private var startPosSet = false
    private var startTime: Long = 0
    private var clickCount: Long = 0
    private var endTime: Long = 0
    private var numFingers = 0
    private var scrollHasStarted = false
    private var scrollTouchLocation = 0

    fun setHidService(service: IHidService?) {
        hidService = service
    }

    override fun parseInput(line: String) {
        if (modeCoordinator.activeMode.value != ModeCoordinator.OverlayMode.ON)
            return

        when {
            line.contains("BTN_TOUCH") && line.contains("DOWN") -> {
                touchDown = true
                startPosSet = false
                scrollHasStarted = false
                val currentTime = System.nanoTime()
                val elapsedMs = (currentTime - startTime) / 1_000_000.0
                if (elapsedMs > DOUBLE_TAP_MAX_LENGTH)
                    clickCount = 0

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
                            lastPositionX = newX
                            startPositionX = newX
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
                            lastPositionY = newY
                            startPositionY = newY
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
                val settings = settingsFlow.value.getActiveConfig()
                if (touchDown && !startPosSet) {
                    numFingers = if (width <= settings.twoFingerSensitivity) 1 else 2
                    startPosSet = true
                }

                var inputType : InputType = if (backScreenMode) settings.backScreenInputType else settings.touchPadMainInputType
                var scrollConfig = if (backScreenMode) settings.scrollSettings[3] else settings.scrollSettings[0]
                scrollTouchLocation = 0

                if (settings.touchpadSplitInput && !backScreenMode) {
                    if (startPositionX < TRACKPAD_WIDTH.toFloat() * settings.touchpadSplitPosition.toFloat() / 100f) {
                        inputType = settings.touchPadLeftInputType
                        scrollConfig = settings.scrollSettings[1]
                        scrollTouchLocation = 1
                    }
                }

                if (settings.touchpadSplitRightInput && !backScreenMode) {
                    if (startPositionX > TRACKPAD_WIDTH.toFloat() * settings.touchpadSplitRightPosition.toFloat() / 100f) {
                        inputType = settings.touchPadRightInputType
                        scrollConfig = settings.scrollSettings[2]
                        scrollTouchLocation = 2
                    }
                }

                if (settings.touchpadDisableTopRow && !backScreenMode && startPositionY <= TRACKPAD_HEIGHT/4) {
                    // Do nothing
                } else {
                    detectGesture(settings, scrollConfig, inputType)
                }
            }
        }
    }


    private fun detectGesture(settings: UsageConfig, scrollConfig: ScrollConfig, inputType: InputType) {
        val dragEnabled = touchDown &&
                (settings.mouseTwoFingerToHold && numFingers > 1) ||
                (settings.mouseDoubleTapToHold && clickCount >= 1)

        if (touchDown && startPosSet) {
            val deltaX = (currentX - lastPositionX + 0.0f) * if (backScreenMode) -1 else 1 // invert input on back screen
            val deltaY = (currentY - lastPositionY + 0.0f)

            if ((startPositionX-currentX).absoluteValue + (startPositionY-currentY).absoluteValue >= (10-scrollConfig.touchSensitivity) * (10-scrollConfig.touchSensitivity))
                scrollHasStarted = true

            if (inputType == InputType.SOFTWARE_MOUSE) {
                val multiplier = settings.softwareMouseSensitivity.toFloat() / if (settings.softwareMouseExponential) 4f else 6f
                val deltaWithSensitivityX = if (settings.softwareMouseExponential) (deltaX.absoluteValue / 4f).pow(multiplier + 1) * deltaX.sign else deltaX * multiplier
                val deltaWithSensitivityY = if (settings.softwareMouseExponential) (deltaY.absoluteValue / 4f).pow(multiplier + 1) * deltaY.sign else deltaY * multiplier

                val newPosition = cursorStateManager.applyMovement(Offset(deltaWithSensitivityX, deltaWithSensitivityY))
                cursorStateManager.updatePosition(newPosition)

                if (dragEnabled) {
                    if (gestureManager.getGestureReady()) {
                        if (cursorStateManager.cursorState.value != null) {
                            val value = cursorStateManager.cursorState.value!!
                            val position = value.position
                            val toX = position.x
                            val toY = position.y

                            scope.launch {
                                gestureManager.moveTo(
                                    toX,
                                    toY
                                )
                            }
                        }
                    }
                }
            } else if (inputType == InputType.HARDWARE_MOUSE) {
                hidService?.setMousePosition(
                    deltaX.toInt(),
                    deltaY.toInt(),
                    if (dragEnabled) 1 else 0,
                    0,
                    0,
                    0
                )
            } else if (inputType == InputType.HARDWARE_WHEEL) {
                hidService?.setMousePosition(
                    0,
                    0,
                    if (dragEnabled) 1 else 0,
                    0,
                    deltaY.toInt(),
                    deltaX.toInt(),
                )
            } else if (inputType == InputType.HARDWARE_SCROLL || inputType == InputType.SOFTWARE_SCROLL) {
                var touchX : Float
                var touchY : Float
                if (scrollHasStarted) {
                    if (!backScreenMode) {
                        when (scrollTouchLocation) {
                            1 -> {
                                touchX =
                                    ((if (scrollConfig.scrollOnlyVertically) startPositionX else currentX).toFloat() / (settings.touchpadSplitPosition.toFloat() / 100f))
                                touchY = currentY.toFloat() * 2
                            }
                            2 -> {
                                val leftSide =
                                    TRACKPAD_WIDTH.toFloat() * settings.touchpadSplitRightPosition.toFloat() / 100f
                                touchX =
                                    ((if (scrollConfig.scrollOnlyVertically) startPositionX - leftSide else currentX - leftSide) / ((100 - settings.touchpadSplitRightPosition.toFloat()) / 100f))
                                touchY = currentY.toFloat() * 2
                            }
                            else -> {
                                touchX =
                                    if (scrollConfig.scrollOnlyVertically) startPositionX.toFloat() else currentX.toFloat()
                                touchY = currentY.toFloat() * 2
                            }
                        }
                    } else {
                        touchX = ((BACK_SCREEN_WIDTH-(if (scrollConfig.scrollOnlyVertically) startPositionX else currentX)).toFloat() * (TRACKPAD_WIDTH.toFloat() / BACK_SCREEN_WIDTH.toFloat()))
                        touchY = (currentY.toFloat() * (TRACKPAD_HEIGHT.toFloat() / BACK_SCREEN_HEIGHT.toFloat())) * 2
                    }

                    val box = BoundingBoxUtil.coerceInto(SCREEN_WIDTH.toFloat(), SCREEN_HEIGHT.toFloat(), scrollConfig.leftCropRegion.toFloat(), scrollConfig.topCropRegion.toFloat(), scrollConfig.rightCropRegion.toFloat(),scrollConfig.bottomCropRegion.toFloat(), touchX, touchY)

                    if (inputType == InputType.HARDWARE_SCROLL) {
                        hidService?.tapScreen(box.x.toInt(), box.y.toInt())
                    } else {
                        scope.launch {
                            gestureManager.moveTo(box.x, box.y)
                        }
                    }
                }
            } else if (inputType == InputType.HARDWARE_JOYSTICK) {
                if (backScreenMode) {
                    hidService?.setJoystick(-currentX + startPositionX, currentY - startPositionY)
                } else {
                    hidService?.setJoystick(currentX - startPositionX, currentY - startPositionY)
                }
            }

            lastPositionX = currentX
            lastPositionY = currentY
            if (width >= settings.twoFingerSensitivity+1) {
                numFingers = 2
            }
        }

        if (!touchDown && !startPosSet) {
            val durationMs = (endTime - startTime) / 1_000_000.0
            val isClick = settings.mouseTapToClick && durationMs < settings.mouseTapMaxDuration

            if (isClick) {
                clickCount += 1
                Logger.d("Click counter: $clickCount")
            }

            if (inputType == InputType.SOFTWARE_MOUSE) {
                val service = AppAccessibilityService.getInstance()
                val clickable =
                    service?.isNodeClickable(cursorStateManager.cursorState.value?.position) == true && service.showClickableInCurrentApp()
                cursorStateManager.updateClickable(clickable)

                if (cursorStateManager.cursorState.value != null) {
                    val value = cursorStateManager.cursorState.value!!
                    val position = value.position
                    val oldFingers = numFingers

                    val fromX = position.x
                    val fromY = position.y

                    scope.launch {
                        if (oldFingers <= 1 || !settings.mouseTwoFingerToHold) {
                            if (isClick) {
                                gestureManager.moveTo(fromX, fromY)
                                delay(TAP_CLICK_LENGTH)
                                if (!dragEnabled) {
                                    gestureManager.endTap()
                                }
                            } else {
                                gestureManager.endTap()
                            }
                        } else {
                            gestureManager.endTap()
                        }
                    }
                }
            } else if (inputType == InputType.HARDWARE_MOUSE) {
                if (isClick) {
                    scope.launch {
                        hidService?.setMousePosition(0, 0, 1, 0, 0, 0)
                        delay(TAP_CLICK_LENGTH)
                        if (!dragEnabled) {
                            hidService?.setMousePosition(0, 0, 0, 1, 0, 0)
                        }
                    }
                } else {
                    hidService?.setMousePosition(0, 0, 0, 1, 0, 0)
                }
            } else if (inputType == InputType.HARDWARE_WHEEL) {
                // do nothing
            } else if (inputType == InputType.HARDWARE_SCROLL) {
                if (scrollHasStarted) {
                    hidService?.tapRelease()
                }
            } else if (inputType == InputType.SOFTWARE_SCROLL) {
                if (scrollHasStarted) {
                    scope.launch {
                        gestureManager.endTap()
                    }
                }
            } else if (inputType == InputType.HARDWARE_JOYSTICK) {
                hidService?.setJoystick(0,0)
            }
            numFingers = 0
        }
    }

    companion object {
        private const val TAP_CLICK_LENGTH = 50L
        private const val DOUBLE_TAP_MAX_LENGTH = 300L
        private const val TRACKPAD_WIDTH = 1440
        private const val TRACKPAD_HEIGHT = 720
        private const val SCREEN_WIDTH = 1440
        private const val SCREEN_HEIGHT = 1440
        private const val BACK_SCREEN_WIDTH = 410
        private const val BACK_SCREEN_HEIGHT = 502
    }
}