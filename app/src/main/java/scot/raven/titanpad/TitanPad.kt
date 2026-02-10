package scot.raven.titanpad

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import scot.raven.titanpad.accessibility.AppAccessibilityService
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.core.shizuku.ShizukuConnection
import scot.raven.titanpad.core.shizuku.ShizukuStatus
import scot.raven.titanpad.settings.repository.SettingsRepository
import scot.raven.titanpad.settings.repository.SettingsRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import scot.raven.titanpad.cursor.control.InputManager
import scot.raven.titanpad.settings.domain.ApplicationSettings

/**
 * Checks accessibility service and initializes Shizuku service on Android 11.
 */
class TitanPad : Application() {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val _settingsRepository: Lazy<SettingsRepository> =
        lazy {
            SettingsRepositoryImpl(applicationContext.dataStore)
        }
    val settingsRepository: SettingsRepository by _settingsRepository
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var shizukuObserverJob: Job? = null
    private var _inputManager: InputManager? = null
    private var settingsObserverJob: Job? = null
    private lateinit var _settingsFlow: StateFlow<ApplicationSettings>

    fun getSettingsFlow(): StateFlow<ApplicationSettings> {
        if (!::_settingsFlow.isInitialized) {
            _settingsFlow = settingsRepository.getSettings().stateIn(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                started = SharingStarted.Eagerly,
                initialValue = ApplicationSettings()
            )
        }
        return _settingsFlow
    }

    fun setTrackpadActionHandler(handler: InputManager) {
        _inputManager = handler
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeShizuku()
        Logger.i("TitanPad application initialized")
    }

    private fun initializeShizuku() {
        Logger.i("Initializing Shizuku")
        ShizukuConnection.initialize()
        shizukuObserverJob =
            ShizukuConnection.observeStatus { status ->
                Logger.d("Shizuku status changed: $status")

                when (status) {
                    ShizukuStatus.PERMISSION_REQUIRED -> {
                        Logger.d("Auto-requesting Shizuku permission")
                        ShizukuConnection.requestPermission()
                    }

                    ShizukuStatus.NOT_AVAILABLE -> {
                        ShizukuConnection.resetPermissionRetryCount()
                        _inputManager?.stop()
                    }

                    ShizukuStatus.ERROR -> {
                        _inputManager?.stop()
                    }

                    ShizukuStatus.READY -> {
                        ShizukuConnection.resetPermissionRetryCount()
                        _inputManager?.start()
                        Logger.i("Shizuku ready")
                    }

                    else -> {}
                }
            }
    }

    private fun cleanupShizuku() {
        shizukuObserverJob?.cancel()
        _inputManager?.stop()
        ShizukuConnection.cleanup()
        shizukuObserverJob?.cancel()
    }

    override fun onTerminate() {
        Logger.i("TitanPad application terminating")

        settingsObserverJob?.cancel()
        applicationScope.cancel()
        cleanupShizuku()

        super.onTerminate()
    }

    companion object {
        private lateinit var instance: TitanPad

        fun getInstance(): TitanPad {
            return instance
        }

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            try {
                val am =
                    context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
                val enabledServices =
                    am.getEnabledAccessibilityServiceList(
                        AccessibilityServiceInfo.FEEDBACK_ALL_MASK,
                    )
                return enabledServices.any {
                    it.id.contains(context.packageName) &&
                            it.id.contains(AppAccessibilityService::class.java.simpleName)
                }
            } catch (e: Exception) {
                Logger.e("Error checking accessibility service status", e)
                return false
            }
        }
    }
}
