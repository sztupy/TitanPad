package scot.raven.titanpad.settings.domain

data class ApplicationSettings(
    val configurations: Set<String>
) {
    companion object
}
