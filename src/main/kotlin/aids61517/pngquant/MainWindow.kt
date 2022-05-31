// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package aids61517.pngquant

import aids61517.pngquant.core.Application
import aids61517.pngquant.core.BaseWindow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okio.buffer
import okio.sink
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class MainWindow : BaseWindow() {
    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            if (args.isEmpty()) {
                MainWindow().show()

                application {
                    val application = remember { Application }

                    for (window in application.windows) {
                        key(window) {
                            window.startWindow(this)
                        }
                    }
                }
            } else {
                val paths = Paths.get("test.txt")
                if (Files.notExists(paths)) {
                    Files.createFile(paths)
                }

                Files.newOutputStream(paths)
                    .sink()
                    .buffer()
                    .use {
                        it.writeString(args.joinToString(separator = ","), Charset.defaultCharset())
                    }
            }
        }
    }

    @Composable
    override fun ApplicationScope.setupWindow() {
        Window(
            onCloseRequest = ::exitApplication,
            title = "PngquantWebp",
            state = rememberWindowState(
                width = 300.dp,
                height = 200.dp,
            )
        ) {
            MaterialTheme {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(10.dp)
                ) {
                    val coroutineScope = rememberCoroutineScope()
                    Button(
                        onClick = {
                            chooseFile()?.let {
                                coroutineScope.launch {
                                    println("file path = $it")
                                    val pngquantPath = PngquantHelper.run(it)
                                    println("pngquantPath = $pngquantPath")
                                    WebpHelper.run(pngquantPath)
                                }
                            }
                        },
                        border = BorderStroke(1.dp, Color.Magenta),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                        contentPadding = PaddingValues(0.dp, 0.dp),
                    ) {
                        Text(
                            text = "選擇要轉換的檔案",
                            modifier = Modifier.padding(horizontal = 10.dp),
                        )
                    }
                }


                DisposableEffect(Unit) {
                    this.onDispose {
                        println("onDispose")
                        PngquantHelper.coroutineScope.cancel()
                        WebpHelper.coroutineScope.cancel()
                    }
                }
            }
        }
    }

    private fun chooseFile(): Path? {
        val fileChooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("*.png", "png")
        }
        return when(val returnValue = fileChooser.showOpenDialog(null)) {
            JFileChooser.APPROVE_OPTION -> {
                println("returnValue = $returnValue")
                Paths.get(fileChooser.selectedFile.absolutePath)
            }
            else -> {
                println("returnValue = $returnValue")
                null
            }
        }
    }
}