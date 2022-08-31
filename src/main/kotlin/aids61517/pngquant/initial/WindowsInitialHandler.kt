package aids61517.pngquant.initial

import aids61517.pngquant.MainWindow
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class WindowsInitialHandler : InitialHandler() {
    companion object {
        private const val PNGQUANT_LINK =
            "https://github.com/aids61517/Pngquant_Webp/raw/develop/resources/windows/pngquant_2.17.0.exe"
        private const val WEBP_LINK =
            "https://github.com/aids61517/Pngquant_Webp/raw/develop/resources/windows/cwebp_windows_1.2.2.exe"
        private const val APP_PATH_FORMAT =
            "C:\\Users\\%s\\AppData\\Local\\PngquantWebp"
        private const val PNGQUANT_FILE_NAME = "pngquant.exe"
        private const val WEBP_FILE_NAME = "webp.exe"
    }

    override val pngquantPath: Path
        get() = Paths.get(String.format(APP_PATH_FORMAT, userName)).resolve(PNGQUANT_FILE_NAME)

    override val webpPath: Path
        get() = Paths.get(String.format(APP_PATH_FORMAT, userName)).resolve(WEBP_FILE_NAME)

    override fun initialize(coroutineScope: CoroutineScope, appState: MutableState<MainWindow.State>) {
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

            val webpJob = launch(Dispatchers.IO) {
                downloadFileIfNeed(
                    downloadUrl = WEBP_LINK,
                    filePath = webpPath,
                )
            }

            pngquantJob.join()
            webpJob.join()
            appState.value = originAppState
        }
    }
}