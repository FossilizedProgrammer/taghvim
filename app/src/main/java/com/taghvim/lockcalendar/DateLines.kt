package com.taghvim.lockcalendar

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** ساخت متن دو خط نوتیفیکیشن: خط شمسی و خط میلادی */
object DateLines {

    private val GREGORIAN_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

    /** نمونه: سه‌شنبه ۱۴ مهر ۱۴۰۵ */
    fun persianLine(date: LocalDate): String {
        val j = JalaliDate.fromGregorian(date)
        return JalaliDate.weekdayFa(date) + " " +
            JalaliDate.toPersianDigits(j[2].toString()) + " " +
            JalaliDate.MONTHS_FA[j[1] - 1] + " " +
            JalaliDate.toPersianDigits(j[0].toString())
    }

    /** نمونه: Tue, 6 Oct 2026 */
    fun gregorianLine(date: LocalDate): String = GREGORIAN_FORMAT.format(date)
}
