package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.NoteReminderScheduler
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.ConfettiCelebrationOverlay
import com.example.ui.components.MaintenanceOverlayScreen
import com.example.ui.components.ProGiftModalDialog
import com.example.ui.components.ThemeRevealContainer
import com.example.ui.screens.*
import com.example.ui.theme.VeilEasing
import com.example.ui.theme.VeilTheme
import com.example.ui.theme.VeilThemeTokens
import com.example.viewmodel.VeilViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
  private val viewModel: VeilViewModel by viewModels()
  private val pendingNotificationNoteId = MutableStateFlow<Long?>(null)

  override fun onResume() {
    super.onResume()
    viewModel.onAppForegrounded()
  }

  override fun onPause() {
    super.onPause()
    // Leaving the app automatically conceals any unlocked hidden notes (stealth panic behavior)
    viewModel.panicHide()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    extractNotificationNoteId(intent)
  }

  private fun extractNotificationNoteId(intent: Intent?) {
    val noteId = intent?.getLongExtra(NoteReminderScheduler.EXTRA_NOTE_ID, 0L) ?: 0L
    if (noteId > 0L) {
      pendingNotificationNoteId.value = noteId
      intent?.removeExtra(NoteReminderScheduler.EXTRA_NOTE_ID)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    extractNotificationNoteId(intent)

    setContent {
      val themeMode by viewModel.themeMode.collectAsState()
      val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsState()
      val fontSize by viewModel.fontSize.collectAsState()
      val fontStyle by viewModel.fontStyle.collectAsState()
      val firstInstallTimeMs by viewModel.firstInstallTimeMs.collectAsState()
      val proGiftResolved by viewModel.proGiftResolved.collectAsState()
      val proCelebrationEvent by viewModel.proCelebrationEvent.collectAsState()
      val openNoteFromNotification by pendingNotificationNoteId.collectAsState()
      val remoteConfig by viewModel.remoteConfig.collectAsState()
      val isRefreshingConfig by viewModel.isRefreshingConfig.collectAsState()
      val availableUpdate by viewModel.availableUpdate.collectAsState()
      val isMandatoryUpdate by viewModel.isMandatoryUpdate.collectAsState()
      val isOptionalUpdateDismissed by viewModel.isOptionalUpdateDismissed.collectAsState()
      val updateDownloadState by viewModel.updateDownloadState.collectAsState()

      val systemDark = isSystemInDarkTheme()
      val isDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemDark
      }

      // Sync Android status/nav bar appearance with theme
      val view = LocalView.current
      if (!view.isInEditMode) {
        SideEffect {
          val window = (view.context as? Activity)?.window
          if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
          }
        }
      }

      var revealTrigger by remember { mutableIntStateOf(0) }
      var revealOrigin by remember { mutableStateOf(Offset.Unspecified) }
      var showProGiftModal by remember { mutableStateOf(false) }

      // 1-Minute (60s) Post-Install Welcome Gift Popup — shown ONLY ONCE ever
      LaunchedEffect(firstInstallTimeMs, proGiftResolved) {
        if (firstInstallTimeMs > 0L && !proGiftResolved) {
          val targetMs = firstInstallTimeMs + 60_000L
          val waitMs = targetMs - System.currentTimeMillis()
          if (waitMs > 0L) {
            delay(waitMs)
          }
          if (!viewModel.proGiftResolved.value) {
            showProGiftModal = true
          }
        } else {
          showProGiftModal = false
        }
      }

      VeilTheme(
        darkTheme = isDark,
        hapticEnabled = hapticFeedbackEnabled,
        fontSize = fontSize,
        fontStyle = fontStyle
      ) {
        val reduceMotion = VeilThemeTokens.reduceMotion
        val animDuration = if (reduceMotion) 0 else 250

        Box(modifier = Modifier.fillMaxSize()) {
          ThemeRevealContainer(
            revealTrigger = revealTrigger,
            revealOrigin = revealOrigin,
            isDarkTheme = isDark
          ) {
            // NEVER show a Secret Code popup on launch or restart!
            val navController = rememberNavController()

            // Deep-link directly to the note when tapping a native Android Reminder notification
            LaunchedEffect(openNoteFromNotification) {
              val targetId = openNoteFromNotification
              if (targetId != null && targetId > 0L) {
                pendingNotificationNoteId.value = null
                viewModel.clearSelection()
                navController.navigate("editor/$targetId?mode=text") {
                  launchSingleTop = true
                }
              }
            }

            NavHost(
              navController = navController,
              startDestination = "home",
              enterTransition = {
                fadeIn(tween(animDuration, easing = VeilEasing)) +
                  scaleIn(initialScale = 0.96f, animationSpec = tween(animDuration, easing = VeilEasing))
              },
              exitTransition = {
                fadeOut(tween(animDuration, easing = VeilEasing))
              },
              popEnterTransition = {
                fadeIn(tween(animDuration, easing = VeilEasing))
              },
              popExitTransition = {
                fadeOut(tween(animDuration, easing = VeilEasing)) +
                  scaleOut(targetScale = 0.96f, animationSpec = tween(animDuration, easing = VeilEasing))
              }
            ) {
              composable("home") {
                HomeScreen(
                  viewModel = viewModel,
                  isDarkTheme = isDark,
                  onToggleTheme = { origin ->
                    revealOrigin = origin
                    revealTrigger += 1
                    viewModel.setThemeMode(if (isDark) "light" else "dark")
                  },
                  onNoteClick = { noteId ->
                    viewModel.clearSelection()
                    navController.navigate("editor/$noteId?mode=text")
                  },
                  onCreateNoteWithMode = { mode ->
                    viewModel.clearSelection()
                    navController.navigate("editor/0?mode=$mode")
                  },
                  onOpenSettings = {
                    viewModel.clearSelection()
                    navController.navigate("settings")
                  },
                  onOpenArchive = {
                    viewModel.clearSelection()
                    navController.navigate("archive")
                  },
                  onOpenTrash = {
                    viewModel.clearSelection()
                    navController.navigate("trash")
                  }
                )
              }

              composable(
                route = "editor/{noteId}?mode={mode}",
                arguments = listOf(
                  navArgument("noteId") { type = NavType.LongType },
                  navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "text"
                  }
                )
              ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                val mode = backStackEntry.arguments?.getString("mode") ?: "text"
                NoteEditorScreen(
                  viewModel = viewModel,
                  noteId = noteId,
                  initialMode = mode,
                  onBack = {
                    viewModel.clearSelection()
                    navController.popBackStack()
                  }
                )
              }

              composable("settings") {
                SettingsScreen(
                  viewModel = viewModel,
                  onBack = { navController.popBackStack() },
                  onOpenProFeatures = { navController.navigate("pro_features") },
                  onOpenAboutSupport = { navController.navigate("about_support") }
                )
              }

              composable("about_support") {
                AboutSupportScreen(
                  viewModel = viewModel,
                  onBack = { navController.popBackStack() }
                )
              }

              composable("pro_features") {
                ProFeaturesScreen(
                  viewModel = viewModel,
                  onBack = { navController.popBackStack() }
                )
              }

              composable("archive") {
                ArchiveTrashScreen(
                  viewModel = viewModel,
                  isTrash = false,
                  onBack = { navController.popBackStack() },
                  onNoteClick = { note -> navController.navigate("editor/${note.id}?mode=text") }
                )
              }

              composable("trash") {
                ArchiveTrashScreen(
                  viewModel = viewModel,
                  isTrash = true,
                  onBack = { navController.popBackStack() },
                  onNoteClick = { note -> navController.navigate("editor/${note.id}?mode=text") }
                )
              }
            }
          }

          // Sleek 1-Minute Welcome Gift Modal (shown ONCE ever)
          if (showProGiftModal && !remoteConfig.appSettings.maintenanceMode) {
            ProGiftModalDialog(
              onClaim = {
                showProGiftModal = false
                viewModel.claimFreeProGift()
              },
              onDismiss = {
                showProGiftModal = false
                viewModel.dismissProGift()
              }
            )
          }

          // In-App Update System Popup (version.json)
          val updateTarget = availableUpdate
          var installNotice by remember(updateTarget?.versionCode) { mutableStateOf<String?>(null) }
          if (updateTarget != null && (isMandatoryUpdate || !isOptionalUpdateDismissed)) {
            AppUpdateDialog(
              versionInfo = updateTarget,
              isMandatory = isMandatoryUpdate,
              downloadState = updateDownloadState,
              installNotice = installNotice,
              onStartDownload = {
                installNotice = null
                viewModel.startApkUpdateDownload()
              },
              onInstallNow = {
                installNotice = viewModel.installDownloadedApk()
              },
              onDismiss = {
                installNotice = null
                viewModel.resetUpdateDownloadState()
                viewModel.dismissOptionalUpdate()
              }
            )
          }

          // Maintenance Mode Overlay (controlled by config.json appSettings.maintenanceMode)
          if (remoteConfig.appSettings.maintenanceMode) {
            MaintenanceOverlayScreen(
              message = remoteConfig.appSettings.maintenanceMessage,
              isRefreshing = isRefreshingConfig,
              onRefresh = { viewModel.refreshRemoteData(force = true) }
            )
          }

          // Lightweight 2-second Compose Canvas confetti celebration + "Pro Features Unlocked! 🎉" toast
          ConfettiCelebrationOverlay(triggerKey = proCelebrationEvent)
        }
      }
    }
  }
}
