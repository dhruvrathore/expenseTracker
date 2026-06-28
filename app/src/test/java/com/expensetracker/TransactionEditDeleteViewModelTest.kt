package com.expensetracker

import com.expensetracker.domain.AlertLevel
import com.expensetracker.domain.Transaction
import com.expensetracker.ui.ExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionEditDeleteViewModelTest {

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

    private fun viewModel() =
        ExpenseViewModel(FakeExpenseRepository(initialLimit = 1000.0, seedMonth = month), currentMonth = month)

    @Test
    fun `editing a transaction updates its fields and recomputes totals`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.addTransaction("200", "Lunch", "Food")
        advanceUntilIdle()

        val id = vm.uiState.value.transactions.first().id
        val ok = vm.updateTransaction(id, "350", "Dinner", "Food")
        advanceUntilIdle()

        assertTrue(ok)
        val txn = vm.uiState.value.transactions.first { it.id == id }
        assertEquals(350.0, txn.amount, 0.001)
        assertEquals("Dinner", txn.description)
        assertEquals(350.0, vm.uiState.value.totalSpent, 0.001)
        assertEquals(650.0, vm.uiState.value.remaining, 0.001)
    }

    @Test
    fun `editing with an invalid amount is rejected and leaves totals unchanged`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.addTransaction("200", "Lunch", "Food")
        advanceUntilIdle()
        val id = vm.uiState.value.transactions.first().id

        assertFalse(vm.updateTransaction(id, "", "x", "Food"))
        assertFalse(vm.updateTransaction(id, "abc", "x", "Food"))
        assertFalse(vm.updateTransaction(id, "0", "x", "Food"))
        advanceUntilIdle()

        assertEquals(200.0, vm.uiState.value.totalSpent, 0.001)
    }

    @Test
    fun `deleting a transaction removes it and restores the balance`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.addTransaction("200", "Lunch", "Food")
        vm.addTransaction("100", "Bus", "Transport")
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.transactions.size)
        assertEquals(300.0, vm.uiState.value.totalSpent, 0.001)

        val toDelete = vm.uiState.value.transactions.first { it.description == "Lunch" }.id
        vm.deleteTransaction(toDelete)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.transactions.size)
        assertEquals(100.0, vm.uiState.value.totalSpent, 0.001)
        assertEquals(900.0, vm.uiState.value.remaining, 0.001)
    }

    @Test
    fun `clearing the current month empties it but keeps the limit and other months`() = runTest {
        val repo = FakeExpenseRepository(initialLimit = 1000.0, seedMonth = month)
        repo.addTransaction("2026-05", Transaction(amount = 999.0, description = "Old", category = "Food"))
        val vm = ExpenseViewModel(repo, currentMonth = month)
        advanceUntilIdle()
        vm.addTransaction("200", "Lunch", "Food")
        vm.addTransaction("100", "Bus", "Transport")
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.transactions.size)

        vm.clearCurrentMonthTransactions()
        advanceUntilIdle()

        // Current month emptied, limit preserved.
        assertTrue(vm.uiState.value.transactions.isEmpty())
        assertEquals(0.0, vm.uiState.value.totalSpent, 0.001)
        assertEquals(1000.0, vm.uiState.value.remaining, 0.001)
        assertTrue(vm.uiState.value.hasLimit)

        // A previous month is untouched.
        val may = vm.observeMonth("2026-05").first()
        assertEquals(1, may.transactions.size)
    }

    @Test
    fun `editing a transaction up to its category limit re-triggers the alert`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.setCategoryLimit("Food", "500")
        advanceUntilIdle()
        vm.addTransaction("100", "Snack", "Food") // 20% -> no alert
        advanceUntilIdle()
        assertNull(vm.pendingAlert.value)

        val id = vm.uiState.value.transactions.first().id
        vm.updateTransaction(id, "480", "Big snack", "Food") // 96% -> NEARING
        advanceUntilIdle()

        assertEquals(AlertLevel.NEARING, vm.pendingAlert.value!!.level)
        assertEquals("Food", vm.pendingAlert.value!!.category)
    }

    @Test
    fun `editing into a different category alerts on the new category`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.setCategoryLimit("Transport", "300")
        advanceUntilIdle()
        vm.addTransaction("280", "Cab", "Food") // Food has no limit -> no alert
        advanceUntilIdle()
        assertNull(vm.pendingAlert.value)

        val id = vm.uiState.value.transactions.first().id
        vm.updateTransaction(id, "280", "Cab", "Transport") // 280/300 = 93% -> NEARING
        advanceUntilIdle()

        assertEquals(AlertLevel.NEARING, vm.pendingAlert.value!!.level)
        assertEquals("Transport", vm.pendingAlert.value!!.category)
    }

    @Test
    fun `editing that stays well under the threshold raises no alert`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.setCategoryLimit("Food", "500")
        advanceUntilIdle()
        vm.addTransaction("100", "x", "Food")
        advanceUntilIdle()

        val id = vm.uiState.value.transactions.first().id
        vm.updateTransaction(id, "150", "x", "Food") // 30% -> no alert
        advanceUntilIdle()

        assertNull(vm.pendingAlert.value)
    }
}
