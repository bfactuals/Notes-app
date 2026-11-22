package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Live Rich Text + WhatsApp-style + Headings (H1/H2) + Underline + Find-in-Note VisualTransformation.
 * Preserves exact character offsets so cursor & selection stay 100% accurate.
 */
class VeilRichTextTransformation(
  private val highlightBg: Color,
  private val mutedColor: Color,
  private val accentColor: Color,
  private val findQuery: String = "",
  private val activeMatchIndex: Int = -1
) : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    val raw = text.text
    val styled = buildAnnotatedString {
      append(raw)

      // Line-based Headings: # H1 and ## H2
      var lineStart = 0
      raw.split("\n").forEach { line ->
        val lineEnd = lineStart + line.length
        when {
          line.startsWith("## ") -> {
            addStyle(SpanStyle(color = mutedColor.copy(alpha = 0.45f), fontSize = 14.sp), lineStart, lineStart + 3)
            addStyle(SpanStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold), lineStart + 3, lineEnd)
          }
          line.startsWith("# ") -> {
            addStyle(SpanStyle(color = mutedColor.copy(alpha = 0.45f), fontSize = 14.sp), lineStart, lineStart + 2)
            addStyle(SpanStyle(fontSize = 23.sp, fontWeight = FontWeight.Bold), lineStart + 2, lineEnd)
          }
        }
        lineStart = lineEnd + 1
      }

      // __underline__ (evaluated before single _italic_)
      Regex("__([^_\n]+)__").findAll(raw).forEach { match ->
        addStyle(SpanStyle(color = mutedColor.copy(alpha = 0.45f)), match.range.first, match.range.first + 2)
        addStyle(SpanStyle(textDecoration = TextDecoration.Underline), match.range.first + 2, match.range.last - 1)
        addStyle(SpanStyle(color = mutedColor.copy(alpha = 0.45f)), match.range.last - 1, match.range.last + 1)
      }

      // *bold*
      Regex("\\*([^*\n]+)\\*").findAll(raw).forEach { match ->
        addStyle(SpanStyle(color = mutedColor.copy(alpha = 0.45f)), match.range.first, match.range.first + 1)
        addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first + 1, match.range.last)
        addStyle(SpanStyle(color = mutedColor.copy(alpha = 0.45f)), match.range.last, match.range.last + 1)
      }

      // _italic_ (avoid matching __underline__)
      Regex("(?<!_)_([^_\n]+)_(?!_)").findAll(raw).forEach { match ->
        addStyle(SpanStyle(color = mutedColor.copy(alpha = 0.45f)), match.range.first, match.range.first + 1)
        addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first + 1, match.range.last)
        addStyle(SpanStyle(color = mutedColor.copy(alpha = 0.45f)), match.range.last, match.range.last + 1)
      }

      // ~strikethrough~
      Regex("~([^~\n]+)~").findAll(raw).forEach { match ->
        addStyle(SpanStyle(color = mutedColor.copy(alpha = 0.45f)), match.range.first, match.range.first + 1)
        addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = mutedColor), match.range.first + 1, match.range.last)
        addStyle(SpanStyle(color = mutedColor.copy(alpha = 0.45f)), match.range.last, match.range.last + 1)
      }

      // `monospace`
      Regex("`([^`\n]+)`").findAll(raw).forEach { match ->
        addStyle(SpanStyle(color = mutedColor.copy(alpha = 0.45f)), match.range.first, match.range.first + 1)
        addStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = accentColor), match.range.first + 1, match.range.last)
        addStyle(SpanStyle(color = mutedColor.copy(alpha = 0.45f)), match.range.last, match.range.last + 1)
      }

      // ==highlighted==
      Regex("==([^=\n]+)==").findAll(raw).forEach { match ->
        addStyle(SpanStyle(color = mutedColor.copy(alpha = 0.45f)), match.range.first, match.range.first + 2)
        addStyle(SpanStyle(background = highlightBg), match.range.first + 2, match.range.last - 1)
        addStyle(SpanStyle(color = mutedColor.copy(alpha = 0.45f)), match.range.last - 1, match.range.last + 1)
      }

      // Live Find-in-Note match highlights
      if (findQuery.isNotBlank()) {
        val matches = findMatchRanges(raw, findQuery)
        matches.forEachIndexed { idx, range ->
          val isCurrent = idx == activeMatchIndex
          addStyle(
            SpanStyle(
              background = if (isCurrent) accentColor.copy(alpha = 0.48f) else highlightBg.copy(alpha = 0.55f),
              fontWeight = if (isCurrent) FontWeight.Bold else null
            ),
            range.first,
            range.last + 1
          )
        }
      }
    }
    return TransformedText(styled, OffsetMapping.Identity)
  }
}

