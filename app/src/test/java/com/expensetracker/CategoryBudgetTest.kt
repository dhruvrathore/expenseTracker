package com.expensetracker

import com.expensetracker.domain.BudgetCalculator
import com.expensetracker.domain.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryBudgetTest {

    private fun txn(amount: Double, category: String) =
        Transaction(amount = amount, description = "", category = category)

    @Test
    fun `spentByCategory groups and sums amounts per category`() {
        val txns = listOf(
            txn(100.0, "Food"),
            txn(50.0, "Food"),
            txn(200.0, "Transport")
        )

        val spent = BudgetCalculator.spentByCategory(txns)

        assertEquals(150.0, spent["Food"]!!, 0.001)
        assertEquals(200.0, spent["Transport"]!!, 0.001)
        assertNull(spent["Rent"])
    }

    @Test
    fun `categorySummaries reports limit, spent and remaining for each category`() {
        val categories = listOf("Food", "Transport", "Rent")
        val limits = mapOf("Food" to 1000.0, "Transport" to 300.0)
        val txns = listOf(
            txn(250.0, "Food"),
            txn(400.0, "Transport") // over its 300 limit
        )

        val summaries = BudgetCalculator.categorySummaries(categories, limits, txns)
            .associateBy { it.category }

        // Food: has a limit, partially spent
        assertEquals(1000.0, summaries["Food"]!!.limit!!, 0.001)
        assertEquals(250.0, summaries["Food"]!!.spent, 0.001)
        assertEquals(750.0, summaries["Food"]!!.remaining!!, 0.001)
        assertFalse(summaries["Food"]!!.isOverBudget)

        // Transport: over budget
        assertEquals(-100.0, summaries["Transport"]!!.remaining!!, 0.001)
        assertTrue(summaries["Transport"]!!.isOverBudget)

        // Rent: no limit set -> null limit/remaining, zero spent, never over budget
        assertNull(summaries["Rent"]!!.limit)
        assertNull(summaries["Rent"]!!.remaining)
        assertEquals(0.0, summaries["Rent"]!!.spent, 0.001)
        assertFalse(summaries["Rent"]!!.isOverBudget)
    }
}
