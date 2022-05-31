package aids61517.pngquant

import kotlinx.coroutines.*
import okio.buffer
import okio.sink
import okio.source
import java.nio.charset.Charset
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchEvent
import java.nio.file.WatchService
import kotlin.coroutines.resume
import kotlin.io.path.absolute

object WebpHelper {
    val coroutineScope = CoroutineScope(SupervisorJob())

    suspend fun run(filePath: Path) = suspendCancellableCoroutine<Path> { continuation ->
        val osName = System.getProperty("os.name")
            .lowercase()
//        osName.startsWith("windows")
        println("WebpHelper run")
        val exePath = copyExe(filePath)
        val outputFileName = filePath.toString().replace(".png", ".webp")
        val cmd = String.format("%s -lossless \"%s\" -o \"%s\"", exePath.toString(), filePath.toString(), outputFileName)
        println("WebpHelper cmd = $cmd")
        observeImageCreated(filePath) {
            println("WebpHelper observeImageCreated path = $it")
            coroutineScope.launch(Dispatchers.IO) {
                delay(200)
                Files.deleteIfExists(exePath)
            }
            continuation.resume(it)
        }

        Runtime.getRuntime()
            .exec(cmd)
    }

    private fun observeImageCreated(filePath: Path, onFileCreated: (Path) -> Unit) {
        val directoryPath = filePath.parent
        val watcher = FileSystems.getDefault().newWatchService()
        val watchKey = directoryPath.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
        coroutineScope.launch(Dispatchers.IO) {
            var doFind = true
            while (doFind) {
                val key = watcher.take()
                key.pollEvents()
                    .filter { it.kind() != OVERFLOW }
                    .map { (it as WatchEvent<Path>).context() }
                    .find { it.toString().endsWith("tmp").not() }
                    ?.let {
                        val absolutePath = directoryPath.resolve(it.toString())
                        println("observeImageCreated path = $absolutePath")
                        watchKey.cancel()
                        onFileCreated(absolutePath)
                        doFind = false
                    } ?: key.reset()
            }
        }
    }

    private fun copyExe(filePath: Path): Path {
        val tempFilePath = filePath.parent.resolve("webp.exe")
        javaClass.getResourceAsStream("/cwebp-windows-1.2.2.exe")
            ?.let {
                println("WebpHelper read file successfully")
                if (Files.notExists(tempFilePath)) {
                    Files.createFile(tempFilePath)
                }

                it.source()
                    .buffer()
                    .use { source ->
                        Files.newOutputStream(tempFilePath)
                            .sink()
                            .buffer()
                            .use { sink ->
                                sink.writeAll(source)
                                println("WebpHelper write file successfully, path = $tempFilePath")
                            }
                    }
            }

        return tempFilePath
    }
}