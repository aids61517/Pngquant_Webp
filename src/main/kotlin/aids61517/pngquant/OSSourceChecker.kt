package aids61517.pngquant

import aids61517.pngquant.data.OSSource

object OSSourceChecker {
    val osSource: OSSource by lazy {
        val osName = System.getProperty("os.name")
            .lowercase()
        println("OSSourceChecker osName = $osName")
        when {
            osName.startsWith("windows") -> OSSource.WINDOWS
            osName.startsWith("mac") -> OSSource.MAC
            else -> OSSource.OTHER
        }
    }
}