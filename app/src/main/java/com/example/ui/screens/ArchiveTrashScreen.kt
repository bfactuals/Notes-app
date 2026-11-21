package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.NoteEntity
import com.example.ui.components.VeilIcons
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens
import com.example.viewmodel.VeilViewModel

@Composable
fun ArchiveTrashScreen(
  viewModel: VeilViewModel,
  isTrash: Boolean,
  onBack: () -> Unit,
  onNoteClick: (NoteEntity) -> Unit
) {
  val colors = VeilThemeTokens.colors
  val notes by (if (isTrash) viewModel.trashedNotes else viewModel.archivedNotes).collectAsState()
  val proFeatures by viewModel.proFeatures.collectAsState()

  val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val effectiveTopPadding = if (statusBarTop > 44.dp) statusBarTop + 12.dp else 56.dp

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 12.dp, end = 12.dp, top = effectiveTopPadding, bottom = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(48.dp)
          .semantics { contentDescription = "Navigate back" }
          .testTag("archive_trash_back_button")
      ) {
        VeilIcons.ArrowBack(tint = colors.text, size = 24.dp)
      }

      Text(
        text = if (isTrash) "Trash" else "Archive",
        style = VeilTextStyles.editorTitle,
        color = colors.text,
        modifier = Modifier
          .weight(1f)
          .padding(start = 4.dp)
      )

      if (isTrash && notes.isNotEmpty()) {
        IconButton(
          onClick = { viewModel.emptyTrash() },
          modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = "Empty trash" }
            .testTag("empty_trash_button")
        ) {
          VeilIcons.Trash(tint = colors.text, size = 22.dp)
        }
      }
    }

    // Subtle 30-day auto-empty Trash notice when enabled in Pro Features
    if (isTrash && proFeatures.autoEmptyTrashEnabled) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(colors.surface.copy(alpha = 0.85f))
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .testTag("trash_auto_empty_notice"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        VeilIcons.InfoOutline(tint = colors.muted, size = 16.dp)
        Text(
          text = "Notes in Trash are permanently deleted after 30 days.",
          style = VeilTextStyles.meta,
          color = colors.muted
        )
      }
    }

    if (notes.isEmpty()) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          if (isTrash) {
            VeilIcons.Trash(tint = colors.muted.copy(alpha = 0.5f), size = 36.dp)
          } else {
            VeilIcons.Archive(tint = colors.muted.copy(alpha = 0.5f), size = 36.dp)
          }
          Text(
            text = if (isTrash) "Trash is empty" else "No archived notes",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.muted
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(notes, key = { it.id }) { note ->
          val cardShape = RoundedCornerShape(16.dp)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .shadow(2.dp, cardShape, ambientColor = colors.shadow, spotColor = colors.shadow)
              .clip(cardShape)
              .background(colors.noteCardColor(note.color))
              .clickable { onNoteClick(note) }
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              if (note.title.isNotEmpty()) {
                Text(
                  text = note.title,
                  style = VeilThemeTokens.cardTitleStyle,
                  color = colors.text,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
              }
              Text(
                text = note.content,
                style = VeilThemeTokens.cardBodyStyle,
                color = colors.text,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
              )
            }
            Row {
              IconButton(
                onClick = {
                  if (isTrash) viewModel.restoreNote(note) else viewModel.unarchiveNote(note)
                },
                modifier = Modifier
                  .size(44.dp)
                  .semantics { contentDescription = "Restore note" }
                  .testTag("restore_note_button_${note.id}")
              ) {
                VeilIcons.RestoreOutline(tint = colors.accent, size = 20.dp)
              }
              if (isTrash) {
                IconButton(
                  onClick = { viewModel.deletePermanently(note) },
                  modifier = Modifier
                    .size(44.dp)
                    .semantics { contentDescription = "Delete permanently" }
                    .testTag("delete_permanently_button_${note.id}")
                ) {
                  VeilIcons.Trash(tint = colors.muted, size = 20.dp)
                }
              }
            }
          }
        }
      }
    }
  }
}
