package com.expensetracker

import com.expensetracker.domain.LimitValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitValidatorTest {

    @Test
    fun `a category limit fits when the total stays within the monthly limit`() {
        val current = mapOf("Food" to 400.0, "Transport" to 300.0)
        // Raising Food to 600 -> 600 + 300 = 900 <= 1000
        assertTrue(LimitValidator.categoryLimitFits(1000.0, current, "Food", 600.0))
    }

    @Test
    fun `a category limit does not fit when the total exceeds the monthly limit`() {
        val current = mapOf("Food" to 400.0, "Transport" to 300.0)
        // Raising Food to 800 -> 800 + 300 = 1100 > 1000
        assertFalse(LimitValidator.categoryLimitFits(1000.0, current, "Food", 800.0))
    }

    @Test
    fun `remaining headroom excludes the category being edited`() {
        val current = mapOf("Food" to 400.0, "Transport" to 300.0)
        // Other categories sum to 300 (Transport), so 700 is available for Food.
        assertEquals(700.0, LimitValidator.categoryLimitsRemaining(1000.0, current, "Food"), 0.001)
    }

    @Test
    fun `setting the first category limit fits up to the whole monthly limit`() {
        assertTrue(LimitValidator.categoryLimitFits(1000.0, emptyMap(), "Food", 1000.0))
        assertFalse(LimitValidator.categoryLimitFits(1000.0, emptyMap(), "Food", 1000.01))
    }

    @Test
    fun `a monthly limit fits only when it covers the sum of existing category limits`() {
        val current = mapOf("Food" to 400.0, "Transport" to 300.0) // sum 700
        assertTrue(LimitValidator.monthlyLimitFits(700.0, current))
        assertTrue(LimitValidator.monthlyLimitFits(900.0, current))
        assertFalse(LimitValidator.monthlyLimitFits(600.0, current))
    }
}
