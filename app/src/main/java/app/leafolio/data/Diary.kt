package app.leafolio.data

import androidx.room.Index
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diaries", indices = [Index(value = ["isDraft", "deletedAt", "isPinned", "entryDate", "updatedAt", "id"])])
data class Diary(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val entryDate: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isDraft: Boolean,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val mood: String? = null,
    val tags: String = "",
    val deletedAt: Long? = null,
)
