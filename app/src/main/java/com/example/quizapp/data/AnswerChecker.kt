package com.example.quizapp.data

import com.example.quizapp.data.entities.Question

/** Represents the user's in-progress answer for a question. */
sealed class UserAnswer {
    object Unanswered : UserAnswer()
    data class Indices(val selected: Set<Int>) : UserAnswer()       // SINGLE / MULTIPLE
    data class Judge(val value: Boolean) : UserAnswer()             // JUDGE
    data class Fills(val values: List<String>) : UserAnswer()       // FILL
}

object AnswerChecker {
    fun isCorrect(question: Question, user: UserAnswer): Boolean {
        if (user is UserAnswer.Unanswered) return false
        return when (question.type) {
            QuestionType.SINGLE -> {
                val u = (user as? UserAnswer.Indices)?.selected ?: return false
                val correct = question.answer.indices.toSet()
                u.size == 1 && u == correct
            }
            QuestionType.MULTIPLE -> {
                val u = (user as? UserAnswer.Indices)?.selected ?: return false
                val correct = question.answer.indices.toSet()
                u == correct
            }
            QuestionType.JUDGE -> {
                (user as? UserAnswer.Judge)?.value == question.answer.judge
            }
            QuestionType.FILL -> {
                val u = (user as? UserAnswer.Fills)?.values ?: return false
                val blanks = question.answer.blanks
                if (u.size != blanks.size) return false
                u.indices.all { i ->
                    val given = u[i].trim()
                    blanks[i].any { it.trim().equals(given, true) }
                }
            }
        }
    }

    fun userAnswerJson(user: UserAnswer): String = when (user) {
        is UserAnswer.Indices -> Json.to(user.selected.toList())
        is UserAnswer.Judge -> user.value.toString()
        is UserAnswer.Fills -> Json.to(user.values)
        UserAnswer.Unanswered -> ""
    }

    /** Build a UserAnswer representing the question's correct answer (for record storage). */
    fun correctUserAnswer(q: Question): UserAnswer = when (q.type) {
        QuestionType.SINGLE, QuestionType.MULTIPLE -> UserAnswer.Indices(q.answer.indices.toSet())
        QuestionType.JUDGE -> UserAnswer.Judge(q.answer.judge)
        QuestionType.FILL -> UserAnswer.Fills(q.answer.blanks.map { it.firstOrNull() ?: "" })
    }

    fun describeUser(question: Question, user: UserAnswer): String = when (question.type) {
        QuestionType.SINGLE, QuestionType.MULTIPLE -> {
            val u = (user as? UserAnswer.Indices)?.selected ?: emptySet()
            u.sorted().joinToString(", ") { "${'A' + it}" }.ifEmpty { "未作答" }
        }
        QuestionType.JUDGE -> (user as? UserAnswer.Judge)?.value?.let { if (it) "对" else "错" } ?: "未作答"
        QuestionType.FILL -> (user as? UserAnswer.Fills)?.values?.joinToString(" | ") { it.ifBlank { "空" } } ?: "未作答"
    }

    fun describeCorrect(question: Question): String = when (question.type) {
        QuestionType.SINGLE, QuestionType.MULTIPLE ->
            question.answer.indices.sorted().joinToString(", ") { "${'A' + it}" }
        QuestionType.JUDGE -> if (question.answer.judge) "对" else "错"
        QuestionType.FILL -> question.answer.blanks.joinToString(" | ") { b -> b.joinToString("/") }
    }
}
