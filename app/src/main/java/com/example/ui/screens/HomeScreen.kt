package com.example.ui.screens

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.DoodleStrokeData
import com.example.data.NoteEntity
import com.example.data.NoteExtrasCodec
import com.example.data.NoteImageHelper
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.RemoteConfigValidator
import com.example.ui.components.ArchiveUndoToast
import com.example.ui.components.ClipboardQuickSavePill
import com.example.ui.components.GitHubStarNudgeCard
import com.example.ui.components.HomeAnnouncementBanner
import com.example.ui.components.HomeSelectionTopBar
import com.example.ui.components.LabelManagerDialog
import com.example.ui.components.NoteBackgroundPatternOverlay
import com.example.ui.components.NoteColorAndPatternPickerContent
import com.example.ui.components.NoteSmartCategory
import com.example.ui.components.ReminderPickerDialog
import com.example.ui.components.RichStyle
import com.example.ui.components.SmartCategoryFilterRow
import com.example.ui.components.SortOrderMenuButton
import com.example.ui.components.VeilIcons
import com.example.ui.components.buildWysiwygAnnotatedString
import com.example.ui.components.drawNormalizedStrokes
import com.example.ui.components.extractDoodleCount
import com.example.ui.components.formatCountdownBadge
import com.example.ui.components.formatReminderChip
import com.example.ui.components.isReminderPast
import com.example.ui.components.matchesSmartCategory
import com.example.ui.components.migrateLegacyMarkdownIfNeeded
import com.example.ui.components.openSafeExternalUrl
import com.example.ui.components.stripDoodleMarkers
import com.example.ui.components.tactilePress
import com.example.ui.theme.VeilEasing
import com.example.ui.theme.VeilTextStyles
import com.example.ui.theme.VeilThemeTokens
import com.example.viewmodel.VeilViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private data class ToastState(
  val key: Long = 0L,
  val message: String = "",
  val archivedNotes: List<NoteEntity> = emptyList(),
  val showUndo: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  viewModel: VeilViewModel,
  isDarkTheme: Boolean,
  onToggleTheme: (Offset) -> Unit,
  onNoteClick: (Long) -> Unit,
  onCreateNoteWithMode: (String) -> Unit,
  onOpenSettings: () -> Unit,
  onOpenArchive: () -> Unit,
  onOpenTrash: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  val reduceMotion = VeilThemeTokens.reduceMotion
  val focusManager = LocalFocusManager.current
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  val activeNotes by viewModel.activeNotes.collectAsState()
  val reminderNotes by viewModel.reminderNotes.collectAsState()
  val hiddenNotes by viewModel.hiddenNotes.collectAsState()
  val areHiddenUnlocked by viewModel.areHiddenNotesRevealed.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val isGridView by viewModel.isGridView.collectAsState()
  val selectedIds by viewModel.selectedNoteIds.collectAsState()
  val currentDrawerScreen by viewModel.currentDrawerScreen.collectAsState()
  val selectedLabelFilter by viewModel.selectedLabelFilter.collectAsState()
  val allLabels by viewModel.allLabels.collectAsState()
  val sortOrder by viewModel.sortOrder.collectAsState()
  val customNoteOrder by viewModel.customNoteOrder.collectAsState()
  val enableSharing by viewModel.enableSharing.collectAsState()
  val moveTickedToBottom by viewModel.moveTickedToBottom.collectAsState()
  val proFeatures by viewModel.proFeatures.collectAsState()
  val remoteConfig by viewModel.remoteConfig.collectAsState()
  val dismissedAnnouncementHash by viewModel.dismissedAnnouncementHash.collectAsState()
  val totalNotesCreated by viewModel.totalNotesCreated.collectAsState()
  val starNudgeDismissed by viewModel.starNudgeDismissed.collectAsState()

  var themeButtonCenter by remember { mutableStateOf(Offset.Unspecified) }

  // Strictly clear any stale multi-select state whenever Home is entered/composed or Back is pressed while selecting
  LaunchedEffect(Unit) {
    viewModel.clearSelection()
  }
  BackHandler(enabled = selectedIds.isNotEmpty()) {
    viewModel.clearSelection()
  }

  // Smart Auto-Categories filter state (reset when feature is toggled OFF)
  var selectedCategory by remember { mutableStateOf(NoteSmartCategory.ALL) }
  LaunchedEffect(proFeatures.autoCategoriesEnabled) {
    if (!proFeatures.autoCategoriesEnabled) {
      selectedCategory = NoteSmartCategory.ALL
    }
  }

  // Clipboard Quick-Save Pill state (5-second auto-dismiss, silent fail-safe check on resume)
  var clipboardOfferText by remember { mutableStateOf<String?>(null) }
  var clipboardDismissJob by remember { mutableStateOf<Job?>(null) }
  val lifecycleOwner = LocalLifecycleOwner.current
  val latestClipboardEnabled by rememberUpdatedState(proFeatures.clipboardPillEnabled)

  fun triggerClipboardCheck() {
    if (!latestClipboardEnabled) return
    scope.launch {
      // Brief post-focus delay required on Android 10+ so window focus settles before reading clipboard
      delay(280L)
      if (!latestClipboardEnabled) return@launch
      val clipText = runCatching {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.primaryClip?.takeIf { it.itemCount > 0 }
          ?.getItemAt(0)
          ?.coerceToText(context)
          ?.toString()
          ?.trim()
      }.getOrNull()
      if (!clipText.isNullOrBlank() && viewModel.shouldOfferClipboardText(clipText)) {
        clipboardDismissJob?.cancel()
        clipboardOfferText = clipText
        clipboardDismissJob = scope.launch {
          delay(5000L)
          if (clipboardOfferText == clipText) {
            clipboardOfferText = null
          }
        }
      }
    }
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        triggerClipboardCheck()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(proFeatures.clipboardPillEnabled) {
    if (proFeatures.clipboardPillEnabled) {
      triggerClipboardCheck()
    } else {
      clipboardDismissJob?.cancel()
      clipboardOfferText = null
    }
  }

  // Selection action dialogs
  var showSelectionReminderDialog by remember { mutableStateOf(false) }
  var showSelectionColorDialog by remember { mutableStateOf(false) }
  var showSelectionLabelDialog by remember { mutableStateOf(false) }

  // 5-second Archive Undo Toast state
  var toastState by remember { mutableStateOf<ToastState?>(null) }
  var toastDismissJob by remember { mutableStateOf<Job?>(null) }

  fun showTransientToast(
    message: String,
    archived: List<NoteEntity> = emptyList(),
    durationMs: Long = if (archived.isNotEmpty()) 5000L else 2600L
  ) {
    // Never allow Clipboard pill and toast to overlap on screen
    clipboardDismissJob?.cancel()
    clipboardOfferText = null
    toastDismissJob?.cancel()
    val key = System.currentTimeMillis()
    toastState = ToastState(
      key = key,
      message = message,
      archivedNotes = archived,
      showUndo = archived.isNotEmpty()
    )
    toastDismissJob = scope.launch {
      delay(durationMs)
      if (toastState?.key == key) {
        toastState = null
      }
    }
  }

  var pendingBulkReminder by remember { mutableStateOf<Pair<List<NoteEntity>, Long>?>(null) }
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { _ ->
    val pending = pendingBulkReminder
    pendingBulkReminder = null
    if (pending != null) {
      viewModel.updateNotesReminder(pending.first, pending.second)
      viewModel.clearSelection()
      showTransientToast("Reminder set")
    }
  }

  val baseNotes = remember(activeNotes, reminderNotes, currentDrawerScreen, selectedLabelFilter) {
    when {
      currentDrawerScreen == "reminders" -> reminderNotes
      selectedLabelFilter != null -> activeNotes.filter { note ->
        note.labels.split(",").map { it.trim() }.any { it.equals(selectedLabelFilter, ignoreCase = true) }
      }
      currentDrawerScreen == "labels" -> activeNotes.filter { it.labels.isNotBlank() }
      else -> activeNotes
    }
  }

  // Offline Smart Auto-Category counts: NEVER include hidden Vault notes unless actively unlocked
  val smartCategoryCounts = remember(baseNotes, hiddenNotes, areHiddenUnlocked, proFeatures.autoCategoriesEnabled) {
    if (!proFeatures.autoCategoriesEnabled) {
      emptyMap()
    } else {
      val pool = if (areHiddenUnlocked) baseNotes + hiddenNotes else baseNotes
      val decoded = pool.map { it to NoteExtrasCodec.decode(it.checklistJson) }
      NoteSmartCategory.entries.associateWith { cat ->
        decoded.count { (n, ex) -> matchesSmartCategory(n, ex, cat) }
      }
    }
  }

  val filteredHiddenNotes = remember(hiddenNotes, areHiddenUnlocked, selectedCategory, proFeatures.autoCategoriesEnabled) {
    if (!areHiddenUnlocked) {
      emptyList()
    } else if (proFeatures.autoCategoriesEnabled && selectedCategory != NoteSmartCategory.ALL) {
      hiddenNotes.filter { n ->
        matchesSmartCategory(n, NoteExtrasCodec.decode(n.checklistJson), selectedCategory)
      }
    } else {
      hiddenNotes
    }
  }

  val filteredActive = remember(baseNotes, searchQuery, areHiddenUnlocked, selectedCategory, proFeatures.autoCategoriesEnabled) {
    val searchFiltered = if (searchQuery.isNotEmpty() && !areHiddenUnlocked) {
      baseNotes.filter { note ->
        val extras = NoteExtrasCodec.decode(note.checklistJson)
        val checklistMatch = extras.checklist.any { it.text.contains(searchQuery, ignoreCase = true) }
        val codeMatch = extras.codeBlocks.any { it.text.contains(searchQuery, ignoreCase = true) }
        note.title.contains(searchQuery, ignoreCase = true) ||
          note.content.contains(searchQuery, ignoreCase = true) ||
          note.labels.contains(searchQuery, ignoreCase = true) ||
          checklistMatch ||
          codeMatch
      }
    } else {
      baseNotes
    }
    if (proFeatures.autoCategoriesEnabled && selectedCategory != NoteSmartCategory.ALL) {
      searchFiltered.filter { n ->
        matchesSmartCategory(n, NoteExtrasCodec.decode(n.checklistJson), selectedCategory)
      }
    } else {
      searchFiltered
    }
  }

  fun sortNotesList(notes: List<NoteEntity>): List<NoteEntity> {
    return when (sortOrder) {
      "created_desc" -> notes.sortedByDescending { it.createdAt }
      "created_asc" -> notes.sortedBy { it.createdAt }
      "modified_desc" -> notes.sortedByDescending { it.updatedAt }
      "modified_asc" -> notes.sortedBy { it.updatedAt }
      else -> {
        if (customNoteOrder.isEmpty()) {
          notes.sortedByDescending { it.updatedAt }
        } else {
          val orderMap = customNoteOrder.withIndex().associate { (i, id) -> id to i }
          notes.sortedWith(
            compareBy<NoteEntity> { orderMap[it.id] ?: Int.MAX_VALUE }
              .thenByDescending { it.updatedAt }
          )
        }
      }
    }
  }

  val sortedPinnedBase = remember(filteredActive, sortOrder, customNoteOrder) {
    sortNotesList(filteredActive.filter { it.isPinned })
  }
  val sortedOtherBase = remember(filteredActive, sortOrder, customNoteOrder) {
    sortNotesList(filteredActive.filter { !it.isPinned })
  }

  // Live drag-and-drop reordering state
  var draggingNoteId by remember { mutableStateOf<Long?>(null) }
  var draggingIsPinned by remember { mutableStateOf(false) }
  var dragOffset by remember { mutableStateOf(Offset.Zero) }
  var longPressStartedSelection by remember { mutableStateOf(false) }
  var livePinnedIds by remember { mutableStateOf<List<Long>>(emptyList()) }
  var liveOtherIds by remember { mutableStateOf<List<Long>>(emptyList()) }
  val itemBounds = remember { mutableStateMapOf<Long, Rect>() }

  LaunchedEffect(sortedPinnedBase, draggingNoteId) {
    if (draggingNoteId == null) {
      livePinnedIds = sortedPinnedBase.map { it.id }
    }
  }
  LaunchedEffect(sortedOtherBase, draggingNoteId) {
    if (draggingNoteId == null) {
      liveOtherIds = sortedOtherBase.map { it.id }
    }
  }

  val pinnedNotes = remember(sortedPinnedBase, livePinnedIds, draggingNoteId) {
    if (draggingNoteId != null && livePinnedIds.isNotEmpty()) {
      val map = sortedPinnedBase.associateBy { it.id }
      livePinnedIds.mapNotNull { map[it] } + sortedPinnedBase.filter { it.id !in livePinnedIds }
    } else {
      sortedPinnedBase
    }
  }

  val otherNotes = remember(sortedOtherBase, liveOtherIds, draggingNoteId) {
    if (draggingNoteId != null && liveOtherIds.isNotEmpty()) {
      val map = sortedOtherBase.associateBy { it.id }
      liveOtherIds.mapNotNull { map[it] } + sortedOtherBase.filter { it.id !in liveOtherIds }
    } else {
      sortedOtherBase
    }
  }

  // All selectable notes currently visible
  val allVisibleNotes = remember(pinnedNotes, otherNotes, filteredHiddenNotes, areHiddenUnlocked) {
    val hiddenList = if (areHiddenUnlocked) filteredHiddenNotes else emptyList()
    hiddenList + pinnedNotes + otherNotes
  }
  val selectedNotes = remember(allVisibleNotes, selectedIds) {
    allVisibleNotes.filter { selectedIds.contains(it.id) }
  }

  // Selection dialogs
  if (showSelectionReminderDialog && selectedNotes.isNotEmpty()) {
    val firstRem = selectedNotes.firstOrNull { it.reminderTime > 0L }?.reminderTime ?: 0L
    ReminderPickerDialog(
      currentReminderTime = firstRem,
      onDismiss = { showSelectionReminderDialog = false },
      onSetReminder = { ts ->
        showSelectionReminderDialog = false
        val targets = selectedNotes.toList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
          ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
          pendingBulkReminder = targets to ts
          notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
          viewModel.updateNotesReminder(targets, ts)
          viewModel.clearSelection()
          showTransientToast("Reminder set")
        }
      },
      onClearReminder = {
        viewModel.updateNotesReminder(selectedNotes, 0L)
        showSelectionReminderDialog = false
        viewModel.clearSelection()
      }
    )
  }

  if (showSelectionColorDialog && selectedNotes.isNotEmpty()) {
    val first = selectedNotes.first()
    val firstExtras = remember(first.checklistJson) { NoteExtrasCodec.decode(first.checklistJson) }
    var pickedColor by remember(first.id) { mutableStateOf(first.color) }
    var pickedPattern by remember(first.id) { mutableStateOf(firstExtras.backgroundPattern) }

    AlertDialog(
      onDismissRequest = { showSelectionColorDialog = false },
      containerColor = colors.surface,
      titleContentColor = colors.text,
      shape = RoundedCornerShape(24.dp),
      title = { Text("Color & Background", style = VeilTextStyles.cardTitle) },
      text = {
        NoteColorAndPatternPickerContent(
          selectedColor = pickedColor,
          selectedPattern = pickedPattern,
          onSelectColor = { c ->
            pickedColor = c
            viewModel.updateNotesStyle(selectedNotes, color = c, pattern = null)
          },
          onSelectPattern = { p ->
            pickedPattern = p
            viewModel.updateNotesStyle(selectedNotes, color = null, pattern = p)
          }
        )
      },
      confirmButton = {
        TextButton(onClick = { showSelectionColorDialog = false }) {
          Text("Done", style = VeilTextStyles.cardTitle, color = colors.accent)
        }
      }
    )
  }

  if (showSelectionLabelDialog && selectedNotes.isNotEmpty()) {
    val commonLabels = remember(selectedNotes) {
      selectedNotes
        .flatMap { it.labels.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() } }
        .toSet()
    }
    LabelManagerDialog(
      allLabels = allLabels,
      assignedLabels = commonLabels,
      onToggleLabel = { lbl ->
        viewModel.toggleLabelForNotes(selectedNotes, lbl)
      },
      onCreateAndAssignLabel = { newLbl ->
        viewModel.toggleLabelForNotes(selectedNotes, newLbl)
      },
      onDismiss = { showSelectionLabelDialog = false }
    )
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet(
        drawerContainerColor = colors.surface,
        drawerContentColor = colors.text,
        modifier = Modifier.widthIn(max = 300.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
        ) {
          Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
          Spacer(modifier = Modifier.height(24.dp))
          Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            VeilIcons.NoteDoc(tint = colors.accent, size = 24.dp)
            Text(
              "Notes",
              style = VeilTextStyles.editorTitle.copy(fontSize = 22.sp),
              color = colors.text
            )
          }
          Spacer(modifier = Modifier.height(20.dp))
          HorizontalDivider(color = colors.accentSoft, thickness = 1.dp)
          Spacer(modifier = Modifier.height(8.dp))

          DrawerRowItem(
            label = "Notes",
            selected = currentDrawerScreen == "notes" && selectedLabelFilter == null,
            icon = { tint -> VeilIcons.NoteDoc(tint = tint, size = 22.dp) },
            onClick = {
              viewModel.setDrawerScreen("notes", null)
              scope.launch { drawerState.close() }
            }
          )
          DrawerRowItem(
            label = "Reminders",
            selected = currentDrawerScreen == "reminders",
            icon = { tint -> VeilIcons.Bell(tint = tint, size = 22.dp) },
            onClick = {
              viewModel.setDrawerScreen("reminders", null)
              scope.launch { drawerState.close() }
            }
          )

          HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
            color = colors.accentSoft,
            thickness = 1.dp
          )

          Text(
            text = "LABELS",
            style = VeilTextStyles.sectionLabel,
            color = colors.muted,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
          )

          if (allLabels.isEmpty()) {
            DrawerRowItem(
              label = "Labels",
              selected = currentDrawerScreen == "labels" && selectedLabelFilter == null,
              icon = { tint -> VeilIcons.Tag(tint = tint, size = 22.dp) },
              onClick = {
                viewModel.setDrawerScreen("labels", null)
                scope.launch { drawerState.close() }
              }
            )
          } else {
            allLabels.forEach { label ->
              DrawerRowItem(
                label = label,
                selected = selectedLabelFilter == label,
                icon = { tint -> VeilIcons.Tag(tint = tint, size = 20.dp) },
                onClick = {
                  viewModel.setDrawerScreen("labels", label)
                  scope.launch { drawerState.close() }
                }
              )
            }
          }

          HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
            color = colors.accentSoft,
            thickness = 1.dp
          )
          DrawerRowItem(
            label = "Archive",
            selected = false,
            icon = { tint -> VeilIcons.Archive(tint = tint, size = 22.dp) },
            onClick = {
              scope.launch { drawerState.close() }
              onOpenArchive()
            }
          )
          DrawerRowItem(
            label = "Trash",
            selected = false,
            icon = { tint -> VeilIcons.Trash(tint = tint, size = 22.dp) },
            onClick = {
              scope.launch { drawerState.close() }
              onOpenTrash()
            }
          )
          DrawerRowItem(
            label = "Settings",
            selected = false,
            icon = { tint -> VeilIcons.SettingsGear(tint = tint, size = 22.dp) },
            onClick = {
              scope.launch { drawerState.close() }
              onOpenSettings()
            }
          )
        }
      }
    }
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(colors.bg)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val effectiveTopPadding = if (statusBarTop > 44.dp) statusBarTop + 12.dp else 56.dp

        // Animated Top Header: transitions smoothly between Search/Sort/Theme Bar and Upgraded Selection Bar
        AnimatedContent(
          targetState = selectedIds.isNotEmpty(),
          transitionSpec = {
            (fadeIn(tween(if (reduceMotion) 0 else 180)) +
              slideInVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy)) { -it / 4 }) togetherWith
              (fadeOut(tween(if (reduceMotion) 0 else 140)) +
                slideOutVertically { -it / 4 })
          },
          label = "top_bar_selection_switch",
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = effectiveTopPadding, bottom = 12.dp)
        ) { isSelecting ->
          if (isSelecting) {
            val allPinned = selectedNotes.isNotEmpty() && selectedNotes.all { it.isPinned }
            HomeSelectionTopBar(
              selectedCount = selectedIds.size,
              allPinned = allPinned,
              enableSharing = enableSharing,
              onClearSelection = { viewModel.clearSelection() },
              onTogglePin = {
                viewModel.setNotesPinned(selectedNotes, !allPinned)
                viewModel.clearSelection()
              },
              onOpenReminder = { showSelectionReminderDialog = true },
              onOpenColorPicker = { showSelectionColorDialog = true },
              onOpenLabelManager = { showSelectionLabelDialog = true },
              onArchive = {
                val snapshot = selectedNotes.toList()
                snapshot.forEach { viewModel.archiveNote(it) }
                viewModel.clearSelection()
                showTransientToast(
                  message = if (snapshot.size == 1) "Archived" else "Archived ${snapshot.size} notes",
                  archived = snapshot
                )
              },
              onDelete = {
                selectedNotes.forEach { viewModel.trashNote(it) }
                viewModel.clearSelection()
                showTransientToast("Moved to Trash")
              },
              onMakeCopy = {
                selectedNotes.forEach { viewModel.duplicateNote(it) }
                viewModel.clearSelection()
                showTransientToast(if (selectedNotes.size == 1) "Copied note" else "Copied ${selectedNotes.size} notes")
              },
              onCopyAllText = {
                val text = NoteImageHelper.formatNotesPlainText(selectedNotes)
                NoteImageHelper.copyTextToClipboard(context, "Notes", text)
                viewModel.clearSelection()
                showTransientToast("Copied all text")
              },
              onShare = {
                val text = NoteImageHelper.formatNotesPlainText(selectedNotes)
                val firstTitle = selectedNotes.firstOrNull()?.title.orEmpty()
                val allImages = selectedNotes.flatMap { NoteExtrasCodec.decode(it.checklistJson).images }
                NoteImageHelper.shareNote(context, firstTitle, text, allImages)
                viewModel.clearSelection()
              }
            )
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              // Plain-looking Stealth Search Pill: NEVER masks dots, NEVER shows accent ring giveaway.
              val searchPillShape = RoundedCornerShape(24.dp)
              Row(
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .shadow(
                    elevation = 3.dp,
                    shape = searchPillShape,
                    ambientColor = colors.shadow,
                    spotColor = colors.shadow
                  )
                  .clip(searchPillShape)
                  .background(colors.surface)
                  .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .tactilePress(scaleDown = 0.88f) { scope.launch { drawerState.open() } }
                    .semantics { contentDescription = "Open navigation menu" }
                    .testTag("drawer_menu_button"),
                  contentAlignment = Alignment.Center
                ) {
                  VeilIcons.MenuLines(tint = colors.muted, size = 20.dp)
                }

                BasicTextField(
                  value = searchQuery,
                  onValueChange = { viewModel.setSearchQuery(it) },
                  singleLine = true,
                  textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = colors.text,
                    fontSize = 16.sp
                  ),
                  cursorBrush = SolidColor(colors.accent),
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                  keyboardActions = KeyboardActions(
                    onSearch = {
                      viewModel.submitSearch()
                      focusManager.clearFocus()
                    }
                  ),
                  decorationBox = { innerTextField ->
                    Box(
                      modifier = Modifier.fillMaxWidth(),
                      contentAlignment = Alignment.CenterStart
                    ) {
                      if (searchQuery.isEmpty()) {
                        Text(
                          text = "Search notes",
                          style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                          color = colors.muted
                        )
                      }
                      innerTextField()
                    }
                  },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("search_input")
                )

                if (searchQuery.isNotEmpty()) {
                  Box(
                    modifier = Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .clickable(
                        role = Role.Button,
                        onClickLabel = "Clear search",
                        onClick = { viewModel.setSearchQuery("") }
                      )
                      .testTag("clear_search_button"),
                    contentAlignment = Alignment.Center
                  ) {
                    VeilIcons.Close(tint = colors.muted, size = 16.dp)
                  }
                } else {
                  VeilIcons.Search(tint = colors.muted, size = 18.dp)
                }
              }

              // Clean Sort Menu Button next to search bar
              SortOrderMenuButton(
                currentSort = sortOrder,
                onSelectSort = { newSort ->
                  viewModel.setSortOrder(newSort)
                }
              )

              // 44x44 round sun/dark-mode toggle
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    themeButtonCenter = Offset(
                      pos.x + coords.size.width / 2f,
                      pos.y + coords.size.height / 2f
                    )
                  }
                  .clip(CircleShape)
                  .tactilePress(scaleDown = 0.88f, hapticOnClick = true) { onToggleTheme(themeButtonCenter) }
                  .semantics {
                    contentDescription = if (isDarkTheme) "Switch to light mode" else "Switch to dark mode"
                  }
                  .testTag("theme_toggle_button"),
                contentAlignment = Alignment.Center
              ) {
                VeilIcons.SunMoonMorph(
                  isDark = isDarkTheme,
                  tint = colors.text,
                  size = 23.dp
                )
              }
            }
          }
        }

        // Slim offline Smart Auto-Categories filter chips directly below Search Bar
        if (proFeatures.autoCategoriesEnabled && selectedIds.isEmpty()) {
          SmartCategoryFilterRow(
            selectedCategory = selectedCategory,
            counts = smartCategoryCounts,
            onSelectCategory = { selectedCategory = it },
            modifier = Modifier.padding(bottom = 8.dp)
          )
        }

        // Dismissible Remote Announcement Banner (controlled by config.json announcements)
        val announcement = remoteConfig.announcements
        val showAnnouncementBanner = selectedIds.isEmpty() &&
          announcement.isDisplayable &&
          dismissedAnnouncementHash != announcement.contentHash
        HomeAnnouncementBanner(
          announcement = announcement,
          visible = showAnnouncementBanner,
          onOpenLink = { url -> openSafeExternalUrl(context, url) },
          onDismiss = { viewModel.dismissAnnouncement(announcement.contentHash) }
        )

        // Smart GitHub Star Nudge Card — shown ONLY ONCE ever after the user creates their 5th note
        val githubRepoUrl = remoteConfig.socialLinks.githubRepo
        val showStarNudge = selectedIds.isEmpty() &&
          totalNotesCreated >= 5 &&
          !starNudgeDismissed &&
          RemoteConfigValidator.isValidHttpsUrl(githubRepoUrl)
        GitHubStarNudgeCard(
          visible = showStarNudge,
          onStarNow = {
            viewModel.dismissStarNudge()
            openSafeExternalUrl(context, githubRepoUrl)
          },
          onMaybeLater = {
            viewModel.dismissStarNudge()
          }
        )

        // Active Reminders / Label filter indicator pill
        if (currentDrawerScreen == "reminders" || selectedLabelFilter != null || currentDrawerScreen == "labels") {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              if (currentDrawerScreen == "reminders") {
                VeilIcons.Bell(tint = colors.accent, size = 16.dp)
                Text("REMINDERS", style = VeilTextStyles.sectionLabel, color = colors.accent)
              } else {
                VeilIcons.Tag(tint = colors.accent, size = 16.dp)
                Text(
                  text = (selectedLabelFilter ?: "ALL LABELED").uppercase(),
                  style = VeilTextStyles.sectionLabel,
                  color = colors.accent
                )
              }
            }
            Text(
              text = "Show all",
              style = VeilTextStyles.meta.copy(fontWeight = FontWeight.Medium),
              color = colors.muted,
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { viewModel.clearLabelFilter() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }

        val hasAnythingToShow =
          (areHiddenUnlocked && filteredHiddenNotes.isNotEmpty()) ||
            pinnedNotes.isNotEmpty() ||
            otherNotes.isNotEmpty()

        if (!hasAnythingToShow) {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(bottom = 110.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              if (currentDrawerScreen == "reminders") {
                VeilIcons.Bell(tint = colors.muted.copy(alpha = 0.5f), size = 40.dp)
                Text("No upcoming reminders", style = MaterialTheme.typography.bodyLarge, color = colors.muted)
              } else {
                VeilIcons.NoteDoc(tint = colors.muted.copy(alpha = 0.5f), size = 40.dp)
                Text(
                  text = when {
                    searchQuery.isNotEmpty() -> "No matching notes"
                    proFeatures.autoCategoriesEnabled && selectedCategory != NoteSmartCategory.ALL ->
                      "No ${selectedCategory.label.lowercase()} notes yet"
                    else -> "Your notes appear here"
                  },
                  style = MaterialTheme.typography.bodyLarge,
                  color = colors.muted
                )
              }
            }
          }
        } else {
          val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
          LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(if (isGridView) 2 else 1),
            userScrollEnabled = draggingNoteId == null,
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            contentPadding = PaddingValues(
              start = 16.dp,
              end = 16.dp,
              top = 8.dp,
              bottom = 132.dp + navBottom
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp
          ) {
            // Silently reveal hidden notes when unlocked via Enter/Search (NO green banner!)
            if (areHiddenUnlocked && filteredHiddenNotes.isNotEmpty()) {
              item(span = StaggeredGridItemSpan.FullLine, key = "header_hidden") {
                SectionLabel("HIDDEN NOTES")
              }

              itemsIndexed(
                items = filteredHiddenNotes,
                span = { _, _ -> StaggeredGridItemSpan.FullLine },
                key = { _, note -> "hidden_${note.id}" }
              ) { index, note ->
                StaggeredFadeRise(
                  index = index,
                  reduceMotion = reduceMotion,
                  modifier = Modifier.animateItem()
                ) {
                  HiddenNoteCard(
                    note = note,
                    isSelected = selectedIds.contains(note.id),
                    moveTickedToBottom = moveTickedToBottom,
                    showSelfDestructBadge = proFeatures.selfDestructEnabled,
                    onClick = {
                      if (selectedIds.isNotEmpty()) viewModel.toggleNoteSelection(note.id)
                      else onNoteClick(note.id)
                    },
                    onLongClick = {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      viewModel.toggleNoteSelection(note.id)
                    }
                  )
                }
              }
            }

            // PINNED SECTION (supports Swipe-to-Archive + Drag-and-Drop Reorder)
            if (pinnedNotes.isNotEmpty()) {
              item(span = StaggeredGridItemSpan.FullLine, key = "header_pinned") {
                SectionLabel("PINNED")
              }
              itemsIndexed(
                items = pinnedNotes,
                key = { _, note -> "pinned_${note.id}" }
              ) { index, note ->
                val isDraggingThis = draggingNoteId == note.id
                val dragOffsetForCard = if (isDraggingThis) dragOffset else Offset.Zero
                StaggeredFadeRise(
                  index = index,
                  reduceMotion = reduceMotion,
                  modifier = Modifier
                    .animateItem()
                    .zIndex(if (isDraggingThis) 20f else 0f)
                ) {
                  InteractiveHomeNoteCard(
                    note = note,
                    isSelected = selectedIds.contains(note.id),
                    isDragging = isDraggingThis,
                    dragOffset = dragOffsetForCard,
                    swipeEnabled = draggingNoteId == null && selectedIds.isEmpty(),
                    canStartDrag = selectedIds.size <= 1,
                    moveTickedToBottom = moveTickedToBottom,
                    showSelfDestructBadge = proFeatures.selfDestructEnabled,
                    onBoundsMeasured = { rect -> itemBounds[note.id] = rect },
                    onClick = {
                      if (selectedIds.isNotEmpty()) viewModel.toggleNoteSelection(note.id)
                      else onNoteClick(note.id)
                    },
                    onSwipeArchive = {
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      viewModel.archiveNote(note)
                      showTransientToast("Archived", archived = listOf(note))
                    },
                    onLongPressSelect = {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      longPressStartedSelection = selectedIds.isEmpty()
                      viewModel.toggleNoteSelection(note.id)
                    },
                    onLongPressDragStart = { initialOffset ->
                      if (longPressStartedSelection) {
                        viewModel.clearSelection()
                        longPressStartedSelection = false
                      }
                      livePinnedIds = pinnedNotes.map { it.id }
                      draggingNoteId = note.id
                      draggingIsPinned = true
                      dragOffset = initialOffset
                    },
                    onLongPressDrag = { delta ->
                      dragOffset += delta
                      val originRect = itemBounds[note.id]
                      if (originRect != null && draggingIsPinned) {
                        val liveCenter = originRect.center + dragOffset
                        val targetId = livePinnedIds.firstOrNull { candidateId ->
                          candidateId != note.id && itemBounds[candidateId]?.contains(liveCenter) == true
                        }
                        if (targetId != null) {
                          val fromIdx = livePinnedIds.indexOf(note.id)
                          val toIdx = livePinnedIds.indexOf(targetId)
                          if (fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
                            val targetRect = itemBounds[targetId]
                            livePinnedIds = livePinnedIds.toMutableList().apply {
                              add(toIdx, removeAt(fromIdx))
                            }
                            if (targetRect != null) {
                              dragOffset -= (targetRect.center - originRect.center)
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          }
                        }
                      }
                    },
                    onLongPressDragEnd = { commitOrder ->
                      if (commitOrder) {
                        viewModel.updateCustomNoteOrder(livePinnedIds + liveOtherIds)
                      }
                      draggingNoteId = null
                      dragOffset = Offset.Zero
                      longPressStartedSelection = false
                    }
                  )
                }
              }
            }

            // OTHERS SECTION (supports Swipe-to-Archive + Drag-and-Drop Reorder)
            if (otherNotes.isNotEmpty()) {
              if (pinnedNotes.isNotEmpty() || (areHiddenUnlocked && hiddenNotes.isNotEmpty())) {
                item(span = StaggeredGridItemSpan.FullLine, key = "header_others") {
                  SectionLabel("OTHERS")
                }
              }
              itemsIndexed(
                items = otherNotes,
                key = { _, note -> "other_${note.id}" }
              ) { index, note ->
                val isDraggingThis = draggingNoteId == note.id
                val dragOffsetForCard = if (isDraggingThis) dragOffset else Offset.Zero
                StaggeredFadeRise(
                  index = index,
                  reduceMotion = reduceMotion,
                  modifier = Modifier
                    .animateItem()
                    .zIndex(if (isDraggingThis) 20f else 0f)
                ) {
                  InteractiveHomeNoteCard(
                    note = note,
                    isSelected = selectedIds.contains(note.id),
                    isDragging = isDraggingThis,
                    dragOffset = dragOffsetForCard,
                    swipeEnabled = draggingNoteId == null && selectedIds.isEmpty(),
                    canStartDrag = selectedIds.size <= 1,
                    moveTickedToBottom = moveTickedToBottom,
                    showSelfDestructBadge = proFeatures.selfDestructEnabled,
                    onBoundsMeasured = { rect -> itemBounds[note.id] = rect },
                    onClick = {
                      if (selectedIds.isNotEmpty()) viewModel.toggleNoteSelection(note.id)
                      else onNoteClick(note.id)
                    },
                    onSwipeArchive = {
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      viewModel.archiveNote(note)
                      showTransientToast("Archived", archived = listOf(note))
                    },
                    onLongPressSelect = {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      longPressStartedSelection = selectedIds.isEmpty()
                      viewModel.toggleNoteSelection(note.id)
                    },
                    onLongPressDragStart = { initialOffset ->
                      if (longPressStartedSelection) {
                        viewModel.clearSelection()
                        longPressStartedSelection = false
                      }
                      liveOtherIds = otherNotes.map { it.id }
                      draggingNoteId = note.id
                      draggingIsPinned = false
                      dragOffset = initialOffset
                    },
                    onLongPressDrag = { delta ->
                      dragOffset += delta
                      val originRect = itemBounds[note.id]
                      if (originRect != null && !draggingIsPinned) {
                        val liveCenter = originRect.center + dragOffset
                        val targetId = liveOtherIds.firstOrNull { candidateId ->
                          candidateId != note.id && itemBounds[candidateId]?.contains(liveCenter) == true
                        }
                        if (targetId != null) {
                          val fromIdx = liveOtherIds.indexOf(note.id)
                          val toIdx = liveOtherIds.indexOf(targetId)
                          if (fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
                            val targetRect = itemBounds[targetId]
                            liveOtherIds = liveOtherIds.toMutableList().apply {
                              add(toIdx, removeAt(fromIdx))
                            }
                            if (targetRect != null) {
                              dragOffset -= (targetRect.center - originRect.center)
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          }
                        }
                      }
                    },
                    onLongPressDragEnd = { commitOrder ->
                      if (commitOrder) {
                        viewModel.updateCustomNoteOrder(livePinnedIds + liveOtherIds)
                      }
                      draggingNoteId = null
                      dragOffset = Offset.Zero
                      longPressStartedSelection = false
                    }
                  )
                }
              }
            }
          }
        }
      }

      // 5-second Floating "Archived — Undo" Toast sitting cleanly above the Bottom Bar
      val currentToast = toastState
      ArchiveUndoToast(
        visible = currentToast != null,
        toastKey = currentToast?.key ?: 0L,
        message = currentToast?.message.orEmpty(),
        showUndo = currentToast?.showUndo == true,
        onUndo = {
          toastDismissJob?.cancel()
          val toRestore = toastState?.archivedNotes.orEmpty()
          toRestore.forEach { n ->
            viewModel.unarchiveNote(n, restorePinned = n.isPinned)
          }
          toastState = null
        },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(bottom = 104.dp)
      )

      // 5-second Floating Clipboard Quick-Save Pill (strictly never overlaps confirmation toast or clicks through)
      val currentClipOffer = clipboardOfferText
      ClipboardQuickSavePill(
        visible = proFeatures.clipboardPillEnabled && currentClipOffer != null && currentToast == null,
        previewText = currentClipOffer.orEmpty(),
        onSave = {
          clipboardDismissJob?.cancel()
          val textToSave = clipboardOfferText.orEmpty()
          clipboardOfferText = null
          viewModel.quickSaveClipboardNote(textToSave)
          scope.launch {
            delay(180L)
            showTransientToast("Note saved from clipboard")
          }
        },
        onDismiss = {
          clipboardDismissJob?.cancel()
          clipboardOfferText?.let { viewModel.markClipboardTextHandled(it) }
          clipboardOfferText = null
        },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(bottom = 104.dp)
      )

      // Fixed Bottom Bar with real action triggers
      VeilBottomBar(
        onChecklistClick = { onCreateNoteWithMode("checklist") },
        onDrawClick = { onCreateNoteWithMode("draw") },
        onVoiceClick = { onCreateNoteWithMode("voice") },
        onCreateNote = { onCreateNoteWithMode("text") },
        modifier = Modifier.align(Alignment.BottomCenter)
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InteractiveHomeNoteCard(
  note: NoteEntity,
  isSelected: Boolean,
  isDragging: Boolean,
  dragOffset: Offset,
  swipeEnabled: Boolean,
  canStartDrag: Boolean,
  moveTickedToBottom: Boolean,
  showSelfDestructBadge: Boolean,
  onBoundsMeasured: (Rect) -> Unit,
  onClick: () -> Unit,
  onSwipeArchive: () -> Unit,
  onLongPressSelect: () -> Unit,
  onLongPressDragStart: (initialOffset: Offset) -> Unit,
  onLongPressDrag: (Offset) -> Unit,
  onLongPressDragEnd: (commitOrder: Boolean) -> Unit
) {
  val colors = VeilThemeTokens.colors
  val reduceMotion = VeilThemeTokens.reduceMotion
  val cardShape = RoundedCornerShape(16.dp)

  val latestSwipeEnabled by rememberUpdatedState(swipeEnabled)
  val latestCanStartDrag by rememberUpdatedState(canStartDrag)
  val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { value ->
      if (!latestSwipeEnabled) {
        false
      } else if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
        onSwipeArchive()
        true
      } else {
        false
      }
    }
  )

  val latestOnLongPressSelect by rememberUpdatedState(onLongPressSelect)
  val latestOnDragStart by rememberUpdatedState(onLongPressDragStart)
  val latestOnDrag by rememberUpdatedState(onLongPressDrag)
  val latestOnDragEnd by rememberUpdatedState(onLongPressDragEnd)

  val dragScale by animateFloatAsState(
    targetValue = if (isDragging && !reduceMotion) 1.04f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "drag_lift_scale"
  )

  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (isPressed && !isDragging && !reduceMotion) 0.97f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "card_press_scale"
  )

  val cardBg = colors.noteCardColor(note.color)
  val extras = remember(note.checklistJson) { NoteExtrasCodec.decode(note.checklistJson) }

  // Keep SwipeToDismissBox ALWAYS mounted so toggling swipeEnabled never unmounts pointerInput mid-gesture.
  SwipeToDismissBox(
    state = dismissState,
    enableDismissFromStartToEnd = swipeEnabled,
    enableDismissFromEndToStart = swipeEnabled,
    backgroundContent = {
      if (swipeEnabled) {
        val dir = dismissState.dismissDirection
        val align = if (dir == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
        val iconScale by animateFloatAsState(
          targetValue = if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) 1.15f else 0.85f,
          animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
          label = "swipe_archive_icon_scale"
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .clip(cardShape)
            .background(colors.accentSoft)
            .padding(horizontal = 20.dp),
          contentAlignment = align
        ) {
          Box(
            modifier = Modifier.graphicsLayer {
              scaleX = iconScale
              scaleY = iconScale
            }
          ) {
            VeilIcons.Archive(tint = colors.accent, size = 22.dp)
          }
        }
      }
    },
    content = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .onGloballyPositioned { coords ->
            if (!isDragging) {
              onBoundsMeasured(coords.boundsInRoot())
            }
          }
          .graphicsLayer {
            translationX = dragOffset.x
            translationY = dragOffset.y
            scaleX = dragScale * pressScale
            scaleY = dragScale * pressScale
          }
          .shadow(
            elevation = if (isDragging) 14.dp else 2.dp,
            shape = cardShape,
            ambientColor = colors.shadow,
            spotColor = colors.shadow
          )
          .clip(cardShape)
          .background(cardBg)
          .then(
            if (isSelected || isDragging) {
              Modifier.border(2.dp, colors.accent, cardShape)
            } else {
              Modifier
            }
          )
          // clickable BEFORE pointerInput so inner pointerInput consumes UP after a 400ms long-press
          // without firing onClick, while normal taps (<400ms) pass cleanly through to clickable.
          .clickable(
            enabled = !isDragging,
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = onClick
          )
          .pointerInput(note.id) {
            val touchSlop = viewConfiguration.touchSlop
            val reorderThreshold = maxOf(touchSlop * 1.6f, 28.dp.toPx())
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false)
              val downId = down.id
              val downPos = down.position

              // Phase 1: Wait ~400ms while stationary for Long-Press
              var cancelledBeforeLongPress = false
              val timedOut = withTimeoutOrNull(400L) {
                while (true) {
                  val event = awaitPointerEvent(PointerEventPass.Main)
                  val change = event.changes.firstOrNull { it.id == downId }
                  if (change == null || !change.pressed || change.changedToUpIgnoreConsumed()) {
                    cancelledBeforeLongPress = true
                    break
                  }
                  if (change.isConsumed || (change.position - downPos).getDistance() > touchSlop) {
                    cancelledBeforeLongPress = true
                    break
                  }
                }
              } == null

              if (!timedOut || cancelledBeforeLongPress) {
                return@awaitEachGesture
              }

              // Phase 2: Stationary 400ms Long-Press confirmed -> haptic + select immediately
              latestOnLongPressSelect()

              // Phase 3: Track release (stays selected) vs active drag past threshold (starts reorder)
              var dragStarted = false
              try {
                while (true) {
                  val event = awaitPointerEvent(PointerEventPass.Main)
                  val change = event.changes.firstOrNull { it.id == downId } ?: break
                  if (!change.pressed || change.changedToUpIgnoreConsumed()) {
                    change.consume()
                    break
                  }
                  val delta = change.positionChange()
                  change.consume()
                  val distFromDown = (change.position - downPos).getDistance()
                  if (!dragStarted && distFromDown > reorderThreshold && latestCanStartDrag) {
                    dragStarted = true
                    latestOnDragStart(change.position - downPos)
                  } else if (dragStarted && delta != Offset.Zero) {
                    latestOnDrag(delta)
                  }
                }
              } finally {
                if (dragStarted) {
                  latestOnDragEnd(true)
                }
              }
            }
          }
          .testTag("note_card_${note.id}")
      ) {
        NoteBackgroundPatternOverlay(
          pattern = extras.backgroundPattern,
          tint = colors.text,
          alphaScale = 0.9f,
          modifier = Modifier.matchParentSize()
        )
        Box(modifier = Modifier.padding(14.dp)) {
          NoteCardContent(
            note = note,
            isHiddenStyle = false,
            moveTickedToBottom = moveTickedToBottom,
            showSelfDestructBadge = showSelfDestructBadge
          )
        }
      }
    }
  )
}

