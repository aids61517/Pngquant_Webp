package aids61517.pngquant.webp

import aids61517.pngquant.OSSourceChecker
import aids61517.pngquant.data.ExePath
import aids61517.pngquant.util.CopyFileHandler
import kotlinx.coroutines.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolute

class WindowsWebpHandler(coroutineScope: CoroutineScope) : WebpHandler(coroutineScope) {
    companion object {
        private val EXE_PATH = ExePath(
            source = "/windows/cwebp_windows_1.2.2.exe",
            targetPath = "cwebp-windows.exe",
        )
    }

    private val copyFileHandler by lazy {
        CopyFileHandler.create(OSSourceChecker.osSource)
    }

    override suspend fun run(filePathList: List<Path>): List<Path> {
        return withContext(Dispatchers.IO) {
            val exePath = createExeFile()
            val createdFileList = filePathList.map {
                coroutineScope.async(Dispatchers.IO) {
                    val cmd = createCmd(exePath, it)
                    executeCmdAndGetImageCreated(cmd, it)
                }
            }.awaitAll()

            coroutineScope.launch(Dispatchers.IO) {
//                filePathList.forEach { Files.deleteIfExists(it) }
                delay(200)
                Files.deleteIfExists(exePath)
            }

            createdFileList
        }
    }

    private fun createExeFile(): Path {
        println("WebpHelper createExeFile")
        return copyFileHandler.execute(EXE_PATH)
    }

    private fun createCmd(exePath: Path, filePath: Path): Array<String> {
        val outputFileName = filePath.absolute()
            .toString()
            .replace(".png", ".webp")
        return arrayOf(
            exePath.absolute().toString(),
            "-lossless",
            filePath.absolute().toString(),
            "-o",
            outputFileName,
        )
    }
}