package aids61517.pngquant.initial

import aids61517.pngquant.MainWindow
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.CoroutineScope
import java.nio.file.Path

class MacInitialHandler : InitialHandler() {
    override val pngquantPath: Path
        get() = TODO("Not yet implemented")

    override val webpPath: Path?
        get() = null

    override fun initialize(coroutineScope: CoroutineScope, appState: MutableState<MainWindow.State>) {

    }
}