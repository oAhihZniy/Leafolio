package app.leafolio.data

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class DailyReminderTest {
    @Test fun disabledHasNoAlarm() { assertNull(DailyReminder.nextDue(-1, ZonedDateTime.now())) }
    @Test fun nextLocalTimeIsTomorrowWhenPassed() {
        val now = ZonedDateTime.of(2026, 10, 23, 21, 0, 0, 0, ZoneId.of("Asia/Hong_Kong"))
        assertEquals(now.plusDays(1), DailyReminder.nextDue(1260, now))
        assertEquals(now.plusHours(1), DailyReminder.nextDue(1320, now))
    }
    @Test fun daylightSavingGapStillHasOneFutureLocalReminder() {
        val now = ZonedDateTime.of(2026, 3, 8, 1, 0, 0, 0, ZoneId.of("America/New_York"))
        val next = DailyReminder.nextDue(150, now)!!
        assertEquals(3, next.hour)
        assertEquals(30, next.minute)
        assertTrue(next.isAfter(now))
    }
}
