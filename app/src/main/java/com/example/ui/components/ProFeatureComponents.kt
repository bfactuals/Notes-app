package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CodeBlockData
import com.example.data.NoteImageHelper
import com.example.data.NoteVersionSnapshot
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Compact 1-click popup for Self-Destructing Notes directly above the bottom bar:
 * Off, 1 Hour, 24 Hours, 7 Days.
 */
@Composable
fun SelfDestructQuickPopup(
  currentExpiresAt: Long,
  onSelectDurationMs: (Long) -> Unit, // 0L = Off, > 0L = duration from now
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  val shape = RoundedCornerShape(22.dp)
  val now = System.currentTimeMillis()
  val remainingHours = if (currentExpiresAt > now) (currentExpiresAt - now) / 3_600_000.0 else 0.0

  val selectedKey = when {
    currentExpiresAt <= 0L -> "off"
    remainingHours <= 1.2 -> "1h"
    remainingHours <= 25.0 -> "24h"
    else -> "7d"
  }

  val options = listOf(
    Triple("off", "Off", 0L),
    Triple("1h", "1 Hour", 3_600_000L),
    Triple("24h", "24 Hours", 86_400_000L),
    Triple("7d", "7 Days", 7L * 86_400_000L)
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .shadow(14.dp, shape, ambientColor = colors.toolbarShadow, spotColor = colors.toolbarShadow)
      .clip(shape)
      .background(colors.surface.copy(alpha = 0.97f))
      .border(1.dp, colors.hiddenBorder.copy(alpha = 0.45f), shape)
      .padding(horizontal = 14.dp, vertical = 12.dp)
      .testTag("self_destruct_popup"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      VeilIcons.HourglassOutline(
        tint = if (currentExpiresAt > 0L) colors.accent else colors.muted,
        size = 16.dp
      )
      Text(
        text = "Self-destruct timer",
        style = VeilTextStyles.cardTitle.copy(fontSize = 13.sp),
        color = colors.text
      )
      Spacer(modifier = Modifier.weight(1f))
      if (currentExpiresAt > now) {
        Text(
          text = "⏱ ${formatCountdownBadge(currentExpiresAt, now)}",
          style = VeilTextStyles.labelChip,
          color = colors.accent
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      options.forEach { (key, label, durationMs) ->
        val active = selectedKey == key
        Box(
          modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) colors.accentSoft else colors.bg.copy(alpha = 0.72f))
            .then(
              if (active) Modifier.border(1.dp, colors.accent.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
              else Modifier
            )
            .tactilePress(scaleDown = 0.92f, hapticOnClick = true) {
              onSelectDurationMs(durationMs)
            }
            .testTag("self_destruct_option_$key"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = label,
            style = VeilTextStyles.labelChip.copy(
              fontSize = 12.sp,
              fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = if (active) colors.accent else colors.text,
            maxLines = 1
          )
        }
      }
    }
  }
}

/**
 * Clean subtly shaded rounded Prompt / Code Block with a compact one-tap "Copy" button
 * that copies ONLY this block's text and briefly switches to "Copied!".
 */
@Composable
fun CopyableCodeBlockCard(
  block: CodeBlockData,
  isReadOnlyMode: Boolean,
  onTextChange: (String) -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var justCopied by remember(block.id) { mutableStateOf(false) }

  val containerBg = if (colors.isDark) {
    colors.bg.copy(alpha = 0.72f)
  } else {
    colors.bg.copy(alpha = 0.92f)
  }
  val shape = RoundedCornerShape(14.dp)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(containerBg)
      .border(1.dp, colors.hiddenBorder.copy(alpha = 0.55f), shape)
      .padding(horizontal = 12.dp, vertical = 10.dp)
      .testTag("code_block_${block.id}"),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        VeilIcons.CodeOutline(tint = colors.muted, size = 14.dp)
        Text(
          text = "PROMPT / CODE",
          style = VeilTextStyles.labelChip.copy(fontSize = 10.sp, letterSpacing = 0.5.sp),
          color = colors.muted
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (justCopied) colors.accent else colors.accentSoft)
            .tactilePress(scaleDown = 0.92f, hapticOnClick = true) {
              NoteImageHelper.copyTextToClipboard(context, "Snippet", block.text)
              justCopied = true
              scope.launch {
                delay(1800L)
                justCopied = false
              }
            }
            .padding(horizontal = 9.dp, vertical = 4.dp)
            .testTag("code_block_copy_${block.id}"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          if (justCopied) {
            VeilIcons.CheckOutline(tint = colors.surface, size = 12.dp)
            Text(
              text = "Copied!",
              style = VeilTextStyles.labelChip.copy(fontWeight = FontWeight.SemiBold),
              color = colors.surface
            )
          } else {
            VeilIcons.CopyOutline(tint = colors.accent, size = 12.dp)
            Text(
              text = "Copy",
              style = VeilTextStyles.labelChip.copy(fontWeight = FontWeight.SemiBold),
              color = colors.accent
            )
          }
        }

        if (!isReadOnlyMode) {
          Box(
            modifier = Modifier
              .size(24.dp)
              .clip(CircleShape)
              .clickable { onDelete() }
              .semantics { contentDescription = "Remove code block" }
              .testTag("code_block_delete_${block.id}"),
            contentAlignment = Alignment.Center
          ) {
            VeilIcons.Close(tint = colors.muted, size = 13.dp)
          }
        }
      }
    }

    val monoStyle = VeilThemeTokens.editorBodyStyle.copy(
      fontFamily = FontFamily.Monospace,
      fontSize = (13.5f * VeilThemeTokens.typographyPrefs.scale).sp,
      lineHeight = (20.5f * VeilThemeTokens.typographyPrefs.scale).sp,
      color = colors.text
    )

    if (isReadOnlyMode) {
      Text(
        text = block.text.ifBlank { "Empty snippet" },
        style = monoStyle,
        color = if (block.text.isBlank()) colors.muted.copy(alpha = 0.55f) else colors.text,
        modifier = Modifier.fillMaxWidth()
      )
    } else {
      BasicTextField(
        value = block.text,
        onValueChange = onTextChange,
        textStyle = monoStyle,
        cursorBrush = SolidColor(colors.accent),
        decorationBox = { inner ->
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 44.dp)
          ) {
            if (block.text.isEmpty()) {
              Text(
                text = "Paste a prompt, command, or code snippet…",
                style = monoStyle,
                color = colors.muted.copy(alpha = 0.55f)
              )
            }
            inner()
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("code_block_input_${block.id}")
      )
    }
  }
}

