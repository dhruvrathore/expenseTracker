package com.expensetracker

import app.cash.turbine.test
import com.expensetracker.domain.AlertLevel
import com.expensetracker.domain.Transaction
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonthAndAlertViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a new month inherits last month's limits with spending reset to zero`() = runTest {
        val repo = FakeExpenseRepository(initialLimit = 1000.0, seedMonth = "2026-05")
        repo.setCategoryLimit("2026-05", "Food", 400.0)
        repo.addTransaction("2026-05", Transaction(amount = 200.0, description = "x", category = "Food"))

        val vm = ExpenseViewModel(repo, currentMonth = "2026-06")
        advanceUntilIdle()

        // Limits carried forward...
        assertTrue(vm.uiState.value.hasLimit)
        assertEquals(1000.0, vm.uiState.value.monthlyLimit, 0.001)
        val food = vm.categoryState.value.first { it.category == "Food" }
        assertEquals(400.0, food.limit!!, 0.001)
        // ...but spending reset.
        assertEquals(0.0, vm.uiState.value.totalSpent, 0.001)
        assertEquals(0.0, food.spent, 0.001)

        // The prior month is available as history.
        assertTrue(vm.historyMonths.value.contains("2026-05"))
    }

    @Test
    fun `observing a past month returns its own totals and transactions`() = runTest {
        val repo = FakeExpenseRepository(initialLimit = 1000.0, seedMonth = "2026-05")
        repo.addTransaction("2026-05", Transaction(amount = 200.0, description = "Old", category = "Food"))
        val vm = ExpenseViewModel(repo, currentMonth = "2026-06")
        advanceUntilIdle()

        vm.observeMonth("2026-05").test {
            val view = awaitItem()
            assertEquals(1000.0, view.monthlyLimit, 0.001)
            assertEquals(200.0, view.totalSpent, 0.001)
            assertEquals(1, view.transactions.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `adding a transaction that nears a category limit raises a NEARING alert`() = runTest {
        val vm = ExpenseViewModel(FakeExpenseRepository(initialLimit = 5000.0, seedMonth = "2026-06"), currentMonth = "2026-06")
        advanceUntilIdle()
        vm.setCategoryLimit("Food", "1000")
        advanceUntilIdle()
        assertNull(vm.pendingAlert.value)

        vm.addTransaction("950", "Big grocery run", "Food") // 95% of 1000
        advanceUntilIdle()

        val alert = vm.pendingAlert.value
        assertEquals("Food", alert!!.category)
        assertEquals(AlertLevel.NEARING, alert.level)
    }

    @Test
    fun `adding a transaction that exceeds a category limit raises an OVER alert`() = runTest {
        val vm = ExpenseViewModel(FakeExpenseRepository(initialLimit = 5000.0, seedMonth = "2026-06"), currentMonth = "2026-06")
        advanceUntilIdle()
        vm.setCategoryLimit("Transport", "300")
        advanceUntilIdle()

        vm.addTransaction("400", "Airport cab", "Transport")
        advanceUntilIdle()

        val alert = vm.pendingAlert.value
        assertEquals(AlertLevel.OVER, alert!!.level)
        assertEquals("Transport", alert.category)

        vm.clearAlert()
        assertNull(vm.pendingAlert.value)
    }

    @Test
    fun `no alert when the category has no limit or stays well under it`() = runTest {
        val vm = ExpenseViewModel(FakeExpenseRepository(initialLimit = 5000.0, seedMonth = "2026-06"), currentMonth = "2026-06")
        advanceUntilIdle()

        // No category limit set at all.
        vm.addTransaction("100", "Snacks", "Food")
        advanceUntilIdle()
        assertNull(vm.pendingAlert.value)

        // Limit set, but spending stays under 90%.
        vm.setCategoryLimit("Utilities", "1000")
        advanceUntilIdle()
        vm.addTransaction("100", "Electricity", "Utilities")
        advanceUntilIdle()
        assertNull(vm.pendingAlert.value)
    }
}
