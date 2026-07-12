package com.adoktl.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

sealed class Screen {
    data object Welcome : Screen()
    data object LevelSelect : Screen()
    data class Game(val levelJson: String, val title: String = "", val baseUri: String? = null) : Screen()
}

@Composable
fun ADOKTLApp(
    initialLevelJson: String? = null,
    initialTitle: String = "",
    filePickerButton: @Composable (onResult: (FilePickResult) -> Unit) -> Unit = {},
    gameViewFactory: @Composable (levelJson: String) -> Unit = {}
) {
    var currentScreen by remember {
        mutableStateOf<Screen>(
            if (initialLevelJson != null) Screen.Game(initialLevelJson, initialTitle)
            else Screen.Welcome
        )
    }
    var recentFiles by remember { mutableStateOf(listOf<String>()) }

    Surface(modifier = Modifier.fillMaxSize()) {
        MaterialTheme {
            when (val screen = currentScreen) {
                is Screen.Welcome -> WelcomeScreen(
                    onOpenLevel = { currentScreen = Screen.LevelSelect },
                    onRecentLevel = { path ->
                        recentFiles = (recentFiles.filter { it != path } + path).take(10)
                        currentScreen = Screen.Game(path, path.split("/").lastOrNull()?.split("\\")?.lastOrNull() ?: path)
                    },
                    recentFiles = recentFiles
                )

                is Screen.LevelSelect -> LevelSelectScreen(
                    onFileSelected = { result ->
                        recentFiles = (recentFiles.filter { it != result.title } + result.title).take(10)
                        currentScreen = Screen.Game(result.json, result.title, result.baseUri)
                    },
                    onBack = { currentScreen = Screen.Welcome },
                    filePickerButton = filePickerButton
                )

                is Screen.Game -> GameScreen(
                    levelPath = screen.title.ifEmpty { "Unknown Level" },
                    onBack = { currentScreen = Screen.Welcome },
                    gameView = { gameViewFactory(screen.levelJson) }
                )
            }
        }
    }
}