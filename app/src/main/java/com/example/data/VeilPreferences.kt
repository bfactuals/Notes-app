package com.example.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "veil_prefs")

data class ProFeaturesConfig(
  val selfDestructEnabled: Boolean = false,
  val inlineCalcEnabled: Boolean = false,
  val autoEmptyTrashEnabled: Boolean = false,
  val codeBlocksEnabled: Boolean = false,
  val versionHistoryEnabled: Boolean = false,
  val clipboardPillEnabled: Boolean = false,
  val autoCategoriesEnabled: Boolean = false
) {
  val allEnabled: Boolean
    get() = selfDestructEnabled &&
      inlineCalcEnabled &&
      autoEmptyTrashEnabled &&
      codeBlocksEnabled &&
      versionHistoryEnabled &&
      clipboardPillEnabled &&
      autoCategoriesEnabled
}

class VeilPreferences(private val context: Context) {
  companion object {
    val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
    val SECRET_PASSWORD_HASH = stringPreferencesKey("secret_password_hash")
    val SECRET_PASSWORD_SALT = stringPreferencesKey("secret_password_salt")
    val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
    val DEFAULT_NOTE_COLOR = stringPreferencesKey("default_note_color")
    val HAS_SEEDED_WELCOME = booleanPreferencesKey("has_seeded_welcome")
    val SAVED_LABELS = stringSetPreferencesKey("saved_labels")
    val SORT_ORDER = stringPreferencesKey("sort_order") // custom, created_desc, created_asc, modified_desc, modified_asc
    val CUSTOM_NOTE_ORDER = stringPreferencesKey("custom_note_order") // comma-separated note IDs
    val ENABLE_SHARING = booleanPreferencesKey("enable_sharing")
    val MOVE_TICKED_TO_BOTTOM = booleanPreferencesKey("move_ticked_to_bottom")
    val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")

    // Pro Features toggles & Free Gift experience
    val PRO_SELF_DESTRUCT = booleanPreferencesKey("pro_self_destruct")
    val PRO_INLINE_CALC = booleanPreferencesKey("pro_inline_calc")
    val PRO_AUTO_EMPTY_TRASH = booleanPreferencesKey("pro_auto_empty_trash")
    val PRO_CODE_BLOCKS = booleanPreferencesKey("pro_code_blocks")
    val PRO_VERSION_HISTORY = booleanPreferencesKey("pro_version_history")
    val PRO_CLIPBOARD_PILL = booleanPreferencesKey("pro_clipboard_pill")
    val PRO_AUTO_CATEGORIES = booleanPreferencesKey("pro_auto_categories")
    val FIRST_INSTALL_TIME_MS = longPreferencesKey("first_install_time_ms")
    val PRO_GIFT_RESOLVED = booleanPreferencesKey("pro_gift_resolved")
    val PRO_GIFT_CLAIMED = booleanPreferencesKey("pro_gift_claimed")

    // Typography preferences
    val FONT_SIZE = stringPreferencesKey("font_size") // "small", "medium", "large"
    val FONT_STYLE = stringPreferencesKey("font_style") // "sans", "serif", "mono"

    // Remote Config, Update System & GitHub Star Nudge
    val CACHED_REMOTE_CONFIG_JSON = stringPreferencesKey("cached_remote_config_json")
    val LAST_CONFIG_FETCH_MS = longPreferencesKey("last_config_fetch_ms")
    val CACHED_VERSION_JSON = stringPreferencesKey("cached_version_json")
    val LAST_VERSION_FETCH_MS = longPreferencesKey("last_version_fetch_ms")
    val DISMISSED_ANNOUNCEMENT_HASH = stringPreferencesKey("dismissed_announcement_hash")
    val TOTAL_NOTES_CREATED = intPreferencesKey("total_notes_created")
    val STAR_NUDGE_DISMISSED = booleanPreferencesKey("star_nudge_dismissed")
  }

