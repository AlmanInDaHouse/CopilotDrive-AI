package ai.copilotdrive.core

import java.net.URLEncoder

object NavigationUris {
    fun validate(destination: String) {
        require(destination.isNotBlank() && destination.length <= 200)
        require(destination.none { it.isISOControl() })
        require(!Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(destination.trim()))
    }
    private fun encoded(destination: String): String {
        validate(destination)
        return URLEncoder.encode(destination.trim(), "UTF-8").replace("+", "%20")
    }
    fun maps(destination: String) = "google.navigation:q=${encoded(destination)}&mode=d"
    fun waze(destination: String) = "https://waze.com/ul?q=${encoded(destination)}&navigate=yes"
    fun validPhone(number: String) = Regex("\\+?[0-9][0-9 ()-]{2,24}").matches(number)
}
