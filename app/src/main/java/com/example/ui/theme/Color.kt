package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Light Theme Tokens
val VeilLightBg = Color(0xFFF7F7FB)
val VeilLightSurface = Color(0xFFFFFFFF)
val VeilLightText = Color(0xFF1B1B2F)
val VeilLightMuted = Color(0xFF5E5E78)
val VeilLightAccent = Color(0xFF5B5BD6)
val VeilLightAccentSoft = Color(0xFFE8E6FF)
val VeilLightShadow = Color(0x141B1B2F) // rgba(27,27,47,.08)
val VeilLightToolbarShadow = Color(0x241B1B2F) // rgba(27,27,47,.14)
val VeilLightBottomBarShadow = Color(0x0F000000) // rgba(0,0,0,.06)
val VeilLightHiddenBorder = Color(0xFFD4D0FF)
val VeilLightBannerBg = Color(0xFFE3F6EA)
val VeilLightBannerText = Color(0xFF14532D)
val VeilLightHighlight = Color(0xFFFFE066)

// Dark Theme Tokens
val VeilDarkBg = Color(0xFF0F0F1A)
val VeilDarkSurface = Color(0xFF1A1A2B)
val VeilDarkText = Color(0xFFEDEDF7)
val VeilDarkMuted = Color(0xFFA0A0BC)
val VeilDarkAccent = Color(0xFF8B8BFF)
val VeilDarkAccentSoft = Color(0xFF2A2A4A)
val VeilDarkShadow = Color(0x80000000) // rgba(0,0,0,.5)
val VeilDarkToolbarShadow = Color(0x99000000)
val VeilDarkBottomBarShadow = Color(0x66000000)
val VeilDarkHiddenBorder = Color(0xFF3A3A66)
val VeilDarkBannerBg = Color(0xFF153B28)
val VeilDarkBannerText = Color(0xFF8EE6B5)
val VeilDarkHighlight = Color(0xFF8A7A1F)

// Expanded Note Card Colors - Light
val NoteCardYellowLight = Color(0xFFFFF4C2)
val NoteCardLavenderLight = Color(0xFFE8E6FF)
val NoteCardMintLight = Color(0xFFD9F5E6)
val NoteCardPeachLight = Color(0xFFFFE3D6)
val NoteCardBlueLight = Color(0xFFDCEBFF)
val NoteCardPinkLight = Color(0xFFFCE0EC)
val NoteCardTealLight = Color(0xFFD4F3F4)
val NoteCardCoralLight = Color(0xFFFFDFD8)
val NoteCardStoneLight = Color(0xFFECEBE6)
val NoteCardWhiteLight = Color(0xFFFFFFFF)

// Expanded Muted Note Card Colors - Dark
val NoteCardYellowDark = Color(0xFF423A1C)
val NoteCardLavenderDark = Color(0xFF2C294E)
val NoteCardMintDark = Color(0xFF1D3A2F)
val NoteCardPeachDark = Color(0xFF462B22)
val NoteCardBlueDark = Color(0xFF1C2E45)
val NoteCardPinkDark = Color(0xFF422235)
val NoteCardTealDark = Color(0xFF19383D)
val NoteCardCoralDark = Color(0xFF4A2525)
val NoteCardStoneDark = Color(0xFF282834)
val NoteCardSurfaceDark = Color(0xFF1A1A2B)

// Expanded Drawing Stroke Palette Tokens (10 curated inks)
val DoodlePink = Color(0xFFE5527A)
val DoodleYellowHighlighter = Color(0xFFFFE066)
val DoodleMint = Color(0xFF2EAD7B)
val DoodleSkyBlue = Color(0xFF4EA8DE)
val DoodleCoral = Color(0xFFF07167)
val DoodlePurple = Color(0xFFB388FF)
val DoodleOrange = Color(0xFFF4A261)
val DoodleWhite = Color(0xFFF8F8FC)
val DoodleCharcoal = Color(0xFF222233)
val FabShadowColor = Color(0x665B5BD6) // rgba(91,91,214,.4)

val NoteColorOrder = listOf(
  "plain", "yellow", "lavender", "mint", "peach",
  "blue", "pink", "teal", "coral", "stone"
)

val NotePatternOrder = listOf(
  "none", "dots", "grid", "lines", "waves", "stars", "botanical"
)

