package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.DoodleStrokeData
import com.example.ui.theme.VeilEasing
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens
import kotlin.math.hypot

enum class EditorDrawTool(val brushTypeId: Int, val minSize: Float, val maxSize: Float, val defaultSize: Float) {
  PEN(0, 1.5f, 16f, 3.4f),
  FOUNTAIN_PEN(1, 0.8f, 7.5f, 2.2f),
  HIGHLIGHTER(2, 8f, 36f, 15f),
  ERASER(3, 8f, 44f, 20f)
}

/**
 * Full-page drawing surface with slim tap-to-reveal drawing toolbar.
 * Auto-collapses the brush size/color popover as soon as the user touches the canvas.
 */
@Composable
fun FullPageDrawingCanvas(
  strokes: List<DoodleStrokeData>,
  selectedTool: EditorDrawTool,
  selectedColorIndex: Int,
  toolStrokeSizes: Map<EditorDrawTool, Float>,
  onStrokeSizeChange: (EditorDrawTool, Float) -> Unit,
  onAddStroke: (DoodleStrokeData) -> Unit,
  onSelectTool: (EditorDrawTool) -> Unit,
  onSelectColor: (Int) -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  canUndo: Boolean,
  canRedo: Boolean,
  onBackAndDone: () -> Unit,
  onSwitchToTextNote: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  val reduceMotion = VeilThemeTokens.reduceMotion
  val haptic = LocalHapticFeedback.current
  val strokePalette = remember(colors) { colors.drawingPalette() }

  var canvasSize by remember { mutableStateOf(IntSize.Zero) }
  var activePoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
  var showBrushPopover by remember { mutableStateOf(false) }

  val currentColorIndex = if (selectedTool == EditorDrawTool.ERASER) -1 else selectedColorIndex
  val currentWidthDp = toolStrokeSizes[selectedTool] ?: selectedTool.defaultSize
  val currentAlpha = if (selectedTool == EditorDrawTool.HIGHLIGHTER) 0.38f else 1f
  val currentBrushType = selectedTool.brushTypeId

  val latestColorIndex by rememberUpdatedState(currentColorIndex)
  val latestWidthDp by rememberUpdatedState(currentWidthDp)
  val latestAlpha by rememberUpdatedState(currentAlpha)
  val latestBrushType by rememberUpdatedState(currentBrushType)
  val latestCanvasSize by rememberUpdatedState(canvasSize)
  val latestOnAddStroke by rememberUpdatedState(onAddStroke)

  val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val effectiveTopPadding = if (statusBarTop > 44.dp) statusBarTop + 12.dp else 56.dp
  val density = LocalDensity.current
  val slideOffsetPx = with(density) { 24.dp.roundToPx() }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.surface)
      .testTag("full_page_drawing_canvas")
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .onSizeChanged { canvasSize = it }
        .pointerInput(Unit) {
          detectTapGestures(onTap = {
            if (showBrushPopover) showBrushPopover = false
          })
        }
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { offset ->
              // Auto-collapse popover immediately when drawing starts
              showBrushPopover = false
              val w = latestCanvasSize.width.coerceAtLeast(1).toFloat()
              val h = latestCanvasSize.height.coerceAtLeast(1).toFloat()
              activePoints = listOf((offset.x / w).coerceIn(0f, 1f) to (offset.y / h).coerceIn(0f, 1f))
            },
            onDrag = { change, _ ->
              change.consume()
              val w = latestCanvasSize.width.coerceAtLeast(1).toFloat()
              val h = latestCanvasSize.height.coerceAtLeast(1).toFloat()
              activePoints = activePoints + ((change.position.x / w).coerceIn(0f, 1f) to (change.position.y / h).coerceIn(0f, 1f))
            },
            onDragEnd = {
              if (activePoints.isNotEmpty()) {
                latestOnAddStroke(
                  DoodleStrokeData(
                    points = activePoints,
                    colorIndex = latestColorIndex,
                    widthDp = latestWidthDp,
                    alpha = latestAlpha,
                    brushType = latestBrushType
                  )
                )
                activePoints = emptyList()
              }
            },
            onDragCancel = { activePoints = emptyList() }
          )
        }
    ) {
      drawNormalizedStrokes(strokes, strokePalette, colors.surface)
      if (activePoints.isNotEmpty()) {
        val activeStroke = DoodleStrokeData(
          points = activePoints,
          colorIndex = currentColorIndex,
          widthDp = currentWidthDp,
          alpha = currentAlpha,
          brushType = currentBrushType
        )
        drawNormalizedStrokes(listOf(activeStroke), strokePalette, colors.surface)
      }
    }

    // Minimalist transparent Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 12.dp, end = 12.dp, top = effectiveTopPadding),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      IconButton(
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onBackAndDone()
        },
        modifier = Modifier
          .size(48.dp)
          .semantics { contentDescription = "Save drawing and go back" }
          .testTag("full_draw_back_button")
      ) {
        VeilIcons.ArrowBack(tint = colors.text, size = 24.dp)
      }

      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(colors.bg.copy(alpha = 0.92f))
          .tactilePress(scaleDown = 0.94f, hapticOnClick = true, onClick = onSwitchToTextNote)
          .padding(horizontal = 14.dp, vertical = 8.dp)
          .testTag("switch_to_text_note_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        VeilIcons.NoteDoc(tint = colors.text, size = 16.dp)
        Text("Add text", style = VeilTextStyles.toolbarLabel, color = colors.text)
      }
    }

    // Ultra-slim Drawing Toolbar with tap-to-reveal brush popover
    AnimatedVisibility(
      visible = true,
      enter = slideInVertically(
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 250, easing = VeilEasing),
        initialOffsetY = { slideOffsetPx }
      ) + fadeIn(tween(if (reduceMotion) 0 else 250, easing = VeilEasing)),
      exit = slideOutVertically(
        animationSpec = tween(if (reduceMotion) 0 else 200),
        targetOffsetY = { slideOffsetPx }
      ) + fadeOut(tween(if (reduceMotion) 0 else 200)),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
    ) {
      FloatingDrawingToolbar(
        strokePalette = strokePalette,
        selectedColorIndex = selectedColorIndex,
        selectedTool = selectedTool,
        strokeSizeDp = currentWidthDp,
        isPopoverExpanded = showBrushPopover,
        onTogglePopover = { showBrushPopover = it },
        onStrokeSizeChange = { onStrokeSizeChange(selectedTool, it) },
        canUndo = canUndo,
        canRedo = canRedo,
        onSelectColor = onSelectColor,
        onSelectTool = onSelectTool,
        onUndo = onUndo,
        onRedo = onRedo,
        onClose = onBackAndDone
      )
    }
  }
}

