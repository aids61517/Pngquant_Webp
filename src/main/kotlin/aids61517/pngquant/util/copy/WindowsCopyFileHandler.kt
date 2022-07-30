package aids61517.pngquant.util.copy

import aids61517.pngquant.data.ExePath
import java.nio.file.Path

class WindowsCopyFileHandler: CopyFileHandler() {
    override fun execute(exePath: ExePath): Path {
        return copyExe(exePath)
    }
}