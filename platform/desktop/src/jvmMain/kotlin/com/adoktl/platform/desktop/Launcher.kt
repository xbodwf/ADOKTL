package com.adoktl.platform.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.adoktl.ui.ADOKTLApp
import com.adoktl.ui.FilePickResult
import com.adoktl.util.DebugLog
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.system.exitProcess

fun main() = application {
    var levelJson by remember { mutableStateOf<String?>(null) }
    var levelTitle by remember { mutableStateOf("") }

    Window(
        onCloseRequest = ::exitApplication,
        title = "ADOKTL - A Dance of Fire and Ice",
        state = rememberWindowState(size = DpSize(960.dp, 700.dp))
    ) {
        if (levelJson != null) {
            DesktopGameWindow(
                levelJson = levelJson!!,
                levelTitle = levelTitle,
                onBack = {
                    levelJson = null
                    levelTitle = ""
                }
            )
        } else {
            ADOKTLApp(
                filePickerButton = { onResult ->
                    DesktopFilePickerButton(onResult = onResult)
                },
                gameViewFactory = { json ->
                    LaunchedEffect(json) {
                        levelJson = json
                        levelTitle = "Selected Level"
                    }
                }
            )
        }
    }
}

@Composable
fun DesktopFilePickerButton(onResult: (FilePickResult) -> Unit) {
    Button(
        onClick = {
            val chooser = JFileChooser().apply {
                dialogTitle = "Select ADOFAI Level"
                fileFilter = FileNameExtensionFilter("ADOFAI Level Files (*.adofai)", "adofai")
                fileSelectionMode = JFileChooser.FILES_ONLY
                currentDirectory = File(System.getProperty("user.home"))
            }

            val result = chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                val file = chooser.selectedFile
                try {
                    val json = file.readText()
                    val title = file.name
                    onResult(FilePickResult(json, title))
                    DebugLog.log("Loaded level: ${file.absolutePath}")
                } catch (e: Exception) {
                    DebugLog.log("Failed to load level: ${e.message}")
                }
            }
        },
        modifier = Modifier.width(220.dp).height(48.dp)
    ) {
        Text("Browse Files", fontSize = 16.sp)
    }
}

@Composable
fun DesktopGameWindow(
    levelJson: String,
    levelTitle: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Playing: $levelTitle",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Game will launch in a separate LWJGL window.\nClose it to return here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    launchLwjglGame(levelJson)
                    onBack()
                },
                modifier = Modifier.width(200.dp).height(48.dp)
            ) {
                Text("Launch Game", fontSize = 16.sp)
            }
        }
    }
}

fun launchLwjglGame(levelJson: String) {
    Thread {
        try {
            DebugLog.log("Starting LWJGL game window...")
            GameLauncher.start(levelJson)
        } catch (e: Exception) {
            DebugLog.log("LWJGL game error: ${e.message}")
        }
    }.apply {
        isDaemon = true
        name = "LWJGL-Game-Thread"
        start()
    }
}