package app.leafolio.data

import android.content.Context
import androidx.room.withTransaction
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock

object DiaryDeletion {
    suspend fun clearTrash(context: Context, db: LeafolioDatabase) = withContext(Dispatchers.IO) {
        FileOperations.mutex.withLock {
        val removed = db.withTransaction {
            val ids = db.diaryDao().getTrash().mapTo(hashSetOf()) { it.id }
            val photos = db.photoDao().getAll().filter { it.diaryId in ids }
            db.diaryDao().deleteTrash()
            photos
        }
        deleteUnusedFiles(context, db, removed)
        }
    }

    suspend fun deletePermanently(context: Context, db: LeafolioDatabase, id: String) = withContext(Dispatchers.IO) {
        FileOperations.mutex.withLock {
        val removed = db.withTransaction {
            // Permanent deletion is only available for records already in the trash.
            if (db.diaryDao().getById(id)?.deletedAt == null) return@withTransaction emptyList()
            val photos = db.photoDao().getAll().filter { it.diaryId == id }
            db.diaryDao().deletePermanently(id)
            photos
        }
        deleteUnusedFiles(context, db, removed)
        }
    }

    private suspend fun deleteUnusedFiles(context: Context, db: LeafolioDatabase, removed: List<PhotoAttachment>) {
        val retained = db.photoDao().getAll().mapTo(hashSetOf()) { it.relativePath }
        val root = File(context.filesDir, "diary-photos").canonicalFile
        var failed = false
        removed.distinctBy { it.relativePath }.filter { it.relativePath !in retained }.forEach { photo ->
            val file = File(context.filesDir, photo.relativePath).canonicalFile
            if (!file.path.startsWith(root.path + File.separator)) return@forEach
            if (file.exists() && !file.delete()) failed = true
            file.parentFile?.takeIf { it != root && it.list()?.isEmpty() == true }?.delete()
        }
        if (failed) throw IOException("记录已删除，部分照片文件未能清理")
    }
}
