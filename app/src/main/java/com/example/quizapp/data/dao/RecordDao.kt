package com.example.quizapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.quizapp.data.RecordSource
import com.example.quizapp.data.entities.ExamSession
import com.example.quizapp.data.entities.FavoriteRecord
import com.example.quizapp.data.entities.QuestionProgress
import com.example.quizapp.data.entities.WrongRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    // ---- Progress ----
    @Query("SELECT * FROM question_progress WHERE bankId = :bankId")
    suspend fun progressByBank(bankId: Long): List<QuestionProgress>

    @Query("SELECT * FROM question_progress WHERE questionId = :qid")
    suspend fun progress(qid: Long): QuestionProgress?

    @Upsert
    suspend fun upsertProgress(p: QuestionProgress)

    @Query("SELECT COUNT(*) FROM question_progress WHERE bankId = :bankId AND practicedCount > 0")
    suspend fun practicedCount(bankId: Long): Int

    @Query("SELECT COUNT(*) FROM question_progress WHERE bankId = :bankId AND mastered = 1")
    suspend fun masteredCount(bankId: Long): Int

    // ---- Wrong records ----
    @Query("SELECT * FROM wrong_records WHERE source = :source ORDER BY addedAt DESC")
    fun observeWrong(source: RecordSource): Flow<List<WrongRecord>>

    @Query("SELECT * FROM wrong_records WHERE bankId = :bankId AND source = :source ORDER BY addedAt DESC")
    fun observeWrongByBank(bankId: Long, source: RecordSource): Flow<List<WrongRecord>>

    @Query("SELECT * FROM wrong_records WHERE questionId = :qid AND source = :source LIMIT 1")
    suspend fun wrongFor(qid: Long, source: RecordSource): WrongRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWrong(r: WrongRecord): Long

    @Query("DELETE FROM wrong_records WHERE questionId = :qid AND source = :source")
    suspend fun deleteWrong(qid: Long, source: RecordSource)

    @Query("DELETE FROM wrong_records WHERE id = :id")
    suspend fun deleteWrongById(id: Long)

    @Query("SELECT COUNT(DISTINCT questionId) FROM wrong_records WHERE source = :source")
    fun wrongCountFlow(source: RecordSource): Flow<Int>

    // ---- Favorites ----
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteRecord>>

    @Query("SELECT * FROM favorites WHERE questionId = :qid LIMIT 1")
    suspend fun favorite(qid: Long): FavoriteRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(f: FavoriteRecord)

    @Query("DELETE FROM favorites WHERE questionId = :qid")
    suspend fun deleteFavorite(qid: Long)

    @Query("SELECT COUNT(*) FROM favorites")
    fun observeFavoritesFlow(): Flow<Int>

    // ---- Sessions ----
    @Query("SELECT * FROM exam_sessions ORDER BY finishedAt DESC")
    fun observeSessions(): Flow<List<ExamSession>>

    @Insert
    suspend fun insertSession(s: ExamSession): Long
}
