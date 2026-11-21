package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.example.data.RichSpanData

object RichStyle {
  const val BOLD = "b"
  const val ITALIC = "i"
  const val UNDERLINE = "u"
  const val STRIKE = "s"
  const val HIGHLIGHT = "hl"
}

data class DetectedUrl(
  val start: Int,
  val end: Int,
  val displayText: String,
  val launchUrl: String
)

private val URL_REGEX = Regex(
  pattern = "(?i)\\b((?:https?://|www\\.)[^\\s<>\"'()]+|[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.(?:com|org|net|io|dev|app|co|edu|gov|ai|me|in|uk)(?:/[^\\s<>\"'()]*)?)",
)

fun detectUrls(text: String): List<DetectedUrl> {
  if (text.isBlank()) return emptyList()
  return URL_REGEX.findAll(text).mapNotNull { match ->
    var raw = match.value
    var end = match.range.last + 1
    while (raw.isNotEmpty() && raw.last() in listOf('.', ',', ';', ':', '!', '?', ')', ']')) {
      raw = raw.dropLast(1)
      end -= 1
    }
    if (raw.length < 4) return@mapNotNull null
    val launch = if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
      raw
    } else {
      "https://$raw"
    }
    DetectedUrl(
      start = match.range.first,
      end = end,
      displayText = raw,
      launchUrl = launch
    )
  }.toList()
}

/**
 * Pure WYSIWYG AnnotatedString builder — zero Markdown symbols anywhere.
 * Supports Bold, Italic, Underline, Strikethrough, Highlight, H1, H2,
 * Auto-Clickable Links, and Find-in-Note match highlighting.
 */
fun buildWysiwygAnnotatedString(
  text: String,
  spans: List<RichSpanData>,
  lineHeadings: Map<Int, Int>,
  highlightBg: Color,
  mutedColor: Color,
  accentColor: Color,
  findQuery: String = "",
  activeMatchIndex: Int = -1,
  clickableLinks: Boolean = false,
  onLinkClick: ((String) -> Unit)? = null
): AnnotatedString = buildAnnotatedString {
  append(text)
  val len = text.length

  // Apply line-level headings (H1 = 23sp Bold, H2 = 19sp SemiBold)
  if (lineHeadings.isNotEmpty() && len > 0) {
    var lineIdx = 0
    var lineStart = 0
    val lines = text.split("\n")
    for (line in lines) {
      val lineEnd = (lineStart + line.length).coerceAtMost(len)
      when (lineHeadings[lineIdx]) {
        1 -> if (lineEnd > lineStart) {
          addStyle(SpanStyle(fontSize = 23.sp, fontWeight = FontWeight.Bold), lineStart, lineEnd)
        }
        2 -> if (lineEnd > lineStart) {
          addStyle(SpanStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold), lineStart, lineEnd)
        }
      }
      lineStart = lineEnd + 1
      lineIdx++
    }
  }

  // Apply inline character spans (Bold, Italic, Underline, Strikethrough, Highlight)
  // Group underline + strikethrough so combined decorations render both cleanly
  for (sp in spans) {
    val s = sp.start.coerceIn(0, len)
    val e = sp.end.coerceIn(0, len)
    if (e <= s) continue
    when (sp.style) {
      RichStyle.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), s, e)
      RichStyle.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), s, e)
      RichStyle.UNDERLINE -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), s, e)
      RichStyle.STRIKE -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = mutedColor), s, e)
      RichStyle.HIGHLIGHT -> addStyle(SpanStyle(background = highlightBg), s, e)
    }
  }

  // Auto-detect URLs and style / attach clickable LinkAnnotation
  val urls = detectUrls(text)
  for (u in urls) {
    val s = u.start.coerceIn(0, len)
    val e = u.end.coerceIn(0, len)
    if (e <= s) continue
    val linkStyle = SpanStyle(
      color = accentColor,
      textDecoration = TextDecoration.Underline
    )
    if (clickableLinks && onLinkClick != null) {
      addLink(
        clickable = LinkAnnotation.Clickable(
          tag = u.launchUrl,
          styles = TextLinkStyles(style = linkStyle),
          linkInteractionListener = { onLinkClick(u.launchUrl) }
        ),
        start = s,
        end = e
      )
    } else {
      addStyle(linkStyle, s, e)
    }
  }

  // Live Find-in-Note match highlights
  if (findQuery.isNotBlank()) {
    val matches = findMatchRanges(text, findQuery)
    matches.forEachIndexed { idx, range ->
      val isCurrent = idx == activeMatchIndex
      addStyle(
        SpanStyle(
          background = if (isCurrent) accentColor.copy(alpha = 0.48f) else highlightBg.copy(alpha = 0.55f),
          fontWeight = if (isCurrent) FontWeight.Bold else null
        ),
        range.first.coerceIn(0, len),
        (range.last + 1).coerceIn(0, len)
      )
    }
  }
}

