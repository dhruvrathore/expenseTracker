package com.expensetracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Overall monthly limit, one row per month ("yyyy-MM"). */
@Entity(tableName = "monthly_budgets")
data class MonthlyBudgetEntity(
    @PrimaryKey val month: String,
    val monthlyLimit: Double
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val description: String,
    val category: String,
    val month: String,
    val timestamp: Long,
    val tag: String? = null
)

/** Per-category spending limit, one row per (month, category). */
@Entity(tableName = "category_limits", primaryKeys = ["month", "category"])
data class CategoryLimitEntity(
    val month: String,
    val category: String,
    @ColumnInfo(name = "limit_amount") val limit: Double
)

/** Monthly take-home income, one row per month ("yyyy-MM"). Independent of the spending limit. */
@Entity(tableName = "income")
data class IncomeEntity(
    @PrimaryKey val month: String,
    val income: Double
)

/** A single contribution to savings/investments. Independent of spending and income. */
@Entity(tableName = "savings_entries")
data class SavingsEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val description: String,
    val kind: String,
    val tag: String? = null,
    val month: String,
    val timestamp: Long
)
