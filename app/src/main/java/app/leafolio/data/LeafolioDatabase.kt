package app.leafolio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Diary::class, PhotoAttachment::class, Tag::class, DiaryTag::class], version = 5, exportSchema = true)
abstract class LeafolioDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
    abstract fun photoDao(): PhotoDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile private var instance: LeafolioDatabase? = null

        fun get(context: Context): LeafolioDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                LeafolioDatabase::class.java,
                "leafolio.db",
            ).addMigrations(*ALL_MIGRATIONS).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diaries ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE diaries ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS photo_attachments (id TEXT NOT NULL, diaryId TEXT NOT NULL, relativePath TEXT NOT NULL, position INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(diaryId) REFERENCES diaries(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_photo_attachments_diaryId ON photo_attachments(diaryId)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS tags (id TEXT NOT NULL, name TEXT NOT NULL COLLATE NOCASE, PRIMARY KEY(id))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name COLLATE NOCASE)")
                db.execSQL("CREATE TABLE IF NOT EXISTS diary_tags (diaryId TEXT NOT NULL, tagId TEXT NOT NULL, PRIMARY KEY(diaryId, tagId), FOREIGN KEY(diaryId) REFERENCES diaries(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diary_tags_tagId ON diary_tags(tagId)")
                db.query("SELECT id, tags FROM diaries WHERE tags != ''").use { cursor ->
                    while (cursor.moveToNext()) {
                        val diaryId = cursor.getString(0)
                        cursor.getString(1).split('|').map(String::trim).filter(String::isNotBlank).distinctBy { it.lowercase() }.forEach { name ->
                            val tagId = java.util.UUID.nameUUIDFromBytes(("leafolio-tag:" + name.lowercase()).toByteArray()).toString()
                            val values = android.content.ContentValues().apply { put("id", tagId); put("name", name) }
                            db.insert("tags", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE, values)
                            db.execSQL("INSERT OR IGNORE INTO diary_tags(diaryId, tagId) SELECT ?, id FROM tags WHERE name = ? COLLATE NOCASE", arrayOf(diaryId, name))
                        }
                    }
                }
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diaries_isDraft_deletedAt_isPinned_entryDate_updatedAt_id ON diaries(isDraft, deletedAt, isPinned, entryDate, updatedAt, id)")
            }
        }
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

    }
}
