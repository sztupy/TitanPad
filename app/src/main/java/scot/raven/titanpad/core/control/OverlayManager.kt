package scot.raven.titanpad.core.control

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Looper
import android.view.Choreographer
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import scot.raven.titanpad.core.domain.OrientationHandler
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.core.ui.AppTheme
import scot.raven.titanpad.cursor.ui.CursorOverlay
import scot.raven.titanpad.gesture.ui.GestureVisualization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import scot.raven.titanpad.settings.domain.ApplicationSettings

/**
 * Creates and updates the overlay according to observed state changes.
 */
class OverlayManager(
    private val context: Context,
    private val backgroundScope: CoroutineScope,
    private val mainScope: CoroutineScope,
    private val windowManager: WindowManager,
    private val settingsFlow: StateFlow<ApplicationSettings>,
    private val orientationHandler: OrientationHandler,
    private val coreManager: CoreManager,
    private val lifecycleOwner: LifecycleOwner,
    private val savedStateRegistryOwner: SavedStateRegistryOwner,
    private val keysPressed: StateFlow<Int>,
    private val _layoutApplied: MutableSharedFlow<Unit>
) {
    private var overlayView: ComposeView? = null
    private var currentPaths: Int? = null

    fun initialize() {
        try {
            Logger.i("Initializing OverlayManager")
            observeStateChanges()
        } catch (e: Exception) {
            Logger.e("Error initializing OverlayManager", e)
        }
    }

    private fun observeStateChanges() {
        coreManager.cursorStateManager.cursorState
            .onEach { cursor ->
//                Logger.d("Cursor state changed: ${cursor != null}")
                mainScope.launch {
                    updateOverlayUI()
                }
            }
            .launchIn(backgroundScope)

        coreManager.getGesturePaths()
            .onEach { paths ->
                if (currentPaths != paths.size) {
                    Logger.d("Gesture paths changed: ${paths.size} paths")
                }
                currentPaths = paths.size
                mainScope.launch {
                    updateOverlayUI()
                }
            }
            .launchIn(backgroundScope)

        settingsFlow
            .onEach { settings ->
                Logger.d("Settings changed")
                coreManager.updateGestureVisualization(settings.getActiveConfig().showGestureVisualization)
                mainScope.launch {
                    updateOverlayUI(touchChanged = !settings.getActiveConfig().disableTouchscreen)
                }
            }
            .launchIn(backgroundScope)

        keysPressed
            .onEach { num ->
                val settings = settingsFlow.value

                if (settings.getActiveConfig().disableTouchscreen) {
                    when (num) {
                        0 -> mainScope.launch {
                            updateOverlayUI(touchEnabled = false)
                        }

                        1 -> mainScope.launch {
                            updateOverlayUI(touchEnabled = true)
                        }
                    }
                }
            }
            .launchIn(backgroundScope)
    }

    fun updateOverlayUI(touchEnabled: Boolean? = null, touchChanged: Boolean? = null) {
        try {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                Logger.e("updateOverlayUI was called from a non-main thread")
                mainScope.launch { updateOverlayUI() }
                return
            }

//            if (isOrientationChanging) {
//                Logger.d("Skipping overlay update during orientation change")
//                return
//            }

            val cursor = coreManager.cursorStateManager.cursorState.value
            val gesturePaths = coreManager.getGesturePaths().value
            val settings = settingsFlow.value

            val shouldShowOverlay = cursor != null ||
                    (gesturePaths.isNotEmpty() && settings.getActiveConfig().showGestureVisualization)

            if (!shouldShowOverlay && overlayView != null) {
                removeOverlayView()
                return
            }

            if (shouldShowOverlay && overlayView == null) {
                createOverlayView()
            }

            // Notify cursor of touchscreen changes due to key presses
            if (touchEnabled != null && overlayView != null) {
                try {
                    windowManager.updateViewLayout(overlayView, createOverlayLayoutParams(touchEnabled))
                    Choreographer.getInstance().postFrameCallbackDelayed({ _ ->
                        _layoutApplied.tryEmit(Unit)
                    }, 50)
                } catch (e: Exception) {
                    Logger.e("Failed to update overlay layout", e)
                }
            }

            // Immediately update layout depending on setting
            if (touchChanged != null && overlayView != null) {
                try {
                    windowManager.updateViewLayout(overlayView, createOverlayLayoutParams(touchChanged))
                } catch (e: Exception) {
                    Logger.e("Failed to update overlay layout", e)
                }
            }

            overlayView?.setContent {
                val currentSettings by settingsFlow.collectAsState()
                val currentGesturePaths by coreManager.getGesturePaths().collectAsState()
                val dimensions by orientationHandler.screenDimensions.collectAsState()

                AppTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent,
                    ) {
                        val currentCursor by coreManager.cursorStateManager.cursorState.collectAsState()
                        currentCursor?.let { activeCursor ->
                            CursorOverlay(
                                cursorState = activeCursor,
                                settings = currentSettings,
                                dimensions = dimensions
                            )
                        }

                        if (currentSettings.getActiveConfig().showGestureVisualization && currentGesturePaths.isNotEmpty()) {
                            GestureVisualization(
                                gesturePaths = currentGesturePaths,
                                settings = currentSettings,
                                dimensions = dimensions
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("Error updating overlay UI", e)
            try {
                mainScope.launch {
                    removeOverlayView()
                    if (coreManager.cursorStateManager.cursorState.value != null
                    ) {
                        createOverlayView()
                    }
                }
            } catch (innerE: Exception) {
                Logger.e("Failed to recover overlay UI", innerE)
            }
        }
    }

    private fun createOverlayView() {
        try {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                Logger.e("createOverlayView was called from a non-main thread")
                mainScope.launch { createOverlayView() }
                return
            }
            val settings = settingsFlow.value

            val composeView = ComposeView(context)
            composeView.setViewTreeLifecycleOwner(lifecycleOwner)
            composeView.setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
            composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

            composeView.setOnApplyWindowInsetsListener { _, insets ->
                mainScope.launch { updateOverlayUI() }
                insets
            }

            overlayView = composeView

            windowManager.addView(overlayView, createOverlayLayoutParams(touchEnabled = !settings.getActiveConfig().disableTouchscreen))
            Logger.d("Overlay view created and added to window manager")
        } catch (e: Exception) {
            Logger.e("Failed to add overlay view", e)
            overlayView = null
        }
    }

    private fun removeOverlayView() {
        try {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                Logger.e("removeOverlayView was called from a non-main thread")
                mainScope.launch { removeOverlayView() }
                return
            }

            overlayView?.let { view ->
                windowManager.removeView(view)
                overlayView = null
                Logger.d("Overlay view removed")
            }
        } catch (e: Exception) {
            Logger.e("Failed to remove overlay view", e)
            overlayView = null
        }
    }

    private fun createOverlayLayoutParams(touchEnabled: Boolean? = null): WindowManager.LayoutParams {
        val settings = settingsFlow.value
        val insets = if (!settings.getActiveConfig().usePhysicalSize) {
            orientationHandler.getSystemInsets()
        } else {
            Rect(0, 0, 0, 0)
        }

        return WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            flags = (
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    )

            if (touchEnabled != null && touchEnabled) {
                // Overlay does not absorb touches and will let gestures pass through
                flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }

            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START

            x = insets.left
            y = insets.top
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }
    }

    fun cleanup() {
        overlayView?.disposeComposition()
        mainScope.launch {
            removeOverlayView()
        }
        Logger.d("OverlayManager cleanup completed")
    }
}