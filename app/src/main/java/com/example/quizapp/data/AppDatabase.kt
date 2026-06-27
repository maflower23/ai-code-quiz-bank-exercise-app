package com.example.quizapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.quizapp.data.dao.QuestionBankDao
import com.example.quizapp.data.dao.QuestionDao
import com.example.quizapp.data.dao.RecordDao
import com.example.quizapp.data.entities.ExamSession
import com.example.quizapp.data.entities.FavoriteRecord
import com.example.quizapp.data.entities.Question
import com.example.quizapp.data.entities.QuestionBank
import com.example.quizapp.data.entities.QuestionProgress
import com.example.quizapp.data.entities.WrongRecord

@Database(
    entities = [
        QuestionBank::class,
        Question::class,
        QuestionProgress::class,
        WrongRecord::class,
        FavoriteRecord::class,
        ExamSession::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bankDao(): QuestionBankDao
    abstract fun questionDao(): QuestionDao
    abstract fun recordDao(): RecordDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quiz.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
