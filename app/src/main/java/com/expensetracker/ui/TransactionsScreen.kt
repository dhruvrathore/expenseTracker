package com.expensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.Categories
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
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Categories that actually occur this month, in canonical order (any unknown ones last).
    val presentCategories = remember(transactions) {
        transactions.map { it.category }.distinct().sortedBy {
            val i = Categories.DEFAULTS.indexOf(it)
            if (i < 0) Int.MAX_VALUE else i
        }
    }
    // If the active filter's last transaction was deleted, fall back to showing all.
    LaunchedEffect(presentCategories) {
        val sel = selectedCategory
        if (sel != null && sel !in presentCategories) selectedCategory = null
    }
    val visibleTransactions = remember(transactions, selectedCategory, searchQuery) {
        val sel = selectedCategory
        val query = searchQuery.trim()
        transactions
            .filter { sel == null || it.category == sel }
            .filter {
                query.isEmpty() ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.tag?.contains(query, ignoreCase = true) == true
            }
    }

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search") },
                    placeholder = { Text("Description or tag, e.g. Ooty") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                if (presentCategories.size > 1) {
                    CategoryFilterRow(
                        categories = presentCategories,
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it }
                    )
                }
                if (visibleTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No transactions match your search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(visibleTransactions, key = { it.id }) { txn ->
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

/** Horizontally scrollable category filter: an "All" chip plus one chip per category present. */
@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("All") }
            )
        }
        items(categories) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(category) }
            )
        }
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
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
