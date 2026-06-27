package com.example.quizapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.quizapp.data.QuestionType
import com.example.quizapp.data.entities.Question
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE bankId = :bankId ORDER BY id ASC")
    fun observeByBank(bankId: Long): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE bankId = :bankId ORDER BY id ASC")
    suspend fun getByBank(bankId: Long): List<Question>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: Long): Question?

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Question>

    @Query("SELECT * FROM questions WHERE bankId = :bankId AND type = :type ORDER BY id ASC")
    suspend fun getByBankAndType(bankId: Long, type: QuestionType): List<Question>

    @Query("SELECT * FROM questions WHERE bankId = :bankId AND (stem LIKE '%' || :kw || '%' OR explanation LIKE '%' || :kw || '%') ORDER BY id ASC")
    fun search(bankId: Long, kw: String): Flow<List<Question>>

    @Query("SELECT COUNT(*) FROM questions WHERE bankId = :bankId")
    fun countFlow(bankId: Long): Flow<Int>

    @Query("SELECT type, COUNT(*) as c FROM questions WHERE bankId = :bankId GROUP BY type")
    suspend fun typeCounts(bankId: Long): List<TypeCount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: Question): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<Question>): List<Long>

    @Update
    suspend fun update(question: Question)

    @Delete
    suspend fun delete(question: Question)

    @Query("SELECT * FROM questions WHERE bankId IN (:bankIds) ORDER BY RANDOM()")
    suspend fun allFromBanks(bankIds: List<Long>): List<Question>
}

data class TypeCount(val type: QuestionType, val c: Int)
