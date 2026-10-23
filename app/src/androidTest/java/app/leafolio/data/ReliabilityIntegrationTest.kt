package app.leafolio.data

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReliabilityIntegrationTest {
    private suspend fun <T> LeafolioDatabase.useDb(block: suspend (LeafolioDatabase) -> T): T = try { block(this) } finally { close() }
    private val base get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun diary(id: String, date: Long, draft: Boolean = false, deleted: Long? = null) =
        Diary(id, "标题$id", "正文 关键字$id", date, date, date, draft, deletedAt = deleted)

    @Test fun allHistoricalSchemasMigrateWithoutLosingRecords() = runBlocking {
        for (version in 1..4) {
            val name = "migration-${UUID.randomUUID()}.db"
            try {
                val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
                    .open("app.leafolio.data.LeafolioDatabase/$version.json").bufferedReader().readText()).getJSONObject("database")
                SQLiteDatabase.openOrCreateDatabase(base.getDatabasePath(name), null).use { sqlite ->
                    val entities = schema.getJSONArray("entities")
                    for (i in 0 until entities.length()) {
                        val entity = entities.getJSONObject(i)
                        sqlite.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}", entity.getString("tableName")))
                        val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                        for (j in 0 until indices.length()) sqlite.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}", entity.getString("tableName")))
                    }
                    val queries = schema.getJSONArray("setupQueries")
                    for (i in 0 until queries.length()) sqlite.execSQL(queries.getString(i))
                    val columns = "id,title,content,entryDate,createdAt,updatedAt,isDraft,isFavorite,isPinned" + if (version >= 2) ",tags" else ""
                    sqlite.execSQL("INSERT INTO diaries($columns) VALUES('retained','旧记录','不能丢失',1,1,1,0,1,1" + (if (version >= 2) ",''" else "") + ")")
                    if (version >= 2) sqlite.execSQL("UPDATE diaries SET tags = '生活|Life'")
                    if (version >= 3) sqlite.execSQL("INSERT INTO photo_attachments VALUES('photo','retained','diary-photos/photo.jpg',0,1)")
                    sqlite.version = version
                }
                Room.databaseBuilder(base, LeafolioDatabase::class.java, name).addMigrations(*LeafolioDatabase.ALL_MIGRATIONS).build().useDb { db ->
                    val entry = db.diaryDao().getById("retained")!!
                    assertEquals("不能丢失", entry.content)
                    assertTrue(entry.isFavorite && entry.isPinned)
                    if (version >= 3) assertEquals(1, db.photoDao().getAll().size)
                    if (version in 2..3) assertEquals(2, db.tagDao().getAll().size)
                    assertEquals(5, db.openHelper.readableDatabase.version)
                }
            } finally { base.deleteDatabase(name) }
        }
    }

    @Test fun tenThousandRecordsPageInStableOrderAndRemainSearchable() = runBlocking {
        Room.inMemoryDatabaseBuilder(base, LeafolioDatabase::class.java).build().useDb { db ->
            val start = android.os.SystemClock.elapsedRealtime()
            db.withTransaction { for (i in 1..10000) db.diaryDao().upsert(diary("entry$i", i.toLong())) }
            db.diaryDao().upsert(diary("draft", 20000, draft = true))
            db.diaryDao().upsert(diary("trash", 20001, deleted = 1))
            val first = db.diaryDao().observePage(60).first()
            val second = db.diaryDao().observePage(120).first()
            assertEquals(60, first.size)
            assertEquals(120, second.size)
            assertEquals(first.map { it.id }, second.take(60).map { it.id })
            assertEquals("entry10000", first.first().id)
            assertEquals(10000, db.diaryDao().observeEntryCount().first())
            assertEquals(listOf("entry9999"), db.diaryDao().getAll().filter { DiarySearchFilter(query = "关键字entry9999").matches(it) }.map { it.id })
            android.util.Log.i("LeafolioAudit", "10000 records insert + paging + search: ${android.os.SystemClock.elapsedRealtime() - start} ms")
            Unit
        }
    }

    @Test fun startupRemovesExpiredTrashAndOnlyUnreferencedFiles() = runBlocking {
        val root = File(base.cacheDir, "maintenance-test-${UUID.randomUUID()}").apply { mkdirs() }
        val files = File(root, "files").apply { mkdirs() }
        val cache = File(root, "cache").apply { mkdirs() }
        val context = object : ContextWrapper(base) { override fun getFilesDir() = files; override fun getCacheDir() = cache }
        try {
            Room.inMemoryDatabaseBuilder(base, LeafolioDatabase::class.java).build().useDb { db ->
                val now = 40L * 86_400_000
                listOf(diary("live", 1), diary("draft", 2, draft = true), diary("recent", 3, deleted = now - 1), diary("expired", 4, deleted = 1)).forEach { db.diaryDao().upsert(it) }
                val shared = File(files, "diary-photos/shared.jpg").apply { parentFile!!.mkdirs(); writeText("shared") }
                for (id in listOf("live", "draft", "recent", "expired")) db.photoDao().insert(PhotoAttachment(id, id, "diary-photos/shared.jpg", 0, 1))
                val orphan = File(files, "diary-photos/orphan.jpg").apply { writeText("orphan") }
                val outside = File(files, "keep.txt").apply { writeText("keep") }
                File(cache, "restore-abandoned").mkdirs()
                StorageMaintenance.run(context, db, now)
                assertEquals(3, db.diaryDao().getAll().size)
                assertTrue(shared.exists())
                assertFalse(orphan.exists())
                assertTrue(outside.exists())
                assertFalse(File(cache, "restore-abandoned").exists())
            }
        } finally { root.deleteRecursively() }
    }
    @Test fun dailyReminderUsesPrivateGenericNotificationAndCancelsWhenDisabled() = runBlocking {
        org.junit.Assume.assumeTrue(DailyReminder.allowed(base))
        val original = UserPreferencesStore(base).preferences.first()
        val manager = base.getSystemService(android.app.NotificationManager::class.java)
        try {
            DailyReminder.sync(base, UserPreferences(dailyReminderMinutes = 1260))
            DailyReminder.notify(base)
            // Notification delivery and cancellation are asynchronous binder operations.
            val notification = kotlinx.coroutines.withTimeout(3_000) {
                var posted = manager.activeNotifications.firstOrNull { it.id == 2002 }
                while (posted == null) {
                    kotlinx.coroutines.delay(50)
                    posted = manager.activeNotifications.firstOrNull { it.id == 2002 }
                }
                posted.notification
            }
            assertEquals(android.app.Notification.VISIBILITY_PRIVATE, notification.visibility)
            assertEquals("为今天留下一页", notification.extras.getString(android.app.Notification.EXTRA_TITLE))
            DailyReminder.sync(base, UserPreferences())
            kotlinx.coroutines.withTimeout(3_000) {
                while (manager.activeNotifications.any { it.id == 2002 }) kotlinx.coroutines.delay(50)
            }
            assertFalse(manager.activeNotifications.any { it.id == 2002 })
        } finally {
            manager.cancel(2002)
            DailyReminder.sync(base, original)
        }
    }

    @Test fun emptyDraftCleanupPreservesPhotoOnlyDrafts() = runBlocking {
        Room.inMemoryDatabaseBuilder(base, LeafolioDatabase::class.java).build().useDb { db ->
            val photoOnly = diary("photo-only", 1, draft = true).copy(title = "", content = "")
            val empty = photoOnly.copy(id = "empty")
            db.diaryDao().upsert(photoOnly)
            db.diaryDao().upsert(empty)
            db.photoDao().insert(PhotoAttachment("photo", photoOnly.id, "diary-photos/test.jpg", 0, 1))
            db.diaryDao().deleteEmptyDraft(photoOnly.id)
            db.diaryDao().deleteEmptyDraft(empty.id)
            assertNotNull(db.diaryDao().getById(photoOnly.id))
            assertEquals(1, db.photoDao().getAll().size)
            assertNull(db.diaryDao().getById(empty.id))
        }
    }

}
