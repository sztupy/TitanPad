package scot.raven.titanpad.core.domain

import android.content.Context
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.Display
import android.view.WindowInsets
import android.view.WindowManager
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.core.util.OrientationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import scot.raven.titanpad.settings.domain.ApplicationSettings

/**
 * Handles device orientation changes.
 */
class OrientationHandler(
    private val context: Context,
    private val settingsFlow: StateFlow<ApplicationSettings>
) {
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _screenDimensions = MutableStateFlow(getPhysicalDimensions())
    val screenDimensions: StateFlow<ScreenDimensions> = _screenDimensions.asStateFlow()

    private val _currentOrientation = MutableStateFlow(getOrientation())

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}

        override fun onDisplayChanged(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY) return

            mainHandler.post {
                Choreographer.getInstance().postFrameCallback {
                    updateScreenInfo()
                }
            }
        }
    }

    init {
        displayManager.registerDisplayListener(displayListener, mainHandler)

        settingsFlow.onEach {
            updateScreenInfo()
        }.launchIn(CoroutineScope(Dispatchers.Main + SupervisorJob()))

        updateScreenInfo()
    }

    private fun updateScreenInfo() {
        try {
            val settings = settingsFlow.value
            val dimensions = if (settings.getActiveConfig().usePhysicalSize) getPhysicalDimensions() else getUsableDimensions()
            _screenDimensions.value = dimensions

            val orientation = getOrientation()
            _currentOrientation.value = orientation

            Logger.i("Updated screen dimensions to: ${dimensions.width} x ${dimensions.height}, $orientation")
        } catch (e: Exception) {
            Logger.e("Error updating screen dimensions", e)
        }
    }

    private fun getPhysicalDimensions(): ScreenDimensions {
        val bounds = windowManager.currentWindowMetrics.bounds
        return ScreenDimensions(bounds.width(), bounds.height())
    }

    fun getSystemInsets(): Rect {
        val windowMetrics = windowManager.currentWindowMetrics
        val insets = windowMetrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        return Rect(insets.left, insets.top, insets.right, insets.bottom)
    }

    private fun getUsableDimensions(): ScreenDimensions {
        val windowMetrics = windowManager.currentWindowMetrics
        val insets = windowMetrics.windowInsets
            .getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or
                        WindowInsets.Type.displayCutout()
            )
        val insetsWidth = insets.left + insets.right
        val insetsHeight = insets.top + insets.bottom

        val bounds = windowMetrics.bounds
        return ScreenDimensions(
            bounds.width() - insetsWidth,
            bounds.height() - insetsHeight
        )
    }

    private fun getOrientation(): OrientationUtil.Orientation {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val defaultRotation = windowManager.defaultDisplay.rotation

        val rotation = runCatching {
            context.display.rotation
        }.getOrDefault(defaultRotation)

        return OrientationUtil.getOrientationFromRotation(rotation)
    }

    fun cleanup() {
        displayManager.unregisterDisplayListener(displayListener)
    }
}