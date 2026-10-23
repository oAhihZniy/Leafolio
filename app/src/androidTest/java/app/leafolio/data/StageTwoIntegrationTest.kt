package app.leafolio.data

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StageTwoIntegrationTest {
    private lateinit var base: Context
    private lateinit var context: Context
    private lateinit var root: File
    private lateinit var db: LeafolioDatabase
    private lateinit var preferenceScope: CoroutineScope
    private lateinit var preferences: UserPreferencesStore
    private val shareFiles = mutableListOf<Uri>()

    @Before fun setup() {
        base = InstrumentationRegistry.getInstrumentation().targetContext
        root = File(base.cacheDir, "stage-test-${UUID.randomUUID()}").apply { mkdirs() }
        val files = File(root, "files").apply { mkdirs() }
        context = object : ContextWrapper(base) {
            override fun getFilesDir(): File = files
        }
        db = Room.inMemoryDatabaseBuilder(base, LeafolioDatabase::class.java).build()
        preferenceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        preferences = UserPreferencesStore(context, PreferenceDataStoreFactory.create(scope = preferenceScope) { File(root, "test.preferences_pb") })
    }

    @After fun cleanup() {
        db.close()
        preferenceScope.cancel()
        shareFiles.forEach { base.contentResolver.delete(it, null, null) }
        root.deleteRecursively()
    }

    private fun diary(id: String, deleted: Boolean = false, draft: Boolean = false) = Diary(
        id = id, title = "测试标题", content = "第一行\n第二行", entryDate = 1,
        createdAt = 1, updatedAt = 1, isDraft = draft, deletedAt = if (deleted) 1 else null,
        mood = "开心", tags = "不可分享的标签",
    )

    private fun source(extension: String = "png"): File {
        val source = File(root, "source-${UUID.randomUUID()}.$extension")
        val bitmap = Bitmap.createBitmap(2300, 80, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.GREEN)
        source.outputStream().use { bitmap.compress(if (extension == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        return source
    }

    @Test fun originalIsByteIdenticalAndCompressionRemovesMetadata() = runBlocking {
        val source = source("jpg")
        ExifInterface(source.path).apply {
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "22/1,18/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            saveAttributes()
        }
        val original = PhotoStore.import(context, "original", Uri.fromFile(source), keepOriginal = true)
        val originalFile = File(context.filesDir, original.relativePath)
        assertArrayEquals(source.readBytes(), originalFile.readBytes())
        val compressed = PhotoStore.import(context, "compressed", Uri.fromFile(source))
        val compressedFile = File(context.filesDir, compressed.relativePath)
        assertNull(ExifInterface(compressedFile.path).getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(compressedFile.path, bounds)
        assertTrue(bounds.outWidth <= 2048)
        source.delete()
        assertTrue(originalFile.isFile)
        assertTrue(compressedFile.isFile)
    }

    @Test fun clearTrashKeepsActiveDraftAndSharedPhotoFiles() = runBlocking {
        val active = diary("active")
        val draft = diary("draft", draft = true)
        val trash = diary("trash", deleted = true)
        listOf(active, draft, trash).forEach { db.diaryDao().upsert(it) }
        val privateFile = File(context.filesDir, "diary-photos/trash/private.jpg").apply { parentFile!!.mkdirs(); writeText("private") }
        val sharedFile = File(context.filesDir, "diary-photos/shared.jpg").apply { writeText("shared") }
        val sharedPath = "diary-photos/shared.jpg"
        db.photoDao().insert(PhotoAttachment("private", "trash", "diary-photos/trash/private.jpg", 0, 1))
        db.photoDao().insert(PhotoAttachment("shared-trash", "trash", sharedPath, 1, 1))
        db.photoDao().insert(PhotoAttachment("shared-active", "active", sharedPath, 0, 1))
        DiaryDeletion.clearTrash(context, db)
        assertEquals(setOf("active", "draft"), db.diaryDao().getAll().map { it.id }.toSet())
        assertEquals(listOf("shared-active"), db.photoDao().getAll().map { it.id })
        assertFalse(privateFile.exists())
        assertTrue(sharedFile.isFile)
        DiaryDeletion.clearTrash(context, db)
        DiaryDeletion.deletePermanently(context, db, "active")
        assertNotNull(db.diaryDao().getById("active"))
    }

    @Test fun photoSharingUsesReadableCacheUrisWithoutDiaryMetadata() = runBlocking {
        val photo = PhotoStore.import(context, "sharing", Uri.fromFile(source()), keepOriginal = true)
        val intent = DiarySharing.photosIntent(context, listOf(photo))
        val uri = intent.clipData!!.getItemAt(0).uri
        shareFiles += uri
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("content", uri.scheme)
        assertEquals("image/png", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertFalse(intent.hasExtra(Intent.EXTRA_TEXT))
        val originalBytes = File(context.filesDir, photo.relativePath).readBytes()
        File(context.filesDir, photo.relativePath).delete()
        assertArrayEquals(originalBytes, base.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
        val text = DiarySharing.textIntent(diary("text")).getStringExtra(Intent.EXTRA_TEXT)!!
        assertEquals("测试标题\n\n第一行\n第二行", text)
    }

    @Test fun originalPngSurvivesBackupRestoreAndPortableExport() = runBlocking {
        db.diaryDao().upsert(diary("png"))
        val photo = PhotoStore.import(context, "png", Uri.fromFile(source()), keepOriginal = true)
        db.photoDao().insert(photo)
        val bytes = File(context.filesDir, photo.relativePath).readBytes()
        val backup = File(root, "backup.zip")
        DataTransfer.createBackup(context, Uri.fromFile(backup), db, UserPreferences())
        ZipFile(backup).use { assertNotNull(it.getEntry("photos/${photo.id}.png")) }
        val export = File(root, "export.zip")
        DataTransfer.createPortableExport(context, Uri.fromFile(export), db)
        ZipFile(export).use { zip ->
            assertNotNull(zip.getEntry("photos/${photo.id}.png"))
            val markdown = zip.entries().asSequence().first { it.name.endsWith(".md") }
            assertTrue(zip.getInputStream(markdown).bufferedReader().use { it.readText() }.contains("../photos/${photo.id}.png"))
        }
        db.diaryDao().deleteAll()
        File(context.filesDir, "diary-photos").deleteRecursively()
        DataTransfer.restore(context, Uri.fromFile(backup), db, preferences)
        val restored = db.photoDao().getAll().single()
        assertTrue(restored.relativePath.endsWith(".png"))
        assertArrayEquals(bytes, File(context.filesDir, restored.relativePath).readBytes())
    }

    @Test fun invalidOriginalLeavesNoPartialAttachment() = runBlocking {
        val source = File(root, "invalid.png").apply { writeText("not an image") }
        val result = runCatching { PhotoStore.import(context, "invalid", Uri.fromFile(source), keepOriginal = true) }
        assertTrue(result.isFailure)
        assertTrue(File(context.filesDir, "diary-photos/invalid").listFiles().orEmpty().isEmpty())
    }

    @Test fun multiplePhotoSharingGrantsEveryUriInDisplayOrder() = runBlocking {
        val first = PhotoStore.import(context, "multiple", Uri.fromFile(source()), keepOriginal = true).copy(position = 0)
        val second = PhotoStore.import(context, "multiple", Uri.fromFile(source("jpg")), keepOriginal = true).copy(position = 1)
        val intent = DiarySharing.photosIntent(context, listOf(second, first))
        assertEquals(Intent.ACTION_SEND_MULTIPLE, intent.action)
        assertEquals("image/*", intent.type)
        assertEquals(2, intent.clipData!!.itemCount)
        listOf(first, second).forEachIndexed { index, photo ->
            val uri = intent.clipData!!.getItemAt(index).uri
            shareFiles += uri
            assertArrayEquals(File(context.filesDir, photo.relativePath).readBytes(), base.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
        }
    }
}
