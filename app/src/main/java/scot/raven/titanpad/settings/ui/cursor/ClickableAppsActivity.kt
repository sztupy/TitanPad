package scot.raven.titanpad.settings.ui.cursor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.core.ui.AppTheme
import scot.raven.titanpad.settings.ui.SettingsState

/**
 * Auto-hide apps screen.
 */
class ClickableAppsActivity : ComponentActivity() {
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
                ClickableAppsScreen(
                    settingsState = settingsState,
                    onNavigateBack = {
                        finish()
                    }
                )
            }
        }
    }
}
