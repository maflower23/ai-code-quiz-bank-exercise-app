package com.example.quizapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizapp.data.AnswerChecker
import com.example.quizapp.data.QuizConfig
import com.example.quizapp.data.QuizMode
import com.example.quizapp.data.QuizRepository
import com.example.quizapp.data.QuizSession
import com.example.quizapp.data.RecordSource
import com.example.quizapp.data.UserAnswer
import com.example.quizapp.data.entities.ExamSession
import com.example.quizapp.data.entities.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuizViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = QuizRepository.from(app)

    val banks = repo.observeBanks()
    val sessions = repo.observeSessions()

    private val _session = MutableStateFlow<QuizSession?>(null)
    val session: StateFlow<QuizSession?> = _session.asStateFlow()

    private val _index = MutableStateFlow(0)
    val index: StateFlow<Int> = _index.asStateFlow()

    private val _revealed = MutableStateFlow(false)
    val revealed: StateFlow<Boolean> = _revealed.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    /** Bumped whenever in-session answer state mutates, so Compose recomposes. */
    private val _tick = MutableStateFlow(0)
    val tick: StateFlow<Int> = _tick.asStateFlow()

    val isExam: Boolean get() = _session.value?.config?.mode != QuizMode.PRACTICE

    fun startPractice(bankId: Long, onReady: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val questions = repo.getQuestions(bankId)
            if (questions.isEmpty()) { withContext(Dispatchers.Main) { onReady(false) }; return@launch }
            reset(QuizSession(questions, QuizConfig(QuizMode.PRACTICE, listOf(bankId), 0, false)))
            withContext(Dispatchers.Main) { onReady(true) }
        }
    }

    /** Start a practice session from an explicit list of questions (e.g. wrong questions). */
    fun startFromQuestions(questions: List<Question>, shuffleOptions: Boolean = false, onReady: (Boolean) -> Unit = {}) {
        if (questions.isEmpty()) { onReady(false); return }
        reset(QuizSession(questions, QuizConfig(QuizMode.PRACTICE, questions.map { it.bankId }.distinct(), 0, shuffleOptions)))
        onReady(true)
    }

    fun startRandomExam(bankId: Long, count: Int, shuffleOptions: Boolean, onReady: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repo.getQuestions(bankId).shuffled()
            val picked = if (count <= 0) all else all.take(count)
            if (picked.isEmpty()) { withContext(Dispatchers.Main) { onReady(false) }; return@launch }
            reset(QuizSession(picked, QuizConfig(QuizMode.RANDOM_EXAM, listOf(bankId), picked.size, shuffleOptions)))
            withContext(Dispatchers.Main) { onReady(true) }
        }
    }

    fun startMixedExam(bankIds: List<Long>, count: Int, shuffleOptions: Boolean, onReady: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val pool = repo.allFromBanks(bankIds).shuffled()
            val picked = if (count <= 0) pool else pool.take(count)
            if (picked.isEmpty()) { withContext(Dispatchers.Main) { onReady(false) }; return@launch }
            reset(QuizSession(picked, QuizConfig(QuizMode.MIXED_EXAM, bankIds, picked.size, shuffleOptions)))
            withContext(Dispatchers.Main) { onReady(true) }
        }
    }

    private fun reset(s: QuizSession) {
        _session.value = s
        _index.value = 0
        _revealed.value = false
        _finished.value = false
        com.example.quizapp.ui.ActiveSession.session = s
    }

    fun currentQuestion(): Question? {
        val s = _session.value ?: return null
        return s.questions.getOrNull(_index.value)
    }

    fun setUserAnswer(ua: UserAnswer) {
        val s = _session.value ?: return
        val q = currentQuestion() ?: return
        s.setUserAnswer(q, ua)
        _tick.value = _tick.value + 1
    }

    fun submitPractice() {
        val s = _session.value ?: return
        val q = currentQuestion() ?: return
        if (!s.hasAnswered(q)) return
        val correct = s.evaluate(q)
        val userJson = AnswerChecker.userAnswerJson(s.userAnswer(q))
        val correctJson = AnswerChecker.userAnswerJson(AnswerChecker.correctUserAnswer(q))
        viewModelScope.launch(Dispatchers.IO) {
            repo.recordAnswer(q, q.bankId, correct, RecordSource.PRACTICE, userJson, correctJson)
        }
        _revealed.value = true
        _tick.value = _tick.value + 1
    }

    fun next() {
        val s = _session.value ?: return
        if (_index.value < s.size() - 1) { _index.value++; syncRevealed() }
    }

    fun prev() {
        if (_index.value > 0) { _index.value--; syncRevealed() }
    }

    fun goto(i: Int) {
        val s = _session.value ?: return
        if (i in 0 until s.size()) { _index.value = i; syncRevealed() }
    }

    fun jump(i: Int) {
        val s = _session.value ?: return
        if (i in 0 until s.size()) { _index.value = i; syncRevealed() }
    }

    private fun syncRevealed() {
        val s = _session.value ?: return
        val q = s.questions.getOrNull(_index.value)
        _revealed.value = !isExam && q != null && s.corrected.containsKey(q.id)
    }

    fun finishExam() {
        val s = _session.value ?: return
        var correct = 0
        var wrong = 0
        viewModelScope.launch(Dispatchers.IO) {
            s.questions.forEach { q ->
                val ok = s.evaluate(q)
                if (ok) correct++ else wrong++
                repo.recordAnswer(q, q.bankId, ok, RecordSource.EXAM,
                    AnswerChecker.userAnswerJson(s.userAnswer(q)),
                    AnswerChecker.userAnswerJson(AnswerChecker.correctUserAnswer(q)))
            }
            repo.saveSession(ExamSession(
                bankIds = s.config.bankIds,
                mode = s.config.mode.name,
                totalQuestions = s.size(),
                correctCount = correct,
                wrongCount = wrong,
                startedAt = System.currentTimeMillis() - 1000
            ))
            _finished.value = true
        }
    }

    fun toggleFavorite(q: Question, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(repo.toggleFavorite(q, q.bankId)) }
    }

    suspend fun isFavorite(qid: Long) = repo.isFavorite(qid)

    fun clearSession() {
        _session.value = null
        _index.value = 0
        _revealed.value = false
        _finished.value = false
    }
}
