package com.example.quizapp.data

import android.content.Context
import com.example.quizapp.data.dao.QuestionBankDao
import com.example.quizapp.data.dao.QuestionDao
import com.example.quizapp.data.dao.RecordDao
import com.example.quizapp.data.entities.ExamSession
import com.example.quizapp.data.entities.FavoriteRecord
import com.example.quizapp.data.entities.Question
import com.example.quizapp.data.entities.QuestionBank
import com.example.quizapp.data.entities.QuestionProgress
import com.example.quizapp.data.entities.WrongRecord
import com.example.quizapp.data.exchange.BankExchange
import com.example.quizapp.data.exchange.ImportExporter
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

class QuizRepository(
    private val bankDao: QuestionBankDao,
    private val questionDao: QuestionDao,
    private val recordDao: RecordDao,
    private val _imageStore: ImageStore
) {
    val imageStore get() = _imageStore

    // Banks
    fun observeBanks(): Flow<List<QuestionBank>> = bankDao.observeAll()
    suspend fun getBank(id: Long) = bankDao.getById(id)
    suspend fun upsertBank(bank: QuestionBank): Long =
        if (bank.id == 0L) bankDao.insert(bank) else { bankDao.update(bank); bank.id }
    suspend fun deleteBank(bank: QuestionBank) = bankDao.delete(bank)

    // Questions
    fun observeQuestions(bankId: Long) = questionDao.observeByBank(bankId)
    suspend fun getQuestions(bankId: Long) = questionDao.getByBank(bankId)
    suspend fun getQuestion(id: Long) = questionDao.getById(id)
    suspend fun getQuestionsByIds(ids: List<Long>) = questionDao.getByIds(ids)
    fun countFlow(bankId: Long) = questionDao.countFlow(bankId)
    suspend fun typeCounts(bankId: Long) = questionDao.typeCounts(bankId)
    fun search(bankId: Long, kw: String) = questionDao.search(bankId, kw)
    suspend fun getByBankAndType(bankId: Long, type: QuestionType) = questionDao.getByBankAndType(bankId, type)
    suspend fun allFromBanks(ids: List<Long>) = questionDao.allFromBanks(ids)
    suspend fun upsertQuestion(q: Question): Long =
        if (q.id == 0L) questionDao.insert(q) else { questionDao.update(q); q.id }
    suspend fun deleteQuestion(q: Question) = questionDao.delete(q)

    // Import / Export
    suspend fun importBank(ex: BankExchange): Long {
        val (bank, questions) = ImportExporter.toEntities(ex, imageStore)
        val bankId = bankDao.insert(bank.copy(updatedAt = System.currentTimeMillis()))
        questions.forEach { q ->
            questionDao.insert(q.copy(bankId = bankId))
        }
        return bankId
    }

    suspend fun importJson(stream: InputStream): Long = importBank(ImportExporter.parseJson(stream))

    suspend fun importXlsx(stream: InputStream): Long = importBank(ImportExporter.parseXlsx(stream))

    suspend fun exportJson(bankId: Long, out: OutputStream) {
        val bank = bankDao.getById(bankId) ?: error("题库不存在")
        val questions = questionDao.getByBank(bankId)
        out.write(ImportExporter.exportJson(bank, questions, imageStore).toByteArray())
    }

    suspend fun exportXlsx(bankId: Long, out: OutputStream) {
        val bank = bankDao.getById(bankId) ?: error("题库不存在")
        val questions = questionDao.getByBank(bankId)
        ImportExporter.exportXlsx(bank, questions, out)
    }

    // Progress
    suspend fun bankStats(bankId: Long): BankStats {
        val counts = questionDao.typeCounts(bankId)
        val totalCount = counts.sumOf { it.c }
        val practiced = recordDao.practicedCount(bankId)
        val mastered = recordDao.masteredCount(bankId)
        return BankStats(totalCount, practiced, mastered, counts)
    }

    suspend fun recordAnswer(question: Question, bankId: Long, correct: Boolean, source: RecordSource,
                             userAnswerJson: String, correctAnswerJson: String) {
        val prog = recordDao.progress(question.id) ?: QuestionProgress(questionId = question.id, bankId = bankId)
        prog.practicedCount += 1
        if (correct) prog.correctCount += 1
        prog.lastPracticedAt = System.currentTimeMillis()
        prog.mastered = prog.correctCount >= 2 && prog.practicedCount - prog.correctCount == 0
        recordDao.upsertProgress(prog)

        if (correct) {
            recordDao.deleteWrong(question.id, source)
        } else {
            recordDao.insertWrong(WrongRecord(
                questionId = question.id, bankId = bankId, source = source,
                userAnswerJson = userAnswerJson, correctAnswerJson = correctAnswerJson
            ))
        }
    }

    // Favorites
    fun observeFavorites() = recordDao.observeFavorites()
    fun observeFavoriteCount() = recordDao.observeFavoritesFlow()
    suspend fun isFavorite(qid: Long) = recordDao.favorite(qid) != null
    suspend fun deleteFavorite(qid: Long) = recordDao.deleteFavorite(qid)
    suspend fun toggleFavorite(question: Question, bankId: Long): Boolean {
        val existing = recordDao.favorite(question.id)
        return if (existing == null) {
            recordDao.insertFavorite(FavoriteRecord(questionId = question.id, bankId = bankId))
            true
        } else {
            recordDao.deleteFavorite(question.id)
            false
        }
    }

    // Wrong
    fun observeWrong(source: RecordSource) = recordDao.observeWrong(source)
    fun wrongCountFlow(source: RecordSource) = recordDao.wrongCountFlow(source)
    suspend fun deleteWrongById(id: Long) = recordDao.deleteWrongById(id)

    // Sessions
    fun observeSessions() = recordDao.observeSessions()
    suspend fun saveSession(s: ExamSession) = recordDao.insertSession(s)

    companion object {
        fun from(context: Context): QuizRepository {
            val db = AppDatabase.getInstance(context)
            return QuizRepository(db.bankDao(), db.questionDao(), db.recordDao(), ImageStore(context))
        }
    }
}

data class BankStats(
    val totalQuestions: Int,
    val practicedCount: Int,
    val masteredCount: Int,
    val typeCounts: List<com.example.quizapp.data.dao.TypeCount>
)
