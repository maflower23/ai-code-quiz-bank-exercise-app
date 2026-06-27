package com.example.quizapp.ui.navigation

sealed class Routes(val route: String) {
    object Banks : Routes("banks")
    object BankDetail : Routes("bank_detail/{bankId}") {
        fun create(bankId: Long) = "bank_detail/$bankId"
    }
    object BankEdit : Routes("bank_edit/{bankId}") {
        fun create(bankId: Long) = "bank_edit/$bankId"
    }
    object QuestionEdit : Routes("question_edit/{bankId}/{questionId}") {
        fun create(bankId: Long, questionId: Long) = "question_edit/$bankId/$questionId"
    }
    object Practice : Routes("practice")
    object Quiz : Routes("quiz")
    object QuizResult : Routes("quiz_result")
    object Records : Routes("records")
}
