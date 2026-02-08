package scot.raven.titanpad.gesture.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import scot.raven.titanpad.core.constants.GestureConstants
import scot.raven.titanpad.core.domain.ScreenDimensions
import scot.raven.titanpad.settings.domain.OverlaySettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

data class GesturePath(
    val id: String,
    val currentPosition: Offset,
    val type: GestureType,
    val startTime: Long,
)

enum class GestureType {
    TAP
}

/**
 * Defines gesture visualization paths rendered in the overlay UI manager.
 */
@Composable
fun GestureVisualization(
    gesturePaths: List<GesturePath>,
    dimensions: ScreenDimensions,
    modifier: Modifier = Modifier,
    settings: OverlaySettings? = null
) {
    var visualSize = settings?.visualSize?.toFloat() ?: GestureConstants.DEFAULT_SIZE.toFloat()
    visualSize *= GestureConstants.SIZE_MULTIPLIER * dimensions.getScreenScaleFactor()
    val circleColor = Color.White.copy(alpha = 0.8f)

    Canvas(modifier = modifier.fillMaxSize()) {
        gesturePaths.forEach { gesturePath ->
            drawCircle(
                color = Color.Black,
                radius = visualSize,
                center = gesturePath.currentPosition,
                style = Stroke(width = visualSize * 0.3f),
            )
            drawCircle(
                color = circleColor,
                radius = visualSize,
                center = gesturePath.currentPosition,
            )
        }
    }
}
fun showStationaryGesture(
    gestureId: String,
    position: Offset,
    type: GestureType,
    pathsFlow: MutableStateFlow<List<GesturePath>>
) {
    val path =
        GesturePath(
            id = gestureId,
            currentPosition = position,
            type = type,
            startTime = System.currentTimeMillis(),
        )
    pathsFlow.update { it + path }
}

fun endStationaryGesture(
    gestureId: String,
    pathsFlow: MutableStateFlow<List<GesturePath>>,
) {
    pathsFlow.update { currentPaths ->
        currentPaths.filter { it.id != gestureId }
    }
}