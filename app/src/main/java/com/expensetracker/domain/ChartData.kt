package com.expensetracker.domain

import java.time.Instant
import java.time.ZoneId

/** One slice of the spending-by-category chart. */
data class CategorySlice(
    val category: String,
    val amount: Double,
    val fraction: Double
)

/** Total spent in one week of the month (weeks of 7 days, 1-based). */
data class WeeklyTotal(
    val week: Int,
    val startDay: Int,
    val endDay: Int,
    val amount: Double
)

/** Pure aggregation for the charts screen — no Android/Compose dependencies. */
object ChartData {

    /** Categories with spending, sorted by amount desc, each with its share of the total. */
    fun categorySlices(categories: List<CategorySummary>): List<CategorySlice> {
        val spending = categories.filter { it.spent > 0.0 }
        val total = spending.sumOf { it.spent }
        if (total <= 0.0) return emptyList()
        return spending
            .sortedByDescending { it.spent }
            .map { CategorySlice(it.category, it.spent, it.spent / total) }
    }

    /** Spending per week of the month (7-day buckets), zero-filled for every week. */
    fun weeklyTotals(
        transactions: List<Transaction>,
        daysInMonth: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<WeeklyTotal> {
        val byWeek = transactions.groupBy { txn ->
            val day = Instant.ofEpochMilli(txn.timestamp).atZone(zone).dayOfMonth
            (day - 1) / 7 + 1
        }.mapValues { (_, txns) -> txns.sumOf { it.amount } }

        val weekCount = (daysInMonth + 6) / 7 // ceil(daysInMonth / 7)
        return (1..weekCount).map { week ->
            WeeklyTotal(
                week = week,
                startDay = (week - 1) * 7 + 1,
                endDay = minOf(week * 7, daysInMonth),
                amount = byWeek[week] ?: 0.0
            )
        }
    }

    /** The [count] largest transactions by amount, descending. */
    fun topTransactions(transactions: List<Transaction>, count: Int = 5): List<Transaction> =
        transactions.sortedByDescending { it.amount }.take(count)
}
