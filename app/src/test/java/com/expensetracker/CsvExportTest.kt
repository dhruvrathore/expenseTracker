package com.expensetracker

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
    fun `header only for no transactions`() {
        assertEquals("Date,Category,Description,Tag,Amount", buildTransactionsCsv(emptyList()))
    }

    @Test
    fun `formats rows oldest first with date and blank tag`() {
        val earlier = 1_700_000_000_000L
        val later = earlier + 86_400_000L // one day later

        val txns = listOf(
            Transaction(amount = 500.0, description = "Train", category = "Travel", timestamp = later, tag = "Ooty"),
            Transaction(amount = 100.0, description = "Coffee", category = "Food", timestamp = earlier, tag = null)
        )

        val lines = buildTransactionsCsv(txns).lines()

        assertEquals("Date,Category,Description,Tag,Amount", lines[0])
        assertEquals("${dateOf(earlier)},Food,Coffee,,100.0", lines[1])
        assertEquals("${dateOf(later)},Travel,Train,Ooty,500.0", lines[2])
    }

    @Test
    fun `escapes fields containing commas or quotes`() {
        val timestamp = 1_700_000_000_000L
        val txns = listOf(
            Transaction(amount = 50.0, description = "Lunch, with a \"friend\"", category = "Food", timestamp = timestamp)
        )

        val csv = buildTransactionsCsv(txns)

        assertEquals(
            "Date,Category,Description,Tag,Amount\n${dateOf(timestamp)},Food,\"Lunch, with a \"\"friend\"\"\",,50.0",
            csv
        )
    }
}