/**
 * Compact inline Draw Box inside a text note.
 * - 60fps Smooth Resizing with parent scroll lock while dragging handles or sketching.
 * - Calls onDrawStart() immediately when drawing begins so any open brush popover auto-collapses.
 * - Top-right Delete button whenever active or in resize mode.
 */
@Composable
fun ResizableInlineDrawBox(
  strokes: List<DoodleStrokeData>,
  heightDp: Float,
  widthFraction: Float,
  isDrawActive: Boolean,
  isResizeMode: Boolean,
  selectedTool: EditorDrawTool,
  selectedColorIndex: Int,
  strokeSizeDp: Float,
  onGestureLockChanged: (Boolean) -> Unit,
  onDrawStart: () -> Unit,
  onEnterDrawMode: () -> Unit,
  onEnterResizeMode: () -> Unit,
  onResize: (newHeightDp: Float, newWidthFraction: Float) -> Unit,
  onAddStroke: (DoodleStrokeData) -> Unit,
  onDeleteDrawBox: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  val reduceMotion = VeilThemeTokens.reduceMotion
  val haptic = LocalHapticFeedback.current
  val density = LocalDensity.current
  val strokePalette = remember(colors) { colors.drawingPalette() }

  var parentMaxWidthPx by remember { mutableFloatStateOf(1f) }
  var boxSize by remember { mutableStateOf(IntSize.Zero) }
  var activePoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

  var localHeightDp by remember { mutableFloatStateOf(heightDp) }
  var localWidthFrac by remember { mutableFloatStateOf(widthFraction) }
  var isActivelyResizing by remember { mutableStateOf(false) }

  LaunchedEffect(heightDp, widthFraction, isActivelyResizing) {
    if (!isActivelyResizing) {
      localHeightDp = heightDp
      localWidthFrac = widthFraction
    }
  }

  val currentColorIndex = if (selectedTool == EditorDrawTool.ERASER) -1 else selectedColorIndex
  val currentAlpha = if (selectedTool == EditorDrawTool.HIGHLIGHTER) 0.38f else 1f
  val currentBrushType = selectedTool.brushTypeId

  val latestColorIndex by rememberUpdatedState(currentColorIndex)
  val latestStrokeSize by rememberUpdatedState(strokeSizeDp)
  val latestAlpha by rememberUpdatedState(currentAlpha)
  val latestBrushType by rememberUpdatedState(currentBrushType)
  val latestBoxSize by rememberUpdatedState(boxSize)
  val latestParentWidth by rememberUpdatedState(parentMaxWidthPx)
  val latestOnResize by rememberUpdatedState(onResize)
  val latestOnAddStroke by rememberUpdatedState(onAddStroke)
  val latestOnGestureLock by rememberUpdatedState(onGestureLockChanged)
  val latestOnDrawStart by rememberUpdatedState(onDrawStart)
  val latestIsDrawActive by rememberUpdatedState(isDrawActive)
  val latestIsResizeMode by rememberUpdatedState(isResizeMode)

  val blockShape = RoundedCornerShape(16.dp)
  val resizeScale by animateFloatAsState(
    targetValue = if (isActivelyResizing && !reduceMotion) 1.008f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "draw_box_resize_scale"
  )

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .onSizeChanged { parentMaxWidthPx = it.width.coerceAtLeast(1).toFloat() }
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(localWidthFrac.coerceIn(0.45f, 1f))
        .height(localHeightDp.coerceIn(100f, 480f).dp)
        .graphicsLayer {
          scaleX = resizeScale
          scaleY = resizeScale
        }
        .shadow(
          elevation = if (isResizeMode || isActivelyResizing) 8.dp else 3.dp,
          shape = blockShape,
          ambientColor = colors.shadow,
          spotColor = colors.shadow
        )
        .clip(blockShape)
        .background(colors.surface)
        .then(
          if (isResizeMode) {
            Modifier.border(2.dp, colors.accent, blockShape)
          } else if (isDrawActive) {
            Modifier.border(1.5.dp, colors.accent.copy(alpha = 0.55f), blockShape)
          } else {
            Modifier
          }
        )
        .onSizeChanged { boxSize = it }
        .pointerInput(Unit) {
          detectTapGestures(
            onLongPress = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onEnterResizeMode()
            },
            onTap = {
              if (!latestIsResizeMode && !latestIsDrawActive) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onEnterDrawMode()
              } else {
                latestOnDrawStart()
              }
            }
          )
        }
        .testTag("inline_doodle_block")
    ) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .padding(8.dp)
          .pointerInput(isDrawActive, isResizeMode) {
            if (!isDrawActive || isResizeMode) return@pointerInput
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
              latestOnDrawStart()
              latestOnGestureLock(true)
              val w = latestBoxSize.width.coerceAtLeast(1).toFloat()
              val h = latestBoxSize.height.coerceAtLeast(1).toFloat()
              val pts = mutableListOf(
                (down.position.x / w).coerceIn(0f, 1f) to (down.position.y / h).coerceIn(0f, 1f)
              )
              activePoints = pts.toList()
              down.consume()

              try {
                while (true) {
                  val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                  val change = event.changes.firstOrNull { it.id == down.id } ?: break
                  if (!change.pressed) {
                    change.consume()
                    break
                  }
                  change.consume()
                  val cw = latestBoxSize.width.coerceAtLeast(1).toFloat()
                  val ch = latestBoxSize.height.coerceAtLeast(1).toFloat()
                  pts.add((change.position.x / cw).coerceIn(0f, 1f) to (change.position.y / ch).coerceIn(0f, 1f))
                  activePoints = pts.toList()
                }
              } finally {
                if (pts.isNotEmpty()) {
                  latestOnAddStroke(
                    DoodleStrokeData(
                      points = pts.toList(),
                      colorIndex = latestColorIndex,
                      widthDp = latestStrokeSize,
                      alpha = latestAlpha,
                      brushType = latestBrushType
                    )
                  )
                }
                activePoints = emptyList()
                latestOnGestureLock(false)
              }
            }
          }
      ) {
        drawNormalizedStrokes(strokes, strokePalette, colors.surface)
        if (activePoints.isNotEmpty()) {
          val s = DoodleStrokeData(
            points = activePoints,
            colorIndex = currentColorIndex,
            widthDp = strokeSizeDp,
            alpha = currentAlpha,
            brushType = currentBrushType
          )
          drawNormalizedStrokes(listOf(s), strokePalette, colors.surface)
        }
      }

      if (strokes.isEmpty() && activePoints.isEmpty() && !isResizeMode) {
        Text(
          text = if (isDrawActive) "Sketch here" else "Tap to sketch · Hold to resize",
          style = VeilTextStyles.meta,
          color = colors.muted.copy(alpha = 0.6f),
          modifier = Modifier.align(Alignment.Center)
        )
      }

      AnimatedVisibility(
        visible = isDrawActive || isResizeMode,
        enter = scaleIn(
          spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        ) + fadeIn(tween(150)),
        exit = scaleOut(tween(120)) + fadeOut(tween(120)),
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .shadow(4.dp, CircleShape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .clip(CircleShape)
            .background(colors.bg.copy(alpha = 0.92f))
            .border(1.dp, colors.hiddenBorder, CircleShape)
            .tactilePress(scaleDown = 0.86f, hapticOnClick = true, onClick = onDeleteDrawBox)
            .semantics { contentDescription = "Delete drawing box" }
            .testTag("delete_inline_draw_box_button"),
          contentAlignment = Alignment.Center
        ) {
          VeilIcons.Trash(tint = colors.doodlePink, size = 16.dp)
        }
      }

      if (isResizeMode) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 2.dp)
            .size(width = 76.dp, height = 32.dp)
            .pointerInput(Unit) {
              awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                down.consume()
                isActivelyResizing = true
                latestOnGestureLock(true)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                try {
                  while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                      change.consume()
                      break
                    }
                    val deltaY = change.positionChange().y
                    if (deltaY != 0f) {
                      change.consume()
                      val deltaDp = with(density) { deltaY.toDp().value }
                      localHeightDp = (localHeightDp + deltaDp).coerceIn(100f, 480f)
                      latestOnResize(localHeightDp, localWidthFrac)
                    }
                  }
                } finally {
                  isActivelyResizing = false
                  latestOnGestureLock(false)
                }
              }
            }
            .testTag("doodle_bottom_resize_handle"),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(width = 42.dp, height = 5.dp)
              .clip(CircleShape)
              .background(colors.accent)
          )
        }

        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(4.dp)
            .size(36.dp)
            .pointerInput(Unit) {
              awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                down.consume()
                isActivelyResizing = true
                latestOnGestureLock(true)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                try {
                  while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                      change.consume()
                      break
                    }
                    val delta = change.positionChange()
                    if (delta != Offset.Zero) {
                      change.consume()
                      val deltaHDp = with(density) { delta.y.toDp().value }
                      val deltaWFrac = delta.x / latestParentWidth.coerceAtLeast(1f)
                      localHeightDp = (localHeightDp + deltaHDp).coerceIn(100f, 480f)
                      localWidthFrac = (localWidthFrac + deltaWFrac).coerceIn(0.45f, 1f)
                      latestOnResize(localHeightDp, localWidthFrac)
                    }
                  }
                } finally {
                  isActivelyResizing = false
                  latestOnGestureLock(false)
                }
              }
            }
            .testTag("doodle_corner_resize_handle"),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(22.dp)
              .clip(CircleShape)
              .background(colors.accentSoft)
              .border(2.dp, colors.accent, CircleShape)
          )
        }
      }
    }
  }
}