  val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
    prefs[THEME_MODE] ?: "system"
  }

  val secretPasswordHash: Flow<String> = context.dataStore.data.map { prefs ->
    prefs[SECRET_PASSWORD_HASH] ?: ""
  }

  val isGridView: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[IS_GRID_VIEW] ?: true
  }

  val defaultNoteColor: Flow<String> = context.dataStore.data.map { prefs ->
    prefs[DEFAULT_NOTE_COLOR] ?: "plain"
  }

  val hasSeededWelcome: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[HAS_SEEDED_WELCOME] ?: false
  }

  val savedLabels: Flow<Set<String>> = context.dataStore.data.map { prefs ->
    prefs[SAVED_LABELS] ?: emptySet()
  }

  val sortOrder: Flow<String> = context.dataStore.data.map { prefs ->
    prefs[SORT_ORDER] ?: "custom"
  }

  val customNoteOrder: Flow<List<Long>> = context.dataStore.data.map { prefs ->
    val raw = prefs[CUSTOM_NOTE_ORDER] ?: ""
    if (raw.isBlank()) emptyList()
    else raw.split(",").mapNotNull { it.trim().toLongOrNull() }
  }

  val enableSharing: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[ENABLE_SHARING] ?: true
  }

  val moveTickedToBottom: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[MOVE_TICKED_TO_BOTTOM] ?: false
  }

  val hapticFeedbackEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[HAPTIC_FEEDBACK_ENABLED] ?: true
  }

  val proFeatures: Flow<ProFeaturesConfig> = context.dataStore.data.map { prefs ->
    ProFeaturesConfig(
      selfDestructEnabled = prefs[PRO_SELF_DESTRUCT] ?: false,
      inlineCalcEnabled = prefs[PRO_INLINE_CALC] ?: false,
      autoEmptyTrashEnabled = prefs[PRO_AUTO_EMPTY_TRASH] ?: false,
      codeBlocksEnabled = prefs[PRO_CODE_BLOCKS] ?: false,
      versionHistoryEnabled = prefs[PRO_VERSION_HISTORY] ?: false,
      clipboardPillEnabled = prefs[PRO_CLIPBOARD_PILL] ?: false,
      autoCategoriesEnabled = prefs[PRO_AUTO_CATEGORIES] ?: false
    )
  }

  val firstInstallTimeMs: Flow<Long> = context.dataStore.data.map { prefs ->
    prefs[FIRST_INSTALL_TIME_MS] ?: 0L
  }

  val proGiftResolved: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[PRO_GIFT_RESOLVED] ?: false
  }

  val proGiftClaimed: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[PRO_GIFT_CLAIMED] ?: false
  }

  val fontSize: Flow<String> = context.dataStore.data.map { prefs ->
    prefs[FONT_SIZE] ?: "medium"
  }

  val fontStyle: Flow<String> = context.dataStore.data.map { prefs ->
    prefs[FONT_STYLE] ?: "sans"
  }

  val dismissedAnnouncementHash: Flow<String> = context.dataStore.data.map { prefs ->
    prefs[DISMISSED_ANNOUNCEMENT_HASH] ?: ""
  }

  val totalNotesCreated: Flow<Int> = context.dataStore.data.map { prefs ->
    prefs[TOTAL_NOTES_CREATED] ?: 0
  }

  val starNudgeDismissed: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[STAR_NUDGE_DISMISSED] ?: false
  }

  suspend fun setThemeMode(mode: String) {
    context.dataStore.edit { prefs ->
      prefs[THEME_MODE] = mode
    }
  }

  suspend fun setSecretPassword(password: String) {
    val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val saltB64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
    val hashHex = pbkdf2Hash(password, saltBytes)
    context.dataStore.edit { prefs ->
      prefs[SECRET_PASSWORD_SALT] = saltB64
      prefs[SECRET_PASSWORD_HASH] = hashHex
    }
  }

  suspend fun verifySecretPassword(candidate: String): Boolean {
    if (candidate.isEmpty()) return false
    val prefs = context.dataStore.data.first()
    val storedHash = prefs[SECRET_PASSWORD_HASH] ?: ""
    if (storedHash.isEmpty()) return false
    val storedSalt = prefs[SECRET_PASSWORD_SALT] ?: ""
    return if (storedSalt.isNotEmpty()) {
      val saltBytes = Base64.decode(storedSalt, Base64.NO_WRAP)
      val candidateHash = pbkdf2Hash(candidate, saltBytes)
      MessageDigest.isEqual(candidateHash.toByteArray(), storedHash.toByteArray())
    } else {
      val legacy = legacySha256(candidate)
      MessageDigest.isEqual(legacy.toByteArray(), storedHash.toByteArray())
    }
  }

  suspend fun setGridView(isGrid: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[IS_GRID_VIEW] = isGrid
    }
  }

  suspend fun setDefaultNoteColor(color: String) {
    context.dataStore.edit { prefs ->
      prefs[DEFAULT_NOTE_COLOR] = color
    }
  }

  suspend fun markWelcomeSeeded() {
    context.dataStore.edit { prefs ->
      prefs[HAS_SEEDED_WELCOME] = true
    }
  }

  suspend fun addSavedLabel(label: String) {
    val cleaned = label.trim()
    if (cleaned.isEmpty()) return
    context.dataStore.edit { prefs ->
      val current = prefs[SAVED_LABELS] ?: emptySet()
      prefs[SAVED_LABELS] = current + cleaned
    }
  }

  suspend fun setSortOrder(order: String) {
    context.dataStore.edit { prefs ->
      prefs[SORT_ORDER] = order
    }
  }

  suspend fun setCustomNoteOrder(ids: List<Long>) {
    context.dataStore.edit { prefs ->
      prefs[CUSTOM_NOTE_ORDER] = ids.distinct().joinToString(",")
    }
  }

  suspend fun setEnableSharing(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[ENABLE_SHARING] = enabled
    }
  }

  suspend fun setMoveTickedToBottom(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[MOVE_TICKED_TO_BOTTOM] = enabled
    }
  }

  suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[HAPTIC_FEEDBACK_ENABLED] = enabled
    }
  }

  suspend fun ensureFirstInstallTimeRecorded(): Long {
    var recorded = 0L
    context.dataStore.edit { prefs ->
      val existing = prefs[FIRST_INSTALL_TIME_MS] ?: 0L
      if (existing <= 0L) {
        recorded = System.currentTimeMillis()
        prefs[FIRST_INSTALL_TIME_MS] = recorded
      } else {
        recorded = existing
      }
    }
    return recorded
  }

  suspend fun claimProGift() {
    context.dataStore.edit { prefs ->
      prefs[PRO_SELF_DESTRUCT] = true
      prefs[PRO_INLINE_CALC] = true
      prefs[PRO_AUTO_EMPTY_TRASH] = true
      prefs[PRO_CODE_BLOCKS] = true
      prefs[PRO_VERSION_HISTORY] = true
      prefs[PRO_CLIPBOARD_PILL] = true
      prefs[PRO_AUTO_CATEGORIES] = true
      prefs[PRO_GIFT_RESOLVED] = true
      prefs[PRO_GIFT_CLAIMED] = true
    }
  }

  suspend fun dismissProGift() {
    context.dataStore.edit { prefs ->
      prefs[PRO_GIFT_RESOLVED] = true
    }
  }

  suspend fun setAllProFeatures(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[PRO_SELF_DESTRUCT] = enabled
      prefs[PRO_INLINE_CALC] = enabled
      prefs[PRO_AUTO_EMPTY_TRASH] = enabled
      prefs[PRO_CODE_BLOCKS] = enabled
      prefs[PRO_VERSION_HISTORY] = enabled
      prefs[PRO_CLIPBOARD_PILL] = enabled
      prefs[PRO_AUTO_CATEGORIES] = enabled
      if (enabled) {
        prefs[PRO_GIFT_RESOLVED] = true
        prefs[PRO_GIFT_CLAIMED] = true
      }
    }
  }

  suspend fun setProSelfDestruct(enabled: Boolean) {
    context.dataStore.edit { it[PRO_SELF_DESTRUCT] = enabled }
  }

  suspend fun setProInlineCalc(enabled: Boolean) {
    context.dataStore.edit { it[PRO_INLINE_CALC] = enabled }
  }

  suspend fun setProAutoEmptyTrash(enabled: Boolean) {
    context.dataStore.edit { it[PRO_AUTO_EMPTY_TRASH] = enabled }
  }

  suspend fun setProCodeBlocks(enabled: Boolean) {
    context.dataStore.edit { it[PRO_CODE_BLOCKS] = enabled }
  }

  suspend fun setProVersionHistory(enabled: Boolean) {
    context.dataStore.edit { it[PRO_VERSION_HISTORY] = enabled }
  }

  suspend fun setProClipboardPill(enabled: Boolean) {
    context.dataStore.edit { it[PRO_CLIPBOARD_PILL] = enabled }
  }

  suspend fun setProAutoCategories(enabled: Boolean) {
    context.dataStore.edit { it[PRO_AUTO_CATEGORIES] = enabled }
  }

  suspend fun setFontSize(size: String) {
    context.dataStore.edit { it[FONT_SIZE] = size }
  }

  suspend fun setFontStyle(style: String) {
    context.dataStore.edit { it[FONT_STYLE] = style }
  }

  suspend fun getCachedRemoteConfigJson(): String {
    return context.dataStore.data.first()[CACHED_REMOTE_CONFIG_JSON] ?: ""
  }

  suspend fun getLastConfigFetchMs(): Long {
    return context.dataStore.data.first()[LAST_CONFIG_FETCH_MS] ?: 0L
  }

  suspend fun saveCachedRemoteConfig(rawJson: String, fetchedAtMs: Long) {
    context.dataStore.edit { prefs ->
      prefs[CACHED_REMOTE_CONFIG_JSON] = rawJson
      prefs[LAST_CONFIG_FETCH_MS] = fetchedAtMs
    }
  }

  suspend fun getCachedVersionJson(): String {
    return context.dataStore.data.first()[CACHED_VERSION_JSON] ?: ""
  }

  suspend fun getLastVersionFetchMs(): Long {
    return context.dataStore.data.first()[LAST_VERSION_FETCH_MS] ?: 0L
  }

  suspend fun saveCachedVersionInfo(rawJson: String, fetchedAtMs: Long) {
    context.dataStore.edit { prefs ->
      prefs[CACHED_VERSION_JSON] = rawJson
      prefs[LAST_VERSION_FETCH_MS] = fetchedAtMs
    }
  }

  suspend fun dismissAnnouncement(hash: String) {
    context.dataStore.edit { it[DISMISSED_ANNOUNCEMENT_HASH] = hash }
  }

  suspend fun incrementTotalNotesCreated() {
    context.dataStore.edit { prefs ->
      val current = prefs[TOTAL_NOTES_CREATED] ?: 0
      prefs[TOTAL_NOTES_CREATED] = current + 1
    }
  }

  suspend fun dismissStarNudge() {
    context.dataStore.edit { it[STAR_NUDGE_DISMISSED] = true }
  }

  private fun pbkdf2Hash(password: String, salt: ByteArray): String {
    val spec = PBEKeySpec(password.toCharArray(), salt, 65_536, 256)
    val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    return skf.generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }
  }

  private fun legacySha256(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
  }
}
