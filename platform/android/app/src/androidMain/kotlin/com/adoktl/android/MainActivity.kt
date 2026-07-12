package com.adoktl.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adoktl.ui.ADOKTLApp
import com.adoktl.ui.ComposeGameView
import com.adoktl.ui.FilePickResult
import com.adoktl.ui.rememberPlayerEngine
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
                    ComposeGameView(playerEngine = rememberPlayerEngine(levelJson))
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
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        isPicking = false
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                val adofaiFiles = findAllAdofai(context, uri)
                if (adofaiFiles.isEmpty()) {
                    DebugLog.log("No .adofai file found in selected folder")
                    return@rememberLauncherForActivityResult
                }

                val levels = adofaiFiles.mapNotNull { (fileUri, name) ->
                    val json = context.contentResolver.openInputStream(fileUri)?.use {
                        it.reader().readText()
                    } ?: return@mapNotNull null
                    com.adoktl.ui.LevelEntry(json, name)
                }

                onResult(FilePickResult("", "", uri.toString(), levels))
            } catch (e: Exception) {
                DebugLog.log("File picker error: ${e.message}")
            }
        }
    }

    Button(
        onClick = {
            isPicking = true
            launcher.launch(null)
        },
        enabled = !isPicking,
        modifier = Modifier.width(220.dp).height(48.dp)
    ) {
        Text(if (isPicking) "Opening..." else "Browse Folder", fontSize = 16.sp)
    }
}

private fun findAllAdofai(
    context: android.content.Context,
    treeUri: Uri,
    parentDocId: String? = null
): List<Pair<Uri, String>> {
    val result = mutableListOf<Pair<Uri, String>>()
    val docId = parentDocId ?: DocumentsContract.getTreeDocumentId(treeUri)
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
    val cursor = context.contentResolver.query(childrenUri, null, null, null, null)
    cursor?.use {
        while (it.moveToNext()) {
            val childId = it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
            val mime = it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE))
            val name = it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)) ?: continue
            if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                result.addAll(findAllAdofai(context, treeUri, childId))
            } else if (name.endsWith(".adofai")) {
                result.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, childId) to name)
            }
        }
    }
    return result
}