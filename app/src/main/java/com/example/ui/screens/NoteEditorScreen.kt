package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.ChecklistItemData
import com.example.data.CodeBlockData
import com.example.data.DoodleBlockData
import com.example.data.DoodleStrokeData
import com.example.data.NoteEntity
import com.example.data.NoteExtrasCodec
import com.example.data.NoteExtrasData
import com.example.data.NoteImageHelper
import com.example.data.NoteVersionSnapshot
import com.example.data.RichSpanData
import com.example.ui.components.CopyableCodeBlockCard
import com.example.ui.components.EditorDrawTool
import com.example.ui.components.FindInNoteBar
import com.example.ui.components.FloatingDrawingToolbar
import com.example.ui.components.FullPageDrawingCanvas
import com.example.ui.components.FullScreenImageViewer
import com.example.ui.components.InsertToolsPanel
import com.example.ui.components.LabelManagerDialog
import com.example.ui.components.NoteBackgroundPatternOverlay
import com.example.ui.components.NoteColorAndPatternPickerContent
import com.example.ui.components.NoteEditorBottomBar
import com.example.ui.components.NoteImagesGrid
import com.example.ui.components.NoteOverflowMenuPanel
import com.example.ui.components.ReminderPickerDialog
import com.example.ui.components.ResizableInlineDrawBox
import com.example.ui.components.RichStyle
import com.example.ui.components.RichTextFormattingBar
import com.example.ui.components.SelfDestructQuickPopup
import com.example.ui.components.VeilIcons
import com.example.ui.components.VersionHistoryDialog
import com.example.ui.components.WysiwygVisualTransformation
import com.example.ui.components.adjustRichFormatOnTextChange
import com.example.ui.components.applyHeadingToSelection
import com.example.ui.components.buildWysiwygAnnotatedString
import com.example.ui.components.detectUrls
import com.example.ui.components.evaluateInlineMathIfTriggered
import com.example.ui.components.findMatchRanges
import com.example.ui.components.formatCountdownBadge
import com.example.ui.components.formatReminderChip
import com.example.ui.components.lineIndicesForSelection
import com.example.ui.components.migrateLegacyMarkdownIfNeeded
import com.example.ui.components.stripDoodleMarkers
import com.example.ui.components.stylesActiveAtCursor
import com.example.ui.components.tactilePress
import com.example.ui.components.toggleStyleOverRange
import com.example.ui.theme.VeilEasing
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens
import com.example.viewmodel.VeilViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

private enum class ActiveEditorPanel { NONE, INSERT, TIMER, PALETTE, OVERFLOW }

private data class TextHistorySnapshot(
  val text: String,
  val selection: TextRange,
  val spans: List<RichSpanData>,
  val headings: Map<Int, Int>
)

