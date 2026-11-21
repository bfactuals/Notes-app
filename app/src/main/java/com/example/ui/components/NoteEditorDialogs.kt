package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPickerDialog(
  currentReminderTime: Long,
  onDismiss: () -> Unit,
  onSetReminder: (Long) -> Unit,
  onClearReminder: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  var step by remember { mutableStateOf("presets") } // "presets", "date", "time"

  val initialCal = remember(currentReminderTime) {
    Calendar.getInstance().apply {
      if (currentReminderTime > System.currentTimeMillis()) {
        timeInMillis = currentReminderTime
      } else {
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
      }
    }
  }

  val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialCal.timeInMillis)
  val timePickerState = rememberTimePickerState(
    initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
    initialMinute = initialCal.get(Calendar.MINUTE)
  )

  when (step) {
    "date" -> {
      DatePickerDialog(
        onDismissRequest = { step = "presets" },
        confirmButton = {
          TextButton(onClick = { step = "time" }) {
            Text("Next", color = colors.accent, style = VeilTextStyles.cardTitle)
          }
        },
        dismissButton = {
          TextButton(onClick = { step = "presets" }) {
            Text("Back", color = colors.muted)
          }
        }
      ) {
        DatePicker(state = datePickerState)
      }
    }

    "time" -> {
      AlertDialog(
        onDismissRequest = { step = "presets" },
        containerColor = colors.surface,
        titleContentColor = colors.text,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Pick Time", style = VeilTextStyles.cardTitle) },
        text = {
          Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
          ) {
            TimePicker(state = timePickerState)
          }
        },
        confirmButton = {
          TextButton(
            onClick = {
              val selectedUtc = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
              val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selectedUtc
              }
              val localCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                set(Calendar.MINUTE, timePickerState.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
              }
              onSetReminder(localCal.timeInMillis)
            },
            modifier = Modifier.testTag("confirm_custom_reminder_button")
          ) {
            Text("Save", color = colors.accent, style = VeilTextStyles.cardTitle)
          }
        },
        dismissButton = {
          TextButton(onClick = { step = "date" }) {
            Text("Back", color = colors.muted)
          }
        }
      )
    }

    else -> {
      AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.text,
        textContentColor = colors.text,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Set Reminder", style = VeilTextStyles.cardTitle) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (currentReminderTime > 0L) {
              Text(
                text = "Current: ${formatReminderChip(currentReminderTime)}",
                style = VeilTextStyles.meta,
                color = colors.accent
              )
            }
            ReminderOptionRow(label = "In 1 hour") {
              val cal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
              onSetReminder(cal.timeInMillis)
            }
            ReminderOptionRow(label = "Tonight, 8:00 PM") {
              val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 20)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
              }
              onSetReminder(cal.timeInMillis)
            }
            ReminderOptionRow(label = "Tomorrow, 9:00 AM") {
              val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
              }
              onSetReminder(cal.timeInMillis)
            }
            ReminderOptionRow(label = "Pick date & time…", accent = true) {
              step = "date"
            }
          }
        },
        confirmButton = {
          TextButton(onClick = onDismiss) {
            Text("Close", color = colors.muted)
          }
        },
        dismissButton = {
          if (currentReminderTime > 0L) {
            TextButton(onClick = onClearReminder) {
              Text("Remove", color = colors.doodlePink)
            }
          }
        }
      )
    }
  }
}

@Composable
private fun ReminderOptionRow(
  label: String,
  accent: Boolean = false,
  onClick: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(if (accent) colors.accentSoft else colors.bg)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    VeilIcons.Bell(tint = if (accent) colors.accent else colors.text, size = 18.dp)
    Text(
      text = label,
      style = VeilTextStyles.cardBody.copy(
        fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Medium
      ),
      color = if (accent) colors.accent else colors.text
    )
  }
}

@Composable
fun LabelManagerDialog(
  allLabels: List<String>,
  assignedLabels: Set<String>,
  onToggleLabel: (String) -> Unit,
  onCreateAndAssignLabel: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  var newLabelText by remember { mutableStateOf("") }
  val combinedLabels = remember(allLabels, assignedLabels) {
    (allLabels + assignedLabels).distinct().sortedBy { it.lowercase() }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.surface,
    titleContentColor = colors.text,
    textContentColor = colors.text,
    shape = RoundedCornerShape(24.dp),
    title = { Text("Labels", style = VeilTextStyles.cardTitle) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = newLabelText,
            onValueChange = { newLabelText = it },
            placeholder = { Text("Create new label", style = VeilTextStyles.cardBody, color = colors.muted) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.accent,
              unfocusedBorderColor = colors.hiddenBorder,
              focusedTextColor = colors.text,
              unfocusedTextColor = colors.text,
              cursorColor = colors.accent
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("new_label_input")
          )
          TextButton(
            onClick = {
              val cleaned = newLabelText.trim()
              if (cleaned.isNotEmpty()) {
                onCreateAndAssignLabel(cleaned)
                newLabelText = ""
              }
            },
            modifier = Modifier.testTag("add_label_button")
          ) {
            Text("Add", style = VeilTextStyles.cardTitle, color = colors.accent)
          }
        }

        if (combinedLabels.isEmpty()) {
          Text(
            text = "No labels yet. Type above to create one.",
            style = VeilTextStyles.meta,
            color = colors.muted
          )
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            items(combinedLabels, key = { it }) { label ->
              val isChecked = assignedLabels.any { it.equals(label, ignoreCase = true) }
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { onToggleLabel(label) }
                  .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                VeilIcons.ChecklistCircle(
                  checked = isChecked,
                  tint = colors.muted,
                  accent = colors.accent
                )
                Text(
                  text = label,
                  style = VeilTextStyles.cardBody,
                  color = colors.text,
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done", style = VeilTextStyles.cardTitle, color = colors.accent)
      }
    }
  )
}
