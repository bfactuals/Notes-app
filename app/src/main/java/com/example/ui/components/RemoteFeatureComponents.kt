package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.data.AnnouncementConfig
import com.example.data.RemoteConfigValidator
import com.example.data.RemoteVersionInfo
import com.example.data.UpdateDownloadState
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens

fun openSafeExternalUrl(context: Context, url: String): Boolean {
  if (!RemoteConfigValidator.isValidHttpsUrl(url)) return false
  return runCatching {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
    true
  }.getOrDefault(false)
}

fun openSafeSupportEmail(context: Context, email: String, subject: String): Boolean {
  if (!RemoteConfigValidator.isValidEmail(email)) return false
  return runCatching {
    val uri = Uri.Builder()
      .scheme("mailto")
      .opaquePart(email)
      .build()
    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
      putExtra(Intent.EXTRA_SUBJECT, subject)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
    true
  }.getOrDefault(false)
}

@Composable
fun HomeAnnouncementBanner(
  announcement: AnnouncementConfig,
  visible: Boolean,
  onOpenLink: (String) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  val hasValidLink = RemoteConfigValidator.isValidHttpsUrl(announcement.link)

  val reduceMotion = VeilThemeTokens.reduceMotion

  AnimatedVisibility(
    visible = visible && announcement.isDisplayable,
    enter = fadeIn(tween(if (reduceMotion) 0 else 220)) +
      expandVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) +
      slideInVertically(tween(if (reduceMotion) 0 else 220)) { -it / 5 },
    exit = fadeOut(tween(if (reduceMotion) 0 else 160)) +
      shrinkVertically(tween(if (reduceMotion) 0 else 180)) +
      slideOutVertically(tween(if (reduceMotion) 0 else 160)) { -it / 5 },
    modifier = modifier
  ) {
    val shape = RoundedCornerShape(16.dp)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .clip(shape)
        .background(colors.accentSoft.copy(alpha = if (colors.isDark) 0.62f else 0.85f))
        .border(1.dp, colors.accent.copy(alpha = 0.35f), shape)
        .then(
          if (hasValidLink) {
            Modifier.clickable(role = Role.Button) { onOpenLink(announcement.link) }
          } else {
            Modifier
          }
        )
        .padding(start = 14.dp, top = 11.dp, bottom = 11.dp, end = 6.dp)
        .testTag("home_announcement_banner"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(colors.accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
      ) {
        VeilIcons.MegaphoneOutline(tint = colors.accent, size = 18.dp)
      }

      Column(modifier = Modifier.weight(1f)) {
        if (announcement.title.isNotBlank()) {
          Text(
            text = announcement.title,
            style = VeilTextStyles.cardTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
        if (announcement.message.isNotBlank()) {
          if (announcement.title.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
          }
          Text(
            text = announcement.message,
            style = VeilTextStyles.meta.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
            color = colors.muted,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
          )
        }
        if (hasValidLink) {
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "Learn more",
              style = VeilTextStyles.labelChip.copy(fontWeight = FontWeight.SemiBold),
              color = colors.accent
            )
            VeilIcons.ExternalLinkOutline(tint = colors.accent, size = 13.dp)
          }
        }
      }

      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .clickable(
            role = Role.Button,
            onClickLabel = "Dismiss announcement",
            onClick = onDismiss
          )
          .semantics { contentDescription = "Dismiss announcement" }
          .testTag("dismiss_announcement_button"),
        contentAlignment = Alignment.Center
      ) {
        VeilIcons.Close(tint = colors.muted, size = 16.dp)
      }
    }
  }
}

