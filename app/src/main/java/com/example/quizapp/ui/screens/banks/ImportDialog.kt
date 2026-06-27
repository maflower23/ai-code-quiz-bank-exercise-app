@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.quizapp.ui.screens.banks

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.quizapp.ui.viewmodels.BankViewModel

@Composable
fun ImportDialog(vm: BankViewModel, onMessage: (String?) -> Unit) {
    val ctx = LocalContext.current
    var snack by remember { mutableStateOf<String?>(null) }

    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.importJson(ctx, uri) { ok, msg ->
                snack = if (ok) "JSON 导入成功" else msg
            }
        }
    }
    val xlsxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.importXlsx(ctx, uri) { ok, msg ->
                snack = if (ok) "Excel 导入成功" else msg
            }
        }
    }

    AlertDialog(
        onDismissRequest = { onMessage(snack) },
        title = { Text("导入题库") },
        text = {
            Column {
                Text("选择要导入的文件格式：", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Icon(Icons.Filled.Description, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { jsonLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }) {
                        Text("导入 .json（含图片）")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Icon(Icons.Filled.TableChart, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = {
                        xlsxLauncher.launch(arrayOf(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/vnd.ms-excel", "*/*"
                        ))
                    }) { Text("导入 .xlsx（纯文本）") }
                }
                snack?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onMessage(snack) }) { Text("关闭") } }
    )
}

@Composable
fun ExportButtons(bankId: Long, vm: BankViewModel) {
    val ctx = LocalContext.current
    var msg by remember { mutableStateOf<String?>(null) }

    val jsonOut = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            vm.exportJson(ctx, bankId, uri) { ok, m -> msg = if (ok) "已导出 JSON" else m }
        }
    }
    val xlsxOut = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        if (uri != null) {
            vm.exportXlsx(ctx, bankId, uri) { ok, m -> msg = if (ok) "已导出 Excel" else m }
        }
    }
    Row {
        TextButton(onClick = { jsonOut.launch("bank.json") }) { Text("导出 JSON") }
        TextButton(onClick = { xlsxOut.launch("bank.xlsx") }) { Text("导出 Excel") }
    }
    msg?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
}
