package aids61517.pngquant

import kotlinx.coroutines.*
import okio.buffer
import okio.sink
import okio.source
import java.nio.charset.Charset
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import kotlin.coroutines.resume
import kotlin.io.path.absolute

object WebpHelper {
    val coroutineScope = CoroutineScope(SupervisorJob())

    suspend fun run(filePathList: List<Path>) = withContext(Dispatchers.IO) {
        val osName = System.getProperty("os.name")
            .lowercase()
//        osName.startsWith("windows")
        println("WebpHelper run, osName = $osName")
        val exePath = createExeFile()
        val createdFileList = filePathList.map {
            coroutineScope.async(Dispatchers.IO) {
                val outputFileName = it.toString().replace(".png", ".webp")
                val cmd =
                    String.format("%s -lossless \"%s\" -o \"%s\"", exePath.toString(), it.toString(), outputFileName)
                executeCmdAndGetImageCreated(cmd, it)
            }
        }.awaitAll()

        coroutineScope.launch(Dispatchers.IO) {
            filePathList.forEach { Files.deleteIfExists(it) }
            delay(200)
            Files.deleteIfExists(exePath)
        }

        createdFileList
    }

    private suspend fun executeCmdAndGetImageCreated(cmd: String, filePath: Path): Path {
        return suspendCancellableCoroutine { continuation ->
            val directoryPath = filePath.parent
            val watcher = FileSystems.getDefault().newWatchService()
            val watchKey = directoryPath.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
            Runtime.getRuntime()
                .exec(cmd)

            var doFind = true
            val targetFileName = filePath.fileName.toString()
                .replace(".png", ".webp")

            while (doFind) {
                val key = watcher.take()
                key.pollEvents()
                    .filter { it.kind() != OVERFLOW }
                    .map { (it as WatchEvent<Path>).context() }
                    .find { it.fileName.toString() == targetFileName }
                    ?.let {
                        val absolutePath = directoryPath.resolve(it.toString())
                        println("observeImageCreated path = $absolutePath")
                        watchKey.cancel()
                        continuation.resume(absolutePath)
                        doFind = false
                    } ?: key.reset()
            }
        }
    }

    private fun createExeFile(): Path {
        val exeFilePath = Paths.get("webp.exe")
        javaClass.getResourceAsStream("/cwebp-windows-1.2.2.exe")
            ?.let {
                println("WebpHelper read file successfully")
                if (Files.notExists(exeFilePath)) {
                    Files.createFile(exeFilePath)
                }

                it.source()
                    .buffer()
                    .use { source ->
                        Files.newOutputStream(exeFilePath)
                            .sink()
                            .buffer()
                            .use { sink ->
                                sink.writeAll(source)
                                println("WebpHelper create exe successfully, path = $exeFilePath")
                            }
                    }
            }

        return exeFilePath
    }
}