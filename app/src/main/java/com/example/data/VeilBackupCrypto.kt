package com.example.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM encrypted backup & restore using PBKDF2WithHmacSHA256 key derivation.
 * Output bytes are opaque Base64 ciphertext — zero readable metadata or note contents.
 */
object VeilBackupCrypto {
  private const val VERSION: Byte = 1
  private const val SALT_BYTES = 16
  private const val IV_BYTES = 12
  private const val TAG_BITS = 128
  private const val ITERATIONS = 120_000
  private const val KEY_BITS = 256

  fun encryptNotes(notes: List<NoteEntity>, password: String): ByteArray {
    require(password.isNotEmpty()) { "Password cannot be empty" }
    val jsonArray = JSONArray()
    notes.forEach { n ->
      val obj = JSONObject().apply {
        put("title", n.title)
        put("content", n.content)
        put("color", n.color)
        put("isPinned", n.isPinned)
        put("isArchived", n.isArchived)
        put("isTrashed", n.isTrashed)
        put("isHidden", n.isHidden)
        put("labels", n.labels)
        put("checklistJson", n.checklistJson)
        put("reminderTime", n.reminderTime)
        put("createdAt", n.createdAt)
        put("updatedAt", n.updatedAt)
      }
      jsonArray.put(obj)
    }
    val payload = JSONObject().apply {
      put("v", 1)
      put("notes", jsonArray)
    }.toString().toByteArray(Charsets.UTF_8)

    val rng = SecureRandom()
    val salt = ByteArray(SALT_BYTES).also { rng.nextBytes(it) }
    val iv = ByteArray(IV_BYTES).also { rng.nextBytes(it) }
    val key = deriveKey(password, salt)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
    val ciphertext = cipher.doFinal(payload)

    val packed = ByteArray(1 + SALT_BYTES + IV_BYTES + ciphertext.size)
    packed[0] = VERSION
    System.arraycopy(salt, 0, packed, 1, SALT_BYTES)
    System.arraycopy(iv, 0, packed, 1 + SALT_BYTES, IV_BYTES)
    System.arraycopy(ciphertext, 0, packed, 1 + SALT_BYTES + IV_BYTES, ciphertext.size)

    return Base64.encodeToString(packed, Base64.NO_WRAP).toByteArray(Charsets.US_ASCII)
  }

  fun decryptNotes(encryptedBytes: ByteArray, password: String): List<NoteEntity> {
    if (password.isEmpty() || encryptedBytes.isEmpty()) {
      throw IllegalArgumentException("Incorrect password or invalid file.")
    }
    val rawText = String(encryptedBytes, Charsets.US_ASCII).trim()
    val packed = Base64.decode(rawText, Base64.DEFAULT)
    if (packed.size < 1 + SALT_BYTES + IV_BYTES + 16 || packed[0] != VERSION) {
      throw IllegalArgumentException("Incorrect password or invalid file.")
    }

    val salt = packed.copyOfRange(1, 1 + SALT_BYTES)
    val iv = packed.copyOfRange(1 + SALT_BYTES, 1 + SALT_BYTES + IV_BYTES)
    val ciphertext = packed.copyOfRange(1 + SALT_BYTES + IV_BYTES, packed.size)

    val key = deriveKey(password, salt)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
    val plaintext = cipher.doFinal(ciphertext)

    val root = JSONObject(String(plaintext, Charsets.UTF_8))
    val arr = root.getJSONArray("notes")
    val result = ArrayList<NoteEntity>(arr.length())
    for (i in 0 until arr.length()) {
      val obj = arr.getJSONObject(i)
      result.add(
        NoteEntity(
          id = 0L,
          title = obj.optString("title", ""),
          content = obj.optString("content", ""),
          color = obj.optString("color", "plain"),
          isPinned = obj.optBoolean("isPinned", false),
          isArchived = obj.optBoolean("isArchived", false),
          isTrashed = obj.optBoolean("isTrashed", false),
          isHidden = obj.optBoolean("isHidden", false),
          labels = obj.optString("labels", ""),
          checklistJson = obj.optString("checklistJson", ""),
          reminderTime = obj.optLong("reminderTime", 0L),
          createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
          updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
        )
      )
    }
    return result
  }

  private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
    val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
    val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val bytes = skf.generateSecret(spec).encoded
    return SecretKeySpec(bytes, "AES")
  }
}
