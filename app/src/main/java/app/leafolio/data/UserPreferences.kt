package app.leafolio.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.map

private val Context.preferencesStore by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val theme: String = "system",
    val bodySize: String = "medium",
    val keepOriginalPhotos: Boolean = false,
    val backupReminderDays: Int = 0,
    val lastBackupAt: Long = 0,
    val reminderEnabledAt: Long = 0,
    val lastBackupReminderAt: Long = 0,
    val dailyReminderMinutes: Int = -1,
    val lockDelaySeconds: Int = -1,
    val hidePreview: Boolean = false,
)

class UserPreferencesStore(context: Context, private val store: DataStore<Preferences> = context.preferencesStore) {
    val preferences = store.data.map { values ->
        UserPreferences(
            dailyReminderMinutes = values[DAILY_REMINDER] ?: -1,
            lockDelaySeconds = values[LOCK_DELAY] ?: -1,
            hidePreview = values[HIDE_PREVIEW] ?: false,
            theme = values[THEME] ?: "system",
            bodySize = values[BODY_SIZE] ?: "medium",
            keepOriginalPhotos = values[KEEP_ORIGINAL_PHOTOS] ?: false,
            backupReminderDays = values[BACKUP_REMINDER_DAYS] ?: 0,
            lastBackupAt = values[LAST_BACKUP_AT] ?: 0,
            reminderEnabledAt = values[REMINDER_ENABLED_AT] ?: 0,
            lastBackupReminderAt = values[LAST_BACKUP_REMINDER_AT] ?: 0,
        )
    }

    suspend fun setDailyReminder(value: Int) = store.edit { require(value in -1..1439); it[DAILY_REMINDER] = value }
    suspend fun setLockDelay(value: Int) = store.edit { require(value in setOf(-1, 0, 60)); it[LOCK_DELAY] = value }
    suspend fun setHidePreview(value: Boolean) = store.edit { it[HIDE_PREVIEW] = value }
    suspend fun setTheme(value: String) = store.edit { it[THEME] = value }
    suspend fun setBodySize(value: String) = store.edit { it[BODY_SIZE] = value }
    suspend fun setKeepOriginalPhotos(value: Boolean) = store.edit { it[KEEP_ORIGINAL_PHOTOS] = value }
    suspend fun setBackupReminderDays(value: Int) = store.edit {
        require(value in setOf(0, 7, 30))
        it[BACKUP_REMINDER_DAYS] = value
        it[REMINDER_ENABLED_AT] = System.currentTimeMillis()
        it[LAST_BACKUP_REMINDER_AT] = 0
    }
    suspend fun markBackupSuccess(now: Long = System.currentTimeMillis()) = store.edit { it[LAST_BACKUP_AT] = now }
    suspend fun markBackupReminder(now: Long) = store.edit { it[LAST_BACKUP_REMINDER_AT] = now }
    suspend fun restorePortable(value: UserPreferences) = store.edit {
        it[DAILY_REMINDER] = value.dailyReminderMinutes
        it[HIDE_PREVIEW] = value.hidePreview
        it[THEME] = value.theme
        it[BODY_SIZE] = value.bodySize
        it[KEEP_ORIGINAL_PHOTOS] = value.keepOriginalPhotos
        it[BACKUP_REMINDER_DAYS] = value.backupReminderDays
        it[REMINDER_ENABLED_AT] = System.currentTimeMillis()
        it[LAST_BACKUP_REMINDER_AT] = 0
        // The last successful backup belongs to this device, not the source device.
    }
    private companion object {
        val DAILY_REMINDER = intPreferencesKey("daily_reminder_minutes")
        val LOCK_DELAY = intPreferencesKey("lock_delay_seconds")
        val HIDE_PREVIEW = booleanPreferencesKey("hide_preview")
        val THEME = stringPreferencesKey("theme")
        val BODY_SIZE = stringPreferencesKey("body_size")
        val KEEP_ORIGINAL_PHOTOS = booleanPreferencesKey("keep_original_photos")
        val BACKUP_REMINDER_DAYS = intPreferencesKey("backup_reminder_days")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val REMINDER_ENABLED_AT = longPreferencesKey("reminder_enabled_at")
        val LAST_BACKUP_REMINDER_AT = longPreferencesKey("last_backup_reminder_at")
    }
}
