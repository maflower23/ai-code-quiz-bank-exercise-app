@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.quizapp.ui.screens.banks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
fun BankDetailScreen(
    bankId: Long,
    onBack: () -> Unit,
    onEditBank: () -> Unit,
    onEditQuestion: (Long) -> Unit,
    onStartPractice: () -> Unit,
    onStartRandomExam: () -> Unit,
    vm: BankViewModel = viewModel()
) {
    val questions by vm.questionsFlow(bankId).collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    var searchFlow by remember { mutableStateOf<kotlinx.coroutines.flow.Flow<List<Question>>>(kotlinx.coroutines.flow.flowOf(emptyList())) }
    LaunchedEffect(query) {
        searchActive = query.isNotBlank()
        searchFlow = if (searchActive) vm.search(bankId, query) else kotlinx.coroutines.flow.flowOf(emptyList())
    }
    val searchResults by searchFlow.collectAsState(initial = emptyList())

    // type filter chips
    var selectedType by remember { mutableStateOf<QuestionType?>(null) }
    val typeCounts by produceState(initialValue = emptyMap<QuestionType, Int>(), questions) {
        value = questions.groupingBy { it.type }.eachCount()
    }

    var stats by remember { mutableStateOf<com.example.quizapp.data.BankStats?>(null) }
    LaunchedEffect(bankId, questions.size) {
        stats = vm.stats(bankId)
    }

    val shown = when {
        searchActive -> searchResults
        selectedType != null -> questions.filter { it.type == selectedType }
        else -> questions
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("题库详情", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = onEditBank) { Icon(Icons.Filled.Edit, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            // Stats row
            stats?.let { StatsBar(it) }
            // Action buttons
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartPractice, modifier = Modifier.weight(1f)) { Text("练习") }
                OutlinedButton(onClick = onStartRandomExam, modifier = Modifier.weight(1f)) { Text("随机考试") }
            }
            // Search bar
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                label = { Text("搜索题目") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true
            )
            // Type chips
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = selectedType == null, onClick = { selectedType = null },
                    label = { Text("全部 ${questions.size}") })
                QuestionType.entries.forEach { t ->
                    FilterChip(
                        selected = selectedType == t,
                        onClick = { selectedType = if (selectedType == t) null else t },
                        label = { Text("${t.displayName} ${typeCounts[t] ?: 0}") }
                    )
                }
            }
            Divider(color = MaterialTheme.colorScheme.outline)
            // List
            if (shown.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无题目", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(shown, key = { it.id }) { q ->
                        QuestionBrowseCard(q, onClick = { onEditQuestion(q.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsBar(stats: com.example.quizapp.data.BankStats) {
    val masteredRate = if (stats.totalQuestions > 0) stats.masteredCount * 100 / stats.totalQuestions else 0
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceAround) {
            StatItem("总题数", stats.totalQuestions.toString())
            StatItem("已练习", stats.practicedCount.toString())
            StatItem("已掌握", stats.masteredCount.toString())
            StatItem("掌握率", "$masteredRate%")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuestionBrowseCard(q: Question, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text(q.type.displayName) })
                Spacer(Modifier.width(8.dp))
                Text("第 ${q.id} 题", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Text(q.stem, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 3)
            if (!q.stemImage.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                AppImage(q.stemImage, Modifier.fillMaxWidth().heightIn(max = 140.dp))
            }
        }
    }
}
