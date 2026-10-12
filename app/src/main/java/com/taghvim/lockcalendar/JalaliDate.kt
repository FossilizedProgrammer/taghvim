package com.taghvim.lockcalendar

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * تبدیل میلادی به هجری شمسی بر پایهٔ تقویم نجومی (همان مبنای تقویم رسمی ایران).
 * الگوریتم برای سال‌های ۱۱۷۸ تا ۱۶۳۳ شمسی (۱۷۹۹ تا ۲۲۵۴ میلادی) دقیق است.
 */
object JalaliDate {

    val MONTHS_FA = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val WEEKDAYS_FA = mapOf(
        DayOfWeek.SATURDAY to "شنبه",
        DayOfWeek.SUNDAY to "یکشنبه",
        DayOfWeek.MONDAY to "دوشنبه",
        DayOfWeek.TUESDAY to "سه‌شنبه",
        DayOfWeek.WEDNESDAY to "چهارشنبه",
        DayOfWeek.THURSDAY to "پنجشنبه",
        DayOfWeek.FRIDAY to "جمعه"
    )

    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    private const val PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹"

    /** [سال شمسی، ماه ۱..۱۲، روز ۱..۳۱] را برمی‌گرداند */
    fun fromGregorian(date: LocalDate): IntArray {
        val jdn = toJulianDayNumber(date.year, date.monthValue, date.dayOfMonth)
        val gregorianYear = julianDayToGregorianYear(jdn)
        var jy = gregorianYear - 621
        val (leap, march) = jalCal(jy)
        val firstDayJdn = toJulianDayNumber(gregorianYear, 3, march)
        var k = jdn - firstDayJdn
        if (k in 0..185) {
            return intArrayOf(jy, 1 + k / 31, k % 31 + 1)
        }
        if (k >= 0) {
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (leap == 1) k += 1
        }
        return intArrayOf(jy, 7 + k / 30, k % 30 + 1)
    }

    fun weekdayFa(date: LocalDate): String = WEEKDAYS_FA.getValue(date.dayOfWeek)

    /** «۱۴۰۵» → «۱۴۰۵» با ارقام فارسی */
    fun toPersianDigits(value: String): String {
        val sb = StringBuilder(value.length)
        for (c in value) {
            sb.append(if (c in '0'..'9') PERSIAN_DIGITS[c - '0'] else c)
        }
        return sb.toString()
    }

    /**
     * مشخصات سال شمسی: (تعداد سال از کبیسهٔ قبلی، روز میلادیِ اول فروردین)
     * تقسیم و باقیمانده باید همان معنای JS (جزء صحیح به سمت صفر) را داشته باشد
     *؛ در کاتلین / و % برای اعداد صحیح دقیقاً همین رفتار را دارند.
     */
    private fun jalCal(jy: Int): Pair<Int, Int> {
        var leapJ = -14
        var jp = BREAKS[0]
        var jump = 0
        for (i in 1 until BREAKS.size) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += jump / 33 * 8 + jump % 33 / 4
            jp = jm
        }
        var n = jy - jp
        leapJ += n / 33 * 8 + (n % 33 + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1
        val gy = jy + 621
        val leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + (jump + 4) / 33 * 33
        var leap = ((n + 1) % 33 - 1) % 4
        if (leap == -1) leap = 4
        return leap to march
    }

    private fun toJulianDayNumber(gy: Int, gm: Int, gd: Int): Int {
        var d = (gy + (gm - 8) / 6 + 100100) * 1461 / 4 +
            (153 * ((gm + 9) % 12) + 2) / 5 + gd - 34840408
        d -= (gy + 100100 + (gm - 8) / 6) / 100 * 3 / 4
        d += 752
        return d
    }

    private fun julianDayToGregorianYear(jdn: Int): Int {
        var j = 4 * jdn + 139361631
        j += (4 * jdn + 183187720) / 146097 * 3 / 4 * 4 - 3908
        val i = j % 1461 / 4 * 5 + 308
        val gm = (i / 153) % 12 + 1
        return j / 1461 - 100100 + (8 - gm) / 6
    }
}
