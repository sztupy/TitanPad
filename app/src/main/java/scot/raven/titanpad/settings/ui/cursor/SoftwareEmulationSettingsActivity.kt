package scot.raven.titanpad.settings.ui.cursor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.core.ui.AppTheme
import scot.raven.titanpad.settings.ui.SettingsActivity.Companion.CONFIG_ID_EXTRA
import scot.raven.titanpad.settings.ui.SettingsState

/**
 * Common gesture settings screen.
 */
class SoftwareEmulationSettingsActivity : ComponentActivity() {
    private lateinit var settingsState: SettingsState

    private fun startCustomActivity(context: Context, activityClass: Class<*>, configId: String) {
        val intent = Intent(context, activityClass)
        intent.putExtra(CONFIG_ID_EXTRA, configId)
        val options = ActivityOptionsCompat.makeBasic()
        context.startActivity(intent, options.toBundle())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var configId = intent.getStringExtra(CONFIG_ID_EXTRA)
        if (configId == null)
            configId = "default"

        val factory =
            SettingsState.Factory(
                TitanPad.getInstance().settingsRepository,
                configId
            )
        settingsState = ViewModelProvider(this, factory)[SettingsState::class.java]

        setContent {
            AppTheme {
                SoftwareEmulationSettingsScreen(
                    settingsState = settingsState,
                    onNavigateToCursorIcon = { startCustomActivity(this, CursorIconActivity::class.java, configId) },
                    onNavigateToLocationClickableIcon = { startCustomActivity(this, LocationClickableIconActivity::class.java, configId) },
                    onNavigateToClickableAppsScreen = { startCustomActivity(this, ClickableAppsActivity::class.java, configId) },
                    onNavigateBack = {
                        finish()
                    },
                )
            }
        }
    }
}
