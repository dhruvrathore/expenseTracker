package com.expensetracker.domain

/** Canonical list of expense categories used across transactions and per-category limits. */
object Categories {
    val DEFAULTS: List<String> = listOf(
        "Food", "Transport", "Rent", "Shopping", "Utilities", "Entertainment", "Health", "Eating Out", "Other"
    )
}
