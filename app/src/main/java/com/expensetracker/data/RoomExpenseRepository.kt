package com.expensetracker.data

import com.expensetracker.domain.Budget
import com.expensetracker.domain.CategoryLimit
import com.expensetracker.domain.ExpenseRepository
import com.expensetracker.domain.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed [ExpenseRepository], mapping between Room entities and domain models. */
class RoomExpenseRepository(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
    private val categoryLimitDao: CategoryLimitDao,
    private val now: () -> Long = System::currentTimeMillis
) : ExpenseRepository {

    override fun budget(month: String): Flow<Budget?> =
        budgetDao.observeBudget(month).map { entity ->
            entity?.let { Budget(monthlyLimit = it.monthlyLimit) }
        }

    override fun transactions(month: String): Flow<List<Transaction>> =
        transactionDao.observeForMonth(month).map { list -> list.map { it.toDomain() } }

    override fun categoryLimits(month: String): Flow<List<CategoryLimit>> =
        categoryLimitDao.observeForMonth(month).map { list ->
            list.map { CategoryLimit(category = it.category, limit = it.limit) }
        }

    override val availableMonths: Flow<List<String>> =
        budgetDao.observeAvailableMonths()

    override val allTransactions: Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun ensureMonthInitialized(month: String) {
        if (budgetDao.getBudget(month) != null) return
        val previous = budgetDao.latestBudgetBefore(month) ?: return

        budgetDao.upsert(MonthlyBudgetEntity(month = month, monthlyLimit = previous.monthlyLimit))
        val carried = categoryLimitDao.getForMonth(previous.month)
            .map { it.copy(month = month) }
        if (carried.isNotEmpty()) {
            categoryLimitDao.upsertAll(carried)
        }
    }

    override suspend fun setMonthlyLimit(month: String, limit: Double) {
        budgetDao.upsert(MonthlyBudgetEntity(month = month, monthlyLimit = limit))
    }

    override suspend fun setCategoryLimit(month: String, category: String, limit: Double) {
        categoryLimitDao.upsert(
            CategoryLimitEntity(month = month, category = category, limit = limit)
        )
    }

    override suspend fun addTransaction(month: String, transaction: Transaction) {
        transactionDao.insert(
            TransactionEntity(
                amount = transaction.amount,
                description = transaction.description,
                category = transaction.category,
                month = month,
                timestamp = transaction.timestamp.takeIf { it > 0L } ?: now(),
                tag = transaction.tag
            )
        )
    }

    override suspend fun updateTransaction(
        id: Long,
        amount: Double,
        description: String,
        category: String,
        tag: String?
    ) {
        transactionDao.updateFields(id, amount, description, category, tag)
    }

    override suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteById(id)
    }

    override suspend fun deleteTransactionsForMonth(month: String) {
        transactionDao.deleteForMonth(month)
    }

    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        amount = amount,
        description = description,
        category = category,
        timestamp = timestamp,
        tag = tag
    )
}