@Composable
private fun SectionLabel(text: String) {
  val colors = VeilThemeTokens.colors
  Text(
    text = text.uppercase(),
    style = VeilTextStyles.sectionLabel,
    color = colors.muted,
    modifier = Modifier.padding(top = 4.dp, bottom = 0.dp)
  )
}

@Composable
private fun StaggeredFadeRise(
  index: Int,
  reduceMotion: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val alpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
  val translationY = remember { Animatable(if (reduceMotion) 0f else 8f) }

  LaunchedEffect(Unit) {
    if (!reduceMotion) {
      delay((index.coerceAtMost(8) * 35L))
      launch { alpha.animateTo(1f, animationSpec = tween(230, easing = VeilEasing)) }
      launch { translationY.animateTo(0f, animationSpec = tween(230, easing = VeilEasing)) }
    }
  }

  Box(
    modifier = modifier.graphicsLayer {
      this.alpha = alpha.value
      this.translationY = translationY.value.dp.toPx()
    }
  ) {
    content()
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HiddenNoteCard(
  note: NoteEntity,
  isSelected: Boolean,
  moveTickedToBottom: Boolean,
  showSelfDestructBadge: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  val reduceMotion = VeilThemeTokens.reduceMotion
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed && !reduceMotion) 0.98f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "hidden_card_press_scale"
  )

  val cardShape = RoundedCornerShape(16.dp)
  val extras = remember(note.checklistJson) { NoteExtrasCodec.decode(note.checklistJson) }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clip(cardShape)
      .background(colors.accentSoft)
      .border(
        width = if (isSelected) 2.dp else 1.dp,
        color = if (isSelected) colors.accent else colors.hiddenBorder,
        shape = cardShape
      )
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(),
        onClick = onClick,
        onLongClick = onLongClick
      )
      .testTag("hidden_note_card_${note.id}")
  ) {
    NoteBackgroundPatternOverlay(
      pattern = extras.backgroundPattern,
      tint = colors.text,
      alphaScale = 0.85f,
      modifier = Modifier.matchParentSize()
    )
    Box(modifier = Modifier.padding(14.dp)) {
      NoteCardContent(
        note = note,
        isHiddenStyle = true,
        moveTickedToBottom = moveTickedToBottom,
        showSelfDestructBadge = showSelfDestructBadge
      )
    }
  }
}

