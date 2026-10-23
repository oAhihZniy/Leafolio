package app.leafolio.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diaries WHERE isDraft = 0 AND deletedAt IS NULL ORDER BY isPinned DESC, entryDate DESC, updatedAt DESC")
    fun observeEntries(): Flow<List<Diary>>

    @Query("SELECT * FROM diaries WHERE isDraft = 0 AND deletedAt IS NULL ORDER BY isPinned DESC, entryDate DESC, updatedAt DESC, id DESC LIMIT :limit")
    fun observePage(limit: Int): Flow<List<Diary>>

    @Query("SELECT COUNT(*) FROM diaries WHERE isDraft = 0 AND deletedAt IS NULL")
    fun observeEntryCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM diaries")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM diaries WHERE isDraft = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeDrafts(): Flow<List<Diary>>

    @Query("SELECT * FROM diaries WHERE isDraft = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1")
    fun observeLatestDraft(): Flow<Diary?>

    @Query("SELECT * FROM diaries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Diary?

    @Query("SELECT * FROM diaries ORDER BY entryDate")
    suspend fun getAll(): List<Diary>

    @Query("SELECT * FROM diaries ORDER BY entryDate")
    fun observeAll(): Flow<List<Diary>>

    @Query("DELETE FROM diaries")
    suspend fun deleteAll()

    @Query("SELECT * FROM diaries WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<Diary>>

    @Query("SELECT * FROM diaries WHERE deletedAt IS NOT NULL")
    suspend fun getTrash(): List<Diary>

    @Query("DELETE FROM diaries WHERE deletedAt IS NOT NULL")
    suspend fun deleteTrash()

    @Query("DELETE FROM diaries WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("DELETE FROM diaries WHERE deletedAt IS NOT NULL AND deletedAt <= :olderThan")
    suspend fun purgeExpiredTrash(olderThan: Long)

    @Upsert
    suspend fun upsert(diary: Diary)

    @Query("DELETE FROM diaries WHERE id = :id AND isDraft = 1 AND NOT EXISTS (SELECT 1 FROM photo_attachments WHERE diaryId = :id)")
    suspend fun deleteEmptyDraft(id: String)
}
