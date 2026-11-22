package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.BuildConfig
import com.example.data.NoteImageHelper
import com.example.data.RemoteConfigValidator
import com.example.ui.components.VeilIcons
import com.example.ui.components.openSafeExternalUrl
import com.example.ui.components.openSafeSupportEmail
import com.example.ui.components.tactilePress
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens
import com.example.viewmodel.VeilViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutSupportScreen(
  viewModel: VeilViewModel,
  onBack: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val remoteConfig by viewModel.remoteConfig.collectAsState()
  val isRefreshing by viewModel.isRefreshingConfig.collectAsState()
  val availableUpdate by viewModel.availableUpdate.collectAsState()
  val remoteVersionInfo by viewModel.remoteVersionInfo.collectAsState()
  val qrReachableMap by viewModel.qrReachableMap.collectAsState()

  var showChangelogDialog by remember { mutableStateOf(false) }
  var avatarLoadFailed by remember(remoteConfig.about.avatarUrl) { mutableStateOf(false) }
  var qrImageLoadFailed by remember(remoteConfig.support.qrImageUrl) { mutableStateOf(false) }

  val qrUrl = remoteConfig.support.qrImageUrl
  LaunchedEffect(qrUrl, remoteConfig.support.enabled, remoteConfig.support.upiEnabled) {
    if (remoteConfig.support.enabled && remoteConfig.support.upiEnabled && qrUrl.isNotEmpty()) {
      viewModel.checkQrReachability(qrUrl)
    }
  }

  val spinTransition = rememberInfiniteTransition(label = "refresh_spin")
  val spinAngle by spinTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 850, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "refresh_rotation"
  )

  fun openUrlOrNotify(url: String) {
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    if (!openSafeExternalUrl(context, url)) {
      scope.launch { snackbarHostState.showSnackbar("Could not open link.") }
    }
  }

  if (showChangelogDialog) {
    val changelogItems = remember(remoteConfig.about.changelog, remoteVersionInfo?.whatsNew) {
      val combined = (remoteVersionInfo?.whatsNew.orEmpty() + remoteConfig.about.changelog).distinct()
      combined
    }
    AlertDialog(
      onDismissRequest = { showChangelogDialog = false },
      containerColor = colors.surface,
      titleContentColor = colors.text,
      textContentColor = colors.text,
      shape = RoundedCornerShape(24.dp),
      title = {
        Column {
          Text("What's New", style = VeilTextStyles.cardTitle.copy(fontSize = 18.sp))
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Installed version v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = VeilTextStyles.meta,
            color = colors.muted
          )
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          changelogItems.forEach { item ->
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.Top
            ) {
              Text("•", style = VeilTextStyles.cardBody, color = colors.accent)
              Text(
                text = item,
                style = VeilTextStyles.cardBody.copy(fontSize = 14.sp),
                color = colors.text,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showChangelogDialog = false }) {
          Text("Close", style = VeilTextStyles.cardTitle, color = colors.accent)
        }
      }
    )
  }

  val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val effectiveTopPadding = if (statusBarTop > 44.dp) statusBarTop + 12.dp else 56.dp

  Scaffold(
    containerColor = colors.bg,
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = innerPadding.calculateBottomPadding())
        .background(colors.bg)
    ) {
      // Top Bar with manual Refresh button
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 12.dp, end = 12.dp, top = effectiveTopPadding, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = "Navigate back" }
            .testTag("about_support_back_button")
        ) {
          VeilIcons.ArrowBack(tint = colors.text, size = 24.dp)
        }
        Text(
          text = "About & Support",
          style = VeilTextStyles.editorTitle,
          color = colors.text,
          modifier = Modifier.weight(1f)
        )
        IconButton(
          onClick = {
            if (!isRefreshing) {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              viewModel.refreshRemoteData(force = true) { updated ->
                scope.launch {
                  snackbarHostState.showSnackbar(
                    if (updated) "Updated from server." else "Offline — showing cached info."
                  )
                }
              }
            }
          },
          modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = "Refresh remote configuration" }
            .testTag("about_support_refresh_button")
        ) {
          VeilIcons.RefreshOutline(
            tint = if (isRefreshing) colors.accent else colors.text,
            size = 21.dp,
            modifier = Modifier.rotate(if (isRefreshing) spinAngle else 0f)
          )
        }
      }

      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // 1. CREATOR & APP CARD
        item {
          val cardShape = RoundedCornerShape(20.dp)
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .shadow(3.dp, cardShape, ambientColor = colors.shadow, spotColor = colors.shadow)
              .clip(cardShape)
              .background(colors.surface)
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              val avatarUrl = remoteConfig.about.avatarUrl
              Box(
                modifier = Modifier
                  .size(56.dp)
                  .clip(CircleShape)
                  .background(colors.accentSoft)
                  .border(1.5.dp, colors.accent.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                if (avatarUrl.isNotEmpty() && !avatarLoadFailed) {
                  AsyncImage(
                    model = ImageRequest.Builder(context)
                      .data(avatarUrl)
                      .diskCachePolicy(CachePolicy.ENABLED)
                      .memoryCachePolicy(CachePolicy.ENABLED)
                      .crossfade(true)
                      .build(),
                    contentDescription = "${remoteConfig.about.creatorName} avatar",
                    contentScale = ContentScale.Crop,
                    onError = { avatarLoadFailed = true },
                    modifier = Modifier.fillMaxSize()
                  )
                } else {
                  Text(
                    text = remoteConfig.about.creatorName.firstOrNull()?.uppercase() ?: "B",
                    style = VeilTextStyles.editorTitle.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    color = colors.accent
                  )
                }
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = remoteConfig.about.creatorName,
                  style = VeilTextStyles.editorTitle.copy(fontSize = 19.sp),
                  color = colors.text
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Veil Notes · v${BuildConfig.VERSION_NAME}",
                  style = VeilTextStyles.meta.copy(fontWeight = FontWeight.Medium),
                  color = colors.muted
                )
              }
            }

            if (remoteConfig.about.bio.isNotBlank()) {
              Text(
                text = remoteConfig.about.bio,
                style = VeilTextStyles.cardBody,
                color = colors.text
              )
            }

            // Action chips: Changelog + optional Update Available trigger
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                modifier = Modifier
                  .clip(RoundedCornerShape(14.dp))
                  .background(colors.accentSoft)
                  .tactilePress(scaleDown = 0.96f, hapticOnClick = true, role = Role.Button) {
                    showChangelogDialog = true
                  }
                  .padding(horizontal = 12.dp, vertical = 8.dp)
                  .testTag("whats_new_changelog_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                VeilIcons.SparklesOutline(tint = colors.accent, size = 16.dp)
                Text(
                  text = "What's New / Changelog",
                  style = VeilTextStyles.labelChip.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                  color = colors.accent
                )
              }

              val pendingUpdate = availableUpdate
              if (pendingUpdate != null) {
                Row(
                  modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accent)
                    .tactilePress(scaleDown = 0.96f, hapticOnClick = true, role = Role.Button) {
                      viewModel.reopenUpdateDialog()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("about_update_available_button"),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  VeilIcons.DownloadOutline(tint = Color.White, size = 15.dp)
                  Text(
                    text = "Update v${pendingUpdate.latestVersion.ifBlank { pendingUpdate.versionCode.toString() }}",
                    style = VeilTextStyles.labelChip.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White
                  )
                }
              }
            }

            // Privacy Badge
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.bg.copy(alpha = 0.8f))
                .border(1.dp, colors.accent.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("privacy_offline_badge"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              VeilIcons.ShieldCheckOutline(tint = colors.accent, size = 20.dp)
              Text(
                text = "100% Offline & Local — Your notes never leave your device",
                style = VeilTextStyles.meta.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp),
                color = colors.text,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }

        // 2. PROMINENT STAR ON GITHUB BUTTON
        val githubUrl = remoteConfig.socialLinks.githubRepo
        if (RemoteConfigValidator.isValidHttpsUrl(githubUrl)) {
          item {
            val starShape = RoundedCornerShape(18.dp)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, starShape, ambientColor = colors.shadow, spotColor = colors.shadow)
                .clip(starShape)
                .background(colors.accent)
                .tactilePress(scaleDown = 0.97f, hapticOnClick = true, role = Role.Button) {
                  openUrlOrNotify(githubUrl)
                }
                .padding(horizontal = 18.dp, vertical = 15.dp)
                .testTag("star_on_github_button"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
              ) {
                VeilIcons.StarOutline(tint = Color.White, size = 21.dp)
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "⭐ Star on GitHub",
                  style = VeilTextStyles.cardTitle.copy(fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold),
                  color = Color.White
                )
                Text(
                  text = "Support open-source development",
                  style = VeilTextStyles.meta,
                  color = Color.White.copy(alpha = 0.85f)
                )
              }
              VeilIcons.ExternalLinkOutline(tint = Color.White, size = 18.dp)
            }
          }
        }

        // 3. CONNECT & ACTIONS
        item {
          Text(
            text = "CONNECT & ACTIONS",
            style = VeilTextStyles.sectionLabel,
            color = colors.muted,
            modifier = Modifier.padding(top = 4.dp)
          )
        }

        val socialChips = buildList {
          if (remoteConfig.socialLinks.website.isNotEmpty()) add("Website" to remoteConfig.socialLinks.website)
          if (remoteConfig.socialLinks.instagram.isNotEmpty()) add("Instagram" to remoteConfig.socialLinks.instagram)
          if (remoteConfig.socialLinks.telegram.isNotEmpty()) add("Telegram" to remoteConfig.socialLinks.telegram)
          if (remoteConfig.socialLinks.youtube.isNotEmpty()) add("YouTube" to remoteConfig.socialLinks.youtube)
          remoteConfig.socialLinks.customLinks.forEach { item ->
            if (item.enabled && item.label.isNotBlank() && item.url.isNotEmpty()) {
              add(item.label to item.url)
            }
          }
        }

        if (socialChips.isNotEmpty()) {
          item {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              socialChips.forEach { (label, url) ->
                Row(
                  modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.accentSoft, RoundedCornerShape(14.dp))
                    .tactilePress(scaleDown = 0.96f, hapticOnClick = true, role = Role.Button) {
                      openUrlOrNotify(url)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("social_link_${label.lowercase().replace(" ", "_")}"),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(
                    text = label,
                    style = VeilTextStyles.cardTitle.copy(fontSize = 13.5.sp),
                    color = colors.text
                  )
                  VeilIcons.ExternalLinkOutline(tint = colors.muted, size = 14.dp)
                }
              }
            }
          }
        }

        item {
          val groupShape = RoundedCornerShape(16.dp)
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .shadow(2.dp, groupShape, ambientColor = colors.shadow, spotColor = colors.shadow)
              .clip(groupShape)
              .background(colors.surface)
          ) {
            val bugUrl = remoteConfig.contact.bugReportUrl
            val supportEmail = remoteConfig.contact.supportEmail

            if (bugUrl.isNotEmpty() || supportEmail.isNotEmpty()) {
              AboutActionRow(
                title = "Report a Bug",
                subtitle = if (bugUrl.isNotEmpty()) "Open issue tracker" else "Send diagnostic email",
                icon = { VeilIcons.BugOutline(tint = colors.text, size = 21.dp) },
                onClick = {
                  if (bugUrl.isNotEmpty()) {
                    openUrlOrNotify(bugUrl)
                  } else if (!openSafeSupportEmail(context, supportEmail, "Veil Notes v${BuildConfig.VERSION_NAME} Bug Report")) {
                    scope.launch { snackbarHostState.showSnackbar("No email app found.") }
                  }
                },
                tag = "about_report_bug_row"
              )
            }

            if (supportEmail.isNotEmpty()) {
              AboutActionRow(
                title = "Contact Support",
                subtitle = supportEmail,
                icon = { VeilIcons.MailOutline(tint = colors.text, size = 21.dp) },
                onClick = {
                  if (!openSafeSupportEmail(context, supportEmail, "Veil Notes Support")) {
                    NoteImageHelper.copyTextToClipboard(context, "Support Email", supportEmail)
                    scope.launch { snackbarHostState.showSnackbar("Email copied: $supportEmail") }
                  }
                },
                tag = "about_contact_email_row"
              )
            }

            AboutActionRow(
              title = "Share App",
              subtitle = "Share Veil Notes with friends",
              icon = { VeilIcons.ShareOutline(tint = colors.text, size = 21.dp) },
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val shareText = buildString {
                  append("Check out Veil — a fast, minimalist, 100% offline notes app!")
                  if (RemoteConfigValidator.isValidHttpsUrl(githubUrl)) {
                    append("\n$githubUrl")
                  }
                }
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_SUBJECT, "Veil Notes")
                  putExtra(Intent.EXTRA_TEXT, shareText)
                }
                runCatching {
                  context.startActivity(Intent.createChooser(shareIntent, "Share Veil Notes"))
                }
              },
              tag = "about_share_app_row"
            )
          }
        }

        // 4. SUPPORT ME SECTION (shown ONLY when support.enabled == true && has valid methods)
        val support = remoteConfig.support
        if (support.enabled && support.hasAnyActionableMethod) {
          item {
            Text(
              text = "SUPPORT THE CREATOR",
              style = VeilTextStyles.sectionLabel,
              color = colors.muted,
              modifier = Modifier.padding(top = 6.dp)
            )
          }

          item {
            val supportShape = RoundedCornerShape(20.dp)
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, supportShape, ambientColor = colors.shadow, spotColor = colors.shadow)
                .clip(supportShape)
                .background(colors.surface)
                .border(1.dp, colors.accent.copy(alpha = 0.25f), supportShape)
                .padding(18.dp)
                .testTag("support_me_section"),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.accentSoft),
                  contentAlignment = Alignment.Center
                ) {
                  VeilIcons.HeartOutline(tint = colors.accent, size = 21.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = support.title,
                    style = VeilTextStyles.cardTitle.copy(fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.text
                  )
                  if (support.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = support.description,
                      style = VeilTextStyles.meta.copy(fontSize = 13.sp, lineHeight = 18.sp),
                      color = colors.muted
                    )
                  }
                }
              }

              // UPI QR image (only when upiEnabled, valid HTTPS URL, reachable check passed, and load succeeded)
              val isQrReachable = qrUrl.isNotEmpty() && qrReachableMap[qrUrl] == true
              if (support.upiEnabled && isQrReachable && !qrImageLoadFailed) {
                Box(
                  modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, colors.accentSoft, RoundedCornerShape(16.dp))
                    .padding(10.dp)
                    .testTag("support_upi_qr_image"),
                  contentAlignment = Alignment.Center
                ) {
                  AsyncImage(
                    model = ImageRequest.Builder(context)
                      .data(qrUrl)
                      .diskCachePolicy(CachePolicy.ENABLED)
                      .memoryCachePolicy(CachePolicy.ENABLED)
                      .crossfade(true)
                      .build(),
                    contentDescription = "UPI QR code",
                    contentScale = ContentScale.Fit,
                    onError = { qrImageLoadFailed = true },
                    modifier = Modifier.fillMaxSize()
                  )
                }
              }

              // UPI ID copy + native UPI intent
              if (support.upiEnabled && RemoteConfigValidator.isValidUpiId(support.upiId)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(14.dp))
                      .background(colors.bg.copy(alpha = 0.85f))
                      .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "UPI ID",
                        style = VeilTextStyles.labelChip,
                        color = colors.muted
                      )
                      Text(
                        text = support.upiId,
                        style = VeilTextStyles.cardTitle.copy(fontSize = 14.sp),
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                    IconButton(
                      onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        NoteImageHelper.copyTextToClipboard(context, "UPI ID", support.upiId)
                        scope.launch { snackbarHostState.showSnackbar("UPI ID copied") }
                      },
                      modifier = Modifier
                        .size(44.dp)
                        .semantics { contentDescription = "Copy UPI ID" }
                        .testTag("copy_upi_id_button")
                    ) {
                      VeilIcons.CopyOutline(tint = colors.accent, size = 19.dp)
                    }
                  }

                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(16.dp))
                      .background(colors.accent)
                      .tactilePress(scaleDown = 0.97f, hapticOnClick = true, role = Role.Button) {
                        val upiUri = RemoteConfigValidator.buildUpiPaymentUri(
                          upiId = support.upiId,
                          payeeName = remoteConfig.about.creatorName
                        )
                        val launched = upiUri != null && runCatching {
                          val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
                          context.startActivity(Intent.createChooser(upiIntent, "Pay with UPI"))
                          true
                        }.getOrDefault(false)
                        if (!launched) {
                          NoteImageHelper.copyTextToClipboard(context, "UPI ID", support.upiId)
                          scope.launch { snackbarHostState.showSnackbar("No UPI app found — UPI ID copied") }
                        }
                      }
                      .padding(vertical = 13.dp)
                      .testTag("pay_via_upi_button"),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "Pay via UPI App",
                      style = VeilTextStyles.cardTitle.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                      color = Color.White
                    )
                  }
                }
              }

              // External Payment Link
              if (support.paymentLinkEnabled && RemoteConfigValidator.isValidHttpsUrl(support.paymentUrl)) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.accentSoft)
                    .border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .tactilePress(scaleDown = 0.97f, hapticOnClick = true, role = Role.Button) {
                      openUrlOrNotify(support.paymentUrl)
                    }
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .testTag("support_payment_link_button"),
                  contentAlignment = Alignment.Center
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Text(
                      text = "Support via Payment Link",
                      style = VeilTextStyles.cardTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                      color = colors.accent
                    )
                    VeilIcons.ExternalLinkOutline(tint = colors.accent, size = 15.dp)
                  }
                }
              }

              // Thank-you / Custom Message
              if (support.showCustomMessage && support.customMessage.isNotBlank()) {
                Text(
                  text = support.customMessage,
                  style = VeilTextStyles.meta.copy(fontSize = 13.sp, lineHeight = 18.sp),
                  color = colors.muted,
                  textAlign = TextAlign.Center,
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.bg.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                )
              }
            }
          }
        }

        // 5. LEGAL LINKS (only render valid configured URLs)
        val hasPrivacy = remoteConfig.legal.privacyPolicyUrl.isNotEmpty()
        val hasTerms = remoteConfig.legal.termsUrl.isNotEmpty()
        if (hasPrivacy || hasTerms) {
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (hasPrivacy) {
                Text(
                  text = "Privacy Policy",
                  style = VeilTextStyles.meta.copy(fontWeight = FontWeight.Medium),
                  color = colors.muted,
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) { openUrlOrNotify(remoteConfig.legal.privacyPolicyUrl) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("legal_privacy_policy_link")
                )
              }
              if (hasTerms) {
                Text(
                  text = "Terms of Service",
                  style = VeilTextStyles.meta.copy(fontWeight = FontWeight.Medium),
                  color = colors.muted,
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) { openUrlOrNotify(remoteConfig.legal.termsUrl) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("legal_terms_link")
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AboutActionRow(
  title: String,
  subtitle: String,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
  tag: String
) {
  val colors = VeilThemeTokens.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 14.dp)
      .testTag(tag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    icon()
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = VeilTextStyles.cardTitle, color = colors.text)
      Spacer(modifier = Modifier.height(2.dp))
      Text(subtitle, style = VeilTextStyles.meta, color = colors.muted)
    }
    VeilIcons.ChevronRight(tint = colors.muted, size = 18.dp)
  }
}