fun findMatchRanges(text: String, query: String): List<IntRange> {
  if (query.isBlank() || text.isEmpty()) return emptyList()
  val results = mutableListOf<IntRange>()
  var startIndex = 0
  while (startIndex < text.length) {
    val found = text.indexOf(query, startIndex = startIndex, ignoreCase = true)
    if (found < 0) break
    results.add(found until (found + query.length))
    startIndex = found + query.length.coerceAtLeast(1)
  }
  return results
}

/**
 * Wraps the current selection with inline rich text markers (e.g. "*", "_", "__", "~"),
 * or inserts paired markers at cursor and moves cursor between them.
 */
fun applyInlineFormatting(value: TextFieldValue, marker: String): TextFieldValue {
  val text = value.text
  val start = value.selection.min.coerceIn(0, text.length)
  val end = value.selection.max.coerceIn(0, text.length)

  if (start == end) {
    val updated = text.substring(0, start) + marker + marker + text.substring(end)
    val cursor = start + marker.length
    return value.copy(text = updated, selection = TextRange(cursor))
  }

  val selected = text.substring(start, end)
  return if (selected.startsWith(marker) && selected.endsWith(marker) && selected.length >= marker.length * 2) {
    val unwrapped = selected.removePrefix(marker).removeSuffix(marker)
    val updated = text.substring(0, start) + unwrapped + text.substring(end)
    value.copy(text = updated, selection = TextRange(start, start + unwrapped.length))
  } else {
    val wrapped = "$marker$selected$marker"
    val updated = text.substring(0, start) + wrapped + text.substring(end)
    value.copy(text = updated, selection = TextRange(start, start + wrapped.length))
  }
}

/**
 * Applies or clears heading prefixes ("# ", "## ", or "" for Normal text) on the current line.
 */
fun applyHeadingFormat(value: TextFieldValue, prefix: String): TextFieldValue {
  val text = value.text
  val cursor = value.selection.min.coerceIn(0, text.length)
  val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
  val lineEnd = text.indexOf('\n', cursor).let { if (it < 0) text.length else it }
  val line = text.substring(lineStart, lineEnd)

  val stripped = when {
    line.startsWith("## ") -> line.removePrefix("## ")
    line.startsWith("# ") -> line.removePrefix("# ")
    else -> line
  }
  val newLine = if (prefix.isEmpty()) stripped else "$prefix$stripped"
  val updated = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
  val delta = newLine.length - line.length
  val newCursor = (cursor + delta).coerceIn(lineStart, lineStart + newLine.length)
  return value.copy(text = updated, selection = TextRange(newCursor))
}

fun formatReminderChip(reminderMillis: Long): String {
  if (reminderMillis <= 0L) return ""
  val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
  return fmt.format(Date(reminderMillis))
}

fun isReminderPast(reminderMillis: Long, nowMs: Long = System.currentTimeMillis()): Boolean {
  return reminderMillis in 1L..nowMs
}

fun extractDoodleCount(content: String): Int {
  val match = Regex("\\[Doodle with (\\d+) strokes?\\]").find(content)
  return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: if (content.contains("[Doodle")) 3 else 0
}

fun stripDoodleMarkers(content: String): String =
  content.replace(Regex("\\[Doodle[^\\]]*\\]"), "").trim()
