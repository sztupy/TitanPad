package scot.raven.titanpad.settings.domain

data class ApplicationSettings(
    val currentConfigKey: String = "",
    val configurations: Map<String, UsageConfig> = HashMap()
) {
    companion object
}