class WysiwygVisualTransformation(
  private val spans: List<RichSpanData>,
  private val lineHeadings: Map<Int, Int>,
  private val highlightBg: Color,
  private val mutedColor: Color,
  private val accentColor: Color,
  private val findQuery: String = "",
  private val activeMatchIndex: Int = -1
) : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    val styled = buildWysiwygAnnotatedString(
      text = text.text,
      spans = spans,
      lineHeadings = lineHeadings,
      highlightBg = highlightBg,
      mutedColor = mutedColor,
      accentColor = accentColor,
      findQuery = findQuery,
      activeMatchIndex = activeMatchIndex,
      clickableLinks = false
    )
    return TransformedText(styled, OffsetMapping.Identity)
  }
}

/**
 * Normalizes and merges overlapping/adjacent spans of the same style.
 */
fun normalizeSpans(spans: List<RichSpanData>, maxLength: Int): List<RichSpanData> {
  if (spans.isEmpty() || maxLength <= 0) return emptyList()
  val result = mutableListOf<RichSpanData>()
  spans
    .mapNotNull { sp ->
      val s = sp.start.coerceIn(0, maxLength)
      val e = sp.end.coerceIn(0, maxLength)
      if (e > s) sp.copy(start = s, end = e) else null
    }
    .groupBy { it.style }
    .forEach { (style, group) ->
      val sorted = group.sortedBy { it.start }
      var cur = sorted.first()
      for (i in 1 until sorted.size) {
        val next = sorted[i]
        if (next.start <= cur.end) {
          cur = cur.copy(end = maxOf(cur.end, next.end))
        } else {
          result.add(cur)
          cur = next
        }
      }
      result.add(cur)
    }
  return result
}

/**
 * Adjusts rich spans and line headings accurately when text is edited (typed, pasted, deleted).
 */
