package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
 * Clean 4-button Google Keep-style Note Editor Bottom Bar:
 * Left: "+", [optional subtle Self-Destruct Timer icon when Pro enabled], Color Palette, "Aa"
 * Right: "⋮" (Three-Dot Menu)
 */
@Composable
fun NoteEditorBottomBar(
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
  val colors = VeilThemeTokens.colors
  val barShape = RoundedCornerShape(24.dp)

  Row(
    modifier = modifier
      .fillMaxWidth()
      .shadow(
        elevation = 10.dp,
        shape = barShape,
        ambientColor = colors.toolbarShadow,
        spotColor = colors.toolbarShadow
      )
      .clip(barShape)
      .background(colors.surface.copy(alpha = 0.96f))
      .border(1.dp, colors.hiddenBorder.copy(alpha = 0.35f), barShape)
      .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    // Left side: 1) "+", [optional Timer right next to "+"], 2) Color Palette, 3) "Aa"
    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      EditorBottomActionButton(
        active = isInsertOpen,
        enabled = !isReadOnlyMode,
        description = "Insert attachment or tool",
        tag = "editor_insert_plus_button",
        onClick = onToggleInsert
      ) { tint ->
        Box(
          modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.8.dp, tint, RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center
        ) {
          VeilIcons.Plus(tint = tint, size = 16.dp)
        }
      }

      if (showTimerButton) {
        EditorBottomActionButton(
          active = isTimerOpen || hasActiveTimer,
          enabled = true,
          description = "Self-destruct timer",
          tag = "editor_self_destruct_button",
          onClick = onToggleTimer
        ) { tint ->
          VeilIcons.HourglassOutline(tint = tint, size = 21.dp)
        }
      }

      EditorBottomActionButton(
        active = isPaletteOpen,
        enabled = true,
        description = "Note colors and backgrounds",
        tag = "editor_color_button",
        onClick = onTogglePalette
      ) { tint ->
        VeilIcons.PaletteOutline(tint = tint, size = 22.dp)
      }

      EditorBottomActionButton(
        active = isFormatBarOpen,
        enabled = !isReadOnlyMode,
        description = "Rich text formatting",
        tag = "editor_format_aa_button",
        onClick = onToggleFormatBar
      ) { tint ->
        Text(
          text = "Aa",
          style = VeilTextStyles.cardTitle.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp
          ),
          color = tint
        )
      }
    }

    // Right side: 4) "⋮" Three-Dot Menu
    EditorBottomActionButton(
      active = isOverflowOpen,
      enabled = true,
      description = "More note options",
      tag = "editor_overflow_menu_button",
      onClick = onToggleOverflow
    ) { tint ->
      VeilIcons.MoreVert(tint = tint, size = 22.dp)
    }
  }
}

@Composable
private fun EditorBottomActionButton(
  active: Boolean,
  enabled: Boolean = true,
  description: String,
  tag: String,
  onClick: () -> Unit,
  content: @Composable (Color) -> Unit
) {
  val colors = VeilThemeTokens.colors
  val tint = when {
    !enabled -> colors.muted.copy(alpha = 0.35f)
    active -> colors.accent
    else -> colors.text
  }
  Box(
    modifier = Modifier
      .size(48.dp)
      .clip(CircleShape)
      .background(if (active && enabled) colors.accentSoft else Color.Transparent)
      .tactilePress(scaleDown = 0.88f, enabled = enabled, hapticOnClick = true, onClick = onClick)
      .semantics { contentDescription = description }
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    content(tint)
  }
}

/**
 * True WYSIWYG Rich Text Formatting Bar toggled by "Aa" right above the keyboard.
 * Active buttons glow with accent tint so users always see current heading & inline styles.
 * Includes: H1, H2, Normal text, Bold, Italic, Underline, Strikethrough, Highlight.
 */
