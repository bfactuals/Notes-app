package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class RemoteConfigRepository(
  private val preferences: VeilPreferences
) {
  companion object {
    const val CONFIG_URL = "https://raw.githubusercontent.com/bfactuals/Notes-app/main/config.json"
    const val VERSION_URL = "https://raw.githubusercontent.com/bfactuals/Notes-app/main/version.json"
    const val AUTO_REFRESH_COOLDOWN_MS = 5L * 60L * 1000L // 5 minutes rate limit
    private const val MAX_JSON_BYTES = 256 * 1024 // 256 KB safety cap
  }

  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .callTimeout(15, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

  suspend fun loadCachedConfig(): RemoteConfig {
    val cachedRaw = preferences.getCachedRemoteConfigJson()
    return if (cachedRaw.isNotBlank()) {
      RemoteConfigParser.parseConfig(cachedRaw) ?: RemoteConfig()
    } else {
      RemoteConfig()
    }
  }

  /**
   * Fetches config.json with rate-limiting (bypassed when force = true).
   * On success, validates & caches locally. On error/offline, silently falls back to cache or safe defaults.
   */
  suspend fun fetchConfig(force: Boolean): Pair<RemoteConfig, Boolean> = withContext(Dispatchers.IO) {
    val cached = loadCachedConfig()
    val now = System.currentTimeMillis()
    if (!force) {
      val lastFetch = preferences.getLastConfigFetchMs()
      if (lastFetch > 0L && (now - lastFetch) < AUTO_REFRESH_COOLDOWN_MS) {
        return@withContext cached to false
      }
    }

    val raw = fetchUtf8Safely(CONFIG_URL) ?: return@withContext cached to false
    val parsed = RemoteConfigParser.parseConfig(raw) ?: return@withContext cached to false
    preferences.saveCachedRemoteConfig(raw, now)
    parsed to true
  }

  /**
   * Fetches version.json separately. Returns null silently if offline, rate-limited, or malformed.
   */
  suspend fun fetchVersionInfo(force: Boolean): RemoteVersionInfo? = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    if (!force) {
      val lastFetch = preferences.getLastVersionFetchMs()
      if (lastFetch > 0L && (now - lastFetch) < AUTO_REFRESH_COOLDOWN_MS) {
        val cachedRaw = preferences.getCachedVersionJson()
        return@withContext if (cachedRaw.isNotBlank()) RemoteConfigParser.parseVersionInfo(cachedRaw) else null
      }
    }

    val raw = fetchUtf8Safely(VERSION_URL)
    if (raw != null) {
      val parsed = RemoteConfigParser.parseVersionInfo(raw)
      if (parsed != null) {
        preferences.saveCachedVersionInfo(raw, now)
        return@withContext parsed
      }
    }
    val cachedRaw = preferences.getCachedVersionJson()
    if (cachedRaw.isNotBlank()) RemoteConfigParser.parseVersionInfo(cachedRaw) else null
  }

  /**
   * Checks whether a validated HTTPS image URL (e.g., UPI QR image) is reachable and returns an image response.
   */
  suspend fun isImageUrlReachable(url: String): Boolean = withContext(Dispatchers.IO) {
    if (!RemoteConfigValidator.isValidHttpsUrl(url)) return@withContext false
    runCatching {
      val headReq = Request.Builder()
        .url(url)
        .head()
        .header("User-Agent", "VeilNotes-Android")
        .build()
      client.newCall(headReq).execute().use { resp ->
        if (resp.isSuccessful) {
          val ct = resp.header("Content-Type").orEmpty().lowercase()
          return@use ct.isEmpty() || ct.startsWith("image/") || ct.contains("octet-stream")
        }
      }
      // Fallback to tiny ranged GET for servers that reject HEAD
      val getReq = Request.Builder()
        .url(url)
        .get()
        .header("Range", "bytes=0-32")
        .header("User-Agent", "VeilNotes-Android")
        .build()
      client.newCall(getReq).execute().use { resp ->
        if (!resp.isSuccessful) return@use false
        val ct = resp.header("Content-Type").orEmpty().lowercase()
        ct.isEmpty() || ct.startsWith("image/") || ct.contains("octet-stream")
      }
    }.getOrDefault(false)
  }

  private fun fetchUtf8Safely(url: String): String? {
    if (!RemoteConfigValidator.isValidHttpsUrl(url)) return null
    return runCatching {
      val request = Request.Builder()
        .url(url)
        .get()
        .header("Accept", "application/json")
        .header("Cache-Control", "no-cache")
        .header("User-Agent", "VeilNotes-Android")
        .build()

      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return@use null
        val body = response.body ?: return@use null
        val declaredLen = body.contentLength()
        if (declaredLen > MAX_JSON_BYTES) return@use null
        val stream = body.byteStream()
        val buffer = ByteArray(8192)
        val out = java.io.ByteArrayOutputStream()
        var total = 0
        while (true) {
          val read = stream.read(buffer)
          if (read == -1) break
          total += read
          if (total > MAX_JSON_BYTES) {
            throw IOException("Payload exceeded safety limit")
          }
          out.write(buffer, 0, read)
        }
        out.toString(Charsets.UTF_8.name())
      }
    }.getOrNull()
  }
}
