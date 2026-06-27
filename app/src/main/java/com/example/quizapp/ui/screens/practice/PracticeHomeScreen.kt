@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.quizapp.ui.screens.practice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizapp.data.QuizMode
import com.example.quizapp.ui.viewmodels.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeHomeScreen(
    onSessionReady: () -> Unit,
    vm: QuizViewModel = viewModel()
) {
    val banks by vm.banks.collectAsState(initial = emptyList())
    val sessions by vm.sessions.collectAsState(initial = emptyList())

    var mode by remember { mutableStateOf(QuizMode.PRACTICE) }
    var selectedBank by remember { mutableStateOf<Long?>(null) }
    var selectedBanks by remember { mutableStateOf(setOf<Long>()) }
    var countText by remember { mutableStateOf("10") }
    var shuffle by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun start() {
        error = null
        when (mode) {
            QuizMode.PRACTICE -> {
                val bid = selectedBank
                if (bid == null) { error = "请选择题库"; return }
                vm.startPractice(bid) { ok -> if (ok) onSessionReady() else error = "该题库没有题目" }
            }
            QuizMode.RANDOM_EXAM -> {
                val bid = selectedBank
                if (bid == null) { error = "请选择题库"; return }
                val n = countText.toIntOrNull() ?: 0
                vm.startRandomExam(bid, n, shuffle) { ok -> if (ok) onSessionReady() else error = "该题库没有题目" }
            }
            QuizMode.MIXED_EXAM -> {
                if (selectedBanks.isEmpty()) { error = "请至少选择一个题库"; return }
                val n = countText.toIntOrNull() ?: 0
                vm.startMixedExam(selectedBanks.toList(), n, shuffle) { ok -> if (ok) onSessionReady() else error = "所选题库没有题目" }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("练习 / 考试", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeSelector(mode) { mode = it }

            when (mode) {
                QuizMode.PRACTICE, QuizMode.RANDOM_EXAM -> {
                    SectionCard("选择题库") {
                        if (banks.isEmpty()) {
                            Text("暂无题库，请先在题库页创建或导入",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            banks.forEach { b ->
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    RadioButton(selected = selectedBank == b.id,
                                        onClick = { selectedBank = b.id })
                                    Text("${b.name}", modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                QuizMode.MIXED_EXAM -> {
                    SectionCard("选择题库（可多选）") {
                        if (banks.isEmpty()) {
                            Text("暂无题库", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            banks.forEach { b ->
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Checkbox(checked = b.id in selectedBanks,
                                        onCheckedChange = { c ->
                                            selectedBanks = if (c) selectedBanks + b.id else selectedBanks - b.id
                                        })
                                    Text("${b.name}", modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            if (mode != QuizMode.PRACTICE) {
                SectionCard("考试设置") {
                    OutlinedTextField(value = countText, onValueChange = { countText = it.filter { c -> c.isDigit() } },
                        label = { Text("题目数量 (0=全部)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("打乱单选/多选选项顺序", modifier = Modifier.weight(1f))
                        Switch(checked = shuffle, onCheckedChange = { shuffle = it })
                    }
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(onClick = { start() }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(8.dp))
                Text(if (mode == QuizMode.PRACTICE) "开始练习" else "开始考试", fontWeight = FontWeight.Bold)
            }

            if (sessions.isNotEmpty()) {
                SectionCard("最近考试记录") {
                    sessions.take(10).forEach { s ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(when (s.mode) {
                                "PRACTICE" -> "练习"
                                "RANDOM_EXAM" -> "随机考试"
                                else -> "混合考试"
                            }, color = MaterialTheme.colorScheme.onSurface)
                            Text("${s.correctCount}/${s.totalQuestions} 正确",
                                color = if (s.correctCount * 2 >= s.totalQuestions) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(selected: QuizMode, onSelect: (QuizMode) -> Unit) {
    SectionCard("选择模式") {
        Row {
            ModeChip("单题库练习", selected == QuizMode.PRACTICE) { onSelect(QuizMode.PRACTICE) }
            Spacer(Modifier.width(6.dp))
            ModeChip("随机抽题考试", selected == QuizMode.RANDOM_EXAM) { onSelect(QuizMode.RANDOM_EXAM) }
            Spacer(Modifier.width(6.dp))
            ModeChip("多库混合考试", selected == QuizMode.MIXED_EXAM) { onSelect(QuizMode.MIXED_EXAM) }
        }
    }
}

@Composable
private fun ModeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text) })
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
