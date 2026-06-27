package com.example.quizapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.quizapp.data.entities.QuestionBank
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionBankDao {
    @Query("SELECT * FROM question_banks ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<QuestionBank>>

    @Query("SELECT * FROM question_banks WHERE id = :id")
    suspend fun getById(id: Long): QuestionBank?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bank: QuestionBank): Long

    @Update
    suspend fun update(bank: QuestionBank)

    @Delete
    suspend fun delete(bank: QuestionBank)

    @Query("SELECT COUNT(*) FROM questions WHERE bankId = :bankId")
    suspend fun countQuestions(bankId: Long): Int
}
