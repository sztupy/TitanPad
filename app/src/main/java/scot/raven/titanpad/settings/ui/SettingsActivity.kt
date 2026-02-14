package scot.raven.titanpad.settings.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.accessibility.AppAccessibilityService.Companion.BROADCAST_CURSOR_ACTIVATED
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.core.ui.AppTheme
import scot.raven.titanpad.settings.ui.setup.SetupOptionsActivity
import scot.raven.titanpad.settings.ui.setup.TutorialActivity
import scot.raven.titanpad.settings.ui.setup.UsageConfigurationActivity


/**
 * Main settings screen.
 */
class SettingsActivity : ComponentActivity() {
    private lateinit var settingsState: SettingsState
    private lateinit var accessibilitySettingsObserver: ContentObserver

    private val _activeConfiguration = MutableStateFlow("")
    val activeConfiguration: StateFlow<String> = _activeConfiguration.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BROADCAST_CURSOR_ACTIVATED -> {
                    _activeConfiguration.value = intent.getStringExtra(CONFIG_ID_EXTRA).orEmpty()

                    Logger.d("Received active cursor setting change to ${_activeConfiguration.value}")
                }
            }
        }
    }

    private fun startCustomActivity(context: Context, activityClass: Class<*>, configId: String) {
        val intent = Intent(context, activityClass)
        intent.putExtra(CONFIG_ID_EXTRA, configId)
        val options = ActivityOptionsCompat.makeBasic()
        context.startActivity(intent, options.toBundle())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = SettingsState.Factory(TitanPad.getInstance().settingsRepository,"default")

        settingsState = ViewModelProvider(this, factory)[SettingsState::class.java]
        settingsState.setToastFunction { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

         _activeConfiguration.value = TitanPad.getInstance().getActiveConfig()
        Logger.d("Active config for Activity: ${_activeConfiguration.value}")

        registerAccessibilitySettingsObserver()
        checkAccessibilityServiceStatus()

        setContent {
            AppTheme {
                SettingsScreen(
                    settingsState = settingsState,
                    activeConfiguration = activeConfiguration,
                    onNavigateToCursorSettings = { configId -> startCustomActivity(this, UsageConfigurationActivity::class.java, configId) },
                    onNavigateToSetupOptions = { startCustomActivity(this, SetupOptionsActivity::class.java, "default") },
                    onNavigateToTutorials = { startCustomActivity(this, TutorialActivity::class.java, "default") },
                )
            }
        }
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

    override fun onResume() {
        super.onResume()

        val filter = IntentFilter()
        filter.addAction(BROADCAST_CURSOR_ACTIVATED)
        registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)

        checkAccessibilityServiceStatus()

        _activeConfiguration.value = TitanPad.getInstance().getActiveConfig()
        Logger.d("Active config for Activity: ${_activeConfiguration.value}")
    }

    public override fun onPause() {
        super.onPause()

        unregisterReceiver(receiver)
    }

    private fun checkAccessibilityServiceStatus() {
        val isServiceEnabled = TitanPad.isAccessibilityServiceEnabled(this)
        settingsState.updateAccessibilityServiceStatus(isServiceEnabled)
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(accessibilitySettingsObserver)
        super.onDestroy()
    }

    companion object {
        const val CONFIG_ID_EXTRA = "config_id"
        const val SCROLL_ID_EXTRA = "scroll_id"
    }
}
