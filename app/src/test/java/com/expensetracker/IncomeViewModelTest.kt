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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IncomeViewModelTest {

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

    @Test
    fun `income is null until set`() = runTest {
        val vm = ExpenseViewModel(FakeExpenseRepository(seedMonth = month), currentMonth = month)
        advanceUntilIdle()

        assertNull(vm.income.value)
    }

    @Test
    fun `setting a valid income updates the income state`() = runTest {
        val vm = ExpenseViewModel(FakeExpenseRepository(seedMonth = month), currentMonth = month)
        advanceUntilIdle()

        assertNull(vm.setIncome("50000"))
        advanceUntilIdle()

        assertEquals(50000.0, vm.income.value!!, 0.001)
    }

    @Test
    fun `invalid income input is rejected and does not change existing income`() = runTest {
        val vm = ExpenseViewModel(FakeExpenseRepository(seedMonth = month), currentMonth = month)
        advanceUntilIdle()
        vm.setIncome("50000")
        advanceUntilIdle()

        assertNotNull(vm.setIncome(""))
        assertNotNull(vm.setIncome("abc"))
        assertNotNull(vm.setIncome("-100"))
        advanceUntilIdle()

        assertEquals(50000.0, vm.income.value!!, 0.001)
    }

    @Test
    fun `income carries forward to a newly initialized month`() = runTest {
        val repo = FakeExpenseRepository(initialLimit = 1000.0, seedMonth = month)
        val vm = ExpenseViewModel(repo, currentMonth = month)
        advanceUntilIdle()
        vm.setIncome("60000")
        advanceUntilIdle()

        val nextMonthVm = ExpenseViewModel(repo, currentMonth = "2026-07")
        advanceUntilIdle()

        assertEquals(60000.0, nextMonthVm.income.value!!, 0.001)
    }
}
