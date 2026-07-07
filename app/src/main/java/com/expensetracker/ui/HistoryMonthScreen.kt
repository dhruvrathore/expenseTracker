package com.expensetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.CategorySummary
import com.expensetracker.domain.MonthView
import com.expensetracker.domain.Transaction
import com.expensetracker.util.asCurrency
import com.expensetracker.util.asDayLabel
import com.expensetracker.util.asMonthLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryMonthScreen(
    month: String,
    view: MonthView?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(month.asMonthLabel()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SummaryCard(view)
            }

            val categoriesWithData = view?.categories?.filter { it.limit != null || it.spent > 0.0 }.orEmpty()
            if (categoriesWithData.isNotEmpty()) {
                item {
                    Text(
                        "By category",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(categoriesWithData, key = { "cat-${it.category}" }) { CategoryLine(it) }
            }

            val transactions = view?.transactions.orEmpty()
            if (transactions.isNotEmpty()) {
                item {
                    Text(
                        "Transactions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(transactions, key = { "txn-${it.id}" }) { TransactionLine(it) }
            }
        }
    }
}

@Composable
private fun SummaryCard(view: MonthView?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Spent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                (view?.totalSpent ?: 0.0).asCurrency(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (view?.hasLimit == true) {
                Text(
                    "Limit ${view.monthlyLimit.asCurrency()} · ${view.remaining.asCurrency()} " +
                        if (view.isOverBudget) "over" else "left",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (view.isOverBudget) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "No monthly limit was set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryLine(summary: CategorySummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(summary.category, style = MaterialTheme.typography.bodyLarge)
        val trailing = if (summary.limit != null) {
            "${summary.spent.asCurrency()} / ${summary.limit.asCurrency()}"
        } else {
            summary.spent.asCurrency()
        }
        Text(
            trailing,
            style = MaterialTheme.typography.bodyMedium,
            color = if (summary.isOverBudget) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider()
}

@Composable
private fun TransactionLine(txn: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                txn.description.ifBlank { txn.category },
                style = MaterialTheme.typography.bodyLarge
            )
            val tagSuffix = txn.tag?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
            Text(
                "${txn.category} · ${txn.timestamp.asDayLabel()}$tagSuffix",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            txn.amount.asCurrency(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
    HorizontalDivider()
}
