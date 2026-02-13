package scot.raven.titanpad.settings.domain

import kotlinx.serialization.Serializable
import scot.raven.titanpad.BuildConfig

@Serializable
data class ApplicationSettings(
    val className: String = "ApplicationSettings",
    val versionCode: Int = BuildConfig.VERSION_CODE,
    val alwaysRemapFuncKeys: Boolean = Defaults.Settings.ALWAYS_REMAP_FUNC_KEYS,
    val alwaysRemapFuncKeysCompat: Boolean = Defaults.Settings.ALWAYS_REMAP_FUNC_KEYS_COMPAT,
    val runAfterBoot: String = Defaults.Settings.RUN_AFTER_BOOT,
    val lastActiveSetting: String = "",
    var disclosureAccepted: Boolean = false,

    val defaultConfig: UsageConfig = UsageConfig(),
    val additionalConfigs: List<UsageConfig> = ArrayList()
) {
    companion object {
        val DEFAULT = ApplicationSettings()
    }

    fun getActiveConfig() : UsageConfig {
        val result = additionalConfigs.find{ it.configId == lastActiveSetting }
        return result ?: defaultConfig
    }

    fun validate(): ValidationResult {
        val errors = (additionalConfigs + defaultConfig).map { it.validate().errors }.flatten()

        return ValidationResult(errors.isEmpty(), errors)
    }

    fun sanitized(): ApplicationSettings {
        return copy(
            defaultConfig = defaultConfig.sanitized(),
            additionalConfigs = additionalConfigs.map{ it.sanitized() }
        )
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
    )
}
