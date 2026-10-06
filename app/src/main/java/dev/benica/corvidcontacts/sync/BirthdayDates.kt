// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.sync

import java.time.DateTimeException
import java.time.LocalDate
import java.time.MonthDay

/** Reads the birthdays contacts store, and says which day each one is celebrated. */
internal object BirthdayDates {
    private val FULL = Regex("""(\d{4})-?(\d{2})-?(\d{2})""")
    private val YEARLESS = Regex("""(?:--)?(\d{2})-?(\d{2})""")

    /**
     * The month and day of [birthday], or `null` if it isn't a date. A year, if there is one, is
     * dropped; these forms are read: `1990-01-15` and `19900115`, `--01-15` and `--0115` (no
     * year, as vCard writes it), and `01-15`. A date that doesn't exist, like February 30, isn't one.
     */
    fun monthDay(birthday: String?): MonthDay? {
        val text = birthday?.trim() ?: return null
        return try {
            FULL.matchEntire(text)?.let { LocalDate.of(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt()) }
                ?.let { MonthDay.from(it) }
                ?: YEARLESS.matchEntire(text)?.let { MonthDay.of(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        } catch (_: DateTimeException) {
            null
        }
    }

    /** Whether [birthday] is celebrated on [date]. A February 29 birthday is celebrated on February 28 in other years. */
    fun fallsOn(birthday: MonthDay, date: LocalDate): Boolean =
        if (birthday == LEAP_DAY && !date.isLeapYear) date.monthValue == 2 && date.dayOfMonth == 28
        else birthday == MonthDay.from(date)

    private val LEAP_DAY = MonthDay.of(2, 29)
}
