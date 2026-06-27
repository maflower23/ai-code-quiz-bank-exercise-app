package com.example.quizapp.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.quizapp.data.RecordSource

/** Per-question progress / statistics. */
@Entity(tableName = "question_progress", indices = [Index(value = ["questionId"], unique = true)])
data class QuestionProgress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: Long,
    val bankId: Long,
    var practicedCount: Int = 0,
    var correctCount: Int = 0,
    var lastPracticedAt: Long = 0,
    var mastered: Boolean = false
)

/** A wrong-answer record (practice or exam). */
@Entity(tableName = "wrong_records",
    indices = [Index(value = ["questionId", "source"], unique = true), Index("bankId")])
data class WrongRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: Long,
    val bankId: Long,
    val source: RecordSource,
    val userAnswerJson: String,
    val correctAnswerJson: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites", indices = [Index(value = ["questionId"], unique = true)])
data class FavoriteRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: Long,
    val bankId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exam_sessions")
data class ExamSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankIds: List<Long> = emptyList(),
    val mode: String,            // PRACTICE / RANDOM_EXAM / MIXED_EXAM
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val startedAt: Long,
    val finishedAt: Long = System.currentTimeMillis()
)