/**
 * Ultra-slim Drawing Toolbar:
 * - Default state: ONLY a single compact icon row (Pen, Fine, Mark, Erase, Undo, Redo, Close ×).
 * - Tapping an already-selected brush tool opens a compact floating popover right above the bar
 *   with the Stroke Size Slider (px preview) and Color Swatches (Erase shows only Size Slider).
 * - Auto-collapses when the user draws on canvas or taps outside.
 */
@Composable
fun FloatingDrawingToolbar(
  strokePalette: List<Color>,
  selectedColorIndex: Int,
  selectedTool: EditorDrawTool,
  strokeSizeDp: Float,
  isPopoverExpanded: Boolean,
  onTogglePopover: (Boolean) -> Unit,
  onStrokeSizeChange: (Float) -> Unit,
  canUndo: Boolean,
  canRedo: Boolean,
  onSelectColor: (Int) -> Unit,
  onSelectTool: (EditorDrawTool) -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onClose: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  val reduceMotion = VeilThemeTokens.reduceMotion
  val toolbarShape = RoundedCornerShape(26.dp)
  val popoverShape = RoundedCornerShape(22.dp)
  val activeInkColor = strokePalette.getOrElse(selectedColorIndex) { colors.accent }
  val previewDotColor = if (selectedTool == EditorDrawTool.ERASER) colors.muted else activeInkColor

  fun onBrushToolTap(tool: EditorDrawTool) {
    if (selectedTool == tool) {
      onTogglePopover(!isPopoverExpanded)
    } else {
      onSelectTool(tool)
      onTogglePopover(false)
    }
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Floating On-Tap Popover (hidden by default!)
    AnimatedVisibility(
      visible = isPopoverExpanded,
      enter = fadeIn(tween(if (reduceMotion) 0 else 160)) + slideInVertically(
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
      ) { 22 } + scaleIn(initialScale = 0.96f),
      exit = fadeOut(tween(if (reduceMotion) 0 else 120)) + slideOutVertically { 16 }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(12.dp, popoverShape, ambientColor = colors.toolbarShadow, spotColor = colors.toolbarShadow)
          .clip(popoverShape)
          .background(colors.surface.copy(alpha = 0.97f))
          .border(1.dp, colors.hiddenBorder.copy(alpha = 0.45f), popoverShape)
          .padding(horizontal = 14.dp, vertical = 12.dp)
          .testTag("drawing_brush_popover"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Stroke Size Slider with live animated px preview
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier.size(26.dp),
            contentAlignment = Alignment.Center
          ) {
            val norm = ((strokeSizeDp - selectedTool.minSize) / (selectedTool.maxSize - selectedTool.minSize)).coerceIn(0f, 1f)
            val dotDp = (5f + norm * 16f).dp
            Box(
              modifier = Modifier
                .size(dotDp)
                .clip(CircleShape)
                .background(previewDotColor.copy(alpha = if (selectedTool == EditorDrawTool.HIGHLIGHTER) 0.55f else 1f))
                .then(
                  if (selectedTool == EditorDrawTool.ERASER) {
                    Modifier.border(1.5.dp, colors.text, CircleShape)
                  } else {
                    Modifier
                  }
                )
            )
          }

          Slider(
            value = strokeSizeDp.coerceIn(selectedTool.minSize, selectedTool.maxSize),
            onValueChange = onStrokeSizeChange,
            valueRange = selectedTool.minSize..selectedTool.maxSize,
            colors = SliderDefaults.colors(
              thumbColor = colors.accent,
              activeTrackColor = colors.accent,
              inactiveTrackColor = colors.accentSoft
            ),
            modifier = Modifier
              .weight(1f)
              .height(28.dp)
              .testTag("drawing_stroke_size_slider")
          )

          Text(
            text = "${strokeSizeDp.toInt().coerceAtLeast(1)}px",
            style = VeilTextStyles.labelChip,
            color = colors.muted
          )
        }

        // Color Swatches (hidden when Erase is selected — Erase shows only Size Slider)
        if (selectedTool != EditorDrawTool.ERASER) {
          LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            itemsIndexed(strokePalette) { idx, dotColor ->
              val isSelected = selectedColorIndex == idx
              Box(
                modifier = Modifier
                  .size(30.dp)
                  .clip(CircleShape)
                  .then(if (isSelected) Modifier.border(2.5.dp, colors.accent, CircleShape) else Modifier)
                  .tactilePress(scaleDown = 0.86f, hapticOnClick = true) { onSelectColor(idx) },
                contentAlignment = Alignment.Center
              ) {
                Box(
                  modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(0.8.dp, colors.hiddenBorder.copy(alpha = 0.6f), CircleShape)
                )
              }
            }
          }
        }
      }
    }

    // Ultra-Compact Single Icon Row ONLY: Pen, Fine, Mark, Erase, Undo, Redo, Close ×
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(
          elevation = 10.dp,
          shape = toolbarShape,
          ambientColor = colors.toolbarShadow,
          spotColor = colors.toolbarShadow
        )
        .clip(toolbarShape)
        .background(colors.surface.copy(alpha = 0.97f))
        .border(1.dp, colors.hiddenBorder.copy(alpha = 0.38f), toolbarShape)
        .padding(horizontal = 8.dp, vertical = 6.dp)
        .testTag("slim_drawing_toolbar"),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      SlimDrawToolIcon(
        description = "Pen",
        selected = selectedTool == EditorDrawTool.PEN,
        indicatorDot = if (selectedTool == EditorDrawTool.PEN) activeInkColor else null,
        onClick = { onBrushToolTap(EditorDrawTool.PEN) },
        tag = "draw_tool_pen"
      ) { tint -> VeilIcons.Pen(tint = tint, size = 19.dp) }

      SlimDrawToolIcon(
        description = "Fine pen",
        selected = selectedTool == EditorDrawTool.FOUNTAIN_PEN,
        indicatorDot = if (selectedTool == EditorDrawTool.FOUNTAIN_PEN) activeInkColor else null,
        onClick = { onBrushToolTap(EditorDrawTool.FOUNTAIN_PEN) },
        tag = "draw_tool_fine"
      ) { tint -> VeilIcons.FountainPen(tint = tint, size = 19.dp) }

      SlimDrawToolIcon(
        description = "Highlighter",
        selected = selectedTool == EditorDrawTool.HIGHLIGHTER,
        indicatorDot = if (selectedTool == EditorDrawTool.HIGHLIGHTER) activeInkColor else null,
        onClick = { onBrushToolTap(EditorDrawTool.HIGHLIGHTER) },
        tag = "draw_tool_mark"
      ) { tint -> VeilIcons.Highlighter(tint = tint, size = 19.dp) }

      SlimDrawToolIcon(
        description = "Eraser",
        selected = selectedTool == EditorDrawTool.ERASER,
        indicatorDot = null,
        onClick = { onBrushToolTap(EditorDrawTool.ERASER) },
        tag = "draw_tool_erase"
      ) { tint -> VeilIcons.Eraser(tint = tint, size = 19.dp) }

      SlimDrawToolIcon(
        description = "Undo stroke",
        selected = false,
        enabled = canUndo,
        indicatorDot = null,
        onClick = {
          onTogglePopover(false)
          onUndo()
        },
        tag = "draw_tool_undo"
      ) { tint -> VeilIcons.Undo(tint = tint, size = 19.dp) }

      SlimDrawToolIcon(
        description = "Redo stroke",
        selected = false,
        enabled = canRedo,
        indicatorDot = null,
        onClick = {
          onTogglePopover(false)
          onRedo()
        },
        tag = "draw_tool_redo"
      ) { tint -> VeilIcons.Redo(tint = tint, size = 19.dp) }

      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(colors.accentSoft)
          .tactilePress(scaleDown = 0.88f, hapticOnClick = true) {
            onTogglePopover(false)
            onClose()
          }
          .semantics { contentDescription = "Close drawing toolbar" }
          .testTag("close_draw_toolbar_button"),
        contentAlignment = Alignment.Center
      ) {
        VeilIcons.Close(tint = colors.accent, size = 16.dp)
      }
    }
  }
}

