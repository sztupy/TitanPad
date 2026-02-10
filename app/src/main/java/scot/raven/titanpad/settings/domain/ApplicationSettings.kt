package scot.raven.titanpad.settings.domain

data class ApplicationSettings(
    val alwaysRemapFuncKeys: Boolean = Defaults.Settings.ALWAYS_REMAP_FUNC_KEYS,
    val alwaysRemapFuncKeysCompat: Boolean = Defaults.Settings.ALWAYS_REMAP_FUNC_KEYS_COMPAT,
    val lastActiveSetting: String = "",

    val defaultConfig: UsageConfig = UsageConfig(),
    val additionalConfigs: List<UsageConfig> = ArrayList()
) {
    companion object {
        val DEFAULT = ApplicationSettings()
    }

    fun getActiveConfig() : UsageConfig {
        var result = additionalConfigs.find{ it.configId == lastActiveSetting }
        if (result==null)
            return defaultConfig
        else
            return result
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
