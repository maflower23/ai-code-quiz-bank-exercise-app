package com.example.quizapp

import android.app.Application
import com.example.quizapp.data.AppDatabase

class QuizApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
