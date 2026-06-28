package com.expensetracker

import com.expensetracker.domain.AlertLevel
import com.expensetracker.domain.BudgetCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertLevelTest {

    @Test
    fun `no alert when spending is well under the limit`() {
        assertNull(BudgetCalculator.alertLevel(spent = 500.0, limit = 1000.0))
    }

    @Test
    fun `nearing alert at or above 90 percent but under the limit`() {
        assertEquals(AlertLevel.NEARING, BudgetCalculator.alertLevel(spent = 900.0, limit = 1000.0))
        assertEquals(AlertLevel.NEARING, BudgetCalculator.alertLevel(spent = 950.0, limit = 1000.0))
    }

    @Test
    fun `over alert when the limit is reached exactly`() {
        assertEquals(AlertLevel.OVER, BudgetCalculator.alertLevel(spent = 1000.0, limit = 1000.0))
    }

    @Test
    fun `over alert when spending exceeds the limit`() {
        assertEquals(AlertLevel.OVER, BudgetCalculator.alertLevel(spent = 1200.0, limit = 1000.0))
    }

    @Test
    fun `no alert when the limit is zero or negative`() {
        assertNull(BudgetCalculator.alertLevel(spent = 100.0, limit = 0.0))
    }
}