fun adjustRichFormatOnTextChange(
  oldText: String,
  newText: String,
  oldSpans: List<RichSpanData>,
  oldHeadings: Map<Int, Int>,
  activeStyles: Set<String>
): Pair<List<RichSpanData>, Map<Int, Int>> {
  if (oldText == newText) return oldSpans to oldHeadings

  var prefix = 0
  val minLen = minOf(oldText.length, newText.length)
  while (prefix < minLen && oldText[prefix] == newText[prefix]) {
    prefix++
  }

  var suffix = 0
  while (
    suffix < (minLen - prefix) &&
    oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
  ) {
    suffix++
  }

  val removedStart = prefix
  val removedEnd = oldText.length - suffix
  val insertedLen = newText.length - prefix - suffix
  val delta = insertedLen - (removedEnd - removedStart)

  val updatedSpans = mutableListOf<RichSpanData>()
  for (sp in oldSpans) {
    when {
      sp.end <= removedStart -> {
        if (sp.end == removedStart && insertedLen > 0 && sp.style in activeStyles) {
          updatedSpans.add(sp.copy(end = sp.end + insertedLen))
        } else {
          updatedSpans.add(sp)
        }
      }
      sp.start >= removedEnd -> {
        updatedSpans.add(sp.copy(start = sp.start + delta, end = sp.end + delta))
      }
      else -> {
        val newStart = if (sp.start < removedStart) sp.start else removedStart
        val newEnd = if (sp.end > removedEnd) sp.end + delta else removedStart + (if (sp.style in activeStyles) insertedLen else 0)
        if (newEnd > newStart) {
          updatedSpans.add(sp.copy(start = newStart, end = newEnd))
        }
      }
    }
  }

  // Apply active styles to newly inserted text
  if (insertedLen > 0 && activeStyles.isNotEmpty()) {
    val insEnd = removedStart + insertedLen
    for (style in activeStyles) {
      updatedSpans.add(RichSpanData(removedStart, insEnd, style))
    }
  }

  // Update line headings when newlines are inserted or removed
  val removedText = oldText.substring(removedStart, removedEnd)
  val insertedText = newText.substring(removedStart, removedStart + insertedLen)
  val removedNewlines = removedText.count { it == '\n' }
  val insertedNewlines = insertedText.count { it == '\n' }
  val newlineDelta = insertedNewlines - removedNewlines

  val updatedHeadings = if (newlineDelta == 0) {
    oldHeadings
  } else {
    val editLine = oldText.substring(0, removedStart).count { it == '\n' }
    val nextMap = mutableMapOf<Int, Int>()
    for ((lineIdx, lvl) in oldHeadings) {
      when {
        lineIdx <= editLine -> nextMap[lineIdx] = lvl
        lineIdx > editLine + removedNewlines -> {
          val shifted = lineIdx + newlineDelta
          if (shifted >= 0) nextMap[shifted] = lvl
        }
      }
    }
    nextMap
  }

  return normalizeSpans(updatedSpans, newText.length) to updatedHeadings
}

/**
 * Toggles an inline style (b, i, u, s, hl) over a selected range cleanly without adding any symbols.
 */
fun toggleStyleOverRange(
  spans: List<RichSpanData>,
  range: TextRange,
  style: String,
  textLength: Int
): List<RichSpanData> {
  val start = range.min.coerceIn(0, textLength)
  val end = range.max.coerceIn(0, textLength)
  if (end <= start) return spans

  val normalized = normalizeSpans(spans, textLength)
  val covered = isRangeFullyStyled(normalized, start, end, style)

  val next = mutableListOf<RichSpanData>()
  if (covered) {
    // Subtract [start, end) from spans of this style
    for (sp in normalized) {
      if (sp.style != style || sp.end <= start || sp.start >= end) {
        next.add(sp)
      } else {
        if (sp.start < start) next.add(sp.copy(end = start))
        if (sp.end > end) next.add(sp.copy(start = end))
      }
    }
  } else {
    next.addAll(normalized)
    next.add(RichSpanData(start, end, style))
  }
  return normalizeSpans(next, textLength)
}

fun isRangeFullyStyled(spans: List<RichSpanData>, start: Int, end: Int, style: String): Boolean {
  if (end <= start) return false
  val relevant = spans.filter { it.style == style && it.end > start && it.start < end }.sortedBy { it.start }
  var cursor = start
  for (sp in relevant) {
    if (sp.start > cursor) return false
    cursor = maxOf(cursor, sp.end)
    if (cursor >= end) return true
  }
  return cursor >= end
}

fun stylesActiveAtCursor(spans: List<RichSpanData>, selection: TextRange, textLength: Int): Set<String> {
  val styles = mutableSetOf<String>()
  if (selection.collapsed) {
    val probe = (selection.start - 1).coerceIn(0, (textLength - 1).coerceAtLeast(0))
    for (sp in spans) {
      if (textLength > 0 && probe >= sp.start && probe < sp.end) {
        styles.add(sp.style)
      }
    }
  } else {
    val s = selection.min.coerceIn(0, textLength)
    val e = selection.max.coerceIn(0, textLength)
    for (st in listOf(RichStyle.BOLD, RichStyle.ITALIC, RichStyle.UNDERLINE, RichStyle.STRIKE, RichStyle.HIGHLIGHT)) {
      if (isRangeFullyStyled(spans, s, e, st)) styles.add(st)
    }
  }
  return styles
}

