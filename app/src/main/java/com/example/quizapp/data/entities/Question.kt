package com.example.quizapp.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.quizapp.data.AnswerData
import com.example.quizapp.data.OptionItem
import com.example.quizapp.data.QuestionType

@Entity(
    tableName = "questions",
    foreignKeys = [ForeignKey(
        entity = QuestionBank::class,
        parentColumns = ["id"],
        childColumns = ["bankId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("bankId")]
)
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankId: Long,
    val type: QuestionType,
    val stem: String,
    val stemImage: String? = null,
    val options: List<OptionItem> = emptyList(),
    val answer: AnswerData = AnswerData(),
    val explanation: String = "",
    val explanationImage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
