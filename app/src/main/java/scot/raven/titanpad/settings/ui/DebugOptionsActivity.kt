package scot.raven.titanpad.settings.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.core.ui.AppTheme

class DebugOptionsActivity : ComponentActivity() {
    private lateinit var settingsState: SettingsState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = SettingsState.Factory(TitanPad.getInstance().settingsRepository)
        settingsState = ViewModelProvider(this, factory)[SettingsState::class.java]

        setContent {
            AppTheme {
                DebugOptionsScreen(
                    settingsState = settingsState,
                    onNavigateBack = {
                        finish()
                    }
                )
            }
        }
    }
}