@Composable
fun GitHubStarNudgeCard(
  visible: Boolean,
  onStarNow: () -> Unit,
  onMaybeLater: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  val haptic = LocalHapticFeedback.current

  val reduceMotion = VeilThemeTokens.reduceMotion

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(if (reduceMotion) 0 else 220)) +
      expandVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) +
      slideInVertically(tween(if (reduceMotion) 0 else 220)) { -it / 5 },
    exit = fadeOut(tween(if (reduceMotion) 0 else 160)) +
      shrinkVertically(tween(if (reduceMotion) 0 else 180)) +
      slideOutVertically(tween(if (reduceMotion) 0 else 160)) { -it / 5 },
    modifier = modifier
  ) {
    val shape = RoundedCornerShape(18.dp)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .shadow(2.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
        .clip(shape)
        .background(colors.surface)
        .border(1.dp, colors.accent.copy(alpha = 0.28f), shape)
        .padding(horizontal = 15.dp, vertical = 13.dp)
        .testTag("github_star_nudge_card"),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.accentSoft),
          contentAlignment = Alignment.Center
        ) {
          VeilIcons.StarOutline(tint = colors.accent, size = 18.dp)
        }
        Text(
          text = "Enjoying Veil? Support this free, ad-free app with a ⭐ on GitHub!",
          style = VeilTextStyles.cardBody.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
          color = colors.text,
          modifier = Modifier.weight(1f)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onMaybeLater()
          },
          modifier = Modifier.testTag("star_nudge_later_button")
        ) {
          Text(
            text = "Maybe Later",
            style = VeilTextStyles.meta.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
            color = colors.muted
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.accent)
            .tactilePress(scaleDown = 0.95f, hapticOnClick = true, role = Role.Button, onClick = onStarNow)
            .padding(horizontal = 16.dp, vertical = 9.dp)
            .testTag("star_nudge_action_button")
        ) {
          Text(
            text = "Star Now ⭐",
            style = VeilTextStyles.cardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = Color.White
          )
        }
      }
    }
  }
}

/**
 * Clean, dismissible 10-notes milestone card on the Home screen inviting the user
 * to share Veil ("Gift Free Pro to a friend") via the native Android Share Sheet.
 */
@Composable
fun GiftProMilestoneCard(
  visible: Boolean,
  onShareGift: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  val reduceMotion = VeilThemeTokens.reduceMotion
  val haptic = LocalHapticFeedback.current

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(if (reduceMotion) 0 else 240)) +
      expandVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) +
      slideInVertically(tween(if (reduceMotion) 0 else 240)) { -it / 5 },
    exit = fadeOut(tween(if (reduceMotion) 0 else 160)) +
      shrinkVertically(tween(if (reduceMotion) 0 else 180)) +
      slideOutVertically(tween(if (reduceMotion) 0 else 160)) { -it / 5 },
    modifier = modifier
  ) {
    val shape = RoundedCornerShape(18.dp)
    val cardBg = if (colors.isDark) {
      colors.accentSoft.copy(alpha = 0.72f)
    } else {
      colors.accentSoft.copy(alpha = 0.86f)
    }
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .shadow(3.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
        .clip(shape)
        .background(cardBg)
        .border(1.dp, colors.accent.copy(alpha = if (colors.isDark) 0.48f else 0.35f), shape)
        .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 13.dp)
        .testTag("gift_pro_milestone_card"),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .padding(top = 2.dp)
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.accent.copy(alpha = 0.2f))
            .border(1.dp, colors.accent.copy(alpha = 0.38f), RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          VeilIcons.CrownOutline(tint = colors.accent, size = 20.dp)
        }

        Column(
          modifier = Modifier
            .weight(1f)
            .padding(top = 1.dp),
          verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          Text(
            text = "Loving Veil? Gift Free Pro to a friend",
            style = VeilTextStyles.cardTitle.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
            color = colors.text
          )
          Text(
            text = "You've created 10+ notes. Share this free, ad-free app with someone who'd love it.",
            style = VeilTextStyles.meta.copy(fontSize = 12.5.sp, lineHeight = 17.5.sp),
            color = colors.muted
          )
        }

        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(
              role = Role.Button,
              onClickLabel = "Dismiss milestone reminder",
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDismiss()
              }
            )
            .semantics { contentDescription = "Dismiss milestone reminder" }
            .testTag("gift_pro_milestone_dismiss_button"),
          contentAlignment = Alignment.Center
        ) {
          VeilIcons.Close(tint = colors.muted, size = 16.dp)
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(end = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.accent)
            .tactilePress(scaleDown = 0.95f, hapticOnClick = true, role = Role.Button, onClick = onShareGift)
            .padding(horizontal = 16.dp, vertical = 9.dp)
            .testTag("gift_pro_milestone_share_button"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
          VeilIcons.GiftOutline(tint = Color.White, size = 16.dp)
          Text(
            text = "Share Gift",
            style = VeilTextStyles.cardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = Color.White
          )
        }
      }
    }
  }
}

