package app.leafolio.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DiarySearchFilter(
    val query: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val mood: String? = null,
    val tags: Set<String> = emptySet(),
    val favoritesOnly: Boolean = false,
) {
    fun matches(diary: Diary, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (diary.isDraft || diary.deletedAt != null) return false
        val keyword = query.trim()
        if (keyword.isNotEmpty() && !diary.title.contains(keyword, true) && !diary.content.contains(keyword, true)) return false
        if (favoritesOnly && !diary.isFavorite) return false
        if (mood != null && diary.mood != mood) return false
        if (tags.isNotEmpty() && diary.tags.split('|').none { name -> tags.any { it.equals(name, true) } }) return false
        if (startDate != null || endDate != null) {
            val date = Instant.ofEpochMilli(diary.entryDate).atZone(zone).toLocalDate()
            if (startDate != null && date < startDate) return false
            if (endDate != null && date > endDate) return false
        }
        return true
    }
}
