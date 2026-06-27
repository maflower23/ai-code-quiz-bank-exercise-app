package com.example.quizapp.ui.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizapp.data.QuizRepository
import com.example.quizapp.data.QuestionType
import com.example.quizapp.data.entities.Question
import com.example.quizapp.data.entities.QuestionBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BankViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = QuizRepository.from(app)

    val banks: StateFlow<List<QuestionBank>> =
        repo.observeBanks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBank(name: String, description: String, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repo.upsertBank(QuestionBank(name = name, description = description))
            onDone(id)
        }
    }

    fun updateBank(bank: QuestionBank) {
        viewModelScope.launch { repo.upsertBank(bank.copy(updatedAt = System.currentTimeMillis())) }
    }

    fun deleteBank(bank: QuestionBank) {
        viewModelScope.launch { repo.deleteBank(bank) }
    }

    // ---- Bank detail ----
    fun questionsFlow(bankId: Long) = repo.observeQuestions(bankId)
    fun countFlow(bankId: Long) = repo.countFlow(bankId)
    fun search(bankId: Long, kw: String) = repo.search(bankId, kw)

    suspend fun getBank(id: Long) = repo.getBank(id)
    suspend fun stats(bankId: Long) = repo.bankStats(bankId)
    suspend fun getByType(bankId: Long, type: QuestionType) = repo.getByBankAndType(bankId, type)

    // ---- Question editing ----
    suspend fun getQuestion(id: Long) = repo.getQuestion(id)
    fun saveQuestion(q: Question, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { repo.upsertQuestion(q); withContext(Dispatchers.Main) { onDone() } }
    }
    fun deleteQuestion(q: Question) { viewModelScope.launch { repo.deleteQuestion(q) } }

    // ---- Import / Export ----
    fun importJson(ctx: Context, uri: Uri, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    repo.importJson(input)
                }
                onDone(true, "导入成功")
            } catch (e: Exception) {
                onDone(false, "导入失败: ${e.message ?: e}")
            }
        }
    }

    fun importXlsx(ctx: Context, uri: Uri, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    repo.importXlsx(input)
                }
                onDone(true, "导入成功")
            } catch (e: Exception) {
                onDone(false, "导入失败: ${e.message ?: e}")
            }
        }
    }

    fun exportJson(ctx: Context, bankId: Long, uri: Uri, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { out ->
                    repo.exportJson(bankId, out)
                }
                onDone(true, "导出成功")
            } catch (e: Exception) {
                onDone(false, "导出失败: ${e.message ?: e}")
            }
        }
    }

    fun exportXlsx(ctx: Context, bankId: Long, uri: Uri, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { out ->
                    repo.exportXlsx(bankId, out)
                }
                onDone(true, "导出成功")
            } catch (e: Exception) {
                onDone(false, "导出失败: ${e.message ?: e}")
            }
        }
    }

    fun imageStore() = repo.imageStore
}
