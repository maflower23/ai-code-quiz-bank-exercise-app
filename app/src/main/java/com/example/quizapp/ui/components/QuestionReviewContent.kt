@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.quizapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.quizapp.data.AnswerChecker
import com.example.quizapp.data.QuestionType
import com.example.quizapp.data.entities.Question

@Composable
fun QuestionReviewContent(q: Question) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(onClick = {}, label = { Text(q.type.displayName) })
        Text(q.stem, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        AppImage(q.stemImage, Modifier.fillMaxWidth())

        when (q.type) {
            QuestionType.SINGLE, QuestionType.MULTIPLE -> {
                q.options.forEachIndexed { i, opt ->
                    val isCorrect = i in q.answer.indices
                    Card(colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Text("${'A' + i}. ${opt.text}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal)
                            AppImage(opt.imagePath, Modifier.fillMaxWidth().heightIn(max = 160.dp))
                        }
                    }
                }
            }
            QuestionType.JUDGE -> {
                Text("正确答案：${if (q.answer.judge) "对" else "错"}",
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            QuestionType.FILL -> {
                q.answer.blanks.forEachIndexed { i, alts ->
                    Text("第 ${i + 1} 空：${alts.joinToString(" / ")}",
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (q.explanation.isNotBlank()) {
            Divider(color = MaterialTheme.colorScheme.outline)
            Text("解析：${q.explanation}", color = MaterialTheme.colorScheme.onSurface)
            AppImage(q.explanationImage, Modifier.fillMaxWidth())
        }
    }
}
