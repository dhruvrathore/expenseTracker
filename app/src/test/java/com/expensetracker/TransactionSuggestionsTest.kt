package com.expensetracker

import com.expensetracker.domain.Transaction
import com.expensetracker.domain.TransactionSuggestions
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionSuggestionsTest {

    private fun txn(description: String, timestamp: Long) =
        Transaction(amount = 1.0, description = description, category = "Food", timestamp = timestamp)

    @Test
    fun `returns distinct descriptions, most recent first, ignoring blanks`() {
        val txns = listOf(
            txn("Coffee", 100),
            txn("", 200),        // blank -> ignored
            txn("Lunch", 300),
            txn("Coffee", 400)   // duplicate, but newer
        )

        val suggestions = TransactionSuggestions.fromTransactions(txns)

        assertEquals(listOf("Coffee", "Lunch"), suggestions)
    }

    @Test
    fun `trims descriptions and dedupes case-sensitively after trimming`() {
        val txns = listOf(
            txn("  Groceries  ", 100),
            txn("Groceries", 200)
        )

        assertEquals(listOf("Groceries"), TransactionSuggestions.fromTransactions(txns))
    }

    @Test
    fun `empty input yields no suggestions`() {
        assertEquals(emptyList<String>(), TransactionSuggestions.fromTransactions(emptyList()))
    }
}