@Composable
fun NoteEditorScreen(
  viewModel: VeilViewModel,
  noteId: Long,
  initialMode: String = "text",
  onBack: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  val reduceMotion = VeilThemeTokens.reduceMotion
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val focusManager = LocalFocusManager.current
  val uriHandler = LocalUriHandler.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val defaultNoteColor by viewModel.defaultNoteColor.collectAsState()
  val allLabels by viewModel.allLabels.collectAsState()
  val enableSharing by viewModel.enableSharing.collectAsState()
  val moveTickedToBottom by viewModel.moveTickedToBottom.collectAsState()
  val proFeatures by viewModel.proFeatures.collectAsState()

  var note by remember { mutableStateOf<NoteEntity?>(null) }
  var title by remember { mutableStateOf("") }
  var contentField by remember { mutableStateOf(TextFieldValue("")) }
  var richSpans by remember { mutableStateOf<List<RichSpanData>>(emptyList()) }
  var lineHeadings by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
  var pendingInlineStyles by remember { mutableStateOf<Set<String>>(emptySet()) }
  var isBodyFocused by remember { mutableStateOf(false) }

  // Pro Features per-note state: Self-Destruct timer, Copyable Code Blocks, Local Version History
  var expiresAt by remember { mutableLongStateOf(0L) }
  var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
  var codeBlocks by remember { mutableStateOf<List<CodeBlockData>>(emptyList()) }
  var versionHistory by remember { mutableStateOf<List<NoteVersionSnapshot>>(emptyList()) }
  var showVersionHistoryDialog by remember { mutableStateOf(false) }

  LaunchedEffect(expiresAt) {
    if (expiresAt > 0L) {
      while (true) {
        val now = System.currentTimeMillis()
        nowTick = now
        if (expiresAt <= now) {
          note?.let { viewModel.deletePermanently(it) }
          onBack()
          break
        }
        delay(30_000L)
      }
    }
  }

  // Text Undo / Redo stacks
  var textUndoStack by remember { mutableStateOf<List<TextHistorySnapshot>>(emptyList()) }
  var textRedoStack by remember { mutableStateOf<List<TextHistorySnapshot>>(emptyList()) }
  var lastUndoPushTime by remember { mutableLongStateOf(0L) }

  // Read-Only Mode
  var isReadOnlyMode by remember { mutableStateOf(false) }

  var color by remember { mutableStateOf("plain") }
  var backgroundPattern by remember { mutableStateOf("none") }
  var isPinned by remember { mutableStateOf(false) }
  var isHidden by remember { mutableStateOf(false) }
  var assignedLabels by remember { mutableStateOf<Set<String>>(emptySet()) }
  var reminderTime by remember { mutableLongStateOf(0L) }

  // Multi-image attachments & full-screen viewer + OCR state
  var imagePaths by remember { mutableStateOf<List<String>>(emptyList()) }
  var viewingImageIndex by remember { mutableStateOf<Int?>(null) }
  var isRunningOcr by remember { mutableStateOf(false) }
  var pendingCameraFile by remember { mutableStateOf<File?>(null) }

  // Checklist state
  var isChecklistMode by remember { mutableStateOf(initialMode == "checklist") }
  var checklistItems by remember {
    mutableStateOf(
      if (initialMode == "checklist") listOf(ChecklistItemData(1L, "", false)) else emptyList()
    )
  }

  // Drawing state: full-page default + 60fps resizable inline draw box + slim popover state
  var isFullPageDraw by remember { mutableStateOf(initialMode == "draw") }
  var hasInlineDrawBox by remember { mutableStateOf(false) }
  var isInlineDrawActive by remember { mutableStateOf(false) }
  var showInlineBrushPopover by remember { mutableStateOf(false) }
  var isResizeMode by remember { mutableStateOf(false) }
  var isInlineGestureLocked by remember { mutableStateOf(false) }
  var inlineBoxHeightDp by remember { mutableFloatStateOf(160f) }
  var inlineBoxWidthFrac by remember { mutableFloatStateOf(1f) }
  var strokes by remember { mutableStateOf<List<DoodleStrokeData>>(emptyList()) }
  var undoneStrokes by remember { mutableStateOf<List<DoodleStrokeData>>(emptyList()) }
  var selectedTool by remember { mutableStateOf(EditorDrawTool.PEN) }
  var selectedColorIndex by remember { mutableIntStateOf(0) }
  var toolStrokeSizes by remember {
    mutableStateOf(
      mapOf(
        EditorDrawTool.PEN to EditorDrawTool.PEN.defaultSize,
        EditorDrawTool.FOUNTAIN_PEN to EditorDrawTool.FOUNTAIN_PEN.defaultSize,
        EditorDrawTool.HIGHLIGHTER to EditorDrawTool.HIGHLIGHTER.defaultSize,
        EditorDrawTool.ERASER to EditorDrawTool.ERASER.defaultSize
      )
    )
  }

  // 4-button Bottom Bar panels, Rich Text Formatting bar, Find-in-Note, & dialogs
  var activePanel by remember { mutableStateOf(ActiveEditorPanel.NONE) }
  var showFormatBar by remember { mutableStateOf(false) }
  var showFindBar by remember { mutableStateOf(false) }
  var findQuery by remember { mutableStateOf("") }
  var activeFindIndex by remember { mutableIntStateOf(0) }
  var showReminderDialog by remember { mutableStateOf(false) }
  var showLabelDialog by remember { mutableStateOf(false) }

  val strokePalette = remember(colors) { colors.drawingPalette() }
  val findMatches = remember(contentField.text, findQuery) {
    findMatchRanges(contentField.text, findQuery)
  }
  LaunchedEffect(findMatches.size) {
    activeFindIndex = if (findMatches.isEmpty()) 0 else activeFindIndex.coerceIn(0, findMatches.lastIndex)
  }

  fun openDetectedUrl(url: String) {
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    runCatching {
      uriHandler.openUri(url)
    }.onFailure {
      scope.launch {
        snackbarHostState.showSnackbar("Could not open link.")
      }
    }
  }

  fun pushTextUndoSnapshot(force: Boolean = false, prevText: String = contentField.text, nextText: String = contentField.text) {
    val now = System.currentTimeMillis()
    val lenDiff = abs(nextText.length - prevText.length)
    val wordBoundary = nextText.endsWith(" ") || nextText.endsWith("\n") || prevText.endsWith("\n")
    val shouldPush = force ||
      textUndoStack.isEmpty() ||
      lenDiff > 1 ||
      wordBoundary ||
      (now - lastUndoPushTime) > 650L

    if (shouldPush) {
      val snapshot = TextHistorySnapshot(
        text = contentField.text,
        selection = contentField.selection,
        spans = richSpans,
        headings = lineHeadings
      )
      if (textUndoStack.lastOrNull() != snapshot) {
        textUndoStack = (textUndoStack + snapshot).takeLast(60)
      }
      lastUndoPushTime = now
    }
  }

  fun handleBodyChange(newValue: TextFieldValue) {
    val oldText = contentField.text
    val newText = newValue.text
    if (oldText != newText) {
      pushTextUndoSnapshot(force = false, prevText = oldText, nextText = newText)
      val activeForTyping = if (contentField.selection.collapsed) {
        pendingInlineStyles
      } else {
        stylesActiveAtCursor(richSpans, contentField.selection, oldText.length)
      }

      // Smart Inline Calculator: auto-solve "<expr>=" + Space/Enter safely when enabled
      if (proFeatures.inlineCalcEnabled && newValue.selection.collapsed) {
        val calcResult = evaluateInlineMathIfTriggered(oldText, newText, newValue.selection.start)
        if (calcResult != null) {
          val (calcSpans, calcHeadings) = adjustRichFormatOnTextChange(
            oldText = oldText,
            newText = calcResult.updatedText,
            oldSpans = richSpans,
            oldHeadings = lineHeadings,
            activeStyles = activeForTyping
          )
          richSpans = calcSpans
          lineHeadings = calcHeadings
          contentField = TextFieldValue(
            text = calcResult.updatedText,
            selection = TextRange(calcResult.newCursorPos)
          )
          textRedoStack = emptyList()
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          return
        }
      }

      val (nextSpans, nextHeadings) = adjustRichFormatOnTextChange(
        oldText = oldText,
        newText = newText,
        oldSpans = richSpans,
        oldHeadings = lineHeadings,
        activeStyles = activeForTyping
      )
      richSpans = nextSpans
      lineHeadings = nextHeadings
      contentField = newValue
      textRedoStack = emptyList()
    } else {
      contentField = newValue
      pendingInlineStyles = stylesActiveAtCursor(richSpans, newValue.selection, newText.length)
    }
  }

  fun performTextUndo() {
    val prev = textUndoStack.lastOrNull() ?: return
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    val current = TextHistorySnapshot(
      text = contentField.text,
      selection = contentField.selection,
      spans = richSpans,
      headings = lineHeadings
    )
    textRedoStack = (textRedoStack + current).takeLast(60)
    textUndoStack = textUndoStack.dropLast(1)
    contentField = TextFieldValue(prev.text, selection = prev.selection)
    richSpans = prev.spans
    lineHeadings = prev.headings
    pendingInlineStyles = stylesActiveAtCursor(prev.spans, prev.selection, prev.text.length)
  }

  fun performTextRedo() {
    val next = textRedoStack.lastOrNull() ?: return
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    val current = TextHistorySnapshot(
      text = contentField.text,
      selection = contentField.selection,
      spans = richSpans,
      headings = lineHeadings
    )
    textUndoStack = (textUndoStack + current).takeLast(60)
    textRedoStack = textRedoStack.dropLast(1)
    contentField = TextFieldValue(next.text, selection = next.selection)
    richSpans = next.spans
    lineHeadings = next.headings
    pendingInlineStyles = stylesActiveAtCursor(next.spans, next.selection, next.text.length)
  }

  fun applyHeadingLevel(level: Int) {
    pushTextUndoSnapshot(force = true)
    lineHeadings = applyHeadingToSelection(
      text = contentField.text,
      selection = contentField.selection,
      currentHeadings = lineHeadings,
      level = level
    )
    textRedoStack = emptyList()
  }

  fun toggleInlineRichStyle(style: String) {
    if (!contentField.selection.collapsed) {
      pushTextUndoSnapshot(force = true)
      val nextSpans = toggleStyleOverRange(
        spans = richSpans,
        range = contentField.selection,
        style = style,
        textLength = contentField.text.length
      )
      richSpans = nextSpans
      pendingInlineStyles = stylesActiveAtCursor(nextSpans, contentField.selection, contentField.text.length)
      textRedoStack = emptyList()
    } else {
      pendingInlineStyles = if (style in pendingInlineStyles) {
        pendingInlineStyles - style
      } else {
        pendingInlineStyles + style
      }
    }
  }

  val activeHeadingLevel = remember(contentField.text, contentField.selection, lineHeadings) {
    val lines = lineIndicesForSelection(contentField.text, contentField.selection)
    val first = lineHeadings[lines.first] ?: 0
    if (lines.all { (lineHeadings[it] ?: 0) == first }) first else 0
  }

  val activeInlineStyles = remember(contentField.text, contentField.selection, richSpans, pendingInlineStyles) {
    if (contentField.selection.collapsed) {
      pendingInlineStyles
    } else {
      stylesActiveAtCursor(richSpans, contentField.selection, contentField.text.length)
    }
  }

  val detectedUrls = remember(contentField.text) { detectUrls(contentField.text) }

  // Gallery multi-image picker (zero-permission Android Photo Picker)
  val pickImagesLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
  ) { uris ->
    if (uris.isNotEmpty()) {
      scope.launch {
        val copied = uris.mapNotNull { uri ->
          NoteImageHelper.copyUriToInternalStorage(context, uri)
        }
        if (copied.isNotEmpty()) {
          imagePaths = imagePaths + copied
        }
      }
    }
  }

  // Camera TakePicture launcher
  val takePictureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
  ) { success ->
    val file = pendingCameraFile
    pendingCameraFile = null
    if (success && file != null && file.exists() && file.length() > 0L) {
      imagePaths = imagePaths + file.absolutePath
    } else if (file != null && file.exists()) {
      file.delete()
    }
  }

  fun launchCameraCapture() {
    runCatching {
      val (file, uri) = NoteImageHelper.createCameraImageFile(context)
      pendingCameraFile = file
      takePictureLauncher.launch(uri)
    }.onFailure {
      scope.launch {
        snackbarHostState.showSnackbar("Camera is not available on this device.")
      }
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      launchCameraCapture()
    } else {
      scope.launch {
        snackbarHostState.showSnackbar("Camera permission is needed to take a photo.")
      }
    }
  }

  fun requestCameraCapture() {
    val hasPerm = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    if (hasPerm) launchCameraCapture() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
  }

  // Voice-to-text dictation launcher
  val speechLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val spoken = result.data
        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        ?.firstOrNull()
        ?.trim()
      if (!spoken.isNullOrEmpty()) {
        if (isChecklistMode) {
          val nextId = (checklistItems.maxOfOrNull { it.id } ?: 0L) + 1L
          checklistItems = checklistItems + ChecklistItemData(nextId, spoken, false)
        } else {
          pushTextUndoSnapshot(force = true)
          val cur = contentField.text
          val next = if (cur.isBlank()) spoken else "$cur $spoken"
          contentField = TextFieldValue(next, selection = TextRange(next.length))
          textRedoStack = emptyList()
        }
      }
    }
  }

  fun launchVoiceDictation() {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your note")
    }
    runCatching {
      speechLauncher.launch(intent)
    }.onFailure {
      scope.launch {
        snackbarHostState.showSnackbar("Voice dictation is not available on this device.")
      }
    }
  }

  val audioPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) launchVoiceDictation() else {
      scope.launch {
        snackbarHostState.showSnackbar("Microphone permission is needed for voice dictation.")
      }
    }
  }

  fun requestVoiceInput() {
    val hasPerm = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    if (hasPerm) launchVoiceDictation() else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
  }

  // Load existing note or initialize new mode (migrates any legacy Markdown symbols cleanly to WYSIWYG)
  LaunchedEffect(noteId, initialMode) {
    if (noteId != 0L) {
      val existing = viewModel.getNoteById(noteId)
      if (existing != null) {
        note = existing
        title = existing.title
        color = existing.color
        isPinned = existing.isPinned
        isHidden = existing.isHidden
        assignedLabels = existing.labels.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        reminderTime = existing.reminderTime

        val extras = NoteExtrasCodec.decode(existing.checklistJson)
        val cleanRaw = stripDoodleMarkers(existing.content)
        val (migratedText, migratedSpans, migratedHeadings) = migrateLegacyMarkdownIfNeeded(
          rawText = cleanRaw,
          existingSpans = extras.richSpans,
          existingHeadings = extras.lineHeadings
        )
        contentField = TextFieldValue(migratedText, selection = TextRange(migratedText.length))
        richSpans = migratedSpans
        lineHeadings = migratedHeadings
        pendingInlineStyles = emptySet()
        textUndoStack = emptyList()
        textRedoStack = emptyList()

        isChecklistMode = extras.isChecklistMode || extras.checklist.isNotEmpty()
        checklistItems = extras.checklist
        imagePaths = extras.images.filter { File(it).exists() }
        backgroundPattern = extras.backgroundPattern
        expiresAt = extras.expiresAt
        codeBlocks = extras.codeBlocks
        versionHistory = extras.versionHistory
        extras.doodle?.let { d ->
          strokes = d.strokes
          inlineBoxHeightDp = d.heightDp
          inlineBoxWidthFrac = d.widthFraction
          if (d.isFullPage && migratedText.isBlank() && checklistItems.isEmpty() && imagePaths.isEmpty() && codeBlocks.isEmpty()) {
            isFullPageDraw = true
          } else {
            hasInlineDrawBox = true
          }
        }
      }
    } else {
      color = defaultNoteColor
      if (initialMode == "voice") {
        requestVoiceInput()
      }
    }
  }

  fun buildExtrasJson(): String {
    val cleanChecklist = checklistItems.filter { it.text.isNotBlank() }
    val cleanCodeBlocks = codeBlocks.filter { it.text.isNotBlank() }
    val doodleData = if (strokes.isNotEmpty() || hasInlineDrawBox || isFullPageDraw) {
      DoodleBlockData(
        strokes = strokes,
        heightDp = inlineBoxHeightDp,
        widthFraction = inlineBoxWidthFrac,
        isFullPage = isFullPageDraw && contentField.text.isBlank() && cleanChecklist.isEmpty() && imagePaths.isEmpty() && cleanCodeBlocks.isEmpty()
      )
    } else {
      null
    }
    return NoteExtrasCodec.encode(
      NoteExtrasData(
        isChecklistMode = isChecklistMode && cleanChecklist.isNotEmpty(),
        checklist = cleanChecklist,
        doodle = doodleData,
        images = imagePaths,
        backgroundPattern = backgroundPattern,
        richSpans = richSpans,
        lineHeadings = lineHeadings,
        expiresAt = expiresAt,
        codeBlocks = cleanCodeBlocks,
        versionHistory = versionHistory
      )
    )
  }

  fun persistNote(overrideHidden: Boolean = isHidden) {
    val cleanChecklist = checklistItems.filter { it.text.isNotBlank() }
    val cleanCodeBlocks = codeBlocks.filter { it.text.isNotBlank() }
    val hasContent = title.isNotBlank() || contentField.text.isNotBlank() || cleanChecklist.isNotEmpty() ||
      strokes.isNotEmpty() || imagePaths.isNotEmpty() || reminderTime > 0L || cleanCodeBlocks.isNotEmpty()
    if (hasContent) {
      viewModel.saveNote(
        id = noteId,
        title = title,
        content = contentField.text,
        color = color,
        isPinned = isPinned,
        isHidden = overrideHidden,
        labels = assignedLabels.joinToString(", "),
        checklistJson = buildExtrasJson(),
        reminderTime = reminderTime
      )
    }
  }

  BackHandler {
    when {
      viewingImageIndex != null -> viewingImageIndex = null
      showInlineBrushPopover -> showInlineBrushPopover = false
      activePanel != ActiveEditorPanel.NONE -> activePanel = ActiveEditorPanel.NONE
      showFindBar -> {
        showFindBar = false
        findQuery = ""
      }
      isResizeMode -> isResizeMode = false
      isInlineDrawActive -> {
        isInlineDrawActive = false
        showInlineBrushPopover = false
      }
      isFullPageDraw && (contentField.text.isNotBlank() || checklistItems.isNotEmpty() || imagePaths.isNotEmpty()) -> {
        isFullPageDraw = false
        hasInlineDrawBox = strokes.isNotEmpty()
      }
      else -> {
        persistNote()
        onBack()
      }
    }
  }

  // Full-Page Drawing Mode (uses the ultra-slim tap-to-reveal drawing toolbar)
  if (isFullPageDraw) {
    FullPageDrawingCanvas(
      strokes = strokes,
      selectedTool = selectedTool,
      selectedColorIndex = selectedColorIndex,
      toolStrokeSizes = toolStrokeSizes,
      onStrokeSizeChange = { tool, size ->
        toolStrokeSizes = toolStrokeSizes + (tool to size)
      },
      onAddStroke = { newStroke ->
        strokes = strokes + newStroke
        undoneStrokes = emptyList()
      },
      onSelectTool = { selectedTool = it },
      onSelectColor = { idx ->
        selectedColorIndex = idx
        if (selectedTool == EditorDrawTool.ERASER) selectedTool = EditorDrawTool.PEN
      },
      onUndo = {
        if (strokes.isNotEmpty()) {
          undoneStrokes = undoneStrokes + strokes.last()
          strokes = strokes.dropLast(1)
        }
      },
      onRedo = {
        if (undoneStrokes.isNotEmpty()) {
          strokes = strokes + undoneStrokes.last()
          undoneStrokes = undoneStrokes.dropLast(1)
        }
      },
      canUndo = strokes.isNotEmpty(),
      canRedo = undoneStrokes.isNotEmpty(),
      onBackAndDone = {
        persistNote()
        onBack()
      },
      onSwitchToTextNote = {
        isFullPageDraw = false
        hasInlineDrawBox = strokes.isNotEmpty()
      }
    )
    return
  }

  if (showReminderDialog) {
    ReminderPickerDialog(
      currentReminderTime = reminderTime,
      onDismiss = { showReminderDialog = false },
      onSetReminder = { ts ->
        reminderTime = ts
        showReminderDialog = false
      },
      onClearReminder = {
        reminderTime = 0L
        showReminderDialog = false
      }
    )
  }

  if (showLabelDialog) {
    LabelManagerDialog(
      allLabels = allLabels,
      assignedLabels = assignedLabels,
      onToggleLabel = { lbl ->
        assignedLabels = if (assignedLabels.any { it.equals(lbl, ignoreCase = true) }) {
          assignedLabels.filterNot { it.equals(lbl, ignoreCase = true) }.toSet()
        } else {
          assignedLabels + lbl
        }
      },
      onCreateAndAssignLabel = { newLbl ->
        viewModel.addGlobalLabel(newLbl)
        assignedLabels = assignedLabels + newLbl
      },
      onDismiss = { showLabelDialog = false }
    )
  }

  if (showVersionHistoryDialog) {
    VersionHistoryDialog(
      versions = versionHistory,
      onRestore = { snapshot ->
        pushTextUndoSnapshot(force = true)
        title = snapshot.title
        contentField = TextFieldValue(snapshot.content, selection = TextRange(snapshot.content.length))
        richSpans = snapshot.richSpans
        lineHeadings = snapshot.lineHeadings
        pendingInlineStyles = emptySet()
        textRedoStack = emptyList()
        showVersionHistoryDialog = false
        scope.launch { snackbarHostState.showSnackbar("Version restored.") }
      },
      onDismiss = { showVersionHistoryDialog = false }
    )
  }

  val screenBg = if (color == "plain" || color == "white") colors.bg else colors.noteCardColor(color)
  val wysiwygTransformation = remember(
    richSpans, lineHeadings, colors.highlight, colors.muted, colors.accent, findQuery, activeFindIndex, showFindBar
  ) {
    WysiwygVisualTransformation(
      spans = richSpans,
      lineHeadings = lineHeadings,
      highlightBg = colors.highlight,
      mutedColor = colors.muted,
      accentColor = colors.accent,
      findQuery = if (showFindBar) findQuery else "",
      activeMatchIndex = if (showFindBar && findMatches.isNotEmpty()) activeFindIndex else -1
    )
  }

  val readModeAnnotatedBody = remember(
    contentField.text, richSpans, lineHeadings, colors.highlight, colors.muted, colors.accent, findQuery, activeFindIndex, showFindBar, isReadOnlyMode
  ) {
    if (isReadOnlyMode) {
      buildWysiwygAnnotatedString(
        text = contentField.text,
        spans = richSpans,
        lineHeadings = lineHeadings,
        highlightBg = colors.highlight,
        mutedColor = colors.muted,
        accentColor = colors.accent,
        findQuery = if (showFindBar) findQuery else "",
        activeMatchIndex = if (showFindBar && findMatches.isNotEmpty()) activeFindIndex else -1,
        clickableLinks = true,
        onLinkClick = { url -> openDetectedUrl(url) }
      )
    } else {
      null
    }
  }

  val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val effectiveTopPadding = if (statusBarTop > 44.dp) statusBarTop + 12.dp else 56.dp
  val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  val density = LocalDensity.current
  val slideOffsetPx = with(density) { 24.dp.roundToPx() }
  val reminderLabel = remember(reminderTime) { formatReminderChip(reminderTime) }
  val scrollState = rememberScrollState()

  val orderedChecklist = remember(checklistItems, moveTickedToBottom) {
    if (moveTickedToBottom) {
      checklistItems.filter { !it.checked } + checklistItems.filter { it.checked }
    } else {
      checklistItems
    }
  }

  val showTextUndoRedo = !isReadOnlyMode && (isBodyFocused || textUndoStack.isNotEmpty() || textRedoStack.isNotEmpty())

  Scaffold(
    containerColor = screenBg,
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { _ ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(screenBg)
        .pointerInput(isResizeMode, activePanel, showInlineBrushPopover) {
          if (isResizeMode || activePanel != ActiveEditorPanel.NONE || showInlineBrushPopover) {
            detectTapGestures(onTap = {
              isResizeMode = false
              activePanel = ActiveEditorPanel.NONE
              showInlineBrushPopover = false
            })
          }
        }
    ) {
      // Subtle aesthetic background pattern overlay across the note canvas
      NoteBackgroundPatternOverlay(
        pattern = backgroundPattern,
        tint = colors.text,
        modifier = Modifier.fillMaxSize()
      )

      Column(modifier = Modifier.fillMaxSize()) {
        // Minimalist Top Bar: Back | [Subtle Undo/Redo when editing OR Read Mode pill] | Pin (1.5s stealth vault hold)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = effectiveTopPadding, bottom = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              persistNote()
              onBack()
            },
            modifier = Modifier
              .size(48.dp)
              .semantics { contentDescription = "Navigate back" }
              .testTag("editor_back_button")
          ) {
            VeilIcons.ArrowBack(tint = colors.text, size = 24.dp)
          }

          Spacer(modifier = Modifier.weight(1f))

          // Subtle Read-Mode indicator pill (tap to return to editing)
          AnimatedVisibility(
            visible = isReadOnlyMode,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(120))
          ) {
            Row(
              modifier = Modifier
                .padding(end = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.accentSoft)
                .tactilePress(scaleDown = 0.94f, hapticOnClick = true) {
                  isReadOnlyMode = false
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("read_mode_indicator_pill"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              VeilIcons.ReadBookOutline(tint = colors.accent, size = 15.dp)
              Text("Read mode", style = VeilTextStyles.labelChip, color = colors.accent)
            }
          }

          // Subtle Text Undo & Redo buttons while actively editing text
          AnimatedVisibility(
            visible = showTextUndoRedo,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(120))
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              modifier = Modifier.padding(end = 4.dp)
            ) {
              val canUndoText = textUndoStack.isNotEmpty()
              val canRedoText = textRedoStack.isNotEmpty()
              IconButton(
                onClick = { performTextUndo() },
                enabled = canUndoText,
                modifier = Modifier
                  .size(44.dp)
                  .semantics { contentDescription = "Undo text edit" }
                  .testTag("editor_text_undo_button")
              ) {
                VeilIcons.Undo(
                  tint = if (canUndoText) colors.text else colors.muted.copy(alpha = 0.35f),
                  size = 20.dp
                )
              }
              IconButton(
                onClick = { performTextRedo() },
                enabled = canRedoText,
                modifier = Modifier
                  .size(44.dp)
                  .semantics { contentDescription = "Redo text edit" }
                  .testTag("editor_text_redo_button")
              ) {
                VeilIcons.Redo(
                  tint = if (canRedoText) colors.text else colors.muted.copy(alpha = 0.35f),
                  size = 20.dp
                )
              }
            }
          }

          val latestHidden by rememberUpdatedState(isHidden)
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .pointerInput(noteId) {
                awaitEachGesture {
                  awaitFirstDown(requireUnconsumed = false)
                  var triggeredVault = false
                  val holdJob = scope.launch {
                    delay(1500L)
                    triggeredVault = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val nextHidden = !latestHidden
                    isHidden = nextHidden
                    persistNote(overrideHidden = nextHidden)
                    onBack()
                  }
                  val up = waitForUpOrCancellation()
                  holdJob.cancel()
                  if (!triggeredVault && up != null) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isPinned = !isPinned
                  }
                }
              }
              .semantics { contentDescription = if (isPinned) "Unpin note" else "Pin note" }
              .testTag("editor_pin_button"),
            contentAlignment = Alignment.Center
          ) {
            VeilIcons.Pin(
              tint = if (isPinned) colors.accent else colors.text,
              size = 24.dp
            )
          }
        }

        // Animated Find-in-Note search bar
        AnimatedVisibility(
          visible = showFindBar,
          enter = expandVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeIn(tween(180)),
          exit = shrinkVertically() + fadeOut(tween(150))
        ) {
          FindInNoteBar(
            query = findQuery,
            matchCount = findMatches.size,
            activeIndex = activeFindIndex,
            onQueryChange = {
              findQuery = it
              activeFindIndex = 0
            },
            onPrev = {
              if (findMatches.isNotEmpty()) {
                activeFindIndex = (activeFindIndex - 1 + findMatches.size) % findMatches.size
              }
            },
            onNext = {
              if (findMatches.isNotEmpty()) {
                activeFindIndex = (activeFindIndex + 1) % findMatches.size
              }
            },
            onClose = {
              showFindBar = false
              findQuery = ""
            }
          )
        }

        // Scrollable Note Content (scroll locks automatically while resizing or sketching inline!)
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(state = scrollState, enabled = !isInlineGestureLocked)
            .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
          // Responsive multi-image grid at the top of the note
          if (imagePaths.isNotEmpty()) {
            NoteImagesGrid(
              imagePaths = imagePaths,
              onImageClick = { idx ->
                activePanel = ActiveEditorPanel.NONE
                viewingImageIndex = idx
              }
            )
            Spacer(modifier = Modifier.height(16.dp))
          }

          if (isReadOnlyMode) {
            if (title.isNotBlank()) {
              Text(
                text = title,
                style = VeilThemeTokens.editorTitleStyle,
                color = colors.text,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("note_title_readonly")
              )
            }
          } else {
            BasicTextField(
              value = title,
              onValueChange = { title = it },
              textStyle = VeilThemeTokens.editorTitleStyle.copy(color = colors.text),
              cursorBrush = SolidColor(colors.accent),
              decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth()) {
                  if (title.isEmpty()) {
                    Text(
                      text = "Title",
                      style = VeilThemeTokens.editorTitleStyle,
                      color = colors.muted.copy(alpha = 0.55f)
                    )
                  }
                  innerTextField()
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("note_title_input")
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Active Self-Destruct Countdown, Reminder & Label chips
          val countdownBadge = remember(expiresAt, nowTick, proFeatures.selfDestructEnabled) {
            if (proFeatures.selfDestructEnabled && expiresAt > nowTick) {
              formatCountdownBadge(expiresAt, nowTick)
            } else {
              ""
            }
          }
          if (countdownBadge.isNotEmpty() || reminderLabel.isNotEmpty() || assignedLabels.isNotEmpty()) {
            LazyRow(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (countdownBadge.isNotEmpty()) {
                item {
                  Row(
                    modifier = Modifier
                      .clip(RoundedCornerShape(10.dp))
                      .background(colors.accentSoft)
                      .border(1.dp, colors.accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                      .tactilePress(scaleDown = 0.94f, hapticOnClick = true) {
                        activePanel = ActiveEditorPanel.TIMER
                      }
                      .padding(horizontal = 8.dp, vertical = 4.dp)
                      .testTag("editor_countdown_badge"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Text("⏱ $countdownBadge", style = VeilTextStyles.labelChip, color = colors.accent)
                  }
                }
              }
              if (reminderLabel.isNotEmpty()) {
                item {
                  Row(
                    modifier = Modifier
                      .clip(RoundedCornerShape(10.dp))
                      .background(colors.accentSoft)
                      .tactilePress(scaleDown = 0.94f, hapticOnClick = true) { showReminderDialog = true }
                      .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    VeilIcons.Bell(tint = colors.accent, size = 13.dp)
                    Text(reminderLabel, style = VeilTextStyles.labelChip, color = colors.accent)
                  }
                }
              }
              items(assignedLabels.take(4).toList()) { lbl ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.accentSoft)
                    .tactilePress(scaleDown = 0.94f, hapticOnClick = true) { showLabelDialog = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(lbl, style = VeilTextStyles.labelChip, color = colors.accent)
                }
              }
            }
          }

          // 60fps Resizable Inline Draw Box with Top-Right Delete Button & Popover Auto-Collapse
          if (hasInlineDrawBox) {
            ResizableInlineDrawBox(
              strokes = strokes,
              heightDp = inlineBoxHeightDp,
              widthFraction = inlineBoxWidthFrac,
              isDrawActive = isInlineDrawActive && !isReadOnlyMode,
              isResizeMode = isResizeMode && !isReadOnlyMode,
              selectedTool = selectedTool,
              selectedColorIndex = selectedColorIndex,
              strokeSizeDp = toolStrokeSizes[selectedTool] ?: selectedTool.defaultSize,
              onGestureLockChanged = { locked -> isInlineGestureLocked = locked },
              onDrawStart = { showInlineBrushPopover = false },
              onEnterDrawMode = {
                if (!isReadOnlyMode) {
                  activePanel = ActiveEditorPanel.NONE
                  isInlineDrawActive = true
                }
              },
              onEnterResizeMode = {
                if (!isReadOnlyMode) {
                  activePanel = ActiveEditorPanel.NONE
                  isInlineDrawActive = false
                  showInlineBrushPopover = false
                  isResizeMode = true
                }
              },
              onResize = { newH, newW ->
                inlineBoxHeightDp = newH
                inlineBoxWidthFrac = newW
              },
              onAddStroke = { newStroke ->
                strokes = strokes + newStroke
                undoneStrokes = emptyList()
              },
              onDeleteDrawBox = {
                strokes = emptyList()
                undoneStrokes = emptyList()
                hasInlineDrawBox = false
                isInlineDrawActive = false
                showInlineBrushPopover = false
                isResizeMode = false
                isInlineGestureLocked = false
              }
            )
            Spacer(modifier = Modifier.height(16.dp))
          }

          // Interactive Checklist Mode (non-editable in Read-Only Mode so keyboard never pops up)
          if (isChecklistMode) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              orderedChecklist.forEach { item ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .tactilePress(
                        scaleDown = 0.85f,
                        enabled = !isReadOnlyMode,
                        hapticOnClick = true,
                        role = Role.Checkbox
                      ) {
                        checklistItems = checklistItems.map { existing ->
                          if (existing.id == item.id) existing.copy(checked = !existing.checked) else existing
                        }
                      },
                    contentAlignment = Alignment.Center
                  ) {
                    VeilIcons.ChecklistCircle(
                      checked = item.checked,
                      tint = colors.muted,
                      accent = colors.accent
                    )
                  }

                  if (isReadOnlyMode) {
                    Text(
                      text = item.text,
                      style = VeilThemeTokens.editorBodyStyle.copy(
                        color = if (item.checked) colors.muted else colors.text,
                        textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                      ),
                      modifier = Modifier.weight(1f)
                    )
                  } else {
                    BasicTextField(
                      value = item.text,
                      onValueChange = { updated ->
                        checklistItems = checklistItems.map { existing ->
                          if (existing.id == item.id) existing.copy(text = updated) else existing
                        }
                      },
                      textStyle = VeilThemeTokens.editorBodyStyle.copy(
                        color = if (item.checked) colors.muted else colors.text,
                        textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                      ),
                      cursorBrush = SolidColor(colors.accent),
                      decorationBox = { inner ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                          if (item.text.isEmpty()) {
                            Text(
                              text = "List item",
                              style = VeilThemeTokens.editorBodyStyle,
                              color = colors.muted.copy(alpha = 0.5f)
                            )
                          }
                          inner()
                        }
                      },
                      modifier = Modifier
                        .weight(1f)
                        .testTag("checklist_item_input_${item.id}")
                    )

                    Box(
                      modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          checklistItems = checklistItems.filterNot { it.id == item.id }
                        },
                      contentAlignment = Alignment.Center
                    ) {
                      VeilIcons.Close(tint = colors.muted, size = 15.dp)
                    }
                  }
                }
              }

              if (!isReadOnlyMode) {
                Row(
                  modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .tactilePress(scaleDown = 0.94f, hapticOnClick = true) {
                      val nextId = (checklistItems.maxOfOrNull { it.id } ?: 0L) + 1L
                      checklistItems = checklistItems + ChecklistItemData(nextId, "", false)
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .testTag("add_checklist_item_button"),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  VeilIcons.Plus(tint = colors.accent, size = 18.dp)
                  Text("Add item", style = VeilTextStyles.cardBody, color = colors.accent)
                }
              }
            }
            Spacer(modifier = Modifier.height(16.dp))
          }

          // True WYSIWYG Rich Text Body (ZERO Markdown symbols; clickable links inline in Read Mode)
          if (!isChecklistMode || contentField.text.isNotBlank()) {
            if (isReadOnlyMode) {
              val bodyToShow = readModeAnnotatedBody
              if (bodyToShow != null && contentField.text.isNotEmpty()) {
                Text(
                  text = bodyToShow,
                  style = VeilThemeTokens.editorBodyStyle.copy(color = colors.text),
                  modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = if (codeBlocks.isEmpty()) 220.dp else 80.dp)
                    .testTag("note_content_readonly")
                )
              } else if (codeBlocks.isEmpty()) {
                Text(
                  text = "Empty note",
                  style = VeilThemeTokens.editorBodyStyle,
                  color = colors.muted.copy(alpha = 0.55f)
                )
              }
            } else {
              BasicTextField(
                value = contentField,
                onValueChange = { handleBodyChange(it) },
                textStyle = VeilThemeTokens.editorBodyStyle.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                visualTransformation = wysiwygTransformation,
                decorationBox = { innerTextField ->
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .defaultMinSize(minHeight = if (codeBlocks.isEmpty()) 220.dp else 96.dp)
                  ) {
                    if (contentField.text.isEmpty()) {
                      Text(
                        text = "Write a note...",
                        style = VeilThemeTokens.editorBodyStyle,
                        color = colors.muted.copy(alpha = 0.55f)
                      )
                    }
                    innerTextField()
                  }
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .onFocusChanged { isBodyFocused = it.isFocused }
                  .testTag("note_content_input")
              )
            }
          }

          // Copyable Prompt / Code Blocks inside note
          if (codeBlocks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              codeBlocks.forEach { block ->
                CopyableCodeBlockCard(
                  block = block,
                  isReadOnlyMode = isReadOnlyMode,
                  onTextChange = { updatedText ->
                    codeBlocks = codeBlocks.map { existing ->
                      if (existing.id == block.id) existing.copy(text = updatedText) else existing
                    }
                  },
                  onDelete = {
                    codeBlocks = codeBlocks.filterNot { it.id == block.id }
                  }
                )
              }
            }
          }

          // Auto-Detected Clickable Link Chips (gives instant one-tap link opening in Edit Mode too)
          if (detectedUrls.isNotEmpty() && !isReadOnlyMode) {
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("detected_links_row"),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              items(detectedUrls.distinctBy { it.launchUrl }) { link ->
                Row(
                  modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accentSoft)
                    .border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .tactilePress(scaleDown = 0.94f, hapticOnClick = true) {
                      openDetectedUrl(link.launchUrl)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  VeilIcons.LinkOutline(tint = colors.accent, size = 14.dp)
                  Text(
                    text = link.displayText,
                    style = VeilTextStyles.labelChip,
                    color = colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 220.dp)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(180.dp + navBottom))
        }
      }

      // Clean 4-Button Google Keep-style Bottom Bar + Popups + WYSIWYG Rich Text Bar
      AnimatedVisibility(
        visible = !isInlineDrawActive,
        enter = fadeIn(tween(if (reduceMotion) 0 else 200, easing = VeilEasing)),
        exit = fadeOut(tween(if (reduceMotion) 0 else 150)),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .imePadding()
          .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // "+" Insert Tools Glassmorphism Panel
          AnimatedVisibility(
            visible = activePanel == ActiveEditorPanel.INSERT && !isReadOnlyMode,
            enter = fadeIn(tween(if (reduceMotion) 0 else 180)) + slideInVertically(
              spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
            ) { 28 },
            exit = fadeOut(tween(if (reduceMotion) 0 else 140)) + slideOutVertically { 20 }
          ) {
            InsertToolsPanel(
              showCodeBlockOption = proFeatures.codeBlocksEnabled,
              onTakePhoto = {
                activePanel = ActiveEditorPanel.NONE
                requestCameraCapture()
              },
              onAddImage = {
                activePanel = ActiveEditorPanel.NONE
                pickImagesLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              onFullPageDrawing = {
                activePanel = ActiveEditorPanel.NONE
                isFullPageDraw = true
              },
              onInlineDrawBox = {
                activePanel = ActiveEditorPanel.NONE
                hasInlineDrawBox = true
                isInlineDrawActive = true
                showInlineBrushPopover = false
                isResizeMode = false
              },
              onChecklist = {
                activePanel = ActiveEditorPanel.NONE
                isChecklistMode = !isChecklistMode
                if (isChecklistMode && checklistItems.isEmpty()) {
                  checklistItems = listOf(ChecklistItemData(1L, "", false))
                }
              },
              onVoiceNote = {
                activePanel = ActiveEditorPanel.NONE
                requestVoiceInput()
              },
              onInsertCodeBlock = {
                activePanel = ActiveEditorPanel.NONE
                val nextId = (codeBlocks.maxOfOrNull { it.id } ?: 0L) + 1L
                codeBlocks = codeBlocks + CodeBlockData(nextId, "")
              }
            )
          }

          // Self-Destruct Quick-Select Popup (1-click Off / 1h / 24h / 7d)
          AnimatedVisibility(
            visible = activePanel == ActiveEditorPanel.TIMER && proFeatures.selfDestructEnabled,
            enter = fadeIn(tween(if (reduceMotion) 0 else 180)) + slideInVertically(
              spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
            ) { 28 },
            exit = fadeOut(tween(if (reduceMotion) 0 else 140)) + slideOutVertically { 20 }
          ) {
            SelfDestructQuickPopup(
              currentExpiresAt = expiresAt,
              onSelectDurationMs = { durationMs ->
                expiresAt = if (durationMs <= 0L) 0L else System.currentTimeMillis() + durationMs
                nowTick = System.currentTimeMillis()
                activePanel = ActiveEditorPanel.NONE
              }
            )
          }

          // Two-Row Color & Background Pattern Picker Panel
          AnimatedVisibility(
            visible = activePanel == ActiveEditorPanel.PALETTE,
            enter = fadeIn(tween(if (reduceMotion) 0 else 180)) + slideInVertically(
              spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
            ) { 28 },
            exit = fadeOut(tween(if (reduceMotion) 0 else 140)) + slideOutVertically { 20 }
          ) {
            val panelShape = RoundedCornerShape(24.dp)
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .shadow(14.dp, panelShape, ambientColor = colors.toolbarShadow, spotColor = colors.toolbarShadow)
                .clip(panelShape)
                .background(colors.surface.copy(alpha = 0.96f))
                .border(1.dp, colors.hiddenBorder.copy(alpha = 0.45f), panelShape)
            ) {
              NoteColorAndPatternPickerContent(
                selectedColor = color,
                selectedPattern = backgroundPattern,
                onSelectColor = { color = it },
                onSelectPattern = { backgroundPattern = it }
              )
            }
          }

          // Three-Dot Overflow Menu Panel (includes Read Mode + Version History when enabled)
          AnimatedVisibility(
            visible = activePanel == ActiveEditorPanel.OVERFLOW,
            enter = fadeIn(tween(if (reduceMotion) 0 else 180)) + slideInVertically(
              spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
            ) { 28 },
            exit = fadeOut(tween(if (reduceMotion) 0 else 140)) + slideOutVertically { 20 }
          ) {
            NoteOverflowMenuPanel(
              isReadOnlyMode = isReadOnlyMode,
              hasReminder = reminderTime > 0L,
              hasLabels = assignedLabels.isNotEmpty(),
              enableSharing = enableSharing,
              showVersionHistory = proFeatures.versionHistoryEnabled,
              versionCount = versionHistory.size,
              onToggleReadMode = {
                activePanel = ActiveEditorPanel.NONE
                val nextRead = !isReadOnlyMode
                isReadOnlyMode = nextRead
                if (nextRead) {
                  focusManager.clearFocus()
                  showFormatBar = false
                  isInlineDrawActive = false
                  isResizeMode = false
                  showInlineBrushPopover = false
                }
              },
              onVersionHistory = {
                activePanel = ActiveEditorPanel.NONE
                showVersionHistoryDialog = true
              },
              onReminder = {
                activePanel = ActiveEditorPanel.NONE
                showReminderDialog = true
              },
              onLabels = {
                activePanel = ActiveEditorPanel.NONE
                showLabelDialog = true
              },
              onFindInNote = {
                activePanel = ActiveEditorPanel.NONE
                showFindBar = true
              },
              onMakeCopy = {
                activePanel = ActiveEditorPanel.NONE
                viewModel.duplicateDraftNote(
                  title = title,
                  content = contentField.text,
                  color = color,
                  isPinned = isPinned,
                  isHidden = isHidden,
                  labels = assignedLabels.joinToString(", "),
                  checklistJson = buildExtrasJson(),
                  reminderTime = reminderTime
                )
                scope.launch { snackbarHostState.showSnackbar("Copied note.") }
              },
              onCopyAllText = {
                activePanel = ActiveEditorPanel.NONE
                val fullText = NoteImageHelper.formatNotePlainText(title, contentField.text, checklistItems)
                NoteImageHelper.copyTextToClipboard(context, title.ifBlank { "Note" }, fullText)
                scope.launch { snackbarHostState.showSnackbar("All text copied to clipboard.") }
              },
              onSendShare = {
                activePanel = ActiveEditorPanel.NONE
                val fullText = NoteImageHelper.formatNotePlainText(title, contentField.text, checklistItems)
                NoteImageHelper.shareNote(context, title, fullText, imagePaths)
              },
              onArchive = {
                activePanel = ActiveEditorPanel.NONE
                note?.let { viewModel.archiveNote(it) } ?: persistNote()
                onBack()
              },
              onDelete = {
                activePanel = ActiveEditorPanel.NONE
                note?.let { viewModel.trashNote(it) }
                onBack()
              }
            )
          }

          // "Aa" True WYSIWYG Rich Text Formatting Bar right above the bottom bar / keyboard
          AnimatedVisibility(
            visible = showFormatBar && activePanel == ActiveEditorPanel.NONE && !isReadOnlyMode,
            enter = fadeIn(tween(if (reduceMotion) 0 else 160)) + slideInVertically(
              spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
            ) { 18 },
            exit = fadeOut(tween(if (reduceMotion) 0 else 120)) + slideOutVertically { 14 }
          ) {
            RichTextFormattingBar(
              activeHeadingLevel = activeHeadingLevel,
              activeInlineStyles = activeInlineStyles,
              onHeading1 = { applyHeadingLevel(1) },
              onHeading2 = { applyHeadingLevel(2) },
              onNormalText = { applyHeadingLevel(0) },
              onBold = { toggleInlineRichStyle(RichStyle.BOLD) },
              onItalic = { toggleInlineRichStyle(RichStyle.ITALIC) },
              onUnderline = { toggleInlineRichStyle(RichStyle.UNDERLINE) },
              onStrikethrough = { toggleInlineRichStyle(RichStyle.STRIKE) },
              onHighlight = { toggleInlineRichStyle(RichStyle.HIGHLIGHT) }
            )
          }

          // Sleek 4-Button Bottom Bar (+, [optional Timer], Palette, Aa | ⋮)
          NoteEditorBottomBar(
            isInsertOpen = activePanel == ActiveEditorPanel.INSERT,
            isPaletteOpen = activePanel == ActiveEditorPanel.PALETTE,
            isFormatBarOpen = showFormatBar,
            isOverflowOpen = activePanel == ActiveEditorPanel.OVERFLOW,
            isReadOnlyMode = isReadOnlyMode,
            showTimerButton = proFeatures.selfDestructEnabled,
            isTimerOpen = activePanel == ActiveEditorPanel.TIMER,
            hasActiveTimer = expiresAt > nowTick,
            onToggleInsert = {
              activePanel = if (activePanel == ActiveEditorPanel.INSERT) ActiveEditorPanel.NONE else ActiveEditorPanel.INSERT
            },
            onToggleTimer = {
              activePanel = if (activePanel == ActiveEditorPanel.TIMER) ActiveEditorPanel.NONE else ActiveEditorPanel.TIMER
            },
            onTogglePalette = {
              activePanel = if (activePanel == ActiveEditorPanel.PALETTE) ActiveEditorPanel.NONE else ActiveEditorPanel.PALETTE
            },
            onToggleFormatBar = {
              activePanel = ActiveEditorPanel.NONE
              showFormatBar = !showFormatBar
            },
            onToggleOverflow = {
              activePanel = if (activePanel == ActiveEditorPanel.OVERFLOW) ActiveEditorPanel.NONE else ActiveEditorPanel.OVERFLOW
            }
          )
        }
      }

      // Ultra-Slim Floating Drawing Toolbar for Inline Draw Box (tap active brush to reveal size/colors)
      AnimatedVisibility(
        visible = isInlineDrawActive && !isReadOnlyMode,
        enter = slideInVertically(
          animationSpec = tween(durationMillis = if (reduceMotion) 0 else 250, easing = VeilEasing),
          initialOffsetY = { slideOffsetPx }
        ) + fadeIn(tween(if (reduceMotion) 0 else 250, easing = VeilEasing)),
        exit = slideOutVertically(
          animationSpec = tween(if (reduceMotion) 0 else 200, easing = VeilEasing),
          targetOffsetY = { slideOffsetPx }
        ) + fadeOut(tween(if (reduceMotion) 0 else 200)),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
      ) {
        FloatingDrawingToolbar(
          strokePalette = strokePalette,
          selectedColorIndex = selectedColorIndex,
          selectedTool = selectedTool,
          strokeSizeDp = toolStrokeSizes[selectedTool] ?: selectedTool.defaultSize,
          isPopoverExpanded = showInlineBrushPopover,
          onTogglePopover = { showInlineBrushPopover = it },
          onStrokeSizeChange = { newSize ->
            toolStrokeSizes = toolStrokeSizes + (selectedTool to newSize)
          },
          canUndo = strokes.isNotEmpty(),
          canRedo = undoneStrokes.isNotEmpty(),
          onSelectColor = { idx ->
            selectedColorIndex = idx
            if (selectedTool == EditorDrawTool.ERASER) selectedTool = EditorDrawTool.PEN
          },
          onSelectTool = { selectedTool = it },
          onUndo = {
            if (strokes.isNotEmpty()) {
              undoneStrokes = undoneStrokes + strokes.last()
              strokes = strokes.dropLast(1)
            }
          },
          onRedo = {
            if (undoneStrokes.isNotEmpty()) {
              strokes = strokes + undoneStrokes.last()
              undoneStrokes = undoneStrokes.dropLast(1)
            }
          },
          onClose = {
            isInlineDrawActive = false
            showInlineBrushPopover = false
          }
        )
      }

      // Full-Screen Image Viewer with Offline OCR ("Grab Image Text"), Copy Image, Share, Delete
      viewingImageIndex?.let { idx ->
        FullScreenImageViewer(
          imagePaths = imagePaths,
          initialIndex = idx,
          enableSharing = enableSharing,
          isRunningOcr = isRunningOcr,
          onDismiss = { viewingImageIndex = null },
          onGrabImageText = { imgPath ->
            if (!isRunningOcr) {
              isRunningOcr = true
              scope.launch {
                val res = NoteImageHelper.extractTextFromImage(context, imgPath)
                isRunningOcr = false
                res.onSuccess { extracted ->
                  if (extracted.isNotBlank()) {
                    pushTextUndoSnapshot(force = true)
                    val cur = contentField.text
                    val appended = if (cur.isBlank()) extracted else "$cur\n\n$extracted"
                    contentField = TextFieldValue(appended, selection = TextRange(appended.length))
                    textRedoStack = emptyList()
                    viewingImageIndex = null
                    snackbarHostState.showSnackbar("Image text grabbed and added to note.")
                  } else {
                    snackbarHostState.showSnackbar("No readable text found in image.")
                  }
                }.onFailure {
                  snackbarHostState.showSnackbar("Could not extract text from image.")
                }
              }
            }
          },
          onCopyImage = { imgPath ->
            val ok = NoteImageHelper.copyImageToClipboard(context, imgPath)
            scope.launch {
              snackbarHostState.showSnackbar(
                if (ok) "Image copied to clipboard." else "Could not copy image."
              )
            }
          },
          onShareImage = { imgPath ->
            NoteImageHelper.shareImage(context, imgPath)
          },
          onDeleteImage = { imgPath ->
            imagePaths = imagePaths.filterNot { it == imgPath }
            NoteImageHelper.deleteImageFile(imgPath)
            if (imagePaths.isEmpty()) {
              viewingImageIndex = null
            } else {
              viewingImageIndex = idx.coerceAtMost(imagePaths.lastIndex)
            }
            scope.launch { snackbarHostState.showSnackbar("Image deleted.") }
          }
        )
      }
    }
  }
}
