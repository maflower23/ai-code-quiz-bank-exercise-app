package com.example.quizapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_banks")
data class QuestionBank(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
