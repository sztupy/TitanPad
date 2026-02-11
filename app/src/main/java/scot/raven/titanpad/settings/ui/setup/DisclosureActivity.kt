package scot.raven.titanpad.settings.ui.setup

import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.core.ui.AppTheme
import scot.raven.titanpad.settings.ui.SettingsActivity.Companion.CONFIG_ID_EXTRA
import scot.raven.titanpad.settings.ui.SettingsState

/**
 * Auto-hide apps screen.
 */
class DisclosureActivity : ComponentActivity() {
    private lateinit var settingsState: SettingsState
    private lateinit var accessibilitySettingsObserver: ContentObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = SettingsState.Factory(TitanPad.getInstance().settingsRepository, "default")
        settingsState = ViewModelProvider(this, factory)[SettingsState::class.java]

        registerAccessibilitySettingsObserver()
        checkAccessibilityServiceStatus()

        setContent {
            AppTheme {
                DisclosureScreen (
                    settingsState = settingsState,
                    onNavigateBack = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        checkAccessibilityServiceStatus()
    }

    private fun registerAccessibilitySettingsObserver() {
        accessibilitySettingsObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    checkAccessibilityServiceStatus()
                }
            }

        val uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        contentResolver.registerContentObserver(uri, false, accessibilitySettingsObserver)
    }

    private fun checkAccessibilityServiceStatus() {
        val isServiceEnabled = TitanPad.isAccessibilityServiceEnabled(this)
        settingsState.updateAccessibilityServiceStatus(isServiceEnabled)
    }
}
