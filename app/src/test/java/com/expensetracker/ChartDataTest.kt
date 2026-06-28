package com.expensetracker

import com.expensetracker.domain.ChartData
import com.expensetracker.domain.CategorySummary
import com.expensetracker.domain.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class ChartDataTest {

    private fun summary(category: String, spent: Double, limit: Double? = null) =
        CategorySummary(
            category = category,
            limit = limit,
            spent = spent,
            remaining = limit?.let { it - spent },
            isOverBudget = limit != null && spent > limit
        )

    @Test
    fun `category slices keep only spent categories, sorted desc, with fractions`() {
        val categories = listOf(
            summary("Food", 300.0),
            summary("Transport", 100.0),
            summary("Rent", 0.0) // no spend -> excluded
        )

        val slices = ChartData.categorySlices(categories)

        assertEquals(2, slices.size)
        assertEquals("Food", slices[0].category)
        assertEquals(300.0, slices[0].amount, 0.001)
        assertEquals(0.75, slices[0].fraction, 0.001) // 300 / 400
        assertEquals("Transport", slices[1].category)
        assertEquals(0.25, slices[1].fraction, 0.001)
    }

    @Test
    fun `category slices are empty when nothing was spent`() {
        val categories = listOf(summary("Food", 0.0), summary("Rent", 0.0))
        assertEquals(emptyList<Any>(), ChartData.categorySlices(categories))
    }

    private fun tsOnDay(day: Int): Long =
        java.time.LocalDate.of(2026, 6, day)
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun `weekly totals bucket days into weeks of seven, zero-filled with clamped ranges`() {
        val txns = listOf(
            Transaction(amount = 100.0, description = "a", category = "Food", timestamp = tsOnDay(3)),  // week 1
            Transaction(amount = 50.0, description = "b", category = "Food", timestamp = tsOnDay(7)),   // week 1
            Transaction(amount = 200.0, description = "c", category = "Transport", timestamp = tsOnDay(12)), // week 2
            Transaction(amount = 70.0, description = "d", category = "Food", timestamp = tsOnDay(30))    // week 5
        )

        val weeks = ChartData.weeklyTotals(txns, daysInMonth = 30, zone = ZoneOffset.UTC)

        assertEquals(5, weeks.size) // ceil(30/7)
        assertEquals(150.0, weeks[0].amount, 0.001)
        assertEquals(1, weeks[0].startDay)
        assertEquals(7, weeks[0].endDay)
        assertEquals(200.0, weeks[1].amount, 0.001)
        assertEquals(0.0, weeks[2].amount, 0.001) // week 3, no spend
        assertEquals(70.0, weeks[4].amount, 0.001)
        assertEquals(29, weeks[4].startDay)
        assertEquals(30, weeks[4].endDay) // clamped to days in month
    }

    @Test
    fun `weekly totals week count adapts to month length`() {
        assertEquals(5, ChartData.weeklyTotals(emptyList(), daysInMonth = 31, zone = ZoneOffset.UTC).size)
        assertEquals(4, ChartData.weeklyTotals(emptyList(), daysInMonth = 28, zone = ZoneOffset.UTC).size)
    }

    @Test
    fun `top transactions are sorted by amount descending and capped`() {
        val txns = listOf(
            Transaction(amount = 100.0, description = "a", category = "Food", timestamp = tsOnDay(1)),
            Transaction(amount = 500.0, description = "b", category = "Rent", timestamp = tsOnDay(2)),
            Transaction(amount = 250.0, description = "c", category = "Food", timestamp = tsOnDay(3)),
            Transaction(amount = 30.0, description = "d", category = "Food", timestamp = tsOnDay(4))
        )

        val top = ChartData.topTransactions(txns, count = 2)

        assertEquals(2, top.size)
        assertEquals(500.0, top[0].amount, 0.001)
        assertEquals(250.0, top[1].amount, 0.001)
    }
}
