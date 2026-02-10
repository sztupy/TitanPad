package scot.raven.titanpad.settings.ui.setup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import scot.raven.titanpad.core.ui.AppTheme

class SetupOptionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                SetupOptionsScreen(
                    onNavigateBack = {
                        finish()
                    }
                )
            }
        }
    }
}