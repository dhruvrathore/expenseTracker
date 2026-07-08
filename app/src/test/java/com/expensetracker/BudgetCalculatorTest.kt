package com.expensetracker

import com.expensetracker.domain.BudgetCalculator
import com.expensetracker.domain.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetCalculatorTest {

    private fun txn(amount: Double) =
        Transaction(amount = amount, description = "", category = "Other")

    @Test
    fun `remaining equals limit when there are no transactions`() {
        assertEquals(1000.0, BudgetCalculator.remaining(1000.0, emptyList()), 0.001)
    }

    @Test
    fun `total spent is the sum of all transaction amounts`() {
        val txns = listOf(txn(200.0), txn(150.0))
        assertEquals(350.0, BudgetCalculator.totalSpent(txns), 0.001)
    }

    @Test
    fun `remaining is limit minus the sum of transactions`() {
        val txns = listOf(txn(200.0), txn(150.5))
        assertEquals(649.5, BudgetCalculator.remaining(1000.0, txns), 0.001)
    }

    @Test
    fun `remaining goes negative and flags over budget when overspent`() {
        val txns = listOf(txn(1200.0))
        assertEquals(-200.0, BudgetCalculator.remaining(1000.0, txns), 0.001)
        assertTrue(BudgetCalculator.isOverBudget(1000.0, txns))
    }

    @Test
    fun `a zero limit is handled and is not flagged as over budget`() {
        assertEquals(0.0, BudgetCalculator.remaining(0.0, emptyList()), 0.001)
        assertFalse(BudgetCalculator.isOverBudget(0.0, emptyList()))
    }

    @Test
    fun `percent of income divides amount by income`() {
        assertEquals(10.0, BudgetCalculator.percentOfIncome(2000.0, 20000.0)!!, 0.001)
        assertEquals(100.0, BudgetCalculator.percentOfIncome(500.0, 500.0)!!, 0.001)
    }

    @Test
    fun `percent of income is null when amount or income is unset or income is non-positive`() {
        assertEquals(null, BudgetCalculator.percentOfIncome(null, 20000.0))
        assertEquals(null, BudgetCalculator.percentOfIncome(2000.0, null))
        assertEquals(null, BudgetCalculator.percentOfIncome(2000.0, 0.0))
        assertEquals(null, BudgetCalculator.percentOfIncome(2000.0, -1.0))
    }
}
