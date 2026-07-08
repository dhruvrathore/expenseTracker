package com.expensetracker

import com.expensetracker.domain.SavingsEntry
import com.expensetracker.domain.SavingsKind
import com.expensetracker.domain.Transaction
import com.expensetracker.util.buildTransactionsCsv
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

// Mirrors the production conversion so assertions aren't tied to the test JVM's timezone.
private fun dateOf(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

class CsvExportTest {

    @Test
    fun `header only for no transactions or savings`() {
        assertEquals("Date,Type,Category,Description,Tag,Amount", buildTransactionsCsv(emptyList()))
    }

    @Test
    fun `formats rows oldest first with date, DEBIT type, and blank tag`() {
        val earlier = 1_700_000_000_000L
        val later = earlier + 86_400_000L // one day later

        val txns = listOf(
            Transaction(amount = 500.0, description = "Train", category = "Travel", timestamp = later, tag = "Ooty"),
            Transaction(amount = 100.0, description = "Coffee", category = "Food", timestamp = earlier, tag = null)
        )

        val lines = buildTransactionsCsv(txns).lines()

        assertEquals("Date,Type,Category,Description,Tag,Amount", lines[0])
        assertEquals("${dateOf(earlier)},DEBIT,Food,Coffee,,100.0", lines[1])
        assertEquals("${dateOf(later)},DEBIT,Travel,Train,Ooty,500.0", lines[2])
    }

    @Test
    fun `escapes fields containing commas or quotes`() {
        val timestamp = 1_700_000_000_000L
        val txns = listOf(
            Transaction(amount = 50.0, description = "Lunch, with a \"friend\"", category = "Food", timestamp = timestamp)
        )

        val csv = buildTransactionsCsv(txns)

        assertEquals(
            "Date,Type,Category,Description,Tag,Amount\n" +
                "${dateOf(timestamp)},DEBIT,Food,\"Lunch, with a \"\"friend\"\"\",,50.0",
            csv
        )
    }

    @Test
    fun `savings contributions appear as their own CREDIT rows, category is kind and tag carries through`() {
        val txnTimestamp = Instant.parse("2026-06-10T00:00:00Z").toEpochMilli()
        val savingsTimestamp = Instant.parse("2026-06-15T00:00:00Z").toEpochMilli()
        val txns = listOf(
            Transaction(amount = 450.0, description = "Swiggy order", category = "Food", timestamp = txnTimestamp)
        )
        val savings = listOf(
            SavingsEntry(
                amount = 15000.0,
                description = "Zerodha SIP",
                kind = SavingsKind.INVESTMENT,
                tag = "MF",
                month = "2026-06",
                timestamp = savingsTimestamp
            )
        )

        val lines = buildTransactionsCsv(txns, savings).lines()

        assertEquals("Date,Type,Category,Description,Tag,Amount", lines[0])
        assertEquals("${dateOf(txnTimestamp)},DEBIT,Food,Swiggy order,,450.0", lines[1])
        assertEquals(
            "${dateOf(savingsTimestamp)},CREDIT,Investment,Zerodha SIP,MF,15000.0",
            lines[2]
        )
    }
}
