package scot.raven.titanpad.settings.ui.cursor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.core.ui.AppTheme
import scot.raven.titanpad.settings.ui.SettingsState

/**
 * Standard cursor settings screen.
 */
class CursorSettingsActivity : ComponentActivity() {
    private lateinit var settingsState: SettingsState

    private fun startCustomActivity(context: Context, activityClass: Class<*>) {
        val intent = Intent(context, activityClass)
        val options = ActivityOptionsCompat.makeBasic()
        context.startActivity(intent, options.toBundle())
    }

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
                CursorSettingsScreen(
                    settingsState = settingsState,
                    onNavigateToCursorIcon = { startCustomActivity(this, CursorIconActivity::class.java) },
                    onNavigateToLocationClickableIcon = { startCustomActivity(this, LocationClickableIconActivity::class.java) },
                    onNavigateToScrollToggleIcon = { startCustomActivity(this, ScrollToggleIconActivity::class.java) },
                    onNavigateToClickableAppsScreen = { startCustomActivity(this, ClickableAppsActivity::class.java) },
                    onNavigateToAssignScrollScreen = { startCustomActivity(this, AssignScrollActivity::class.java) },
                    onNavigateBack = {
                        finish()
                    },
                )
            }
        }
    }
}
