package aids61517.pngquant

import aids61517.pngquant.data.ExePath
import aids61517.pngquant.data.OSSource
import aids61517.pngquant.util.CopyFileHandler
import aids61517.pngquant.webp.WebpHandler
import kotlinx.coroutines.*
import okio.buffer
import okio.sink
import okio.source
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import kotlin.coroutines.resume
import kotlin.io.path.absolute

object WebpHelper {
    val coroutineScope = CoroutineScope(SupervisorJob())

    private val webpHandler by lazy {
        WebpHandler.create(OSSourceChecker.osSource, coroutineScope)
    }

    suspend fun run(filePathList: List<Path>) = withContext(Dispatchers.IO) {
        webpHandler.run(filePathList)
    }
}