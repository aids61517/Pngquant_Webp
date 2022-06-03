package aids61517.pngquant.webp

import aids61517.pngquant.data.OSSource
import aids61517.pngquant.util.Logger
import kotlinx.coroutines.*
import okio.buffer
import okio.source
import java.nio.charset.Charset
import java.nio.file.*
import kotlin.coroutines.resume

abstract class WebpHandler(protected val coroutineScope: CoroutineScope) {
    companion object {
        fun create(osSource: OSSource, coroutineScope: CoroutineScope): WebpHandler {
            return when (osSource) {
                OSSource.WINDOWS -> WindowsWebpHandler(coroutineScope)
                OSSource.MAC -> MacWebpHandler(coroutineScope)
                else -> throw IllegalStateException("Unsupported OS")
            }
        }
    }

    abstract suspend fun run(
        filePathList: List<Path>,
        deletePngquantFile: Boolean,
    ): List<Path>

    protected suspend fun executeCmdAndGetImageCreated(cmd: Array<String>, filePath: Path): Path {
        return suspendCancellableCoroutine { continuation ->
            val directoryPath = filePath.parent
            val targetFileName = filePath.fileName.toString()
                .replace(".png", ".webp")
            val targetPath = directoryPath.resolve(targetFileName)
            Files.deleteIfExists(targetPath)

            val watcher = FileSystems.getDefault().newWatchService()
            val watchKey = directoryPath.register(
                watcher,
                StandardWatchEventKinds.ENTRY_CREATE,
            )

            Logger.print("cmd = ${createCmdLog(cmd)}")
            val processBuilder = ProcessBuilder()
            lateinit var process: Process

            coroutineScope.launch(Dispatchers.IO) {
                var doFind = true

                while (doFind) {
                    Logger.print("WebpHandler watcher do take")
                    val key = watcher.take()
                    key.pollEvents()
                        .filter { it.kind() != StandardWatchEventKinds.OVERFLOW }
                        .map { (it as WatchEvent<Path>).context() }
                        .find {
                            Logger.print("WebpHandler take ${it.toAbsolutePath()}")
                            it.fileName.toString() == targetFileName
                        }
                        ?.let {
                            val absolutePath = directoryPath.resolve(it.toString())
                            Logger.print("WebpHandler observeImageCreated path = $absolutePath")
                            watchKey.cancel()
                            process.waitFor()
                            continuation.resume(absolutePath)
                            doFind = false
                        } ?: key.reset()
                }
            }

            Logger.print("WebpHandler execute cmd")
            process = processBuilder.command(*cmd)
                .start()

            process.inputStream
                .source()
                .buffer()
                .use {
                    while (it.exhausted().not()) {
                        Logger.print("WebpHandler process ${it.readString(Charset.defaultCharset())}")
                    }
                    Logger.print("WebpHandler process exhausted")
                }

            val exit = process.waitFor()
            Logger.print("WebpHandler process finish $exit")
        }
    }

    abstract fun createCmdLog(cmd: Array<String>): String
}