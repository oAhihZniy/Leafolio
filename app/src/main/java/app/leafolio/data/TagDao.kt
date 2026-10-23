package app.leafolio.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Tag>>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Tag>

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Tag?

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): Tag?

    @Query("SELECT tagId FROM diary_tags WHERE diaryId = :diaryId")
    suspend fun getIdsForDiary(diaryId: String): List<String>

    @Query("SELECT diaryId FROM diary_tags WHERE tagId = :tagId")
    suspend fun getDiaryIds(tagId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: Tag)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(relation: DiaryTag)

    @Query("DELETE FROM diary_tags WHERE diaryId = :diaryId")
    suspend fun clearForDiary(diaryId: String)

    @Query("DELETE FROM diary_tags WHERE diaryId = :diaryId AND tagId = :tagId")
    suspend fun unlink(diaryId: String, tagId: String)

    @Query("UPDATE tags SET name = :newName WHERE id = :id")
    suspend fun rename(id: String, newName: String)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tags WHERE id NOT IN (SELECT DISTINCT tagId FROM diary_tags)")
    suspend fun deleteUnused()

    @Query("DELETE FROM tags")
    suspend fun deleteAll()
}