@Composable
private fun SlimDrawToolIcon(
  description: String,
  selected: Boolean,
  enabled: Boolean = true,
  indicatorDot: Color? = null,
  onClick: () -> Unit,
  tag: String,
  icon: @Composable (Color) -> Unit
) {
  val colors = VeilThemeTokens.colors
  val tint = when {
    !enabled -> colors.muted.copy(alpha = 0.35f)
    selected -> colors.accent
    else -> colors.text
  }

  Box(
    modifier = Modifier
      .size(40.dp)
      .clip(CircleShape)
      .background(if (selected) colors.accentSoft else Color.Transparent)
      .tactilePress(scaleDown = 0.88f, enabled = enabled, hapticOnClick = true, onClick = onClick)
      .semantics { contentDescription = description }
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      icon(tint)
      if (indicatorDot != null) {
        Spacer(modifier = Modifier.height(2.dp))
        Box(
          modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(indicatorDot)
        )
      }
    }
  }
}

fun DrawScope.drawNormalizedStrokes(
  strokes: List<DoodleStrokeData>,
  palette: List<Color>,
  eraserColor: Color,
  widthScale: Float = 1f
) {
  val w = size.width
  val h = size.height
  strokes.forEach { s ->
    val strokeColor = if (s.colorIndex < 0) {
      eraserColor
    } else {
      palette.getOrElse(s.colorIndex) { palette.first() }.copy(alpha = s.alpha)
    }
    val basePx = (s.widthDp * widthScale).coerceAtLeast(1.1f).dp.toPx()

    if (s.points.size == 1) {
      val pt = s.points.first()
      drawCircle(color = strokeColor, radius = basePx / 2f, center = Offset(pt.first * w, pt.second * h))
    } else if (s.points.size > 1) {
      if (s.brushType == EditorDrawTool.FOUNTAIN_PEN.brushTypeId && s.colorIndex >= 0) {
        for (i in 1 until s.points.size) {
          val p0 = Offset(s.points[i - 1].first * w, s.points[i - 1].second * h)
          val p1 = Offset(s.points[i].first * w, s.points[i].second * h)
          val dist = hypot((p1.x - p0.x).toDouble(), (p1.y - p0.y).toDouble()).toFloat()
          val speedFactor = (1.18f - (dist / 42f).coerceIn(0f, 0.58f))
          drawLine(
            color = strokeColor,
            start = p0,
            end = p1,
            strokeWidth = (basePx * speedFactor).coerceAtLeast(1f),
            cap = StrokeCap.Round
          )
        }
      } else {
        val path = Path().apply {
          moveTo(s.points[0].first * w, s.points[0].second * h)
          for (i in 1 until s.points.size) {
            lineTo(s.points[i].first * w, s.points[i].second * h)
          }
        }
        drawPath(
          path = path,
          color = strokeColor,
          style = Stroke(width = basePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
      }
    }
  }
}
