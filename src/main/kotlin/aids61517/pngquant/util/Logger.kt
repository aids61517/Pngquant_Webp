package aids61517.pngquant.util

import aids61517.pngquant.LogPrinter

object Logger {
    private lateinit var logPrinter: LogPrinter

    fun init(logPrinter: LogPrinter) {
        this.logPrinter = logPrinter
    }

    fun print(log: String) {
        logPrinter.print(log)
    }
}