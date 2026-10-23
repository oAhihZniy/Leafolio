package app.leafolio.data

import android.content.Context
import androidx.room.withTransaction
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object FileOperations { val mutex = Mutex() }

object StorageMaintenance {
    suspend fun run(context: Context, db: LeafolioDatabase, now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        FileOperations.mutex.withLock {
            db.withTransaction { db.diaryDao().purgeExpiredTrash(now - 30L * 86_400_000) }
            val retained = db.photoDao().getAll().mapTo(hashSetOf()) { it.relativePath }
            val root = File(context.filesDir, "diary-photos")
            var failed = false
            if (root.isDirectory) {
                root.walkBottomUp().forEach { file ->
                    if (file.isFile && file.relativeTo(context.filesDir).invariantSeparatorsPath !in retained && !file.delete()) failed = true
                    if (file != root && file.isDirectory && file.list()?.isEmpty() == true) file.delete()
                }
            }
            // Only this app's abandoned transfer/share files are eligible for cleanup.
            context.cacheDir.listFiles()?.filter { it.name.startsWith("inspect-") || it.name.startsWith("restore-") || it.name.startsWith("leafolio-export-") }
                ?.forEach { if (!it.deleteRecursively()) failed = true }
            File(context.cacheDir, "share").listFiles()?.filter { now - it.lastModified() > 86_400_000 }?.forEach { it.deleteRecursively() }
            if (failed) throw IOException("部分未使用的临时文件暂时无法清理，下次启动会重试")
        }
    }
}
