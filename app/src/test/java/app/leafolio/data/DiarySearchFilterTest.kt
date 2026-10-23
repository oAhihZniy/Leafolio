package app.leafolio.data

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiarySearchFilterTest {
    private val zone = ZoneId.of("Asia/Hong_Kong")
    private val day = LocalDate.of(2026, 10, 23)
    private val diary = Diary(
        id = "example", title = "旅行 Leafolio", content = "海边的一天",
        entryDate = day.atStartOfDay(zone).toInstant().toEpochMilli(),
        createdAt = 0, updatedAt = 0, isDraft = false,
        isFavorite = true, mood = "开心", tags = "旅行|生活",
    )

    @Test fun keywordMatchesTitleAndBodyIgnoringCaseAndSurroundingSpaces() {
        assertTrue(DiarySearchFilter(query = " leafOLIO ").matches(diary, zone))
        assertTrue(DiarySearchFilter(query = "海边").matches(diary, zone))
        assertTrue(DiarySearchFilter(query = "  ").matches(diary, zone))
        assertFalse(DiarySearchFilter(query = "不存在").matches(diary, zone))
    }

    @Test fun datesIncludeBothBoundariesInLocalTime() {
        val filter = DiarySearchFilter(startDate = day, endDate = day)
        assertTrue(filter.matches(diary, zone))
        assertTrue(filter.matches(diary.copy(entryDate = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1), zone))
        assertFalse(filter.matches(diary.copy(entryDate = diary.entryDate - 1), zone))
        assertFalse(filter.matches(diary.copy(entryDate = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()), zone))
    }

    @Test fun dateRangeCanHaveOnlyOneBoundary() {
        assertTrue(DiarySearchFilter(startDate = day.minusDays(1)).matches(diary, zone))
        assertTrue(DiarySearchFilter(endDate = day.plusDays(1)).matches(diary, zone))
        assertFalse(DiarySearchFilter(startDate = day.plusDays(1)).matches(diary, zone))
        assertFalse(DiarySearchFilter(endDate = day.minusDays(1)).matches(diary, zone))
    }

    @Test fun selectedTagsMatchAnyWholeTag() {
        assertTrue(DiarySearchFilter(tags = setOf("工作", "旅行")).matches(diary, zone))
        assertFalse(DiarySearchFilter(tags = setOf("旅")).matches(diary, zone))
        assertTrue(DiarySearchFilter(tags = setOf("work")).matches(diary.copy(tags = "Work|生活"), zone))
    }

    @Test fun allDifferentFilterTypesMustMatchTogether() {
        val filter = DiarySearchFilter(query = "海边", startDate = day, endDate = day, mood = "开心", tags = setOf("旅行"), favoritesOnly = true)
        assertTrue(filter.matches(diary, zone))
        assertFalse(filter.matches(diary.copy(isFavorite = false), zone))
        assertFalse(filter.matches(diary.copy(mood = "平静"), zone))
        assertFalse(filter.matches(diary.copy(tags = "工作"), zone))
        assertFalse(filter.copy(query = "山间").matches(diary, zone))
    }

    @Test fun draftsAndTrashNeverAppearEvenWithNoFilters() {
        val filter = DiarySearchFilter()
        assertTrue(filter.matches(diary, zone))
        assertFalse(filter.matches(diary.copy(isDraft = true), zone))
        assertFalse(filter.matches(diary.copy(deletedAt = 1), zone))
    }
}
