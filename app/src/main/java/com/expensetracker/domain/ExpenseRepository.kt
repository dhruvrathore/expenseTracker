package com.expensetracker.domain

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over expense storage, scoped by month (a "yyyy-MM" key). The Room-backed
 * implementation lives in the data layer; tests use an in-memory fake. Reads are reactive
 * [Flow]s; writes are suspending.
 */
interface ExpenseRepository {
    fun budget(month: String): Flow<Budget?>
    fun transactions(month: String): Flow<List<Transaction>>
    fun categoryLimits(month: String): Flow<List<CategoryLimit>>

    /** Every transaction across all months — used to suggest past descriptions. */
    val allTransactions: Flow<List<Transaction>>

    /** All months that have any data, most-recent first — used to build history. */
    val availableMonths: Flow<List<String>>

    /**
     * Ensures [month] has a budget row, carrying the monthly + category limits forward from the
     * most recent earlier month (spending starts fresh). No-op if [month] is already initialized
     * or there is no earlier month to inherit from.
     */
    suspend fun ensureMonthInitialized(month: String)

    suspend fun setMonthlyLimit(month: String, limit: Double)
    suspend fun setCategoryLimit(month: String, category: String, limit: Double)
    suspend fun addTransaction(month: String, transaction: Transaction)

    /** Updates a transaction's editable fields, preserving its month and timestamp. */
    suspend fun updateTransaction(id: Long, amount: Double, description: String, category: String, tag: String?)
    suspend fun deleteTransaction(id: Long)

    /** Deletes all transactions in [month]. Budgets and category limits are kept. */
    suspend fun deleteTransactionsForMonth(month: String)
}
