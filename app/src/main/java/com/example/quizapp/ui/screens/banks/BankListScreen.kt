@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.quizapp.ui.screens.banks

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizapp.data.entities.QuestionBank
import com.example.quizapp.ui.components.AppImage
import com.example.quizapp.ui.navigation.Routes
import com.example.quizapp.ui.viewmodels.BankViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankListScreen(
    onOpenBank: (Long) -> Unit,
    onEditBank: (Long) -> Unit,
    vm: BankViewModel = viewModel()
) {
    val banks by vm.banks.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val ctx = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("题库", fontWeight = FontWeight.Bold) },
                colors = topBarColors(),
                actions = {
                    IconButton(onClick = { showImport = true }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "导入")
                    }
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "新建")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        if (banks.isEmpty()) {
            EmptyBanks(modifier = Modifier.padding(pad)) { showAdd = true }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(banks, key = { it.id }) { bank ->
                    BankCard(bank, vm = vm,
                        onClick = { onOpenBank(bank.id) },
                        onEdit = { onEditBank(bank.id) },
                        onDelete = { vm.deleteBank(bank) })
                }
            }
        }
    }

    if (showAdd) {
        AddBankDialog(onDismiss = { showAdd = false }) { name, desc ->
            vm.addBank(name, desc) { id -> showAdd = false; onEditBank(id) }
        }
    }
    if (showImport) {
        ImportDialog(
            vm = vm,
            onMessage = { msg ->
                showImport = false
                msg?.let { showImport = false }
            }
        )
    }

    LaunchedEffect(Unit) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun topBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    actionIconContentColor = MaterialTheme.colorScheme.primary
)

@Composable
private fun BankCard(
    bank: QuestionBank,
    vm: BankViewModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val count by vm.countFlow(bank.id).collectAsState(initial = 0)
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(bank.name, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                if (bank.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(bank.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Spacer(Modifier.height(6.dp))
                Text("$count 题", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("编辑") }, onClick = { menuOpen = false; onEdit() })
                    DropdownMenuItem(text = { Text("删除") }, onClick = { menuOpen = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun EmptyBanks(modifier: Modifier = Modifier, onCreate: () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.LibraryBooks, contentDescription = null,
            modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("还没有题库", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text("点击右上角新建题库，或导入 .xlsx / .json 文件",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onCreate) { Text("新建题库") }
    }
}

@Composable
private fun AddBankDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建题库") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("题库名称") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it },
                    label = { Text("描述（可选）") }, maxLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim(), desc.trim()) },
                enabled = name.isNotBlank()) { Text("创建并编辑") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