@Composable
private fun NoteCardContent(
  note: NoteEntity,
  isHiddenStyle: Boolean,
  moveTickedToBottom: Boolean,
  showSelfDestructBadge: Boolean
) {
  val colors = VeilThemeTokens.colors
  val context = LocalContext.current
  val extras = remember(note.checklistJson) { NoteExtrasCodec.decode(note.checklistJson) }
  val validImages = remember(extras.images) { extras.images.filter { File(it).exists() } }
  val doodleStrokes = extras.doodle?.strokes.orEmpty()
  val legacyDoodleCount = remember(note.content) { extractDoodleCount(note.content) }
  val hasDoodle = doodleStrokes.isNotEmpty() || legacyDoodleCount > 0
  val cleanedBodyTriple = remember(note.content, extras.richSpans, extras.lineHeadings) {
    migrateLegacyMarkdownIfNeeded(
      rawText = stripDoodleMarkers(note.content),
      existingSpans = extras.richSpans,
      existingHeadings = extras.lineHeadings
    )
  }
  val cleanedBody = cleanedBodyTriple.first
  val cardPreviewAnnotated = remember(cleanedBodyTriple, colors.highlight, colors.muted, colors.accent) {
    val (text, spans, headings) = cleanedBodyTriple
    // Render heading lines bold at card body size so 2-column cards stay clean & proportional
    val headingBoldSpans = if (headings.isNotEmpty() && text.isNotEmpty()) {
      val extra = mutableListOf<com.example.data.RichSpanData>()
      var offset = 0
      text.split("\n").forEachIndexed { idx, line ->
        if ((headings[idx] ?: 0) > 0 && line.isNotEmpty()) {
          extra.add(com.example.data.RichSpanData(offset, offset + line.length, RichStyle.BOLD))
        }
        offset += line.length + 1
      }
      spans + extra
    } else {
      spans
    }
    buildWysiwygAnnotatedString(
      text = text,
      spans = headingBoldSpans,
      lineHeadings = emptyMap(),
      highlightBg = colors.highlight,
      mutedColor = colors.muted,
      accentColor = colors.accent
    )
  }
  val labelList = remember(note.labels) {
    note.labels.split(",").map { it.trim() }.filter { it.isNotEmpty() }
  }
  val reminderLabel = remember(note.reminderTime) { formatReminderChip(note.reminderTime) }

  val orderedChecklist = remember(extras.checklist, moveTickedToBottom) {
    if (moveTickedToBottom) {
      extras.checklist.filter { !it.checked } + extras.checklist.filter { it.checked }
    } else {
      extras.checklist
    }
  }

  Column(modifier = Modifier.fillMaxWidth()) {
    // Clean image attachment preview at top of card
    if (validImages.isNotEmpty()) {
      val imgShape = RoundedCornerShape(10.dp)
      if (validImages.size == 1) {
        AsyncImage(
          model = ImageRequest.Builder(context).data(File(validImages[0])).crossfade(true).build(),
          contentDescription = "Note image preview",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(imgShape)
        )
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          validImages.take(2).forEach { path ->
            AsyncImage(
              model = ImageRequest.Builder(context).data(File(path)).crossfade(true).build(),
              contentDescription = "Note image preview",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .weight(1f)
                .height(78.dp)
                .clip(imgShape)
            )
          }
        }
      }
      if (note.title.isNotEmpty() || cleanedBody.isNotEmpty() || hasDoodle || orderedChecklist.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
      }
    }

    if (note.title.isNotEmpty()) {
      Text(
        text = note.title,
        style = VeilThemeTokens.cardTitleStyle,
        color = colors.text,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      if (cleanedBody.isNotEmpty() || hasDoodle || orderedChecklist.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
      }
    }

    if (hasDoodle) {
      DoodleThumbnail(
        strokes = doodleStrokes,
        modifier = Modifier
          .fillMaxWidth()
          .height(78.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(colors.surface.copy(alpha = 0.8f))
      )
      if (cleanedBody.isNotEmpty() || orderedChecklist.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
      }
    }

    if (orderedChecklist.isNotEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        orderedChecklist.take(5).forEach { item ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            VeilIcons.ChecklistCircle(
              checked = item.checked,
              tint = colors.muted,
              accent = colors.accent
            )
            Text(
              text = item.text,
              style = VeilThemeTokens.cardBodyStyle.copy(
                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
              ),
              color = if (item.checked) colors.muted else colors.text,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    } else if (cleanedBody.isNotEmpty()) {
      Text(
        text = cardPreviewAnnotated,
        style = VeilThemeTokens.cardBodyStyle,
        color = colors.text,
        maxLines = 6,
        overflow = TextOverflow.Ellipsis
      )
    }

    val selfDestructBadge = remember(extras.expiresAt, showSelfDestructBadge) {
      if (showSelfDestructBadge && extras.expiresAt > System.currentTimeMillis()) {
        formatCountdownBadge(extras.expiresAt)
      } else {
        ""
      }
    }
    if (selfDestructBadge.isNotEmpty()) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(if (isHiddenStyle) colors.surface else colors.accentSoft)
          .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = "⏱ $selfDestructBadge",
          style = VeilTextStyles.labelChip,
          color = colors.accent,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    if (reminderLabel.isNotEmpty()) {
      val reminderPast = isReminderPast(note.reminderTime)
      val chipTint = if (reminderPast) colors.muted else colors.accent
      val chipBg = when {
        reminderPast -> colors.bg.copy(alpha = 0.55f)
        isHiddenStyle -> colors.surface
        else -> colors.accentSoft
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(chipBg)
          .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        VeilIcons.Bell(tint = chipTint, size = 12.dp)
        Text(
          text = if (reminderPast) "Past · $reminderLabel" else reminderLabel,
          style = VeilTextStyles.labelChip.copy(
            textDecoration = if (reminderPast) TextDecoration.LineThrough else TextDecoration.None
          ),
          color = chipTint,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    if (labelList.isNotEmpty()) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        labelList.take(3).forEach { tag ->
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(if (isHiddenStyle) colors.surface else colors.accentSoft)
              .padding(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text(
              text = tag,
              style = VeilTextStyles.labelChip,
              color = colors.accent,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DoodleThumbnail(
  strokes: List<DoodleStrokeData>,
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  val palette = remember(colors) { colors.drawingPalette() }

  Canvas(modifier = modifier.padding(6.dp)) {
    val w = size.width
    val h = size.height
    if (strokes.isEmpty()) {
      drawLine(
        color = colors.doodleHighlighter.copy(alpha = 0.45f),
        start = Offset(w * 0.1f, h * 0.65f),
        end = Offset(w * 0.88f, h * 0.42f),
        strokeWidth = 8.dp.toPx(),
        cap = StrokeCap.Round
      )
      val p = Path().apply {
        moveTo(w * 0.14f, h * 0.55f)
        cubicTo(w * 0.32f, h * 0.18f, w * 0.52f, h * 0.82f, w * 0.78f, h * 0.35f)
      }
      drawPath(p, colors.accent, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    } else {
      drawNormalizedStrokes(
        strokes = strokes,
        palette = palette,
        eraserColor = colors.surface,
        widthScale = 0.45f
      )
    }
  }
}

@Composable
private fun VeilBottomBar(
  onChecklistClick: () -> Unit,
  onDrawClick: () -> Unit,
  onVoiceClick: () -> Unit,
  onCreateNote: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = VeilThemeTokens.colors
  val reduceMotion = VeilThemeTokens.reduceMotion
  val haptic = LocalHapticFeedback.current
  val fabInteraction = remember { MutableInteractionSource() }
  val isFabPressed by fabInteraction.collectIsPressedAsState()

  val fabScale by animateFloatAsState(
    targetValue = if (isFabPressed && !reduceMotion) 0.88f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "fab_spring_scale"
  )

  val barShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(88.dp)
        .align(Alignment.BottomCenter)
        .shadow(
          elevation = 10.dp,
          shape = barShape,
          ambientColor = colors.bottomBarShadow,
          spotColor = colors.bottomBarShadow
        )
        .clip(barShape)
        .background(colors.surface)
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .tactilePress(scaleDown = 0.88f, hapticOnClick = true, onClick = onChecklistClick)
            .semantics { contentDescription = "New checklist note" }
            .testTag("quick_checklist_button"),
          contentAlignment = Alignment.Center
        ) {
          VeilIcons.Checklist(tint = colors.text, size = 24.dp)
        }
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .tactilePress(scaleDown = 0.88f, hapticOnClick = true, onClick = onDrawClick)
            .semantics { contentDescription = "New drawing note" }
            .testTag("quick_draw_button"),
          contentAlignment = Alignment.Center
        ) {
          VeilIcons.Pen(tint = colors.text, size = 24.dp)
        }
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .tactilePress(scaleDown = 0.88f, hapticOnClick = true, onClick = onVoiceClick)
            .semantics { contentDescription = "New voice note" }
            .testTag("quick_voice_button"),
          contentAlignment = Alignment.Center
        ) {
          VeilIcons.Mic(tint = colors.text, size = 24.dp)
        }
      }
    }

    Box(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 20.dp, bottom = 68.dp)
        .size(56.dp)
        .graphicsLayer {
          scaleX = fabScale
          scaleY = fabScale
        }
        .shadow(
          elevation = 12.dp,
          shape = CircleShape,
          ambientColor = colors.fabShadow,
          spotColor = colors.fabShadow
        )
        .clip(CircleShape)
        .background(colors.accent)
        .clickable(
          interactionSource = fabInteraction,
          indication = ripple(color = Color.White),
          role = Role.Button,
          onClickLabel = "Create new note",
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCreateNote()
          }
        )
        .semantics { contentDescription = "Create new note" }
        .testTag("fab_create_note"),
      contentAlignment = Alignment.Center
    ) {
      VeilIcons.Plus(tint = Color.White, size = 26.dp)
    }
  }
}

@Composable
private fun DrawerRowItem(
  label: String,
  selected: Boolean,
  icon: @Composable (Color) -> Unit,
  onClick: () -> Unit
) {
  val colors = VeilThemeTokens.colors
  val tint = if (selected) colors.accent else colors.text
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 2.dp)
      .clip(RoundedCornerShape(24.dp))
      .background(if (selected) colors.accentSoft else Color.Transparent)
      .tactilePress(scaleDown = 0.97f, hapticOnClick = true, onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    icon(tint)
    Text(
      text = label,
      style = VeilTextStyles.cardTitle.copy(
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
      ),
      color = tint,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}
