package com.example.data

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class AboutConfig(
  val creatorName: String = "Bfactuals",
  val bio: String = "Building fast, minimalist, privacy-first tools.",
  val avatarUrl: String = "",
  val changelog: List<String> = DEFAULT_CHANGELOG
) {
  companion object {
    val DEFAULT_CHANGELOG = listOf(
      "Smooth drag-and-drop note reordering persisted across restarts",
      "Fixed Bold, Highlight, H1 & H2 rich-text formatting in the editor",
      "Redesigned Pro Features & Welcome Gift experience with Share Veil",
      "Compact Settings, shorter 3-dot editor menu, and crisper haptic feedback"
    )
  }
}

data class CustomLinkItem(
  val enabled: Boolean = true,
  val label: String = "",
  val url: String = ""
)

data class SocialLinksConfig(
  val instagram: String = "",
  val telegram: String = "",
  val youtube: String = "",
  val website: String = "",
  val githubRepo: String = DEFAULT_GITHUB_REPO,
  val customLinks: List<CustomLinkItem> = emptyList()
) {
  companion object {
    const val DEFAULT_GITHUB_REPO = "https://github.com/bfactuals/Notes-app"
  }
}

data class ContactConfig(
  val supportEmail: String = "",
  val bugReportUrl: String = ""
)

data class LegalConfig(
  val privacyPolicyUrl: String = "",
  val termsUrl: String = ""
)

data class SupportConfig(
  val enabled: Boolean = false,
  val title: String = "Support Veil",
  val description: String = "Veil is 100% free, ad-free, and offline-first. Your support keeps development going!",
  val upiEnabled: Boolean = false,
  val upiId: String = "",
  val qrImageUrl: String = "",
  val paymentLinkEnabled: Boolean = false,
  val paymentUrl: String = "",
  val showCustomMessage: Boolean = false,
  val customMessage: String = ""
) {
  val hasAnyActionableMethod: Boolean
    get() = (upiEnabled && (RemoteConfigValidator.isValidUpiId(upiId) || RemoteConfigValidator.isValidHttpsUrl(qrImageUrl))) ||
      (paymentLinkEnabled && RemoteConfigValidator.isValidHttpsUrl(paymentUrl)) ||
      (showCustomMessage && customMessage.isNotBlank())
}

data class AnnouncementConfig(
  val enabled: Boolean = false,
  val title: String = "",
  val message: String = "",
  val link: String = ""
) {
  val isDisplayable: Boolean
    get() = enabled && (title.isNotBlank() || message.isNotBlank())

  val contentHash: String
    get() = RemoteConfigValidator.sha256Short("$title|$message|$link")
}

data class AppSettingsConfig(
  val maintenanceMode: Boolean = false,
  val maintenanceMessage: String = "Veil is undergoing scheduled maintenance. Please check back shortly."
)

data class RemoteConfig(
  val about: AboutConfig = AboutConfig(),
  val socialLinks: SocialLinksConfig = SocialLinksConfig(),
  val contact: ContactConfig = ContactConfig(),
  val legal: LegalConfig = LegalConfig(),
  val support: SupportConfig = SupportConfig(),
  val announcements: AnnouncementConfig = AnnouncementConfig(),
  val appSettings: AppSettingsConfig = AppSettingsConfig()
)

data class RemoteVersionInfo(
  val versionCode: Int = 0,
  val latestVersion: String = "",
  val minVersion: String = "",
  val title: String = "Update Available",
  val updateMessage: String = "",
  val whatsNew: List<String> = emptyList(),
  val updateUrl: String = "",
  val sha256: String = "",
  val forceUpdate: Boolean = false
)

sealed class UpdateDownloadState {
  data object Idle : UpdateDownloadState()
  data class Downloading(
    val progress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long
  ) : UpdateDownloadState()
  data object Verifying : UpdateDownloadState()
  data class ReadyToInstall(val apkFile: File) : UpdateDownloadState()
  data class Error(val message: String) : UpdateDownloadState()
}

