package aids61517.pngquant

import kotlinx.coroutines.*
import okio.buffer
import okio.sink
import okio.source
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import kotlin.coroutines.resume

object PngquantHelper {
    val coroutineScope = CoroutineScope(SupervisorJob())

    suspend fun run(filePathList: List<Path>) = withContext(Dispatchers.IO) {
        val osName = System.getProperty("os.name")
            .lowercase()
//        osName.startsWith("windows")
        println("PngquantHelper run, osName = $osName")
        val exePath = createExeFile()
        val createdFileList = filePathList.map {
            coroutineScope.async(Dispatchers.IO) {
                val cmd = String.format("%s \"%s\"", exePath.toString(), it.toString())
                executeCmdAndGetImageCreated(cmd, it)
            }
        }.awaitAll()

        coroutineScope.launch(Dispatchers.IO) {
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
                .replace(".png", "")
                .let { String.format("%s-fs8.png", it) }

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
        val exeFilePath = Paths.get("pngquant.exe")
        javaClass.getResourceAsStream("/pngquant.exe")
            ?.let {
                println("PngquantHelper read file successfully")
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
                                println("PngquantHelper create exe successfully, path = $exeFilePath")
                            }
                    }
            }

        return exeFilePath
    }
}