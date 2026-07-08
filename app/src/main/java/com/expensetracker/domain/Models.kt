package com.expensetracker.domain

/** A single expense entry, debited from the monthly limit. */
data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val category: String,
    val timestamp: Long = 0L,
    val tag: String? = null
)

/** The user's overall monthly spending limit. */
data class Budget(
    val monthlyLimit: Double
)

/** Whether a savings/investment entry is money set aside (FD, RD, plain savings) or invested. */
enum class SavingsKind(val label: String) { SAVINGS("Savings"), INVESTMENT("Investment") }

/** A single contribution to savings/investments (manual or SMS-confirmed), independent of spending. */
data class SavingsEntry(
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val kind: SavingsKind = SavingsKind.SAVINGS,
    val tag: String? = null,
    val month: String,
    val timestamp: Long = 0L
)

/** A spending limit configured for a single category. */
data class CategoryLimit(
    val category: String,
    val limit: Double
)

/**
 * Computed view of one category: its limit (null if unset), amount spent, and remaining
 * (null when no limit is set). [isOverBudget] is only true when a limit exists and is exceeded.
 */
data class CategorySummary(
    val category: String,
    val limit: Double?,
    val spent: Double,
    val remaining: Double?,
    val isOverBudget: Boolean
)

/** Severity of a category-spending alert raised after adding a transaction. */
enum class AlertLevel { NEARING, OVER }

/** A one-shot alert about a category's spending crossing a threshold. */
data class CategoryAlert(
    val category: String,
    val level: AlertLevel,
    val spent: Double,
    val limit: Double
)

/** Read-only snapshot of a single month, used by the current month and history screens. */
data class MonthView(
    val month: String,
    val hasLimit: Boolean,
    val monthlyLimit: Double,
    val totalSpent: Double,
    val remaining: Double,
    val isOverBudget: Boolean,
    val categories: List<CategorySummary>,
    val transactions: List<Transaction>,
    val savingsEntries: List<SavingsEntry> = emptyList()
)
