package app.leafolio.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DiarySharing {
    fun textIntent(diary: Diary): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, listOf(diary.title, diary.content).filter(String::isNotBlank).joinToString("\n\n"))
    }

    suspend fun photosIntent(context: Context, photos: List<PhotoAttachment>): Intent = withContext(Dispatchers.IO) {
        if (photos.isEmpty()) throw IOException("这篇日记没有照片")
        val root = File(context.cacheDir, "share").apply { if (!exists() && !mkdirs()) throw IOException("无法准备分享文件") }
        val expiry = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        root.listFiles()?.filter { it.lastModified() < expiry }?.forEach { it.deleteRecursively() }
        val directory = File(root, UUID.randomUUID().toString())
        if (!directory.mkdir()) throw IOException("无法准备分享文件")
        try {
            val types = mutableSetOf<String>()
            val uris = ArrayList(photos.sortedBy { it.position }.mapIndexed { index, photo ->
                val source = File(context.filesDir, photo.relativePath)
                if (!source.isFile) throw IOException("照片文件缺失，无法分享")
                val extension = source.extension.lowercase().takeIf { it.matches(Regex("[a-z0-9]+")) } ?: "img"
                val target = File(directory, "${index + 1}.$extension")
                source.copyTo(target)
                types += MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/*"
                FileProvider.getUriForFile(context, "${context.packageName}.sharedfiles", target)
            })
            Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
                type = types.singleOrNull() ?: "image/*"
                if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first())
                else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                clipData = ClipData.newUri(context.contentResolver, "日记照片", uris.first()).apply {
                    uris.drop(1).forEach { addItem(ClipData.Item(it)) }
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (error: Throwable) {
            directory.deleteRecursively()
            throw error
        }
    }
}
