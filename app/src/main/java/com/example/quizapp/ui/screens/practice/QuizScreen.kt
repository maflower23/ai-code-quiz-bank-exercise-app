@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.quizapp.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizapp.data.QuestionType
import com.example.quizapp.data.QuizMode
import com.example.quizapp.data.UserAnswer
import com.example.quizapp.data.entities.Question
import com.example.quizapp.ui.components.AppImage
import com.example.quizapp.ui.viewmodels.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onExit: () -> Unit,
    onFinished: () -> Unit,
    vm: QuizViewModel = viewModel()
) {
    val session by vm.session.collectAsState()
    val index by vm.index.collectAsState()
    val revealed by vm.revealed.collectAsState()
    val finished by vm.finished.collectAsState()
    // Reading tick registers a dependency so the UI recomposes when answer state mutates.
    val tick = vm.tick.collectAsState().value

    val s = session
    if (s == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(finished) {
        if (finished) onFinished()
    }

    val q = s.questions.getOrNull(index) ?: run {
        onExit(); return
    }
    val isExam = s.config.mode != QuizMode.PRACTICE
    val userAns = s.userAnswer(q)
    val isCorrect = s.corrected[q.id]
    var showCard by remember { mutableStateOf(false) }
    var isFav by remember { mutableStateOf(false) }
    LaunchedEffect(q.id) { isFav = vm.isFavorite(q.id) }

    val showResult = !isExam && revealed

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("第 ${index + 1} / ${s.size()} 题") },
                navigationIcon = { IconButton(onClick = onExit) { Icon(Icons.Filled.Close, null) } },
                actions = {
                    IconButton(onClick = { vm.toggleFavorite(q) { isFav = it } }) {
                        Icon(
                            if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                            null, tint = if (isFav) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showCard = true }) {
                        Icon(Icons.Filled.Apps, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
                IconButton(onClick = { vm.prev() }, enabled = index > 0) {
                    Icon(Icons.Filled.ChevronLeft, null)
                }
                Spacer(Modifier.weight(1f))
                if (isExam) {
                    Button(onClick = { vm.finishExam() }) { Text("交卷") }
                } else {
                    if (!showResult) {
                        Button(onClick = { vm.submitPractice() }, enabled = s.hasAnswered(q)) { Text("提交") }
                    } else {
                        Button(onClick = {
                            if (index < s.size() - 1) vm.next() else onFinished()
                        }) { Text(if (index < s.size() - 1) "下一题" else "完成") }
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.next() }, enabled = index < s.size() - 1) {
                    Icon(Icons.Filled.ChevronRight, null)
                }
            }
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Question header
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text(q.type.displayName) })
                if (showResult && isCorrect != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isCorrect) "✓ 回答正确" else "✗ 回答错误",
                        color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Stem
            Text(q.stem, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface)
            AppImage(q.stemImage, Modifier.fillMaxWidth())

            // Answer area
            AnswerArea(q, s, userAns, enabled = !showResult, onAnswer = { vm.setUserAnswer(it) })

            // Result / explanation
            if (showResult) {
                Card(colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect == true) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("你的答案：${describeUserSafe(q, userAns)}",
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("正确答案：${describeCorrectSafe(q)}",
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        if (q.explanation.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text("解析：${q.explanation}", color = MaterialTheme.colorScheme.onSurface)
                            AppImage(q.explanationImage, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCard) {
        AnswerCardSheet(
            session = s,
            currentIndex = index,
            isExam = isExam,
            onDismiss = { showCard = false },
            onJump = { i -> vm.jump(i); showCard = false }
        )
    }
}

@Composable
private fun AnswerArea(
    q: Question,
    session: com.example.quizapp.data.QuizSession,
    userAns: UserAnswer,
    enabled: Boolean,
    onAnswer: (UserAnswer) -> Unit
) {
    when (q.type) {
        QuestionType.SINGLE, QuestionType.MULTIPLE -> {
            val order = session.displayOrder(q)
            val selected = (userAns as? UserAnswer.Indices)?.selected ?: emptySet()
            val multi = q.type == QuestionType.MULTIPLE
            order.forEachIndexed { displayPos, origIdx ->
                val opt = q.options[origIdx]
                val isSelected = displayPos in selected
                OptionRow(
                    letter = "${'A' + displayPos}",
                    text = opt.text,
                    image = opt.imagePath,
                    selected = isSelected,
                    enabled = enabled,
                    showCorrect = false,
                    isCorrectOpt = false,
                    onClick = {
                        if (!enabled) return@OptionRow
                        val newSel = if (multi) {
                            if (isSelected) selected - displayPos else selected + displayPos
                        } else setOf(displayPos)
                        onAnswer(UserAnswer.Indices(newSel))
                    }
                )
            }
        }
        QuestionType.JUDGE -> {
            val v = (userAns as? UserAnswer.Judge)?.value
            Row {
                JudgeBtn("对", v == true, enabled) { onAnswer(UserAnswer.Judge(true)) }
                Spacer(Modifier.width(12.dp))
                JudgeBtn("错", v == false, enabled) { onAnswer(UserAnswer.Judge(false)) }
            }
        }
        QuestionType.FILL -> {
            val values = (userAns as? UserAnswer.Fills)?.values
                ?: List(q.answer.blanks.size) { "" }
            val safe = if (values.size != q.answer.blanks.size) List(q.answer.blanks.size) { "" } else values
            safe.forEachIndexed { i, v ->
                OutlinedTextField(
                    value = v, onValueChange = { nv ->
                        val list = safe.toMutableList().also { it[i] = nv }
                        onAnswer(UserAnswer.Fills(list))
                    },
                    enabled = enabled,
                    label = { Text("第 ${i + 1} 空") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun OptionRow(
    letter: String,
    text: String,
    image: String?,
    selected: Boolean,
    enabled: Boolean,
    showCorrect: Boolean,
    isCorrectOpt: Boolean,
    onClick: () -> Unit
) {
    val border = when {
        showCorrect && isCorrectOpt -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val bg = when {
        showCorrect && isCorrectOpt -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surface
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, border, RoundedCornerShape(10.dp))
            .background(bg).clickable(enabled = enabled, onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center) {
            Text(letter, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            if (text.isNotBlank()) Text(text, color = MaterialTheme.colorScheme.onSurface)
            if (!image.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                AppImage(image, Modifier.fillMaxWidth().heightIn(max = 160.dp))
            }
        }
    }
}

@Composable
private fun JudgeBtn(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.width(100.dp)
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
private fun AnswerCardSheet(
    session: com.example.quizapp.data.QuizSession,
    currentIndex: Int,
    isExam: Boolean,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp)) {
            Text("答题卡", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (isExam) "已答 ${session.answeredCount()}/${session.size()}"
                else "已答 ${session.answeredCount()}/${session.size()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.heightIn(max = 360.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(session.size()) { i ->
                    val q = session.questions[i]
                    val answered = session.hasAnswered(q)
                    val correct = session.corrected[q.id]
                    val bg = when {
                        i == currentIndex -> MaterialTheme.colorScheme.secondary
                        !isExam && correct != null && correct -> MaterialTheme.colorScheme.primary
                        !isExam && correct != null && !correct -> MaterialTheme.colorScheme.error
                        answered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val fg = if (answered || i == currentIndex || (!isExam && correct != null))
                        MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    Box(
                        Modifier.aspectRatio(1f).clip(CircleShape).background(bg)
                            .clickable { onJump(i) },
                        contentAlignment = Alignment.Center
                    ) { Text("${i + 1}", color = fg, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// describe helpers using original-space answer (display in practice result uses original opts)
private fun describeUserSafe(q: Question, ua: UserAnswer): String =
    com.example.quizapp.data.AnswerChecker.describeUser(q, ua)
private fun describeCorrectSafe(q: Question): String =
    com.example.quizapp.data.AnswerChecker.describeCorrect(q)
