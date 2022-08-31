package aids61517.pngquant.util

import kotlinx.coroutines.*
import org.apache.batik.anim.dom.SAXSVGDocumentFactory
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import org.apache.batik.util.XMLResourceDescriptor
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.absolutePathString
import kotlin.math.ceil

object AndroidResizeHandler {
    suspend fun run(
        filePathList: List<Path>,
        resizePngForAndroid: Boolean,
        coroutineScope: CoroutineScope,
    ): List<Path> {
        return filePathList.map { dispatchResizeJobAsync(it, resizePngForAndroid, coroutineScope) }
            .awaitAll()
            .flatten()
    }

    private fun dispatchResizeJobAsync(
        filePath: Path,
        resizePngForAndroid: Boolean,
        coroutineScope: CoroutineScope,
    ): Deferred<List<Path>> {
        return coroutineScope.async(Dispatchers.IO) {
            if (resizePngForAndroid.not() && filePath.isPng) {
                return@async listOf(filePath)
            }

            buildList {
                val fileName = filePath.fileName.toString().run { substring(0, lastIndexOf(".")) }
                val directoryPath = filePath.parent.resolve(fileName)
                if (Files.notExists(directoryPath)) {
                    Files.createDirectories(directoryPath)
                }

                add(
                    doResizeAsync(
                        originFilePath = filePath,
                        directoryPath = directoryPath,
                        androidDirectoryName = "drawable-mdpi",
                        ratio = 1.0,
                        coroutineScope = coroutineScope,
                    )
                )

                add(
                    doResizeAsync(
                        originFilePath = filePath,
                        directoryPath = directoryPath,
                        androidDirectoryName = "drawable-hdpi",
                        ratio = 1.5,
                        coroutineScope = coroutineScope,
                    )
                )

                add(
                    doResizeAsync(
                        originFilePath = filePath,
                        directoryPath = directoryPath,
                        androidDirectoryName = "drawable-xhdpi",
                        ratio = 2.0,
                        coroutineScope = coroutineScope,
                    )
                )

                add(
                    doResizeAsync(
                        originFilePath = filePath,
                        directoryPath = directoryPath,
                        androidDirectoryName = "drawable-xxhdpi",
                        ratio = 3.0,
                        coroutineScope = coroutineScope,
                    )
                )

                add(
                    doResizeAsync(
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

    private fun doResizeAsync(
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

            val targetFilePath = originFilePath.fileName
                .run {
                    if (isSvg) {
                        directoryPath.fileName.toString() + ".png"
                    } else {
                        toString()
                    }
                }
                .let { androidDirectoryPath.resolve(it) }
            Files.deleteIfExists(targetFilePath)
            if (originFilePath.isSvg) {
                doSvgResize(
                    originFilePath = originFilePath,
                    targetFilePath = targetFilePath,
                    ratio = ratio,
                )
            } else {
                doPngResize(
                    originFilePath = originFilePath,
                    targetFilePath = targetFilePath,
                    ratio = ratio,
                )
            }
        }
    }

    private fun doSvgResize(
        originFilePath: Path,
        targetFilePath: Path,
        ratio: Double,
    ): Path {
        val inputPath = File(originFilePath.absolutePathString()).toURI().toString()
        val parser = XMLResourceDescriptor.getXMLParserClassName()
        val document = SAXSVGDocumentFactory(parser)
            .createDocument(inputPath)
        val element = document.documentElement
        val width = element.getAttribute("width")
        val height = element.getAttribute("height")
        println("inputPath = $inputPath")
        println("width = $width")
        println("height = $height")

        val widthInt = when {
            width.contains("px") -> width.replace("px", "").toInt()
            else -> width.toInt()
        }

        val heightInt = when {
            height.contains("px") -> height.replace("px", "").toInt()
            else -> width.toInt()
        }

        val pngTranscorder = PNGTranscoder().apply {
            addTranscodingHint(PNGTranscoder.KEY_WIDTH, widthInt * ratio.toFloat())
            addTranscodingHint(PNGTranscoder.KEY_HEIGHT, heightInt * ratio.toFloat())
        }
        val input = TranscoderInput(inputPath)
        FileOutputStream(targetFilePath.absolutePathString())
            .use {
                val output = TranscoderOutput(it)
                output.document
                pngTranscorder.transcode(input, output)
            }

        return targetFilePath
    }

    private fun doPngResize(
        originFilePath: Path,
        targetFilePath: Path,
        ratio: Double,
    ): Path {
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

        return targetFilePath
    }
}

private val Path.isSvg: Boolean
    get() = fileName.toString().endsWith(".svg")

private val Path.isPng: Boolean
    get() {
        val suffix = absolutePathString().run { substring(lastIndexOf(".")).lowercase() }
        return suffix == ".png"
    }