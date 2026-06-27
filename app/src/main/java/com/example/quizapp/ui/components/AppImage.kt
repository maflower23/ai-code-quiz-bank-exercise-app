package com.example.quizapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

@Composable
fun AppImage(
    path: String?,
    modifier: Modifier = Modifier
) {
    if (path.isNullOrBlank()) return
    val file = File(path)
    if (!file.exists()) {
        Box(modifier.heightIn(max = 160.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.BrokenImage, contentDescription = null, modifier = Modifier.size(40.dp))
        }
        return
    }
    AsyncImage(
        model = file,
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        modifier = modifier
    )
}
