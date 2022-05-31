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

object PngquantHelper {
    val coroutineScope = CoroutineScope(SupervisorJob())

    suspend fun run(filePath: Path) = suspendCancellableCoroutine<Path> { continuation ->
        val osName = System.getProperty("os.name")
            .lowercase()
//        osName.startsWith("windows")
        println("PngquantHelper run")
        val exePath = copyExe(filePath)
        val cmd = String.format("%s \"%s\"", exePath.toString(), filePath.toString())
        println("PngquantHelper cmd = $cmd")
        observeImageCreated(filePath) {
            println("PngquantHelper observeImageCreated path = $it")
            coroutineScope.launch(Dispatchers.IO) {
                delay(100)
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
                        println("observeImageCreated path = $it")
                        watchKey.cancel()
                        onFileCreated(it)
                        doFind = false
                    } ?: key.reset()
            }
        }
    }

    private fun copyExe(filePath: Path): Path {
        val tempFilePath = filePath.parent.resolve("test.exe")
        javaClass.getResourceAsStream("/pngquant.exe")
            ?.let {
                println("PngquantHelper read file successfully")
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
                                println("PngquantHelper write file successfully, path = $tempFilePath")
                            }
                    }
            }

        return tempFilePath
    }
}