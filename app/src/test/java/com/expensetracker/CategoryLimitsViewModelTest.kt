package com.expensetracker

import com.expensetracker.domain.Categories
import com.expensetracker.ui.ExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryLimitsViewModelTest {

    private val month = "2026-06"
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(initialLimit: Double? = null) =
        ExpenseViewModel(FakeExpenseRepository(initialLimit, seedMonth = month), currentMonth = month)

    @Test
    fun `category state lists every default category with no limit initially`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val summaries = vm.categoryState.value
        assertEquals(Categories.DEFAULTS.size, summaries.size)
        assertTrue(summaries.all { it.limit == null })
        assertTrue(summaries.all { it.spent == 0.0 })
    }

    @Test
    fun `setting a category limit and spending updates that category summary`() = runTest {
        val vm = viewModel(initialLimit = 5000.0)
        advanceUntilIdle()

        assertNull(vm.setCategoryLimit("Food", "1000"))
        vm.addTransaction("250", "Lunch", "Food")
        advanceUntilIdle()

        val food = vm.categoryState.value.first { it.category == "Food" }
        assertEquals(1000.0, food.limit!!, 0.001)
        assertEquals(250.0, food.spent, 0.001)
        assertEquals(750.0, food.remaining!!, 0.001)
        assertFalse(food.isOverBudget)
    }

    @Test
    fun `a category is flagged over budget when spending exceeds its limit`() = runTest {
        val vm = viewModel(initialLimit = 5000.0)
        advanceUntilIdle()

        vm.setCategoryLimit("Transport", "300")
        advanceUntilIdle()
        vm.addTransaction("400", "Cab", "Transport")
        advanceUntilIdle()

        val transport = vm.categoryState.value.first { it.category == "Transport" }
        assertTrue(transport.isOverBudget)
        assertEquals(-100.0, transport.remaining!!, 0.001)
    }

    @Test
    fun `invalid category limits are rejected`() = runTest {
        val vm = viewModel(initialLimit = 5000.0)
        advanceUntilIdle()

        assertNotNull(vm.setCategoryLimit("Food", ""))
        assertNotNull(vm.setCategoryLimit("Food", "abc"))
        assertNotNull(vm.setCategoryLimit("Food", "-10"))
        advanceUntilIdle()

        val food = vm.categoryState.value.first { it.category == "Food" }
        assertNull(food.limit)
    }

    @Test
    fun `a category limit cannot be set before a monthly limit exists`() = runTest {
        val vm = viewModel() // no monthly limit
        advanceUntilIdle()

        assertNotNull(vm.setCategoryLimit("Food", "100"))
        advanceUntilIdle()

        assertNull(vm.categoryState.value.first { it.category == "Food" }.limit)
    }

    @Test
    fun `category limits whose sum exceeds the monthly limit are rejected`() = runTest {
        val vm = viewModel(initialLimit = 1000.0)
        advanceUntilIdle()

        assertNull(vm.setCategoryLimit("Food", "700"))
        advanceUntilIdle()
        // 700 + 400 = 1100 > 1000 -> rejected
        assertNotNull(vm.setCategoryLimit("Transport", "400"))
        advanceUntilIdle()

        assertNull(vm.categoryState.value.first { it.category == "Transport" }.limit)
        // But a value that fits is accepted: 700 + 300 = 1000
        assertNull(vm.setCategoryLimit("Transport", "300"))
        advanceUntilIdle()
        assertEquals(300.0, vm.categoryState.value.first { it.category == "Transport" }.limit!!, 0.001)
    }

    @Test
    fun `monthly limit cannot drop below the sum of category limits`() = runTest {
        val vm = viewModel(initialLimit = 1000.0)
        advanceUntilIdle()
        vm.setCategoryLimit("Food", "600")
        advanceUntilIdle()

        // Lowering monthly limit to 500 < 600 (sum of category limits) -> rejected
        assertNotNull(vm.setMonthlyLimit("500"))
        advanceUntilIdle()
        assertEquals(1000.0, vm.uiState.value.monthlyLimit, 0.001)

        // 700 >= 600 is fine
        assertNull(vm.setMonthlyLimit("700"))
        advanceUntilIdle()
        assertEquals(700.0, vm.uiState.value.monthlyLimit, 0.001)
    }
}
