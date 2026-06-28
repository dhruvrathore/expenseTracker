package com.expensetracker.util

import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

/** Formats a "yyyy-MM" key as a readable label, e.g. "2026-06" -> "June 2026". */
fun String.asMonthLabel(): String =
    runCatching { YearMonth.parse(this).format(monthFormatter) }.getOrDefault(this)

/** Formats epoch millis as a short day label, e.g. "19 Jun". */
fun Long.asDayLabel(): String {
    val date = java.time.Instant.ofEpochMilli(this)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return "${date.dayOfMonth} $month"
}
