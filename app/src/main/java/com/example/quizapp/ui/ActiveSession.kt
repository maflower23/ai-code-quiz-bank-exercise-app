package com.example.quizapp.ui

import com.example.quizapp.data.QuizSession

/** In-memory holder to pass a prepared quiz session to the quiz screen. */
object ActiveSession {
    var session: QuizSession? = null
    var sourceBankIdForPractice: Long = 0
}
