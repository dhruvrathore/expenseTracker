package com.expensetracker

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
class ExpenseViewModelTest {

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
    fun `initial state reflects the stored limit with zero spent`() = runTest {
        val vm = viewModel(initialLimit = 1000.0)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.hasLimit)
        assertEquals(1000.0, state.monthlyLimit, 0.001)
        assertEquals(0.0, state.totalSpent, 0.001)
        assertEquals(1000.0, state.remaining, 0.001)
        assertFalse(state.isOverBudget)
    }

    @Test
    fun `state has no limit when none is stored`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.hasLimit)
    }

    @Test
    fun `adding a transaction decreases the remaining balance`() = runTest {
        val vm = viewModel(initialLimit = 1000.0)
        advanceUntilIdle()

        val added = vm.addTransaction("200", "Groceries", "Food")
        advanceUntilIdle()

        assertTrue(added)
        assertEquals(200.0, vm.uiState.value.totalSpent, 0.001)
        assertEquals(800.0, vm.uiState.value.remaining, 0.001)
        assertEquals(1, vm.uiState.value.transactions.size)
    }

    @Test
    fun `setting a new limit recomputes the remaining balance`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.hasLimit)

        assertNull(vm.setMonthlyLimit("500"))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.hasLimit)
        assertEquals(500.0, vm.uiState.value.monthlyLimit, 0.001)
        assertEquals(500.0, vm.uiState.value.remaining, 0.001)
    }

    @Test
    fun `overspending flags the state as over budget`() = runTest {
        val vm = viewModel(initialLimit = 100.0)
        advanceUntilIdle()

        vm.addTransaction("150", "Big buy", "Shopping")
        advanceUntilIdle()

        assertEquals(-50.0, vm.uiState.value.remaining, 0.001)
        assertTrue(vm.uiState.value.isOverBudget)
    }

    @Test
    fun `invalid transaction amounts are rejected and do not change totals`() = runTest {
        val vm = viewModel(initialLimit = 1000.0)
        advanceUntilIdle()

        assertFalse(vm.addTransaction("", "x", "Food"))
        assertFalse(vm.addTransaction("abc", "x", "Food"))
        assertFalse(vm.addTransaction("0", "x", "Food"))
        assertFalse(vm.addTransaction("-5", "x", "Food"))
        advanceUntilIdle()
        assertEquals(1000.0, vm.uiState.value.remaining, 0.001)

        assertTrue(vm.addTransaction("5", "x", "Food"))
        advanceUntilIdle()
        assertEquals(995.0, vm.uiState.value.remaining, 0.001)
    }

    @Test
    fun `an invalid monthly limit is rejected`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertNotNull(vm.setMonthlyLimit(""))
        assertNotNull(vm.setMonthlyLimit("abc"))
        assertNotNull(vm.setMonthlyLimit("-100"))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.hasLimit)
    }
}
