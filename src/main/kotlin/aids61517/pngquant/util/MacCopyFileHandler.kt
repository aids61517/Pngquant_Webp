package aids61517.pngquant.util

import aids61517.pngquant.data.ExePath
import java.nio.file.Path

class MacCopyFileHandler: CopyFileHandler() {
    override fun execute(exePath: ExePath): Path {
        return copyExe(exePath).apply {
            arrayOf(
                "/bin/sh",
                "-c",
                String.format("chmod 755 \"%s\"", exePath.targetPath),
            ).let { Runtime.getRuntime().exec(it) }
        }
    }
}