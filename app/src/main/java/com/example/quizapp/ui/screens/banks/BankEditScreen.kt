@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.quizapp.ui.screens.banks

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizapp.data.QuestionType
import com.example.quizapp.data.entities.Question
import com.example.quizapp.ui.components.AppImage
import com.example.quizapp.ui.viewmodels.BankViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankEditScreen(
    bankId: Long,
    onBack: () -> Unit,
    onEditQuestion: (Long) -> Unit,
    vm: BankViewModel = viewModel()
) {
    val questions by vm.questionsFlow(bankId).collectAsState(initial = emptyList())
    var bank by remember { mutableStateOf<com.example.quizapp.data.entities.QuestionBank?>(null) }
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(bankId) {
        val b = vm.getBank(bankId)
        bank = b
        name = b?.name ?: ""
        desc = b?.description ?: ""
        loaded = true
    }

    fun saveMeta() {
        bank?.let { vm.updateBank(it.copy(name = name, description = desc)) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("编辑题库", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { saveMeta(); onBack() }) { Icon(Icons.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { saveMeta(); onBack() }) { Icon(Icons.Filled.Check, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEditQuestion(0) },
                containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, null)
            }
        }
    ) { pad ->
        if (!loaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        OutlinedTextField(value = name, onValueChange = { name = it },
                            label = { Text("题库名称") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = desc, onValueChange = { desc = it },
                            label = { Text("描述") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        Text("导入 / 导出", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        ExportButtons(bankId, vm)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("题目列表 (${questions.size})", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            items(questions, key = { it.id }) { q ->
                QuestionManageCard(q,
                    onClick = { onEditQuestion(q.id) },
                    onDelete = { vm.deleteQuestion(q) })
            }
        }
    }
}

@Composable
private fun QuestionManageCard(q: Question, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AssistChip(onClick = {}, label = { Text(q.type.displayName) })
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(q.stem, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                if (q.options.isNotEmpty()) {
                    Text("选项: ${q.options.size}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
