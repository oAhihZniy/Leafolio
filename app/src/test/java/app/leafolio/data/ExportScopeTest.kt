package app.leafolio.data

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class ExportScopeTest {
    private val zone = ZoneId.of("Asia/Hong_Kong")
    private fun diary(id: String, date: String, draft: Boolean = false, deleted: Boolean = false) = Diary(
        id = id, title = id, content = "", entryDate = LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli(),
        createdAt = 1, updatedAt = 1, isDraft = draft, deletedAt = if (deleted) 1 else null,
    )
    private val entries = listOf(diary("before", "2026-10-21"), diary("start", "2026-10-22"), diary("end", "2026-10-23"), diary("draft", "2026-10-23", draft = true), diary("trash", "2026-10-23", deleted = true))

    @Test fun allAndSelectedExcludeDraftsTrashAndMissingIds() {
        assertEquals(listOf("before", "start", "end"), ExportScope().select(entries).map { it.id })
        assertEquals(listOf("end"), ExportScope("selected", selectedIds = setOf("end", "draft", "trash", "missing")).select(entries).map { it.id })
        assertTrue(ExportScope("selected").select(entries).isEmpty())
    }

    @Test fun datesIncludeBothBoundariesInDeviceTimezone() {
        val scope = ExportScope("dates", LocalDate.parse("2026-10-22"), LocalDate.parse("2026-10-23"))
        assertEquals(listOf("start", "end"), scope.select(entries, zone).map { it.id })
        assertEquals(listOf("end"), ExportScope("dates", LocalDate.parse("2026-10-23")).select(entries, zone).map { it.id })
        assertEquals(listOf("before"), ExportScope("dates", end = LocalDate.parse("2026-10-21")).select(entries, zone).map { it.id })
    }

    @Test fun invalidRangesAreRejected() {
        val scope = ExportScope("dates", LocalDate.parse("2026-10-23"), LocalDate.parse("2026-10-22"))
        assertFalse(scope.valid)
        assertThrows(IllegalArgumentException::class.java) { scope.select(entries) }
    }

    @Test fun backupReminderDefaultsOffAndUsesLatestLocalAnchor() {
        val day = 86_400_000L
        val now = 100 * day
        assertNull(BackupReminder.nextDue(UserPreferences(), now))
        val enabled = UserPreferences(backupReminderDays = 7, reminderEnabledAt = now)
        assertEquals(now + 7 * day, BackupReminder.nextDue(enabled, now))
        assertEquals(now + 9 * day, BackupReminder.nextDue(enabled.copy(lastBackupAt = now + 2 * day), now + 3 * day))
        assertEquals(now + 12 * day, BackupReminder.nextDue(enabled.copy(lastBackupReminderAt = now + 5 * day), now + 6 * day))
        assertEquals(now + 30 * day, BackupReminder.nextDue(enabled.copy(backupReminderDays = 30), now))
    }
}
