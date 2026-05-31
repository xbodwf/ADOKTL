package com.adoktl.platform.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.FileDialog
import androidx.compose.ui.window.FileDialogMode
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.adoktl.ui.ADOKTLApp
import com.adoktl.ui.ComposeGameView
import com.adoktl.ui.FilePickResult
import com.adoktl.ui.rememberPlayerEngine
import com.adoktl.util.DebugLog
import java.io.File

fun main() = application {
    val playerEngineRef = remember { mutableStateOf<com.adoktl.player.PlayerEngine?>(null) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "ADOKTL - A Dance of Fire and Ice",
        state = rememberWindowState(size = DpSize(960.dp, 700.dp)),
        onPreviewKeyEvent = { event ->
            if (event.key == Key.Spacebar || event.key == Key.Enter) {
                if (event.type == KeyEventType.KeyUp) {
                    playerEngineRef.value?.onPress()
                }
                true
            } else false
        }
    ) {
        ADOKTLApp(
            filePickerButton = { onResult ->
                DesktopFilePickerButton(onResult = onResult)
            },
            gameViewFactory = { levelJson ->
                val engine = rememberPlayerEngine(levelJson)
                LaunchedEffect(engine) { playerEngineRef.value = engine }
                DisposableEffect(engine) {
                    onDispose { playerEngineRef.value = null }
                }
                ComposeGameView(playerEngine = engine)
            }
        )
    }
}

@Composable
fun DesktopFilePickerButton(onResult: (FilePickResult) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Button(
        onClick = { showDialog = true },
        modifier = Modifier.width(220.dp).height(48.dp)
    ) {
        Text("Browse Files", fontSize = 16.sp)
    }

    if (showDialog) {
        FileDialog(
            onCloseRequest = { showDialog = false },
            onFileSelected = { file ->
                showDialog = false
                try {
                    val json = file.readText()
                    onResult(FilePickResult(json, file.name))
                    DebugLog.log("Loaded level: ${file.absolutePath}")
                } catch (e: Exception) {
                    DebugLog.log("Failed to load level: ${e.message}")
                }
            },
            title = "Select ADOFAI Level",
            mode = FileDialogMode.Open,
            initialDirectory = System.getProperty("user.home")
        )
    }
}