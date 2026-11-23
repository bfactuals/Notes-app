package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens

/**
 * Card-free edge-to-edge Material You docked Note Editor Bottom Bar:
 * Each action icon sits inside its own individual soft circular tonal button (44.dp)
 * paired dynamically to the active note's color in Light and Dark modes.
 * Left: "+", [optional Timer], Color Palette, "Aa"
 * Right: "⋮" (Three-Dot Menu)
 */
@Composable
fun NoteEditorBottomBar(
  barBgColor: Color,
  tonalColor: Color,
  isInsertOpen: Boolean,
  isPaletteOpen: Boolean,
  isFormatBarOpen: Boolean,
  isOverflowOpen: Boolean,
  isReadOnlyMode: Boolean,
  showTimerButton: Boolean = false,
  isTimerOpen: Boolean = false,
  hasActiveTimer: Boolean = false,
  onToggleInsert: () -> Unit,
  onToggleTimer: () -> Unit = {},
  onTogglePalette: () -> Unit,
  onToggleFormatBar: () -> Unit,
  onToggleOverflow: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(barBgColor)
      .pointerInput(Unit) { detectTapGestures { } }
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    // Left side: 1) "+", [optional Timer], 2) Color Palette, 3) "Aa"
    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      EditorBottomTonalButton(
        active = isInsertOpen,
        enabled = !isReadOnlyMode,
        tonalColor = tonalColor,
        description = "Insert attachment or tool",
        tag = "editor_insert_plus_button",
        onClick = onToggleInsert
      ) { tint ->
        Box(
          modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.7.dp, tint, RoundedCornerShape(6.dp)),
          contentAlignment = Alignment.Center
        ) {
          VeilIcons.Plus(tint = tint, size = 14.dp)
        }
      }

      if (showTimerButton) {
        EditorBottomTonalButton(
          active = isTimerOpen || hasActiveTimer,
          enabled = true,
          tonalColor = tonalColor,
          description = "Self-destruct timer",
          tag = "editor_self_destruct_button",
          onClick = onToggleTimer
        ) { tint ->
          VeilIcons.HourglassOutline(tint = tint, size = 20.dp)
        }
      }

      EditorBottomTonalButton(
        active = isPaletteOpen,
        enabled = true,
        tonalColor = tonalColor,
        description = "Note colors and backgrounds",
        tag = "editor_color_button",
        onClick = onTogglePalette
      ) { tint ->
        VeilIcons.PaletteOutline(tint = tint, size = 20.dp)
      }

      EditorBottomTonalButton(
        active = isFormatBarOpen,
        enabled = !isReadOnlyMode,
        tonalColor = tonalColor,
        description = "Rich text formatting",
        tag = "editor_format_aa_button",
        onClick = onToggleFormatBar
      ) { tint ->
        Text(
          text = "Aa",
          style = VeilTextStyles.cardTitle.copy(
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp
          ),
          color = tint
        )
      }
    }

    // Right side: 4) "⋮" Three-Dot Menu
    EditorBottomTonalButton(
      active = isOverflowOpen,
      enabled = true,
      tonalColor = tonalColor,
      description = "More note options",
      tag = "editor_overflow_menu_button",
      onClick = onToggleOverflow
    ) { tint ->
      VeilIcons.MoreVert(tint = tint, size = 20.dp)
    }
  }
}

@Composable
private fun EditorBottomTonalButton(
  active: Boolean,
  enabled: Boolean = true,
  tonalColor: Color,
  description: String,
  tag: String,
  onClick: () -> Unit,
  content: @Composable (Color) -> Unit
) {
  val colors = VeilThemeTokens.colors
  val containerBg = when {
    !enabled -> tonalColor.copy(alpha = 0.45f)
    active -> colors.accent
    else -> tonalColor
  }
  val tint = when {
    !enabled -> colors.muted.copy(alpha = 0.4f)
    active -> Color.White
    else -> colors.text
  }
  Box(
    modifier = Modifier
      .size(44.dp)
      .clip(CircleShape)
      .background(containerBg)
      .tactilePress(scaleDown = 0.88f, enabled = enabled, hapticOnClick = true, onClick = onClick)
      .semantics { contentDescription = description }
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    content(tint)
  }
}

/**
 * Seamless docked WYSIWYG Rich Text Formatting Bar toggled by "Aa" right above the bottom bar / keyboard.
 * Active buttons highlight with accent fill so users always see current heading & inline styles.
 */
