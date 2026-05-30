package com.adoktl.android

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adoktl.ui.ADOKTLApp
import com.adoktl.ui.FilePickResult
import com.adoktl.util.DebugLog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentJson = intent?.data?.let { uri ->
            try {
                contentResolver.openInputStream(uri)?.use { it.reader().readText() }
            } catch (e: Exception) {
                DebugLog.log("Failed to read intent file: ${e.message}")
                null
            }
        }
        val intentTitle = intentJson?.let {
            intent?.data?.lastPathSegment ?: "Imported Level"
        } ?: ""

        setContent {
            ADOKTLApp(
                initialLevelJson = intentJson,
                initialTitle = intentTitle,
                filePickerButton = { onResult ->
                    AndroidFilePickerButton(onResult = onResult)
                },
                gameViewFactory = { levelJson ->
                    AndroidGameView(levelJson)
                }
            )
        }
    }
}

@Composable
fun AndroidFilePickerButton(onResult: (FilePickResult) -> Unit) {
    val context = LocalContext.current
    var isPicking by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        isPicking = false
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use {
                    it.reader().readText()
                } ?: return@rememberLauncherForActivityResult

                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val displayName = cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) it.getString(nameIndex) else uri.lastPathSegment ?: "Unknown"
                    } else uri.lastPathSegment ?: "Unknown"
                } ?: uri.lastPathSegment ?: "Unknown"

                onResult(FilePickResult(json, displayName))
            } catch (e: Exception) {
                DebugLog.log("File picker error: ${e.message}")
            }
        }
    }

    Button(
        onClick = {
            isPicking = true
            launcher.launch(arrayOf("*/*"))
        },
        enabled = !isPicking,
        modifier = Modifier.width(220.dp).height(48.dp)
    ) {
        Text(if (isPicking) "Opening..." else "Browse Files", fontSize = 16.sp)
    }
}

@Composable
fun AndroidGameView(levelJson: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Game Rendering (OpenGL)\nNot yet implemented in Compose",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}