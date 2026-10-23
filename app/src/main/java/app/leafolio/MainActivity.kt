package app.leafolio

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.RestorePage
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import androidx.room.withTransaction
import app.leafolio.data.Diary
import app.leafolio.ui.DiarySearchPage
import app.leafolio.data.LeafolioDatabase
import app.leafolio.data.PhotoAttachment
import app.leafolio.data.PhotoStore
import app.leafolio.data.DataTransfer
import app.leafolio.data.Tag
import app.leafolio.data.DiaryTag
import app.leafolio.data.UserPreferencesStore
import app.leafolio.data.DiaryDeletion
import app.leafolio.data.DiarySharing
import app.leafolio.data.EditorText
import app.leafolio.data.TextEditHistory
import app.leafolio.data.ExportScope
import app.leafolio.data.BackupReminder
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog

private val Moods = listOf("开心", "平静", "一般", "低落", "难过", "焦虑", "生气")
private val LightLeaf = Color(0xFF486B55)
private val LightInk = Color(0xFF202923)
private val LightMuted = Color(0xFF727B74)
private val LightLine = Color(0xFFE5E8E1)
private val LightSelected = Color(0xFFE8EEE7)

class MainActivity : PrivateActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preparePrivacy()
        setContent {
            Box(Modifier.fillMaxSize()) {
                if (privacyReady) LeafolioApp()
                if (!privacyReady || locked) PrivacyCurtain(this@MainActivity)
            }
        }
    }
}

class DiaryViewModel(
    private val dao: app.leafolio.data.DiaryDao,
    private val photoDao: app.leafolio.data.PhotoDao,
    private val tagDao: app.leafolio.data.TagDao,
    private val database: LeafolioDatabase,
    context: android.content.Context,
) : ViewModel() {
    private val context = context.applicationContext
    private val photoOrderMutex = Mutex()
    init {
        viewModelScope.launch {
            runCatching { app.leafolio.data.StorageMaintenance.run(context, database) }
                .onFailure { Toast.makeText(context, it.message ?: "临时文件清理失败", Toast.LENGTH_LONG).show() }
        }
    }

    private val entryLimit = MutableStateFlow(60)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val entries = entryLimit.flatMapLatest { dao.observePage(it) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val entryCount = dao.observeEntryCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val diaryCount = dao.observeCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val drafts = dao.observeDrafts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var homeLimit = 60
    fun setFullEntries(full: Boolean) { entryLimit.value = if (full) Int.MAX_VALUE else homeLimit }
    fun loadMore() { homeLimit += 60; entryLimit.value = homeLimit }
    val latestDraft = dao.observeLatestDraft().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )
    val tags = tagDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun get(id: String) = dao.getById(id)

    suspend fun save(diary: Diary) {
        database.withTransaction {
            dao.upsert(diary)
            tagDao.clearForDiary(diary.id)
            diary.tags.split('|').map(String::trim).filter(String::isNotBlank).distinctBy { it.lowercase() }.forEach { name ->
                var tag = tagDao.getByName(name)
                if (tag == null) {
                    tagDao.insert(Tag(UUID.randomUUID().toString(), name))
                    tag = tagDao.getByName(name)
                }
                tag?.let { tagDao.link(DiaryTag(diary.id, it.id)) }
            }
        }
    }

    suspend fun createTag(name: String) {
        val normalized = name.trim()
        if (normalized.isEmpty() || tagDao.getByName(normalized) != null) return
        tagDao.insert(Tag(UUID.randomUUID().toString(), normalized))
    }

    suspend fun renameTag(id: String, name: String) {
        val nextName = name.trim()
        val source = tagDao.getById(id) ?: return
        if (nextName.isEmpty()) return
        database.withTransaction {
            val target = tagDao.getByName(nextName)
            val diaryIds = tagDao.getDiaryIds(id)
            if (target == null) {
                tagDao.rename(id, nextName)
                updateTagNames(diaryIds, source.name, nextName)
            } else if (target.id != id) {
                diaryIds.forEach { diaryId ->
                    tagDao.link(DiaryTag(diaryId, target.id))
                    updateTagNames(listOf(diaryId), source.name, target.name)
                }
                tagDao.delete(id)
            }
        }
    }

    suspend fun deleteTag(id: String) {
        val source = tagDao.getById(id) ?: return
        database.withTransaction {
            val diaryIds = tagDao.getDiaryIds(id)
            diaryIds.forEach { diaryId ->
                val diary = dao.getById(diaryId) ?: return@forEach
                val names = diary.tags.split('|').map(String::trim).filter { it.isNotBlank() && !it.equals(source.name, true) }
                dao.upsert(diary.copy(tags = names.distinctBy { it.lowercase() }.joinToString("|")))
            }
            tagDao.delete(id)
        }
    }

    private suspend fun updateTagNames(diaryIds: List<String>, oldName: String, newName: String) {
        diaryIds.forEach { diaryId ->
            val diary = dao.getById(diaryId) ?: return@forEach
            val names = diary.tags.split('|').map(String::trim).filter(String::isNotBlank)
                .map { if (it.equals(oldName, true)) newName else it }
                .distinctBy { it.lowercase() }
            dao.upsert(diary.copy(tags = names.joinToString("|")))
        }
    }

    suspend fun deleteEmptyDraft(id: String) = app.leafolio.data.FileOperations.mutex.withLock { dao.deleteEmptyDraft(id) }
    fun trash() = dao.observeTrash()
    suspend fun deletePermanently(id: String) = DiaryDeletion.deletePermanently(context, database, id)
    suspend fun clearTrash() = DiaryDeletion.clearTrash(context, database)
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        app.leafolio.data.FileOperations.mutex.withLock {
        database.withTransaction {
            dao.deleteAll()
            tagDao.deleteAll()
        }
        val photoDirectory = File(context.filesDir, "diary-photos")
        if (photoDirectory.exists() && !photoDirectory.deleteRecursively()) throw java.io.IOException("无法清理照片文件")
        }
    }
    suspend fun storageBytes(): Long = withContext(Dispatchers.IO) {
        val photos = File(context.filesDir, "diary-photos").walkTopDown().filter(File::isFile).sumOf(File::length)
        val dbFile = context.getDatabasePath("leafolio.db")
        photos + listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm")).sumOf { if (it.isFile) it.length() else 0L }
    }
    fun photos(id: String?) = id?.let(photoDao::observeForDiary) ?: flowOf(emptyList())

    suspend fun importPhoto(diary: Diary, uri: android.net.Uri): PhotoAttachment = app.leafolio.data.FileOperations.mutex.withLock {
        val diaryId = diary.id
        // A cancelled picker or a background save may have removed an empty draft.
        // Keep parent creation and attachment insertion under the same cleanup lock.
        if (dao.getById(diaryId) == null) save(diary)
        if (photoDao.countForDiary(diaryId) >= 9) throw IllegalStateException("每篇日记最多添加 9 张照片")
        val keepOriginal = UserPreferencesStore(context).preferences.first().keepOriginalPhotos
        val attachment = PhotoStore.import(context, diaryId, uri, keepOriginal).copy(position = photoDao.countForDiary(diaryId))
        runCatching { photoDao.insert(attachment); attachment }
            .getOrElse { PhotoStore.delete(context, attachment); throw it }
    }

    suspend fun removePhoto(id: String) = app.leafolio.data.FileOperations.mutex.withLock {
        val attachment = photoDao.getById(id) ?: return@withLock
        photoDao.delete(id)
        if (photoDao.getAll().none { it.relativePath == attachment.relativePath }) PhotoStore.delete(context, attachment)
        photoDao.getAll().filter { it.diaryId == attachment.diaryId }.sortedBy { it.position }
            .forEachIndexed { index, photo -> photoDao.setPosition(photo.id, index) }
    }

    suspend fun movePhotoTo(id: String, targetId: String) {
        photoOrderMutex.withLock {
            val photo = photoDao.getById(id) ?: return
            val ordered = photoDao.getAll().filter { it.diaryId == photo.diaryId }.sortedBy { it.position }.toMutableList()
            val from = ordered.indexOfFirst { it.id == id }
            val to = ordered.indexOfFirst { it.id == targetId }
            if (from < 0 || to < 0 || from == to) return
            val moved = ordered.removeAt(from)
            ordered.add(to, moved)
            database.withTransaction { ordered.forEachIndexed { index, attachment -> photoDao.setPosition(attachment.id, index) } }
        }
    }
}

