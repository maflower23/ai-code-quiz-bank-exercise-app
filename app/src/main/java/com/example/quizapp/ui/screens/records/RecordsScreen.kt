@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.quizapp.ui.screens.records

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizapp.data.entities.Question
import com.example.quizapp.data.entities.WrongRecord
import com.example.quizapp.data.entities.FavoriteRecord
import com.example.quizapp.ui.components.QuestionReviewContent
import com.example.quizapp.ui.viewmodels.QuizViewModel
import com.example.quizapp.ui.viewmodels.RecordViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    onPracticeReady: () -> Unit,
    rvm: RecordViewModel = viewModel(),
    qvm: QuizViewModel = viewModel()
) {
    var tab by remember { mutableStateOf(0) }
    val practiceWrong by rvm.practiceWrong.collectAsState()
    val examWrong by rvm.examWrong.collectAsState()
    val favorites by rvm.favorites.collectAsState()
    val titles = listOf("练习错题", "考试错题", "收藏")
    val scope = rememberCoroutineScope()

    fun practiceWrongList(records: List<WrongRecord>) {
        val ids = records.map { it.questionId }
        if (ids.isEmpty()) return
        scope.launch {
            val qs = withContext(Dispatchers.IO) { rvm.loadQuestions(ids) }
            if (qs.isNotEmpty()) {
                qvm.startFromQuestions(qs, shuffleOptions = true) { ok -> if (ok) onPracticeReady() }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("错题与收藏", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                titles.forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                }
            }
            when (tab) {
                0 -> WrongList(practiceWrong, "暂无练习错题",
                    onLoadQ = { rvm.loadQuestion(it) },
                    onDelete = { rvm.deleteWrong(it) },
                    onPracticeAll = { practiceWrongList(practiceWrong) })
                1 -> WrongList(examWrong, "暂无考试错题",
                    onLoadQ = { rvm.loadQuestion(it) },
                    onDelete = { rvm.deleteWrong(it) },
                    onPracticeAll = { practiceWrongList(examWrong) })
                2 -> FavoritesList(favorites, "暂无收藏题目",
                    onLoadQ = { rvm.loadQuestion(it) },
                    onDelete = { rvm.deleteFavorite(it) })
            }
        }
    }
}

@Composable
private fun WrongList(
    records: List<WrongRecord>,
    emptyText: String,
    onLoadQ: suspend (Long) -> Question?,
    onDelete: (Long) -> Unit,
    onPracticeAll: () -> Unit
) {
    var selectedId by remember { mutableStateOf<Long?>(null) }
    Column(Modifier.fillMaxSize()) {
        if (records.isNotEmpty()) {
            Button(onClick = onPracticeAll,
                modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(8.dp))
                Text("练习这些错题 (${records.size})")
            }
        }
        if (records.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records, key = { it.id }) { r ->
                    WrongCard(r, onLoadQ = onLoadQ,
                        onClick = { selectedId = r.questionId },
                        onDelete = { onDelete(r.id) })
                }
            }
        }
    }
    selectedId?.let { id -> ReviewSheet(id, onLoadQ) { selectedId = null } }
}

@Composable
private fun FavoritesList(
    records: List<FavoriteRecord>,
    emptyText: String,
    onLoadQ: suspend (Long) -> Question?,
    onDelete: (Long) -> Unit
) {
    var selectedId by remember { mutableStateOf<Long?>(null) }
    if (records.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records, key = { it.id }) { r ->
                FavoriteCard(r, onLoadQ = onLoadQ,
                    onClick = { selectedId = r.questionId },
                    onDelete = { onDelete(r.questionId) })
            }
        }
    }
    selectedId?.let { id -> ReviewSheet(id, onLoadQ) { selectedId = null } }
}

@Composable
private fun WrongCard(
    r: WrongRecord,
    onLoadQ: suspend (Long) -> Question?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var q by remember(r.id) { mutableStateOf<Question?>(null) }
    LaunchedEffect(r.id) { q = onLoadQ(r.questionId) }
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                q?.let {
                    AssistChip(onClick = {}, label = { Text(it.type.displayName) })
                    Text(it.stem, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                } ?: Text("加载中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    r: FavoriteRecord,
    onLoadQ: suspend (Long) -> Question?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var q by remember(r.id) { mutableStateOf<Question?>(null) }
    LaunchedEffect(r.id) { q = onLoadQ(r.questionId) }
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                q?.let {
                    AssistChip(onClick = {}, label = { Text(it.type.displayName) })
                    Text(it.stem, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                } ?: Text("加载中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ReviewSheet(questionId: Long, loader: suspend (Long) -> Question?, onDismiss: () -> Unit) {
    var q by remember(questionId) { mutableStateOf<Question?>(null) }
    LaunchedEffect(questionId) { q = loader(questionId) }
    ModalBottomSheet(onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            q?.let { QuestionReviewContent(it) }
                ?: Text("加载中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}
