package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NoteColorOrder
import com.example.ui.theme.NotePatternOrder
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens
import kotlin.math.PI
import kotlin.math.sin

/**
 * Spring-based tactile press modifier so every button, swatch, and card feels plush and responsive.
 */
fun Modifier.tactilePress(
  scaleDown: Float = 0.94f,
  enabled: Boolean = true,
  hapticOnClick: Boolean = false,
  role: Role? = Role.Button,
  onClick: () -> Unit
): Modifier = composed {
  val reduceMotion = VeilThemeTokens.reduceMotion
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed && !reduceMotion) scaleDown else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "tactile_press_scale"
  )
  this
    .graphicsLayer {
      scaleX = scale
      scaleY = scale
    }
    .clickable(
      enabled = enabled,
      interactionSource = interactionSource,
      indication = ripple(),
      role = role,
      onClick = {
        if (hapticOnClick) {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        onClick()
      }
    )
}

/**
 * Subtle aesthetic note background patterns drawn crisply via Canvas with zero image overhead.
 */
@Composable
fun NoteBackgroundPatternOverlay(
  pattern: String,
  tint: Color,
  modifier: Modifier = Modifier,
  alphaScale: Float = 1f
) {
  if (pattern.isBlank() || pattern == "none") return

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@Canvas

    val baseAlpha = (0.11f * alphaScale).coerceIn(0.03f, 0.25f)
    val lineColor = tint.copy(alpha = baseAlpha)

    when (pattern.lowercase()) {
      "dots" -> {
        val spacing = 20.dp.toPx()
        val r = 1.25.dp.toPx()
        var y = spacing * 0.6f
        while (y < h) {
          var x = spacing * 0.6f
          while (x < w) {
            drawCircle(color = lineColor, radius = r, center = Offset(x, y))
            x += spacing
          }
          y += spacing
        }
      }

      "grid" -> {
        val spacing = 22.dp.toPx()
        val strokeW = 0.85.dp.toPx()
        var x = spacing
        while (x < w) {
          drawLine(lineColor, Offset(x, 0f), Offset(x, h), strokeWidth = strokeW)
          x += spacing
        }
        var y = spacing
        while (y < h) {
          drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = strokeW)
          y += spacing
        }
      }

      "lines" -> {
        val spacing = 26.dp.toPx()
        val strokeW = 0.9.dp.toPx()
        var y = spacing
        while (y < h) {
          drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = strokeW)
          y += spacing
        }
      }

      "waves" -> {
        val waveSpacing = 34.dp.toPx()
        val amplitude = 6.dp.toPx()
        val stroke = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round)
        var baseY = waveSpacing * 0.8f
        while (baseY < h + waveSpacing) {
          val path = Path()
          var x = 0f
          val step = 12.dp.toPx()
          while (x <= w + step) {
            val y = baseY + (sin((x / 90f) * PI).toFloat() * amplitude)
            if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
            x += step
          }
          drawPath(path, lineColor, style = stroke)
          baseY += waveSpacing
        }
      }

      "stars" -> {
        val cell = 44.dp.toPx()
        val strokeW = 1.dp.toPx()
        var row = 0
        var y = cell * 0.55f
        while (y < h) {
          var col = 0
          var x = if (row % 2 == 0) cell * 0.45f else cell * 0.95f
          while (x < w) {
            val arm = if ((row + col) % 3 == 0) 4.5.dp.toPx() else 2.5.dp.toPx()
            drawLine(lineColor, Offset(x - arm, y), Offset(x + arm, y), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(lineColor, Offset(x, y - arm), Offset(x, y + arm), strokeWidth = strokeW, cap = StrokeCap.Round)
            x += cell
            col++
          }
          y += cell
          row++
        }
      }

      "botanical" -> {
        val cell = 54.dp.toPx()
        val stroke = Stroke(width = 1.05.dp.toPx(), cap = StrokeCap.Round)
        var row = 0
        var y = cell * 0.6f
        while (y < h + cell) {
          var x = if (row % 2 == 0) cell * 0.5f else cell
          while (x < w + cell) {
            val leaf = Path().apply {
              moveTo(x - 10.dp.toPx(), y + 8.dp.toPx())
              quadraticTo(x - 12.dp.toPx(), y - 8.dp.toPx(), x + 10.dp.toPx(), y - 10.dp.toPx())
              quadraticTo(x + 12.dp.toPx(), y + 8.dp.toPx(), x - 10.dp.toPx(), y + 8.dp.toPx())
            }
            drawPath(leaf, lineColor, style = stroke)
            drawLine(
              lineColor,
              Offset(x - 10.dp.toPx(), y + 8.dp.toPx()),
              Offset(x + 6.dp.toPx(), y - 6.dp.toPx()),
              strokeWidth = 0.85.dp.toPx(),
              cap = StrokeCap.Round
            )
            x += cell
          }
          y += cell
          row++
        }
      }
    }
  }
}

