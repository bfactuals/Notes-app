package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.components.ProGiftModalDialog
import com.example.ui.components.VeilIcons
import com.example.ui.components.tactilePress
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.NoteColorOrder
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens
import com.example.viewmodel.VeilViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: VeilViewModel,
  onBack: () -> Unit,
  onOpenProFeatures: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val themeMode by viewModel.themeMode.collectAsState()
  val isGridView by viewModel.isGridView.collectAsState()
  val defaultColor by viewModel.defaultNoteColor.collectAsState()
  val secretPasswordHash by viewModel.secretPasswordHash.collectAsState()
  val enableSharing by viewModel.enableSharing.collectAsState()
  val moveTickedToBottom by viewModel.moveTickedToBottom.collectAsState()
  val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsState()
  val proGiftClaimed by viewModel.proGiftClaimed.collectAsState()
  val fontSize by viewModel.fontSize.collectAsState()
  val fontStyle by viewModel.fontStyle.collectAsState()
  val hasExistingPassword = secretPasswordHash.isNotEmpty()

  var showSecretModal by remember { mutableStateOf(false) }
  var showProGiftModal by remember { mutableStateOf(false) }
  var showAppearanceSheet by remember { mutableStateOf(false) }
  var showTypographySheet by remember { mutableStateOf(false) }

  // Stealth Version tap counter (100% intact):
  // - No password yet: 5 rapid taps -> Create Secret Code
  // - Password already set: 5 taps does NOTHING -> 7 rapid taps -> Change Secret Code (verifies old first)
  var versionTapCount by remember { mutableIntStateOf(0) }
  var lastVersionTapMs by remember { mutableLongStateOf(0L) }

  // Encrypted Backup & Restore state
  var pendingExportPassword by remember { mutableStateOf<String?>(null) }
  var showExportPasswordDialog by remember { mutableStateOf(false) }
  var exportPasswordError by remember { mutableStateOf("") }

  var pendingRestoreBytes by remember { mutableStateOf<ByteArray?>(null) }
  var showRestorePasswordDialog by remember { mutableStateOf(false) }
  var restorePasswordError by remember { mutableStateOf("") }

  val createBackupLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/octet-stream")
  ) { uri ->
    val pwd = pendingExportPassword
    pendingExportPassword = null
    if (uri != null && pwd != null) {
      scope.launch {
        val result = viewModel.exportEncryptedBackup(pwd)
        result.onSuccess { bytes ->
          runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
              out.write(bytes)
              out.flush()
            }
          }.onSuccess {
            snackbarHostState.showSnackbar("Encrypted backup saved.")
          }.onFailure {
            snackbarHostState.showSnackbar("Failed to write backup file.")
          }
        }.onFailure {
          snackbarHostState.showSnackbar("Backup encryption failed.")
        }
      }
    }
  }

  val openBackupLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      val bytes = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
      }.getOrNull()
      if (bytes != null && bytes.isNotEmpty()) {
        pendingRestoreBytes = bytes
        restorePasswordError = ""
        showRestorePasswordDialog = true
      } else {
        scope.launch {
          snackbarHostState.showSnackbar("Incorrect password or invalid file.")
        }
      }
    }
  }

  // Opening Pro Features when unclaimed opens the "Veil Pro, on us" Gift Modal first — never auto-unlocks!
  if (showProGiftModal) {
    ProGiftModalDialog(
      onClaim = {
        showProGiftModal = false
        viewModel.claimFreeProGift()
        onOpenProFeatures()
      },
      onDismiss = {
        showProGiftModal = false
      }
    )
  }

  if (showSecretModal) {
    SecretPasswordDialog(
      hasExistingPassword = hasExistingPassword,
      onVerifyOldPassword = { candidate -> viewModel.verifyCurrentPassword(candidate) },
      onDismiss = { showSecretModal = false },
      onSave = { newPwd ->
        viewModel.setSecretPassword(newPwd)
        showSecretModal = false
      }
    )
  }

  if (showExportPasswordDialog) {
    BackupPasswordDialog(
      title = "Encrypt Backup",
      description = if (hasExistingPassword) {
        "Enter your password to encrypt all notes with AES-GCM."
      } else {
        "Enter a backup password to encrypt all notes with AES-GCM."
      },
      confirmLabel = "Export",
      errorText = exportPasswordError,
      onDismiss = {
        showExportPasswordDialog = false
        exportPasswordError = ""
      },
      onConfirm = { pwd ->
        scope.launch {
          if (hasExistingPassword) {
            val matches = viewModel.verifyCurrentPassword(pwd)
            if (!matches) {
              exportPasswordError = "Incorrect password."
              return@launch
            }
          }
          exportPasswordError = ""
          showExportPasswordDialog = false
          pendingExportPassword = pwd
          createBackupLauncher.launch("notes_backup.enc")
        }
      }
    )
  }

  if (showRestorePasswordDialog && pendingRestoreBytes != null) {
    BackupPasswordDialog(
      title = "Restore Encrypted Backup",
      description = "Enter the password used to encrypt this backup file.",
      confirmLabel = "Restore",
      errorText = restorePasswordError,
      onDismiss = {
        showRestorePasswordDialog = false
        pendingRestoreBytes = null
        restorePasswordError = ""
      },
      onConfirm = { pwd ->
        val bytes = pendingRestoreBytes ?: return@BackupPasswordDialog
        scope.launch {
          val result = viewModel.restoreEncryptedBackup(bytes, pwd)
          result.onSuccess { count ->
            showRestorePasswordDialog = false
            pendingRestoreBytes = null
            restorePasswordError = ""
            snackbarHostState.showSnackbar("Restored $count notes.")
          }.onFailure {
            restorePasswordError = "Incorrect password or invalid file."
          }
        }
      }
    )
  }

  // Compact Appearance Bottom Sheet (Theme, Default Note Color, Grid/List Layout)
  if (showAppearanceSheet) {
    ModalBottomSheet(
      onDismissRequest = { showAppearanceSheet = false },
      containerColor = colors.surface,
      contentColor = colors.text,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp)
          .testTag("appearance_bottom_sheet"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        Text("Appearance", style = VeilTextStyles.editorTitle.copy(fontSize = 20.sp), color = colors.text)

        // Theme selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("THEME", style = VeilTextStyles.sectionLabel, color = colors.muted)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
              SegmentedOptionPill(
                label = label,
                active = themeMode == key,
                onClick = { viewModel.setThemeMode(key) },
                tag = "sheet_theme_$key",
                modifier = Modifier.weight(1f)
              )
            }
          }
        }

        // Default Note Color swatches
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("DEFAULT NOTE COLOR", style = VeilTextStyles.sectionLabel, color = colors.muted)
            Text(
              text = if (defaultColor == "plain") "Default" else defaultColor.replaceFirstChar { it.uppercase() },
              style = VeilTextStyles.labelChip,
              color = colors.accent
            )
          }
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
          ) {
            items(NoteColorOrder.size) { idx ->
              val key = NoteColorOrder[idx]
              val swatchColor = colors.noteCardColor(key)
              val active = defaultColor == key
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(swatchColor)
                  .border(
                    width = if (active) 2.5.dp else 1.dp,
                    color = if (active) colors.accent else colors.muted.copy(alpha = 0.35f),
                    shape = CircleShape
                  )
                  .tactilePress(scaleDown = 0.88f, hapticOnClick = true, role = Role.RadioButton) {
                    viewModel.setDefaultNoteColor(key)
                  }
                  .semantics { contentDescription = "Default color $key" }
                  .testTag("sheet_color_$key"),
                contentAlignment = Alignment.Center
              ) {
                if (active) {
                  VeilIcons.CheckOutline(tint = colors.accent, size = 17.dp)
                }
              }
            }
          }
        }

        HorizontalDivider(color = colors.accentSoft, thickness = 1.dp)

        // Layout Grid vs List toggle
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Switch) {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              viewModel.setGridView(!isGridView)
            }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          VeilIcons.GridOutline(tint = colors.text, size = 21.dp)
          Column(modifier = Modifier.weight(1f)) {
            Text("2-Column Grid", style = VeilTextStyles.cardTitle, color = colors.text)
            Text("Switch between masonry grid and single-column list", style = VeilTextStyles.meta, color = colors.muted)
          }
          Switch(
            checked = isGridView,
            onCheckedChange = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              viewModel.setGridView(it)
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = colors.accent,
              uncheckedThumbColor = colors.muted,
              uncheckedTrackColor = colors.bg
            ),
            modifier = Modifier.testTag("grid_view_switch")
          )
        }
      }
    }
  }

  // Compact Typography Bottom Sheet (Font Size, Font Style, Live Preview)
  if (showTypographySheet) {
    ModalBottomSheet(
      onDismissRequest = { showTypographySheet = false },
      containerColor = colors.surface,
      contentColor = colors.text,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp)
          .testTag("typography_bottom_sheet"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text("Typography", style = VeilTextStyles.editorTitle.copy(fontSize = 20.sp), color = colors.text)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("FONT SIZE", style = VeilTextStyles.sectionLabel, color = colors.muted)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("small" to "Small", "medium" to "Medium", "large" to "Large").forEach { (key, label) ->
              SegmentedOptionPill(
                label = label,
                active = fontSize == key,
                onClick = { viewModel.setFontSize(key) },
                tag = "font_size_$key",
                modifier = Modifier.weight(1f)
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("FONT STYLE", style = VeilTextStyles.sectionLabel, color = colors.muted)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(
              Triple("sans", "Sans-Serif", InterFontFamily),
              Triple("serif", "Serif", FontFamily.Serif),
              Triple("mono", "Mono", FontFamily.Monospace)
            ).forEach { (key, label, family) ->
              SegmentedOptionPill(
                label = label,
                active = fontStyle == key,
                fontFamily = family,
                onClick = { viewModel.setFontStyle(key) },
                tag = "font_style_$key",
                modifier = Modifier.weight(1f)
              )
            }
          }
        }

        // Live preview card
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.bg.copy(alpha = 0.8f))
            .border(1.dp, colors.accentSoft, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Live Preview",
              style = VeilThemeTokens.cardTitleStyle,
              color = colors.text
            )
            Text(
              text = "Quick brown fox — 450 + 1200 * 2 = 2850",
              style = VeilThemeTokens.cardBodyStyle,
              color = colors.muted
            )
          }
        }
      }
    }
  }

  val themeLabel = when (themeMode) {
    "light" -> "Light"
    "dark" -> "Dark"
    else -> "System"
  }
  val colorLabel = if (defaultColor == "plain") "Default" else defaultColor.replaceFirstChar { it.uppercase() }
  val layoutLabel = if (isGridView) "2-Column Grid" else "List View"
  val appearanceSummary = "$themeLabel · $colorLabel · $layoutLabel"

  val fontSizeLabel = when (fontSize) {
    "small" -> "Small"
    "large" -> "Large"
    else -> "Medium"
  }
  val fontStyleLabel = when (fontStyle) {
    "serif" -> "Serif"
    "mono" -> "Monospace"
    else -> "Sans-Serif"
  }
  val typographySummary = "$fontSizeLabel · $fontStyleLabel"

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
            .testTag("settings_back_button")
        ) {
          VeilIcons.ArrowBack(tint = colors.text, size = 24.dp)
        }
        Text(
          text = "Settings",
          style = VeilTextStyles.editorTitle,
          color = colors.text
        )
      }

      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Pro Features Card at top — opens "Veil Pro, on us" Gift Modal first when unclaimed (never auto-unlocks!)
        item {
          val proCardShape = RoundedCornerShape(18.dp)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .shadow(3.dp, proCardShape, ambientColor = colors.shadow, spotColor = colors.shadow)
              .clip(proCardShape)
              .background(colors.accentSoft.copy(alpha = if (colors.isDark) 0.58f else 0.78f))
              .border(1.dp, colors.accent.copy(alpha = 0.4f), proCardShape)
              .tactilePress(scaleDown = 0.97f, hapticOnClick = true, role = Role.Button) {
                if (!proGiftClaimed) {
                  showProGiftModal = true
                } else {
                  onOpenProFeatures()
                }
              }
              .padding(horizontal = 16.dp, vertical = 15.dp)
              .testTag("settings_pro_features_card"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.accent.copy(alpha = 0.16f)),
              contentAlignment = Alignment.Center
            ) {
              VeilIcons.CrownOutline(tint = colors.accent, size = 21.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Pro Features",
                style = VeilTextStyles.cardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                color = colors.text
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Self-destruct timer, calculator, history & more",
                style = VeilTextStyles.meta,
                color = colors.muted
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (proGiftClaimed) colors.accent else colors.surface)
                .border(1.dp, colors.accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
              Text(
                text = if (proGiftClaimed) "PRO ACTIVE" else "GIFT",
                style = VeilTextStyles.labelChip.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = if (proGiftClaimed) Color.White else colors.accent
              )
            }
          }
        }

        // Compact Display section (single-row Appearance + single-row Typography -> Bottom Sheets)
        item {
          Text(
            text = "DISPLAY",
            style = VeilTextStyles.sectionLabel,
            color = colors.muted,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
          )
        }

        item {
          SettingsCardGroup {
            SettingsRow(
              title = "Appearance",
              subtitle = appearanceSummary,
              icon = { VeilIcons.PaletteOutline(tint = colors.text, size = 22.dp) },
              trailing = { VeilIcons.ChevronRight(tint = colors.muted, size = 18.dp) },
              onClick = { showAppearanceSheet = true },
              tag = "appearance_setting_item"
            )
            HorizontalDivider(
              modifier = Modifier.padding(start = 52.dp, end = 14.dp),
              color = colors.accentSoft,
              thickness = 1.dp
            )
            SettingsRow(
              title = "Typography",
              subtitle = typographySummary,
              icon = { VeilIcons.TypeOutline(tint = colors.text, size = 21.dp) },
              trailing = { VeilIcons.ChevronRight(tint = colors.muted, size = 18.dp) },
              onClick = { showTypographySheet = true },
              tag = "typography_setting_item"
            )
          }
        }

        item {
          Text(
            text = "BEHAVIOR",
            style = VeilTextStyles.sectionLabel,
            color = colors.muted,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
          )
        }

        item {
          SettingsCardGroup {
            SettingsSwitchRow(
              title = "Enable Sharing / Send",
              subtitle = "Show share options for notes and images",
              checked = enableSharing,
              onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setEnableSharing(it)
              },
              icon = { VeilIcons.ShareOutline(tint = colors.text, size = 22.dp) },
              tag = "enable_sharing_switch"
            )
            HorizontalDivider(
              modifier = Modifier.padding(start = 52.dp, end = 14.dp),
              color = colors.accentSoft,
              thickness = 1.dp
            )
            SettingsSwitchRow(
              title = "Move ticked items to bottom",
              subtitle = "Keep unchecked items at the top of checklists",
              checked = moveTickedToBottom,
              onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setMoveTickedToBottom(it)
              },
              icon = { VeilIcons.Checklist(tint = colors.text, size = 22.dp) },
              tag = "move_ticked_bottom_switch"
            )
            HorizontalDivider(
              modifier = Modifier.padding(start = 52.dp, end = 14.dp),
              color = colors.accentSoft,
              thickness = 1.dp
            )
            SettingsSwitchRow(
              title = "Haptic feedback",
              subtitle = "Tactile vibration for taps, pin hold, and drag",
              checked = hapticFeedbackEnabled,
              onCheckedChange = { enabled ->
                viewModel.setHapticFeedbackEnabled(enabled)
                if (enabled) {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
              },
              icon = { VeilIcons.InfoOutline(tint = colors.text, size = 22.dp) },
              tag = "haptic_feedback_switch"
            )
          }
        }

        item {
          Text(
            text = "ENCRYPTED BACKUP & RESTORE",
            style = VeilTextStyles.sectionLabel,
            color = colors.muted,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
          )
        }

        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCardGroup {
              SettingsRow(
                title = "Export Encrypted Backup",
                subtitle = "Save AES-GCM encrypted backup file",
                icon = { VeilIcons.BackupOutline(tint = colors.text, size = 22.dp) },
                onClick = {
                  exportPasswordError = ""
                  showExportPasswordDialog = true
                },
                tag = "export_backup_item"
              )
              HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp, end = 14.dp),
                color = colors.accentSoft,
                thickness = 1.dp
              )
              SettingsRow(
                title = "Restore Encrypted Backup",
                subtitle = "Import notes from encrypted backup file",
                icon = { VeilIcons.ImportOutline(tint = colors.text, size = 22.dp) },
                onClick = {
                  openBackupLauncher.launch(arrayOf("*/*"))
                },
                tag = "restore_backup_item"
              )
            }

            Text(
              text = "Switching phones or reinstalling? Export an encrypted backup first so you never lose your notes.",
              style = VeilTextStyles.meta.copy(lineHeight = 17.sp),
              color = colors.muted,
              modifier = Modifier.padding(horizontal = 6.dp)
            )
          }
        }

        // Stealth Version row (5 rapid taps when unset, 7 rapid taps when already set — zero hint)
        item {
          Spacer(modifier = Modifier.height(2.dp))
          SettingsCardGroup {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  onClick = {
                    val now = System.currentTimeMillis()
                    if (now - lastVersionTapMs > 1200L) {
                      versionTapCount = 1
                    } else {
                      versionTapCount += 1
                    }
                    lastVersionTapMs = now

                    val requiredTaps = if (hasExistingPassword) 7 else 5
                    if (versionTapCount >= requiredTaps) {
                      versionTapCount = 0
                      showSecretModal = true
                    }
                  }
                )
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .testTag("about_version_item"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              VeilIcons.InfoOutline(tint = colors.text, size = 22.dp)
              Column(modifier = Modifier.weight(1f)) {
                Text("Version ${BuildConfig.VERSION_NAME}", style = VeilTextStyles.cardTitle, color = colors.text)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Veil Notes · Offline-first", style = VeilTextStyles.meta, color = colors.muted)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsCardGroup(content: @Composable ColumnScope.() -> Unit) {
  val colors = VeilThemeTokens.colors
  val shape = RoundedCornerShape(16.dp)
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(2.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
      .clip(shape)
      .background(colors.surface)
  ) {
    content()
  }
}

@Composable
private fun SettingsRow(
  title: String,
  subtitle: String,
  icon: @Composable () -> Unit,
  onClick: (() -> Unit)?,
  tag: String,
  trailing: (@Composable () -> Unit)? = null
) {
  val colors = VeilThemeTokens.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(
        if (onClick != null) {
          Modifier.clickable(role = Role.Button, onClick = onClick)
        } else {
          Modifier
        }
      )
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
    trailing?.invoke()
  }
}

@Composable
private fun SettingsSwitchRow(
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
      .padding(horizontal = 14.dp, vertical = 12.dp),
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
        checkedThumbColor = Color.White,
        checkedTrackColor = colors.accent,
        uncheckedThumbColor = colors.muted,
        uncheckedTrackColor = colors.bg
      ),
      modifier = Modifier.testTag(tag)
    )
  }
}

@Composable
private fun SegmentedOptionPill(
  label: String,
  active: Boolean,
  onClick: () -> Unit,
  tag: String,
  modifier: Modifier = Modifier,
  fontFamily: FontFamily? = null
) {
  val colors = VeilThemeTokens.colors
  val shape = RoundedCornerShape(14.dp)
  Box(
    modifier = modifier
      .height(38.dp)
      .clip(shape)
      .background(if (active) colors.accentSoft else colors.bg.copy(alpha = 0.72f))
      .then(
        if (active) Modifier.border(1.dp, colors.accent.copy(alpha = 0.6f), shape) else Modifier
      )
      .tactilePress(scaleDown = 0.94f, hapticOnClick = true, role = Role.RadioButton, onClick = onClick)
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      style = VeilTextStyles.cardBody.copy(
        fontSize = 13.5.sp,
        fontFamily = fontFamily ?: InterFontFamily,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
      ),
      color = if (active) colors.accent else colors.text
    )
  }
}
