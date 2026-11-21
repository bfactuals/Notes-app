package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NoteEntity
import com.example.data.NoteExtrasCodec
import com.example.data.NoteExtrasData
import com.example.data.NoteRepository
import com.example.data.NoteVersionSnapshot
import com.example.data.ProFeaturesConfig
import com.example.data.RichSpanData
import com.example.data.VeilBackupCrypto
import com.example.data.VeilDatabase
import com.example.data.VeilPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VeilViewModel(application: Application) : AndroidViewModel(application) {
  companion object {
    private const val THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L
    private const val SELF_DESTRUCT_CHECK_INTERVAL_MS = 30_000L
  }

  private val repository: NoteRepository
  private val preferences: VeilPreferences

  // Tracks last offered clipboard string in session so dismissed/saved snippets aren't re-prompted
  private var lastHandledClipboardText: String? = null

  init {
    val db = VeilDatabase.getDatabase(application)
    repository = NoteRepository(db.noteDao())
    preferences = VeilPreferences(application)
    seedWelcomeNoteIfFirstLaunch()
    runStartupMaintenance()
    startPeriodicSelfDestructWatcher()
  }

  val activeNotes: StateFlow<List<NoteEntity>> = repository.activeNotes
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val hiddenNotes: StateFlow<List<NoteEntity>> = repository.hiddenNotes
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val archivedNotes: StateFlow<List<NoteEntity>> = repository.archivedNotes
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val trashedNotes: StateFlow<List<NoteEntity>> = repository.trashedNotes
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val reminderNotes: StateFlow<List<NoteEntity>> = repository.reminderNotes
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val themeMode: StateFlow<String> = preferences.themeMode
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

  val secretPasswordHash: StateFlow<String> = preferences.secretPasswordHash
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

  val isGridView: StateFlow<Boolean> = preferences.isGridView
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  val defaultNoteColor: StateFlow<String> = preferences.defaultNoteColor
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "plain")

  val sortOrder: StateFlow<String> = preferences.sortOrder
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "custom")

  val customNoteOrder: StateFlow<List<Long>> = preferences.customNoteOrder
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val enableSharing: StateFlow<Boolean> = preferences.enableSharing
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  val moveTickedToBottom: StateFlow<Boolean> = preferences.moveTickedToBottom
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val hapticFeedbackEnabled: StateFlow<Boolean> = preferences.hapticFeedbackEnabled
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  val proFeatures: StateFlow<ProFeaturesConfig> = preferences.proFeatures
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProFeaturesConfig())

  val fontSize: StateFlow<String> = preferences.fontSize
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "medium")

  val fontStyle: StateFlow<String> = preferences.fontStyle
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "sans")

  val allLabels: StateFlow<List<String>> = combine(
    repository.activeNotes,
    repository.archivedNotes,
    preferences.savedLabels
  ) { active, archived, saved ->
    val fromNotes = (active + archived)
      .flatMap { it.labels.split(",") }
      .map { it.trim() }
      .filter { it.isNotEmpty() }
    (saved + fromNotes).distinct().sortedBy { it.lowercase() }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // UI state
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery

  private val _areHiddenNotesRevealed = MutableStateFlow(false)
  val areHiddenNotesRevealed: StateFlow<Boolean> = _areHiddenNotesRevealed

  // "notes", "reminders", "labels"
  private val _currentDrawerScreen = MutableStateFlow("notes")
  val currentDrawerScreen: StateFlow<String> = _currentDrawerScreen

  private val _selectedLabelFilter = MutableStateFlow<String?>(null)
  val selectedLabelFilter: StateFlow<String?> = _selectedLabelFilter

  private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
  val selectedNoteIds: StateFlow<Set<Long>> = _selectedNoteIds

  private fun runStartupMaintenance() {
    viewModelScope.launch(Dispatchers.IO) {
      purgeExpiredSelfDestructNotesNow()
      val config = preferences.proFeatures.first()
      if (config.autoEmptyTrashEnabled) {
        purgeExpiredTrashNotesNow()
      }
    }
  }

  private fun startPeriodicSelfDestructWatcher() {
    viewModelScope.launch(Dispatchers.IO) {
      while (isActive) {
        delay(SELF_DESTRUCT_CHECK_INTERVAL_MS)
        purgeExpiredSelfDestructNotesNow()
      }
    }
  }

  fun onAppForegrounded() {
    viewModelScope.launch(Dispatchers.IO) {
      purgeExpiredSelfDestructNotesNow()
      val config = preferences.proFeatures.first()
      if (config.autoEmptyTrashEnabled) {
        purgeExpiredTrashNotesNow()
      }
    }
  }

  private suspend fun purgeExpiredSelfDestructNotesNow() {
    val now = System.currentTimeMillis()
    val allNotes = repository.getAllNotesSnapshot()
    allNotes.forEach { note ->
      val extras = NoteExtrasCodec.decode(note.checklistJson)
      if (extras.expiresAt > 0L && extras.expiresAt <= now) {
        // Permanently delete bypassing Trash
        repository.deleteNoteById(note.id)
      }
    }
  }

  private suspend fun purgeExpiredTrashNotesNow() {
    val now = System.currentTimeMillis()
    val cutoff = now - THIRTY_DAYS_MS
    val allNotes = repository.getAllNotesSnapshot()
    allNotes.forEach { note ->
      if (note.isTrashed) {
        val extras = NoteExtrasCodec.decode(note.checklistJson)
        val trashedSince = if (extras.deletedAt > 0L) extras.deletedAt else note.updatedAt
        if (trashedSince in 1L..cutoff) {
          repository.deleteNoteById(note.id)
        }
      }
    }
  }

  fun shouldOfferClipboardText(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed == lastHandledClipboardText) return false
    val alreadyExists = (activeNotes.value + hiddenNotes.value + archivedNotes.value).any { n ->
      n.content.trim() == trimmed || n.title.trim() == trimmed
    }
    return !alreadyExists
  }

  fun markClipboardTextHandled(text: String) {
    lastHandledClipboardText = text.trim()
  }

  fun quickSaveClipboardNote(text: String) {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return
    lastHandledClipboardText = trimmed
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      val defaultCol = preferences.defaultNoteColor.first()
      val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
      val autoTitle = if (lines.size > 1 && lines.first().length <= 60) lines.first() else ""
      val autoContent = if (autoTitle.isNotEmpty()) trimmed.substringAfter("\n").trim() else trimmed
      val newNote = NoteEntity(
        title = autoTitle,
        content = autoContent,
        color = defaultCol,
        createdAt = now,
        updatedAt = now
      )
      val newId = repository.insertNote(newNote)
      val existingOrder = preferences.customNoteOrder.first()
      if (existingOrder.isNotEmpty()) {
        preferences.setCustomNoteOrder(listOf(newId) + existingOrder)
      }
    }
  }

  private fun seedWelcomeNoteIfFirstLaunch() {
    viewModelScope.launch {
      val alreadySeeded = preferences.hasSeededWelcome.first()
      if (!alreadySeeded) {
        preferences.markWelcomeSeeded()
        val now = System.currentTimeMillis()
        val body = buildString {
          appendLine("1. Everyday Features")
          appendLine("• Format notes seamlessly with Bold, Italic, Underline, Strikethrough, Highlight, and Headings using the Aa bar — zero raw symbols.")
          appendLine("• Tap + in the editor to attach photos, extract text from images (OCR), start a checklist, sketch full-page or inline, or dictate voice notes.")
          appendLine("• Long-press an inline Draw Box to resize smoothly, or swipe cards on Home to archive.")
          appendLine("• Use Read Mode from the editor menu for distraction-free scrolling with clickable links.")
          appendLine()
          appendLine("2. Secret Vault Instructions")
          appendLine("• Create Secret Code: Open Settings and tap \"Version 1.0\" 5 times rapidly.")
          appendLine("• Change Secret Code: Once set, 5 taps does nothing — tap \"Version 1.0\" 7 times rapidly and verify your old code first.")
          appendLine("• Silently Hide / Unhide a Note: Press and hold (1.5 seconds) on the Pin icon inside any note.")
          appendLine("• Open Hidden Notes: Type your exact Secret Code in the Search Bar and press Enter / Search. Clear the search to hide them again.")
          appendLine("• Encrypted Backups: Exported backups are AES-GCM encrypted and always require your password to open or restore.")
          appendLine()
          append("⚠️ PRIVACY REMINDER: Once you've memorized these steps, delete this note (and empty your Trash) so nobody else can ever discover the hidden features of this app.")
        }

        // Clean WYSIWYG rich spans & H2 section headings (zero Markdown syntax symbols)
        val spans = mutableListOf<RichSpanData>()
        fun addSpan(target: String, style: String) {
          val idx = body.indexOf(target)
          if (idx >= 0) spans.add(RichSpanData(idx, idx + target.length, style))
        }
        addSpan("Bold", "b")
        addSpan("Italic", "i")
        addSpan("Underline", "u")
        addSpan("Strikethrough", "s")
        addSpan("Highlight", "hl")
        addSpan("Create Secret Code:", "b")
        addSpan("Change Secret Code:", "b")
        addSpan("Silently Hide / Unhide a Note:", "b")
        addSpan("Open Hidden Notes:", "b")
        addSpan("Encrypted Backups:", "b")
        addSpan("⚠️ PRIVACY REMINDER:", "b")
        addSpan("⚠️ PRIVACY REMINDER:", "hl")

        val headings = mapOf(
          0 to 2, // "1. Everyday Features" -> H2
          6 to 2  // "2. Secret Vault Instructions" -> H2
        )

        val welcomeExtras = NoteExtrasCodec.encode(
          NoteExtrasData(
            richSpans = spans,
            lineHeadings = headings
          )
        )

        val welcomeNote = NoteEntity(
          title = "Welcome to Veil — Quick Guide",
          content = body,
          color = "lavender",
          isPinned = true,
          checklistJson = welcomeExtras,
          createdAt = now,
          updatedAt = now
        )
        repository.insertNote(welcomeNote)
      }
    }
  }

  /**
   * Plain search typing: never auto-reveals hidden notes while typing.
   * Clearing the search immediately hides unlocked notes again.
   */
  fun setSearchQuery(query: String) {
    _searchQuery.value = query
    if (query.isEmpty()) {
      _areHiddenNotesRevealed.value = false
    }
  }

  /**
   * Explicitly triggered only when the user presses Enter / Search on the keyboard.
   * Silently unlocks hidden notes if the exact code matches; gives zero feedback otherwise.
   */
  fun submitSearch() {
    val candidate = _searchQuery.value.trim()
    if (candidate.isEmpty()) {
      _areHiddenNotesRevealed.value = false
      return
    }
    viewModelScope.launch {
      val matched = preferences.verifySecretPassword(candidate)
      if (matched) {
        _areHiddenNotesRevealed.value = true
      }
    }
  }

  suspend fun verifyCurrentPassword(candidate: String): Boolean {
    return preferences.verifySecretPassword(candidate)
  }

  fun panicHide() {
    _areHiddenNotesRevealed.value = false
    _searchQuery.value = ""
  }

  fun setDrawerScreen(screen: String, labelFilter: String? = null) {
    _currentDrawerScreen.value = screen
    _selectedLabelFilter.value = labelFilter
    _selectedNoteIds.value = emptySet()
  }

  fun clearLabelFilter() {
    _selectedLabelFilter.value = null
    if (_currentDrawerScreen.value == "labels") {
      _currentDrawerScreen.value = "notes"
    }
  }

  fun addGlobalLabel(label: String) {
    viewModelScope.launch {
      preferences.addSavedLabel(label)
    }
  }

  fun toggleNoteSelection(id: Long) {
    _selectedNoteIds.update { selected ->
      if (selected.contains(id)) selected - id else selected + id
    }
  }

  fun clearSelection() {
    _selectedNoteIds.value = emptySet()
  }

  fun setThemeMode(mode: String) {
    viewModelScope.launch { preferences.setThemeMode(mode) }
  }

  fun setSecretPassword(password: String) {
    viewModelScope.launch { preferences.setSecretPassword(password) }
  }

  fun setGridView(isGrid: Boolean) {
    viewModelScope.launch { preferences.setGridView(isGrid) }
  }

  fun setDefaultNoteColor(color: String) {
    viewModelScope.launch { preferences.setDefaultNoteColor(color) }
  }

  fun setSortOrder(order: String) {
    viewModelScope.launch { preferences.setSortOrder(order) }
  }

  fun updateCustomNoteOrder(orderedIds: List<Long>) {
    viewModelScope.launch {
      preferences.setSortOrder("custom")
      preferences.setCustomNoteOrder(orderedIds)
    }
  }

  fun setEnableSharing(enabled: Boolean) {
    viewModelScope.launch { preferences.setEnableSharing(enabled) }
  }

  fun setMoveTickedToBottom(enabled: Boolean) {
    viewModelScope.launch { preferences.setMoveTickedToBottom(enabled) }
  }

  fun setHapticFeedbackEnabled(enabled: Boolean) {
    viewModelScope.launch { preferences.setHapticFeedbackEnabled(enabled) }
  }

  // Pro Features setters
  fun setAllProFeatures(enabled: Boolean) {
    viewModelScope.launch {
      preferences.setAllProFeatures(enabled)
      if (enabled) {
        withContext(Dispatchers.IO) { purgeExpiredTrashNotesNow() }
      }
    }
  }

  fun setProSelfDestruct(enabled: Boolean) {
    viewModelScope.launch { preferences.setProSelfDestruct(enabled) }
  }

  fun setProInlineCalc(enabled: Boolean) {
    viewModelScope.launch { preferences.setProInlineCalc(enabled) }
  }

  fun setProAutoEmptyTrash(enabled: Boolean) {
    viewModelScope.launch {
      preferences.setProAutoEmptyTrash(enabled)
      if (enabled) {
        withContext(Dispatchers.IO) { purgeExpiredTrashNotesNow() }
      }
    }
  }

  fun setProCodeBlocks(enabled: Boolean) {
    viewModelScope.launch { preferences.setProCodeBlocks(enabled) }
  }

  fun setProVersionHistory(enabled: Boolean) {
    viewModelScope.launch { preferences.setProVersionHistory(enabled) }
  }

  fun setProClipboardPill(enabled: Boolean) {
    viewModelScope.launch { preferences.setProClipboardPill(enabled) }
  }

  fun setProAutoCategories(enabled: Boolean) {
    viewModelScope.launch { preferences.setProAutoCategories(enabled) }
  }

  // Typography setters
  fun setFontSize(size: String) {
    viewModelScope.launch { preferences.setFontSize(size) }
  }

  fun setFontStyle(style: String) {
    viewModelScope.launch { preferences.setFontStyle(style) }
  }

  fun saveNote(
    id: Long = 0L,
    title: String,
    content: String,
    color: String,
    isPinned: Boolean,
    isHidden: Boolean,
    labels: String,
    checklistJson: String,
    reminderTime: Long
  ) {
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      val parsedLabels = labels.split(",").map { it.trim() }.filter { it.isNotEmpty() }
      parsedLabels.forEach { preferences.addSavedLabel(it) }

      if (id == 0L) {
        val note = NoteEntity(
          title = title,
          content = content,
          color = color,
          isPinned = isPinned,
          isHidden = isHidden,
          labels = parsedLabels.joinToString(", "),
          checklistJson = checklistJson,
          reminderTime = reminderTime,
          createdAt = now,
          updatedAt = now
        )
        val newId = repository.insertNote(note)
        val existingOrder = preferences.customNoteOrder.first()
        if (existingOrder.isNotEmpty()) {
          preferences.setCustomNoteOrder(listOf(newId) + existingOrder)
        }
      } else {
        val existing = repository.getNoteById(id)
        if (existing != null) {
          val proCfg = preferences.proFeatures.first()
          val incomingExtras = NoteExtrasCodec.decode(checklistJson)
          val oldExtras = NoteExtrasCodec.decode(existing.checklistJson)

          // Local Version History: record lightweight snapshot of previous state on meaningful edit (max 5)
          val finalExtras = if (proCfg.versionHistoryEnabled) {
            val prevHadText = existing.title.isNotBlank() || existing.content.isNotBlank()
            val meaningfulChange = prevHadText &&
              (existing.title.trim() != title.trim() || existing.content.trim() != content.trim())
            if (meaningfulChange) {
              val snapshot = NoteVersionSnapshot(
                timestamp = existing.updatedAt.takeIf { it > 0L } ?: now,
                title = existing.title,
                content = existing.content,
                richSpans = oldExtras.richSpans,
                lineHeadings = oldExtras.lineHeadings
              )
              val mergedHistory = (listOf(snapshot) + oldExtras.versionHistory)
                .distinctBy { "${it.title}\u0000${it.content}" }
                .take(5)
              incomingExtras.copy(versionHistory = mergedHistory)
            } else {
              incomingExtras.copy(versionHistory = oldExtras.versionHistory)
            }
          } else {
            incomingExtras.copy(versionHistory = oldExtras.versionHistory)
          }

          val updated = existing.copy(
            title = title,
            content = content,
            color = color,
            isPinned = isPinned,
            isHidden = isHidden,
            labels = parsedLabels.joinToString(", "),
            checklistJson = NoteExtrasCodec.encode(finalExtras),
            reminderTime = reminderTime,
            updatedAt = now
          )
          repository.updateNote(updated)
        }
      }
    }
  }

  suspend fun getNoteById(id: Long): NoteEntity? = repository.getNoteById(id)

  fun togglePin(note: NoteEntity) {
    viewModelScope.launch {
      repository.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }
  }

  fun setNotesPinned(notes: List<NoteEntity>, pinned: Boolean) {
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      notes.forEach { note ->
        repository.updateNote(note.copy(isPinned = pinned, updatedAt = now))
      }
    }
  }

  fun toggleHide(note: NoteEntity) {
    viewModelScope.launch {
      repository.updateNote(note.copy(isHidden = !note.isHidden, updatedAt = System.currentTimeMillis()))
    }
  }

  fun archiveNote(note: NoteEntity) {
    viewModelScope.launch {
      repository.updateNote(note.copy(isArchived = true, isPinned = false, updatedAt = System.currentTimeMillis()))
    }
  }

  fun unarchiveNote(note: NoteEntity, restorePinned: Boolean = false) {
    viewModelScope.launch {
      repository.updateNote(
        note.copy(
          isArchived = false,
          isPinned = restorePinned || note.isPinned,
          updatedAt = System.currentTimeMillis()
        )
      )
    }
  }

  fun trashNote(note: NoteEntity) {
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      val extras = NoteExtrasCodec.decode(note.checklistJson)
      val stampedJson = NoteExtrasCodec.encode(extras.copy(deletedAt = now))
      repository.updateNote(
        note.copy(
          isTrashed = true,
          isPinned = false,
          isArchived = false,
          checklistJson = stampedJson,
          updatedAt = now
        )
      )
    }
  }

  fun restoreNote(note: NoteEntity) {
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      val extras = NoteExtrasCodec.decode(note.checklistJson)
      val clearedJson = NoteExtrasCodec.encode(extras.copy(deletedAt = 0L))
      repository.updateNote(
        note.copy(
          isTrashed = false,
          checklistJson = clearedJson,
          updatedAt = now
        )
      )
    }
  }

  fun deletePermanently(note: NoteEntity) {
    viewModelScope.launch {
      repository.deleteNoteById(note.id)
    }
  }

  fun emptyTrash() {
    viewModelScope.launch {
      repository.emptyTrash()
    }
  }

  fun duplicateNote(note: NoteEntity) {
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      val copy = note.copy(
        id = 0L,
        title = if (note.title.isNotBlank()) "${note.title} (Copy)" else "",
        createdAt = now,
        updatedAt = now
      )
      val newId = repository.insertNote(copy)
      val existingOrder = preferences.customNoteOrder.first()
      if (existingOrder.isNotEmpty()) {
        preferences.setCustomNoteOrder(listOf(newId) + existingOrder)
      }
    }
  }

  fun duplicateDraftNote(
    title: String,
    content: String,
    color: String,
    isPinned: Boolean,
    isHidden: Boolean,
    labels: String,
    checklistJson: String,
    reminderTime: Long
  ) {
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      val copy = NoteEntity(
        id = 0L,
        title = if (title.isNotBlank()) "$title (Copy)" else "",
        content = content,
        color = color,
        isPinned = isPinned,
        isHidden = isHidden,
        labels = labels,
        checklistJson = checklistJson,
        reminderTime = reminderTime,
        createdAt = now,
        updatedAt = now
      )
      val newId = repository.insertNote(copy)
      val existingOrder = preferences.customNoteOrder.first()
      if (existingOrder.isNotEmpty()) {
        preferences.setCustomNoteOrder(listOf(newId) + existingOrder)
      }
    }
  }

  fun updateNotesStyle(notes: List<NoteEntity>, color: String?, pattern: String?) {
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      notes.forEach { note ->
        val nextColor = color ?: note.color
        val nextChecklistJson = if (pattern != null) {
          val extras = NoteExtrasCodec.decode(note.checklistJson)
          NoteExtrasCodec.encode(extras.copy(backgroundPattern = pattern))
        } else {
          note.checklistJson
        }
        repository.updateNote(
          note.copy(
            color = nextColor,
            checklistJson = nextChecklistJson,
            updatedAt = now
          )
        )
      }
    }
  }

  fun updateNotesReminder(notes: List<NoteEntity>, reminderTime: Long) {
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      notes.forEach { note ->
        repository.updateNote(note.copy(reminderTime = reminderTime, updatedAt = now))
      }
    }
  }

  fun toggleLabelForNotes(notes: List<NoteEntity>, label: String) {
    val cleaned = label.trim()
    if (cleaned.isEmpty()) return
    viewModelScope.launch {
      preferences.addSavedLabel(cleaned)
      val now = System.currentTimeMillis()
      val allHaveLabel = notes.all { n ->
        n.labels.split(",").map { it.trim() }.any { it.equals(cleaned, ignoreCase = true) }
      }
      notes.forEach { note ->
        val current = note.labels.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        val updated = if (allHaveLabel) {
          current.filterNot { it.equals(cleaned, ignoreCase = true) }
        } else {
          if (current.none { it.equals(cleaned, ignoreCase = true) }) current.add(cleaned)
          current
        }
        repository.updateNote(note.copy(labels = updated.joinToString(", "), updatedAt = now))
      }
    }
  }

  suspend fun exportEncryptedBackup(password: String): Result<ByteArray> = withContext(Dispatchers.IO) {
    runCatching {
      val allNotes = repository.getAllNotesSnapshot()
      VeilBackupCrypto.encryptNotes(allNotes, password)
    }
  }

  suspend fun restoreEncryptedBackup(encryptedBytes: ByteArray, password: String): Result<Int> =
    withContext(Dispatchers.IO) {
      runCatching {
        val restored = VeilBackupCrypto.decryptNotes(encryptedBytes, password)
        repository.insertNotes(restored)
        restored.forEach { n ->
          n.labels.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
            preferences.addSavedLabel(it)
          }
        }
        restored.size
      }
    }
}
