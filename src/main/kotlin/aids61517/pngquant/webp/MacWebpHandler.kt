package aids61517.pngquant.webp

import kotlinx.coroutines.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute

class MacWebpHandler(coroutineScope: CoroutineScope) : WebpHandler(coroutineScope) {
    companion object {
        val CWEP_PATH: Path?
            get() = cwebpPathList.map { Paths.get(it) }
                .find { Files.exists(it) }
                ?.let {
                    Files.newDirectoryStream(it)
                        .first()
                        .resolve("bin")
                        .resolve("cwebp")
                }

        private val cwebpPathList = listOf(
            "/usr/local/Cellar/webp/",
            "/opt/homebrew/Cellar/webp/",
        )
    }

    override suspend fun run(
        filePathList: List<Path>,
        deletePngquantFile: Boolean,
    ): List<Path> {
        return withContext(Dispatchers.IO) {
            val createdFileList = filePathList.map {
                coroutineScope.async(Dispatchers.IO) {
                    val cmd = createCmd(it)
                    executeCmdAndGetImageCreated(cmd, it)
                }
            }.awaitAll()

            coroutineScope.launch(Dispatchers.IO) {
                if (deletePngquantFile) {
                    filePathList.forEach { Files.deleteIfExists(it) }
                }
            }

            createdFileList.map { origin ->
                origin.absolute().toString()
                    .replace("-fs8.webp", ".webp")
                    .let { Paths.get(it) }
                    .also {
                        Files.move(origin, it)
                    }
            }
        }
    }

    private fun createCmd(filePath: Path): Array<String> {
        val outputFileName = filePath.absolute()
            .toString()
            .replace(".png", ".webp")
        return arrayOf(
            "/bin/sh",
            "-c",
            String.format(
                "\"%s\" -lossless \"%s\" -o \"%s\"",
                CWEP_PATH!!.absolute().toString(),
                filePath.absolute().toString(),
                outputFileName,
            ),
        )
    }

    override fun createCmdLog(cmd: Array<String>): String {
        return cmd.last()
    }
}