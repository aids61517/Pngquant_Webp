package aids61517.pngquant

import aids61517.pngquant.webp.WebpHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.nio.file.Path

object WebpHelper {
    val coroutineScope = CoroutineScope(SupervisorJob())

    private val webpHandler by lazy {
        WebpHandler.create(OSSourceChecker.osSource, coroutineScope)
    }

    val isWebpAvailable: Boolean
        get() = webpHandler.isWebpAvailable

    suspend fun run(
        filePathList: List<Path>,
        deletePngquantFile: Boolean,
    ) = withContext(Dispatchers.IO) {
        webpHandler.run(filePathList, deletePngquantFile)
    }
}