@Composable
fun RichTextFormattingBar(
  sheetBg: Color,
  tonalColor: Color,
  activeHeadingLevel: Int,
  activeInlineStyles: Set<String>,
  onHeading1: () -> Unit,
  onHeading2: () -> Unit,
  onNormalText: () -> Unit,
  onBold: () -> Unit,
  onItalic: () -> Unit,
  onUnderline: () -> Unit,
  onStrikethrough: () -> Unit,
  onHighlight: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

  LazyRow(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(sheetBg)
      .pointerInput(Unit) { detectTapGestures { } }
      .padding(horizontal = 14.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    item {
      FormatChip(
        label = "H1",
        active = activeHeadingLevel == 1,
        tonalColor = tonalColor,
        weight = FontWeight.Bold,
        onClick = onHeading1,
        tag = "fmt_h1"
      )
    }
    item {
      FormatChip(
        label = "H2",
        active = activeHeadingLevel == 2,
        tonalColor = tonalColor,
        weight = FontWeight.SemiBold,
        onClick = onHeading2,
        tag = "fmt_h2"
      )
    }
    item {
      FormatChip(
        label = "Normal",
        active = activeHeadingLevel == 0,
        tonalColor = tonalColor,
        weight = FontWeight.Medium,
        onClick = onNormalText,
        tag = "fmt_normal"
      )
    }
    item {
      Box(
        modifier = Modifier
          .width(1.dp)
          .height(22.dp)
          .background(colors.muted.copy(alpha = 0.25f))
      )
    }
    item {
      FormatChip(
        label = "B",
        active = RichStyle.BOLD in activeInlineStyles,
        tonalColor = tonalColor,
        weight = FontWeight.Bold,
        onClick = onBold,
        tag = "fmt_bold"
      )
    }
    item {
      FormatChip(
        label = "I",
        active = RichStyle.ITALIC in activeInlineStyles,
        tonalColor = tonalColor,
        style = FontStyle.Italic,
        weight = FontWeight.SemiBold,
        onClick = onItalic,
        tag = "fmt_italic"
      )
    }
    item {
      FormatChip(
        label = "U",
        active = RichStyle.UNDERLINE in activeInlineStyles,
        tonalColor = tonalColor,
        decoration = TextDecoration.Underline,
        weight = FontWeight.SemiBold,
        onClick = onUnderline,
        tag = "fmt_underline"
      )
    }
    item {
      FormatChip(
        label = "S",
        active = RichStyle.STRIKE in activeInlineStyles,
        tonalColor = tonalColor,
        decoration = TextDecoration.LineThrough,
        weight = FontWeight.SemiBold,
        onClick = onStrikethrough,
        tag = "fmt_strike"
      )
    }
    item {
      FormatChip(
        label = "HL",
        active = RichStyle.HIGHLIGHT in activeInlineStyles,
        tonalColor = tonalColor,
        weight = FontWeight.SemiBold,
        highlightBadge = true,
        onClick = onHighlight,
        tag = "fmt_highlight"
      )
    }
  }
}

@Composable
private fun FormatChip(
  label: String,
  active: Boolean,
  tonalColor: Color,
  weight: FontWeight = FontWeight.Medium,
  style: FontStyle = FontStyle.Normal,
  decoration: TextDecoration = TextDecoration.None,
  highlightBadge: Boolean = false,
  onClick: () -> Unit,
  tag: String
) {
  val colors = VeilThemeTokens.colors
  val bg = when {
    active && highlightBadge -> colors.highlight.copy(alpha = 0.85f)
    active -> colors.accent
    else -> tonalColor
  }
  val fg = when {
    active && highlightBadge -> colors.text
    active -> Color.White
    else -> colors.text
  }

  Box(
    modifier = Modifier
      .height(36.dp)
      .defaultMinSize(minWidth = 38.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(bg)
      .tactilePress(scaleDown = 0.90f, hapticOnClick = true, onClick = onClick)
      .padding(horizontal = 12.dp)
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      style = VeilTextStyles.cardTitle.copy(
        fontSize = 14.sp,
        fontWeight = weight,
        fontStyle = style,
        textDecoration = decoration
      ),
      color = fg
    )
  }
}

/**
 * Seamless docked sheet for the "+" Insert button matching the note's opaque tonal surface.
 */
@Composable
fun InsertToolsPanel(
  sheetBg: Color,
  showCodeBlockOption: Boolean = false,
  onTakePhoto: () -> Unit,
  onAddImage: () -> Unit,
  onFullPageDrawing: () -> Unit,
  onInlineDrawBox: () -> Unit,
  onChecklist: () -> Unit,
  onVoiceNote: () -> Unit,
  onInsertCodeBlock: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(sheetBg)
      .pointerInput(Unit) { detectTapGestures { } }
      .padding(top = 10.dp, bottom = 6.dp, start = 10.dp, end = 10.dp)
      .testTag("insert_tools_panel")
  ) {
    PanelActionRow(
      label = "Take photo",
      icon = { VeilIcons.CameraOutline(tint = colors.text, size = 21.dp) },
      onClick = onTakePhoto,
      tag = "insert_take_photo"
    )
    PanelActionRow(
      label = "Add image",
      icon = { VeilIcons.ImageOutline(tint = colors.text, size = 21.dp) },
      onClick = onAddImage,
      tag = "insert_add_image"
    )
    PanelActionRow(
      label = "Full-page drawing",
      icon = { VeilIcons.Pen(tint = colors.text, size = 21.dp) },
      onClick = onFullPageDrawing,
      tag = "insert_full_drawing"
    )
    PanelActionRow(
      label = "Inline draw box",
      icon = { VeilIcons.DrawBoxOutline(tint = colors.text, size = 21.dp) },
      onClick = onInlineDrawBox,
      tag = "insert_inline_draw_box"
    )
    PanelActionRow(
      label = "Checklist",
      icon = { VeilIcons.Checklist(tint = colors.text, size = 21.dp) },
      onClick = onChecklist,
      tag = "insert_checklist"
    )
    PanelActionRow(
      label = "Voice note",
      icon = { VeilIcons.Mic(tint = colors.text, size = 21.dp) },
      onClick = onVoiceNote,
      tag = "insert_voice_note"
    )
    if (showCodeBlockOption) {
      PanelActionRow(
        label = "Prompt / Code block",
        icon = { VeilIcons.CodeOutline(tint = colors.accent, size = 21.dp) },
        onClick = onInsertCodeBlock,
        tag = "insert_code_block"
      )
    }
  }
}

