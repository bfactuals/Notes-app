package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.VeilIcons
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens
import com.example.viewmodel.VeilViewModel

@Composable
fun ProFeaturesScreen(
  viewModel: VeilViewModel,
  onBack: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  val haptic = LocalHapticFeedback.current
  val proFeatures by viewModel.proFeatures.collectAsState()
  val proGiftClaimed by viewModel.proGiftClaimed.collectAsState()
  val isUnlocked = proGiftClaimed || proFeatures.allEnabled

  val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val effectiveTopPadding = if (statusBarTop > 44.dp) statusBarTop + 12.dp else 56.dp

  Scaffold(
    containerColor = colors.bg
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = innerPadding.calculateBottomPadding())
        .background(colors.bg)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 12.dp, end = 16.dp, top = effectiveTopPadding, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = "Navigate back" }
            .testTag("pro_features_back_button")
        ) {
          VeilIcons.ArrowBack(tint = colors.text, size = 24.dp)
        }
        Text(
          text = "✨ Pro Features",
          style = VeilTextStyles.editorTitle.copy(fontSize = 23.sp),
          color = colors.text,
          modifier = Modifier.weight(1f)
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.accentSoft)
            .border(1.dp, colors.accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = if (isUnlocked) "UNLOCKED" else "FREE",
            style = VeilTextStyles.labelChip.copy(
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.6.sp
            ),
            color = colors.accent
          )
        }
      }

      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Master Toggle Hero Card
        item {
          val cardShape = RoundedCornerShape(20.dp)
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .shadow(3.dp, cardShape, ambientColor = colors.shadow, spotColor = colors.shadow)
              .clip(cardShape)
              .background(colors.accentSoft.copy(alpha = if (colors.isDark) 0.55f else 0.72f))
              .border(1.dp, colors.accent.copy(alpha = 0.35f), cardShape)
          ) {
            ProFeatureToggleRow(
              title = "Enable All Pro Features",
              subtitle = "Turn on every modular power tool at once — 100% free forever",
              checked = proFeatures.allEnabled,
              onCheckedChange = { enabled ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setAllProFeatures(enabled)
              },
              icon = { VeilIcons.SparklesOutline(tint = colors.accent, size = 23.dp) },
              tag = "pro_enable_all_switch"
            )
          }
        }

        item {
          Text(
            text = "INDIVIDUAL TOOLS",
            style = VeilTextStyles.sectionLabel,
            color = colors.muted,
            modifier = Modifier.padding(top = 4.dp)
          )
        }

        item {
          val groupShape = RoundedCornerShape(18.dp)
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .shadow(2.dp, groupShape, ambientColor = colors.shadow, spotColor = colors.shadow)
              .clip(groupShape)
              .background(colors.surface)
          ) {
            ProFeatureToggleRow(
              title = "Self-Destructing Notes",
              subtitle = "Quick timer button next to + for auto-deleting notes",
              checked = proFeatures.selfDestructEnabled,
              onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setProSelfDestruct(it)
              },
              icon = { VeilIcons.HourglassOutline(tint = colors.text, size = 21.dp) },
              tag = "pro_self_destruct_switch"
            )
            ProFeatureToggleRow(
              title = "Smart Inline Calculator",
              subtitle = "Auto-solve math ending with = + Space or Enter",
              checked = proFeatures.inlineCalcEnabled,
              onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setProInlineCalc(it)
              },
              icon = { VeilIcons.CalculatorOutline(tint = colors.text, size = 21.dp) },
              tag = "pro_inline_calc_switch"
            )
            ProFeatureToggleRow(
              title = "30-Day Auto-Empty Trash",
              subtitle = "Permanently purge trashed notes older than 30 days",
              checked = proFeatures.autoEmptyTrashEnabled,
              onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setProAutoEmptyTrash(it)
              },
              icon = { VeilIcons.Trash(tint = colors.text, size = 21.dp) },
              tag = "pro_auto_empty_trash_switch"
            )
            ProFeatureToggleRow(
              title = "Copyable Prompt / Code Blocks",
              subtitle = "Insert shaded snippet cards with 1-tap Copy button",
              checked = proFeatures.codeBlocksEnabled,
              onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setProCodeBlocks(it)
              },
              icon = { VeilIcons.CodeOutline(tint = colors.text, size = 21.dp) },
              tag = "pro_code_blocks_switch"
            )
            ProFeatureToggleRow(
              title = "Local Version History",
              subtitle = "Keep last 5 snapshots per note with 1-tap Restore",
              checked = proFeatures.versionHistoryEnabled,
              onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setProVersionHistory(it)
              },
              icon = { VeilIcons.HistoryOutline(tint = colors.text, size = 21.dp) },
              tag = "pro_version_history_switch"
            )
            ProFeatureToggleRow(
              title = "Clipboard Quick-Save Pill",
              subtitle = "Silently offer to save newly copied text on open",
              checked = proFeatures.clipboardPillEnabled,
              onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setProClipboardPill(it)
              },
              icon = { VeilIcons.ClipboardOutline(tint = colors.text, size = 21.dp) },
              tag = "pro_clipboard_pill_switch"
            )
            ProFeatureToggleRow(
              title = "Smart Auto-Categories",
              subtitle = "Offline filter chips for Links, Expenses & media",
              checked = proFeatures.autoCategoriesEnabled,
              onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setProAutoCategories(it)
              },
              icon = { VeilIcons.Tag(tint = colors.text, size = 21.dp) },
              tag = "pro_auto_categories_switch"
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ProFeatureToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  icon: @Composable () -> Unit,
  tag: String
) {
  val colors = VeilThemeTokens.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(role = Role.Switch) { onCheckedChange(!checked) }
      .padding(horizontal = 14.dp, vertical = 13.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    icon()
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = VeilTextStyles.cardTitle, color = colors.text)
      Spacer(modifier = Modifier.height(2.dp))
      Text(subtitle, style = VeilTextStyles.meta, color = colors.muted)
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = colors.surface,
        checkedTrackColor = colors.accent,
        uncheckedThumbColor = colors.muted,
        uncheckedTrackColor = colors.bg
      ),
      modifier = Modifier.testTag(tag)
    )
  }
}
