package com.example.quizapp.data.exchange

import com.example.quizapp.data.AnswerData
import com.example.quizapp.data.ImageStore
import com.example.quizapp.data.OptionItem
import com.example.quizapp.data.QuestionType
import com.example.quizapp.data.entities.Question
import com.example.quizapp.data.entities.QuestionBank
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.io.OutputStream

object ImportExporter {

    // ---------- JSON ----------
    fun parseJson(stream: InputStream): BankExchange {
        val text = stream.bufferedReader().use { it.readText() }
        return ExchangeJson.gson.fromJson(text, BankExchange::class.java)
            ?: throw IllegalArgumentException("JSON 内容为空或格式错误")
    }

    fun parseJsonString(text: String): BankExchange =
        ExchangeJson.gson.fromJson(text, BankExchange::class.java)
            ?: throw IllegalArgumentException("JSON 内容为空或格式错误")

    /** Convert parsed exchange into entities (saving embedded images). */
    fun toEntities(ex: BankExchange, imageStore: ImageStore): Pair<QuestionBank, List<Question>> {
        val bank = QuestionBank(name = ex.name, description = ex.description)
        val questions = ex.questions.mapIndexed { _, qe ->
            val type = QuestionType.fromName(qe.type)
            val answer = when (type) {
                QuestionType.SINGLE, QuestionType.MULTIPLE -> AnswerData(indices = qe.answerIndices)
                QuestionType.JUDGE -> AnswerData(judge = qe.answerJudge)
                QuestionType.FILL -> AnswerData(blanks = qe.answerBlanks)
            }
            Question(
                bankId = 0,
                type = type,
                stem = qe.stem,
                stemImage = imageStore.saveBase64(qe.stemImage),
                options = qe.options.map { OptionItem(it.text, imageStore.saveBase64(it.image)) },
                answer = answer,
                explanation = qe.explanation,
                explanationImage = imageStore.saveBase64(qe.explanationImage)
            )
        }
        return bank to questions
    }

    fun exportJson(bank: QuestionBank, questions: List<Question>, imageStore: ImageStore): String {
        val ex = BankExchange(
            name = bank.name,
            description = bank.description,
            questions = questions.map { q ->
                QuestionExchange(
                    type = q.type.name,
                    stem = q.stem,
                    stemImage = imageStore.toBase64(q.stemImage),
                    options = q.options.map { OptionExchange(it.text, imageStore.toBase64(it.imagePath)) },
                    answerIndices = q.answer.indices,
                    answerJudge = q.answer.judge,
                    answerBlanks = q.answer.blanks,
                    explanation = q.explanation,
                    explanationImage = imageStore.toBase64(q.explanationImage)
                )
            }
        )
        return ExchangeJson.gson.toJson(ex)
    }

    // ---------- XLSX ----------
    // Columns: 类型 | 题干 | 选项 | 答案 | 解析
    // Options (single/multiple): "A.文本||B.文本||..."  (the leading letter is stripped)
    // Answer: single -> "A" ; multiple -> "A,C" ; judge -> "对"/"错"
    // Fill answer: blanks separated by "##", alternatives within a blank by "//"
    private val optionSep = "||"
    private val blankSep = "##"
    private val altSep = "//"

