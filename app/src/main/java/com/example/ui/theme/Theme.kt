package com.example.ui.theme

import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

val VeilEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

data class VeilTypographyPrefs(
  val fontSize: String = "medium", // "small", "medium", "large"
  val fontStyle: String = "sans"   // "sans", "serif", "mono"
) {
  val scale: Float
    get() = when (fontSize) {
      "small" -> 0.88f
      "large" -> 1.16f
      else -> 1f
    }

  val family: FontFamily
    get() = when (fontStyle) {
      "serif" -> FontFamily.Serif
      "mono" -> FontFamily.Monospace
      else -> InterFontFamily
    }
}

val LocalVeilColors = staticCompositionLocalOf { LightVeilColors }
val LocalReduceMotion = staticCompositionLocalOf { false }
val LocalVeilTypographyPrefs = staticCompositionLocalOf { VeilTypographyPrefs() }

object VeilThemeTokens {
  val colors: VeilColors
    @Composable
    @ReadOnlyComposable
    get() = LocalVeilColors.current

  val reduceMotion: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalReduceMotion.current

  val typographyPrefs: VeilTypographyPrefs
    @Composable
    @ReadOnlyComposable
    get() = LocalVeilTypographyPrefs.current

  val editorTitleStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() {
      val prefs = LocalVeilTypographyPrefs.current
      return VeilTextStyles.editorTitle.copy(
        fontFamily = prefs.family,
        fontSize = (26f * prefs.scale).sp,
        lineHeight = (32f * prefs.scale).sp
      )
    }

  val editorBodyStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() {
      val prefs = LocalVeilTypographyPrefs.current
      return VeilTextStyles.editorBody.copy(
        fontFamily = prefs.family,
        fontSize = (16f * prefs.scale).sp,
        lineHeight = (25.6f * prefs.scale).sp
      )
    }

  val cardTitleStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() {
      val prefs = LocalVeilTypographyPrefs.current
      return VeilTextStyles.cardTitle.copy(
        fontFamily = prefs.family,
        fontSize = (15f * prefs.scale).sp,
        lineHeight = (20f * prefs.scale).sp
      )
    }

  val cardBodyStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() {
      val prefs = LocalVeilTypographyPrefs.current
      return VeilTextStyles.cardBody.copy(
        fontFamily = prefs.family,
        fontSize = (14f * prefs.scale).sp,
        lineHeight = (20.3f * prefs.scale).sp
      )
    }
}

@Composable
private fun rememberReduceMotion(): Boolean {
  val context = LocalContext.current
  return remember(context) {
    try {
      Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
      ) == 0f
    } catch (_: Exception) {
      false
    }
  }
}

@Composable
private fun animateVeilColor(target: Color, reduceMotion: Boolean): Color {
  if (reduceMotion) return target
  val animated by animateColorAsState(
    targetValue = target,
    animationSpec = tween(durationMillis = 300, easing = VeilEasing),
    label = "veil_theme_color"
  )
  return animated
}

@Composable
fun VeilTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  hapticEnabled: Boolean = true,
  fontSize: String = "medium",
  fontStyle: String = "sans",
  content: @Composable () -> Unit,
) {
  val reduceMotion = rememberReduceMotion()
  val view = LocalView.current
  val typographyPrefs = remember(fontSize, fontStyle) {
    VeilTypographyPrefs(fontSize = fontSize, fontStyle = fontStyle)
  }
  val controlledHaptic = remember(hapticEnabled, view) {
    object : HapticFeedback {
      override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        if (!hapticEnabled) return
        val constant = when (hapticFeedbackType) {
          HapticFeedbackType.LongPress -> HapticFeedbackConstants.LONG_PRESS
          HapticFeedbackType.TextHandleMove -> HapticFeedbackConstants.CLOCK_TICK
          else -> HapticFeedbackConstants.CONTEXT_CLICK
        }
        view.performHapticFeedback(constant)
      }
    }
  }
  val target = if (darkTheme) DarkVeilColors else LightVeilColors

  val animatedColors = VeilColors(
    isDark = darkTheme,
    bg = animateVeilColor(target.bg, reduceMotion),
    surface = animateVeilColor(target.surface, reduceMotion),
    text = animateVeilColor(target.text, reduceMotion),
    muted = animateVeilColor(target.muted, reduceMotion),
    accent = animateVeilColor(target.accent, reduceMotion),
    accentSoft = animateVeilColor(target.accentSoft, reduceMotion),
    shadow = animateVeilColor(target.shadow, reduceMotion),
    toolbarShadow = animateVeilColor(target.toolbarShadow, reduceMotion),
    bottomBarShadow = animateVeilColor(target.bottomBarShadow, reduceMotion),
    hiddenBorder = animateVeilColor(target.hiddenBorder, reduceMotion),
    bannerBg = animateVeilColor(target.bannerBg, reduceMotion),
    bannerText = animateVeilColor(target.bannerText, reduceMotion),
    highlight = animateVeilColor(target.highlight, reduceMotion),
    cardYellow = animateVeilColor(target.cardYellow, reduceMotion),
    cardLavender = animateVeilColor(target.cardLavender, reduceMotion),
    cardMint = animateVeilColor(target.cardMint, reduceMotion),
    cardPeach = animateVeilColor(target.cardPeach, reduceMotion),
    cardBlue = animateVeilColor(target.cardBlue, reduceMotion),
    cardPink = animateVeilColor(target.cardPink, reduceMotion),
    cardTeal = animateVeilColor(target.cardTeal, reduceMotion),
    cardCoral = animateVeilColor(target.cardCoral, reduceMotion),
    cardStone = animateVeilColor(target.cardStone, reduceMotion),
    cardPlain = animateVeilColor(target.cardPlain, reduceMotion),
    doodlePink = target.doodlePink,
    doodleHighlighter = target.doodleHighlighter,
    doodleMint = target.doodleMint,
    fabShadow = target.fabShadow
  )

  val colorScheme = if (darkTheme) {
    darkColorScheme(
      primary = animatedColors.accent,
      onPrimary = Color.White,
      primaryContainer = animatedColors.accentSoft,
      onPrimaryContainer = animatedColors.text,
      background = animatedColors.bg,
      onBackground = animatedColors.text,
      surface = animatedColors.surface,
      onSurface = animatedColors.text,
      surfaceVariant = animatedColors.surface,
      onSurfaceVariant = animatedColors.muted,
      outline = animatedColors.hiddenBorder,
      outlineVariant = animatedColors.accentSoft
    )
  } else {
    lightColorScheme(
      primary = animatedColors.accent,
      onPrimary = Color.White,
      primaryContainer = animatedColors.accentSoft,
      onPrimaryContainer = animatedColors.text,
      background = animatedColors.bg,
      onBackground = animatedColors.text,
      surface = animatedColors.surface,
      onSurface = animatedColors.text,
      surfaceVariant = animatedColors.surface,
      onSurfaceVariant = animatedColors.muted,
      outline = animatedColors.hiddenBorder,
      outlineVariant = animatedColors.accentSoft
    )
  }

  CompositionLocalProvider(
    LocalVeilColors provides animatedColors,
    LocalReduceMotion provides reduceMotion,
    LocalVeilTypographyPrefs provides typographyPrefs,
    LocalHapticFeedback provides controlledHaptic
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}
