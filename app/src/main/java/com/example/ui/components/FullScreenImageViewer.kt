package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens
import java.io.File

/**
 * Clean responsive grid of attached note images shown at the top of the Note Editor.
 */
@Composable
fun NoteImagesGrid(
  imagePaths: List<String>,
  onImageClick: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  if (imagePaths.isEmpty()) return
  val context = LocalContext.current
  val shape = RoundedCornerShape(14.dp)

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    when (imagePaths.size) {
      1 -> {
        ImageGridTile(
          path = imagePaths[0],
          context = context,
          onClick = { onImageClick(0) },
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(shape)
            .testTag("note_image_0")
        )
      }

      2 -> {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          imagePaths.forEachIndexed { idx, path ->
            ImageGridTile(
              path = path,
              context = context,
              onClick = { onImageClick(idx) },
              modifier = Modifier
                .weight(1f)
                .height(155.dp)
                .clip(shape)
                .testTag("note_image_$idx")
            )
          }
        }
      }

      else -> {
        // Hero first image + clean 2-column grid for remaining attachments
        ImageGridTile(
          path = imagePaths[0],
          context = context,
          onClick = { onImageClick(0) },
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(shape)
            .testTag("note_image_0")
        )
        val rest = imagePaths.drop(1)
        val rows = rest.chunked(2)
        rows.forEachIndexed { rowIdx, pair ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            pair.forEachIndexed { colIdx, path ->
              val realIdx = 1 + rowIdx * 2 + colIdx
              ImageGridTile(
                path = path,
                context = context,
                onClick = { onImageClick(realIdx) },
                modifier = Modifier
                  .weight(1f)
                  .height(126.dp)
                  .clip(shape)
                  .testTag("note_image_$realIdx")
              )
            }
            if (pair.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ImageGridTile(
  path: String,
  context: android.content.Context,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  Box(
    modifier = modifier
      .background(colors.surface)
      .tactilePress(scaleDown = 0.97f, hapticOnClick = false, onClick = onClick)
  ) {
    AsyncImage(
      model = ImageRequest.Builder(context)
        .data(File(path))
        .crossfade(true)
        .build(),
      contentDescription = "Attached note image",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize()
    )
  }
}

/**
 * Full-screen immersive image viewer with pinch-to-zoom, multi-image navigation,
 * and a Three-Dot Menu containing Grab Image Text (OCR), Copy Image, Send/Share, and Delete Image.
 */
@Composable
fun FullScreenImageViewer(
  imagePaths: List<String>,
  initialIndex: Int,
  enableSharing: Boolean,
  isRunningOcr: Boolean,
  onDismiss: () -> Unit,
  onGrabImageText: (String) -> Unit,
  onCopyImage: (String) -> Unit,
  onShareImage: (String) -> Unit,
  onDeleteImage: (String) -> Unit
) {
  if (imagePaths.isEmpty()) {
    LaunchedEffect(Unit) { onDismiss() }
    return
  }

  val colors = VeilThemeTokens.colors
  val context = LocalContext.current
  var currentIndex by remember(imagePaths.size, initialIndex) {
    mutableIntStateOf(initialIndex.coerceIn(0, (imagePaths.size - 1).coerceAtLeast(0)))
  }
  val currentPath = imagePaths.getOrNull(currentIndex) ?: return

  var scale by remember(currentPath) { mutableFloatStateOf(1f) }
  var offset by remember(currentPath) { mutableStateOf(Offset.Zero) }
  var menuExpanded by remember { mutableStateOf(false) }

  BackHandler { onDismiss() }

  val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val effectiveTopPadding = if (statusBarTop > 40.dp) statusBarTop + 8.dp else 48.dp

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xF50B0B14))
      .testTag("full_screen_image_viewer")
  ) {
    // Zoomable image canvas
    Box(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(currentPath) {
          detectTransformGestures { _, pan, zoom, _ ->
            val nextScale = (scale * zoom).coerceIn(1f, 4f)
            scale = nextScale
            offset = if (nextScale > 1.02f) offset + pan else Offset.Zero
          }
        },
      contentAlignment = Alignment.Center
    ) {
      AsyncImage(
        model = ImageRequest.Builder(context)
          .data(File(currentPath))
          .crossfade(true)
          .build(),
        contentDescription = "Full screen attached image",
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = offset.x
            translationY = offset.y
          }
      )
    }

    // OCR progress overlay
    AnimatedVisibility(
      visible = isRunningOcr,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.Center)
    ) {
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(colors.surface.copy(alpha = 0.95f))
          .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          color = colors.accent,
          strokeWidth = 2.dp
        )
        Text("Extracting text…", style = VeilTextStyles.cardTitle, color = colors.text)
      }
    }

    // Top overlay bar: Back + counter + Three-Dot Menu
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 12.dp, end = 12.dp, top = effectiveTopPadding),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      IconButton(
        onClick = onDismiss,
        modifier = Modifier
          .size(48.dp)
          .semantics { contentDescription = "Close image viewer" }
          .testTag("image_viewer_back_button")
      ) {
        VeilIcons.ArrowBack(tint = Color.White, size = 24.dp)
      }

      if (imagePaths.size > 1) {
        Text(
          text = "${currentIndex + 1} / ${imagePaths.size}",
          style = VeilTextStyles.cardTitle,
          color = Color.White.copy(alpha = 0.9f)
        )
      }

      Box {
        IconButton(
          onClick = { menuExpanded = true },
          modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = "Image actions" }
            .testTag("image_viewer_more_button")
        ) {
          VeilIcons.MoreVert(tint = Color.White, size = 24.dp)
        }

        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false },
          containerColor = colors.surface,
          shape = RoundedCornerShape(18.dp)
        ) {
          DropdownMenuItem(
            text = { Text("Grab image text", style = VeilTextStyles.cardBody, color = colors.text) },
            leadingIcon = { VeilIcons.TextScanOutline(tint = colors.accent, size = 20.dp) },
            onClick = {
              menuExpanded = false
              onGrabImageText(currentPath)
            },
            modifier = Modifier.testTag("image_action_ocr")
          )
          DropdownMenuItem(
            text = { Text("Copy image", style = VeilTextStyles.cardBody, color = colors.text) },
            leadingIcon = { VeilIcons.CopyOutline(tint = colors.text, size = 20.dp) },
            onClick = {
              menuExpanded = false
              onCopyImage(currentPath)
            },
            modifier = Modifier.testTag("image_action_copy")
          )
          if (enableSharing) {
            DropdownMenuItem(
              text = { Text("Send / Share", style = VeilTextStyles.cardBody, color = colors.text) },
              leadingIcon = { VeilIcons.ShareOutline(tint = colors.text, size = 20.dp) },
              onClick = {
                menuExpanded = false
                onShareImage(currentPath)
              },
              modifier = Modifier.testTag("image_action_share")
            )
          }
          HorizontalDivider(color = colors.accentSoft, thickness = 1.dp)
          DropdownMenuItem(
            text = { Text("Delete image", style = VeilTextStyles.cardBody, color = colors.doodlePink) },
            leadingIcon = { VeilIcons.Trash(tint = colors.doodlePink, size = 20.dp) },
            onClick = {
              menuExpanded = false
              onDeleteImage(currentPath)
            },
            modifier = Modifier.testTag("image_action_delete")
          )
        }
      }
    }

    // Bottom thumbnail selector strip if there are multiple images
    if (imagePaths.size > 1) {
      Row(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(bottom = 24.dp)
          .clip(RoundedCornerShape(24.dp))
          .background(Color(0xAA141422))
          .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        imagePaths.forEachIndexed { idx, path ->
          val active = idx == currentIndex
          Box(
            modifier = Modifier
              .size(if (active) 48.dp else 40.dp)
              .clip(RoundedCornerShape(10.dp))
              .border(
                width = if (active) 2.dp else 0.dp,
                color = if (active) colors.accent else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
              )
              .clickable { currentIndex = idx }
          ) {
            AsyncImage(
              model = File(path),
              contentDescription = "Thumbnail ${idx + 1}",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }
  }
}
