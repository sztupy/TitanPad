package scot.raven.titanpad.settings.ui.scroll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.core.ui.AppTheme
import scot.raven.titanpad.settings.ui.SettingsActivity.Companion.CONFIG_ID_EXTRA
import scot.raven.titanpad.settings.ui.SettingsActivity.Companion.SCROLL_ID_EXTRA
import scot.raven.titanpad.settings.ui.SettingsState

/**
 * Auto-hide cursor settings screen.
 */
class WheelSettingsActivity : ComponentActivity() {
    private lateinit var settingsState: SettingsState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var configId = intent.getStringExtra(CONFIG_ID_EXTRA)
        if (configId == null)
            configId = "default"

        val scrollId = intent.getIntExtra(SCROLL_ID_EXTRA, 0)

        val factory =
            SettingsState.Factory(
                TitanPad.getInstance().settingsRepository,
                configId
            )
        settingsState = ViewModelProvider(this, factory)[SettingsState::class.java]

        setContent {
            AppTheme {
                WheelSettingsScreen (
                    settingsState = settingsState,
                    scrollId = scrollId,
                    onNavigateBack = {
                        finish()
                    }
                )
            }
        }
    }
}
