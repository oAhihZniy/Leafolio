package app.leafolio.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import org.json.JSONArray
import org.json.JSONObject

data class RestoreSummary(val added: Int, val skipped: Int, val conflicts: Int, val photos: Int, val settingsWarning: Boolean = false)
data class TransferProgress(val label: String, val completed: Long = 0, val total: Long = 0)
data class RestorePreview(val diaries: Int, val tags: Int, val photos: Int, val requiredBytes: Long, val availableBytes: Long, val hasSettings: Boolean = false) {
    val hasSpace: Boolean get() = availableBytes >= requiredBytes
}

object DataTransfer {
    suspend fun inspectBackup(context: Context, uri: Uri): RestorePreview = withContext(Dispatchers.IO) {
        FileOperations.mutex.withLock {
        val temp = File(context.cacheDir, "inspect-${UUID.randomUUID()}")
        if (!temp.mkdirs()) throw IOException("无法准备备份检查空间")
        try {
            val seenPaths = hashSetOf<String>()
            var totalBytes = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory) continue
                        if (!safePath(entry.name) || !seenPaths.add(entry.name)) throw IOException("备份包含无效或重复路径")
                        val target = File(temp, entry.name)
                        target.parentFile?.mkdirs()
                        var entryBytes = 0L
                        target.outputStream().buffered().use { output ->
                            val buffer = ByteArray(32 * 1024)
                            while (true) {
                                coroutineContext.ensureActive()
                                val read = zip.read(buffer)
                                if (read < 0) break
                                entryBytes += read
                                totalBytes += read
                                if (entryBytes > 100L * 1024 * 1024 || totalBytes > 1024L * 1024 * 1024) throw IOException("备份文件过大")
                                output.write(buffer, 0, read)
                            }
                        }
                        zip.closeEntry()
                    }
                }
            } ?: throw IOException("无法读取备份文件")
            val manifestFile = File(temp, "data.json")
            if (!manifestFile.isFile || manifestFile.length() > 50L * 1024 * 1024) throw IOException("这不是有效的 Leafolio 完整备份")
            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            if (manifest.optString("format") != "leafolio-backup" || manifest.optInt("formatVersion") != 1) throw IOException("不支持此备份版本")
            val diaryIds = manifest.getJSONArray("diaries").let { array -> (0 until array.length()).map { array.getJSONObject(it).toDiary().id } }
            if (diaryIds.toSet().size != diaryIds.size) throw IOException("备份中存在重复日记 ID")
            val tags = manifest.optJSONArray("tags") ?: JSONArray()
            val tagIds = (0 until tags.length()).map { tags.getJSONObject(it).toTag().id }
            if (tagIds.toSet().size != tagIds.size) throw IOException("备份中存在重复标签 ID")
            val photos = manifest.getJSONArray("photos")
            val photoIds = hashSetOf<String>()
            val photoPaths = hashSetOf<String>()
            val knownDiaries = diaryIds.toSet()
            var photoBytes = 0L
            for (index in 0 until photos.length()) {
                val photo = photos.getJSONObject(index)
                val path = photo.getString("archivePath")
                val photoId = photo.getString("id")
                if (!photoIds.add(photoId) || !photoPaths.add(path)) throw IOException("备份中存在重复照片条目")
                val file = File(temp, path)
                if (!safePath(path) || !file.isFile || sha256(file) != photo.getString("sha256")) throw IOException("照片附件缺失或校验失败")
                if (photo.getString("diaryId") !in knownDiaries) throw IOException("照片关联的日记不存在")
                photoBytes += file.length()
            }
            val settings = manifest.portableSettings()
            RestorePreview(diaryIds.size, tags.length(), photos.length(), totalBytes + photoBytes, context.filesDir.usableSpace, settings != null)
        } finally {
            temp.deleteRecursively()
        }
        }
    }

    suspend fun createBackup(context: Context, uri: Uri, db: LeafolioDatabase,
        preferences: UserPreferences? = null, onProgress: (TransferProgress) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        FileOperations.mutex.withLock {
        onProgress(TransferProgress("正在整理记录"))
        val snapshot = db.withTransaction { Triple(db.diaryDao().getAll(), db.photoDao().getAll(), db.tagDao().getAll()) }
        val settings = preferences ?: UserPreferencesStore(context).preferences.first()
        writeArchive(context, uri, onProgress) { zip ->
                val (diaries, photos, tags) = snapshot
                val photoJson = JSONArray()
                photos.forEachIndexed { index, photo ->
                    onProgress(TransferProgress("正在打包照片", index.toLong(), photos.size.toLong()))
                    val file = File(context.filesDir, photo.relativePath)
                    if (!file.isFile) throw IOException("照片文件缺失，备份未完成")
                    val archivePath = "photos/${photo.id}.${photoExtension(photo.relativePath)}"
                    photoJson.put(photo.toJson(archivePath, sha256(file)))
                    zip.putNextEntry(ZipEntry(archivePath))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                val manifest = JSONObject()
                    .put("format", "leafolio-backup")
                    .put("formatVersion", 1)
                    .put("settings", settings.toPortableJson())
                    .put("diaries", JSONArray().apply { diaries.forEach { put(it.toJson()) } })
                    .put("tags", JSONArray().apply { tags.forEach { put(it.toJson()) } })
                    .put("photos", photoJson)
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
        }
        }
    }

    suspend fun createPortableExport(context: Context, uri: Uri, db: LeafolioDatabase,
        selection: ExportScope = ExportScope(), onProgress: (TransferProgress) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        FileOperations.mutex.withLock {
        onProgress(TransferProgress("正在整理记录"))
        val snapshot = db.withTransaction { Triple(db.diaryDao().getAll(), db.photoDao().getAll(), db.tagDao().getAll()) }
        val diaries = selection.select(snapshot.first)
        if (diaries.isEmpty()) throw IOException("没有符合范围的正式日记")
        writeArchive(context, uri, onProgress) { zip ->
                val diaryIds = diaries.mapTo(hashSetOf()) { it.id }
                val photos = snapshot.second.filter { it.diaryId in diaryIds }.sortedWith(compareBy({ it.diaryId }, { it.position }))
                val grouped = photos.groupBy { it.diaryId }
                val photoJson = JSONArray()
                photos.forEachIndexed { index, photo ->
                    onProgress(TransferProgress("正在打包照片", index.toLong(), (photos.size + diaries.size).toLong()))
                    val file = File(context.filesDir, photo.relativePath)
                    if (!file.isFile) throw IOException("照片文件缺失，导出未完成")
                    val archivePath = "photos/${photo.id}.${photoExtension(photo.relativePath)}"
                    photoJson.put(photo.toJson(archivePath, sha256(file)))
                    zip.putNextEntry(ZipEntry(archivePath))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                diaries.forEachIndexed { index, diary ->
                    onProgress(TransferProgress("正在生成日记文件", (photos.size + index).toLong(), (photos.size + diaries.size).toLong()))
                    val date = java.time.Instant.ofEpochMilli(diary.entryDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    val entryPath = "entries/${date}_${diary.id}.md"
                    val markdown = buildString {
                        appendLine("---")
                        appendLine("date: $date")
                        diary.mood?.let { appendLine("mood: $it") }
                        if (diary.tags.isNotBlank()) appendLine("tags: ${diary.tags.replace('|', ',')}")
                        appendLine("---")
                        appendLine()
                        if (diary.title.isNotBlank()) appendLine("# ${diary.title}\n")
                        appendLine(diary.content)
                        grouped[diary.id].orEmpty().forEach { appendLine("\n![photo](../photos/${it.id}.${photoExtension(it.relativePath)})") }
                    }
                    zip.putNextEntry(ZipEntry(entryPath))
                    zip.write(markdown.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                val manifest = JSONObject()
                    .put("format", "leafolio-portable-export")
                    .put("formatVersion", 1)
                    .put("diaries", JSONArray().apply { diaries.forEach { put(it.toJson()) } })
                    .put("tags", JSONArray().apply { snapshot.third.filter { tag -> diaries.any { tag.name in it.tags.split('|') } }.forEach { put(it.toJson()) } })
                    .put("photos", photoJson)
                zip.putNextEntry(ZipEntry("metadata.json"))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
        }
        }
    }

    suspend fun restore(context: Context, uri: Uri, db: LeafolioDatabase,
        settingsStore: UserPreferencesStore = UserPreferencesStore(context),
    ): RestoreSummary = withContext(Dispatchers.IO) {
        FileOperations.mutex.withLock {
        val temp = File(context.cacheDir, "restore-${UUID.randomUUID()}")
        if (!temp.mkdirs()) throw IOException("无法准备恢复空间")
        val committedFiles = mutableListOf<File>()
        try {
            val manifestFile = File(temp, "data.json")
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    val seenPaths = hashSetOf<String>()
                    var totalBytes = 0L
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory) continue
                        if (!safePath(entry.name) || !seenPaths.add(entry.name)) throw IOException("备份包含无效或重复路径")
                        val target = File(temp, entry.name)
                        target.parentFile?.mkdirs()
                        var entryBytes = 0L
                        target.outputStream().buffered().use { out ->
                            val buffer = ByteArray(32 * 1024)
                            while (true) {
                                coroutineContext.ensureActive()
                                val read = zip.read(buffer)
                                if (read < 0) break
                                entryBytes += read
                                totalBytes += read
                                if (entryBytes > 100L * 1024 * 1024 || totalBytes > 1024L * 1024 * 1024) throw IOException("备份文件过大")
                                out.write(buffer, 0, read)
                            }
                        }
                        zip.closeEntry()
                    }
                }
            } ?: throw IOException("无法读取备份文件")
            if (!manifestFile.isFile || manifestFile.length() > 50L * 1024 * 1024) throw IOException("这不是有效的 Leafolio 完整备份")
            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            if (manifest.optString("format") != "leafolio-backup" || manifest.optInt("formatVersion") != 1) throw IOException("不支持此备份版本")
            val settings = manifest.portableSettings()
            val incomingDiaries = manifest.getJSONArray("diaries").let { array -> (0 until array.length()).map { array.getJSONObject(it).toDiary() } }
            val incomingPhotos = manifest.getJSONArray("photos").let { array -> (0 until array.length()).map { array.getJSONObject(it).toPhoto() } }
            val incomingTags = (manifest.optJSONArray("tags") ?: JSONArray()).let { array -> (0 until array.length()).map { array.getJSONObject(it).toTag() } }
            if (incomingDiaries.map { it.id }.toSet().size != incomingDiaries.size) throw IOException("备份中存在重复日记 ID")
            if (incomingTags.map { it.id }.toSet().size != incomingTags.size) throw IOException("备份中存在重复标签 ID")
            if (incomingPhotos.map { it.id }.toSet().size != incomingPhotos.size || incomingPhotos.map { it.archivePath }.toSet().size != incomingPhotos.size) throw IOException("备份中存在重复照片条目")
            val knownDiaries = incomingDiaries.mapTo(hashSetOf()) { it.id }
            val needed = incomingPhotos.sumOf { File(temp, it.archivePath).length() } + 2L * manifestFile.length() + 1024 * 1024
            if (context.filesDir.usableSpace < needed) throw IOException("设备可用空间不足，无法恢复备份")
            incomingPhotos.forEach { photo ->
                val file = File(temp, photo.archivePath)
                if (!safePath(photo.archivePath) || !file.isFile || sha256(file) != photo.checksum) throw IOException("照片附件缺失或校验失败")
                if (photo.diaryId !in knownDiaries) throw IOException("照片关联的日记不存在")
            }

            val localDiaries = db.diaryDao().getAll().associateBy { it.id }
            val existingAll = localDiaries.values.associateBy { it.id }.toMutableMap()
            val localPhotos = db.photoDao().getAll().groupBy { it.diaryId }
            val incomingPhotoSignatures = incomingPhotos.groupBy { it.diaryId }.mapValues { (_, photos) ->
                photos.sortedBy { it.position }.joinToString("|") { "${it.position}:${it.checksum}" }
            }
            val localPhotoSignatures = mutableMapOf<String, String>()
            fun photosMatch(localId: String, incomingId: String): Boolean {
                val signature = localPhotoSignatures.getOrPut(localId) {
                    localPhotos[localId].orEmpty().sortedBy { it.position }.joinToString("|") {
                        val file = File(context.filesDir, it.relativePath)
                        "${it.position}:${if (file.isFile) sha256(file) else "missing"}"
                    }
                }
                return signature == incomingPhotoSignatures[incomingId].orEmpty()
            }
            val finalIds = mutableMapOf<String, String>()
            val toInsert = mutableListOf<Diary>()
            var skipped = 0
            var conflicts = 0
            incomingDiaries.forEach { incoming ->
                val existing = localDiaries[incoming.id]
                when {
                    existing == null -> {
                        finalIds[incoming.id] = incoming.id
                        toInsert += incoming
                    }
                    existing.sameContent(incoming) && photosMatch(existing.id, incoming.id) -> {
                        skipped++
                    }
                    else -> {
                        val legacyId = UUID.nameUUIDFromBytes((incoming.id + ":" + incoming.signature()).toByteArray()).toString()
                        val legacy = existingAll[legacyId]
                        if (legacy != null && legacy.sameContent(incoming) && photosMatch(legacy.id, incoming.id)) { skipped++; return@forEach }
                        val source = incoming.id + ":" + incoming.signature() + ":" + incomingPhotoSignatures[incoming.id].orEmpty()
                        var conflictId = UUID.nameUUIDFromBytes(source.toByteArray()).toString()
                        var prior = existingAll[conflictId]
                        var attempts = 0
                        while (prior != null && !(prior.sameContent(incoming) && photosMatch(prior.id, incoming.id))) {
                            if (++attempts > 1000) throw IOException("备份冲突过多，未修改现有数据")
                            conflictId = UUID.nameUUIDFromBytes((source + ":" + conflictId + ":" + prior.signature()).toByteArray()).toString()
                            prior = existingAll[conflictId]
                        }
                        if (prior == null) {
                            val conflict = incoming.copy(id = conflictId)
                            finalIds[incoming.id] = conflictId
                            toInsert += conflict
                            existingAll[conflictId] = conflict
                            localPhotoSignatures[conflictId] = incomingPhotoSignatures[incoming.id].orEmpty()
                            conflicts++
                        } else skipped++
                    }
                }
            }

            val photoRows = mutableListOf<PhotoAttachment>()
            incomingPhotos.forEach { incoming ->
                coroutineContext.ensureActive()
                val finalDiaryId = finalIds[incoming.diaryId] ?: return@forEach
                val photoId = UUID.randomUUID().toString()
                val targetDir = File(context.filesDir, "diary-photos/$finalDiaryId")
                if (!targetDir.exists() && !targetDir.mkdirs()) throw IOException("无法创建照片目录")
                val target = File(targetDir, "$photoId.${photoExtension(incoming.archivePath)}")
                committedFiles += target
                File(temp, incoming.archivePath).copyTo(target, overwrite = false)
                photoRows += PhotoAttachment(photoId, finalDiaryId, "diary-photos/$finalDiaryId/${target.name}", incoming.position, incoming.createdAt)
            }
            db.withTransaction {
                incomingTags.forEach { incomingTag ->
                    if (db.tagDao().getByName(incomingTag.name) == null) {
                        val idCollision = db.tagDao().getById(incomingTag.id)
                        val destinationId = if (idCollision == null) incomingTag.id else UUID.nameUUIDFromBytes((incomingTag.id + ":" + incomingTag.name).toByteArray()).toString()
                        db.tagDao().insert(Tag(destinationId, incomingTag.name))
                    }
                }
                toInsert.forEach { db.diaryDao().upsert(it) }
                toInsert.forEach { diary ->
                    diary.tags.split('|').map(String::trim).filter(String::isNotBlank).distinctBy { it.lowercase() }.forEach { name ->
                        var tag = db.tagDao().getByName(name)
                        if (tag == null) {
                            val id = UUID.nameUUIDFromBytes(("leafolio-tag:" + name.lowercase()).toByteArray()).toString()
                            db.tagDao().insert(Tag(id, name))
                            tag = db.tagDao().getByName(name)
                        }
                        tag?.let { db.tagDao().link(DiaryTag(diary.id, it.id)) }
                    }
                }
                photoRows.forEach { db.photoDao().insert(it) }
            }
            // Data is already committed. A settings failure must never remove restored photos.
            val settingsWarning = settings != null && runCatching { settingsStore.restorePortable(settings) }.isFailure
            RestoreSummary(toInsert.size, skipped, conflicts, photoRows.size, settingsWarning)
        } catch (error: Throwable) {
            committedFiles.forEach(File::delete)
            throw error
        } finally {
            temp.deleteRecursively()
        }
        }
    }

    private fun writeArchive(context: Context, uri: Uri, progress: (TransferProgress) -> Unit, write: (ZipOutputStream) -> Unit) {
        val temp = File.createTempFile("leafolio-export-", ".zip", context.cacheDir)
        try {
            ZipOutputStream(temp.outputStream().buffered()).use(write)
            progress(TransferProgress("正在保存文件", 0, temp.length()))
            val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("无法创建文件")
            output.buffered().use { target ->
                temp.inputStream().use { source ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    while (true) {
                        val size = source.read(buffer)
                        if (size < 0) break
                        target.write(buffer, 0, size)
                        written += size
                        progress(TransferProgress("正在保存文件", written, temp.length()))
                    }
                }
            }
        } finally {
            temp.delete()
        }
    }

    private fun UserPreferences.toPortableJson() = JSONObject()
        .put("theme", theme).put("bodySize", bodySize).put("keepOriginalPhotos", keepOriginalPhotos)
        .put("backupReminderDays", backupReminderDays)
        .put("dailyReminderMinutes", dailyReminderMinutes).put("hidePreview", hidePreview)

    private fun JSONObject.portableSettings(): UserPreferences? {
        if (!has("settings")) return null // Version 1 backups made before settings support.
        val value = getJSONObject("settings")
        val theme = value.getString("theme")
        val size = value.getString("bodySize")
        val days = value.optInt("backupReminderDays", 0)
        if (theme !in setOf("system", "light", "dark") || size !in setOf("small", "medium", "large") || days !in setOf(0, 7, 30)) {
            throw IOException("备份中的设置无效")
        }
        val daily = value.optInt("dailyReminderMinutes", -1)
        if (daily !in -1..1439) throw IOException("备份中的提醒时间无效")
        return UserPreferences(theme, size, value.getBoolean("keepOriginalPhotos"), days,
            dailyReminderMinutes = daily, hidePreview = value.optBoolean("hidePreview", false))
    }

    private fun photoExtension(path: String): String = File(path).extension.lowercase()
        .takeIf { it.matches(Regex("[a-z0-9]{1,10}")) } ?: "img"

    private data class IncomingPhoto(
        val id: String,
        val diaryId: String,
        val position: Int,
        val createdAt: Long,
        val archivePath: String,
        val checksum: String,
    )

    private fun Diary.toJson() = JSONObject()
        .put("id", id).put("title", title).put("content", content)
        .put("entryDate", entryDate).put("createdAt", createdAt).put("updatedAt", updatedAt)
        .put("isDraft", isDraft).put("isFavorite", isFavorite).put("isPinned", isPinned)
        .put("mood", mood ?: JSONObject.NULL).put("tags", tags)
        .put("deletedAt", deletedAt ?: JSONObject.NULL)

    private fun PhotoAttachment.toJson(path: String, checksum: String) = JSONObject()
        .put("id", id).put("diaryId", diaryId).put("position", position)
        .put("createdAt", createdAt).put("archivePath", path).put("sha256", checksum)

    private fun Tag.toJson() = JSONObject().put("id", id).put("name", name)

    private fun JSONObject.safeId(key: String): String = getString(key).also {
        if (!it.matches(Regex("[A-Za-z0-9_-]{1,128}"))) throw IOException("备份中的标识无效")
    }
    private fun JSONObject.toDiary() = Diary(
        id = safeId("id"), title = getString("title"), content = getString("content"),
        entryDate = getLong("entryDate"), createdAt = getLong("createdAt"), updatedAt = getLong("updatedAt"),
        isDraft = getBoolean("isDraft"), isFavorite = getBoolean("isFavorite"), isPinned = getBoolean("isPinned"),
        mood = nullableString("mood"), tags = optString("tags", ""), deletedAt = nullableLong("deletedAt"),
    )

    private fun JSONObject.toPhoto() = IncomingPhoto(
        id = safeId("id"), diaryId = safeId("diaryId"), position = getInt("position"),
        createdAt = getLong("createdAt"), archivePath = getString("archivePath"), checksum = getString("sha256"),
    )

    private fun JSONObject.toTag() = Tag(safeId("id"), getString("name"))

    private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONObject.nullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)
    private fun Diary.sameContent(other: Diary) = copy(id = "") == other.copy(id = "")
    private fun Diary.signature(): String = sha256(toJson().toString().toByteArray())

    private fun safePath(path: String) = path.isNotBlank() && !path.startsWith('/') &&
        path.split('/').all { it.isNotBlank() && it != "." && it != ".." } &&
        path.all { it.isLetterOrDigit() || it in "/._-" }

    private fun sha256(file: File): String = file.inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(32 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}
