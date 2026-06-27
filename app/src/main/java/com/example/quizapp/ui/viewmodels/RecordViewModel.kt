package com.example.quizapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizapp.data.QuizRepository
import com.example.quizapp.data.QuestionType
import com.example.quizapp.data.RecordSource
import com.example.quizapp.data.entities.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = QuizRepository.from(app)

    val practiceWrong = repo.observeWrong(RecordSource.PRACTICE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val examWrong = repo.observeWrong(RecordSource.EXAM)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites = repo.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val practiceWrongCount = repo.wrongCountFlow(RecordSource.PRACTICE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val examWrongCount = repo.wrongCountFlow(RecordSource.EXAM)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val favoriteCount = repo.observeFavoriteCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    suspend fun loadQuestion(id: Long): Question? = repo.getQuestion(id)

    suspend fun loadQuestions(ids: List<Long>): List<Question> = repo.getQuestionsByIds(ids)

    fun deleteWrong(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { repo.deleteWrongById(id) }
    }

    fun deleteFavorite(qid: Long) {
        viewModelScope.launch(Dispatchers.IO) { repo.deleteFavorite(qid) }
    }

    fun buildWrongPracticeSession(ids: List<Long>, onReady: (List<Question>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val qs = repo.getQuestionsByIds(ids)
            onReady(qs)
        }
    }
}