/**
 * Seamless docked sheet for the "⋮" Three-Dot Menu in Note Editor matching the note's opaque tonal surface.
 * Zero Hide/Lock icons anywhere.
 */
@Composable
fun NoteOverflowMenuPanel(
  sheetBg: Color,
  tonalColor: Color = VeilThemeTokens.colors.bg,
  isReadOnlyMode: Boolean,
  hasReminder: Boolean,
  hasLabels: Boolean,
  enableSharing: Boolean,
  showVersionHistory: Boolean = false,
  versionCount: Int = 0,
  onToggleReadMode: () -> Unit,
  onVersionHistory: () -> Unit = {},
  onReminder: () -> Unit,
  onLabels: () -> Unit,
  onFindInNote: () -> Unit,
  onMakeCopy: () -> Unit,
  onCopyAllText: () -> Unit,
  onSendShare: () -> Unit,
  onArchive: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(sheetBg)
      .pointerInput(Unit) { detectTapGestures { } }
      .padding(top = 12.dp, bottom = 6.dp, start = 12.dp, end = 12.dp)
      .testTag("editor_overflow_panel")
  ) {
    // Compact horizontal quick-action icon row at the top (Copy, Share, Archive, Delete)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OverflowQuickActionTile(
        label = "Copy",
        tonalColor = tonalColor,
        tint = colors.text,
        icon = { VeilIcons.CopyOutline(tint = colors.text, size = 19.dp) },
        onClick = onCopyAllText,
        tag = "overflow_copy_text",
        modifier = Modifier.weight(1f)
      )
      if (enableSharing) {
        OverflowQuickActionTile(
          label = "Share",
          tonalColor = tonalColor,
          tint = colors.text,
          icon = { VeilIcons.ShareOutline(tint = colors.text, size = 19.dp) },
          onClick = onSendShare,
          tag = "overflow_share",
          modifier = Modifier.weight(1f)
        )
      }
      OverflowQuickActionTile(
        label = "Archive",
        tonalColor = tonalColor,
        tint = colors.text,
        icon = { VeilIcons.Archive(tint = colors.text, size = 19.dp) },
        onClick = onArchive,
        tag = "overflow_archive",
        modifier = Modifier.weight(1f)
      )
      OverflowQuickActionTile(
        label = "Delete",
        tonalColor = tonalColor,
        tint = colors.doodlePink,
        icon = { VeilIcons.Trash(tint = colors.doodlePink, size = 19.dp) },
        onClick = onDelete,
        tag = "overflow_delete",
        modifier = Modifier.weight(1f)
      )
    }

    HorizontalDivider(
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
      color = colors.muted.copy(alpha = 0.2f),
      thickness = 1.dp
    )

    PanelActionRow(
      label = if (isReadOnlyMode) "Read mode (On)" else "Read mode",
      labelColor = if (isReadOnlyMode) colors.accent else null,
      icon = { VeilIcons.ReadBookOutline(tint = if (isReadOnlyMode) colors.accent else colors.text, size = 20.dp) },
      trailing = {
        if (isReadOnlyMode) {
          VeilIcons.CheckOutline(tint = colors.accent, size = 18.dp)
        }
      },
      onClick = onToggleReadMode,
      tag = "overflow_read_mode"
    )
    if (showVersionHistory) {
      PanelActionRow(
        label = "Version history",
        icon = { VeilIcons.HistoryOutline(tint = colors.text, size = 20.dp) },
        trailing = {
          if (versionCount > 0) {
            Text(
              text = versionCount.toString(),
              style = VeilTextStyles.labelChip,
              color = colors.accent
            )
          }
        },
        onClick = onVersionHistory,
        tag = "overflow_version_history"
      )
    }
    PanelActionRow(
      label = "Reminder",
      icon = { VeilIcons.Bell(tint = if (hasReminder) colors.accent else colors.text, size = 20.dp) },
      onClick = onReminder,
      tag = "overflow_reminder"
    )
    PanelActionRow(
      label = "Labels",
      icon = { VeilIcons.Tag(tint = if (hasLabels) colors.accent else colors.text, size = 20.dp) },
      onClick = onLabels,
      tag = "overflow_labels"
    )
    PanelActionRow(
      label = "Find in note",
      icon = { VeilIcons.Search(tint = colors.text, size = 19.dp) },
      onClick = onFindInNote,
      tag = "overflow_find"
    )
    PanelActionRow(
      label = "Make a copy",
      icon = { VeilIcons.NoteDoc(tint = colors.text, size = 20.dp) },
      onClick = onMakeCopy,
      tag = "overflow_copy_note"
    )
  }
}

