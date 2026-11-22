package com.example.ui.components

import com.example.data.NoteEntity
import com.example.data.NoteExtrasData
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class NoteSmartCategory(val label: String) {
  ALL("All"),
  LINKS("Links"),
  EXPENSES("Expenses"),
  CHECKLISTS("Checklists"),
  DRAWINGS("Drawings"),
  IMAGES("Images")
}

private val CURRENCY_REGEX = Regex("[₹\$€£]")
// Strict inline-math result pattern: requires an arithmetic operator (+, -, *, /) or '%' before '= <number>'
private val CALC_TOTAL_REGEX = Regex(
  "(?:\\d[\\d.,]*\\s*[+\\-*/]\\s*\\d|\\d[\\d.,]*\\s*%)[^\\n=]{0,40}=\\s*-?\\d[\\d.,]*"
)

fun matchesSmartCategory(
  note: NoteEntity,
  extras: NoteExtrasData,
  category: NoteSmartCategory
): Boolean {
  return when (category) {
    NoteSmartCategory.ALL -> true
    NoteSmartCategory.LINKS -> {
      detectUrls(note.content).isNotEmpty() ||
        detectUrls(note.title).isNotEmpty() ||
        extras.checklist.any { detectUrls(it.text).isNotEmpty() } ||
        extras.codeBlocks.any { detectUrls(it.text).isNotEmpty() }
    }
    NoteSmartCategory.EXPENSES -> {
      val combined = buildString {
        append(note.title).append('\n').append(note.content)
        extras.checklist.forEach { append('\n').append(it.text) }
      }
      CURRENCY_REGEX.containsMatchIn(combined) || CALC_TOTAL_REGEX.containsMatchIn(combined)
    }
    NoteSmartCategory.CHECKLISTS -> extras.checklist.isNotEmpty()
    NoteSmartCategory.DRAWINGS -> {
      (extras.doodle?.strokes?.isNotEmpty() == true) || extractDoodleCount(note.content) > 0
    }
    NoteSmartCategory.IMAGES -> extras.images.any { File(it).exists() }
  }
}

fun formatCountdownBadge(expiresAt: Long, nowMs: Long = System.currentTimeMillis()): String {
  if (expiresAt <= 0L) return ""
  val remainingMs = expiresAt - nowMs
  if (remainingMs <= 0L) return "Expired"
  val totalMinutes = remainingMs / 60_000L
  val totalHours = remainingMs / 3_600_000L
  val totalDays = remainingMs / 86_400_000L
  return when {
    totalDays >= 1L -> "${totalDays}d left"
    totalHours >= 1L -> "${totalHours}h left"
    totalMinutes >= 1L -> "${totalMinutes}m left"
    else -> "<1m left"
  }
}

fun formatVersionTimestamp(timestamp: Long): String {
  if (timestamp <= 0L) return ""
  return SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(Date(timestamp))
}

data class MathAutoEvalResult(
  val updatedText: String,
  val newCursorPos: Int,
  val formattedAnswer: String
)

/**
 * Smart Inline Calculator:
 * When the user types Space or Enter right after a valid math expression ending with '=',
 * safely evaluates +, -, *, /, parentheses, and percentages using a recursive-descent parser
 * (zero eval) and inserts the formatted result cleanly right after '='.
 */
fun evaluateInlineMathIfTriggered(
  oldText: String,
  newText: String,
  newCursorPos: Int
): MathAutoEvalResult? {
  // Only trigger on single-character Space or Enter insertion
  if (newText.length != oldText.length + 1) return null
  if (newCursorPos <= 1 || newCursorPos > newText.length) return null

  val triggerChar = newText[newCursorPos - 1]
  if (triggerChar != ' ' && triggerChar != '\n') return null

  // Character right before the trigger must be '='
  val eqPos = newCursorPos - 2
  if (newText[eqPos] != '=') return null

  // Extract current line before '='
  val lineStart = newText.lastIndexOf('\n', startIndex = (eqPos - 1).coerceAtLeast(0)) + 1
  val beforeEq = newText.substring(lineStart, eqPos)

  // Find trailing math expression on this line (allow labels like "Total: 450 + 1200 * 2 ")
  val exprMatch = Regex("([0-9()+\\-*/.%\\s]+)$").find(beforeEq) ?: return null
  val rawExpr = exprMatch.value.trim()
  if (rawExpr.isEmpty()) return null

  // Must contain at least one math operator or '%' so plain "x = " or "1 = " isn't triggered
  val hasOperator = rawExpr.any { it == '+' || it == '-' || it == '*' || it == '/' || it == '%' }
  val hasDigit = rawExpr.any { it.isDigit() }
  if (!hasOperator || !hasDigit) return null

  val result = SafeMathParser.evaluate(rawExpr) ?: return null
  if (result.isNaN() || result.isInfinite()) return null

  val formatted = formatMathResult(result)
  val insertion = if (triggerChar == ' ') {
    "$formatted "
  } else {
    " $formatted\n"
  }

  val prefix = newText.substring(0, newCursorPos)
  val suffix = newText.substring(newCursorPos)
  val updatedText = if (triggerChar == ' ') {
    "$prefix$insertion$suffix"
  } else {
    // Place result on the same line right after '=', then the newline
    newText.substring(0, eqPos + 1) + insertion + suffix
  }
  val cursorAfter = if (triggerChar == ' ') {
    prefix.length + insertion.length
  } else {
    (eqPos + 1) + insertion.length
  }
  return MathAutoEvalResult(
    updatedText = updatedText,
    newCursorPos = cursorAfter,
    formattedAnswer = formatted
  )
}

