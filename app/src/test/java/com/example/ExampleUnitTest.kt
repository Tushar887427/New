package com.example

import com.example.data.WorkedFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun dateKeyValidation_acceptsRealDatesAndRejectsMalformedOrInvalidCalendarDates() {
        assertTrue(WorkedFormatters.isValidDateKey("2026-11-24"))
        assertTrue(WorkedFormatters.isValidDateKey("2024-02-29")) // leap year
        assertFalse(WorkedFormatters.isValidDateKey("2026-02-29")) // non-leap year
        assertFalse(WorkedFormatters.isValidDateKey("2026-13-01"))
        assertFalse(WorkedFormatters.isValidDateKey("2026-04-31"))
        assertFalse(WorkedFormatters.isValidDateKey("24-11-2026"))
        assertFalse(WorkedFormatters.isValidDateKey(""))
    }

    @Test
    fun time24NormalizationAndMinutes_handlesSingleDigitHourAndRejectsOutOfRange() {
        assertEquals("09:30", WorkedFormatters.normalizeTime24("9:30"))
        assertEquals("09:30", WorkedFormatters.normalizeTime24("09:30"))
        assertEquals("00:00", WorkedFormatters.normalizeTime24("0:00"))
        assertEquals("23:59", WorkedFormatters.normalizeTime24("23:59"))
        assertNull(WorkedFormatters.normalizeTime24("24:00"))
        assertNull(WorkedFormatters.normalizeTime24("12:60"))
        assertNull(WorkedFormatters.normalizeTime24("abc"))

        // Verify numeric comparison fixes string comparison bug ("10:00" > "9:30")
        val shiftMins = WorkedFormatters.time24ToMinutes("9:30")!!
        val punchMins = WorkedFormatters.time24ToMinutes("10:00")!!
        assertTrue(punchMins > shiftMins)
        assertEquals("10:00 AM", WorkedFormatters.formatTime12h("10:00"))
        assertEquals("12:15 AM", WorkedFormatters.formatTime12h("00:15"))
        assertEquals("05:05 PM", WorkedFormatters.formatTime12h("17:05"))
    }

    @Test
    fun enumerateDateRange_boundsAndOrdersCorrectly() {
        assertEquals(
            listOf("2026-11-24", "2026-11-25", "2026-11-26"),
            WorkedFormatters.enumerateDateRange("2026-11-24", "2026-11-26")
        )
        assertEquals(emptyList<String>(), WorkedFormatters.enumerateDateRange("2026-11-26", "2026-11-24"))
        assertEquals(emptyList<String>(), WorkedFormatters.enumerateDateRange("bad", "2026-11-26"))
    }
}