@Composable
fun AppUpdateDialog(
  versionInfo: RemoteVersionInfo,
  isMandatory: Boolean,
  downloadState: UpdateDownloadState,
  installNotice: String?,
  onStartDownload: () -> Unit,
  onInstallNow: () -> Unit,
  onDismiss: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  val haptic = LocalHapticFeedback.current
  val isBusy = downloadState is UpdateDownloadState.Downloading || downloadState is UpdateDownloadState.Verifying

  // Automatically trigger native package installer once download & SHA-256 verification succeed
  LaunchedEffect(downloadState) {
    if (downloadState is UpdateDownloadState.ReadyToInstall) {
      onInstallNow()
    }
  }

  Dialog(
    onDismissRequest = {
      if (!isMandatory && !isBusy) {
        onDismiss()
      }
    },
    properties = DialogProperties(
      dismissOnBackPress = !isMandatory && !isBusy,
      dismissOnClickOutside = !isMandatory && !isBusy
    )
  ) {
    val shape = RoundedCornerShape(24.dp)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(16.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
        .clip(shape)
        .background(colors.surface)
        .border(1.dp, colors.accent.copy(alpha = 0.25f), shape)
        .padding(22.dp)
        .testTag("app_update_dialog"),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.accentSoft),
          contentAlignment = Alignment.Center
        ) {
          VeilIcons.DownloadOutline(tint = colors.accent, size = 22.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = versionInfo.title.ifBlank { "Update Available" },
            style = VeilTextStyles.cardTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            color = colors.text
          )
          Spacer(modifier = Modifier.height(2.dp))
          val versionLabel = buildString {
            append("v${BuildConfig.VERSION_NAME}")
            if (versionInfo.latestVersion.isNotBlank()) {
              append("  →  v${versionInfo.latestVersion}")
            }
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = versionLabel,
              style = VeilTextStyles.labelChip.copy(fontWeight = FontWeight.SemiBold),
              color = colors.accent
            )
            if (isMandatory) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(colors.accent.copy(alpha = 0.16f))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "REQUIRED",
                  style = VeilTextStyles.labelChip.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                  color = colors.accent
                )
              }
            }
          }
        }
      }

      if (versionInfo.updateMessage.isNotBlank()) {
        Text(
          text = versionInfo.updateMessage,
          style = VeilTextStyles.cardBody,
          color = colors.text
        )
      }

      if (versionInfo.whatsNew.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.bg.copy(alpha = 0.8f))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "WHAT'S NEW",
            style = VeilTextStyles.sectionLabel,
            color = colors.muted
          )
          versionInfo.whatsNew.forEach { bullet ->
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.Top
            ) {
              Text("•", style = VeilTextStyles.cardBody, color = colors.accent)
              Text(
                text = bullet,
                style = VeilTextStyles.meta.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = colors.text,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      when (downloadState) {
        is UpdateDownloadState.Downloading -> {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (downloadState.progress >= 0f) {
              LinearProgressIndicator(
                progress = { downloadState.progress },
                color = colors.accent,
                trackColor = colors.accentSoft,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(4.dp))
              )
            } else {
              LinearProgressIndicator(
                color = colors.accent,
                trackColor = colors.accentSoft,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(4.dp))
              )
            }
            val progressText = if (downloadState.totalBytes > 0L) {
              val pct = (downloadState.progress * 100f).toInt().coerceIn(0, 100)
              val dlMb = downloadState.downloadedBytes / (1024f * 1024f)
              val totMb = downloadState.totalBytes / (1024f * 1024f)
              String.format("Downloading update… %d%% (%.1f / %.1f MB)", pct, dlMb, totMb)
            } else {
              val dlMb = downloadState.downloadedBytes / (1024f * 1024f)
              String.format("Downloading update… %.1f MB", dlMb)
            }
            Text(
              text = progressText,
              style = VeilTextStyles.meta,
              color = colors.muted
            )
          }
        }
        is UpdateDownloadState.Verifying -> {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            CircularProgressIndicator(
              color = colors.accent,
              strokeWidth = 2.dp,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "Verifying SHA-256 integrity…",
              style = VeilTextStyles.meta,
              color = colors.text
            )
          }
        }
        is UpdateDownloadState.ReadyToInstall -> {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(colors.accentSoft.copy(alpha = 0.65f))
              .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            VeilIcons.ShieldCheckOutline(tint = colors.accent, size = 18.dp)
            Text(
              text = installNotice ?: "Verified and ready to install.",
              style = VeilTextStyles.meta.copy(fontSize = 12.5.sp),
              color = colors.text,
              modifier = Modifier.weight(1f)
            )
          }
        }
        is UpdateDownloadState.Error -> {
          Text(
            text = downloadState.message,
            style = VeilTextStyles.meta.copy(fontSize = 12.5.sp),
            color = MaterialTheme.colorScheme.error
          )
        }
        UpdateDownloadState.Idle -> Unit
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (!isMandatory && !isBusy) {
          TextButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onDismiss()
            },
            modifier = Modifier.testTag("update_later_button")
          ) {
            Text(
              text = "Later",
              style = VeilTextStyles.cardTitle.copy(fontSize = 14.sp),
              color = colors.muted
            )
          }
        }

        val primaryLabel = when (downloadState) {
          is UpdateDownloadState.Downloading -> "Downloading…"
          is UpdateDownloadState.Verifying -> "Verifying…"
          is UpdateDownloadState.ReadyToInstall -> "Install Now"
          is UpdateDownloadState.Error -> "Retry Update"
          UpdateDownloadState.Idle -> "Update Now"
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (isBusy) colors.accent.copy(alpha = 0.5f) else colors.accent)
            .then(
              if (!isBusy) {
                Modifier.tactilePress(
                  scaleDown = 0.95f,
                  hapticOnClick = true,
                  role = Role.Button
                ) {
                  if (downloadState is UpdateDownloadState.ReadyToInstall) {
                    onInstallNow()
                  } else {
                    onStartDownload()
                  }
                }
              } else {
                Modifier
              }
            )
            .padding(horizontal = 20.dp, vertical = 11.dp)
            .testTag("update_now_button")
        ) {
          Text(
            text = primaryLabel,
            style = VeilTextStyles.cardTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = Color.White
          )
        }
      }
    }
  }
}

