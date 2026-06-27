package com.example.quizapp.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun typeToString(t: QuestionType?): String? = t?.name

    @TypeConverter
    fun stringToType(s: String?): QuestionType? = s?.let { QuestionType.fromName(it) }

    @TypeConverter
    fun optionsToString(opts: List<OptionItem>?): String =
        if (opts.isNullOrEmpty()) "[]" else Json.to(opts)

    @TypeConverter
    fun stringToOptions(s: String?): List<OptionItem> =
        if (s.isNullOrBlank()) emptyList() else try { Json.from<List<OptionItem>>(s) } catch (_: Exception) { emptyList() }

    @TypeConverter
    fun answerToString(a: AnswerData?): String = a?.let { Json.to(it) } ?: "{}"

    @TypeConverter
    fun stringToAnswer(s: String?): AnswerData =
        if (s.isNullOrBlank()) AnswerData() else try { Json.from<AnswerData>(s) } catch (_: Exception) { AnswerData() }

    @TypeConverter
    fun sourceToString(s: RecordSource?): String? = s?.name

    @TypeConverter
    fun stringToSource(s: String?): RecordSource? = s?.let { runCatching { RecordSource.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun longListToString(l: List<Long>?): String = if (l.isNullOrEmpty()) "[]" else Json.to(l)

    @TypeConverter
    fun stringToLongList(s: String?): List<Long> =
        if (s.isNullOrBlank()) emptyList() else try { Json.from<List<Long>>(s) } catch (_: Exception) { emptyList() }
}
