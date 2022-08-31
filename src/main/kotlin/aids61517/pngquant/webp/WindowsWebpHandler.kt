package aids61517.pngquant.webp

import aids61517.pngquant.OSSourceChecker
import aids61517.pngquant.data.ExePath
import aids61517.pngquant.util.copy.CopyFileHandler
import aids61517.pngquant.util.Logger
import kotlinx.coroutines.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute

class WindowsWebpHandler(coroutineScope: CoroutineScope) : WebpHandler(coroutineScope) {
    companion object {
        private val EXE_PATH = ExePath(
            source = "/windows/cwebp_windows_1.2.2.exe",
            targetPath = "cwebp-windows.exe",
        )
    }

//    private val copyFileHandler by lazy {
//        CopyFileHandler.create(OSSourceChecker.osSource)
//    }

    override val isWebpAvailable: Boolean
        get() = true

    override suspend fun run(
        filePathList: List<Path>,
        deletePngquantFile: Boolean,
        webpExePath: Path?,
    ): List<Path> {
        webpExePath!!
        return withContext(Dispatchers.IO) {
            val createdFileList = filePathList.map {
                coroutineScope.async(Dispatchers.IO) {
                    val cmd = createCmd(webpExePath, it)
                    executeCmdAndGetImageCreated(cmd, it)
                }
            }.awaitAll()

            coroutineScope.launch(Dispatchers.IO) {
                if (deletePngquantFile) {
                    filePathList.forEach { Files.deleteIfExists(it) }
                }
//                delay(200)
//                Files.deleteIfExists(webpExePath)
            }

            createdFileList.map { origin ->
                origin.absolute().toString()
                    .replace("-fs8.webp", ".webp")
                    .let { Paths.get(it) }
                    .also {
                        Files.deleteIfExists(it)
                        Files.move(origin, it)
                    }
            }
        }
    }

//    private fun createExeFile(): Path {
//        Logger.print("WebpHelper createExeFile")
//        return copyFileHandler.execute(EXE_PATH)
//    }

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

    override fun createCmdLog(cmd: Array<String>): String {
        return cmd.joinToString(separator = " ")
    }
}