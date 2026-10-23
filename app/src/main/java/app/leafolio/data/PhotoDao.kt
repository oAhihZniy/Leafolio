package app.leafolio.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photo_attachments WHERE diaryId = :diaryId ORDER BY position")
    fun observeForDiary(diaryId: String): Flow<List<PhotoAttachment>>

    @Query("SELECT COUNT(*) FROM photo_attachments WHERE diaryId = :diaryId")
    suspend fun countForDiary(diaryId: String): Int

    @Query("SELECT * FROM photo_attachments ORDER BY diaryId, position")
    suspend fun getAll(): List<PhotoAttachment>

    @Insert
    suspend fun insert(attachment: PhotoAttachment)

    @Query("UPDATE photo_attachments SET position = :position WHERE id = :id")
    suspend fun setPosition(id: String, position: Int)

    @Query("SELECT * FROM photo_attachments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PhotoAttachment?

    @Query("DELETE FROM photo_attachments WHERE id = :id")
    suspend fun delete(id: String)
}
