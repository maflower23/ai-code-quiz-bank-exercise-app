package com.example.quizapp.ui.screens.practice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizapp.ui.viewmodels.QuizViewModel

@Composable
fun QuizResultScreen(
    onHome: () -> Unit,
    vm: QuizViewModel = viewModel()
) {
    val s = vm.session.collectAsState().value
    val total = s?.size() ?: 0
    val correct = s?.correctCount() ?: 0
    val wrong = s?.wrongCount() ?: 0
    val unanswered = total - (s?.answeredCount() ?: 0)
    val rate = if (total > 0) correct * 100 / total else 0

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(16.dp))
        Text("完成！", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text("$rate 分", style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                ResultRow("总题数", "$total")
                ResultRow("答对", "$correct", color = MaterialTheme.colorScheme.primary)
                ResultRow("答错", "$wrong", color = MaterialTheme.colorScheme.error)
                if (unanswered > 0) ResultRow("未答", "$unanswered")
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { vm.clearSession(); onHome() },
            modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Icon(Icons.Filled.Home, null); Spacer(Modifier.width(8.dp))
            Text("返回首页", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = color, fontWeight = FontWeight.Bold)
    }
}