@Composable
fun MaintenanceOverlayScreen(
  message: String,
  isRefreshing: Boolean,
  onRefresh: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(28.dp)
      .testTag("maintenance_overlay"),
    contentAlignment = Alignment.Center
  ) {
    val cardShape = RoundedCornerShape(24.dp)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 420.dp)
        .shadow(6.dp, cardShape, ambientColor = colors.shadow, spotColor = colors.shadow)
        .clip(cardShape)
        .background(colors.surface)
        .padding(26.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(colors.accentSoft),
        contentAlignment = Alignment.Center
      ) {
        VeilIcons.SettingsGear(tint = colors.accent, size = 28.dp)
      }

      Text(
        text = "Scheduled Maintenance",
        style = VeilTextStyles.editorTitle.copy(fontSize = 20.sp),
        color = colors.text,
        textAlign = TextAlign.Center
      )

      Text(
        text = message,
        style = VeilTextStyles.cardBody,
        color = colors.muted,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(4.dp))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(22.dp))
          .background(colors.accent)
          .tactilePress(
            scaleDown = 0.95f,
            hapticOnClick = true,
            role = Role.Button,
            onClick = { if (!isRefreshing) onRefresh() }
          )
          .padding(horizontal = 24.dp, vertical = 12.dp)
          .testTag("maintenance_refresh_button")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (isRefreshing) {
            CircularProgressIndicator(
              color = Color.White,
              strokeWidth = 2.dp,
              modifier = Modifier.size(16.dp)
            )
          } else {
            VeilIcons.RefreshOutline(tint = Color.White, size = 17.dp)
          }
          Text(
            text = if (isRefreshing) "Checking…" else "Check Again",
            style = VeilTextStyles.cardTitle.copy(fontSize = 14.sp),
            color = Color.White
          )
        }
      }
    }
  }
}
