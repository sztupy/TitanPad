package scot.raven.titanpad.core.logs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import scot.raven.titanpad.core.ui.AppTheme

/**
 * Basic console for real-time logs.
 */
class LogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                LogScreen(
                    onNavigateBack = {
                        finish()
                    }
                )
            }
        }
    }
}