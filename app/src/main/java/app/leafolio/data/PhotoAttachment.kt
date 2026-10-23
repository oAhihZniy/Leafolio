package app.leafolio.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photo_attachments",
    foreignKeys = [ForeignKey(
        entity = Diary::class,
        parentColumns = ["id"],
        childColumns = ["diaryId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("diaryId")],
)
data class PhotoAttachment(
    @PrimaryKey val id: String,
    val diaryId: String,
    val relativePath: String,
    val position: Int,
    val createdAt: Long,
)
