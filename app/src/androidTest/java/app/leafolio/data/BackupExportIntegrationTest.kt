package app.leafolio.data

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupExportIntegrationTest {
    private lateinit var context: Context
    private lateinit var root: File
    private lateinit var db: LeafolioDatabase
    private lateinit var prefs: UserPreferencesStore
    private lateinit var preferenceScope: CoroutineScope

    @Before fun setup() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        root = File(base.cacheDir, "backup-test-${UUID.randomUUID()}").apply { mkdirs() }
        val files = File(root, "files").apply { mkdirs() }
        val cache = File(root, "cache").apply { mkdirs() }
        context = object : ContextWrapper(base) {
            override fun getFilesDir() = files
            override fun getCacheDir() = cache
        }
        db = Room.inMemoryDatabaseBuilder(base, LeafolioDatabase::class.java).build()
        preferenceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        prefs = UserPreferencesStore(context, PreferenceDataStoreFactory.create(scope = preferenceScope) { File(root, "test.preferences_pb") })
    }

    @After fun cleanup() {
        db.close()
        preferenceScope.cancel()
        root.deleteRecursively()
    }

    private fun diary(id: String, draft: Boolean = false, deleted: Boolean = false, tags: String = "") = Diary(
        id = id, title = "标题$id", content = "正文", entryDate = LocalDate.of(2026, 10, 23).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        createdAt = 1, updatedAt = 1, isDraft = draft, deletedAt = if (deleted) 1 else null, tags = tags,
    )

    private fun manifest(file: File, path: String = "data.json") = ZipFile(file).use { zip -> JSONObject(zip.getInputStream(zip.getEntry(path)).bufferedReader().readText()) }

    private fun rewrite(source: File, target: File, change: (JSONObject) -> Unit) {
        ZipFile(source).use { zip ->
            ZipOutputStream(target.outputStream()).use { out ->
                zip.entries().asSequence().forEach { entry ->
                    out.putNextEntry(ZipEntry(entry.name))
                    if (entry.name == "data.json") {
                        val json = JSONObject(zip.getInputStream(entry).bufferedReader().readText())
                        change(json)
                        out.write(json.toString().toByteArray())
                    } else zip.getInputStream(entry).use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
        }
    }

    @Test fun completeBackupRestoresPortableSettingsButKeepsDeviceBackupTime() = runBlocking {
        listOf(diary("active"), diary("draft", draft = true), diary("trash", deleted = true)).forEach { db.diaryDao().upsert(it) }
        val backup = File(root, "backup.zip")
        val progress = mutableListOf<TransferProgress>()
        DataTransfer.createBackup(context, Uri.fromFile(backup), db, UserPreferences("dark", "large", true, 30, lastBackupAt = 999), progress::add)
        val json = manifest(backup)
        assertEquals(3, json.getJSONArray("diaries").length())
        val settings = json.getJSONObject("settings")
        assertFalse(settings.has("lastBackupAt"))
        assertFalse(settings.has("appLock"))
        assertTrue(DataTransfer.inspectBackup(context, Uri.fromFile(backup)).hasSettings)
        assertEquals(progress.last().total, progress.last().completed)
        prefs.markBackupSuccess(123)
        db.diaryDao().deleteAll()
        val result = DataTransfer.restore(context, Uri.fromFile(backup), db, prefs)
        assertEquals(3, result.added)
        val restored = prefs.preferences.first()
        assertEquals("dark", restored.theme)
        assertEquals("large", restored.bodySize)
        assertTrue(restored.keepOriginalPhotos)
        assertEquals(30, restored.backupReminderDays)
        assertEquals(123L, restored.lastBackupAt)
        assertEquals(3, DataTransfer.restore(context, Uri.fromFile(backup), db, prefs).skipped)
    }

    @Test fun legacyBackupLeavesLocalSettingsUnchanged() = runBlocking {
        db.diaryDao().upsert(diary("old"))
        val backup = File(root, "new.zip")
        val legacy = File(root, "old.zip")
        DataTransfer.createBackup(context, Uri.fromFile(backup), db, UserPreferences())
        rewrite(backup, legacy) { it.remove("settings") }
        prefs.setTheme("dark")
        prefs.setKeepOriginalPhotos(true)
        assertFalse(DataTransfer.inspectBackup(context, Uri.fromFile(legacy)).hasSettings)
        DataTransfer.restore(context, Uri.fromFile(legacy), db, prefs)
        assertEquals("dark", prefs.preferences.first().theme)
        assertTrue(prefs.preferences.first().keepOriginalPhotos)
    }

    @Test fun selectedExportContainsOnlySelectedDiaryPhotoAndRelevantTags() = runBlocking {
        listOf(diary("chosen", tags = "选中标签"), diary("other", tags = "其他标签"), diary("draft", draft = true), diary("trash", deleted = true)).forEach { db.diaryDao().upsert(it) }
        db.tagDao().insert(Tag("tag1", "选中标签"))
        db.tagDao().insert(Tag("tag2", "其他标签"))
        listOf("chosen", "other").forEach { id ->
            val path = "diary-photos/$id/photo.png"
            File(context.filesDir, path).apply { parentFile!!.mkdirs(); writeText(id) }
            db.photoDao().insert(PhotoAttachment("photo-$id", id, path, 0, 1))
        }
        val export = File(root, "selected.zip")
        DataTransfer.createPortableExport(context, Uri.fromFile(export), db, ExportScope("selected", selectedIds = setOf("chosen", "draft", "trash")))
        val json = manifest(export, "metadata.json")
        assertEquals(1, json.getJSONArray("diaries").length())
        assertEquals("chosen", json.getJSONArray("diaries").getJSONObject(0).getString("id"))
        assertEquals(1, json.getJSONArray("photos").length())
        assertEquals(1, json.getJSONArray("tags").length())
        ZipFile(export).use { zip ->
            assertNotNull(zip.getEntry("photos/photo-chosen.png"))
            assertNull(zip.getEntry("photos/photo-other.png"))
            val markdown = zip.entries().asSequence().single { it.name.endsWith(".md") }
            assertTrue(zip.getInputStream(markdown).bufferedReader().readText().contains("../photos/photo-chosen.png"))
        }
    }

    @Test fun invalidSettingsAndEmptySelectionFailWithoutChangingRecords() = runBlocking {
        db.diaryDao().upsert(diary("keep"))
        val backup = File(root, "valid.zip")
        val broken = File(root, "broken.zip")
        DataTransfer.createBackup(context, Uri.fromFile(backup), db, UserPreferences())
        rewrite(backup, broken) { it.getJSONObject("settings").put("backupReminderDays", -1) }
        assertTrue(runCatching { DataTransfer.inspectBackup(context, Uri.fromFile(broken)) }.isFailure)
        assertTrue(runCatching { DataTransfer.restore(context, Uri.fromFile(broken), db, prefs) }.isFailure)
        assertEquals(listOf("keep"), db.diaryDao().getAll().map { it.id })
        assertTrue(runCatching { DataTransfer.createPortableExport(context, Uri.fromFile(File(root, "empty.zip")), db, ExportScope("selected")) }.isFailure)
        assertEquals(0L, prefs.preferences.first().lastBackupAt)
        assertTrue(context.cacheDir.listFiles().orEmpty().isEmpty())
    }

    @Test fun failedDestinationWriteDoesNotLeaveTemporaryArchive() = runBlocking {
        db.diaryDao().upsert(diary("keep"))
        val directory = File(root, "destination-directory").apply { mkdirs() }
        assertTrue(runCatching { DataTransfer.createBackup(context, Uri.fromFile(directory), db, UserPreferences()) }.isFailure)
        assertTrue(context.cacheDir.listFiles().orEmpty().isEmpty())
        assertEquals(1, db.diaryDao().getAll().size)
    }
    @Test fun corruptMissingFutureAndUnsafeBackupsLeaveExistingDataUntouched() = runBlocking {
        db.diaryDao().upsert(diary("existing"))
        val backup = File(root, "source.zip")
        DataTransfer.createBackup(context, Uri.fromFile(backup), db, UserPreferences())
        val modifications: List<(JSONObject) -> Unit> = listOf(
            { it.put("formatVersion", 99) },
            { it.getJSONArray("diaries").getJSONObject(0).put("id", "../../escape") },
            { it.getJSONArray("photos").put(JSONObject().put("id", "missing").put("diaryId", "existing").put("position", 0).put("createdAt", 1).put("archivePath", "photos/missing.jpg").put("sha256", "wrong")) },
        )
        modifications.forEachIndexed { index, change ->
            val invalid = File(root, "invalid$index.zip")
            rewrite(backup, invalid, change)
            assertTrue(runCatching { DataTransfer.inspectBackup(context, Uri.fromFile(invalid)) }.isFailure)
            assertTrue(runCatching { DataTransfer.restore(context, Uri.fromFile(invalid), db, prefs) }.isFailure)
            assertEquals(listOf("existing"), db.diaryDao().getAll().map { it.id })
        }
        val truncated = File(root, "truncated.zip").apply { writeBytes(backup.readBytes().take(40).toByteArray()) }
        assertTrue(runCatching { DataTransfer.restore(context, Uri.fromFile(truncated), db, prefs) }.isFailure)
        assertTrue(context.cacheDir.listFiles().orEmpty().isEmpty())
    }

    @Test fun restoreDatabaseFailureRollsBackRowsAndNewFiles() = runBlocking {
        db.diaryDao().upsert(diary("incoming"))
        val source = File(context.filesDir, "diary-photos/incoming/photo.jpg").apply { parentFile!!.mkdirs(); writeText("photo") }
        db.photoDao().insert(PhotoAttachment("photo", "incoming", "diary-photos/incoming/photo.jpg", 0, 1))
        val backup = File(root, "rollback.zip")
        DataTransfer.createBackup(context, Uri.fromFile(backup), db, UserPreferences())
        db.diaryDao().deleteAll()
        source.delete()
        db.diaryDao().upsert(diary("existing"))
        val retained = File(context.filesDir, "diary-photos/existing/retained.jpg").apply { parentFile!!.mkdirs(); writeText("retained") }
        db.photoDao().insert(PhotoAttachment("retained", "existing", "diary-photos/existing/retained.jpg", 0, 1))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_import BEFORE INSERT ON diaries WHEN NEW.id = 'incoming' BEGIN SELECT RAISE(ABORT, 'simulated storage failure'); END")
        assertTrue(runCatching { DataTransfer.restore(context, Uri.fromFile(backup), db, prefs) }.isFailure)
        assertEquals(listOf("existing"), db.diaryDao().getAll().map { it.id })
        assertEquals("retained", retained.readText())
        assertEquals(listOf(retained.canonicalPath), File(context.filesDir, "diary-photos").walkTopDown().filter { it.isFile }.map { it.canonicalPath }.toList())
        assertTrue(context.cacheDir.listFiles().orEmpty().isEmpty())
    }

    @Test fun attachmentOnlyConflictAndEditedConflictCopiesStayIdempotent() = runBlocking {
        db.diaryDao().upsert(diary("same"))
        val photo = File(context.filesDir, "diary-photos/same/photo.jpg").apply { parentFile!!.mkdirs(); writeText("original") }
        db.photoDao().insert(PhotoAttachment("photo", "same", "diary-photos/same/photo.jpg", 0, 1))
        val backup = File(root, "conflict.zip")
        DataTransfer.createBackup(context, Uri.fromFile(backup), db, UserPreferences())
        photo.writeText("changed on this phone")
        assertEquals(1, DataTransfer.restore(context, Uri.fromFile(backup), db, prefs).conflicts)
        assertEquals(1, DataTransfer.restore(context, Uri.fromFile(backup), db, prefs).skipped)
        assertEquals(2, db.diaryDao().getAll().size)
        val conflict = db.diaryDao().getAll().first { it.id != "same" }
        db.diaryDao().upsert(conflict.copy(content = "edited conflict"))
        assertEquals(1, DataTransfer.restore(context, Uri.fromFile(backup), db, prefs).conflicts)
        assertEquals(1, DataTransfer.restore(context, Uri.fromFile(backup), db, prefs).skipped)
        assertEquals(3, db.diaryDao().getAll().size)
        assertEquals("changed on this phone", photo.readText())
    }

    @Test fun privacySettingsMigrationNeverEnablesAppLock() = runBlocking {
        val backup = File(root, "privacy.zip")
        prefs.setLockDelay(60)
        DataTransfer.createBackup(context, Uri.fromFile(backup), db, UserPreferences(dailyReminderMinutes = 1260, lockDelaySeconds = 0, hidePreview = true))
        val settings = manifest(backup).getJSONObject("settings")
        assertFalse(settings.has("lockDelaySeconds"))
        DataTransfer.restore(context, Uri.fromFile(backup), db, prefs)
        val migrated = prefs.preferences.first()
        assertEquals(60, migrated.lockDelaySeconds)
        assertEquals(1260, migrated.dailyReminderMinutes)
        assertTrue(migrated.hidePreview)
    }

    @Test fun insufficientSpaceRejectsRestoreWithoutChangingRecords() = runBlocking {
        db.diaryDao().upsert(diary("existing"))
        val backup = File(root, "space.zip")
        DataTransfer.createBackup(context, Uri.fromFile(backup), db, UserPreferences())
        val limited = object : ContextWrapper(context) {
            override fun getFilesDir(): File = object : File(context.filesDir.path) { override fun getUsableSpace(): Long = 0 }
        }
        assertFalse(DataTransfer.inspectBackup(limited, Uri.fromFile(backup)).hasSpace)
        assertTrue(runCatching { DataTransfer.restore(limited, Uri.fromFile(backup), db, prefs) }.isFailure)
        assertEquals(1, db.diaryDao().getAll().size)
        assertTrue(context.cacheDir.listFiles().orEmpty().isEmpty())
    }

    @Test fun cancelledArchiveWriteCleansTemporaryOutputAndKeepsData() = runBlocking {
        db.diaryDao().upsert(diary("existing"))
        val output = File(root, "cancelled.zip")
        val result = runCatching {
            DataTransfer.createBackup(context, Uri.fromFile(output), db, UserPreferences()) { progress ->
                if (progress.label == "正在保存文件") throw CancellationException("simulated cancellation")
            }
        }
        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals(1, db.diaryDao().getAll().size)
        assertTrue(context.cacheDir.listFiles().orEmpty().isEmpty())
    }

}
