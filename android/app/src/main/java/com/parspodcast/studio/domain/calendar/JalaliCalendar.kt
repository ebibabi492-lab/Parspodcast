package com.parspodcast.studio.domain.calendar

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

data class JalaliDate(
    val year: Int,
    val month: Int, // 1 to 12
    val day: Int,   // 1 to 31
    val dayOfWeek: Int // 1 (Saturday) to 7 (Friday)
) {
    val monthName: String get() = JalaliCalendar.MONTH_NAMES[month - 1]
    val weekdayName: String get() = JalaliCalendar.WEEKDAY_NAMES[dayOfWeek - 1]

    fun toFormattedString(usePersianDigits: Boolean = true): String {
        val str = "$weekdayName $day $monthName $year"
        return if (usePersianDigits) JalaliCalendar.toPersianDigits(str) else str
    }

    fun toIsoPattern(usePersianDigits: Boolean = true): String {
        val str = String.format("%04d/%02d/%02d", year, month, day)
        return if (usePersianDigits) JalaliCalendar.toPersianDigits(str) else str
    }
}

object JalaliCalendar {
    val MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val WEEKDAY_NAMES = listOf(
        "شنبه", "یکشنبه", "دوشنبه",
        "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
    )

    fun toPersianDigits(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val chars = input.toCharArray()
        for (i in chars.indices) {
            val c = chars[i]
            if (c in '0'..'9') {
                chars[i] = persianDigits[c - '0']
            }
        }
        return String(chars)
    }

    fun fromDate(date: Date, timeZone: TimeZone = TimeZone.getTimeZone("Asia/Tehran")): JalaliDate {
        val cal = Calendar.getInstance(timeZone).apply { time = date }
        val gYear = cal.get(Calendar.YEAR)
        val gMonth = cal.get(Calendar.MONTH) + 1
        val gDay = cal.get(Calendar.DAY_OF_MONTH)
        val calDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday=1, Saturday=7

        // Map Java Calendar day of week to Persian (Saturday=1 ... Friday=7)
        val persianDayOfWeek = when (calDayOfWeek) {
            Calendar.SATURDAY -> 1
            Calendar.SUNDAY -> 2
            Calendar.MONDAY -> 3
            Calendar.TUESDAY -> 4
            Calendar.WEDNESDAY -> 5
            Calendar.THURSDAY -> 6
            Calendar.FRIDAY -> 7
            else -> 1
        }

        return gregorianToJalali(gYear, gMonth, gDay, persianDayOfWeek)
    }

    fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int, dayOfWeek: Int = 1): JalaliDate {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gy = gYear - 1600
        var gm = gMonth - 1
        var gd = gDay - 1

        var gDayNo = 365 * gy + ((gy + 3) / 4) - ((gy + 99) / 100) + ((gy + 399) / 400)
        for (i in 0 until gm) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++ // leap year
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        while (jm < 11 && jDayNo >= jDaysInMonth[jm]) {
            jDayNo -= jDaysInMonth[jm]
            jm++
        }
        val jd = jDayNo + 1

        return JalaliDate(year = jy, month = jm + 1, day = jd, dayOfWeek = dayOfWeek)
    }
}
