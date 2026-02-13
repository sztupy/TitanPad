package scot.raven.titanpad.settings.ui.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.core.ui.AppTheme
import scot.raven.titanpad.settings.ui.SettingsActivity
import scot.raven.titanpad.settings.ui.SettingsState

/**
 * Auto-hide apps screen.
 */
class DisclosureActivity : ComponentActivity() {
    private lateinit var settingsState: SettingsState

    private fun startCustomActivity(context: Context, activityClass: Class<*>) {
        val intent = Intent(context, activityClass)
        val options = ActivityOptionsCompat.makeBasic()
        context.startActivity(intent, options.toBundle())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = SettingsState.Factory(TitanPad.getInstance().settingsRepository, "default")
        settingsState = ViewModelProvider(this, factory)[SettingsState::class.java]

        val onNavigateToSettingsPage = {
            startCustomActivity(this, SettingsActivity::class.java)
            finish()
        }

        setContent {
            AppTheme {
                DisclosureScreen (
                    settingsState = settingsState,
                    onNavigateToSettingsPage = onNavigateToSettingsPage,
                    onNavigateBack = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }
}
