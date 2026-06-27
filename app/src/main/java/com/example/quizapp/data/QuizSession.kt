package com.example.quizapp.data

import com.example.quizapp.data.entities.Question

enum class QuizMode { PRACTICE, RANDOM_EXAM, MIXED_EXAM }

data class QuizConfig(
    val mode: QuizMode,
    val bankIds: List<Long>,
    val count: Int = 0,            // 0 = all (for practice / random exam)
    val shuffleOptions: Boolean = true
)

/**
 * A prepared quiz session. Options of single/multiple questions may be shuffled
 * (the answer indices are remapped accordingly).
 */
class QuizSession(
    val questions: List<Question>,
    val config: QuizConfig
) {
    val answers: MutableMap<Long, UserAnswer> = mutableMapOf()
    val corrected: MutableMap<Long, Boolean> = mutableMapOf()

    /** Per-question option index remap (after shuffle). Original index -> display index. */
    private val remap: Map<Long, List<Int>> = buildRemap()

    private fun buildRemap(): Map<Long, List<Int>> {
        val map = HashMap<Long, List<Int>>()
        if (!config.shuffleOptions) return map
        questions.forEach { q ->
            if (q.type == QuestionType.SINGLE || q.type == QuestionType.MULTIPLE) {
                val order = q.options.indices.shuffled()
                map[q.id] = order
            }
        }
        return map
    }

    /** Display order of option indices for a question (after shuffle). */
    fun displayOrder(q: Question): List<Int> = remap[q.id] ?: q.options.indices.toList()

    /** Convert a selected display index back to the original option index. */
    fun toOriginal(q: Question, displayIdx: Int): Int {
        val order = remap[q.id] ?: return displayIdx
        return order[displayIdx]
    }

    fun size() = questions.size
    fun currentIndex(index: Int): Question = questions[index]
    fun setUserAnswer(q: Question, ua: UserAnswer) { answers[q.id] = ua }
    fun userAnswer(q: Question): UserAnswer = answers[q.id] ?: UserAnswer.Unanswered
    fun hasAnswered(q: Question) = answers[q.id] != null && answers[q.id] !is UserAnswer.Unanswered

    /** Evaluate correctness for a question (using original-space comparison). */
    fun evaluate(q: Question): Boolean {
        val raw = answers[q.id] ?: return false
        // Convert display indices to original space for single/multiple
        val normalized = when (raw) {
            is UserAnswer.Indices -> UserAnswer.Indices(raw.selected.map { toOriginal(q, it) }.toSet())
            else -> raw
        }
        val ok = AnswerChecker.isCorrect(q, normalized)
        corrected[q.id] = ok
        return ok
    }

    fun correctCount(): Int = corrected.count { it.value }
    fun wrongCount(): Int = corrected.count { !it.value }
    fun answeredCount(): Int = corrected.size
}
