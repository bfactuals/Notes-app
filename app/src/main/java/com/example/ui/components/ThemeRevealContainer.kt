package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.VeilEasing
import com.example.ui.theme.VeilThemeTokens
import kotlin.math.hypot

/**
 * Full-bleed background container that centers the mobile-first Veil layout
 * (max-width 480dp on larger screens) and plays a smooth 450ms circular reveal
 * expanding from the dark-mode toggle button position.
 */
@Composable
fun ThemeRevealContainer(
  revealTrigger: Int,
  revealOrigin: Offset,
  isDarkTheme: Boolean,
  content: @Composable () -> Unit
) {
  val colors = VeilThemeTokens.colors
  val reduceMotion = VeilThemeTokens.reduceMotion
  val progress = remember { Animatable(1f) }

  LaunchedEffect(revealTrigger) {
    if (revealTrigger > 0 && !reduceMotion) {
      progress.snapTo(0f)
      progress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 450, easing = VeilEasing)
      )
    } else {
      progress.snapTo(1f)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg),
    contentAlignment = Alignment.TopCenter
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 480.dp)
    ) {
      content()
    }

    if (progress.value < 1f && !reduceMotion) {
      val waveColor = if (isDarkTheme) colors.bg else colors.accentSoft
      Canvas(modifier = Modifier.fillMaxSize()) {
        val origin = if (revealOrigin == Offset.Unspecified) {
          Offset(size.width - 38.dp.toPx(), 80.dp.toPx())
        } else {
          revealOrigin
        }
        val maxRadius = hypot(size.width, size.height) * 1.15f
        val currentRadius = (maxRadius * progress.value).coerceAtLeast(1f)
        val alpha = ((1f - progress.value) * 0.28f).coerceIn(0f, 0.28f)
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              Color.Transparent,
              waveColor.copy(alpha = alpha * 0.5f),
              waveColor.copy(alpha = alpha)
            ),
            center = origin,
            radius = currentRadius
          ),
          radius = currentRadius,
          center = origin
        )
      }
    }
  }
}
