package com.expensetracker

import com.expensetracker.domain.Budget
import com.expensetracker.domain.CategoryLimit
import com.expensetracker.domain.ExpenseRepository
import com.expensetracker.domain.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory, month-scoped [ExpenseRepository] for fast, deterministic unit tests.
 * Mirrors the real carry-forward behaviour in [ensureMonthInitialized].
 *
 * @param initialLimit if set, seeds [seedMonth] with this monthly limit.
 */
class FakeExpenseRepository(
    initialLimit: Double? = null,
    seedMonth: String = "2026-06"
) : ExpenseRepository {

    private val budgets = MutableStateFlow<Map<String, Double>>(
        if (initialLimit != null) mapOf(seedMonth to initialLimit) else emptyMap()
    )
    private val txns = MutableStateFlow<Map<String, List<Transaction>>>(emptyMap())
    private val limits = MutableStateFlow<Map<String, List<CategoryLimit>>>(emptyMap())

    override fun budget(month: String): Flow<Budget?> =
        budgets.map { it[month]?.let(::Budget) }

    override fun transactions(month: String): Flow<List<Transaction>> =
        txns.map { it[month].orEmpty() }

    override fun categoryLimits(month: String): Flow<List<CategoryLimit>> =
        limits.map { it[month].orEmpty() }

    override val availableMonths: Flow<List<String>> =
        budgets.map { budgetMap ->
            (budgetMap.keys + txns.value.keys).distinct().sortedDescending()
        }

    override val allTransactions: Flow<List<Transaction>> =
        txns.map { byMonth -> byMonth.values.flatten() }

    override suspend fun ensureMonthInitialized(month: String) {
        if (budgets.value.containsKey(month)) return
        val previous = budgets.value.keys.filter { it < month }.maxOrNull() ?: return
        budgets.value = budgets.value + (month to budgets.value.getValue(previous))
        limits.value[previous]?.let { prevLimits ->
            limits.value = limits.value + (month to prevLimits.map { it })
        }
    }

    override suspend fun setMonthlyLimit(month: String, limit: Double) {
        budgets.value = budgets.value + (month to limit)
    }

    override suspend fun setCategoryLimit(month: String, category: String, limit: Double) {
        val updated = limits.value[month].orEmpty()
            .filterNot { it.category == category } + CategoryLimit(category, limit)
        limits.value = limits.value + (month to updated)
    }

    override suspend fun addTransaction(month: String, transaction: Transaction) {
        val current = txns.value[month].orEmpty()
        val id = nextId++
        // Assign an increasing timestamp so recency-based ordering is deterministic in tests.
        val withId = transaction.copy(
            id = id,
            timestamp = transaction.timestamp.takeIf { it > 0L } ?: id
        )
        txns.value = txns.value + (month to (listOf(withId) + current))
    }

    override suspend fun updateTransaction(
        id: Long,
        amount: Double,
        description: String,
        category: String
    ) {
        txns.value = txns.value.mapValues { (_, list) ->
            list.map { txn ->
                if (txn.id == id) {
                    txn.copy(amount = amount, description = description, category = category)
                } else {
                    txn
                }
            }
        }
    }

    override suspend fun deleteTransaction(id: Long) {
        txns.value = txns.value.mapValues { (_, list) -> list.filterNot { it.id == id } }
    }

    override suspend fun deleteTransactionsForMonth(month: String) {
        txns.value = txns.value - month
    }

    private var nextId = 1L
}
