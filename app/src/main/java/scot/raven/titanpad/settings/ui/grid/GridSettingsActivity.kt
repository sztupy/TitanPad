package scot.raven.titanpad.settings.ui.grid

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.core.ui.AppTheme
import scot.raven.titanpad.settings.ui.SettingsState

/**
 * Grid cursor settings screen.
 */
class GridSettingsActivity : ComponentActivity() {
    private lateinit var settingsState: SettingsState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory =
            SettingsState.Factory(
                TitanPad.getInstance().settingsRepository,
            )
        settingsState = ViewModelProvider(this, factory)[SettingsState::class.java]
        settingsState.setToastFunction { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        setContent {
            AppTheme {
                GridSettingsScreen(
                    settingsState = settingsState,
                    onNavigateBack = {
                        finish()
                    },
                )
            }
        }
    }
}
