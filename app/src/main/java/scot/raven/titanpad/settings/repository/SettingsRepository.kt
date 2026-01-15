package scot.raven.titanpad.settings.repository

import scot.raven.titanpad.settings.domain.OverlaySettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<OverlaySettings>
    suspend fun updateSettings(settings: OverlaySettings)
    suspend fun validateAndUpdateSettings(settings: OverlaySettings): OverlaySettings.ValidationResult
}
