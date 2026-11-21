package com.example.data

import org.json.JSONArray
import org.json.JSONObject

data class ChecklistItemData(
  val id: Long,
  val text: String,
  val checked: Boolean
)

/**
 * Character-range inline style for true WYSIWYG rich text (zero visible Markdown symbols).
 * style: "b" = bold, "i" = italic, "u" = underline, "s" = strikethrough, "hl" = highlight
 */
data class RichSpanData(
  val start: Int,
  val end: Int,
  val style: String
)

/**
 * Copyable Prompt / Code Block stored inside a note.
 */
data class CodeBlockData(
  val id: Long,
  val text: String
)

/**
 * Lightweight local version history snapshot (keep up to 5 latest versions per note).
 */
data class NoteVersionSnapshot(
  val timestamp: Long,
  val title: String,
  val content: String,
  val richSpans: List<RichSpanData> = emptyList(),
  val lineHeadings: Map<Int, Int> = emptyMap()
)

/**
 * Normalized stroke (x, y in 0f..1f relative to canvas dimensions) so drawings
 * scale smoothly between full-page canvas, resizable inline boxes, and card thumbnails.
 * brushType: 0 = pen, 1 = fountain pen, 2 = highlighter, 3 = eraser
 */
data class DoodleStrokeData(
  val points: List<Pair<Float, Float>>,
  val colorIndex: Int, // 0..9 palette, -1 = eraser
  val widthDp: Float,
  val alpha: Float,
  val brushType: Int = 0
)

data class DoodleBlockData(
  val strokes: List<DoodleStrokeData> = emptyList(),
  val heightDp: Float = 160f,
  val widthFraction: Float = 1f,
  val isFullPage: Boolean = false
)

data class NoteExtrasData(
  val isChecklistMode: Boolean = false,
  val checklist: List<ChecklistItemData> = emptyList(),
  val doodle: DoodleBlockData? = null,
  val images: List<String> = emptyList(),
  val backgroundPattern: String = "none",
  val richSpans: List<RichSpanData> = emptyList(),
  val lineHeadings: Map<Int, Int> = emptyMap(), // lineIndex -> 1 (H1) or 2 (H2)
  val expiresAt: Long = 0L, // Self-destruct timestamp (epoch ms), 0L = disabled
  val deletedAt: Long = 0L, // Moved-to-Trash timestamp (epoch ms) for 30-day auto-empty
  val codeBlocks: List<CodeBlockData> = emptyList(),
  val versionHistory: List<NoteVersionSnapshot> = emptyList()
)

