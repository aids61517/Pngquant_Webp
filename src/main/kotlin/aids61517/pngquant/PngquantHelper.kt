package aids61517.pngquant

import aids61517.pngquant.data.ExePath
import aids61517.pngquant.data.OSSource
import aids61517.pngquant.util.copy.CopyFileHandler
import aids61517.pngquant.util.Logger
import kotlinx.coroutines.*
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import kotlin.coroutines.resume
import kotlin.io.path.absolute

object PngquantHelper {
    val coroutineScope = CoroutineScope(SupervisorJob())

//    private val copyFileHandler by lazy {
//        CopyFileHandler.create(OSSourceChecker.osSource)
//    }

    suspend fun run(
        filePathList: List<Path>,
        deleteOriginFile: Boolean,
        pngquantPath: Path,
    ) = withContext(Dispatchers.IO) {
        val createdFileList = filePathList.map {
            coroutineScope.async(Dispatchers.IO) {
                val cmd = createCmd(pngquantPath, it)
                executeCmdAndGetImageCreated(cmd, it)
            }
        }.awaitAll()

        coroutineScope.launch(Dispatchers.IO) {
            if (deleteOriginFile) {
                filePathList.forEach { Files.deleteIfExists(it) }
            }
        }

        createdFileList
    }

    private suspend fun executeCmdAndGetImageCreated(cmd: Array<String>, filePath: Path): Path {
        return suspendCancellableCoroutine { continuation ->
            val directoryPath = filePath.parent
            val targetFileName = filePath.fileName.toString()
                .replace(".png", "-fs8.png")
            val targetPath = directoryPath.resolve(targetFileName)
            Files.deleteIfExists(targetPath)

            val watcher = FileSystems.getDefault().newWatchService()
            val watchKey = directoryPath.register(watcher, ENTRY_CREATE, ENTRY_MODIFY)
            val processBuilder = ProcessBuilder().apply {
                command(*cmd)
            }
            val process = processBuilder.start()

            var doFind = true

            while (doFind) {
                val key = watcher.take()
                key.pollEvents()
                    .filter { it.kind() != OVERFLOW }
                    .map { (it as WatchEvent<Path>).context() }
                    .find { it.fileName.toString() == targetFileName }
                    ?.let {
                        val absolutePath = directoryPath.resolve(it.toString())
                        Logger.print("observeImageCreated path = $absolutePath")
                        watchKey.cancel()
                        process.waitFor()
                        continuation.resume(absolutePath)
                        doFind = false
                    } ?: key.reset()
            }
        }
    }

//    private fun createExeFile(): Path {
//        return copyFileHandler.execute(getExeSource(OSSourceChecker.osSource))
//    }

    private fun getExeSource(osSource: OSSource): ExePath {
        return when (osSource) {
            OSSource.WINDOWS -> ExePath(
                source = "/windows/pngquant_2.17.0.exe",
                targetPath = "pngquant_2.17.0.exe",
            )
            OSSource.MAC -> ExePath(
                source = "/mac/pngquant_2.17.0",
                targetPath = "pngquant",
            )
            else -> throw IllegalStateException("Unsupported OS")
        }
    }

    private fun createCmd(exePath: Path, filePath: Path): Array<String> {
        return when (OSSourceChecker.osSource) {
            OSSource.WINDOWS -> arrayOf(exePath.toString(), filePath.toString())
            OSSource.MAC -> arrayOf(
                "/bin/sh",
                "-c",
                String.format("exec \"%s\" \"%s\"", exePath.absolute().toString(), filePath.toString()),
            )
            else -> throw IllegalStateException("Unsupported OS")
        }
    }
}