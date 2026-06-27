package com.example.quizapp.data.exchange

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/** Self-contained JSON exchange format (images embedded as base64). */
data class BankExchange(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("questions") val questions: List<QuestionExchange> = emptyList()
)

data class QuestionExchange(
    @SerializedName("type") val type: String,
    @SerializedName("stem") val stem: String,
    @SerializedName("stemImage") val stemImage: String? = null,
    @SerializedName("options") val options: List<OptionExchange> = emptyList(),
    @SerializedName("answerIndices") val answerIndices: List<Int> = emptyList(),
    @SerializedName("answerJudge") val answerJudge: Boolean = false,
    @SerializedName("answerBlanks") val answerBlanks: List<List<String>> = emptyList(),
    @SerializedName("explanation") val explanation: String = "",
    @SerializedName("explanationImage") val explanationImage: String? = null
)

data class OptionExchange(
    @SerializedName("text") val text: String,
    @SerializedName("image") val image: String? = null
)

object ExchangeJson {
    val gson: Gson = Gson()
}