    fun parseXlsx(stream: InputStream): BankExchange {
        val wb = WorkbookFactory.create(stream)
        val sheet = wb.getSheetAt(0)
        val header = sheet.getRow(0) ?: throw IllegalArgumentException("Excel 没有表头行")
        fun colIdx(name: String): Int {
            for (i in 0 until header.lastCellNum.toInt()) {
                if (header.getCell(i)?.toString()?.trim() == name) return i
            }
            return -1
        }
        val iType = colIdx("类型")
        val iStem = colIdx("题干")
        val iOpts = colIdx("选项")
        val iAns = colIdx("答案")
        val iExp = colIdx("解析")
        if (iType < 0 || iStem < 0 || iAns < 0)
            throw IllegalArgumentException("Excel 表头需包含: 类型/题干/答案 (可选: 选项/解析)")

        val questions = mutableListOf<QuestionExchange>()
        for (r in 1..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            fun cell(i: Int): String = if (i < 0) "" else row.getCell(i)?.let { c ->
                when (c.cellType) {
                    CellType.NUMERIC -> c.numericCellValue.toInt().toString()
                    CellType.BOOLEAN -> c.booleanCellValue.toString()
                    CellType.FORMULA -> c.cellFormula
                    else -> c.toString().trim()
                }
            } ?: ""
            val typeStr = cell(iType)
            if (typeStr.isBlank()) continue
            val type = parseTypeCn(typeStr)
            val stem = cell(iStem)
            val optsStr = cell(iOpts)
            val ansStr = cell(iAns)
            val exp = cell(iExp)

            when (type) {
                QuestionType.SINGLE, QuestionType.MULTIPLE -> {
                    val opts = optsStr.split(optionSep).map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .map { stripLetter(it) }
                    val indices = ansStr.split(",", "，", " ").mapNotNull { tok ->
                        val t = tok.trim().toUpperCase()
                        if (t.isEmpty()) null else (t.first() - 'A')
                    }.filter { it in opts.indices }
                    questions += QuestionExchange(type.name, stem, null,
                        opts.map { OptionExchange(it) }, indices)
                }
                QuestionType.JUDGE -> {
                    questions += QuestionExchange(type.name, stem, null,
                        emptyList(), answerJudge = ansStr.contains("对") || ansStr.equals("true", true) || ansStr == "1",
                        explanation = exp)
                }
                QuestionType.FILL -> {
                    val blanks = ansStr.split(blankSep).map { b ->
                        b.split(altSep).map { it.trim() }.filter { it.isNotEmpty() }
                    }.filter { it.isNotEmpty() }
                    questions += QuestionExchange(type.name, stem, null,
                        emptyList(), answerBlanks = blanks, explanation = exp)
                }
            }
        }
        wb.close()
        return BankExchange("导入的题库", "", questions)
    }

    fun exportXlsx(bank: QuestionBank, questions: List<Question>, out: OutputStream) {
        val wb = XSSFWorkbook()
        val sheet = wb.createSheet(bank.name.take(28))
        val header = sheet.createRow(0)
        listOf("类型", "题干", "选项", "答案", "解析").forEachIndexed { i, h -> header.createCell(i).setCellValue(h) }

        questions.forEachIndexed { idx, q ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(q.type.displayName)
            row.createCell(1).setCellValue(q.stem)
            when (q.type) {
                QuestionType.SINGLE, QuestionType.MULTIPLE -> {
                    val optStr = q.options.mapIndexed { i, o -> "${('A' + i)}.$o" }.joinToString(optionSep)
                    row.createCell(2).setCellValue(optStr)
                    val ansStr = q.answer.indices.sorted().joinToString(",") { "${('A' + it)}" }
                    row.createCell(3).setCellValue(ansStr)
                }
                QuestionType.JUDGE -> {
                    row.createCell(3).setCellValue(if (q.answer.judge) "对" else "错")
                }
                QuestionType.FILL -> {
                    val ansStr = q.answer.blanks.joinToString(blankSep) { b -> b.joinToString(altSep) }
                    row.createCell(3).setCellValue(ansStr)
                }
            }
            row.createCell(4).setCellValue(q.explanation)
        }
        for (i in 0..4) sheet.setColumnWidth(i, 18 * 256)
        wb.write(out)
        wb.close()
    }

    private fun parseTypeCn(s: String): QuestionType {
        val t = s.trim()
        return when {
            t.contains("单") -> QuestionType.SINGLE
            t.contains("多") -> QuestionType.MULTIPLE
            t.contains("判断") || t == "判断题" -> QuestionType.JUDGE
            t.contains("填空") -> QuestionType.FILL
            else -> QuestionType.fromName(t)
        }
    }

    private fun stripLetter(s: String): String {
        val t = s.trim()
        if (t.length >= 2 && t[0] in 'A'..'Z' && (t[1] == '.' || t[1] == '．' || t[1] == '、')) {
            return t.substring(2).trim()
        }
        return t
    }
}
