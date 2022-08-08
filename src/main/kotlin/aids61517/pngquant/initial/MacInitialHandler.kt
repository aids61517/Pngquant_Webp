package aids61517.pngquant.initial

import aids61517.pngquant.MainWindow
import aids61517.pngquant.util.Logger
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

class MacInitialHandler : InitialHandler() {
    companion object {
        private const val PNGQUANT_LINK =
            "https://github.com/aids61517/Pngquant_Webp/raw/develop/resources/mac/pngquant_2.17.0"
        private const val APP_PATH_FORMAT =
            "/Users/%s/Applications/PngquantToWebp"
        private const val PNGQUANT_FILE_NAME = "pngquant"
    }

    override val pngquantPath: Path
        get() = Paths.get(String.format(APP_PATH_FORMAT, userName)).resolve(PNGQUANT_FILE_NAME)

    override val webpPath: Path?
        get() = null

    override fun initialize(coroutineScope: CoroutineScope, appState: MutableState<MainWindow.State>) {
        Logger.print(pngquantPath.absolutePathString())
        coroutineScope.launch(Dispatchers.IO) {
            val originAppState = appState.value
            appState.value = MainWindow.State.INITIAL
            val directoryPath = Paths.get(String.format(APP_PATH_FORMAT, userName))
            Files.createDirectories(directoryPath)

            val pngquantJob = launch(Dispatchers.IO) {
                downloadFileIfNeed(
                    downloadUrl = PNGQUANT_LINK,
                    filePath = pngquantPath,
                )
            }

            pngquantJob.join()

            arrayOf(
                "/bin/sh",
                "-c",
                String.format("chmod 755 \"%s\"", pngquantPath),
            ).let { Runtime.getRuntime().exec(it) }

            appState.value = originAppState
        }
    }
}