@Immutable
data class VeilColors(
  val isDark: Boolean,
  val bg: Color,
  val surface: Color,
  val text: Color,
  val muted: Color,
  val accent: Color,
  val accentSoft: Color,
  val shadow: Color,
  val toolbarShadow: Color,
  val bottomBarShadow: Color,
  val hiddenBorder: Color,
  val bannerBg: Color,
  val bannerText: Color,
  val highlight: Color,
  val cardYellow: Color,
  val cardLavender: Color,
  val cardMint: Color,
  val cardPeach: Color,
  val cardBlue: Color,
  val cardPink: Color,
  val cardTeal: Color,
  val cardCoral: Color,
  val cardStone: Color,
  val cardPlain: Color,
  val doodlePink: Color,
  val doodleHighlighter: Color,
  val doodleMint: Color,
  val fabShadow: Color
) {
  fun noteCardColor(colorKey: String): Color = when (colorKey.lowercase()) {
    "yellow" -> cardYellow
    "lavender" -> cardLavender
    "mint" -> cardMint
    "peach" -> cardPeach
    "blue" -> cardBlue
    "pink" -> cardPink
    "teal" -> cardTeal
    "coral" -> cardCoral
    "stone" -> cardStone
    else -> cardPlain
  }

  /**
   * Paired Material You tonal button / chip container color for every note color:
   * - Light Mode: slightly deeper/richer shade of the note hue (cool-gray on plain)
   * - Dark Mode: slightly lighter/elevated tint of the note hue (elevated charcoal on plain)
   */
  fun noteTonalColor(colorKey: String): Color = if (isDark) {
    when (colorKey.lowercase()) {
      "yellow" -> Color(0xFF5A5029)
      "lavender" -> Color(0xFF3F3B6B)
      "mint" -> Color(0xFF2B5243)
      "peach" -> Color(0xFF5E3D31)
      "blue" -> Color(0xFF2A4361)
      "pink" -> Color(0xFF5A314A)
      "teal" -> Color(0xFF265057)
      "coral" -> Color(0xFF633535)
      "stone" -> Color(0xFF3A3A4B)
      else -> Color(0xFF232336)
    }
  } else {
    when (colorKey.lowercase()) {
      "yellow" -> Color(0xFFF3E298)
      "lavender" -> Color(0xFFD5D1FC)
      "mint" -> Color(0xFFBBEAD2)
      "peach" -> Color(0xFFF9CDB9)
      "blue" -> Color(0xFFC1DBFC)
      "pink" -> Color(0xFFF6C6DB)
      "teal" -> Color(0xFFB6E5E7)
      "coral" -> Color(0xFFFAC6BC)
      "stone" -> Color(0xFFDBD9D1)
      else -> Color(0xFFE7E7F1)
    }
  }

  /**
   * 100% opaque tonal surface for seamless docked sheets (Color/Background picker, Insert, Timer, Menu)
   * matching the active note hue so zero text bleeds through from behind.
   */
  fun noteSheetColor(colorKey: String): Color = if (isDark) {
    when (colorKey.lowercase()) {
      "yellow" -> Color(0xFF4E4523)
      "lavender" -> Color(0xFF35325C)
      "mint" -> Color(0xFF244539)
      "peach" -> Color(0xFF523429)
      "blue" -> Color(0xFF233952)
      "pink" -> Color(0xFF4E2A40)
      "teal" -> Color(0xFF20444A)
      "coral" -> Color(0xFF562D2D)
      "stone" -> Color(0xFF313140)
      else -> Color(0xFF19192A)
    }
  } else {
    when (colorKey.lowercase()) {
      "yellow" -> Color(0xFFFAECAE)
      "lavender" -> Color(0xFFDFDCFF)
      "mint" -> Color(0xFFC9F0DC)
      "peach" -> Color(0xFFFCD8C7)
      "blue" -> Color(0xFFCFE3FF)
      "pink" -> Color(0xFFF9D3E4)
      "teal" -> Color(0xFFC5ECEE)
      "coral" -> Color(0xFFFDD3CA)
      "stone" -> Color(0xFFE3E1DA)
      else -> Color(0xFFEEEEF6)
    }
  }

  fun drawingPalette(): List<Color> = listOf(
    accent,
    doodlePink,
    doodleHighlighter,
    doodleMint,
    text,
    DoodleSkyBlue,
    DoodleCoral,
    DoodlePurple,
    DoodleOrange,
    if (isDark) DoodleWhite else DoodleCharcoal
  )
}

val LightVeilColors = VeilColors(
  isDark = false,
  bg = VeilLightBg,
  surface = VeilLightSurface,
  text = VeilLightText,
  muted = VeilLightMuted,
  accent = VeilLightAccent,
  accentSoft = VeilLightAccentSoft,
  shadow = VeilLightShadow,
  toolbarShadow = VeilLightToolbarShadow,
  bottomBarShadow = VeilLightBottomBarShadow,
  hiddenBorder = VeilLightHiddenBorder,
  bannerBg = VeilLightBannerBg,
  bannerText = VeilLightBannerText,
  highlight = VeilLightHighlight,
  cardYellow = NoteCardYellowLight,
  cardLavender = NoteCardLavenderLight,
  cardMint = NoteCardMintLight,
  cardPeach = NoteCardPeachLight,
  cardBlue = NoteCardBlueLight,
  cardPink = NoteCardPinkLight,
  cardTeal = NoteCardTealLight,
  cardCoral = NoteCardCoralLight,
  cardStone = NoteCardStoneLight,
  cardPlain = NoteCardWhiteLight,
  doodlePink = DoodlePink,
  doodleHighlighter = DoodleYellowHighlighter,
  doodleMint = DoodleMint,
  fabShadow = FabShadowColor
)

val DarkVeilColors = VeilColors(
  isDark = true,
  bg = VeilDarkBg,
  surface = VeilDarkSurface,
  text = VeilDarkText,
  muted = VeilDarkMuted,
  accent = VeilDarkAccent,
  accentSoft = VeilDarkAccentSoft,
  shadow = VeilDarkShadow,
  toolbarShadow = VeilDarkToolbarShadow,
  bottomBarShadow = VeilDarkBottomBarShadow,
  hiddenBorder = VeilDarkHiddenBorder,
  bannerBg = VeilDarkBannerBg,
  bannerText = VeilDarkBannerText,
  highlight = VeilDarkHighlight,
  cardYellow = NoteCardYellowDark,
  cardLavender = NoteCardLavenderDark,
  cardMint = NoteCardMintDark,
  cardPeach = NoteCardPeachDark,
  cardBlue = NoteCardBlueDark,
  cardPink = NoteCardPinkDark,
  cardTeal = NoteCardTealDark,
  cardCoral = NoteCardCoralDark,
  cardStone = NoteCardStoneDark,
  cardPlain = NoteCardSurfaceDark,
  doodlePink = DoodlePink,
  doodleHighlighter = DoodleYellowHighlighter,
  doodleMint = DoodleMint,
  fabShadow = FabShadowColor
)
