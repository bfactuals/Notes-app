package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RemoteConfigValidator
import com.example.data.SocialLinksConfig
import com.example.ui.components.ProGiftModalDialog
import com.example.ui.components.VeilIcons
import com.example.ui.components.tactilePress
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens
import com.example.viewmodel.VeilViewModel

@Composable
fun ProFeaturesScreen(
  viewModel: VeilViewModel,
  onBack: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val proFeatures by viewModel.proFeatures.collectAsState()
  val proGiftClaimed by viewModel.proGiftClaimed.collectAsState()
  val remoteConfig by viewModel.remoteConfig.collectAsState()

  var showClaimModal by remember { mutableStateOf(false) }

  val enabledCount = remember(proFeatures) {
    listOf(
      proFeatures.selfDestructEnabled,
      proFeatures.inlineCalcEnabled,
      proFeatures.autoEmptyTrashEnabled,
      proFeatures.codeBlocksEnabled,
      proFeatures.versionHistoryEnabled,
      proFeatures.clipboardPillEnabled,
      proFeatures.autoCategoriesEnabled
    ).count { it }
  }

  val downloadUrl = remember(remoteConfig.socialLinks.githubRepo) {
    val repo = remoteConfig.socialLinks.githubRepo
      .takeIf { RemoteConfigValidator.isValidHttpsUrl(it) }
      ?: SocialLinksConfig.DEFAULT_GITHUB_REPO
    "${repo.trimEnd('/')}/releases/latest"
  }

  if (showClaimModal) {
    ProGiftModalDialog(
      onClaim = {
        showClaimModal = false
        viewModel.claimFreeProGift()
      },
      onDismiss = {
        showClaimModal = false
      }
    )
  }

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
          text = "Pro Features",
          style = VeilTextStyles.editorTitle.copy(fontSize = 23.sp),
          color = colors.text,
          modifier = Modifier.weight(1f)
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (proGiftClaimed) colors.accentSoft else colors.accent.copy(alpha = 0.16f))
            .border(1.dp, colors.accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("pro_status_badge")
        ) {
          Text(
            text = if (proGiftClaimed) "PRO ACTIVE" else "GIFT",
            style = VeilTextStyles.labelChip.copy(
              fontSize = 10.5.sp,
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Solid Purple Top Hero Card
        item {
          val heroShape = RoundedCornerShape(22.dp)
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .shadow(6.dp, heroShape, ambientColor = colors.fabShadow, spotColor = colors.fabShadow)
              .clip(heroShape)
              .background(colors.accent)
              .clickable(role = Role.Switch) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (!proGiftClaimed) {
                  showClaimModal = true
                } else {
                  viewModel.setAllProFeatures(!proFeatures.allEnabled)
                }
              }
              .padding(horizontal = 18.dp, vertical = 18.dp)
              .testTag("pro_hero_card"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                VeilIcons.CrownOutline(tint = Color.White, size = 22.dp)
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "All Pro features",
                  style = VeilTextStyles.cardTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                  color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = if (proGiftClaimed) {
                    "$enabledCount tools on · free forever"
                  } else {
                    "7 power tools · 100% free gift"
                  },
                  style = VeilTextStyles.meta.copy(fontSize = 13.sp),
                  color = Color.White.copy(alpha = 0.86f)
                )
              }

              if (proGiftClaimed) {
                Switch(
                  checked = proFeatures.allEnabled,
                  onCheckedChange = { enabled ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.setAllProFeatures(enabled)
                  },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.accent,
                    checkedTrackColor = Color.White,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.28f),
                    uncheckedBorderColor = Color.White.copy(alpha = 0.5f)
                  ),
                  modifier = Modifier.testTag("pro_enable_all_switch")
                )
              }
            }

            if (!proGiftClaimed) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(44.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .background(Color.White)
                  .tactilePress(scaleDown = 0.96f, hapticOnClick = true, role = Role.Button) {
                    showClaimModal = true
                  }
                  .testTag("pro_hero_claim_button"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Claim free Pro",
                  style = VeilTextStyles.cardTitle.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Bold),
                  color = colors.accent
                )
              }
            }
          }
        }

        item {
          Text(
            text = "MODULAR TOOLS",
            style = VeilTextStyles.sectionLabel,
            color = colors.muted,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp)
          )
        }

        // Grouped container listing all 7 modular Pro tools with soft rounded-square icon containers
        item {
          val groupShape = RoundedCornerShape(20.dp)
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .shadow(2.dp, groupShape, ambientColor = colors.shadow, spotColor = colors.shadow)
              .clip(groupShape)
              .background(colors.surface)
          ) {
            fun handleToolToggle(requested: Boolean, apply: (Boolean) -> Unit) {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              if (!proGiftClaimed && requested) {
                showClaimModal = true
              } else {
                apply(requested)
              }
            }

            ProFeatureToggleRow(
              title = "Self-Destructing Notes",
              subtitle = "Auto-delete notes after 1h, 24h, or 7d",
              checked = proFeatures.selfDestructEnabled,
              onCheckedChange = { handleToolToggle(it) { v -> viewModel.setProSelfDestruct(v) } },
              icon = { tint -> VeilIcons.HourglassOutline(tint = tint, size = 20.dp) },
              tag = "pro_self_destruct_switch"
            )
            ProToolDivider()
            ProFeatureToggleRow(
              title = "Smart Inline Calculator",
              subtitle = "Solve math ending with = + Space or Enter",
              checked = proFeatures.inlineCalcEnabled,
              onCheckedChange = { handleToolToggle(it) { v -> viewModel.setProInlineCalc(v) } },
              icon = { tint -> VeilIcons.CalculatorOutline(tint = tint, size = 20.dp) },
              tag = "pro_inline_calc_switch"
            )
            ProToolDivider()
            ProFeatureToggleRow(
              title = "30-Day Auto-Empty Trash",
              subtitle = "Automatically purge trash older than 30 days",
              checked = proFeatures.autoEmptyTrashEnabled,
              onCheckedChange = { handleToolToggle(it) { v -> viewModel.setProAutoEmptyTrash(v) } },
              icon = { tint -> VeilIcons.Trash(tint = tint, size = 20.dp) },
              tag = "pro_auto_empty_trash_switch"
            )
            ProToolDivider()
            ProFeatureToggleRow(
              title = "Copyable Prompt / Code Blocks",
              subtitle = "Insert snippet cards with 1-tap Copy",
              checked = proFeatures.codeBlocksEnabled,
              onCheckedChange = { handleToolToggle(it) { v -> viewModel.setProCodeBlocks(v) } },
              icon = { tint -> VeilIcons.CodeOutline(tint = tint, size = 20.dp) },
              tag = "pro_code_blocks_switch"
            )
            ProToolDivider()
            ProFeatureToggleRow(
              title = "Local Version History",
              subtitle = "Keep last 5 snapshots per note to restore",
              checked = proFeatures.versionHistoryEnabled,
              onCheckedChange = { handleToolToggle(it) { v -> viewModel.setProVersionHistory(v) } },
              icon = { tint -> VeilIcons.HistoryOutline(tint = tint, size = 20.dp) },
              tag = "pro_version_history_switch"
            )
            ProToolDivider()
            ProFeatureToggleRow(
              title = "Clipboard Quick-Save Pill",
              subtitle = "Offer to save newly copied text on open",
              checked = proFeatures.clipboardPillEnabled,
              onCheckedChange = { handleToolToggle(it) { v -> viewModel.setProClipboardPill(v) } },
              icon = { tint -> VeilIcons.ClipboardOutline(tint = tint, size = 20.dp) },
              tag = "pro_clipboard_pill_switch"
            )
            ProToolDivider()
            ProFeatureToggleRow(
              title = "Smart Auto-Categories",
              subtitle = "Offline filter chips for Links, Expenses & media",
              checked = proFeatures.autoCategoriesEnabled,
              onCheckedChange = { handleToolToggle(it) { v -> viewModel.setProAutoCategories(v) } },
              icon = { tint -> VeilIcons.Tag(tint = tint, size = 20.dp) },
              tag = "pro_auto_categories_switch"
            )
          }
        }

        // Bottom "Gift Free Pro to a Friend" card with Share Veil button
        item {
          val shareCardShape = RoundedCornerShape(20.dp)
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .shadow(2.dp, shareCardShape, ambientColor = colors.shadow, spotColor = colors.shadow)
              .clip(shareCardShape)
              .background(colors.surface)
              .border(1.dp, colors.accent.copy(alpha = 0.28f), shareCardShape)
              .padding(16.dp)
              .testTag("gift_friend_pro_card"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(colors.accentSoft),
                contentAlignment = Alignment.Center
              ) {
                VeilIcons.GiftOutline(tint = colors.accent, size = 22.dp)
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Gift Free Pro to a Friend",
                  style = VeilTextStyles.cardTitle.copy(fontSize = 15.5.sp),
                  color = colors.text
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Enjoying Veil? Pass this free ad-free Pro app to a friend.",
                  style = VeilTextStyles.meta.copy(lineHeight = 17.sp),
                  color = colors.muted
                )
              }
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.accent)
                .tactilePress(scaleDown = 0.96f, hapticOnClick = true, role = Role.Button) {
                  val shareText = "I've been using Veil Notes — a fast, offline-first, ad-free notes app with Pro features completely free forever. Try it here:\n$downloadUrl"
                  val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Try Veil Notes (Free Pro)")
                    putExtra(Intent.EXTRA_TEXT, shareText)
                  }
                  runCatching {
                    context.startActivity(Intent.createChooser(intent, "Share Veil"))
                  }
                }
                .testTag("share_veil_friend_button"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
              VeilIcons.ShareOutline(tint = Color.White, size = 18.dp)
              Text(
                text = "Share Veil",
                style = VeilTextStyles.cardTitle.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                color = Color.White
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ProToolDivider() {
  val colors = VeilThemeTokens.colors
  HorizontalDivider(
    modifier = Modifier.padding(start = 66.dp, end = 16.dp),
    color = colors.accentSoft.copy(alpha = 0.65f),
    thickness = 1.dp
  )
}

@Composable
private fun ProFeatureToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  icon: @Composable (Color) -> Unit,
  tag: String
) {
  val colors = VeilThemeTokens.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(role = Role.Switch) { onCheckedChange(!checked) }
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(if (checked) colors.accentSoft else colors.bg),
      contentAlignment = Alignment.Center
    ) {
      icon(if (checked) colors.accent else colors.muted)
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = VeilTextStyles.cardTitle.copy(fontSize = 14.5.sp),
        color = colors.text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = VeilTextStyles.meta,
        color = colors.muted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = colors.accent,
        uncheckedThumbColor = colors.muted,
        uncheckedTrackColor = colors.bg
      ),
      modifier = Modifier.testTag(tag)
    )
  }
}
