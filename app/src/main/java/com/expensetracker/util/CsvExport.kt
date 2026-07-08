package com.expensetracker.util

import com.expensetracker.domain.Transaction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** Quotes a CSV field, doubling any embedded quotes, when it contains a comma, quote, or newline. */
private fun String.csvEscaped(): String =
    if (any { it == ',' || it == '"' || it == '\n' }) "\"${replace("\"", "\"\"")}\"" else this

/**
 * Builds a CSV export of every transaction, oldest first, for sharing outside the app (e.g. to
 * hand to an LLM for spending analysis). Columns: date, category, description, tag, amount.
 */
fun buildTransactionsCsv(transactions: List<Transaction>): String {
    val header = "Date,Category,Description,Tag,Amount"
    val rows = transactions
        .sortedBy { it.timestamp }
        .map { txn ->
            val date = Instant.ofEpochMilli(txn.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            listOf(
                date.format(dateFormatter),
                txn.category,
                txn.description,
                txn.tag.orEmpty(),
                txn.amount.toString()
            ).joinToString(",") { it.csvEscaped() }
        }
    return (listOf(header) + rows).joinToString("\n")
}
