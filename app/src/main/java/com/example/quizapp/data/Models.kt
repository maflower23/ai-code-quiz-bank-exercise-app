package com.example.quizapp.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

enum class QuestionType(val displayName: String) {
    SINGLE("单选题"),
    MULTIPLE("多选题"),
    JUDGE("判断题"),
    FILL("填空题");

    companion object {
        fun fromName(s: String?): QuestionType =
            entries.firstOrNull { it.name.equals(s, true) } ?: SINGLE
    }
}

data class OptionItem(
    val text: String = "",
    val imagePath: String? = null
)

/** Answer storage. Which field is used depends on QuestionType. */
data class AnswerData(
    val indices: List<Int> = emptyList(),      // SINGLE / MULTIPLE option indices
    val judge: Boolean = false,                 // JUDGE true=对 false=错
    val blanks: List<List<String>> = emptyList()// FILL: each blank -> acceptable answers
)

enum class RecordSource { PRACTICE, EXAM }

object Json {
    val gson: Gson = Gson()
    inline fun <reified T> from(json: String?): T =
        gson.fromJson(json ?: "", object : TypeToken<T>() {}.type)
    fun <T> to(obj: T): String = gson.toJson(obj)
}