/**
 * Local Version History dialog showing up to 5 timestamped previous versions
 * with expandable Preview and one-tap Restore.
 */
@Composable
fun VersionHistoryDialog(
  versions: List<NoteVersionSnapshot>,
  onRestore: (NoteVersionSnapshot) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  var expandedIndex by remember { mutableIntStateOf(0) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.surface,
    titleContentColor = colors.text,
    textContentColor = colors.text,
    shape = RoundedCornerShape(24.dp),
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        VeilIcons.HistoryOutline(tint = colors.accent, size = 20.dp)
        Text("Version History", style = VeilTextStyles.cardTitle)
      }
    },
    text = {
      if (versions.isEmpty()) {
        Text(
          text = "No previous versions yet. Snapshots are saved automatically whenever you make meaningful edits.",
          style = VeilTextStyles.cardBody,
          color = colors.muted
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(versions.size) { index ->
            val v = versions[index]
            val isExpanded = expandedIndex == index
            val cardShape = RoundedCornerShape(14.dp)
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(colors.bg.copy(alpha = 0.78f))
                .border(
                  1.dp,
                  if (isExpanded) colors.accent.copy(alpha = 0.45f) else colors.hiddenBorder.copy(alpha = 0.4f),
                  cardShape
                )
                .clickable { expandedIndex = if (isExpanded) -1 else index }
                .padding(12.dp)
                .testTag("version_item_$index"),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = formatVersionTimestamp(v.timestamp),
                    style = VeilTextStyles.labelChip.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accent
                  )
                  if (v.title.isNotBlank()) {
                    Text(
                      text = v.title,
                      style = VeilTextStyles.cardTitle.copy(fontSize = 13.sp),
                      color = colors.text,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = if (isExpanded) "Hide" else "Preview",
                    style = VeilTextStyles.labelChip,
                    color = colors.muted
                  )
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(10.dp))
                      .background(colors.accentSoft)
                      .tactilePress(scaleDown = 0.92f, hapticOnClick = true) {
                        onRestore(v)
                      }
                      .padding(horizontal = 10.dp, vertical = 5.dp)
                      .testTag("restore_version_$index")
                  ) {
                    Text(
                      text = "Restore",
                      style = VeilTextStyles.labelChip.copy(fontWeight = FontWeight.SemiBold),
                      color = colors.accent
                    )
                  }
                }
              }

              Text(
                text = v.content.ifBlank { "(Empty body)" },
                style = VeilTextStyles.cardBody.copy(fontSize = 13.sp),
                color = colors.muted,
                maxLines = if (isExpanded) 10 else 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", style = VeilTextStyles.cardTitle, color = colors.accent)
      }
    }
  )
}

