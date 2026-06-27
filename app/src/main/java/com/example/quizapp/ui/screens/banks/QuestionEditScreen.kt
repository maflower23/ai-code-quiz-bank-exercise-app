@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.quizapp.ui.screens.banks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizapp.data.AnswerData
import com.example.quizapp.data.ImageStore
import com.example.quizapp.data.OptionItem
import com.example.quizapp.data.QuestionType
import com.example.quizapp.data.entities.Question
import com.example.quizapp.ui.components.ImagePickerField
import com.example.quizapp.ui.viewmodels.BankViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionEditScreen(
    bankId: Long,
    questionId: Long,
    onBack: () -> Unit,
    vm: BankViewModel = viewModel()
) {
    var loaded by remember { mutableStateOf(questionId == 0L) }
    var type by remember { mutableStateOf(QuestionType.SINGLE) }
    var stem by remember { mutableStateOf("") }
    var stemImage by remember { mutableStateOf<String?>(null) }
    var explanation by remember { mutableStateOf("") }
    var explanationImage by remember { mutableStateOf<String?>(null) }
    // options for single/multiple
    var options by remember { mutableStateOf(listOf(OptionItem(""), OptionItem(""), OptionItem(""), OptionItem(""))) }
    var answerIndices by remember { mutableStateOf(setOf<Int>()) }
    var judgeAnswer by remember { mutableStateOf(true) }
    // blanks for fill: each blank -> list of acceptable answers (joined by /)
    var blanks by remember { mutableStateOf(listOf(listOf(""))) }

    val imageStore: ImageStore = remember { vm.imageStore() }

    LaunchedEffect(questionId) {
        if (questionId != 0L) {
            vm.getQuestion(questionId)?.let { q ->
                type = q.type
                stem = q.stem
                stemImage = q.stemImage
                explanation = q.explanation
                explanationImage = q.explanationImage
                options = q.options.ifEmpty { listOf(OptionItem(""), OptionItem(""), OptionItem(""), OptionItem("")) }
                answerIndices = q.answer.indices.toSet()
                judgeAnswer = q.answer.judge
                blanks = q.answer.blanks.ifEmpty { listOf(listOf("")) }
            }
        }
        loaded = true
    }

    fun buildAndSave(): Boolean {
        val answer = when (type) {
            QuestionType.SINGLE -> AnswerData(indices = answerIndices.toList().sortedBy { it }.takeIf { it.isNotEmpty() } ?: listOf(0))
            QuestionType.MULTIPLE -> AnswerData(indices = answerIndices.toList())
            QuestionType.JUDGE -> AnswerData(judge = judgeAnswer)
            QuestionType.FILL -> AnswerData(blanks = blanks.map { it.filter { s -> s.isNotBlank() } }.filter { it.isNotEmpty() })
        }
        val cleanedOptions = if (type == QuestionType.SINGLE || type == QuestionType.MULTIPLE)
            options.filter { it.text.isNotBlank() || it.imagePath != null }
        else emptyList()
        val q = Question(
            id = questionId,
            bankId = bankId,
            type = type,
            stem = stem.trim(),
            stemImage = stemImage,
            options = cleanedOptions,
            answer = answer,
            explanation = explanation.trim(),
            explanationImage = explanationImage
        )
        if (q.stem.isBlank() && q.stemImage.isNullOrBlank()) return false
        vm.saveQuestion(q) { onBack() }
        return true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (questionId == 0L) "新建题目" else "编辑题目", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = { buildAndSave() }) { Icon(Icons.Filled.Check, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { pad ->
        if (!loaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Type selector
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("题型", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        QuestionType.entries.forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t.displayName) },
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                }
            }

            // Stem
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("题干", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = stem, onValueChange = { stem = it },
                        label = { Text("输入题干（填空题用 ____ 表示空）") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Spacer(Modifier.height(8.dp))
                    ImagePickerField("题干图片", stemImage, imageStore) { stemImage = it }
                }
            }

            // Options / answer depending on type
            when (type) {
                QuestionType.SINGLE, QuestionType.MULTIPLE -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("选项 (点击左侧圆圈设为正确答案)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            options.forEachIndexed { i, opt ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val selected = i in answerIndices
                                    IconButton(onClick = {
                                        answerIndices = if (type == QuestionType.SINGLE) setOf(i)
                                        else if (selected) answerIndices - i else answerIndices + i
                                    }) {
                                        Icon(
                                            if (type == QuestionType.SINGLE) {
                                                if (selected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked
                                            } else {
                                                if (selected) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank
                                            }, null,
                                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedTextField(value = opt.text, onValueChange = { v ->
                                        options = options.toMutableList().also { it[i] = it[i].copy(text = v) }
                                    }, label = { Text("选项 ${'A' + i}") },
                                        modifier = Modifier.weight(1f))
                                    IconButton(onClick = {
                                        options = options.toMutableList().also { it.removeAt(i) }
                                        answerIndices = answerIndices.mapNotNull { idx ->
                                            when { idx == i -> null; idx > i -> idx - 1; else -> idx }
                                        }.toSet()
                                    }) { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) }
                                }
                                ImagePickerField("选项 ${'A' + i} 图片", opt.imagePath, imageStore) { p ->
                                    options = options.toMutableList().also { it[i] = it[i].copy(imagePath = p) }
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                            Button(onClick = { options = options + OptionItem("") }) { Text("+ 添加选项") }
                        }
                    }
                }
                QuestionType.JUDGE -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("正确答案", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row {
                                FilterChip(selected = judgeAnswer, onClick = { judgeAnswer = true },
                                    label = { Text("对") }, modifier = Modifier.padding(end = 8.dp))
                                FilterChip(selected = !judgeAnswer, onClick = { judgeAnswer = false },
                                    label = { Text("错") })
                            }
                        }
                    }
                }
                QuestionType.FILL -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("填空答案 (每个空可有多个可接受答案，用 / 分隔)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            blanks.forEachIndexed { i, alts ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("空 ${i + 1}", modifier = Modifier.width(48.dp),
                                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = alts.joinToString("/"),
                                        onValueChange = { v ->
                                            blanks = blanks.toMutableList().also {
                                                it[i] = v.split("/").map { s -> s.trim() }
                                            }
                                        },
                                        label = { Text("答案，多个用 / 分隔") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { blanks = blanks.toMutableList().also { it.removeAt(i) } }) {
                                        Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                            Button(onClick = { blanks = blanks + listOf(listOf("")) }) { Text("+ 添加空") }
                        }
                    }
                }
            }

            // Explanation
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("解析", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = explanation, onValueChange = { explanation = it },
                        label = { Text("解析（可选）") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Spacer(Modifier.height(8.dp))
                    ImagePickerField("解析图片", explanationImage, imageStore) { explanationImage = it }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
