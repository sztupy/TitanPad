package scot.raven.titanpad.settings.repository

import scot.raven.titanpad.settings.domain.UsageConfig
import kotlinx.coroutines.flow.Flow
import scot.raven.titanpad.settings.domain.ApplicationSettings

interface SettingsRepository {
    fun getSettings(): Flow<ApplicationSettings>

    suspend fun setActiveKey(configId: String)
    suspend fun updateSettings(settings: ApplicationSettings)
    suspend fun validateAndUpdateSettings(settings: ApplicationSettings): ApplicationSettings.ValidationResult

    fun exportSettings(settings: ApplicationSettings) : String
    fun exportSettings(configId: String, usageConfig: UsageConfig) : String
    fun exportSettingsWithoutAppData(configId: String, usageConfig: UsageConfig): String
    suspend fun importSettings(jsonData: String) : Boolean
    suspend fun importSettings(configId: String, jsonData: String) : Boolean
    suspend fun importSettingsWithoutAppData(configId: String, jsonData: String): Boolean

    suspend fun updateSettings(configId: String, usageConfig: UsageConfig)
    suspend fun validateAndUpdateSettings(configId: String, usageConfig: UsageConfig): ApplicationSettings.ValidationResult
}
