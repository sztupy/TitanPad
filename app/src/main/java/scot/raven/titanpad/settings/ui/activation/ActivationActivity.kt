package scot.raven.titanpad.settings.ui.activation

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
import scot.raven.titanpad.settings.ui.SettingsActivity.Companion.SCROLL_ID_EXTRA
import scot.raven.titanpad.settings.ui.SettingsState

/**
 * Auto-hide apps screen.
 */
class ActivationActivity : ComponentActivity() {
    private lateinit var settingsState: SettingsState

    private fun startCustomActivity(context: Context, activityClass: Class<*>, configId: String, scrollId: Int = -1) {
        val intent = Intent(context, activityClass)
        intent.putExtra(CONFIG_ID_EXTRA, configId)
        if (scrollId>=0)
            intent.putExtra(SCROLL_ID_EXTRA, scrollId)

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
                ActivationScreen(
                    settingsState = settingsState,
                    onNavigateToEnableAppsScreen = { startCustomActivity(this, AutoEnableAppsActivity::class.java, configId) },
                    onNavigateBack = {
                        finish()
                    }
                )
            }
        }
    }
}