@Composable
fun RichTextFormattingBar(
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
  val shape = RoundedCornerShape(22.dp)

  LazyRow(
    modifier = modifier
      .fillMaxWidth()
      .shadow(8.dp, shape, ambientColor = colors.toolbarShadow, spotColor = colors.toolbarShadow)
      .clip(shape)
      .background(colors.surface.copy(alpha = 0.96f))
      .border(1.dp, colors.hiddenBorder.copy(alpha = 0.35f), shape)
      .padding(horizontal = 10.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    item {
      FormatChip(
        label = "H1",
        active = activeHeadingLevel == 1,
        weight = FontWeight.Bold,
        onClick = onHeading1,
        tag = "fmt_h1"
      )
    }
    item {
      FormatChip(
        label = "H2",
        active = activeHeadingLevel == 2,
        weight = FontWeight.SemiBold,
        onClick = onHeading2,
        tag = "fmt_h2"
      )
    }
    item {
      FormatChip(
        label = "Normal",
        active = activeHeadingLevel == 0,
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
          .background(colors.accentSoft)
      )
    }
    item {
      FormatChip(
        label = "B",
        active = RichStyle.BOLD in activeInlineStyles,
        weight = FontWeight.Bold,
        onClick = onBold,
        tag = "fmt_bold"
      )
    }
    item {
      FormatChip(
        label = "I",
        active = RichStyle.ITALIC in activeInlineStyles,
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
  weight: FontWeight = FontWeight.Medium,
  style: FontStyle = FontStyle.Normal,
  decoration: TextDecoration = TextDecoration.None,
  highlightBadge: Boolean = false,
  onClick: () -> Unit,
  tag: String
) {
  val colors = VeilThemeTokens.colors
  val bg = when {
    active && highlightBadge -> colors.highlight.copy(alpha = 0.7f)
    active -> colors.accentSoft
    else -> colors.bg.copy(alpha = 0.72f)
  }
  val fg = when {
    active && highlightBadge -> colors.text
    active -> colors.accent
    else -> colors.text
  }

  Box(
    modifier = Modifier
      .height(36.dp)
      .defaultMinSize(minWidth = 38.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(bg)
      .then(if (active) Modifier.border(1.dp, colors.accent.copy(alpha = 0.55f), RoundedCornerShape(14.dp)) else Modifier)
      .tactilePress(scaleDown = 0.90f, hapticOnClick = true, onClick = onClick)
      .padding(horizontal = 11.dp)
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
 * Compact glassmorphism popup for the "+" Insert button.
 * Includes optional "Prompt / Code Block" when enabled in Pro Features.
 */
@Composable
fun InsertToolsPanel(
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
  val shape = RoundedCornerShape(24.dp)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .shadow(14.dp, shape, ambientColor = colors.toolbarShadow, spotColor = colors.toolbarShadow)
      .clip(shape)
      .background(colors.surface.copy(alpha = 0.95f))
      .border(1.dp, colors.hiddenBorder.copy(alpha = 0.45f), shape)
      .padding(vertical = 8.dp, horizontal = 8.dp)
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
 * Compact popup for the "⋮" Three-Dot Menu in Note Editor.
 * Contains: Read Mode, [optional Version History], Reminder, Labels, Find in Note,
 * Make a Copy, Copy All Text, Send/Share, Archive, Delete. Zero Hide/Lock icons anywhere.
 */
@Composable
fun NoteOverflowMenuPanel(
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
  val shape = RoundedCornerShape(24.dp)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .shadow(14.dp, shape, ambientColor = colors.toolbarShadow, spotColor = colors.toolbarShadow)
      .clip(shape)
      .background(colors.surface.copy(alpha = 0.96f))
      .border(1.dp, colors.hiddenBorder.copy(alpha = 0.45f), shape)
      .padding(vertical = 8.dp, horizontal = 8.dp)
      .testTag("editor_overflow_panel")
  ) {
    PanelActionRow(
      label = if (isReadOnlyMode) "Read mode (On)" else "Read mode",
      labelColor = if (isReadOnlyMode) colors.accent else null,
      icon = { VeilIcons.ReadBookOutline(tint = if (isReadOnlyMode) colors.accent else colors.text, size = 21.dp) },
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
        icon = { VeilIcons.HistoryOutline(tint = colors.text, size = 21.dp) },
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
      icon = { VeilIcons.Bell(tint = if (hasReminder) colors.accent else colors.text, size = 21.dp) },
      onClick = onReminder,
      tag = "overflow_reminder"
    )
    PanelActionRow(
      label = "Labels",
      icon = { VeilIcons.Tag(tint = if (hasLabels) colors.accent else colors.text, size = 21.dp) },
      onClick = onLabels,
      tag = "overflow_labels"
    )
    PanelActionRow(
      label = "Find in note",
      icon = { VeilIcons.Search(tint = colors.text, size = 20.dp) },
      onClick = onFindInNote,
      tag = "overflow_find"
    )
    PanelActionRow(
      label = "Make a copy",
      icon = { VeilIcons.CopyOutline(tint = colors.text, size = 21.dp) },
      onClick = onMakeCopy,
      tag = "overflow_copy_note"
    )
    PanelActionRow(
      label = "Copy all text",
      icon = { VeilIcons.NoteDoc(tint = colors.text, size = 21.dp) },
      onClick = onCopyAllText,
      tag = "overflow_copy_text"
    )
    if (enableSharing) {
      PanelActionRow(
        label = "Send / Share",
        icon = { VeilIcons.ShareOutline(tint = colors.text, size = 21.dp) },
        onClick = onSendShare,
        tag = "overflow_share"
      )
    }
    HorizontalDivider(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
      color = colors.accentSoft,
      thickness = 1.dp
    )
    PanelActionRow(
      label = "Archive",
      icon = { VeilIcons.Archive(tint = colors.text, size = 21.dp) },
      onClick = onArchive,
      tag = "overflow_archive"
    )
    PanelActionRow(
      label = "Delete",
      labelColor = colors.doodlePink,
      icon = { VeilIcons.Trash(tint = colors.doodlePink, size = 21.dp) },
      onClick = onDelete,
      tag = "overflow_delete"
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
