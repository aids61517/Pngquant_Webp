package aids61517.pngquant.initial

import aids61517.pngquant.MainWindow
import aids61517.pngquant.data.OSSource
import aids61517.pngquant.util.Logger
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import okio.source
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

abstract class InitialHandler {

    companion object {
        fun create(osSource: OSSource): InitialHandler {
            return when (osSource) {
                OSSource.WINDOWS -> WindowsInitialHandler()
                OSSource.MAC -> MacInitialHandler()
                else -> throw IllegalStateException("Unsupported OS")
            }
        }
    }

    abstract val pngquantPath: Path

    abstract val webpPath: Path?

    protected val userName
        get() = System.getProperty("user.name")

    protected val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    abstract fun initialize(coroutineScope: CoroutineScope, appState: MutableState<MainWindow.State>)

    protected fun downloadFileIfNeed(
        downloadUrl: String,
        filePath: Path,
    ) {
        val isFileNotExist = Files.notExists(filePath)
        val forceToDownload = isFileNotExist

        if (isFileNotExist) {
            Files.createFile(filePath)
        }

        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .build()
            val response = okHttpClient.newCall(request)
                .execute()

            if (response.isSuccessful.not() && forceToDownload) {
                Logger.print("Cannot fetch file: $downloadUrl")
                throw IOException("download file failed")
            }

            response.body?.run {
                val contentLength = contentLength()
                val shouldDownload = (forceToDownload) || (Files.size(filePath) != contentLength)
                Logger.print("contentLength = $contentLength")
                Logger.print("shouldDownload = $shouldDownload")
                if (shouldDownload) {
                    this.byteStream()
                        .source()
                        .buffer()
                        .use { source ->
                            Files.newOutputStream(filePath)
                                .sink()
                                .buffer()
                                .use { sink ->
                                    sink.writeAll(source)
                                }
                        }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            e.message?.let { Logger.print(it) }

            if (isFileNotExist) {
                throw e
            }
        }
    }
}