object NoteExtrasCodec {
  fun encode(extras: NoteExtrasData): String {
    if (!extras.isChecklistMode &&
      extras.checklist.isEmpty() &&
      extras.doodle == null &&
      extras.images.isEmpty() &&
      (extras.backgroundPattern.isBlank() || extras.backgroundPattern == "none") &&
      extras.richSpans.isEmpty() &&
      extras.lineHeadings.isEmpty() &&
      extras.expiresAt <= 0L &&
      extras.deletedAt <= 0L &&
      extras.codeBlocks.isEmpty() &&
      extras.versionHistory.isEmpty()
    ) {
      return ""
    }
    val root = JSONObject()
    root.put("isChecklist", extras.isChecklistMode)

    if (extras.backgroundPattern.isNotBlank() && extras.backgroundPattern != "none") {
      root.put("bgPattern", extras.backgroundPattern)
    }

    if (extras.expiresAt > 0L) {
      root.put("expiresAt", extras.expiresAt)
    }

    if (extras.deletedAt > 0L) {
      root.put("deletedAt", extras.deletedAt)
    }

    if (extras.images.isNotEmpty()) {
      val imgArr = JSONArray()
      extras.images.forEach { path -> imgArr.put(path) }
      root.put("images", imgArr)
    }

    if (extras.richSpans.isNotEmpty()) {
      val spArr = encodeSpansArray(extras.richSpans)
      if (spArr.length() > 0) root.put("spans", spArr)
    }

    if (extras.lineHeadings.isNotEmpty()) {
      val hObj = encodeHeadingsObject(extras.lineHeadings)
      if (hObj.length() > 0) root.put("headings", hObj)
    }

    if (extras.codeBlocks.isNotEmpty()) {
      val cbArr = JSONArray()
      extras.codeBlocks.forEach { cb ->
        cbArr.put(JSONObject().apply {
          put("id", cb.id)
          put("t", cb.text)
        })
      }
      root.put("codeBlocks", cbArr)
    }

    if (extras.versionHistory.isNotEmpty()) {
      val vhArr = JSONArray()
      extras.versionHistory.take(5).forEach { v ->
        vhArr.put(JSONObject().apply {
          put("ts", v.timestamp)
          put("ti", v.title)
          put("co", v.content)
          if (v.richSpans.isNotEmpty()) {
            put("sp", encodeSpansArray(v.richSpans))
          }
          if (v.lineHeadings.isNotEmpty()) {
            put("hd", encodeHeadingsObject(v.lineHeadings))
          }
        })
      }
      root.put("versions", vhArr)
    }

    if (extras.checklist.isNotEmpty()) {
      val cArr = JSONArray()
      extras.checklist.forEach { item ->
        cArr.put(JSONObject().apply {
          put("id", item.id)
          put("t", item.text)
          put("c", item.checked)
        })
      }
      root.put("checklist", cArr)
    }

    extras.doodle?.let { d ->
      val dObj = JSONObject().apply {
        put("h", d.heightDp.toDouble())
        put("w", d.widthFraction.toDouble())
        put("fp", d.isFullPage)
        val sArr = JSONArray()
        d.strokes.forEach { s ->
          val sObj = JSONObject().apply {
            put("ci", s.colorIndex)
            put("wd", s.widthDp.toDouble())
            put("a", s.alpha.toDouble())
            if (s.brushType != 0) put("bt", s.brushType)
            val pArr = JSONArray()
            s.points.forEach { (x, y) ->
              pArr.put(Math.round(x * 10000.0) / 10000.0)
              pArr.put(Math.round(y * 10000.0) / 10000.0)
            }
            put("p", pArr)
          }
          sArr.put(sObj)
        }
        put("s", sArr)
      }
      root.put("doodle", dObj)
    }

    return root.toString()
  }

