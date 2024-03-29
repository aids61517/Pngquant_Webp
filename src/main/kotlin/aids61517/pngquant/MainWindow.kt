// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package aids61517.pngquant

import aids61517.pngquant.core.Application
import aids61517.pngquant.core.BaseWindow
import aids61517.pngquant.data.OSSource
import aids61517.pngquant.initial.InitialHandler
import aids61517.pngquant.util.AndroidResizeHandler
import aids61517.pngquant.util.Logger
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
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
import kotlin.io.path.absolute

class MainWindow : BaseWindow(), LogPrinter {
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

    enum class State {
        IDLE,
        INITIAL,
        PROCESSING_RESIZE_FOR_ANDROID,
        PROCESSING_PNGQUANT,
        PROCESSING_WEBP,
        FINISHED,
        WEBP_UNAVAILABLE,
    }

    private val logState = mutableStateOf("")

    private val appState = mutableStateOf<State>(State.IDLE)

    private val isInitialize = mutableStateOf(false)

    private val initialHandler by lazy { InitialHandler.create(OSSourceChecker.osSource) }

    private var logBuilder = StringBuilder()

    private var lastChooseDirectoryPath: Path? = null

    init {
        Logger.init(this)
    }

    @Composable
    override fun ApplicationScope.setupWindow() {
        Window(
            onCloseRequest = ::exitApplication,
            title = "PngquantWebp",
            state = rememberWindowState(
                width = 1200.dp,
                height = 1000.dp,
            )
        ) {
            MaterialTheme {
                Column(
                    modifier = Modifier.padding(10.dp)
                        .fillMaxHeight(),
                ) {
                    var state by remember {
                        appState.apply {
                            value = if (WebpHelper.isWebpAvailable) State.IDLE else State.WEBP_UNAVAILABLE
                        }
                    }
                    var deleteOriginFile by remember { mutableStateOf(true) }
                    var deletePngquantFile by remember { mutableStateOf(true) }
                    var skip9Patch by remember { mutableStateOf(true) }
                    var resizePngForAndroid by remember { mutableStateOf(true) }
                    val isInitialize by remember { isInitialize }
                    val coroutineScope = rememberCoroutineScope()

                    if (isInitialize.not()) {
                        initialize(coroutineScope)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "狀態：",
                            modifier = Modifier.padding(start = 10.dp),
                        )

                        val stateText = when (state) {
                            State.IDLE -> "閒置"
                            State.INITIAL -> "正在下載所需檔案"
                            State.PROCESSING_RESIZE_FOR_ANDROID -> "多尺寸處理中"
                            State.PROCESSING_PNGQUANT -> "Pngquant 處理中"
                            State.PROCESSING_WEBP -> "Webp 處理中"
                            State.FINISHED -> "已完成"
                            State.WEBP_UNAVAILABLE -> "webp 無法使用"
                        }

                        val stateColor = if (state == State.WEBP_UNAVAILABLE) {
                            Color.Red
                        } else {
                            Color.Unspecified
                        }
                        Text(
                            text = stateText,
                            color = stateColor,
                        )

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                                .offset(x = (-15).dp),
                        ) {
                            Button(
                                onClick = {
                                    chooseFile(skip9Patch)?.takeIf { it.isNotEmpty() }
                                        ?.let {
                                            lastChooseDirectoryPath = it.first()
                                                .parent
                                            coroutineScope.launch {
                                                state = State.PROCESSING_RESIZE_FOR_ANDROID
                                                val resizePathList =
                                                    handleResizeForAndroid(it, resizePngForAndroid, coroutineScope)
                                                state = State.PROCESSING_PNGQUANT
                                                val pngquantPathList = handlePngquant(resizePathList, deleteOriginFile)
                                                state = State.PROCESSING_WEBP
                                                handleWebp(pngquantPathList, deletePngquantFile)
                                                print("handle finish.")
                                                state = State.FINISHED
                                            }
                                        }
                                },
                                border = BorderStroke(1.dp, Color.Magenta),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                                contentPadding = PaddingValues(0.dp, 0.dp),
                                enabled = when (state) {
                                    State.INITIAL,
                                    State.PROCESSING_PNGQUANT,
                                    State.PROCESSING_WEBP,
                                    State.WEBP_UNAVAILABLE -> false
                                    else -> true
                                },
                            ) {
                                Text(
                                    text = "選擇要轉換的檔案",
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { deleteOriginFile = !deleteOriginFile }
                                .padding(end = 10.dp)
                        ) {
                            Checkbox(
                                checked = deleteOriginFile,
                                onCheckedChange = { deleteOriginFile = it }
                            )

                            Text("刪除原始檔案或 Android 多尺寸 Png")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { deletePngquantFile = !deletePngquantFile }
                                .padding(end = 10.dp)
                        ) {
                            Checkbox(
                                checked = deletePngquantFile,
                                onCheckedChange = { deletePngquantFile = it }
                            )

                            Text("刪除 Pngquant 產生的檔案")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { skip9Patch = !skip9Patch }
                                .padding(end = 10.dp)
                        ) {
                            Checkbox(
                                checked = skip9Patch,
                                onCheckedChange = { skip9Patch = it }
                            )

                            Text("不處理 9 patch png")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { resizePngForAndroid = !resizePngForAndroid }
                                .padding(end = 10.dp)
                        ) {
                            Checkbox(
                                checked = resizePngForAndroid,
                                onCheckedChange = { resizePngForAndroid = it }
                            )

                            Text("產出 Android 的各尺寸圖(請選擇 4 倍圖)")
                        }
                    }

                    if (OSSourceChecker.osSource == OSSource.MAC && WebpHelper.isWebpAvailable.not()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp),
                        ) {
                            Text(
                                buildAnnotatedString {
                                    append("Mac 須先透過 ")
                                    withStyle(style = SpanStyle(color = Color.Blue)) {
                                        append("brew install webp")
                                    }
                                    append(" 安裝")
                                }
                            )
                        }
                    }

                    Text(
                        text = "以下為 log 區，可以 scroll",
                        color = Color.Blue,
                    )

                    val logState by remember { logState }
                    val scrollState = rememberScrollState(0)
                    SelectionContainer {
                        Text(
                            text = logState,
                            modifier = Modifier.fillMaxHeight()
                                .verticalScroll(scrollState),
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

    private fun initialize(coroutineScope: CoroutineScope) {
        if (appState.value != State.IDLE && isInitialize.value) return
        isInitialize.value = true
        initialHandler.initialize(coroutineScope, appState)
    }

    private suspend fun handleResizeForAndroid(
        filePathList: List<Path>,
        resizePngForAndroid: Boolean,
        coroutineScope: CoroutineScope,
    ): List<Path> {
        print("file path = $filePathList")
        return AndroidResizeHandler.run(
            filePathList = filePathList,
            resizePngForAndroid = resizePngForAndroid,
            coroutineScope = coroutineScope,
        ).also { print("resizePathList = $it") }
    }

    private suspend fun handlePngquant(
        filePathList: List<Path>,
        deleteOriginFile: Boolean,
    ): List<Path> {
        print("file path = $filePathList")
        return PngquantHelper.run(
            filePathList = filePathList,
            deleteOriginFile = deleteOriginFile,
            initialHandler.pngquantPath,
        ).also { print("pngquantPathList = $it") }
    }

    private suspend fun handleWebp(
        filePathList: List<Path>,
        deletePngquantFile: Boolean,
    ): List<Path> {
        print("file path = $filePathList")
        return WebpHelper.run(
            filePathList = filePathList,
            deletePngquantFile = deletePngquantFile,
            initialHandler.webpPath,
        )
    }

    override fun print(log: String) {
        println(log)
        logBuilder.append("\n")
            .append(log)
        logState.value = logBuilder.toString()
    }

    private fun chooseFile(skip9patch: Boolean): List<Path>? {
        val fileChooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("png 和 svg", "png", "svg")
            isMultiSelectionEnabled = true
            currentDirectory = lastChooseDirectoryPath?.toFile() ?: File(Paths.get("").absolute().toString())
        }
        return when (val returnValue = fileChooser.showOpenDialog(null)) {
            JFileChooser.APPROVE_OPTION -> {
                fileChooser.selectedFiles
                    .map { Paths.get(it.absolutePath) }
                    .filter {
                        if (skip9patch) {
                            it.fileName.toString().endsWith(".9.png").not()
                        } else {
                            true
                        }
                    }
            }
            else -> {
                null
            }
        }
    }
}