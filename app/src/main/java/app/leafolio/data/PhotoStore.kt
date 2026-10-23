package app.leafolio.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PhotoStore {
    suspend fun import(context: Context, diaryId: String, uri: Uri, keepOriginal: Boolean = false): PhotoAttachment = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "diary-photos/$diaryId")
        if (!directory.exists() && !directory.mkdirs()) throw IOException("无法创建照片目录")
        val id = UUID.randomUUID().toString()
        var file = File(directory, if (keepOriginal) "$id.pending" else "$id.jpg")
        try {
            if (keepOriginal) {
                val input = context.contentResolver.openInputStream(uri) ?: throw IOException("无法读取所选照片")
                input.use { source -> file.outputStream().use { source.copyTo(it) } }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("无法识别照片格式")
                val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(bounds.outMimeType) ?: "img"
                val target = File(directory, "$id.$extension")
                if (!file.renameTo(target)) throw IOException("无法保存原图")
                file = target
            } else {
                val bitmap = decode(context, uri) ?: throw IOException("照片解码失败")
                try {
                    file.outputStream().use { output ->
                        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 86, output)) throw IOException("照片压缩失败")
                    }
                } finally {
                    bitmap.recycle()
                }
            }
            PhotoAttachment(id, diaryId, "diary-photos/$diaryId/${file.name}", 0, System.currentTimeMillis())
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
    }

    private fun decode(context: Context, uri: Uri) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val longest = maxOf(info.size.width, info.size.height).coerceAtLeast(1)
            val scale = minOf(1.0, 2048.0 / longest)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSize(
                (info.size.width * scale).toInt().coerceAtLeast(1),
                (info.size.height * scale).toInt().coerceAtLeast(1),
            )
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("无法读取所选照片")
        input.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("无法识别照片格式")
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }

    suspend fun delete(context: Context, attachment: PhotoAttachment) = withContext(Dispatchers.IO) {
        File(context.filesDir, attachment.relativePath).delete()
        File(context.filesDir, "diary-photos/${attachment.diaryId}").takeIf { it.isDirectory && it.list()?.isEmpty() == true }?.delete()
    }
}
