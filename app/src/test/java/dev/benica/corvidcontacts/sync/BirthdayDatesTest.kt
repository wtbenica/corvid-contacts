// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.MonthDay

class BirthdayDatesTest {

    private val jan15 = MonthDay.of(1, 15)

    @Test
    fun `a full date is read with or without dashes, and the year is dropped`() {
        assertEquals(jan15, BirthdayDates.monthDay("1990-01-15"))
        assertEquals(jan15, BirthdayDates.monthDay("19900115"))
        assertEquals(jan15, BirthdayDates.monthDay(" 1990-01-15 "))
    }

    @Test
    fun `a date with no year is read in the forms vCard and the app write`() {
        assertEquals(jan15, BirthdayDates.monthDay("--01-15"))
        assertEquals(jan15, BirthdayDates.monthDay("--0115"))
        assertEquals(jan15, BirthdayDates.monthDay("01-15"))
    }

    @Test
    fun `a birthday with no year, as the app writes it, is no longer missed`() {
        val parsed = BirthdayDates.monthDay("--03-02")!!

        assertTrue(BirthdayDates.fallsOn(parsed, LocalDate.of(2027, 3, 2)))
    }

    @Test
    fun `text, blanks and dates that don't exist are not birthdays`() {
        listOf(null, "", "  ", "circa 1990", "5 January 1990", "1990", "1990-13-01", "1990-02-30", "--13-01", "--02-30").forEach {
            assertNull(it, BirthdayDates.monthDay(it))
        }
    }

    @Test
    fun `February 29 in a leap year is a real date`() {
        assertEquals(MonthDay.of(2, 29), BirthdayDates.monthDay("2000-02-29"))
        assertEquals(MonthDay.of(2, 29), BirthdayDates.monthDay("--02-29"))
    }

    @Test
    fun `a birthday falls on its own day and no other`() {
        assertTrue(BirthdayDates.fallsOn(jan15, LocalDate.of(2026, 1, 15)))
        assertTrue(BirthdayDates.fallsOn(jan15, LocalDate.of(2031, 1, 15)))
        assertFalse(BirthdayDates.fallsOn(jan15, LocalDate.of(2026, 1, 14)))
        assertFalse(BirthdayDates.fallsOn(jan15, LocalDate.of(2026, 2, 15)))
    }

    @Test
    fun `a February 29 birthday is celebrated on February 28 in other years, and on the 29th in a leap year`() {
        val leapDay = MonthDay.of(2, 29)

        assertTrue(BirthdayDates.fallsOn(leapDay, LocalDate.of(2027, 2, 28)))
        assertFalse(BirthdayDates.fallsOn(leapDay, LocalDate.of(2027, 3, 1)))
        assertTrue(BirthdayDates.fallsOn(leapDay, LocalDate.of(2028, 2, 29)))
        assertFalse(BirthdayDates.fallsOn(leapDay, LocalDate.of(2028, 2, 28)))
    }

    @Test
    fun `tomorrow rolls over the end of the year`() {
        val newYearsDay = MonthDay.of(1, 1)
        val tomorrow = LocalDate.of(2026, 12, 31).plusDays(1)

        assertTrue(BirthdayDates.fallsOn(newYearsDay, tomorrow))
    }
}
