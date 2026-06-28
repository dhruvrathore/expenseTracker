package com.expensetracker.domain

/** Pure budget math — no Android or framework dependencies, fully unit-testable. */
object BudgetCalculator {

    /** Spending at or above this fraction of a category limit raises a "nearing" alert. */
    const val NEARING_THRESHOLD = 0.9

    fun totalSpent(transactions: List<Transaction>): Double =
        transactions.sumOf { it.amount }

    fun remaining(monthlyLimit: Double, transactions: List<Transaction>): Double =
        monthlyLimit - totalSpent(transactions)

    fun isOverBudget(monthlyLimit: Double, transactions: List<Transaction>): Boolean =
        remaining(monthlyLimit, transactions) < 0.0

    /** Sum of transaction amounts grouped by category. */
    fun spentByCategory(transactions: List<Transaction>): Map<String, Double> =
        transactions.groupBy { it.category }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }

    /**
     * Builds a [CategorySummary] for each category. A category without a configured limit
     * reports null limit/remaining and is never flagged over budget.
     */
    fun categorySummaries(
        categories: List<String>,
        limits: Map<String, Double>,
        transactions: List<Transaction>
    ): List<CategorySummary> {
        val spentByCategory = spentByCategory(transactions)
        return categories.map { category ->
            val limit = limits[category]
            val spent = spentByCategory[category] ?: 0.0
            CategorySummary(
                category = category,
                limit = limit,
                spent = spent,
                remaining = limit?.let { it - spent },
                isOverBudget = limit != null && spent > limit
            )
        }
    }

    /**
     * Alert severity for a category given its [spent] and [limit]:
     * OVER at/above 100%, NEARING at/above [NEARING_THRESHOLD], null otherwise.
     * Returns null for a non-positive limit.
     */
    fun alertLevel(spent: Double, limit: Double): AlertLevel? {
        if (limit <= 0.0) return null
        val ratio = spent / limit
        return when {
            ratio >= 1.0 -> AlertLevel.OVER
            ratio >= NEARING_THRESHOLD -> AlertLevel.NEARING
            else -> null
        }
    }
}
