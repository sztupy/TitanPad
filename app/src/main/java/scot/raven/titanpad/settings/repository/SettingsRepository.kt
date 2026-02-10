package scot.raven.titanpad.settings.repository

import scot.raven.titanpad.settings.domain.UsageConfig
import kotlinx.coroutines.flow.Flow
import scot.raven.titanpad.settings.domain.ApplicationSettings

interface SettingsRepository {
    fun getSettings(): Flow<ApplicationSettings>
    suspend fun updateSettings(settings: ApplicationSettings)
    suspend fun validateAndUpdateSettings(settings: ApplicationSettings): ApplicationSettings.ValidationResult

    suspend fun updateSettings(configId: String, usageConfig: UsageConfig)
    suspend fun validateAndUpdateSettings(configId: String, usageConfig: UsageConfig): ApplicationSettings.ValidationResult
}
