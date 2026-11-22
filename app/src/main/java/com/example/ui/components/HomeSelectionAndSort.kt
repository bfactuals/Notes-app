package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens

/**
 * Clean Sort Menu button placed next to the top search bar.
 */
@Composable
fun SortOrderMenuButton(
  currentSort: String,
  onSelectSort: (String) -> Unit
) {
  val colors = VeilThemeTokens.colors
  var expanded by remember { mutableStateOf(false) }

  val options = remember {
    listOf(
      "custom" to "Custom (Manual)",
      "created_desc" to "Date created (Newest)",
      "created_asc" to "Date created (Oldest)",
      "modified_desc" to "Date modified (Newest)",
      "modified_asc" to "Date modified (Oldest)"
    )
  }

  Box {
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .tactilePress(scaleDown = 0.88f, hapticOnClick = true) { expanded = true }
        .semantics { contentDescription = "Sort notes" }
        .testTag("home_sort_button"),
      contentAlignment = Alignment.Center
    ) {
      VeilIcons.SortOutline(
        tint = if (currentSort != "custom") colors.accent else colors.text,
        size = 21.dp
      )
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      containerColor = colors.surface,
      shape = RoundedCornerShape(18.dp)
    ) {
      Text(
        text = "SORT BY",
        style = VeilTextStyles.sectionLabel,
        color = colors.muted,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
      )
      options.forEach { (key, label) ->
        val selected = currentSort == key
        DropdownMenuItem(
          text = {
            Text(
              text = label,
              style = VeilTextStyles.cardBody.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
              ),
              color = if (selected) colors.accent else colors.text
            )
          },
          trailingIcon = {
            if (selected) {
              VeilIcons.CheckOutline(tint = colors.accent, size = 18.dp)
            }
          },
          onClick = {
            expanded = false
            onSelectSort(key)
          },
          modifier = Modifier.testTag("sort_option_$key")
        )
      }
    }
  }
}

/**
 * Upgraded animated Home Screen Top Selection Bar:
 * Close + Count | Pin, Reminder, Color Picker, Label, and Three-Dot Menu
 * (Archive, Delete, Make a Copy, Copy All Text to Clipboard, Send / Share).
 */
@Composable
fun HomeSelectionTopBar(
  selectedCount: Int,
  allPinned: Boolean,
  enableSharing: Boolean,
  onClearSelection: () -> Unit,
  onTogglePin: () -> Unit,
  onOpenReminder: () -> Unit,
  onOpenColorPicker: () -> Unit,
  onOpenLabelManager: () -> Unit,
  onArchive: () -> Unit,
  onDelete: () -> Unit,
  onMakeCopy: () -> Unit,
  onCopyAllText: () -> Unit,
  onShare: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  var menuExpanded by remember { mutableStateOf(false) }
  val barShape = RoundedCornerShape(24.dp)

  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(48.dp)
      .shadow(
        elevation = 4.dp,
        shape = barShape,
        ambientColor = colors.shadow,
        spotColor = colors.shadow
      )
      .clip(barShape)
      .background(colors.accentSoft)
      .border(1.dp, colors.accent.copy(alpha = 0.28f), barShape)
      .padding(horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .tactilePress(scaleDown = 0.88f, hapticOnClick = true, onClick = onClearSelection)
        .semantics { contentDescription = "Clear selection" }
        .testTag("selection_clear_button"),
      contentAlignment = Alignment.Center
    ) {
      VeilIcons.Close(tint = colors.accent, size = 18.dp)
    }

    Text(
      text = "$selectedCount",
      style = VeilTextStyles.cardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
      color = colors.accent,
      modifier = Modifier.padding(start = 2.dp, end = 6.dp)
    )

    Spacer(modifier = Modifier.weight(1f))

    SelectionBarIconButton(
      description = if (allPinned) "Unpin selected" else "Pin selected",
      tag = "selection_pin_button",
      onClick = onTogglePin
    ) {
      VeilIcons.Pin(tint = colors.accent, size = 20.dp)
    }

    SelectionBarIconButton(
      description = "Set reminder for selected",
      tag = "selection_reminder_button",
      onClick = onOpenReminder
    ) {
      VeilIcons.Bell(tint = colors.accent, size = 20.dp)
    }

    SelectionBarIconButton(
      description = "Change color for selected",
      tag = "selection_color_button",
      onClick = onOpenColorPicker
    ) {
      VeilIcons.PaletteOutline(tint = colors.accent, size = 20.dp)
    }

    SelectionBarIconButton(
      description = "Manage labels for selected",
      tag = "selection_label_button",
      onClick = onOpenLabelManager
    ) {
      VeilIcons.Tag(tint = colors.accent, size = 20.dp)
    }

    Box {
      SelectionBarIconButton(
        description = "More selection actions",
        tag = "selection_more_button",
        onClick = { menuExpanded = true }
      ) {
        VeilIcons.MoreVert(tint = colors.accent, size = 20.dp)
      }

      DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
        containerColor = colors.surface,
        shape = RoundedCornerShape(18.dp)
      ) {
        DropdownMenuItem(
          text = { Text("Archive", style = VeilTextStyles.cardBody, color = colors.text) },
          leadingIcon = { VeilIcons.Archive(tint = colors.text, size = 19.dp) },
          onClick = {
            menuExpanded = false
            onArchive()
          },
          modifier = Modifier.testTag("selection_menu_archive")
        )
        DropdownMenuItem(
          text = { Text("Delete", style = VeilTextStyles.cardBody, color = colors.doodlePink) },
          leadingIcon = { VeilIcons.Trash(tint = colors.doodlePink, size = 19.dp) },
          onClick = {
            menuExpanded = false
            onDelete()
          },
          modifier = Modifier.testTag("selection_menu_delete")
        )
        HorizontalDivider(color = colors.accentSoft, thickness = 1.dp)
        DropdownMenuItem(
          text = { Text("Make a copy", style = VeilTextStyles.cardBody, color = colors.text) },
          leadingIcon = { VeilIcons.CopyOutline(tint = colors.text, size = 19.dp) },
          onClick = {
            menuExpanded = false
            onMakeCopy()
          },
          modifier = Modifier.testTag("selection_menu_copy_note")
        )
        DropdownMenuItem(
          text = { Text("Copy all text", style = VeilTextStyles.cardBody, color = colors.text) },
          leadingIcon = { VeilIcons.NoteDoc(tint = colors.text, size = 19.dp) },
          onClick = {
            menuExpanded = false
            onCopyAllText()
          },
          modifier = Modifier.testTag("selection_menu_copy_text")
        )
        if (enableSharing) {
          DropdownMenuItem(
            text = { Text("Send / Share", style = VeilTextStyles.cardBody, color = colors.text) },
            leadingIcon = { VeilIcons.ShareOutline(tint = colors.text, size = 19.dp) },
            onClick = {
              menuExpanded = false
              onShare()
            },
            modifier = Modifier.testTag("selection_menu_share")
          )
        }
      }
    }
  }
}

