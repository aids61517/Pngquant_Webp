package aids61517.pngquant.webp

import aids61517.pngquant.data.OSSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.buffer
import okio.source
import java.nio.charset.Charset
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
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
            val watcher = FileSystems.getDefault().newWatchService()
            val watchKey = directoryPath.register(
                watcher,
                StandardWatchEventKinds.ENTRY_CREATE,
            )
            println("cmd = ${cmd.last()}")
            val process = Runtime.getRuntime()
                .exec(cmd)

            var doFind = true
            val targetFileName = filePath.fileName.toString()
                .replace(".png", ".webp")

            while (doFind) {
                val key = watcher.take()
                key.pollEvents()
                    .filter { it.kind() != StandardWatchEventKinds.OVERFLOW }
                    .map { (it as WatchEvent<Path>).context() }
                    .find { it.fileName.toString() == targetFileName }
                    ?.let {
                        val absolutePath = directoryPath.resolve(it.toString())
                        println("WebpHandler observeImageCreated path = $absolutePath")
                        watchKey.cancel()
                        process.inputStream
                            .source()
                            .buffer()
                            .use {
                                while (it.exhausted().not()) {
                                    println(it.readString(Charset.defaultCharset()))
                                }
                            }
                        continuation.resume(absolutePath)
                        doFind = false
                    } ?: key.reset()
            }
        }
    }
}