fun lineIndicesForSelection(text: String, selection: TextRange): IntRange {
  val s = selection.min.coerceIn(0, text.length)
  val e = selection.max.coerceIn(0, text.length)
  val firstLine = text.substring(0, s).count { it == '\n' }
  val lastLine = text.substring(0, e).count { it == '\n' }
  return firstLine..lastLine
}

fun applyHeadingToSelection(
  text: String,
  selection: TextRange,
  currentHeadings: Map<Int, Int>,
  level: Int // 0 = normal, 1 = H1, 2 = H2
): Map<Int, Int> {
  val lines = lineIndicesForSelection(text, selection)
  val next = currentHeadings.toMutableMap()
  for (idx in lines) {
    if (level == 0) next.remove(idx) else next[idx] = level
  }
  return next
}

/**
 * One-time migration for any older notes saved with raw Markdown symbols (*, _, __, ~, ==, #, ##):
 * converts them into clean plain text + WYSIWYG spans/headings so zero Markdown symbols remain.
 */
fun migrateLegacyMarkdownIfNeeded(
  rawText: String,
  existingSpans: List<RichSpanData>,
  existingHeadings: Map<Int, Int>
): Triple<String, List<RichSpanData>, Map<Int, Int>> {
  if (existingSpans.isNotEmpty() || existingHeadings.isNotEmpty()) {
    return Triple(rawText, existingSpans, existingHeadings)
  }
  val hasLegacyTokens = Regex("(^##?\\s|\\*[^*\n]+\\*|__[^_\n]+__|(?<!_)_[^_\n]+_(?!_)|~[^~\n]+~|==[^=\n]+==)", RegexOption.MULTILINE)
    .containsMatchIn(rawText)
  if (!hasLegacyTokens) {
    return Triple(rawText, emptyList(), emptyMap())
  }

  val outText = StringBuilder()
  val outSpans = mutableListOf<RichSpanData>()
  val outHeadings = mutableMapOf<Int, Int>()

  val lines = rawText.split("\n")
  lines.forEachIndexed { lineIdx, origLine ->
    if (lineIdx > 0) outText.append('\n')
    var line = origLine
    when {
      line.startsWith("## ") -> {
        outHeadings[lineIdx] = 2
        line = line.removePrefix("## ")
      }
      line.startsWith("# ") -> {
        outHeadings[lineIdx] = 1
        line = line.removePrefix("# ")
      }
    }

    val tokenRegex = Regex("(==([^=\n]+)==|__([^_\n]+)__|\\*([^*\n]+)\\*|(?<!_)_([^_\n]+)_(?!_)|~([^~\n]+)~)")
    var cursor = 0
    for (m in tokenRegex.findAll(line)) {
      if (m.range.first > cursor) {
        outText.append(line.substring(cursor, m.range.first))
      }
      val spanStart = outText.length
      val (inner, style) = when {
        m.groupValues[2].isNotEmpty() -> m.groupValues[2] to RichStyle.HIGHLIGHT
        m.groupValues[3].isNotEmpty() -> m.groupValues[3] to RichStyle.UNDERLINE
        m.groupValues[4].isNotEmpty() -> m.groupValues[4] to RichStyle.BOLD
        m.groupValues[5].isNotEmpty() -> m.groupValues[5] to RichStyle.ITALIC
        else -> m.groupValues[6] to RichStyle.STRIKE
      }
      outText.append(inner)
      outSpans.add(RichSpanData(spanStart, outText.length, style))
      cursor = m.range.last + 1
    }
    if (cursor < line.length) {
      outText.append(line.substring(cursor))
    }
  }

  val clean = outText.toString()
  return Triple(clean, normalizeSpans(outSpans, clean.length), outHeadings)
}