  fun decode(raw: String): NoteExtrasData {
    if (raw.isBlank()) return NoteExtrasData()
    return try {
      val root = JSONObject(raw)
      val isChecklist = root.optBoolean("isChecklist", false)
      val bgPattern = root.optString("bgPattern", "none").ifBlank { "none" }
      val expiresAt = root.optLong("expiresAt", 0L)
      val deletedAt = root.optLong("deletedAt", 0L)

      val images = mutableListOf<String>()
      val imgArr = root.optJSONArray("images")
      if (imgArr != null) {
        for (i in 0 until imgArr.length()) {
          val p = imgArr.optString(i, "").trim()
          if (p.isNotEmpty()) images.add(p)
        }
      }

      val richSpans = decodeSpansArray(root.optJSONArray("spans"))
      val lineHeadings = decodeHeadingsObject(root.optJSONObject("headings"))

      val codeBlocks = mutableListOf<CodeBlockData>()
      val cbArr = root.optJSONArray("codeBlocks")
      if (cbArr != null) {
        for (i in 0 until cbArr.length()) {
          val o = cbArr.getJSONObject(i)
          codeBlocks.add(
            CodeBlockData(
              id = o.optLong("id", i.toLong() + 1L),
              text = o.optString("t", "")
            )
          )
        }
      }

      val versionHistory = mutableListOf<NoteVersionSnapshot>()
      val vhArr = root.optJSONArray("versions")
      if (vhArr != null) {
        for (i in 0 until vhArr.length()) {
          val o = vhArr.getJSONObject(i)
          versionHistory.add(
            NoteVersionSnapshot(
              timestamp = o.optLong("ts", 0L),
              title = o.optString("ti", ""),
              content = o.optString("co", ""),
              richSpans = decodeSpansArray(o.optJSONArray("sp")),
              lineHeadings = decodeHeadingsObject(o.optJSONObject("hd"))
            )
          )
        }
      }

      val checklist = mutableListOf<ChecklistItemData>()
      val cArr = root.optJSONArray("checklist")
      if (cArr != null) {
        for (i in 0 until cArr.length()) {
          val o = cArr.getJSONObject(i)
          checklist.add(
            ChecklistItemData(
              id = o.optLong("id", i.toLong() + 1L),
              text = o.optString("t", ""),
              checked = o.optBoolean("c", false)
            )
          )
        }
      }

      val dObj = root.optJSONObject("doodle")
      val doodle = if (dObj != null) {
        val strokes = mutableListOf<DoodleStrokeData>()
        val sArr = dObj.optJSONArray("s")
        if (sArr != null) {
          for (i in 0 until sArr.length()) {
            val sObj = sArr.getJSONObject(i)
            val pArr = sObj.optJSONArray("p")
            val pts = mutableListOf<Pair<Float, Float>>()
            if (pArr != null) {
              var idx = 0
              while (idx + 1 < pArr.length()) {
                pts.add(pArr.getDouble(idx).toFloat() to pArr.getDouble(idx + 1).toFloat())
                idx += 2
              }
            }
            strokes.add(
              DoodleStrokeData(
                points = pts,
                colorIndex = sObj.optInt("ci", 0),
                widthDp = sObj.optDouble("wd", 3.0).toFloat(),
                alpha = sObj.optDouble("a", 1.0).toFloat(),
                brushType = sObj.optInt("bt", 0)
              )
            )
          }
        }
        DoodleBlockData(
          strokes = strokes,
          heightDp = dObj.optDouble("h", 160.0).toFloat().coerceIn(100f, 480f),
          widthFraction = dObj.optDouble("w", 1.0).toFloat().coerceIn(0.45f, 1f),
          isFullPage = dObj.optBoolean("fp", false)
        )
      } else {
        null
      }

      NoteExtrasData(
        isChecklistMode = isChecklist,
        checklist = checklist,
        doodle = doodle,
        images = images,
        backgroundPattern = bgPattern,
        richSpans = richSpans,
        lineHeadings = lineHeadings,
        expiresAt = expiresAt,
        deletedAt = deletedAt,
        codeBlocks = codeBlocks,
        versionHistory = versionHistory
      )
    } catch (_: Exception) {
      NoteExtrasData()
    }
  }

  private fun encodeSpansArray(spans: List<RichSpanData>): JSONArray {
    val spArr = JSONArray()
    spans.forEach { sp ->
      if (sp.end > sp.start) {
        spArr.put(JSONObject().apply {
          put("s", sp.start)
          put("e", sp.end)
          put("t", sp.style)
        })
      }
    }
    return spArr
  }

  private fun decodeSpansArray(spArr: JSONArray?): List<RichSpanData> {
    val list = mutableListOf<RichSpanData>()
    if (spArr != null) {
      for (i in 0 until spArr.length()) {
        val o = spArr.getJSONObject(i)
        val s = o.optInt("s", 0)
        val e = o.optInt("e", 0)
        val t = o.optString("t", "")
        if (e > s && t.isNotEmpty()) {
          list.add(RichSpanData(s, e, t))
        }
      }
    }
    return list
  }

  private fun encodeHeadingsObject(headings: Map<Int, Int>): JSONObject {
    val hObj = JSONObject()
    headings.forEach { (lineIdx, level) ->
      if (level == 1 || level == 2) {
        hObj.put(lineIdx.toString(), level)
      }
    }
    return hObj
  }

  private fun decodeHeadingsObject(hObj: JSONObject?): Map<Int, Int> {
    val map = mutableMapOf<Int, Int>()
    if (hObj != null) {
      val keys = hObj.keys()
      while (keys.hasNext()) {
        val k = keys.next()
        val lineIdx = k.toIntOrNull()
        val lvl = hObj.optInt(k, 0)
        if (lineIdx != null && (lvl == 1 || lvl == 2)) {
          map[lineIdx] = lvl
        }
      }
    }
    return map
  }
}
