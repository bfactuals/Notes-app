package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.components.VeilIcons
import com.example.ui.components.tactilePress
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens
import com.example.viewmodel.VeilViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
  viewModel: VeilViewModel,
  onBack: () -> Unit
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
  val proFeatures by viewModel.proFeatures.collectAsState()
  val fontSize by viewModel.fontSize.collectAsState()
  val fontStyle by viewModel.fontStyle.collectAsState()
  val hasExistingPassword = secretPasswordHash.isNotEmpty()

  var showSecretModal by remember { mutableStateOf(false) }
  var showThemeDialog by remember { mutableStateOf(false) }
  var showColorDialog by remember { mutableStateOf(false) }

  // Stealth Version 1.0 tap counter:
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

  if (showThemeDialog) {
    AlertDialog(
      onDismissRequest = { showThemeDialog = false },
      containerColor = colors.surface,
      titleContentColor = colors.text,
      textContentColor = colors.text,
      title = { Text("Theme", style = VeilTextStyles.cardTitle) },
      text = {
        Column {
          listOf("system" to "Follow System", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  viewModel.setThemeMode(key)
                  showThemeDialog = false
                }
                .padding(vertical = 10.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = themeMode == key,
                onClick = {
                  viewModel.setThemeMode(key)
                  showThemeDialog = false
                },
                colors = RadioButtonDefaults.colors(selectedColor = colors.accent, unselectedColor = colors.muted)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.text)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showThemeDialog = false }) {
          Text("Close", color = colors.accent)
        }
      }
    )
  }

  if (showColorDialog) {
    AlertDialog(
      onDismissRequest = { showColorDialog = false },
      containerColor = colors.surface,
      titleContentColor = colors.text,
      textContentColor = colors.text,
      title = { Text("Default Note Color", style = VeilTextStyles.cardTitle) },
      text = {
        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
          val options = listOf(
            "plain" to "Default",
            "yellow" to "Yellow",
            "lavender" to "Lavender",
            "mint" to "Mint",
            "peach" to "Peach",
            "blue" to "Blue",
            "pink" to "Pink",
            "teal" to "Teal",
            "coral" to "Coral",
            "stone" to "Stone"
          )
          items(options.size) { idx ->
            val (key, label) = options[idx]
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  viewModel.setDefaultNoteColor(key)
                  showColorDialog = false
                }
                .padding(vertical = 8.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = defaultColor == key,
                onClick = {
                  viewModel.setDefaultNoteColor(key)
                  showColorDialog = false
                },
                colors = RadioButtonDefaults.colors(selectedColor = colors.accent, unselectedColor = colors.muted)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.text)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showColorDialog = false }) {
          Text("Close", color = colors.accent)
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          Text(
            text = "APPEARANCE",
            style = VeilTextStyles.sectionLabel,
            color = colors.muted,
            modifier = Modifier.padding(top = 4.dp)
          )
        }

        item {
          SettingsCardGroup {
            SettingsRow(
              title = "Theme",
              subtitle = when (themeMode) {
                "light" -> "Light"
                "dark" -> "Dark"
                else -> "Follow System"
              },
              icon = { VeilIcons.SunMoonMorph(isDark = colors.isDark, tint = colors.text, size = 22.dp) },
              onClick = { showThemeDialog = true },
              tag = "theme_setting_item"
            )
            SettingsRow(
              title = "Default Note Color",
              subtitle = defaultColor.replaceFirstChar { it.uppercase() },
              icon = { VeilIcons.PaletteOutline(tint = colors.text, size = 22.dp) },
              onClick = { showColorDialog = true },
              tag = "default_color_setting_item"
            )
            SettingsSwitchRow(
              title = "Grid View",
              subtitle = "2-column masonry layout",
              checked = isGridView,
              onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setGridView(it)
              },
              icon = { VeilIcons.GridOutline(tint = colors.text, size = 22.dp) },
              tag = "grid_view_switch"
            )
          }
        }

        item {
          Text(
            text = "TYPOGRAPHY",
            style = VeilTextStyles.sectionLabel,
            color = colors.muted,
            modifier = Modifier.padding(top = 8.dp)
          )
        }

        item {
          SettingsCardGroup {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              // Font Size segmented control
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  VeilIcons.TypeOutline(tint = colors.text, size = 20.dp)
                  Text("Font Size", style = VeilTextStyles.cardTitle, color = colors.text)
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  listOf("small" to "Small", "medium" to "Medium", "large" to "Large").forEach { (key, label) ->
                    val active = fontSize == key
                    SegmentedOptionPill(
                      label = label,
                      active = active,
                      onClick = { viewModel.setFontSize(key) },
                      tag = "font_size_$key",
                      modifier = Modifier.weight(1f)
                    )
                  }
                }
              }

              HorizontalDivider(color = colors.accentSoft, thickness = 1.dp)

              // Font Style segmented control
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Font Style", style = VeilTextStyles.cardTitle, color = colors.text)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  listOf(
                    Triple("sans", "Sans", InterFontFamily),
                    Triple("serif", "Serif", FontFamily.Serif),
                    Triple("mono", "Mono", FontFamily.Monospace)
                  ).forEach { (key, label, family) ->
                    val active = fontStyle == key
                    SegmentedOptionPill(
                      label = label,
                      active = active,
                      fontFamily = family,
                      onClick = { viewModel.setFontStyle(key) },
                      tag = "font_style_$key",
                      modifier = Modifier.weight(1f)
                    )
                  }
                }
              }

              // Subtle live typography preview
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(colors.bg.copy(alpha = 0.72f))
                  .padding(horizontal = 12.dp, vertical = 10.dp)
              ) {
                Text(
                  text = "Quick brown fox — 450 + 1200 * 2 = 2850",
                  style = VeilThemeTokens.cardBodyStyle,
                  color = colors.text
                )
              }
            }
          }
        }

        item {
          Text(
            text = "PRO FEATURES",
            style = VeilTextStyles.sectionLabel,
            color = colors.muted,
            modifier = Modifier.padding(top = 8.dp)
          )
        }

        item {
          SettingsCardGroup {
            SettingsSwitchRow(
              title = "Enable All Pro Features",
              subtitle = "Toggle all modular power tools at once",
              checked = proFeatures.allEnabled,
              onCheckedChange = { enabled ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setAllProFeatures(enabled)
              },
              icon = { VeilIcons.SparklesOutline(tint = colors.accent, size = 22.dp) },
              tag = "pro_enable_all_switch"
            )
            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 14.dp),
              color = colors.accentSoft,
              thickness = 1.dp
            )
            SettingsSwitchRow(
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
            SettingsSwitchRow(
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
            SettingsSwitchRow(
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
            SettingsSwitchRow(
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
            SettingsSwitchRow(
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
            SettingsSwitchRow(
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
            SettingsSwitchRow(
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

        item {
          Text(
            text = "NOTES & SHARING",
            style = VeilTextStyles.sectionLabel,
            color = colors.muted,
            modifier = Modifier.padding(top = 8.dp)
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
            SettingsSwitchRow(
              title = "Move ticked checklist items to bottom",
              subtitle = "Keep unchecked items at the top of lists",
              checked = moveTickedToBottom,
              onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setMoveTickedToBottom(it)
              },
              icon = { VeilIcons.Checklist(tint = colors.text, size = 22.dp) },
              tag = "move_ticked_bottom_switch"
            )
            SettingsSwitchRow(
              title = "Haptic feedback",
              subtitle = "Tactile vibration for taps, pin hold, and toolbars",
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
            text = "PRIVACY",
            style = VeilTextStyles.sectionLabel,
            color = colors.muted,
            modifier = Modifier.padding(top = 8.dp)
          )
        }

        item {
          SettingsCardGroup {
            // Keep Panic Hide; NO Secret Password option anywhere in Settings!
            SettingsRow(
              title = "Panic Hide",
              subtitle = "Quickly clear active search & reset view",
              icon = { VeilIcons.EyeOff(tint = colors.text, size = 22.dp) },
              onClick = {
                viewModel.panicHide()
                scope.launch { snackbarHostState.showSnackbar("View cleared.") }
              },
              tag = "panic_mode_setting_item"
            )
          }
        }

        item {
          Text(
            text = "BACKUP & RESTORE",
            style = VeilTextStyles.sectionLabel,
            color = colors.muted,
            modifier = Modifier.padding(top = 8.dp)
          )
        }

        item {
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
        }

        item {
          Text(
            text = "ABOUT",
            style = VeilTextStyles.sectionLabel,
            color = colors.muted,
            modifier = Modifier.padding(top = 8.dp)
          )
        }

        item {
          SettingsCardGroup {
            // Zero-hint stealth tap trigger: 5 rapid taps if unset, 7 rapid taps if already set
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
                Text("Version 1.0", style = VeilTextStyles.cardTitle, color = colors.text)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Notes · Offline-first", style = VeilTextStyles.meta, color = colors.muted)
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
  tag: String
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
        checkedThumbColor = colors.surface,
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
