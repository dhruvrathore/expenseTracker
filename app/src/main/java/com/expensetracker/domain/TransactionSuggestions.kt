package com.expensetracker.domain

/** Builds description autocomplete suggestions from past transactions. */
object TransactionSuggestions {

    /** Distinct, trimmed, non-blank descriptions ordered most-recently-used first. */
    fun fromTransactions(transactions: List<Transaction>): List<String> =
        transactions
            .sortedByDescending { it.timestamp }
            .map { it.description.trim() }
            .filter { it.isNotBlank() }
            .distinct()
}
