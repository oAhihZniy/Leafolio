package app.leafolio.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class Tag(
    @PrimaryKey val id: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
)

@Entity(
    tableName = "diary_tags",
    primaryKeys = ["diaryId", "tagId"],
    indices = [Index("tagId")],
    foreignKeys = [
        androidx.room.ForeignKey(entity = Diary::class, parentColumns = ["id"], childColumns = ["diaryId"], onDelete = androidx.room.ForeignKey.CASCADE),
        androidx.room.ForeignKey(entity = Tag::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = androidx.room.ForeignKey.CASCADE),
    ],
)
data class DiaryTag(
    val diaryId: String,
    val tagId: String,
)