@Composable
private fun OverflowQuickActionTile(
  label: String,
  tonalColor: Color,
  tint: Color,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
  tag: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(tonalColor)
      .tactilePress(scaleDown = 0.93f, hapticOnClick = true, role = Role.Button, onClick = onClick)
      .padding(vertical = 10.dp, horizontal = 6.dp)
      .testTag(tag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(5.dp)
  ) {
    icon()
    Text(
      text = label,
      style = VeilTextStyles.labelChip.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
      color = tint,
      maxLines = 1
    )
  }
}

@Composable
private fun PanelActionRow(
  label: String,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
  tag: String,
  labelColor: Color? = null,
  trailing: (@Composable () -> Unit)? = null
) {
  val colors = VeilThemeTokens.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .tactilePress(scaleDown = 0.97f, hapticOnClick = true, role = Role.Button, onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 11.dp)
      .testTag(tag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    icon()
    Text(
      text = label,
      style = VeilTextStyles.cardBody.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
      color = labelColor ?: colors.text,
      modifier = Modifier.weight(1f)
    )
    trailing?.invoke()
  }
}

/**
 * Sleek inline Find-in-Note bar shown below the top bar when triggered from the Three-Dot Menu.
 */
@Composable
fun FindInNoteBar(
  query: String,
  matchCount: Int,
  activeIndex: Int,
  onQueryChange: (String) -> Unit,
  onPrev: () -> Unit,
  onNext: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  val shape = RoundedCornerShape(20.dp)
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .shadow(4.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
      .clip(shape)
      .background(colors.surface)
      .padding(horizontal = 14.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    VeilIcons.Search(tint = colors.muted, size = 18.dp)
    BasicTextField(
      value = query,
      onValueChange = onQueryChange,
      singleLine = true,
      textStyle = VeilTextStyles.cardBody.copy(color = colors.text, fontSize = 15.sp),
      cursorBrush = SolidColor(colors.accent),
      decorationBox = { inner ->
        Box(modifier = Modifier.fillMaxWidth()) {
          if (query.isEmpty()) {
            Text("Find in note…", style = VeilTextStyles.cardBody, color = colors.muted)
          }
          inner()
        }
      },
      modifier = Modifier
        .weight(1f)
        .testTag("find_in_note_input")
    )

    if (query.isNotEmpty()) {
      Text(
        text = if (matchCount == 0) "0/0" else "${activeIndex + 1}/$matchCount",
        style = VeilTextStyles.labelChip,
        color = if (matchCount > 0) colors.accent else colors.muted
      )
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .clickable(enabled = matchCount > 0, onClick = onPrev),
        contentAlignment = Alignment.Center
      ) {
        VeilIcons.ChevronUp(tint = if (matchCount > 0) colors.text else colors.muted.copy(alpha = 0.4f), size = 18.dp)
      }
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .clickable(enabled = matchCount > 0, onClick = onNext),
        contentAlignment = Alignment.Center
      ) {
        VeilIcons.ChevronDown(tint = if (matchCount > 0) colors.text else colors.muted.copy(alpha = 0.4f), size = 18.dp)
      }
    }

    Box(
      modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .clickable(onClick = onClose)
        .testTag("close_find_in_note"),
      contentAlignment = Alignment.Center
    ) {
      VeilIcons.Close(tint = colors.muted, size = 16.dp)
    }
  }
}
