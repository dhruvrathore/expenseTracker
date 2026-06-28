package com.expensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.Transaction
import com.expensetracker.util.asCurrency
import com.expensetracker.util.asDayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<Transaction>,
    onOpenDrawer: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Long) -> Unit,
    onClearMonth: () -> Unit
) {
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
    var showClearAll by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                    }
                },
                actions = {
                    if (transactions.isNotEmpty()) {
                        IconButton(onClick = { showClearAll = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear this month's transactions")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No transactions this month yet. Tap + on the home screen to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(transactions, key = { it.id }) { txn ->
                    TransactionRow(
                        txn = txn,
                        onClick = { onEdit(txn) },
                        onDelete = { pendingDelete = txn }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    pendingDelete?.let { txn ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete transaction?") },
            text = {
                Text(
                    "${txn.description.ifBlank { txn.category }} · ${txn.amount.asCurrency()} " +
                        "will be removed and added back to your balance."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(txn.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showClearAll) {
        AlertDialog(
            onDismissRequest = { showClearAll = false },
            icon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null) },
            title = { Text("Clear this month's transactions?") },
            text = {
                Text(
                    "This permanently deletes every transaction in the current month. " +
                        "Your monthly and category limits are kept. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onClearMonth()
                    showClearAll = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAll = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TransactionRow(txn: Transaction, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                txn.description.ifBlank { txn.category },
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "${txn.category} · ${txn.timestamp.asDayLabel()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            txn.amount.asCurrency(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
