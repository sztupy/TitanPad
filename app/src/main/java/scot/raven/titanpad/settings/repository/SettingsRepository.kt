package scot.raven.titanpad.settings.repository

import scot.raven.titanpad.settings.domain.UsageConfig
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<UsageConfig>
    suspend fun updateSettings(settings: UsageConfig)
    suspend fun validateAndUpdateSettings(settings: UsageConfig): UsageConfig.ValidationResult
}
