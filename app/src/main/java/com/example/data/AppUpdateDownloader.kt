package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object AppUpdateDownloader {
  private const val MAX_APK_BYTES = 200L * 1024L * 1024L // 200 MB safety cap
  private const val UPDATES_DIR = "updates"
  private const val APK_FILENAME = "veil_update.apk"

  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

  /**
   * Downloads an APK from a validated HTTPS URL into internal cache, streams progress,
   * and verifies its SHA-256 digest when expectedSha256 is non-blank.
   */
  suspend fun downloadAndVerifyApk(
    context: Context,
    updateUrl: String,
    expectedSha256: String,
    onStateChange: (UpdateDownloadState) -> Unit
  ) = withContext(Dispatchers.IO) {
    if (!RemoteConfigValidator.isValidHttpsUrl(updateUrl)) {
      onStateChange(UpdateDownloadState.Error("Invalid update URL. Only HTTPS downloads are permitted."))
      return@withContext
    }

    val normalizedExpectedSha = expectedSha256.trim().lowercase()
    if (normalizedExpectedSha.isNotEmpty() && RemoteConfigValidator.sanitizeSha256(normalizedExpectedSha).isEmpty()) {
      onStateChange(UpdateDownloadState.Error("Update integrity metadata is malformed. Download aborted."))
      return@withContext
    }

    val updatesDir = File(context.cacheDir, UPDATES_DIR).apply { mkdirs() }
    val targetFile = File(updatesDir, APK_FILENAME)
    val tempFile = File(updatesDir, "$APK_FILENAME.tmp")
    // Confinement check
    val canonicalBase = updatesDir.canonicalPath + File.separator
    if (!targetFile.canonicalPath.startsWith(canonicalBase) || !tempFile.canonicalPath.startsWith(canonicalBase)) {
      onStateChange(UpdateDownloadState.Error("Could not prepare safe download directory."))
      return@withContext
    }

    runCatching {
      tempFile.delete()
      onStateChange(UpdateDownloadState.Downloading(0f, 0L, -1L))

      val request = Request.Builder()
        .url(updateUrl)
        .get()
        .header("User-Agent", "VeilNotes-Android")
        .build()

      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          throw IllegalStateException("Download failed (HTTP ${response.code})")
        }
        val body = response.body ?: throw IllegalStateException("Empty update response")
        val contentLength = body.contentLength()
        if (contentLength > MAX_APK_BYTES) {
          throw IllegalStateException("Update file exceeds maximum supported size.")
        }

        val digest = MessageDigest.getInstance("SHA-256")
        var downloaded = 0L
        var lastEmitMs = 0L

        body.byteStream().use { input ->
          FileOutputStream(tempFile).use { output ->
            val buf = ByteArray(16 * 1024)
            while (true) {
              currentCoroutineContext().ensureActive()
              val read = input.read(buf)
              if (read == -1) break
              downloaded += read
              if (downloaded > MAX_APK_BYTES) {
                throw IllegalStateException("Update file exceeded maximum supported size.")
              }
              output.write(buf, 0, read)
              digest.update(buf, 0, read)

              val now = System.currentTimeMillis()
              if (now - lastEmitMs >= 100L) {
                lastEmitMs = now
                val progress = if (contentLength > 0L) {
                  (downloaded.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                } else {
                  -1f
                }
                onStateChange(UpdateDownloadState.Downloading(progress, downloaded, contentLength))
              }
            }
            output.flush()
          }
        }

        if (downloaded <= 0L) {
          throw IllegalStateException("Downloaded APK is empty.")
        }

        onStateChange(UpdateDownloadState.Verifying)
        val actualSha = digest.digest().joinToString("") { "%02x".format(it) }

        if (normalizedExpectedSha.isNotEmpty()) {
          val matches = MessageDigest.isEqual(
            actualSha.toByteArray(Charsets.UTF_8),
            normalizedExpectedSha.toByteArray(Charsets.UTF_8)
          )
          if (!matches) {
            tempFile.delete()
            targetFile.delete()
            throw SecurityException("APK checksum verification failed. Install blocked for your safety.")
          }
        }

        if (targetFile.exists()) targetFile.delete()
        if (!tempFile.renameTo(targetFile)) {
          tempFile.copyTo(targetFile, overwrite = true)
          tempFile.delete()
        }

        onStateChange(UpdateDownloadState.ReadyToInstall(targetFile))
      }
    }.onFailure { err ->
      tempFile.delete()
      onStateChange(
        UpdateDownloadState.Error(
          err.message?.takeIf { it.isNotBlank() } ?: "Could not download update. Please try again."
        )
      )
    }
  }

  /**
   * Launches Android's native package installer for the verified APK file via FileProvider.
   * Returns a human-readable message if the user must first grant "Install unknown apps" permission.
   */
  fun launchPackageInstaller(context: Context, apkFile: File): String? {
    if (!apkFile.exists() || apkFile.length() <= 0L) {
      return "Update file not found. Please download again."
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
      runCatching {
        val settingsIntent = Intent(
          Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
          Uri.parse("package:${context.packageName}")
        ).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(settingsIntent)
      }
      return "Allow \"Install unknown apps\" for Veil, then tap Install Update again."
    }

    return runCatching {
      val apkUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile
      )
      val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(installIntent)
      null
    }.getOrElse {
      "Could not launch Android installer."
    }
  }
}
