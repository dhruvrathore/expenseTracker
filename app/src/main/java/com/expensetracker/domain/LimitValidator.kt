package com.expensetracker.domain

/**
 * Enforces the rule that the sum of all per-category limits never exceeds the monthly limit.
 * Pure and side-effect free.
 */
object LimitValidator {

    // Tolerance so floating-point sums don't reject a value that "just fits".
    private const val EPS = 0.001

    /** How much of the monthly limit is still available for [excludeCategory]. */
    fun categoryLimitsRemaining(
        monthlyLimit: Double,
        currentLimits: Map<String, Double>,
        excludeCategory: String
    ): Double = monthlyLimit - currentLimits.filterKeys { it != excludeCategory }.values.sum()

    /** True if setting [category] to [newLimit] keeps the total within the monthly limit. */
    fun categoryLimitFits(
        monthlyLimit: Double,
        currentLimits: Map<String, Double>,
        category: String,
        newLimit: Double
    ): Boolean = newLimit <= categoryLimitsRemaining(monthlyLimit, currentLimits, category) + EPS

    /** True if [newMonthlyLimit] still covers the sum of all existing category limits. */
    fun monthlyLimitFits(
        newMonthlyLimit: Double,
        currentLimits: Map<String, Double>
    ): Boolean = currentLimits.values.sum() <= newMonthlyLimit + EPS
}
