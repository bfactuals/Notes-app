package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.VeilThemeTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Minimalist 2px-stroke outline icons with rounded line caps for Veil.
 * Strictly outline style — no filled icons.
 */
object VeilIcons {

  @Composable
  fun SunMoonMorph(
    isDark: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
  ) {
    val reduceMotion = VeilThemeTokens.reduceMotion
    val progress by animateFloatAsState(
      targetValue = if (isDark) 1f else 0f,
      animationSpec = tween(
        durationMillis = if (reduceMotion) 0 else 400,
        easing = FastOutSlowInEasing
      ),
      label = "sun_moon_morph"
    )

    // Offscreen compositing allows clean crescent cutout while morphing
    Canvas(
      modifier = modifier
        .size(size)
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
      val cx = this.size.width / 2f
      val cy = this.size.height / 2f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)

      // Sun rotates 180deg over 400ms
      rotate(degrees = 180f * progress, pivot = Offset(cx, cy)) {
        // 8 short rays scale down to 0
        val rayScale = (1f - progress * 1.4f).coerceIn(0f, 1f)
        if (rayScale > 0.01f) {
          val innerR = 8.5.dp.toPx()
          val rayLen = 2.2.dp.toPx() * rayScale
          for (i in 0 until 8) {
            val angle = (i * 45.0) * PI / 180.0
            val sx = cx + (innerR * cos(angle)).toFloat()
            val sy = cy + (innerR * sin(angle)).toFloat()
            val ex = cx + ((innerR + rayLen) * cos(angle)).toFloat()
            val ey = cy + ((innerR + rayLen) * sin(angle)).toFloat()
            drawLine(
              color = tint,
              start = Offset(sx, sy),
              end = Offset(ex, ey),
              strokeWidth = strokePx,
              cap = StrokeCap.Round
            )
          }
        }

        // Core circle: 6dp radius in sun state, slightly larger 7.5dp radius in moon state
        val circleRadius = (6f + 1.5f * progress).dp.toPx()
        drawCircle(
          color = tint,
          radius = circleRadius,
          center = Offset(cx, cy),
          style = stroke
        )

        // Crescent cutout & inner arc morphing in during second half of transition
        if (progress > 0.15f) {
          val cutProgress = ((progress - 0.15f) / 0.85f).coerceIn(0f, 1f)
          val cutRadius = circleRadius * 0.85f
          // Offset approaches upper-left (which after 180deg rotation is lower-right / classic crescent)
          val offsetDist = (circleRadius * 1.9f) - (circleRadius * 1.25f * cutProgress)
          val cutCenter = Offset(
            x = cx - offsetDist * 0.72f,
            y = cy + offsetDist * 0.72f
          )

          // Clear the overlapping outer ring section to form the crescent opening
          drawCircle(
            color = Color.Black,
            radius = cutRadius,
            center = cutCenter,
            blendMode = BlendMode.Clear
          )

          // Draw the crescent inner outline stroke clipped inside the main circle
          val outerPath = Path().apply {
            addOval(
              Rect(
                left = cx - circleRadius,
                top = cy - circleRadius,
                right = cx + circleRadius,
                bottom = cy + circleRadius
              )
            )
          }
          val innerPath = Path().apply {
            addOval(
              Rect(
                left = cutCenter.x - cutRadius,
                top = cutCenter.y - cutRadius,
                right = cutCenter.x + cutRadius,
                bottom = cutCenter.y + cutRadius
              )
            )
          }
          val crescentOutline = Path.combine(
            androidx.compose.ui.graphics.PathOperation.Difference,
            outerPath,
            innerPath
          )
          drawPath(
            path = crescentOutline,
            color = tint,
            style = stroke
          )
        }
      }
    }
  }

  @Composable
  fun Search(tint: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawCircle(color = tint, radius = 7.5f * s, center = Offset(10.5f * s, 10.5f * s), style = stroke)
      drawLine(
        color = tint,
        start = Offset(16.2f * s, 16.2f * s),
        end = Offset(20.5f * s, 20.5f * s),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
      )
    }
  }

  @Composable
  fun Plus(tint: Color, modifier: Modifier = Modifier, size: Dp = 26.dp) {
    Canvas(modifier = modifier.size(size)) {
      val w = this.size.width
      val h = this.size.height
      val strokePx = 2.2.dp.toPx()
      val pad = w * 0.22f
      drawLine(tint, Offset(w / 2f, pad), Offset(w / 2f, h - pad), strokeWidth = strokePx, cap = StrokeCap.Round)
      drawLine(tint, Offset(pad, h / 2f), Offset(w - pad, h / 2f), strokeWidth = strokePx, cap = StrokeCap.Round)
    }
  }

  @Composable
  fun Close(tint: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      drawLine(tint, Offset(6f * s, 6f * s), Offset(18f * s, 18f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      drawLine(tint, Offset(18f * s, 6f * s), Offset(6f * s, 18f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
    }
  }

  @Composable
  fun ArrowBack(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawLine(tint, Offset(19f * s, 12f * s), Offset(5f * s, 12f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      val path = Path().apply {
        moveTo(11f * s, 6f * s)
        lineTo(5f * s, 12f * s)
        lineTo(11f * s, 18f * s)
      }
      drawPath(path, tint, style = stroke)
    }
  }

  @Composable
  fun Checklist(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawRoundRect(
        color = tint,
        topLeft = Offset(3.5f * s, 3.5f * s),
        size = Size(17f * s, 17f * s),
        cornerRadius = CornerRadius(4f * s, 4f * s),
        style = stroke
      )
      val check = Path().apply {
        moveTo(8f * s, 12.2f * s)
        lineTo(10.8f * s, 15f * s)
        lineTo(16.2f * s, 9.2f * s)
      }
      drawPath(check, tint, style = stroke)
    }
  }

  @Composable
  fun Pen(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      val path = Path().apply {
        moveTo(16.5f * s, 3.5f * s)
        lineTo(20.5f * s, 7.5f * s)
        lineTo(8.5f * s, 19.5f * s)
        lineTo(3.5f * s, 20.5f * s)
        lineTo(4.5f * s, 15.5f * s)
        close()
      }
      drawPath(path, tint, style = stroke)
      drawLine(tint, Offset(13.8f * s, 6.2f * s), Offset(17.8f * s, 10.2f * s), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    }
  }

  @Composable
  fun Mic(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawRoundRect(
        color = tint,
        topLeft = Offset(9f * s, 3f * s),
        size = Size(6f * s, 11f * s),
        cornerRadius = CornerRadius(3f * s, 3f * s),
        style = stroke
      )
      drawArc(
        color = tint,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(5.5f * s, 6f * s),
        size = Size(13f * s, 11.5f * s),
        style = stroke
      )
      drawLine(tint, Offset(12f * s, 17.5f * s), Offset(12f * s, 21f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      drawLine(tint, Offset(8.5f * s, 21f * s), Offset(15.5f * s, 21f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
    }
  }

  @Composable
  fun Pin(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      val path = Path().apply {
        moveTo(14.5f * s, 3.5f * s)
        lineTo(20.5f * s, 9.5f * s)
        lineTo(18f * s, 11f * s)
        lineTo(14.5f * s, 14.5f * s)
        lineTo(15.5f * s, 17f * s)
        lineTo(7f * s, 8.5f * s)
        lineTo(9.5f * s, 9.5f * s)
        lineTo(13f * s, 6f * s)
        close()
      }
      drawPath(path, tint, style = stroke)
      drawLine(tint, Offset(10.5f * s, 13.5f * s), Offset(4.5f * s, 19.5f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
    }
  }

  @Composable
  fun EyeOff(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      val upper = Path().apply {
        moveTo(3f * s, 12f * s)
        cubicTo(5.5f * s, 7f * s, 9f * s, 5.5f * s, 12f * s, 5.5f * s)
        cubicTo(15f * s, 5.5f * s, 18.5f * s, 7f * s, 21f * s, 12f * s)
      }
      val lower = Path().apply {
        moveTo(3f * s, 12f * s)
        cubicTo(5.5f * s, 17f * s, 9f * s, 18.5f * s, 12f * s, 18.5f * s)
        cubicTo(15f * s, 18.5f * s, 18.5f * s, 17f * s, 21f * s, 12f * s)
      }
      drawPath(upper, tint, style = stroke)
      drawPath(lower, tint, style = stroke)
      drawCircle(tint, radius = 2.8f * s, center = Offset(12f * s, 12f * s), style = stroke)
      drawLine(tint, Offset(4f * s, 4f * s), Offset(20f * s, 20f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
    }
  }

  @Composable
  fun Eye(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      val upper = Path().apply {
        moveTo(2.5f * s, 12f * s)
        cubicTo(5.2f * s, 7f * s, 8.8f * s, 5.5f * s, 12f * s, 5.5f * s)
        cubicTo(15.2f * s, 5.5f * s, 18.8f * s, 7f * s, 21.5f * s, 12f * s)
      }
      val lower = Path().apply {
        moveTo(2.5f * s, 12f * s)
        cubicTo(5.2f * s, 17f * s, 8.8f * s, 18.5f * s, 12f * s, 18.5f * s)
        cubicTo(15.2f * s, 18.5f * s, 18.8f * s, 17f * s, 21.5f * s, 12f * s)
      }
      drawPath(upper, tint, style = stroke)
      drawPath(lower, tint, style = stroke)
      drawCircle(tint, radius = 3f * s, center = Offset(12f * s, 12f * s), style = stroke)
    }
  }

  @Composable
  fun Highlighter(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      val body = Path().apply {
        moveTo(9f * s, 11f * s)
        lineTo(15f * s, 5f * s)
        lineTo(19f * s, 9f * s)
        lineTo(13f * s, 15f * s)
        close()
      }
      drawPath(body, tint, style = stroke)
      val tip = Path().apply {
        moveTo(9f * s, 11f * s)
        lineTo(5f * s, 15f * s)
        lineTo(6.5f * s, 17.5f * s)
        lineTo(9f * s, 19f * s)
        lineTo(13f * s, 15f * s)
      }
      drawPath(tip, tint, style = stroke)
      drawLine(tint, Offset(4f * s, 20.5f * s), Offset(18f * s, 20.5f * s), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    }
  }

  @Composable
  fun Eraser(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      val path = Path().apply {
        moveTo(7f * s, 20f * s)
        lineTo(3.5f * s, 16.5f * s)
        lineTo(13.5f * s, 6.5f * s)
        lineTo(19.5f * s, 12.5f * s)
        lineTo(12f * s, 20f * s)
        close()
      }
      drawPath(path, tint, style = stroke)
      drawLine(tint, Offset(9.5f * s, 10.5f * s), Offset(15.5f * s, 16.5f * s), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
      drawLine(tint, Offset(12f * s, 20f * s), Offset(20.5f * s, 20f * s), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    }
  }

  @Composable
  fun Undo(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      val arrow = Path().apply {
        moveTo(8f * s, 6f * s)
        lineTo(4f * s, 10f * s)
        lineTo(8f * s, 14f * s)
      }
      drawPath(arrow, tint, style = stroke)
      val curve = Path().apply {
        moveTo(4.5f * s, 10f * s)
        lineTo(14.5f * s, 10f * s)
        cubicTo(18.5f * s, 10f * s, 20f * s, 12.5f * s, 20f * s, 15f * s)
        cubicTo(20f * s, 17.5f * s, 18.2f * s, 19f * s, 15f * s, 19f * s)
        lineTo(11f * s, 19f * s)
      }
      drawPath(curve, tint, style = stroke)
    }
  }

  @Composable
  fun Redo(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      val arrow = Path().apply {
        moveTo(16f * s, 6f * s)
        lineTo(20f * s, 10f * s)
        lineTo(16f * s, 14f * s)
      }
      drawPath(arrow, tint, style = stroke)
      val curve = Path().apply {
        moveTo(19.5f * s, 10f * s)
        lineTo(9.5f * s, 10f * s)
        cubicTo(5.5f * s, 10f * s, 4f * s, 12.5f * s, 4f * s, 15f * s)
        cubicTo(4f * s, 17.5f * s, 5.8f * s, 19f * s, 9f * s, 19f * s)
        lineTo(13f * s, 19f * s)
      }
      drawPath(curve, tint, style = stroke)
    }
  }

  @Composable
  fun ChecklistCircle(checked: Boolean, tint: Color, accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
      val strokePx = 1.6.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      val r = size.minDimension / 2f - strokePx
      drawCircle(
        color = if (checked) accent else tint.copy(alpha = 0.55f),
        radius = r,
        center = center,
        style = stroke
      )
      if (checked) {
        val s = size.width / 16f
        val check = Path().apply {
          moveTo(4.8f * s, 8.2f * s)
          lineTo(7.1f * s, 10.5f * s)
          lineTo(11.4f * s, 5.8f * s)
        }
        drawPath(check, accent, style = stroke)
      }
    }
  }

  @Composable
  fun Trash(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawLine(tint, Offset(4f * s, 7f * s), Offset(20f * s, 7f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      val handle = Path().apply {
        moveTo(9f * s, 7f * s)
        lineTo(9f * s, 4.5f * s)
        lineTo(15f * s, 4.5f * s)
        lineTo(15f * s, 7f * s)
      }
      drawPath(handle, tint, style = stroke)
      val bin = Path().apply {
        moveTo(6f * s, 7f * s)
        lineTo(7.2f * s, 19.5f * s)
        lineTo(16.8f * s, 19.5f * s)
        lineTo(18f * s, 7f * s)
      }
      drawPath(bin, tint, style = stroke)
    }
  }

  @Composable
  fun Archive(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawRoundRect(tint, Offset(3f * s, 4f * s), Size(18f * s, 4.5f * s), CornerRadius(1.5f * s, 1.5f * s), style = stroke)
      val box = Path().apply {
        moveTo(4.5f * s, 8.5f * s)
        lineTo(4.5f * s, 18.5f * s)
        lineTo(19.5f * s, 18.5f * s)
        lineTo(19.5f * s, 8.5f * s)
      }
      drawPath(box, tint, style = stroke)
      drawLine(tint, Offset(10f * s, 12.5f * s), Offset(14f * s, 12.5f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
    }
  }

  @Composable
  fun SettingsGear(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawCircle(tint, radius = 3.2f * s, center = Offset(12f * s, 12f * s), style = stroke)
      drawCircle(tint, radius = 7.2f * s, center = Offset(12f * s, 12f * s), style = stroke)
      for (i in 0 until 6) {
        val angle = (i * 60.0) * PI / 180.0
        val sx = 12f * s + (7.2f * s * cos(angle)).toFloat()
        val sy = 12f * s + (7.2f * s * sin(angle)).toFloat()
        val ex = 12f * s + (9.6f * s * cos(angle)).toFloat()
        val ey = 12f * s + (9.6f * s * sin(angle)).toFloat()
        drawLine(tint, Offset(sx, sy), Offset(ex, ey), strokeWidth = strokePx, cap = StrokeCap.Round)
      }
    }
  }

  @Composable
  fun NoteDoc(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawRoundRect(tint, Offset(5f * s, 3.5f * s), Size(14f * s, 17f * s), CornerRadius(3f * s, 3f * s), style = stroke)
      drawLine(tint, Offset(8.5f * s, 9f * s), Offset(15.5f * s, 9f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      drawLine(tint, Offset(8.5f * s, 13f * s), Offset(13.5f * s, 13f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
    }
  }

  @Composable
  fun Bell(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      val bell = Path().apply {
        moveTo(18f * s, 16f * s)
        lineTo(6f * s, 16f * s)
        lineTo(7.2f * s, 13.5f * s)
        lineTo(7.2f * s, 10f * s)
        cubicTo(7.2f * s, 6.8f * s, 9.3f * s, 4.5f * s, 12f * s, 4.5f * s)
        cubicTo(14.7f * s, 4.5f * s, 16.8f * s, 6.8f * s, 16.8f * s, 10f * s)
        lineTo(16.8f * s, 13.5f * s)
        close()
      }
      drawPath(bell, tint, style = stroke)
      drawArc(tint, 0f, 180f, false, Offset(10f * s, 17f * s), Size(4f * s, 3f * s), style = stroke)
    }
  }

  @Composable
  fun Tag(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      val path = Path().apply {
        moveTo(4f * s, 4f * s)
        lineTo(11.5f * s, 4f * s)
        lineTo(20f * s, 12.5f * s)
        lineTo(12.5f * s, 20f * s)
        lineTo(4f * s, 11.5f * s)
        close()
      }
      drawPath(path, tint, style = stroke)
      drawCircle(tint, radius = 1.2f * s, center = Offset(8.5f * s, 8.5f * s), style = stroke)
    }
  }

  @Composable
  fun LockOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawRoundRect(tint, Offset(5f * s, 11f * s), Size(14f * s, 9.5f * s), CornerRadius(3f * s, 3f * s), style = stroke)
      val shackle = Path().apply {
        moveTo(8.5f * s, 11f * s)
        lineTo(8.5f * s, 7.5f * s)
        cubicTo(8.5f * s, 5.2f * s, 10f * s, 4f * s, 12f * s, 4f * s)
        cubicTo(14f * s, 4f * s, 15.5f * s, 5.2f * s, 15.5f * s, 7.5f * s)
        lineTo(15.5f * s, 11f * s)
      }
      drawPath(shackle, tint, style = stroke)
    }
  }

  @Composable
  fun PaletteOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawCircle(tint, radius = 8.5f * s, center = Offset(12f * s, 12f * s), style = stroke)
      drawCircle(tint, radius = 1.3f * s, center = Offset(9f * s, 9.5f * s), style = stroke)
      drawCircle(tint, radius = 1.3f * s, center = Offset(14.5f * s, 9f * s), style = stroke)
      drawCircle(tint, radius = 1.3f * s, center = Offset(15.5f * s, 14f * s), style = stroke)
    }
  }

  @Composable
  fun GridOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawRoundRect(tint, Offset(4f * s, 4f * s), Size(6.5f * s, 6.5f * s), CornerRadius(1.5f * s, 1.5f * s), style = stroke)
      drawRoundRect(tint, Offset(13.5f * s, 4f * s), Size(6.5f * s, 6.5f * s), CornerRadius(1.5f * s, 1.5f * s), style = stroke)
      drawRoundRect(tint, Offset(4f * s, 13.5f * s), Size(6.5f * s, 6.5f * s), CornerRadius(1.5f * s, 1.5f * s), style = stroke)
      drawRoundRect(tint, Offset(13.5f * s, 13.5f * s), Size(6.5f * s, 6.5f * s), CornerRadius(1.5f * s, 1.5f * s), style = stroke)
    }
  }

  @Composable
  fun RestoreOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawArc(tint, -140f, 290f, false, Offset(4.5f * s, 4.5f * s), Size(15f * s, 15f * s), style = stroke)
      val head = Path().apply {
        moveTo(4.5f * s, 5.5f * s)
        lineTo(5f * s, 10f * s)
        lineTo(9.5f * s, 9.5f * s)
      }
      drawPath(head, tint, style = stroke)
    }
  }

  @Composable
  fun InfoOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawCircle(tint, radius = 8.5f * s, center = Offset(12f * s, 12f * s), style = stroke)
      drawLine(tint, Offset(12f * s, 11.5f * s), Offset(12f * s, 16f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      drawCircle(tint, radius = 0.8f * s, center = Offset(12f * s, 8.2f * s))
    }
  }

  @Composable
  fun BackupOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      val arrow = Path().apply {
        moveTo(12f * s, 4f * s)
        lineTo(12f * s, 14.5f * s)
        moveTo(8.5f * s, 11f * s)
        lineTo(12f * s, 14.5f * s)
        lineTo(15.5f * s, 11f * s)
      }
      drawPath(arrow, tint, style = stroke)
      val tray = Path().apply {
        moveTo(5f * s, 15.5f * s)
        lineTo(5f * s, 19f * s)
        lineTo(19f * s, 19f * s)
        lineTo(19f * s, 15.5f * s)
      }
      drawPath(tray, tint, style = stroke)
    }
  }

  @Composable
  fun ImportOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      val arrow = Path().apply {
        moveTo(12f * s, 14.5f * s)
        lineTo(12f * s, 4f * s)
        moveTo(8.5f * s, 7.5f * s)
        lineTo(12f * s, 4f * s)
        lineTo(15.5f * s, 7.5f * s)
      }
      drawPath(arrow, tint, style = stroke)
      val tray = Path().apply {
        moveTo(5f * s, 15.5f * s)
        lineTo(5f * s, 19f * s)
        lineTo(19f * s, 19f * s)
        lineTo(19f * s, 15.5f * s)
      }
      drawPath(tray, tint, style = stroke)
    }
  }

  @Composable
  fun MenuLines(tint: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      drawLine(tint, Offset(4f * s, 7f * s), Offset(20f * s, 7f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      drawLine(tint, Offset(4f * s, 12f * s), Offset(20f * s, 12f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      drawLine(tint, Offset(4f * s, 17f * s), Offset(15f * s, 17f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
    }
  }

  @Composable
  fun DrawBoxOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawRoundRect(
        color = tint,
        topLeft = Offset(3.5f * s, 5f * s),
        size = Size(17f * s, 14f * s),
        cornerRadius = CornerRadius(3f * s, 3f * s),
        style = stroke
      )
      val wave = Path().apply {
        moveTo(7f * s, 14f * s)
        cubicTo(9.5f * s, 9.5f * s, 12f * s, 15.5f * s, 16.5f * s, 10.5f * s)
      }
      drawPath(wave, tint, style = stroke)
    }
  }

  @Composable
  fun MoreVert(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val r = 1.6f * s
      drawCircle(tint, radius = r, center = Offset(12f * s, 6f * s))
      drawCircle(tint, radius = r, center = Offset(12f * s, 12f * s))
      drawCircle(tint, radius = r, center = Offset(12f * s, 18f * s))
    }
  }

  @Composable
  fun CameraOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      val body = Path().apply {
        moveTo(4f * s, 8f * s)
        lineTo(7.5f * s, 8f * s)
        lineTo(9.2f * s, 5.5f * s)
        lineTo(14.8f * s, 5.5f * s)
        lineTo(16.5f * s, 8f * s)
        lineTo(20f * s, 8f * s)
        lineTo(20f * s, 18.5f * s)
        lineTo(4f * s, 18.5f * s)
        close()
      }
      drawPath(body, tint, style = stroke)
      drawCircle(tint, radius = 3.2f * s, center = Offset(12f * s, 13f * s), style = stroke)
    }
  }

  @Composable
  fun ImageOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawRoundRect(
        color = tint,
        topLeft = Offset(3.5f * s, 4.5f * s),
        size = Size(17f * s, 15f * s),
        cornerRadius = CornerRadius(3f * s, 3f * s),
        style = stroke
      )
      drawCircle(tint, radius = 1.5f * s, center = Offset(8.5f * s, 9.2f * s), style = stroke)
      val mountain = Path().apply {
        moveTo(4.5f * s, 17f * s)
        lineTo(9.5f * s, 12.2f * s)
        lineTo(13.2f * s, 15.5f * s)
        lineTo(16f * s, 13f * s)
        lineTo(19.5f * s, 16.8f * s)
      }
      drawPath(mountain, tint, style = stroke)
    }
  }

  @Composable
  fun SortOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawLine(tint, Offset(7.5f * s, 5.5f * s), Offset(7.5f * s, 18.5f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      val up = Path().apply {
        moveTo(4.5f * s, 8.5f * s)
        lineTo(7.5f * s, 5.5f * s)
        lineTo(10.5f * s, 8.5f * s)
      }
      drawPath(up, tint, style = stroke)
      drawLine(tint, Offset(16.5f * s, 18.5f * s), Offset(16.5f * s, 5.5f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      val down = Path().apply {
        moveTo(13.5f * s, 15.5f * s)
        lineTo(16.5f * s, 18.5f * s)
        lineTo(19.5f * s, 15.5f * s)
      }
      drawPath(down, tint, style = stroke)
    }
  }

  @Composable
  fun CopyOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawRoundRect(
        color = tint,
        topLeft = Offset(8.5f * s, 8.5f * s),
        size = Size(11.5f * s, 11.5f * s),
        cornerRadius = CornerRadius(2.5f * s, 2.5f * s),
        style = stroke
      )
      val back = Path().apply {
        moveTo(15.5f * s, 8.5f * s)
        lineTo(15.5f * s, 6f * s)
        cubicTo(15.5f * s, 4.8f * s, 14.7f * s, 4f * s, 13.5f * s, 4f * s)
        lineTo(6f * s, 4f * s)
        cubicTo(4.8f * s, 4f * s, 4f * s, 4.8f * s, 4f * s, 6f * s)
        lineTo(4f * s, 13.5f * s)
        cubicTo(4f * s, 14.7f * s, 4.8f * s, 15.5f * s, 6f * s, 15.5f * s)
        lineTo(8.5f * s, 15.5f * s)
      }
      drawPath(back, tint, style = stroke)
    }
  }

  @Composable
  fun ShareOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawCircle(tint, radius = 2.4f * s, center = Offset(17.5f * s, 6f * s), style = stroke)
      drawCircle(tint, radius = 2.4f * s, center = Offset(6.5f * s, 12f * s), style = stroke)
      drawCircle(tint, radius = 2.4f * s, center = Offset(17.5f * s, 18f * s), style = stroke)
      drawLine(tint, Offset(8.6f * s, 10.8f * s), Offset(15.3f * s, 7.2f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      drawLine(tint, Offset(8.6f * s, 13.2f * s), Offset(15.3f * s, 16.8f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
    }
  }

  @Composable
  fun FountainPen(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      val nib = Path().apply {
        moveTo(12f * s, 3.5f * s)
        lineTo(17.5f * s, 11.5f * s)
        lineTo(14.5f * s, 19.5f * s)
        lineTo(9.5f * s, 19.5f * s)
        lineTo(6.5f * s, 11.5f * s)
        close()
      }
      drawPath(nib, tint, style = stroke)
      drawLine(tint, Offset(12f * s, 3.5f * s), Offset(12f * s, 11.2f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      drawCircle(tint, radius = 1.4f * s, center = Offset(12f * s, 12.6f * s), style = stroke)
    }
  }

  @Composable
  fun TextScanOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      // Corner brackets
      val tl = Path().apply { moveTo(4f * s, 8f * s); lineTo(4f * s, 4f * s); lineTo(8f * s, 4f * s) }
      val tr = Path().apply { moveTo(16f * s, 4f * s); lineTo(20f * s, 4f * s); lineTo(20f * s, 8f * s) }
      val bl = Path().apply { moveTo(4f * s, 16f * s); lineTo(4f * s, 20f * s); lineTo(8f * s, 20f * s) }
      val br = Path().apply { moveTo(16f * s, 20f * s); lineTo(20f * s, 20f * s); lineTo(20f * s, 16f * s) }
      drawPath(tl, tint, style = stroke)
      drawPath(tr, tint, style = stroke)
      drawPath(bl, tint, style = stroke)
      drawPath(br, tint, style = stroke)
      // Text lines inside
      drawLine(tint, Offset(8f * s, 9.5f * s), Offset(16f * s, 9.5f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      drawLine(tint, Offset(8f * s, 12.5f * s), Offset(16f * s, 12.5f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      drawLine(tint, Offset(8f * s, 15.5f * s), Offset(13f * s, 15.5f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
    }
  }

  @Composable
  fun CheckOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      val p = Path().apply {
        moveTo(5f * s, 12.5f * s)
        lineTo(10f * s, 17.2f * s)
        lineTo(19f * s, 7.2f * s)
      }
      drawPath(p, tint, style = stroke)
    }
  }

  @Composable
  fun ChevronUp(tint: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      val p = Path().apply {
        moveTo(6.5f * s, 14.5f * s)
        lineTo(12f * s, 9f * s)
        lineTo(17.5f * s, 14.5f * s)
      }
      drawPath(p, tint, style = stroke)
    }
  }

  @Composable
  fun ChevronDown(tint: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      val p = Path().apply {
        moveTo(6.5f * s, 9.5f * s)
        lineTo(12f * s, 15f * s)
        lineTo(17.5f * s, 9.5f * s)
      }
      drawPath(p, tint, style = stroke)
    }
  }

  @Composable
  fun ReadBookOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 21.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      val leftPage = Path().apply {
        moveTo(12f * s, 6.5f * s)
        cubicTo(10f * s, 5f * s, 6.5f * s, 4.5f * s, 3.5f * s, 5.5f * s)
        lineTo(3.5f * s, 18f * s)
        cubicTo(6.5f * s, 17f * s, 10f * s, 17.5f * s, 12f * s, 19f * s)
        close()
      }
      val rightPage = Path().apply {
        moveTo(12f * s, 6.5f * s)
        cubicTo(14f * s, 5f * s, 17.5f * s, 4.5f * s, 20.5f * s, 5.5f * s)
        lineTo(20.5f * s, 18f * s)
        cubicTo(17.5f * s, 17f * s, 14f * s, 17.5f * s, 12f * s, 19f * s)
        close()
      }
      drawPath(leftPage, tint, style = stroke)
      drawPath(rightPage, tint, style = stroke)
    }
  }

  @Composable
  fun LinkOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      val left = Path().apply {
        moveTo(10f * s, 13f * s)
        cubicTo(9.2f * s, 12f * s, 9.2f * s, 10.2f * s, 10.4f * s, 9f * s)
        lineTo(13.4f * s, 6f * s)
        cubicTo(14.8f * s, 4.6f * s, 17f * s, 4.6f * s, 18.4f * s, 6f * s)
        cubicTo(19.8f * s, 7.4f * s, 19.8f * s, 9.6f * s, 18.4f * s, 11f * s)
        lineTo(16.6f * s, 12.8f * s)
      }
      val right = Path().apply {
        moveTo(14f * s, 11f * s)
        cubicTo(14.8f * s, 12f * s, 14.8f * s, 13.8f * s, 13.6f * s, 15f * s)
        lineTo(10.6f * s, 18f * s)
        cubicTo(9.2f * s, 19.4f * s, 7f * s, 19.4f * s, 5.6f * s, 18f * s)
        cubicTo(4.2f * s, 16.6f * s, 4.2f * s, 14.4f * s, 5.6f * s, 13f * s)
        lineTo(7.4f * s, 11.2f * s)
      }
      drawPath(left, tint, style = stroke)
      drawPath(right, tint, style = stroke)
    }
  }

  @Composable
  fun HourglassOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 21.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawLine(tint, Offset(6f * s, 3.5f * s), Offset(18f * s, 3.5f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      drawLine(tint, Offset(6f * s, 20.5f * s), Offset(18f * s, 20.5f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      val glass = Path().apply {
        moveTo(7.5f * s, 3.5f * s)
        lineTo(7.5f * s, 7.2f * s)
        lineTo(12f * s, 12f * s)
        lineTo(7.5f * s, 16.8f * s)
        lineTo(7.5f * s, 20.5f * s)
        moveTo(16.5f * s, 3.5f * s)
        lineTo(16.5f * s, 7.2f * s)
        lineTo(12f * s, 12f * s)
        lineTo(16.5f * s, 16.8f * s)
        lineTo(16.5f * s, 20.5f * s)
      }
      drawPath(glass, tint, style = stroke)
    }
  }

  @Composable
  fun CodeOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 21.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      val left = Path().apply {
        moveTo(8.5f * s, 7.5f * s)
        lineTo(4f * s, 12f * s)
        lineTo(8.5f * s, 16.5f * s)
      }
      val right = Path().apply {
        moveTo(15.5f * s, 7.5f * s)
        lineTo(20f * s, 12f * s)
        lineTo(15.5f * s, 16.5f * s)
      }
      drawPath(left, tint, style = stroke)
      drawPath(right, tint, style = stroke)
      drawLine(tint, Offset(13.5f * s, 5.5f * s), Offset(10.5f * s, 18.5f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
    }
  }

  @Composable
  fun HistoryOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 21.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawArc(tint, -145f, 305f, false, Offset(4f * s, 4f * s), Size(16f * s, 16f * s), style = stroke)
      val arrow = Path().apply {
        moveTo(4f * s, 5.5f * s)
        lineTo(4.6f * s, 9.8f * s)
        lineTo(8.8f * s, 9.2f * s)
      }
      drawPath(arrow, tint, style = stroke)
      val hands = Path().apply {
        moveTo(12f * s, 8f * s)
        lineTo(12f * s, 12.2f * s)
        lineTo(15.2f * s, 14f * s)
      }
      drawPath(hands, tint, style = stroke)
    }
  }

  @Composable
  fun SparklesOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 21.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      val big = Path().apply {
        moveTo(10.5f * s, 3.5f * s)
        quadraticTo(10.5f * s, 10.5f * s, 17.5f * s, 10.5f * s)
        quadraticTo(10.5f * s, 10.5f * s, 10.5f * s, 17.5f * s)
        quadraticTo(10.5f * s, 10.5f * s, 3.5f * s, 10.5f * s)
        quadraticTo(10.5f * s, 10.5f * s, 10.5f * s, 3.5f * s)
        close()
      }
      drawPath(big, tint, style = stroke)
      val small = Path().apply {
        moveTo(18f * s, 14.5f * s)
        lineTo(18f * s, 20.5f * s)
        moveTo(15f * s, 17.5f * s)
        lineTo(21f * s, 17.5f * s)
      }
      drawPath(small, tint, style = stroke)
    }
  }

  @Composable
  fun CalculatorOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 21.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 1.9.dp.toPx()
      val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawRoundRect(tint, Offset(4.5f * s, 3.5f * s), Size(15f * s, 17f * s), CornerRadius(3f * s, 3f * s), style = stroke)
      drawLine(tint, Offset(7.5f * s, 7.5f * s), Offset(16.5f * s, 7.5f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      drawCircle(tint, radius = 1f * s, center = Offset(8.5f * s, 12f * s))
      drawCircle(tint, radius = 1f * s, center = Offset(12f * s, 12f * s))
      drawCircle(tint, radius = 1f * s, center = Offset(15.5f * s, 12f * s))
      drawCircle(tint, radius = 1f * s, center = Offset(8.5f * s, 16.2f * s))
      drawLine(tint, Offset(11.5f * s, 16.2f * s), Offset(16.2f * s, 16.2f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
    }
  }

  @Composable
  fun ClipboardOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 21.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
      drawRoundRect(tint, Offset(5f * s, 5.5f * s), Size(14f * s, 15.5f * s), CornerRadius(2.8f * s, 2.8f * s), style = stroke)
      drawRoundRect(tint, Offset(9f * s, 3.2f * s), Size(6f * s, 3.4f * s), CornerRadius(1.2f * s, 1.2f * s), style = stroke)
    }
  }

  @Composable
  fun TypeOutline(tint: Color, modifier: Modifier = Modifier, size: Dp = 21.dp) {
    Canvas(modifier = modifier.size(size)) {
      val s = this.size.width / 24f
      val strokePx = 2.dp.toPx()
      drawLine(tint, Offset(5f * s, 5.5f * s), Offset(19f * s, 5.5f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      drawLine(tint, Offset(12f * s, 5.5f * s), Offset(12f * s, 19f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
      drawLine(tint, Offset(9f * s, 19f * s), Offset(15f * s, 19f * s), strokeWidth = strokePx, cap = StrokeCap.Round)
    }
  }
}
