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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DescriptionSuggestionsViewModelTest {

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
    fun `description suggestions reflect past entries, distinct and most recent first`() = runTest {
        val vm = ExpenseViewModel(
            FakeExpenseRepository(initialLimit = 5000.0, seedMonth = month),
            currentMonth = month
        )
        advanceUntilIdle()

        vm.addTransaction("100", "Coffee", "Food")
        advanceUntilIdle()
        vm.addTransaction("200", "Lunch", "Food")
        advanceUntilIdle()
        vm.addTransaction("50", "Coffee", "Food") // repeat -> should move to front, no dupes
        advanceUntilIdle()

        assertEquals(listOf("Coffee", "Lunch"), vm.descriptionSuggestions.value)
    }
}
