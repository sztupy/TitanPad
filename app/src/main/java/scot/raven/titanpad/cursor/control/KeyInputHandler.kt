package scot.raven.titanpad.cursor.control

import kotlinx.coroutines.flow.StateFlow
import scot.raven.titanpad.core.control.IHidService
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.cursor.domain.FuncButtonMap
import scot.raven.titanpad.settings.domain.ApplicationSettings

class KeyInputHandler(val settingsFlow: StateFlow<ApplicationSettings>) : InputHandler {
    private var hidService: IHidService? = null

    fun setHidService(service: IHidService?) {
        hidService = service
    }

    override fun parseInput(line: String) {
        when {
            line.contains("EV_KEY") && line.contains("DOWN") -> {
                val settings = settingsFlow.value
                val parts = line.trim().split(Regex("\\s+"))
                when(parts[1]) {
                    "00f9" -> {
                        Logger.d("Func1 key down")
                        if (settings.alwaysRemapFuncKeys)
                            hidService?.keyDown(if (settings.alwaysRemapFuncKeysCompat) 0x44 else 0x68)

                        when (settings.getActiveConfig().func1ButtonMap) {
                            FuncButtonMap.MOUSE_LEFT_CLICK -> hidService?.setMousePosition(0,0,1)
                            FuncButtonMap.MOUSE_RIGHT_CLICK -> hidService?.setMousePosition(0,0,2)
                            FuncButtonMap.MOUSE_MIDDLE_CLICK -> hidService?.setMousePosition(0,0,4)
                            else -> {}
                        }

                    }
                    "00fa" -> {
                        Logger.d("Func2 key down")
                        if (settings.alwaysRemapFuncKeys)
                            hidService?.keyDown(if (settings.alwaysRemapFuncKeysCompat) 0x45 else 0x69)

                        when (settings.getActiveConfig().func2ButtonMap) {
                            FuncButtonMap.MOUSE_LEFT_CLICK -> hidService?.setMousePosition(0,0,1)
                            FuncButtonMap.MOUSE_RIGHT_CLICK -> hidService?.setMousePosition(0,0,2)
                            FuncButtonMap.MOUSE_MIDDLE_CLICK -> hidService?.setMousePosition(0,0,4)
                            else -> {}
                        }
                    }
                }
            }

            line.contains("EV_KEY") && line.contains("UP") -> {
                val settings = settingsFlow.value
                val parts = line.trim().split(Regex("\\s+"))
                when(parts[1]) {
                    "00f9" -> {
                        Logger.d("Func1 key up")
                        if (settings.alwaysRemapFuncKeys)
                            hidService?.keyUp(if (settings.alwaysRemapFuncKeysCompat) 0x44 else 0x68)

                        when (settings.getActiveConfig().func1ButtonMap) {
                            FuncButtonMap.MOUSE_LEFT_CLICK -> hidService?.setMousePosition(0,0,0)
                            FuncButtonMap.MOUSE_RIGHT_CLICK -> hidService?.setMousePosition(0,0,0)
                            FuncButtonMap.MOUSE_MIDDLE_CLICK -> hidService?.setMousePosition(0,0,0)
                            else -> {}
                        }
                    }
                    "00fa" -> {
                        Logger.d("Func2 key up")
                        if (settings.alwaysRemapFuncKeys)
                            hidService?.keyUp(if (settings.alwaysRemapFuncKeysCompat) 0x45 else 0x69)

                        when (settings.getActiveConfig().func1ButtonMap) {
                            FuncButtonMap.MOUSE_LEFT_CLICK -> hidService?.setMousePosition(0,0,0)
                            FuncButtonMap.MOUSE_RIGHT_CLICK -> hidService?.setMousePosition(0,0,0)
                            FuncButtonMap.MOUSE_MIDDLE_CLICK -> hidService?.setMousePosition(0,0,0)
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}