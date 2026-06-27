package com.example.quizapp.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.quizapp.data.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImagePickerField(
    label: String,
    path: String?,
    imageStore: ImageStore,
    onPicked: (String?) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { input ->
                        val ext = ctx.contentResolver.getType(uri)?.let { mt ->
                            when { mt.contains("png") -> ".png"; mt.contains("jpeg") || mt.contains("jpg") -> ".jpg"; else -> ".img" }
                        } ?: ".img"
                        imageStore.saveStream(input, ext)
                    }
                }
                onPicked(saved)
            }
        }
    }
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = {
                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Icon(Icons.Filled.AddPhotoAlternate, null); Spacer(Modifier.width(6.dp)); Text("选择图片") }
            if (path != null) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { onPicked(null) }) {
                    Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (path != null) {
            Spacer(Modifier.height(4.dp))
            AppImage(path, Modifier.fillMaxWidth().heightIn(max = 180.dp))
        }
    }
}