@Composable
private fun LeafolioApp() {
    val context = LocalContext.current
    val preferencesStore = remember { UserPreferencesStore(context.applicationContext) }
    val preferencesState by preferencesStore.preferences.collectAsState(initial = null)
    val preferences = preferencesState ?: app.leafolio.data.UserPreferences()
    val dao = remember { LeafolioDatabase.get(context).diaryDao() }
    val diaryVm: DiaryViewModel = viewModel(factory = remember(dao) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = DiaryViewModel(
                dao,
                LeafolioDatabase.get(context).photoDao(),
                LeafolioDatabase.get(context).tagDao(),
                LeafolioDatabase.get(context),
                context.applicationContext,
            ) as T
        }
    })
    var page by rememberSaveable {
        val fromReminder = (context as? Activity)?.intent?.getBooleanExtra("open_backup_settings", false) == true
        (context as? Activity)?.intent?.removeExtra("open_backup_settings")
        mutableStateOf(if (fromReminder) "settings" else "home")
    }
    var tab by rememberSaveable { mutableStateOf(0) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    val searchStateHolder = rememberSaveableStateHolder()
    val entries by diaryVm.entries.collectAsState()
    val diaryCount by diaryVm.diaryCount.collectAsState()
    val entryCount by diaryVm.entryCount.collectAsState()
    val drafts by diaryVm.drafts.collectAsState()
    LaunchedEffect(page, tab, searchOpen) { diaryVm.setFullEntries(page !in setOf("home", "settings", "editor") || tab != 0 || searchOpen) }
    val selectedDiary by androidx.compose.runtime.produceState<Diary?>(null, editingId, entries) {
        value = editingId?.let { diaryVm.get(it) }
    }
    val draft by diaryVm.latestDraft.collectAsState()
    val trash by diaryVm.trash().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val transferVm: TransferViewModel = viewModel()
    val transfer by transferVm.state.collectAsState()
    var exportMode by rememberSaveable { mutableStateOf("all") }
    var exportStart by rememberSaveable { mutableStateOf<String?>(null) }
    var exportEnd by rememberSaveable { mutableStateOf<String?>(null) }
    var exportIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var notificationsAllowed by remember { mutableStateOf(BackupReminder.notificationsAllowed(context)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        notificationsAllowed = BackupReminder.notificationsAllowed(context)
        onPauseOrDispose { }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAllowed = BackupReminder.notificationsAllowed(context)
        if (!it) Toast.makeText(context, "通知未开启，备份提醒暂时无法送达，可在系统设置中开启。", Toast.LENGTH_LONG).show()
    }
    LaunchedEffect(preferencesState?.backupReminderDays, preferencesState?.lastBackupAt, preferencesState?.reminderEnabledAt, preferencesState?.lastBackupReminderAt) {
        if (preferencesState != null) BackupReminder.sync(context, preferences)
    }
    LaunchedEffect(preferencesState?.dailyReminderMinutes) {
        if (preferencesState != null) app.leafolio.data.DailyReminder.sync(context, preferences)
    }
    var storageBytes by remember { mutableStateOf(0L) }
    LaunchedEffect(page, diaryCount) {
        if (page == "settings") storageBytes = diaryVm.storageBytes()
    }
    val updateEntry: (Diary) -> Unit = { entry -> scope.launch { diaryVm.save(entry) } }
    val trashEntry: (Diary) -> Unit = { entry -> scope.launch { diaryVm.save(entry.copy(deletedAt = System.currentTimeMillis())) } }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) transferVm.backup(uri)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) transferVm.export(uri, ExportScope(exportMode, exportStart?.let(LocalDate::parse), exportEnd?.let(LocalDate::parse), exportIds.toSet()))
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) transferVm.inspect(uri)
    }

    fun back() {
        when {
            page != "home" -> page = "home"
            searchOpen -> searchOpen = false
            tab != 0 -> tab = 0
        }
    }
    BackHandler(enabled = page != "home" || tab != 0 || searchOpen) { back() }

    val dark = when (preferences.theme) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    // One Dark Pro: blue-grey surfaces with restrained blue accents.
    val accent = if (dark) Color(0xFF61AFEF) else LightLeaf
    val onCanvas = if (dark) Color(0xFFD7DAE0) else LightInk
    val secondaryText = if (dark) Color(0xFFABB2BF) else LightMuted
    val divider = if (dark) Color(0xFF3E4452) else LightLine
    val selectedSurface = if (dark) Color(0xFF343E4F) else LightSelected
    val softSurface = if (dark) Color(0xFF2C313C) else Color(0xFFF0F2EC)
    val view = LocalView.current
    SideEffect {
        val controller = WindowCompat.getInsetsController((view.context as Activity).window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
    val colors = if (dark) {
        androidx.compose.material3.darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF182632),
            secondary = secondaryText,
            onSecondary = Color(0xFF21252B),
            secondaryContainer = selectedSurface,
            onSecondaryContainer = onCanvas,
            primaryContainer = selectedSurface,
            onPrimaryContainer = accent,
            tertiary = Color(0xFF98C379),
            onTertiary = Color(0xFF1F2D19),
            background = Color(0xFF282C34),
            onBackground = onCanvas,
            surface = Color(0xFF282C34),
            onSurface = onCanvas,
            onSurfaceVariant = secondaryText,
            surfaceContainer = softSurface,
            surfaceContainerLowest = Color(0xFF1E2228),
            surfaceContainerLow = Color(0xFF21252B),
            surfaceContainerHigh = Color(0xFF303640),
            surfaceContainerHighest = Color(0xFF373E49),
            surfaceVariant = softSurface,
            inverseSurface = Color(0xFFD7DAE0),
            inverseOnSurface = Color(0xFF282C34),
            inversePrimary = Color(0xFF285E89),
            error = Color(0xFFE6989D),
            onError = Color(0xFF3A181C),
            errorContainer = Color(0xFF512B31),
            onErrorContainer = Color(0xFFFFDADC),
            surfaceTint = Color.Transparent,
            outline = Color(0xFF687180),
            outlineVariant = divider,
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = LightLeaf,
            secondaryContainer = LightSelected,
            onSecondaryContainer = LightInk,
            onPrimary = Color.White,
            background = Color(0xFFF8F8F4),
            surface = Color(0xFFF8F8F4),
            onSurface = LightInk,
            onSurfaceVariant = LightMuted,
            surfaceContainer = Color(0xFFF0F2EC),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF4F5EF),
            surfaceContainerHigh = Color(0xFFE9ECE5),
            surfaceContainerHighest = Color(0xFFE2E6DD),
            outlineVariant = LightLine,
        )
    }

    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = when {
                    page == "editor" -> "editor"
                    page != "home" -> page
                    searchOpen -> "search"
                    tab == 1 -> "review"
                    else -> "home"
                },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "page",
            ) { target ->
                when (target) {
                    "editor" -> DiaryEditor(
                        id = editingId,
                        viewModel = diaryVm,
                        onBack = { page = "home" },
                        bodySize = preferences.bodySize,
                        onComplete = { savedId -> editingId = savedId; page = if (savedId == null) "home" else "detail" },
                    )
                    "detail" -> (entries.firstOrNull { it.id == editingId } ?: selectedDiary?.takeIf { it.id == editingId })?.let { diary ->
                        DiaryDetail(
                            diary,
                            onBack = { back() },
                            onEdit = { page = "editor" },
                            onUpdate = { diaryVm.save(it) },
                            onTrash = { diaryVm.save(diary.copy(deletedAt = System.currentTimeMillis())) },
                            viewModel = diaryVm,
                            bodySize = preferences.bodySize,
                        )
                    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("正在读取日记…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    "drafts" -> DraftsPage(drafts, onBack = { page = "home" }, onOpen = { editingId = it; page = "editor" }, onTrash = trashEntry)
                    "search" -> searchStateHolder.SaveableStateProvider("search") {
                        SearchPage(entries, onBack = { searchOpen = false }, onOpen = { editingId = it; page = "detail" }, onUpdateEntry = updateEntry, onTrashEntry = trashEntry)
                    }
                    "review" -> ReviewPage(
                        entries = entries,
                        trash = trash,
                        onTab = { tab = it },
                        onOpen = { editingId = it; page = "detail" },
                        onRestore = { diaryVm.save(it.copy(deletedAt = null)) },
                        onDeletePermanently = { diaryVm.deletePermanently(it) },
                        onClearTrash = { diaryVm.clearTrash() },
                        onUpdateEntry = updateEntry,
                        onTrashEntry = trashEntry,
                    )
                    "settings" -> SettingsPage(
                        onBack = { page = "home" },
                        onBackup = { backupLauncher.launch("Leafolio-Backup-${LocalDate.now()}.zip") },
                        onRestore = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                        onExport = { page = "export" },
                        onManageTags = { page = "tags" },
                        diaryCount = diaryCount,
                        storageBytes = storageBytes,
                        privacyPreferences = preferences,
                        onPrivacyChange = { delay, hide, daily ->
                            scope.launch {
                                if (delay != null) preferencesStore.setLockDelay(delay)
                                if (hide != null) preferencesStore.setHidePreview(hide)
                                if (daily != null) preferencesStore.setDailyReminder(daily)
                            }
                            if (daily != null && daily >= 0 && android.os.Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        },
                        theme = preferences.theme,
                        bodySize = preferences.bodySize,
                        keepOriginalPhotos = preferences.keepOriginalPhotos,
                        lastBackupAt = preferences.lastBackupAt,
                        backupReminderDays = preferences.backupReminderDays,
                        notificationsAllowed = notificationsAllowed,
                        onBackupReminderChange = { days ->
                            scope.launch { preferencesStore.setBackupReminderDays(days) }
                            if (days > 0 && android.os.Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onThemeChange = { scope.launch { preferencesStore.setTheme(it) } },
                        onBodySizeChange = { scope.launch { preferencesStore.setBodySize(it) } },
                        onPhotoQualityChange = { scope.launch { preferencesStore.setKeepOriginalPhotos(it) } },
                        onClearAll = {
                            scope.launch {
                                runCatching { diaryVm.clearAllData() }
                                    .onSuccess { Toast.makeText(context, "所有日记、照片和标签已清除", Toast.LENGTH_LONG).show() }
                                    .onFailure { Toast.makeText(context, "清理失败：${it.message}", Toast.LENGTH_LONG).show() }
                            }
                        },
                    )
                    "export" -> ExportPage(entries, onBack = { page = "settings" }, onExport = { selection ->
                        exportMode = selection.mode
                        exportStart = selection.start?.toString()
                        exportEnd = selection.end?.toString()
                        exportIds = ArrayList(selection.selectedIds)
                        exportLauncher.launch("Leafolio-Export-${LocalDate.now()}.zip")
                    })
                    "tags" -> TagManagerPage(
                        tags = diaryVm.tags.collectAsState().value,
                        onBack = { page = "settings" },
                        onCreate = { name -> scope.launch { diaryVm.createTag(name) } },
                        onRename = { tag, name -> scope.launch { diaryVm.renameTag(tag.id, name) } },
                        onDelete = { tag -> scope.launch { diaryVm.deleteTag(tag.id) } },
                    )
                    else -> HomePage(
                        entries = entries,
                        draft = draft,
                        onUpdateEntry = updateEntry,
                        onTrashEntry = trashEntry,
                        onLoadMore = { diaryVm.loadMore() },
                        hasMore = entries.size < entryCount,
                        onDrafts = { page = "drafts" },
                        selectedTab = tab,
                        onTab = { tab = it },
                        onNew = { editingId = null; page = "editor" },
                        onOpen = { editingId = it; page = if (it == draft?.id) "editor" else "detail" },
                        onSearch = { searchOpen = true },
                        onSettings = { page = "settings" },
                    )
                }
            }
            transfer.preview?.let { preview ->
                AlertDialog(
                    onDismissRequest = transferVm::dismissPreview,
                    title = { Text("备份检查完成") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("包含 ${preview.diaries} 篇日记、${preview.tags} 个标签和 ${preview.photos} 张照片。")
                            Text("预计需要 ${storageLabel(preview.requiredBytes)}；设备可用空间 ${storageLabel(preview.availableBytes)}。")
                            if (!preview.hasSpace) Text("可用空间不足，请释放空间后再恢复。", color = MaterialTheme.colorScheme.error)
                            Text("恢复将合并到此设备，不会覆盖内容不同的现有日记。")
                            Text(if (preview.hasSettings) "同时恢复主题、字号、照片质量和备份提醒偏好；设备授权和最近备份时间不迁移。" else "这份旧备份不包含设置，保留此设备的设置。")
                        }
                    },
                    confirmButton = {
                        TextButton(enabled = preview.hasSpace, onClick = {
                            transferVm.restore()
                        }) { Text("开始恢复") }
                    },
                    dismissButton = { TextButton(onClick = transferVm::dismissPreview) { Text("取消") } },
                )
            }
            if (transfer.busy) AlertDialog(
                onDismissRequest = {},
                title = { Text(transfer.progress.label) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val progress = transfer.progress
                        if (progress.total > 0) {
                            androidx.compose.material3.LinearProgressIndicator(progress = { (progress.completed.toFloat() / progress.total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                            Text("${(progress.completed * 100 / progress.total).coerceIn(0, 100)}%")
                        } else androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("请保持应用打开，文件保存完成后会显示结果。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {},
            )
            transfer.result?.takeIf(String::isNotBlank)?.let { result ->
                AlertDialog(onDismissRequest = transferVm::dismissResult, title = { Text("操作结果") }, text = { Text(result) },
                    confirmButton = { TextButton(onClick = transferVm::dismissResult) { Text("知道了") } })
            }
            }
        }
    }
}

@Composable
private fun HomePage(
    entries: List<Diary>,
    draft: Diary?,
    onUpdateEntry: (Diary) -> Unit,
    onTrashEntry: (Diary) -> Unit,
    onLoadMore: () -> Unit = {},
    hasMore: Boolean = false,
    onDrafts: () -> Unit = {},
    selectedTab: Int,
    onTab: (Int) -> Unit,
    onNew: () -> Unit,
    onOpen: (String) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNew,
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.EditNote, contentDescription = "写日记", modifier = Modifier.size(25.dp))
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { onTab(0) },
                    icon = { Icon(Icons.AutoMirrored.Outlined.EventNote, contentDescription = null) },
                    label = { Text("日记") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, indicatorColor = MaterialTheme.colorScheme.secondaryContainer),
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { onTab(1) },
                    icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                    label = { Text("回看") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, indicatorColor = MaterialTheme.colorScheme.secondaryContainer),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Leafolio", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text("一页", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "搜索", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "设置", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            if (entries.isEmpty() && draft == null) {
                EmptyHome(Modifier.weight(1f), onNew)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 24.dp, end = 24.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    item {
                        Text(todayLabel(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("留住今天的片刻", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp, bottom = 22.dp))
                    }
                    if (draft != null) item {
                        DraftRow(draft, onClick = onDrafts)
                        Spacer(Modifier.height(28.dp))
                    }
                    if (entries.isNotEmpty()) {
                        items(entries.size, key = { entries[it].id }) { index ->
                            val entry = entries[index]
                            val previous = entries.getOrNull(index - 1)
                            if (index == 0 || previous?.isPinned != entry.isPinned || (!entry.isPinned && monthLabel(previous!!.entryDate) != monthLabel(entry.entryDate))) {
                                Text(if (entry.isPinned) "置顶" else monthLabel(entry.entryDate), modifier = Modifier.padding(top = 12.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            EntryRow(entry, { onOpen(entry.id) }, onUpdateEntry, onTrashEntry)
                        }
                        if (hasMore) item {
                            LaunchedEffect(entries.size) { onLoadMore() }
                            Text("正在加载更多…", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else item {
                        Text("你的第一页，正在等你写下。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHome(modifier: Modifier, onNew: () -> Unit) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 36.dp).clickable(onClick = onNew),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.EditNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
        Text("写下第一页", modifier = Modifier.padding(top = 22.dp), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Text("记录此刻的想法，之后再慢慢回看。", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("开始记录", modifier = Modifier.padding(top = 24.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary).padding(horizontal = 22.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DraftRow(draft: Diary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainer).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("未完成的草稿", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(draft.title.ifBlank { draft.content.ifBlank { "新日记" } }, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.onSurface)
        }
        Icon(Icons.Outlined.ChevronLeft, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EntryRow(
    entry: Diary,
    onClick: () -> Unit,
    onUpdate: (Diary) -> Unit = {},
    onTrash: (Diary) -> Unit = {},
) {
    var menuOpen by remember(entry.id) { mutableStateOf(false) }
    var confirmTrash by remember(entry.id) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(50.dp)) {
            Text(dayNumber(entry.entryDate), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            Text(weekday(entry.entryDate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            if (entry.title.isNotBlank()) Text(entry.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(entry.content.ifBlank { entry.title }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            entry.mood?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp)) }
        }
        val rowContext = LocalContext.current
        val previewFlow = remember(entry.id) { LeafolioDatabase.get(rowContext).photoDao().observeForDiary(entry.id) }
        val rowPhotos by previewFlow.collectAsState(initial = emptyList())
        rowPhotos.firstOrNull()?.let { photo ->
            AsyncImage(model = File(rowContext.filesDir, photo.relativePath), contentDescription = "日记照片", contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.padding(start = 8.dp).size(48.dp).clip(RoundedCornerShape(8.dp)))
        }
        if (entry.isFavorite) Icon(Icons.Outlined.Favorite, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp).size(18.dp))
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.MoreVert, "更多日记操作", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (entry.isFavorite) "取消收藏" else "收藏") },
                    leadingIcon = { Icon(if (entry.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, null) },
                    onClick = { menuOpen = false; onUpdate(entry.copy(isFavorite = !entry.isFavorite)) },
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (entry.isPinned) "取消置顶" else "置顶") },
                    leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
                    onClick = { menuOpen = false; onUpdate(entry.copy(isPinned = !entry.isPinned)) },
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("移到回收站") },
                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                    onClick = { menuOpen = false; confirmTrash = true },
                )
            }
        }
    }
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
    if (confirmTrash) AlertDialog(
        onDismissRequest = { confirmTrash = false },
        title = { Text("移到回收站？") },
        text = { Text("这篇日记会保留 30 天，你可以从回收站恢复。") },
        confirmButton = { TextButton(onClick = { onTrash(entry); confirmTrash = false }) { Text("移到回收站") } },
        dismissButton = { TextButton(onClick = { confirmTrash = false }) { Text("取消") } },
    )
}

@Composable
private fun ReviewPage(
    entries: List<Diary>,
    trash: List<Diary>,
    onTab: (Int) -> Unit,
    onOpen: (String) -> Unit,
    onRestore: suspend (Diary) -> Unit,
    onDeletePermanently: suspend (String) -> Unit,
    onClearTrash: suspend () -> Unit,
    onUpdateEntry: (Diary) -> Unit,
    onTrashEntry: (Diary) -> Unit,
) {
    var section by rememberSaveable { mutableStateOf("calendar") }
    var showTrash by rememberSaveable { mutableStateOf(false) }
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedDateKey by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var selectedCategory by rememberSaveable { mutableStateOf("收藏") }
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    var confirmClearTrash by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val month = YearMonth.parse(monthKey)
    val selectedDate = LocalDate.parse(selectedDateKey)
    val dateEntries = remember(entries, selectedDate) { entries.filter {
        Instant.ofEpochMilli(it.entryDate).atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
    } }
    val tags = remember(entries) { entries.flatMap { it.tags.split('|') }.filter(String::isNotBlank).distinct().sorted() }
    val categories = listOf("收藏") + tags
    val categoryEntries = when (selectedCategory) {
        "收藏" -> entries.filter { it.isFavorite }
        else -> entries.filter { selectedCategory in it.tags.split('|') }
    }
    val listEntries = when {
        showTrash -> trash
        section == "calendar" -> dateEntries
        else -> categoryEntries
    }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = { onTab(0) },
                    icon = { Icon(Icons.AutoMirrored.Outlined.EventNote, contentDescription = null) },
                    label = { Text("日记") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, indicatorColor = MaterialTheme.colorScheme.secondaryContainer),
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { onTab(1) },
                    icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                    label = { Text("回看") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, indicatorColor = MaterialTheme.colorScheme.secondaryContainer),
                )
            }
        },
    ) { scaffoldPadding ->
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .padding(scaffoldPadding)
        .padding(horizontal = 24.dp)) {
        Text("回看", modifier = Modifier.padding(top = 22.dp), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Text("那些写下来的日子", modifier = Modifier.padding(top = 5.dp, bottom = 12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { section = "calendar"; showTrash = false }) { Text("日历", color = if (!showTrash && section == "calendar") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick = { section = "categories"; showTrash = false }) { Text("分类", color = if (!showTrash && section == "categories") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { showTrash = !showTrash }) { Text("回收站${if (trash.isNotEmpty()) " ${trash.size}" else ""}", color = if (showTrash) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)) {
        item {
        if (!showTrash && section == "calendar") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthKey = month.minusMonths(1).toString() }) { Icon(Icons.Outlined.ChevronLeft, "上个月", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(month.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.SIMPLIFIED_CHINESE)), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { monthKey = month.plusMonths(1).toString() }) { Icon(Icons.Outlined.ChevronRight, "下个月", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { Text(it, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            }
            val offset = month.atDay(1).dayOfWeek.value % 7
            val dates = (0 until 42).map { index -> index - offset + 1 }
            val markedDates = entries.map { Instant.ofEpochMilli(it.entryDate).atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()
            LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxWidth().height(240.dp), userScrollEnabled = false) {
                gridItems(dates) { day ->
                    if (day in 1..month.lengthOfMonth()) {
                        val date = month.atDay(day)
                        val selected = date == selectedDate
                        Column(
                            Modifier.fillMaxWidth().height(40.dp).clip(CircleShape)
                                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                .clickable { selectedDateKey = date.toString() },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(day.toString(), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                            Box(Modifier.size(4.dp).clip(CircleShape).background(if (date in markedDates) MaterialTheme.colorScheme.primary else Color.Transparent))
                        }
                    } else Spacer(Modifier.height(40.dp))
                }
            }
        } else if (!showTrash) {
            CategoryRow(categories, selectedCategory) { selectedCategory = it }
        }
        }
        item {
        Row(Modifier.padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (showTrash) Icons.Outlined.DeleteOutline else if (section == "calendar") Icons.Outlined.CalendarMonth else Icons.Outlined.FavoriteBorder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            val heading = when {
                showTrash -> "回收站"
                section == "calendar" -> selectedDate.format(DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.SIMPLIFIED_CHINESE))
                else -> selectedCategory
            }
            Text(" $heading", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text("${listEntries.size} 篇", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            if (showTrash) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { confirmClearTrash = true }, enabled = trash.isNotEmpty() && !deleting) {
                    Text("清空回收站")
                }
            }
        }
        }
        if (listEntries.isEmpty()) {
            item { Text(when { showTrash -> "回收站是空的。"; section == "calendar" -> "这一天还没有日记。"; else -> "这里还没有日记。" }, modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        } else {
                items(listEntries, key = { it.id }) { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { EntryRow(entry, { if (!showTrash) onOpen(entry.id) }, onUpdateEntry, onTrashEntry) }
                        if (showTrash) {
                            IconButton(onClick = { scope.launch { onRestore(entry) } }, enabled = !deleting) { Icon(Icons.Outlined.Restore, "恢复", tint = MaterialTheme.colorScheme.primary) }
                            IconButton(onClick = { confirmDelete = entry.id }, enabled = !deleting) { Icon(Icons.Outlined.DeleteOutline, "永久删除", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
    confirmDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { if (!deleting) confirmDelete = null },
            title = { Text("永久删除这篇日记？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = { TextButton(enabled = !deleting, onClick = {
                deleting = true
                scope.launch {
                    runCatching { onDeletePermanently(id) }
                        .onFailure { Toast.makeText(context, it.message ?: "删除失败，请重试", Toast.LENGTH_LONG).show() }
                    deleting = false
                    confirmDelete = null
                }
            }) { Text(if (deleting) "删除中…" else "永久删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(enabled = !deleting, onClick = { confirmDelete = null }) { Text("取消") } },
        )
    }
    if (confirmClearTrash) AlertDialog(
        onDismissRequest = { if (!deleting) confirmClearTrash = false },
        title = { Text("清空回收站？") },
        text = { Text("将永久删除回收站中的 ${trash.size} 篇日记及其不再使用的照片，无法恢复。") },
        confirmButton = { TextButton(enabled = !deleting, onClick = {
            deleting = true
            scope.launch {
                runCatching { onClearTrash() }
                    .onSuccess { Toast.makeText(context, "回收站已清空", Toast.LENGTH_SHORT).show() }
                    .onFailure { Toast.makeText(context, it.message ?: "清空失败，请重试", Toast.LENGTH_LONG).show() }
                deleting = false
                confirmClearTrash = false
            }
        }) { Text(if (deleting) "清理中…" else "永久清空", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(enabled = !deleting, onClick = { confirmClearTrash = false }) { Text("取消") } },
    )
    }
}

@Composable
private fun CategoryRow(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        categories.forEach { name ->
            TextButton(onClick = { onSelect(name) }) {
                Text(name, color = if (name == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SearchPage(entries: List<Diary>, onBack: () -> Unit, onOpen: (String) -> Unit, onUpdateEntry: (Diary) -> Unit, onTrashEntry: (Diary) -> Unit) {
    DiarySearchPage(entries = entries, moods = Moods, onBack = onBack) { entry ->
        EntryRow(entry, { onOpen(entry.id) }, onUpdateEntry, onTrashEntry)
    }
}

@Composable
private fun DiaryDetail(
    diary: Diary,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onUpdate: suspend (Diary) -> Unit,
    onTrash: suspend () -> Unit,
    viewModel: DiaryViewModel,
    bodySize: String,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val photos by viewModel.photos(diary.id).collectAsState(initial = emptyList())
    var menuOpen by remember { mutableStateOf(false) }
    var confirmTrash by remember { mutableStateOf(false) }
    var shareOpen by remember { mutableStateOf(false) }
    var preparingShare by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Outlined.ChevronLeft, "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Outlined.MoreVert, "更多操作", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(if (diary.isFavorite) "取消收藏" else "收藏") },
                        leadingIcon = { Icon(if (diary.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, null) },
                        onClick = { menuOpen = false; scope.launch { onUpdate(diary.copy(isFavorite = !diary.isFavorite)) } },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(if (diary.isPinned) "取消置顶" else "置顶") },
                        leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
                        onClick = { menuOpen = false; scope.launch { onUpdate(diary.copy(isPinned = !diary.isPinned)) } },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("分享") },
                        leadingIcon = { Icon(Icons.Outlined.Share, null) },
                        onClick = { menuOpen = false; shareOpen = true },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("移到回收站") },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                        onClick = { menuOpen = false; confirmTrash = true },
                    )
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Outlined.EditNote, "编辑", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Text(
            Instant.ofEpochMilli(diary.entryDate).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy年M月d日 · EEEE", Locale.SIMPLIFIED_CHINESE)),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (diary.mood != null || diary.tags.isNotBlank()) {
            Text(listOfNotNull(diary.mood, diary.tags.takeIf { it.isNotBlank() }?.replace("|", " · ")).joinToString("   "), modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        if (diary.title.isNotBlank()) {
            Text(diary.title, modifier = Modifier.padding(top = 18.dp), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        }
        val bodyFontSize = when (bodySize) { "small" -> 16.sp; "large" -> 22.sp; else -> 18.sp }
        Text(diary.content, modifier = Modifier.padding(top = if (diary.title.isBlank()) 24.dp else 16.dp), style = MaterialTheme.typography.bodyLarge.copy(fontSize = bodyFontSize, lineHeight = (bodyFontSize.value * 1.7f).sp), color = MaterialTheme.colorScheme.onSurface)
        if (photos.isNotEmpty()) {
            PhotoGallery(photos, Modifier.padding(top = 20.dp).fillMaxWidth())
        }
        }
    }
    if (confirmTrash) AlertDialog(
        onDismissRequest = { confirmTrash = false },
        title = { Text("移到回收站？") },
        text = { Text("这篇日记会保留 30 天，你可以从回收站恢复。") },
        confirmButton = { TextButton(onClick = { scope.launch { onTrash(); confirmTrash = false; onBack() } }) { Text("移到回收站") } },
        dismissButton = { TextButton(onClick = { confirmTrash = false }) { Text("取消") } },
    )
    if (shareOpen) AlertDialog(
        onDismissRequest = { if (!preparingShare) shareOpen = false },
        title = { Text("分享日记") },
        text = {
            Column {
                Text("分享文字或照片，不附带心情和标签。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(enabled = !preparingShare && (diary.title.isNotBlank() || diary.content.isNotBlank()), onClick = {
                    runCatching { context.startActivity(android.content.Intent.createChooser(DiarySharing.textIntent(diary), "分享文字")) }
                        .onSuccess { shareOpen = false }
                        .onFailure { Toast.makeText(context, "无法打开系统分享面板", Toast.LENGTH_LONG).show() }
                }) { Text("分享文字") }
                TextButton(enabled = !preparingShare && photos.isNotEmpty(), onClick = {
                    preparingShare = true
                    scope.launch {
                        runCatching {
                            val intent = DiarySharing.photosIntent(context, photos)
                            context.startActivity(android.content.Intent.createChooser(intent, "分享照片"))
                        }.onSuccess { shareOpen = false }
                            .onFailure { Toast.makeText(context, it.message ?: "照片分享失败", Toast.LENGTH_LONG).show() }
                        preparingShare = false
                    }
                }) { Text(if (preparingShare) "正在准备照片…" else "分享照片（${photos.size} 张）") }
            }
        },
        confirmButton = { TextButton(enabled = !preparingShare, onClick = { shareOpen = false }) { Text("取消") } },
    )
}

@Composable
private fun PhotoGallery(photos: List<PhotoAttachment>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<PhotoAttachment?>(null) }
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(photos, key = { it.id }) { photo ->
            AsyncImage(
                model = File(context.filesDir, photo.relativePath),
                contentDescription = "日记照片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(112.dp).clip(RoundedCornerShape(8.dp)).clickable { selected = photo },
            )
        }
    }
    selected?.let { photo ->
        var scale by remember(photo.id) { mutableStateOf(1f) }
        Dialog(onDismissRequest = { selected = null }) {
            Box(Modifier.fillMaxSize().clickable { selected = null }, contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = File(context.filesDir, photo.relativePath),
                    contentDescription = "全屏日记照片",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().pointerInput(photo.id) {
                        detectTransformGestures { _, _, zoom, _ -> scale = (scale * zoom).coerceIn(1f, 5f) }
                    }.graphicsLayer(scaleX = scale, scaleY = scale),
                )
                IconButton(onClick = { selected = null }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(Icons.Outlined.Close, "关闭照片", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(
    onBack: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExport: () -> Unit,
    onManageTags: () -> Unit,
    diaryCount: Int,
    storageBytes: Long,
    privacyPreferences: app.leafolio.data.UserPreferences,
    onPrivacyChange: (Int?, Boolean?, Int?) -> Unit,
    theme: String,
    bodySize: String,
    keepOriginalPhotos: Boolean,
    lastBackupAt: Long,
    backupReminderDays: Int,
    notificationsAllowed: Boolean,
    onBackupReminderChange: (Int) -> Unit,
    onThemeChange: (String) -> Unit,
    onBodySizeChange: (String) -> Unit,
    onPhotoQualityChange: (Boolean) -> Unit,
    onClearAll: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ChevronLeft, "返回", tint = MaterialTheme.colorScheme.onSurface) }
            Text("设置", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("外观", modifier = Modifier.padding(top = 22.dp, bottom = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            PreferenceChoices("主题", theme, listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色"), onThemeChange)
            PreferenceChoices("正文字号", bodySize, listOf("small" to "小", "medium" to "标准", "large" to "大"), onBodySizeChange)
            Text("记录", modifier = Modifier.padding(top = 22.dp, bottom = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            PreferenceChoices("照片质量", if (keepOriginalPhotos) "original" else "compressed", listOf("compressed" to "压缩（推荐）", "original" to "保留原图")) { onPhotoQualityChange(it == "original") }
            Text(if (keepOriginalPhotos) "原图占用更多空间，保留源文件及其位置信息等元数据。仅影响之后导入的照片。" else "压缩后导入，移除位置信息等元数据。仅影响之后导入的照片。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            PrivacyPreferences(privacyPreferences, onPrivacyChange)
            Text("数据管理", modifier = Modifier.padding(top = 22.dp, bottom = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            SettingsAction(Icons.Outlined.Backup, "创建完整备份", "日记、草稿、回收站、照片与可迁移设置", onBackup)
            Text(if (lastBackupAt > 0) "最近成功备份：${Instant.ofEpochMilli(lastBackupAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.SIMPLIFIED_CHINESE))}" else "尚未创建完整备份", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
            SettingsAction(Icons.Outlined.RestorePage, "从备份恢复", "检查备份后合并到此设备", onRestore)
            SettingsAction(Icons.Outlined.FileDownload, "导出通用文件", "Markdown、照片与 JSON 元数据", onExport)
            PreferenceChoices("备份提醒", backupReminderDays.toString(), listOf("0" to "关闭", "7" to "每周", "30" to "每月")) { onBackupReminderChange(it.toInt()) }
            Text("提醒后手动备份，不会自动上传。系统省电可能延迟通知。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            if (backupReminderDays > 0 && !notificationsAllowed) {
                Text("系统通知未开启，提醒暂时无法送达。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                val settingsContext = LocalContext.current
                TextButton(onClick = { settingsContext.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, settingsContext.packageName)) }) { Text("打开通知设置") }
            }
            SettingsAction(Icons.Outlined.LocalOffer, "管理标签", "创建、重命名、合并或删除标签", onManageTags)
            Text("本机数据", modifier = Modifier.padding(top = 22.dp, bottom = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text("${diaryCount} 篇记录 · 本机占用 ${storageLabel(storageBytes)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { confirmClear = true }, modifier = Modifier.padding(top = 8.dp)) { Text("清空全部数据", color = MaterialTheme.colorScheme.error) }
            Text("隐私", modifier = Modifier.padding(top = 22.dp, bottom = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text("备份和导出文件不加密，请保存到你信任的位置。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Text("一页 · Leafolio", modifier = Modifier.padding(top = 28.dp), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
            Text("版本 0.1.0", modifier = Modifier.padding(top = 5.dp, bottom = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("永久清空全部数据？") },
        text = { Text("将永久删除 ${diaryCount} 篇日记、照片和标签，且无法从此设备恢复。建议先创建完整备份。") },
        confirmButton = {
            TextButton(onClick = { confirmClear = false; onClearAll() }) { Text("永久清空", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
    )
}

@Composable
private fun PreferenceChoices(
    title: String,
    selected: String,
    choices: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    Text(title, modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { (value, label) ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}

@Composable
private fun TagManagerPage(
    tags: List<Tag>,
    onBack: () -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Tag, String) -> Unit,
    onDelete: (Tag) -> Unit,
) {
    var editing by remember { mutableStateOf<Tag?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Tag?>(null) }
    var draftName by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ChevronLeft, "返回", tint = MaterialTheme.colorScheme.onSurface) }
            Text("标签", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { draftName = ""; creating = true }) { Icon(Icons.Outlined.Add, "新建标签", tint = MaterialTheme.colorScheme.primary) }
        }
        if (tags.isEmpty()) {
            Text("还没有标签。", modifier = Modifier.padding(top = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(tags, key = { it.id }) { tag ->
                    var expanded by remember(tag.id) { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocalOffer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(tag.name, modifier = Modifier.weight(1f).padding(start = 14.dp), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                        Box {
                            IconButton(onClick = { expanded = true }) { Icon(Icons.Outlined.MoreVert, "标签操作", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                androidx.compose.material3.DropdownMenuItem(text = { Text("重命名或合并") }, onClick = { expanded = false; draftName = tag.name; editing = tag })
                                androidx.compose.material3.DropdownMenuItem(text = { Text("删除标签") }, onClick = { expanded = false; deleting = tag })
                            }
                        }
                    }
                    Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                }
            }
        }
    }
    val current = editing
    if (creating || current != null) AlertDialog(
        onDismissRequest = { creating = false; editing = null },
        title = { Text(if (creating) "新建标签" else "重命名标签") },
        text = {
            BasicTextField(
                value = draftName,
                onValueChange = { draftName = it },
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp)).padding(14.dp)) {
                        if (draftName.isEmpty()) Text("标签名称", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        inner()
                    }
                },
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (draftName.isNotBlank()) {
                    if (creating) onCreate(draftName.trim()) else current?.let { onRename(it, draftName.trim()) }
                    creating = false
                    editing = null
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = { creating = false; editing = null }) { Text("取消") } },
    )
    deleting?.let { tag ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除“${tag.name}”？") },
            text = { Text("只会移除日记与此标签的关联，不会删除日记。") },
            confirmButton = { TextButton(onClick = { onDelete(tag); deleting = null }) { Text("删除标签") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun SettingsAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun DiaryEditor(id: String?, viewModel: DiaryViewModel, onBack: () -> Unit, bodySize: String, onComplete: (String?) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var titleInput by rememberSaveable(id, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var contentInput by rememberSaveable(id, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val title = titleInput.text
    val content = contentInput.text
    val editorBodySize = when (bodySize) { "small" -> 16.sp; "large" -> 21.sp; else -> 18.sp }
    val textHistory = remember(id) { TextEditHistory() }
    var historyRevision by remember(id) { mutableStateOf(0) }
    val canUndo = remember(historyRevision) { textHistory.canUndo }
    val canRedo = remember(historyRevision) { textHistory.canRedo }
    fun updateText(next: EditorText, field: String) {
        textHistory.record(EditorText(title, content), next, field, android.os.SystemClock.elapsedRealtime())
        historyRevision++
    }
    var mood by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var tags by rememberSaveable(id) { mutableStateOf("") }
    var showMoods by rememberSaveable { mutableStateOf(false) }
    var noteId by rememberSaveable(id) { mutableStateOf(id ?: UUID.randomUUID().toString()) }
    var entryDate by rememberSaveable(id) { mutableStateOf(LocalDate.now()) }
    var createdAt by rememberSaveable(id) { mutableStateOf(System.currentTimeMillis()) }
    var original by remember(id) { mutableStateOf<Diary?>(null) }
    var saving by remember { mutableStateOf(false) }
    var loaded by remember(id) { mutableStateOf(id == null) }
    var initialized by remember(id) { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    var photoError by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var draggingPhotoId by remember(id) { mutableStateOf<String?>(null) }
    var photoDragOffset by remember(id) { mutableStateOf(Offset.Zero) }
    var photoDragPoint by remember(id) { mutableStateOf<Offset?>(null) }
    val photoBounds = remember(id) { mutableStateMapOf<String, Rect>() }
    val editorView = LocalView.current
    var editorHydrated by rememberSaveable(id) { mutableStateOf(false) }
    val photos by viewModel.photos(noteId).collectAsState(initial = emptyList())
    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9),
    ) { selectedUris ->
        if (selectedUris.isNotEmpty()) scope.launch {
                val now = System.currentTimeMillis()
                val draft = original ?: Diary(
                    id = noteId,
                    title = title,
                    content = content,
                    entryDate = entryDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    createdAt = createdAt,
                    updatedAt = now,
                    isDraft = true,
                    mood = mood,
                    tags = tags.split(',').map(String::trim).filter(String::isNotBlank).distinct().joinToString("|"),
                )
            selectedUris.take((9 - photos.size).coerceAtLeast(0)).forEach { uri ->
                runCatching { viewModel.importPhoto(draft, uri) }
                    .onSuccess { photoError = null; original = viewModel.get(noteId) }
                    .onFailure { photoError = it.message ?: "无法读取这张照片" }
            }
        }
    }

    LaunchedEffect(id) {
        viewModel.get(noteId)?.let {
                original = it
                if (!editorHydrated) {
                titleInput = TextFieldValue(it.title, TextRange(it.title.length))
                contentInput = TextFieldValue(it.content, TextRange(it.content.length))
                mood = it.mood
                tags = it.tags.split('|').filter(String::isNotBlank).joinToString(", ")
                entryDate = Instant.ofEpochMilli(it.entryDate).atZone(ZoneId.systemDefault()).toLocalDate()
                createdAt = it.createdAt
                }
        }
        editorHydrated = true
        loaded = true
        delay(180)
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }

    suspend fun persist(isDraft: Boolean): Boolean {
        if (title.isBlank() && content.isBlank() && photos.isEmpty()) return true
        val now = System.currentTimeMillis()
        saving = true
        saveError = false
        val updated = (original ?: Diary(
            id = noteId,
            title = "",
            content = "",
            entryDate = entryDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            createdAt = createdAt,
            updatedAt = now,
            isDraft = isDraft,
        )).copy(
                title = title,
                content = content,
                entryDate = entryDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                updatedAt = now,
                isDraft = isDraft,
                mood = mood,
                tags = tags.split(',').map(String::trim).filter(String::isNotBlank).distinct().joinToString("|"),
            )
        val result = runCatching { viewModel.save(updated) }
        result.exceptionOrNull()?.let { if (it is kotlinx.coroutines.CancellationException) throw it }
        result.onSuccess { original = updated }.onFailure { saveError = true }
        saving = false
        initialized = result.isSuccess
        return result.isSuccess
    }

    LaunchedEffect(title, content, entryDate, mood, tags, photos.size, loaded) {
        if (!loaded || (title.isBlank() && content.isBlank() && photos.isEmpty())) return@LaunchedEffect
        delay(450)
        persist(isDraft = original?.isDraft ?: true)
    }

    suspend fun saveBeforeExit(): Boolean {
        if (title.isNotBlank() || content.isNotBlank() || photos.isNotEmpty()) {
            return persist(isDraft = original?.isDraft ?: true)
        }
        if (original?.isDraft == false) {
            Toast.makeText(context, "内容为空，请保留文字或照片后再完成。", Toast.LENGTH_LONG).show()
            return false
        }
        if (original?.isDraft == true) {
            viewModel.deleteEmptyDraft(noteId)
            original = viewModel.get(noteId)
        }
        return true
    }

    val saveOnPause by androidx.compose.runtime.rememberUpdatedState<suspend () -> Unit>({
        if (loaded) saveBeforeExit()
    })
    val editorLifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(editorLifecycle, noteId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) viewModel.viewModelScope.launch { saveOnPause() }
        }
        editorLifecycle.lifecycle.addObserver(observer)
        onDispose { editorLifecycle.lifecycle.removeObserver(observer) }
    }
    BackHandler {
        keyboard?.hide()
        scope.launch {
            if (saveBeforeExit()) onBack()
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(horizontal = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                keyboard?.hide()
                scope.launch { if (saveBeforeExit()) onBack() }
            }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Outlined.ChevronLeft, "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                entryDate.format(DateTimeFormatter.ofPattern("M月d日", Locale.SIMPLIFIED_CHINESE)),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(if (saveError) "保存失败" else if (saving) "保存中" else if (original?.title == title && original?.content == content && initialized) "已保存" else "待保存", color = if (saveError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            if (saveError) TextButton(onClick = { scope.launch { persist(isDraft = original?.isDraft ?: true) } }) { Text("重试") }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                keyboard?.hide()
                scope.launch {
                    val shouldSave = title.isNotBlank() || content.isNotBlank() || photos.isNotEmpty()
                    if (if (shouldSave) persist(isDraft = false) else saveBeforeExit()) onComplete(if (shouldSave) noteId else null)
                }
            }, modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Outlined.Check, "完成", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(21.dp))
            }
        }

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
        val context = LocalContext.current
        Text(
            entryDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日 · EEEE", Locale.SIMPLIFIED_CHINESE)),
            modifier = Modifier.clickable {
                android.app.DatePickerDialog(context, { _, year, month, day -> entryDate = LocalDate.of(year, month + 1, day) }, entryDate.year, entryDate.monthValue - 1, entryDate.dayOfMonth).show()
            },
            color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(22.dp))
        BasicTextField(
            value = titleInput,
            onValueChange = { updateText(EditorText(it.text, content), "title"); titleInput = it },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            decorationBox = { inner -> if (title.isEmpty()) Text("标题（可选）", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); inner() },
        )
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().heightIn(min = 160.dp)) {
            BasicTextField(
                value = contentInput,
                onValueChange = { updateText(EditorText(title, it.text), "content"); contentInput = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).focusRequester(focusRequester),
                textStyle = TextStyle(fontSize = editorBodySize, lineHeight = editorBodySize * 1.6f, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
                decorationBox = { inner ->
                    if (content.isEmpty()) Text("从这里开始写……", style = TextStyle(fontSize = editorBodySize, lineHeight = editorBodySize * 1.6f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                },
            )
        }
        val photoCellCount = photos.size + if (photos.size < 9) 1 else 0
        val photoRows = (photoCellCount + 2) / 3
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cellSize = (maxWidth - 16.dp) / 3
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(cellSize * photoRows + 8.dp * (photoRows - 1).coerceAtLeast(0)),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false,
            ) {
                gridItems(photos, key = { it.id }) { photo ->
                    val isDragging = draggingPhotoId == photo.id
                    Box(
                        Modifier.animateItem()
                            .zIndex(if (isDragging) 1f else 0f)
                            .offset { if (isDragging) IntOffset(photoDragOffset.x.roundToInt(), photoDragOffset.y.roundToInt()) else IntOffset.Zero }
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .onGloballyPositioned { photoBounds[photo.id] = it.boundsInRoot() }
                            .shadow(if (isDragging) 12.dp else 0.dp, RoundedCornerShape(8.dp))
                            .graphicsLayer { scaleX = if (isDragging) 1.04f else 1f; scaleY = if (isDragging) 1.04f else 1f }
                            .pointerInput(photo.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { start ->
                                        editorView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                        draggingPhotoId = photo.id
                                        photoDragOffset = Offset.Zero
                                        photoDragPoint = photoBounds[photo.id]?.let { Offset(it.left + start.x, it.top + start.y) }
                                    },
                                    onDragEnd = {
                                        val targetId = photoDragPoint?.let { point ->
                                            photoBounds.entries.firstOrNull { (candidateId, bounds) -> candidateId != photo.id && bounds.contains(point) }?.key
                                        }
                                        if (targetId != null) scope.launch { viewModel.movePhotoTo(photo.id, targetId) }
                                        draggingPhotoId = null
                                        photoDragOffset = Offset.Zero
                                        photoDragPoint = null
                                    },
                                    onDragCancel = {
                                        draggingPhotoId = null
                                        photoDragOffset = Offset.Zero
                                        photoDragPoint = null
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        photoDragOffset += dragAmount
                                        photoDragPoint = photoDragPoint?.plus(dragAmount)
                                    },
                                )
                            },
                    ) {
                        AsyncImage(
                            model = File(context.filesDir, photo.relativePath),
                            contentDescription = "照片，长按拖动排序",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                        )
                        Box(
                            Modifier.align(Alignment.TopEnd).size(36.dp).clickable { scope.launch { viewModel.removePhoto(photo.id) } }
                                .padding(6.dp).size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Outlined.Close, "移除照片", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp)) }
                    }
                }
                if (photos.size < 9) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Outlined.Add, "添加照片", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)) }
                    }
                }
            }
        }
        photoError?.let { message ->
            Text("照片导入失败：$message", modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = canUndo && loaded, modifier = Modifier.size(40.dp), onClick = {
                val previous = textHistory.undo(EditorText(title, content))
                titleInput = TextFieldValue(previous.title, TextRange(previous.title.length))
                contentInput = TextFieldValue(previous.content, TextRange(previous.content.length))
                historyRevision++
            }) { Icon(Icons.AutoMirrored.Outlined.Undo, "撤销文字", modifier = Modifier.size(20.dp)) }
            IconButton(enabled = canRedo && loaded, modifier = Modifier.size(40.dp), onClick = {
                val next = textHistory.redo(EditorText(title, content))
                titleInput = TextFieldValue(next.title, TextRange(next.title.length))
                contentInput = TextFieldValue(next.content, TextRange(next.content.length))
                historyRevision++
            }) { Icon(Icons.AutoMirrored.Outlined.Redo, "重做文字", modifier = Modifier.size(20.dp)) }
            TextButton(onClick = { showMoods = true }) { Text(mood ?: "心情", color = MaterialTheme.colorScheme.primary) }
            BasicTextField(
                value = tags,
                onValueChange = { tags = it },
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                decorationBox = { inner -> if (tags.isEmpty()) Text("标签，用逗号分隔", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp); inner() },
            )
        }
    }
    if (showMoods) AlertDialog(
        onDismissRequest = { showMoods = false },
        title = { Text("选择心情") },
        text = { Column { Moods.forEach { option -> TextButton(onClick = { mood = option; showMoods = false }, modifier = Modifier.fillMaxWidth()) { Text(option, color = if (mood == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) } } } },
        confirmButton = { TextButton(onClick = { mood = null; showMoods = false }) { Text("不选择") } },
    )
}

private fun todayLabel(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.SIMPLIFIED_CHINESE))
private fun storageLabel(bytes: Long): String = when {
    bytes < 1024L * 1024 -> "${(bytes / 1024).coerceAtLeast(1)} KB"
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024))
    else -> String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024 * 1024))
}
private fun monthLabel(value: Long): String = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.SIMPLIFIED_CHINESE))
private fun dayNumber(value: Long): String = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).dayOfMonth.toString().padStart(2, '0')
private fun weekday(value: Long): String = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE", Locale.SIMPLIFIED_CHINESE))