object RemoteConfigValidator {
  private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+\\-]{1,64}@[A-Za-z0-9.\\-]{1,253}\\.[A-Za-z]{2,24}$")
  private val UPI_ID_REGEX = Regex("^[A-Za-z0-9._\\-]{2,128}@[A-Za-z0-9.\\-]{2,64}$")
  private val SHA256_HEX_REGEX = Regex("^[a-fA-F0-9]{64}$")

  fun sanitizeText(raw: String?, maxLen: Int = 600): String {
    if (raw == null) return ""
    val cleaned = raw
      .replace("\u0000", "")
      .trim()
    return if (cleaned.length > maxLen) cleaned.take(maxLen) else cleaned
  }

  /**
   * Strictly permits only valid HTTPS URLs with a non-blank host (blocks javascript:, file:, data:, http:, etc.).
   */
  fun isValidHttpsUrl(raw: String?): Boolean {
    val cleaned = raw?.trim().orEmpty()
    if (cleaned.isEmpty() || cleaned.length > 2048) return false
    if (!cleaned.startsWith("https://", ignoreCase = true)) return false
    val uri = runCatching { Uri.parse(cleaned) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    val host = uri.host?.trim().orEmpty()
    if (scheme != "https" || host.isEmpty() || !host.contains(".")) return false
    if (host.equals("localhost", ignoreCase = true) || host == "127.0.0.1") return false
    return true
  }

  fun sanitizeHttpsUrl(raw: String?): String {
    val trimmed = raw?.trim().orEmpty()
    return if (isValidHttpsUrl(trimmed)) trimmed else ""
  }

  fun isValidEmail(raw: String?): Boolean {
    val trimmed = raw?.trim().orEmpty()
    return trimmed.isNotEmpty() && EMAIL_REGEX.matches(trimmed)
  }

  fun sanitizeEmail(raw: String?): String {
    val trimmed = raw?.trim().orEmpty()
    return if (isValidEmail(trimmed)) trimmed else ""
  }

  fun isValidUpiId(raw: String?): Boolean {
    val trimmed = raw?.trim().orEmpty()
    return trimmed.isNotEmpty() && UPI_ID_REGEX.matches(trimmed)
  }

  fun sanitizeUpiId(raw: String?): String {
    val trimmed = raw?.trim().orEmpty()
    return if (isValidUpiId(trimmed)) trimmed else ""
  }

  fun sanitizeSha256(raw: String?): String {
    val trimmed = raw?.trim()?.lowercase().orEmpty()
    return if (SHA256_HEX_REGEX.matches(trimmed)) trimmed else ""
  }

  fun buildUpiPaymentUri(upiId: String, payeeName: String): Uri? {
    val safeUpi = sanitizeUpiId(upiId)
    if (safeUpi.isEmpty()) return null
    val safeName = sanitizeText(payeeName, 80).ifBlank { "Bfactuals" }
    return Uri.Builder()
      .scheme("upi")
      .authority("pay")
      .appendQueryParameter("pa", safeUpi)
      .appendQueryParameter("pn", safeName)
      .appendQueryParameter("tn", "Support Veil Notes")
      .appendQueryParameter("cu", "INR")
      .build()
  }

  fun sha256Short(input: String): String {
    return runCatching {
      val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
      digest.take(12).joinToString("") { "%02x".format(it) }
    }.getOrDefault(input.hashCode().toString())
  }

  /**
   * Compares two dot-separated version strings (e.g. "1.0" vs "1.2.0").
   * Returns > 0 if v1 > v2, < 0 if v1 < v2, 0 if equal.
   */
  fun compareVersions(v1: String, v2: String): Int {
    val p1 = v1.trim().removePrefix("v").removePrefix("V")
      .split(".")
      .map { segment -> segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
    val p2 = v2.trim().removePrefix("v").removePrefix("V")
      .split(".")
      .map { segment -> segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
    val maxLen = maxOf(p1.size, p2.size)
    for (i in 0 until maxLen) {
      val a = p1.getOrElse(i) { 0 }
      val b = p2.getOrElse(i) { 0 }
      if (a != b) return a.compareTo(b)
    }
    return 0
  }
}

object RemoteConfigParser {
  fun parseConfig(rawJson: String): RemoteConfig? {
    return runCatching {
      val root = JSONObject(rawJson)

      // 1. About
      val aboutObj = root.optJSONObject("about")
      val rawCreator = RemoteConfigValidator.sanitizeText(
        aboutObj?.optString("creatorName")?.takeIf { it.isNotBlank() }
          ?: aboutObj?.optString("name"),
        80
      )
      val rawBio = RemoteConfigValidator.sanitizeText(aboutObj?.optString("bio"), 400)
      val avatarUrl = RemoteConfigValidator.sanitizeHttpsUrl(aboutObj?.optString("avatarUrl"))
      val changelogList = parseStringListOrLines(aboutObj?.opt("changelog"))
      val about = AboutConfig(
        creatorName = rawCreator.ifBlank { "Bfactuals" },
        bio = rawBio.ifBlank { AboutConfig().bio },
        avatarUrl = avatarUrl,
        changelog = if (changelogList.isNotEmpty()) changelogList else AboutConfig.DEFAULT_CHANGELOG
      )

      // 2. Social links (each standard link supports either plain URL string or { enabled, url } object)
      val socialObj = root.optJSONObject("socialLinks")
      val customLinks = mutableListOf<CustomLinkItem>()
      val customArr = root.optJSONArray("customLinks") ?: socialObj?.optJSONArray("customLinks")
      if (customArr != null) {
        for (i in 0 until minOf(customArr.length(), 20)) {
          val itemObj = customArr.optJSONObject(i) ?: continue
          val enabled = itemObj.optBoolean("enabled", true)
          val label = RemoteConfigValidator.sanitizeText(itemObj.optString("label"), 60)
          val url = RemoteConfigValidator.sanitizeHttpsUrl(itemObj.optString("url"))
          if (enabled && label.isNotBlank() && url.isNotEmpty()) {
            customLinks.add(CustomLinkItem(enabled = true, label = label, url = url))
          }
        }
      }
      val parsedGithub = parseSocialLinkField(socialObj, "githubRepo")
        .ifEmpty { parseSocialLinkField(socialObj, "github") }
      val socialLinks = SocialLinksConfig(
        instagram = parseSocialLinkField(socialObj, "instagram"),
        telegram = parseSocialLinkField(socialObj, "telegram"),
        youtube = parseSocialLinkField(socialObj, "youtube"),
        website = parseSocialLinkField(socialObj, "website"),
        githubRepo = parsedGithub.ifEmpty { SocialLinksConfig.DEFAULT_GITHUB_REPO },
        customLinks = customLinks
      )

      // 3. Contact
      val contactObj = root.optJSONObject("contact")
      val contact = ContactConfig(
        supportEmail = RemoteConfigValidator.sanitizeEmail(contactObj?.optString("supportEmail")),
        bugReportUrl = RemoteConfigValidator.sanitizeHttpsUrl(contactObj?.optString("bugReportUrl"))
      )

      // 4. Legal
      val legalObj = root.optJSONObject("legal")
      val legal = LegalConfig(
        privacyPolicyUrl = RemoteConfigValidator.sanitizeHttpsUrl(legalObj?.optString("privacyPolicyUrl")),
        termsUrl = RemoteConfigValidator.sanitizeHttpsUrl(legalObj?.optString("termsUrl"))
      )

      // 5. Support
      val supportObj = root.optJSONObject("support")
      val support = if (supportObj != null) {
        SupportConfig(
          enabled = supportObj.optBoolean("enabled", false),
          title = RemoteConfigValidator.sanitizeText(supportObj.optString("title"), 100)
            .ifBlank { SupportConfig().title },
          description = RemoteConfigValidator.sanitizeText(supportObj.optString("description"), 500)
            .ifBlank { SupportConfig().description },
          upiEnabled = supportObj.optBoolean("upiEnabled", false),
          upiId = RemoteConfigValidator.sanitizeUpiId(supportObj.optString("upiId")),
          qrImageUrl = RemoteConfigValidator.sanitizeHttpsUrl(supportObj.optString("qrImageUrl")),
          paymentLinkEnabled = supportObj.optBoolean("paymentLinkEnabled", false),
          paymentUrl = RemoteConfigValidator.sanitizeHttpsUrl(supportObj.optString("paymentUrl")),
          showCustomMessage = supportObj.optBoolean("showCustomMessage", false),
          customMessage = RemoteConfigValidator.sanitizeText(supportObj.optString("customMessage"), 400)
        )
      } else {
        SupportConfig()
      }

      // 6. Announcements
      val annObj = root.optJSONObject("announcements")
      val announcements = if (annObj != null) {
        AnnouncementConfig(
          enabled = annObj.optBoolean("enabled", false),
          title = RemoteConfigValidator.sanitizeText(annObj.optString("title"), 120),
          message = RemoteConfigValidator.sanitizeText(annObj.optString("message"), 400),
          link = RemoteConfigValidator.sanitizeHttpsUrl(annObj.optString("link"))
        )
      } else {
        AnnouncementConfig()
      }

      // 7. App settings
      val appSettingsObj = root.optJSONObject("appSettings")
      val appSettings = if (appSettingsObj != null) {
        AppSettingsConfig(
          maintenanceMode = appSettingsObj.optBoolean("maintenanceMode", false),
          maintenanceMessage = RemoteConfigValidator.sanitizeText(
            appSettingsObj.optString("maintenanceMessage"),
            500
          ).ifBlank { AppSettingsConfig().maintenanceMessage }
        )
      } else {
        AppSettingsConfig()
      }

      RemoteConfig(
        about = about,
        socialLinks = socialLinks,
        contact = contact,
        legal = legal,
        support = support,
        announcements = announcements,
        appSettings = appSettings
      )
    }.getOrNull()
  }

  fun parseVersionInfo(rawJson: String): RemoteVersionInfo? {
    return runCatching {
      val root = JSONObject(rawJson)
      val versionCode = root.optInt("versionCode", 0)
      val latestVersion = RemoteConfigValidator.sanitizeText(root.optString("latestVersion"), 32)
      val minVersion = RemoteConfigValidator.sanitizeText(root.optString("minVersion"), 32)
      val title = RemoteConfigValidator.sanitizeText(root.optString("title"), 100)
        .ifBlank { "Update Available" }
      val updateMessage = RemoteConfigValidator.sanitizeText(root.optString("updateMessage"), 600)
      val whatsNew = parseStringListOrLines(root.opt("whatsNew"))
      val updateUrl = RemoteConfigValidator.sanitizeHttpsUrl(root.optString("updateUrl"))
      val rawSha = root.optString("sha256", "").trim()
      // Preserve non-empty raw SHA256 if present so invalid/tampered checksums fail safely rather than silently bypassing verification
      val sha256 = if (rawSha.isEmpty()) "" else rawSha.lowercase()
      val forceUpdate = root.optBoolean("forceUpdate", false)

      if (versionCode <= 0 && latestVersion.isEmpty()) return@runCatching null

      RemoteVersionInfo(
        versionCode = versionCode,
        latestVersion = latestVersion,
        minVersion = minVersion,
        title = title,
        updateMessage = updateMessage,
        whatsNew = whatsNew,
        updateUrl = updateUrl,
        sha256 = sha256,
        forceUpdate = forceUpdate
      )
    }.getOrNull()
  }

  private fun parseSocialLinkField(obj: JSONObject?, key: String): String {
    if (obj == null || !obj.has(key) || obj.isNull(key)) return ""
    val value = obj.opt(key)
    return when (value) {
      is String -> RemoteConfigValidator.sanitizeHttpsUrl(value)
      is JSONObject -> {
        val enabled = value.optBoolean("enabled", true)
        if (!enabled) "" else RemoteConfigValidator.sanitizeHttpsUrl(value.optString("url"))
      }
      else -> ""
    }
  }

  private fun parseStringListOrLines(raw: Any?): List<String> {
    return when (raw) {
      is JSONArray -> {
        val out = mutableListOf<String>()
        for (i in 0 until minOf(raw.length(), 30)) {
          val line = RemoteConfigValidator.sanitizeText(raw.optString(i), 240)
          if (line.isNotBlank()) out.add(line)
        }
        out
      }
      is String -> {
        raw.lines()
          .map { RemoteConfigValidator.sanitizeText(it.removePrefix("•").removePrefix("-"), 240) }
          .filter { it.isNotBlank() }
          .take(30)
      }
      else -> emptyList()
    }
  }
}