/**
 * Sleek floating Clipboard Quick-Save Pill shown at the bottom of HomeScreen for 5 seconds
 * when new unsaved clipboard text is silently detected.
 */
@Composable
fun ClipboardQuickSavePill(
  visible: Boolean,
  previewText: String,
  onSave: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  val reduceMotion = VeilThemeTokens.reduceMotion
  val pillShape = RoundedCornerShape(24.dp)

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(if (reduceMotion) 0 else 200)) + slideInVertically(
      spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
    ) { 40 },
    exit = fadeOut(tween(if (reduceMotion) 0 else 160)) + slideOutVertically { 32 },
    modifier = modifier
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .shadow(12.dp, pillShape, ambientColor = colors.toolbarShadow, spotColor = colors.toolbarShadow)
        .clip(pillShape)
        .background(colors.surface.copy(alpha = 0.98f))
        .border(1.dp, colors.accent.copy(alpha = 0.4f), pillShape)
        .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
        .testTag("clipboard_quick_save_pill"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      VeilIcons.ClipboardOutline(tint = colors.accent, size = 19.dp)

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Save copied text as note?",
          style = VeilTextStyles.cardTitle.copy(fontSize = 13.sp),
          color = colors.text,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = previewText.replace('\n', ' ').trim(),
          style = VeilTextStyles.meta,
          color = colors.muted,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(14.dp))
          .background(colors.accent)
          .tactilePress(scaleDown = 0.92f, hapticOnClick = true, role = Role.Button, onClick = onSave)
          .padding(horizontal = 12.dp, vertical = 6.dp)
          .testTag("clipboard_quick_save_button"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Save",
          style = VeilTextStyles.labelChip.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
          color = colors.surface
        )
      }

      Box(
        modifier = Modifier
          .size(30.dp)
          .clip(CircleShape)
          .tactilePress(scaleDown = 0.88f, hapticOnClick = true, role = Role.Button, onClick = onDismiss)
          .semantics { contentDescription = "Dismiss clipboard prompt" }
          .testTag("clipboard_quick_save_dismiss"),
        contentAlignment = Alignment.Center
      ) {
        VeilIcons.Close(tint = colors.muted, size = 15.dp)
      }
    }
  }
}

/**
 * Slim horizontally scrollable row of offline Smart Auto-Category filter chips
 * directly below the Search Bar on the Home screen.
 */
@Composable
fun SmartCategoryFilterRow(
  selectedCategory: NoteSmartCategory,
  counts: Map<NoteSmartCategory, Int>,
  onSelectCategory: (NoteSmartCategory) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  LazyRow(
    modifier = modifier
      .fillMaxWidth()
      .testTag("smart_category_filter_row"),
    contentPadding = PaddingValues(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    items(NoteSmartCategory.entries) { category ->
      val active = selectedCategory == category
      val count = counts[category] ?: 0
      val chipShape = RoundedCornerShape(16.dp)
      Row(
        modifier = Modifier
          .height(32.dp)
          .clip(chipShape)
          .background(if (active) colors.accentSoft else colors.surface.copy(alpha = 0.85f))
          .border(
            width = 1.dp,
            color = if (active) colors.accent.copy(alpha = 0.55f) else colors.hiddenBorder.copy(alpha = 0.35f),
            shape = chipShape
          )
          .tactilePress(scaleDown = 0.94f, hapticOnClick = true) {
            onSelectCategory(category)
          }
          .padding(horizontal = 12.dp)
          .testTag("smart_cat_${category.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
      ) {
        Text(
          text = category.label,
          style = VeilTextStyles.labelChip.copy(
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
          ),
          color = if (active) colors.accent else colors.text
        )
        if (category != NoteSmartCategory.ALL && count > 0) {
          Text(
            text = count.toString(),
            style = VeilTextStyles.labelChip.copy(fontSize = 10.5.sp),
            color = if (active) colors.accent else colors.muted
          )
        }
      }
    }
  }
}