@Composable
private fun SelectionBarIconButton(
  description: String,
  tag: String,
  onClick: () -> Unit,
  content: @Composable () -> Unit
) {
  Box(
    modifier = Modifier
      .size(40.dp)
      .clip(CircleShape)
      .tactilePress(scaleDown = 0.86f, hapticOnClick = true, onClick = onClick)
      .semantics { contentDescription = description }
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    content()
  }
}

/**
 * 5-second floating "Archived — Undo" toast bar with subtle linear countdown indicator.
 */
@Composable
fun ArchiveUndoToast(
  visible: Boolean,
  toastKey: Long,
  message: String,
  showUndo: Boolean,
  onUndo: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  val reduceMotion = VeilThemeTokens.reduceMotion
  val progress = remember(toastKey) { Animatable(1f) }

  LaunchedEffect(visible, toastKey) {
    if (visible && showUndo) {
      progress.snapTo(1f)
      progress.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
      )
    }
  }

  AnimatedVisibility(
    visible = visible,
    enter = slideInVertically(
      spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
    ) { it } + fadeIn(tween(if (reduceMotion) 0 else 180)),
    exit = slideOutVertically { it } + fadeOut(tween(if (reduceMotion) 0 else 150)),
    modifier = modifier
  ) {
    val shape = RoundedCornerShape(18.dp)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .shadow(12.dp, shape, ambientColor = colors.toolbarShadow, spotColor = colors.toolbarShadow)
        .clip(shape)
        .background(colors.surface)
        .border(1.dp, colors.hiddenBorder.copy(alpha = 0.5f), shape)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {}
        )
        .testTag("archive_undo_toast")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          VeilIcons.Archive(tint = colors.accent, size = 18.dp)
          Text(
            text = message,
            style = VeilTextStyles.cardBody.copy(fontWeight = FontWeight.Medium),
            color = colors.text
          )
        }
        if (showUndo) {
          Text(
            text = "Undo",
            style = VeilTextStyles.cardTitle.copy(color = colors.accent),
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(colors.accentSoft)
              .tactilePress(scaleDown = 0.90f, hapticOnClick = true, onClick = onUndo)
              .padding(horizontal = 12.dp, vertical = 6.dp)
              .testTag("archive_undo_button")
          )
        }
      }
      if (showUndo) {
        Box(
          modifier = Modifier
            .fillMaxWidth(progress.value.coerceIn(0f, 1f))
            .height(2.5.dp)
            .background(colors.accent)
        )
      }
    }
  }
}
