package com.example.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NoteReminderScheduler {
  const val CHANNEL_ID = "veil_note_reminders"
  const val EXTRA_NOTE_ID = "open_note_id"
  const val EXTRA_NOTE_TITLE = "reminder_note_title"
  const val EXTRA_NOTE_PREVIEW = "reminder_note_preview"

  fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      val existing = manager.getNotificationChannel(CHANNEL_ID)
      if (existing == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          "Note Reminders",
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          description = "Scheduled local reminders for your notes"
          enableVibration(true)
        }
        manager.createNotificationChannel(channel)
      }
    }
  }

  fun scheduleReminder(
    context: Context,
    noteId: Long,
    title: String,
    preview: String,
    triggerAtMillis: Long
  ) {
    if (noteId <= 0L) return
    ensureNotificationChannel(context)
    val now = System.currentTimeMillis()
    if (triggerAtMillis <= now) {
      cancelReminder(context, noteId)
      return
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val pendingIntent = buildAlarmPendingIntent(
      context = context,
      noteId = noteId,
      title = title,
      preview = preview
    )

    runCatching {
      alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        pendingIntent
      )
    }
  }

  fun cancelReminder(context: Context, noteId: Long) {
    if (noteId <= 0L) return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val pendingIntent = buildAlarmPendingIntent(
      context = context,
      noteId = noteId,
      title = "",
      preview = ""
    )
    runCatching {
      alarmManager.cancel(pendingIntent)
      pendingIntent.cancel()
    }
  }

  private fun buildAlarmPendingIntent(
    context: Context,
    noteId: Long,
    title: String,
    preview: String
  ): PendingIntent {
    val intent = Intent(context, NoteReminderReceiver::class.java).apply {
      action = "com.example.action.NOTE_REMINDER_$noteId"
      putExtra(EXTRA_NOTE_ID, noteId)
      putExtra(EXTRA_NOTE_TITLE, title.take(120))
      putExtra(EXTRA_NOTE_PREVIEW, preview.take(240))
    }
    return PendingIntent.getBroadcast(
      context,
      requestCodeFor(noteId),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  fun requestCodeFor(noteId: Long): Int = ((noteId xor (noteId ushr 32)).toInt() and 0x7FFFFFFF)
}

class NoteReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val noteId = intent.getLongExtra(NoteReminderScheduler.EXTRA_NOTE_ID, 0L)
    if (noteId <= 0L) return

    NoteReminderScheduler.ensureNotificationChannel(context)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }

    val rawTitle = intent.getStringExtra(NoteReminderScheduler.EXTRA_NOTE_TITLE).orEmpty().trim()
    val rawPreview = intent.getStringExtra(NoteReminderScheduler.EXTRA_NOTE_PREVIEW).orEmpty().trim()
    val displayTitle = rawTitle.ifBlank { "Note Reminder" }
    val displayPreview = rawPreview.ifBlank { "Tap to open your note" }

    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(NoteReminderScheduler.EXTRA_NOTE_ID, noteId)
    }

    val openPendingIntent = PendingIntent.getActivity(
      context,
      NoteReminderScheduler.requestCodeFor(noteId),
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, NoteReminderScheduler.CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(displayTitle)
      .setContentText(displayPreview)
      .setStyle(NotificationCompat.BigTextStyle().bigText(displayPreview))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setAutoCancel(true)
      .setContentIntent(openPendingIntent)
      .build()

    runCatching {
      NotificationManagerCompat.from(context).notify(
        NoteReminderScheduler.requestCodeFor(noteId),
        notification
      )
    }
  }
}
