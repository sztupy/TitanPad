package scot.raven.titanpad.settings.ui.gesture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.core.ui.AppTheme
import scot.raven.titanpad.settings.ui.SettingsState

/**
 * Common gesture settings screen.
 */
class CommonGestureSettingsActivity : ComponentActivity() {
    private lateinit var settingsState: SettingsState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory =
            SettingsState.Factory(
                TitanPad.getInstance().settingsRepository,
            )
        settingsState = ViewModelProvider(this, factory)[SettingsState::class.java]

        setContent {
            AppTheme {
                CommonGestureSettingsScreen(
                    settingsState = settingsState,
                    onNavigateBack = {
                        finish()
                    },
                )
            }
        }
    }
}
