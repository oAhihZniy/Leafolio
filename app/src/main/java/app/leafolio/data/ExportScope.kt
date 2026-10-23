package app.leafolio.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ExportScope(
    val mode: String = "all",
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val selectedIds: Set<String> = emptySet(),
) {
    val valid get() = mode in setOf("all", "dates", "selected") &&
        (mode != "dates" || start == null || end == null || !start.isAfter(end))

    fun select(diaries: List<Diary>, zone: ZoneId = ZoneId.systemDefault()): List<Diary> {
        require(valid) { "导出日期范围无效" }
        return diaries.filter { diary ->
            !diary.isDraft && diary.deletedAt == null && when (mode) {
                "selected" -> diary.id in selectedIds
                "dates" -> {
                    val date = Instant.ofEpochMilli(diary.entryDate).atZone(zone).toLocalDate()
                    (start == null || date >= start) && (end == null || date <= end)
                }
                else -> true
            }
        }
    }
}
