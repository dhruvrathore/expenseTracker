package com.expensetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.BudgetCalculator
import com.expensetracker.domain.Categories
import com.expensetracker.domain.CategoryAlert
import com.expensetracker.domain.CategorySummary
import com.expensetracker.domain.ExpenseRepository
import com.expensetracker.domain.LimitValidator
import com.expensetracker.domain.MonthView
import com.expensetracker.domain.Transaction
import com.expensetracker.domain.TransactionSuggestions
import com.expensetracker.util.asCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

/** UI state for the light home screen: only what's needed to show the remaining balance. */
data class HomeUiState(
    val hasLimit: Boolean = false,
    val monthlyLimit: Double = 0.0,
    val totalSpent: Double = 0.0,
    val remaining: Double = 0.0,
    val isOverBudget: Boolean = false,
    val transactions: List<Transaction> = emptyList()
)

/** The current calendar month as a "yyyy-MM" key. */
fun currentSystemMonth(): String = YearMonth.now().toString()

private const val INVALID_AMOUNT = "Enter a valid non-negative amount"

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    val currentMonth: String = currentSystemMonth()
) : ViewModel() {

    init {
        // Auto-reset: a brand-new month inherits last month's limits with spending zeroed.
        viewModelScope.launch { repository.ensureMonthInitialized(currentMonth) }
    }

    /** Combined month snapshot used for both the current month and any history month. */
    private fun monthViewFlow(month: String): Flow<MonthView> =
        combine(
            repository.budget(month),
            repository.categoryLimits(month),
            repository.transactions(month)
        ) { budget, limits, transactions ->
            val limit = budget?.monthlyLimit ?: 0.0
            MonthView(
                month = month,
                hasLimit = budget != null,
                monthlyLimit = limit,
                totalSpent = BudgetCalculator.totalSpent(transactions),
                remaining = BudgetCalculator.remaining(limit, transactions),
                isOverBudget = budget != null && BudgetCalculator.isOverBudget(limit, transactions),
                categories = BudgetCalculator.categorySummaries(
                    categories = Categories.DEFAULTS,
                    limits = limits.associate { it.category to it.limit },
                    transactions = transactions
                ),
                transactions = transactions
            )
        }

    private val currentMonthView: StateFlow<MonthView> =
        monthViewFlow(currentMonth).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMonthView(currentMonth)
        )

    val uiState: StateFlow<HomeUiState> =
        currentMonthView.map { it.toHomeUiState() }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HomeUiState()
        )

    val categoryState: StateFlow<List<CategorySummary>> =
        currentMonthView.map { it.categories }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMonthView(currentMonth).categories
        )

    /** Past months (earlier than the current month) that have data, most-recent first. */
    val historyMonths: StateFlow<List<String>> =
        repository.availableMonths
            .map { months -> months.filter { it < currentMonth } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    /** Past descriptions for autocomplete in the add/edit screen, most-recent first. */
    val descriptionSuggestions: StateFlow<List<String>> =
        repository.allTransactions
            .map { TransactionSuggestions.fromTransactions(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    private val _pendingAlert = MutableStateFlow<CategoryAlert?>(null)
    val pendingAlert: StateFlow<CategoryAlert?> = _pendingAlert.asStateFlow()

    fun clearAlert() {
        _pendingAlert.value = null
    }

    /** Read-only stream of any month's data, for the history detail screen. */
    fun observeMonth(month: String): Flow<MonthView> = monthViewFlow(month)

    /**
     * Validates and stores a new monthly limit. The limit must cover the sum of category limits.
     * @return null on success, or a human-readable error message.
     */
    fun setMonthlyLimit(input: String): String? {
        val limit = input.trim().toDoubleOrNull() ?: return INVALID_AMOUNT
        if (limit < 0.0) return INVALID_AMOUNT

        val limits = currentCategoryLimits()
        if (!LimitValidator.monthlyLimitFits(limit, limits)) {
            return "Monthly limit must be at least ${limits.values.sum().asCurrency()} " +
                "(the total of your category limits)"
        }
        viewModelScope.launch { repository.setMonthlyLimit(currentMonth, limit) }
        return null
    }

    /**
     * Validates and stores a category limit. Requires a monthly limit and keeps the sum of all
     * category limits within it.
     * @return null on success, or a human-readable error message.
     */
    fun setCategoryLimit(category: String, input: String): String? {
        val limit = input.trim().toDoubleOrNull() ?: return INVALID_AMOUNT
        if (limit < 0.0) return INVALID_AMOUNT
        if (!uiState.value.hasLimit) return "Set a monthly limit first"

        val limits = currentCategoryLimits()
        val monthlyLimit = uiState.value.monthlyLimit
        if (!LimitValidator.categoryLimitFits(monthlyLimit, limits, category, limit)) {
            val available = LimitValidator.categoryLimitsRemaining(monthlyLimit, limits, category)
            return "Only ${available.asCurrency()} of the monthly limit is unallocated"
        }
        viewModelScope.launch { repository.setCategoryLimit(currentMonth, category, limit) }
        return null
    }

    /**
     * Validates and adds a transaction to the current month. Raises a [pendingAlert] when the
     * transaction pushes its category to/over 90% of its limit.
     * @return true if accepted, false if the amount was blank, non-numeric, or not positive.
     */
    fun addTransaction(amountInput: String, description: String, category: String, tag: String = ""): Boolean {
        val amount = amountInput.trim().toDoubleOrNull() ?: return false
        if (amount <= 0.0) return false

        raiseAlert(category, resultingSpent = spentFor(category) + amount)
        viewModelScope.launch {
            repository.addTransaction(
                currentMonth,
                Transaction(
                    amount = amount,
                    description = description.trim(),
                    category = category,
                    tag = tag.trim().ifBlank { null }
                )
            )
        }
        return true
    }

    /**
     * Validates and updates a transaction's fields (month and timestamp are preserved).
     * @return true if accepted, false if the amount was blank, non-numeric, or not positive.
     */
    fun updateTransaction(
        id: Long,
        amountInput: String,
        description: String,
        category: String,
        tag: String = ""
    ): Boolean {
        val amount = amountInput.trim().toDoubleOrNull() ?: return false
        if (amount <= 0.0) return false

        // Re-evaluate the category alert for the resulting spend after this edit.
        val old = uiState.value.transactions.firstOrNull { it.id == id }
        val resultingSpent = if (old != null && old.category == category) {
            spentFor(category) - old.amount + amount
        } else {
            spentFor(category) + amount
        }
        raiseAlert(category, resultingSpent)

        viewModelScope.launch {
            repository.updateTransaction(id, amount, description.trim(), category, tag.trim().ifBlank { null })
        }
        return true
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { repository.deleteTransaction(id) }
    }

    /** Removes all transactions in the current month; budgets and category limits are kept. */
    fun clearCurrentMonthTransactions() {
        viewModelScope.launch { repository.deleteTransactionsForMonth(currentMonth) }
    }

    private fun spentFor(category: String): Double =
        categoryState.value.firstOrNull { it.category == category }?.spent ?: 0.0

    private fun raiseAlert(category: String, resultingSpent: Double) {
        val limit = categoryState.value.firstOrNull { it.category == category }?.limit ?: return
        BudgetCalculator.alertLevel(resultingSpent, limit)?.let { level ->
            _pendingAlert.value = CategoryAlert(category, level, resultingSpent, limit)
        }
    }

    private fun currentCategoryLimits(): Map<String, Double> =
        categoryState.value
            .filter { it.limit != null }
            .associate { it.category to it.limit!! }
}

private fun MonthView.toHomeUiState() = HomeUiState(
    hasLimit = hasLimit,
    monthlyLimit = monthlyLimit,
    totalSpent = totalSpent,
    remaining = remaining,
    isOverBudget = isOverBudget,
    transactions = transactions
)

private fun emptyMonthView(month: String) = MonthView(
    month = month,
    hasLimit = false,
    monthlyLimit = 0.0,
    totalSpent = 0.0,
    remaining = 0.0,
    isOverBudget = false,
    categories = Categories.DEFAULTS.map {
        CategorySummary(it, limit = null, spent = 0.0, remaining = null, isOverBudget = false)
    },
    transactions = emptyList()
)

/** Factory so the ViewModel can receive its repository without a DI framework. */
class ExpenseViewModelFactory(
    private val repository: ExpenseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return ExpenseViewModel(repository) as T
    }
}