fun patternDisplayName(key: String): String = when (key.lowercase()) {
  "dots" -> "Dots"
  "grid" -> "Grid"
  "lines" -> "Ruled"
  "waves" -> "Waves"
  "stars" -> "Cosmos"
  "botanical" -> "Botanical"
  else -> "Plain"
}

/**
 * Compact two-row Color & Background Pattern Picker shared by Note Editor and Home Selection Bar.
 */
@Composable
fun NoteColorAndPatternPickerContent(
  selectedColor: String,
  selectedPattern: String,
  onSelectColor: (String) -> Unit,
  onSelectPattern: (String) -> Unit
) {
  val colors = VeilThemeTokens.colors
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Text(
      text = "COLOR",
      style = VeilTextStyles.sectionLabel,
      color = colors.muted,
      modifier = Modifier.padding(horizontal = 20.dp)
    )
    LazyRow(
      contentPadding = PaddingValues(horizontal = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      items(NoteColorOrder, key = { it }) { colKey ->
        val swatch = colors.noteCardColor(colKey)
        val isSelected = selectedColor == colKey || (colKey == "plain" && selectedColor == "white")
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(swatch)
            .border(
              width = if (isSelected) 2.5.dp else 1.dp,
              color = if (isSelected) colors.accent else colors.hiddenBorder,
              shape = CircleShape
            )
            .tactilePress(scaleDown = 0.88f, hapticOnClick = true) {
              onSelectColor(colKey)
            }
            .testTag("color_swatch_$colKey"),
          contentAlignment = Alignment.Center
        ) {
          if (isSelected) {
            VeilIcons.CheckOutline(tint = colors.accent, size = 16.dp)
          }
        }
      }
    }

    Text(
      text = "BACKGROUND",
      style = VeilTextStyles.sectionLabel,
      color = colors.muted,
      modifier = Modifier.padding(horizontal = 20.dp)
    )
    LazyRow(
      contentPadding = PaddingValues(horizontal = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      items(NotePatternOrder, key = { it }) { patKey ->
        val isSelected = selectedPattern == patKey
        val previewBg = colors.noteCardColor(selectedColor)
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Box(
            modifier = Modifier
              .size(width = 56.dp, height = 46.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(previewBg)
              .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) colors.accent else colors.hiddenBorder,
                shape = RoundedCornerShape(14.dp)
              )
              .tactilePress(scaleDown = 0.92f, hapticOnClick = true) {
                onSelectPattern(patKey)
              }
              .testTag("pattern_swatch_$patKey"),
            contentAlignment = Alignment.Center
          ) {
            NoteBackgroundPatternOverlay(
              pattern = patKey,
              tint = colors.text,
              alphaScale = 1.9f,
              modifier = Modifier.fillMaxSize()
            )
            if (isSelected) {
              VeilIcons.CheckOutline(tint = colors.accent, size = 16.dp)
            }
          }
          Text(
            text = patternDisplayName(patKey),
            style = VeilTextStyles.labelChip,
            color = if (isSelected) colors.accent else colors.muted
          )
        }
      }
    }
  }
}
