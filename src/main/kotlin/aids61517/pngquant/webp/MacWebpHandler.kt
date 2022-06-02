package aids61517.pngquant.webp

import kotlinx.coroutines.*
import java.nio.file.Path
import kotlin.io.path.absolute

class MacWebpHandler(coroutineScope: CoroutineScope) : WebpHandler(coroutineScope) {

    override suspend fun run(filePathList: List<Path>): List<Path> {
        return withContext(Dispatchers.IO) {
            val createdFileList = filePathList.map {
                coroutineScope.async(Dispatchers.IO) {
                    val cmd = createCmd(it)
                    executeCmdAndGetImageCreated(cmd, it)
                }
            }.awaitAll()

            coroutineScope.launch(Dispatchers.IO) {
//                filePathList.forEach { Files.deleteIfExists(it) }
            }

            createdFileList
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
                "cwebp -lossless \"%s\" -o \"%s\"",
                filePath.absolute().toString(),
                outputFileName,
            ),
        )
    }
}