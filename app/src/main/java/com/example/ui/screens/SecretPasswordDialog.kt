package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens
import kotlinx.coroutines.launch

/**
 * Hidden setup / change secret code dialog.
 * - First-time setup (hasExistingPassword = false): Create new secret code.
 * - Change code (hasExistingPassword = true): Requires entering & verifying OLD code first;
 *   never displays or reveals the current password.
 */
@Composable
fun SecretPasswordDialog(
  hasExistingPassword: Boolean,
  onVerifyOldPassword: suspend (String) -> Boolean,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit
) {
  val colors = VeilThemeTokens.colors
  val scope = rememberCoroutineScope()

  var oldPassword by remember { mutableStateOf("") }
  var isOldVerified by remember { mutableStateOf(!hasExistingPassword) }
  var newPassword by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var error by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.surface,
    titleContentColor = colors.text,
    textContentColor = colors.text,
    shape = RoundedCornerShape(24.dp),
    title = {
      Text(
        text = if (hasExistingPassword) "Change Secret Code" else "Create Secret Code",
        style = VeilTextStyles.cardTitle
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        if (!isOldVerified) {
          Text(
            text = "Enter your current code to continue.",
            style = VeilTextStyles.cardBody,
            color = colors.muted
          )
          Spacer(modifier = Modifier.height(14.dp))
          OutlinedTextField(
            value = oldPassword,
            onValueChange = {
              oldPassword = it
              error = ""
            },
            label = { Text("Current code", style = VeilTextStyles.meta) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.accent,
              unfocusedBorderColor = colors.hiddenBorder,
              focusedTextColor = colors.text,
              unfocusedTextColor = colors.text,
              cursorColor = colors.accent
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("old_secret_password_input")
          )
        } else {
          Text(
            text = if (hasExistingPassword) "Enter a new code." else "Set a code to access hidden notes from search.",
            style = VeilTextStyles.cardBody,
            color = colors.muted
          )
          Spacer(modifier = Modifier.height(14.dp))
          OutlinedTextField(
            value = newPassword,
            onValueChange = {
              newPassword = it
              error = ""
            },
            label = { Text("New code", style = VeilTextStyles.meta) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.accent,
              unfocusedBorderColor = colors.hiddenBorder,
              focusedTextColor = colors.text,
              unfocusedTextColor = colors.text,
              cursorColor = colors.accent
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("new_secret_password_input")
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
              confirmPassword = it
              error = ""
            },
            label = { Text("Confirm code", style = VeilTextStyles.meta) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.accent,
              unfocusedBorderColor = colors.hiddenBorder,
              focusedTextColor = colors.text,
              unfocusedTextColor = colors.text,
              cursorColor = colors.accent
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("confirm_secret_password_input")
          )
        }
        if (error.isNotEmpty()) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(error, style = VeilTextStyles.meta, color = colors.doodlePink)
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (!isOldVerified) {
            scope.launch {
              val ok = onVerifyOldPassword(oldPassword.trim())
              if (ok) {
                isOldVerified = true
                error = ""
              } else {
                error = "Incorrect current code"
              }
            }
          } else {
            val cleaned = newPassword.trim()
            if (cleaned.length < 4) {
              error = "Code must be at least 4 characters"
            } else if (cleaned != confirmPassword.trim()) {
              error = "Codes do not match"
            } else {
              onSave(cleaned)
            }
          }
        },
        modifier = Modifier.testTag("save_secret_password_button")
      ) {
        Text(
          text = if (!isOldVerified) "Verify" else "Save",
          style = VeilTextStyles.cardTitle,
          color = colors.accent
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", style = VeilTextStyles.cardBody, color = colors.muted)
      }
    }
  )
}

@Composable
fun BackupPasswordDialog(
  title: String,
  description: String,
  confirmLabel: String,
  errorText: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit
) {
  val colors = VeilThemeTokens.colors
  var password by remember { mutableStateOf("") }
  var localError by remember { mutableStateOf("") }
  val shownError = errorText.ifEmpty { localError }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.surface,
    titleContentColor = colors.text,
    textContentColor = colors.text,
    shape = RoundedCornerShape(24.dp),
    title = { Text(title, style = VeilTextStyles.cardTitle) },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(description, style = VeilTextStyles.cardBody, color = colors.muted)
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = password,
          onValueChange = {
            password = it
            localError = ""
          },
          label = { Text("Password", style = VeilTextStyles.meta) },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.hiddenBorder,
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text,
            cursorColor = colors.accent
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("backup_password_input")
        )
        if (shownError.isNotEmpty()) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(shownError, style = VeilTextStyles.meta, color = colors.doodlePink)
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (password.trim().isEmpty()) {
            localError = "Please enter a password"
          } else {
            onConfirm(password.trim())
          }
        },
        modifier = Modifier.testTag("confirm_backup_password_button")
      ) {
        Text(confirmLabel, style = VeilTextStyles.cardTitle, color = colors.accent)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", style = VeilTextStyles.cardBody, color = colors.muted)
      }
    }
  )
}