private fun formatMathResult(value: Double): String {
  if (abs(value) < 1e-12) return "0"
  return try {
    val bd = BigDecimal.valueOf(value)
      .setScale(6, RoundingMode.HALF_UP)
      .stripTrailingZeros()
    bd.toPlainString()
  } catch (_: Exception) {
    value.toString()
  }
}

/**
 * Safe recursive-descent arithmetic parser supporting:
 * +, -, *, /, unary +/-, nested parentheses (), decimals, and percentages
 * (both standalone e.g. "50%" -> 0.5 and additive/subtractive e.g. "200 + 10%" -> 220).
 */
object SafeMathParser {
  private class Parser(private val input: String) {
    private var pos = 0

    fun parse(): Double? {
      val v = parseExpression() ?: return null
      skipWhitespace()
      return if (pos == input.length) v else null
    }

    private fun skipWhitespace() {
      while (pos < input.length && input[pos].isWhitespace()) pos++
    }

    private fun peek(): Char? {
      skipWhitespace()
      return if (pos < input.length) input[pos] else null
    }

    private fun parseExpression(): Double? {
      var left = parseTerm() ?: return null
      while (true) {
        val op = peek()
        if (op != '+' && op != '-') break
        pos++
        val (rightVal, isPercent) = parseTermWithPercentFlag() ?: return null
        val effectiveRight = if (isPercent) left * rightVal else rightVal
        left = if (op == '+') left + effectiveRight else left - effectiveRight
      }
      return left
    }

    private fun parseTerm(): Double? = parseTermWithPercentFlag()?.first

    private fun parseTermWithPercentFlag(): Pair<Double, Boolean>? {
      val first = parseFactorWithPercentFlag() ?: return null
      var left = first.first
      var wasSinglePercent = first.second

      while (true) {
        val op = peek()
        if (op != '*' && op != '/') break
        pos++
        wasSinglePercent = false
        val right = parseFactorWithPercentFlag()?.first ?: return null
        left = if (op == '*') {
          left * right
        } else {
          if (abs(right) < 1e-15) return null
          left / right
        }
      }
      return left to wasSinglePercent
    }

    private fun parseFactorWithPercentFlag(): Pair<Double, Boolean>? {
      val ch = peek() ?: return null
      var value: Double = when (ch) {
        '+' -> {
          pos++
          parseFactorWithPercentFlag()?.first ?: return null
        }
        '-' -> {
          pos++
          val inner = parseFactorWithPercentFlag()?.first ?: return null
          -inner
        }
        '(' -> {
          pos++
          val inner = parseExpression() ?: return null
          if (peek() != ')') return null
          pos++
          inner
        }
        else -> parseNumber() ?: return null
      }

      var isPercent = false
      while (peek() == '%') {
        pos++
        value /= 100.0
        isPercent = true
      }
      return value to isPercent
    }

    private fun parseNumber(): Double? {
      skipWhitespace()
      val start = pos
      var hasDot = false
      while (pos < input.length) {
        val c = input[pos]
        if (c.isDigit()) {
          pos++
        } else if (c == '.' && !hasDot) {
          hasDot = true
          pos++
        } else {
          break
        }
      }
      if (pos == start) return null
      val slice = input.substring(start, pos)
      if (slice == ".") return null
      return slice.toDoubleOrNull()
    }
  }

  fun evaluate(expression: String): Double? {
    val cleaned = expression.trim()
    if (cleaned.isEmpty() || cleaned.length > 160) return null
    return Parser(cleaned).parse()
  }
}
