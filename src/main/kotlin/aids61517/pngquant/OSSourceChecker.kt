package aids61517.pngquant

import aids61517.pngquant.data.OSSource
import aids61517.pngquant.util.Logger

object OSSourceChecker {
    val osSource: OSSource by lazy {
        val osName = System.getProperty("os.name")
            .lowercase()
        Logger.print("OSSourceChecker osName = $osName")
        when {
            osName.startsWith("windows") -> OSSource.WINDOWS
            osName.startsWith("mac") -> OSSource.MAC
            else -> OSSource.OTHER
        }
    }
}