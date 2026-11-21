package com.example

import android.app.Activity
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
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.ThemeRevealContainer
import com.example.ui.screens.*
import com.example.ui.theme.VeilEasing
import com.example.ui.theme.VeilTheme
import com.example.ui.theme.VeilThemeTokens
import com.example.viewmodel.VeilViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: VeilViewModel by viewModels()

  override fun onResume() {
    super.onResume()
    viewModel.onAppForegrounded()
  }

  override fun onPause() {
    super.onPause()
    // Leaving the app automatically conceals any unlocked hidden notes (stealth panic behavior)
    viewModel.panicHide()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val themeMode by viewModel.themeMode.collectAsState()
      val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsState()
      val fontSize by viewModel.fontSize.collectAsState()
      val fontStyle by viewModel.fontStyle.collectAsState()
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

      VeilTheme(
        darkTheme = isDark,
        hapticEnabled = hapticFeedbackEnabled,
        fontSize = fontSize,
        fontStyle = fontStyle
      ) {
        val reduceMotion = VeilThemeTokens.reduceMotion
        val animDuration = if (reduceMotion) 0 else 250

        ThemeRevealContainer(
          revealTrigger = revealTrigger,
          revealOrigin = revealOrigin,
          isDarkTheme = isDark
        ) {
          // NEVER show a Secret Code popup on launch or restart!
          val navController = rememberNavController()

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
                  navController.navigate("editor/$noteId?mode=text")
                },
                onCreateNoteWithMode = { mode ->
                  navController.navigate("editor/0?mode=$mode")
                },
                onOpenSettings = {
                  navController.navigate("settings")
                },
                onOpenArchive = {
                  navController.navigate("archive")
                },
                onOpenTrash = {
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
                onBack = { navController.popBackStack() }
              )
            }

            composable("settings") {
              SettingsScreen(
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
      }
    }
  }
}
