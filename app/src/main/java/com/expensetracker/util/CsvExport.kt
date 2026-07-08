package com.expensetracker.util

import com.expensetracker.domain.SavingsEntry
import com.expensetracker.domain.Transaction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** Quotes a CSV field, doubling any embedded quotes, when it contains a comma, quote, or newline. */
private fun String.csvEscaped(): String =
    if (any { it == ',' || it == '"' || it == '\n' }) "\"${replace("\"", "\"\"")}\"" else this

private data class CsvRow(
    val sortKey: Long,
    val type: String,
    val category: String,
    val description: String,
    val tag: String,
    val amount: Double
) {
    fun toLine(): String {
        val date = Instant.ofEpochMilli(sortKey).atZone(ZoneId.systemDefault()).toLocalDate()
        return listOf(date.format(dateFormatter), type, category, description, tag, amount.toString())
            .joinToString(",") { it.csvEscaped() }
    }
}

/**
 * Builds a CSV export combining every expense transaction (DEBIT) and every savings/investment
 * contribution (CREDIT), oldest first, for sharing outside the app (e.g. to hand to an LLM for
 * spending analysis). Columns: date, type, category, description, tag, amount.
 */
fun buildTransactionsCsv(transactions: List<Transaction>, savingsEntries: List<SavingsEntry> = emptyList()): String {
    val header = "Date,Type,Category,Description,Tag,Amount"

    val transactionRows = transactions.map { txn ->
        CsvRow(
            sortKey = txn.timestamp,
            type = "DEBIT",
            category = txn.category,
            description = txn.description,
            tag = txn.tag.orEmpty(),
            amount = txn.amount
        )
    }
    val savingsRows = savingsEntries.map { entry ->
        CsvRow(
            sortKey = entry.timestamp,
            type = "CREDIT",
            category = entry.kind.label,
            description = entry.description,
            tag = entry.tag.orEmpty(),
            amount = entry.amount
        )
    }

    val rows = (transactionRows + savingsRows).sortedBy { it.sortKey }.map { it.toLine() }
    return (listOf(header) + rows).joinToString("\n")
}
