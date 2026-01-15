package scot.raven.titanpad.grid.debug

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * Miscellaneous visualization for documentation.
 */
class GridVisualizationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        )
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()
        val levels = 4

        setContent {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellsPerAxis = 3.0.pow(levels.toDouble()).toInt()
                val cellWidth = screenWidth / cellsPerAxis
                val cellHeight = screenHeight / cellsPerAxis
                val dotColor = Color.Red

                for (row in 0 until cellsPerAxis) {
                    for (col in 0 until cellsPerAxis) {
                        val centerX = col * cellWidth + (cellWidth / 2)
                        val centerY = row * cellHeight + (cellHeight / 2)

                        drawCircle(
                            color = dotColor,
                            radius = 3f,
                            center = Offset(centerX, centerY),
                        )
                    }
                }
            }
        }
    }
}
