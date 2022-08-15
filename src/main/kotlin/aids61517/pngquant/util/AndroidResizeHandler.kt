package aids61517.pngquant.util

import kotlinx.coroutines.*
import java.awt.Image
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.ceil

object AndroidResizeHandler {
    suspend fun run(filePathList: List<Path>, coroutineScope: CoroutineScope): List<Path> {
        return filePathList.map { dispatchResizeJob(it, coroutineScope) }
            .awaitAll()
            .flatten()
    }

    private fun dispatchResizeJob(filePath: Path, coroutineScope: CoroutineScope): Deferred<List<Path>> {
        return coroutineScope.async(Dispatchers.IO) {
            buildList {
                val fileName = filePath.fileName.toString().run { substring(0, lastIndexOf(".")) }
                val directoryPath = filePath.parent.resolve(fileName)
                if (Files.notExists(directoryPath)) {
                    Files.createDirectories(directoryPath)
                }

                add(
                    doResize(
                        originFilePath = filePath,
                        directoryPath = directoryPath,
                        androidDirectoryName = "drawable-mdpi",
                        ratio = 1.0,
                        coroutineScope = coroutineScope,
                    )
                )

                add(
                    doResize(
                        originFilePath = filePath,
                        directoryPath = directoryPath,
                        androidDirectoryName = "drawable-hdpi",
                        ratio = 1.5,
                        coroutineScope = coroutineScope,
                    )
                )

                add(
                    doResize(
                        originFilePath = filePath,
                        directoryPath = directoryPath,
                        androidDirectoryName = "drawable-xhdpi",
                        ratio = 2.0,
                        coroutineScope = coroutineScope,
                    )
                )

                add(
                    doResize(
                        originFilePath = filePath,
                        directoryPath = directoryPath,
                        androidDirectoryName = "drawable-xxhdpi",
                        ratio = 3.0,
                        coroutineScope = coroutineScope,
                    )
                )

                add(
                    doResize(
                        originFilePath = filePath,
                        directoryPath = directoryPath,
                        androidDirectoryName = "drawable-xxxhdpi",
                        ratio = 4.0,
                        coroutineScope = coroutineScope,
                    )
                )
            }.awaitAll()
        }
    }

    private fun doResize(
        originFilePath: Path,
        directoryPath: Path,
        androidDirectoryName: String,
        ratio: Double,
        coroutineScope: CoroutineScope
    ): Deferred<Path> {
        return coroutineScope.async(Dispatchers.IO) {
            val androidDirectoryPath = directoryPath.resolve(androidDirectoryName)
            if (Files.notExists(androidDirectoryPath)) {
                Files.createDirectories(androidDirectoryPath)
            }

            val targetFilePath = androidDirectoryPath.resolve(originFilePath.fileName)
            Files.deleteIfExists(targetFilePath)
            val scaleRatio = ratio / 4
            val inputImage = ImageIO.read(Files.newInputStream(originFilePath))
            val targetWidth = ceil(scaleRatio * inputImage.width).toInt()
            val targetHeight = ceil(scaleRatio * inputImage.height).toInt()
            val scaledImage = inputImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT)

            val imageType = when (inputImage.type) {
                BufferedImage.TYPE_BYTE_INDEXED -> BufferedImage.TYPE_4BYTE_ABGR
                else -> inputImage.type
            }
            val outputImage = BufferedImage(targetWidth, targetHeight, imageType).apply {
                createGraphics().apply {
                    drawImage(scaledImage, 0, 0, targetWidth, targetHeight, null)
                    dispose()
                }
            }
            Files.newOutputStream(targetFilePath)
                .use { ImageIO.write(outputImage, "PNG", it) }

            targetFilePath
        }
    }
}