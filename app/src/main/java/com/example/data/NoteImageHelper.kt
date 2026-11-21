package com.example.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.coroutines.resume

object NoteImageHelper {

  private fun imagesDir(context: Context): File {
    return File(context.filesDir, "note_images").apply {
      if (!exists()) mkdirs()
    }
  }

  fun getFileProviderUri(context: Context, file: File): Uri {
    return FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )
  }

  fun createCameraImageFile(context: Context): Pair<File, Uri> {
    val file = File(imagesDir(context), "cam_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
    val uri = getFileProviderUri(context, file)
    return file to uri
  }

  suspend fun copyUriToInternalStorage(context: Context, sourceUri: Uri): String? =
    withContext(Dispatchers.IO) {
      runCatching {
        val destFile = File(
          imagesDir(context),
          "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        )
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
          FileOutputStream(destFile).use { output ->
            input.copyTo(output)
          }
        } ?: return@withContext null
        if (destFile.exists() && destFile.length() > 0L) destFile.absolutePath else null
      }.getOrNull()
    }

  fun deleteImageFile(path: String) {
    runCatching {
      val f = File(path)
      if (f.exists()) f.delete()
    }
  }

  suspend fun extractTextFromImage(context: Context, imagePath: String): Result<String> =
    withContext(Dispatchers.Default) {
      runCatching {
        val file = File(imagePath)
        require(file.exists()) { "Image file not found" }
        val inputImage = InputImage.fromFilePath(context, Uri.fromFile(file))
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
          suspendCancellableCoroutine<String> { cont ->
            recognizer.process(inputImage)
              .addOnSuccessListener { visionText ->
                if (cont.isActive) {
                  cont.resume(visionText.text.trim())
                }
              }
              .addOnFailureListener { e ->
                if (cont.isActive) {
                  cont.resumeWith(Result.failure(e))
                }
              }
          }
        } finally {
          recognizer.close()
        }
      }
    }

  fun copyTextToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
  }

  fun copyImageToClipboard(context: Context, imagePath: String): Boolean {
    return runCatching {
      val file = File(imagePath)
      if (!file.exists()) return false
      val uri = getFileProviderUri(context, file)
      val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
      val clip = ClipData.newUri(context.contentResolver, "Note Image", uri)
      cm.setPrimaryClip(clip)
      true
    }.getOrDefault(false)
  }

  fun shareImage(context: Context, imagePath: String) {
    runCatching {
      val file = File(imagePath)
      if (!file.exists()) return
      val uri = getFileProviderUri(context, file)
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(intent, "Share image"))
    }
  }

  fun formatNotePlainText(
    title: String,
    content: String,
    checklist: List<ChecklistItemData>
  ): String = buildString {
    if (title.isNotBlank()) {
      appendLine(title.trim())
    }
    if (content.isNotBlank()) {
      if (isNotEmpty()) appendLine()
      appendLine(content.trim())
    }
    val nonEmptyChecks = checklist.filter { it.text.isNotBlank() }
    if (nonEmptyChecks.isNotEmpty()) {
      if (isNotEmpty()) appendLine()
      nonEmptyChecks.forEach { item ->
        appendLine("${if (item.checked) "☑" else "☐"} ${item.text.trim()}")
      }
    }
  }.trim()

  fun formatNotesPlainText(notes: List<NoteEntity>): String {
    return notes.mapNotNull { note ->
      val extras = NoteExtrasCodec.decode(note.checklistJson)
      val formatted = formatNotePlainText(note.title, note.content, extras.checklist)
      formatted.ifBlank { null }
    }.joinToString("\n\n---\n\n")
  }

  fun shareNote(
    context: Context,
    title: String,
    bodyText: String,
    imagePaths: List<String> = emptyList()
  ) {
    runCatching {
      val validUris = ArrayList<Uri>()
      imagePaths.forEach { p ->
        val f = File(p)
        if (f.exists()) validUris.add(getFileProviderUri(context, f))
      }

      val intent = when {
        validUris.isEmpty() -> Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
          putExtra(Intent.EXTRA_TEXT, bodyText)
        }
        validUris.size == 1 -> Intent(Intent.ACTION_SEND).apply {
          type = "*/*"
          if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
          if (bodyText.isNotBlank()) putExtra(Intent.EXTRA_TEXT, bodyText)
          putExtra(Intent.EXTRA_STREAM, validUris.first())
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        else -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
          type = "*/*"
          if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
          if (bodyText.isNotBlank()) putExtra(Intent.EXTRA_TEXT, bodyText)
          putParcelableArrayListExtra(Intent.EXTRA_STREAM, validUris)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      }
      context.startActivity(Intent.createChooser(intent, "Send note"))
    }
  }
}
