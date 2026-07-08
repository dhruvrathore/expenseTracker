package com.expensetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.BudgetCalculator
import com.expensetracker.domain.SavingsEntry
import com.expensetracker.domain.SavingsKind
import com.expensetracker.util.asCurrency
import com.expensetracker.util.asDayLabel
import kotlin.math.round

/**
 * This month's savings/investment contributions — each one kept as its own entry (like
 * [TransactionsScreen]), rather than a single editable total, so adding one is a "+" and history
 * is just scrolled through rather than overwritten.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    savingsEntries: List<SavingsEntry>,
    income: Double?,
    onOpenDrawer: () -> Unit,
    onAddSavings: (amount: String, kind: SavingsKind, tag: String) -> Boolean,
    onDeleteSavingsEntry: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SavingsEntry?>(null) }
    val total = savingsEntries.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings and investments") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add savings")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SavingsCard(total)

                val percentOfIncome = BudgetCalculator.percentOfIncome(total, income)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Of income this month", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (percentOfIncome != null) "${percentOfIncome.roundedToOneDecimal()}%" else "— income not set",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (percentOfIncome != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider()

            if (savingsEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No savings or investments logged yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(savingsEntries, key = { it.id }) { entry ->
                        SavingsEntryRow(entry = entry, onDelete = { pendingDelete = entry })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSavingsDialog(
            onConfirm = { amount, kind, tag -> onAddSavings(amount, kind, tag) },
            onDismiss = { showAddDialog = false }
        )
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this entry?") },
            text = {
                Text("${entry.description} · ${entry.amount.asCurrency()} will be removed.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSavingsEntry(entry.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SavingsCard(total: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                "Saved and invested this month",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                total.asCurrency(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SavingsEntryRow(entry: SavingsEntry, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.tag?.takeIf { it.isNotBlank() } ?: entry.description,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "${entry.kind.label} · ${entry.timestamp.asDayLabel()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            entry.amount.asCurrency(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
        }
    }
}

/** Dialog for a manual contribution: amount, Savings-vs-Investment choice, and an optional tag. */
@Composable
private fun AddSavingsDialog(
    onConfirm: (amount: String, kind: SavingsKind, tag: String) -> Boolean,
    onDismiss: () -> Unit
) {
    val fieldShape = RoundedCornerShape(14.dp)
    var amount by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(SavingsKind.SAVINGS) }
    var tag by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        title = { Text("Add to savings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        error = null
                    },
                    label = { Text("Amount") },
                    prefix = { Text("₹ ") },
                    singleLine = true,
                    isError = error != null,
                    shape = fieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text(error ?: "Amount in rupees") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SavingsKind.entries.forEach { option ->
                        FilterChip(
                            selected = kind == option,
                            onClick = { kind = option },
                            label = { Text(option.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Tag (optional)") },
                    placeholder = { Text("e.g. FD, MF, Stock, Gold") },
                    singleLine = true,
                    shape = fieldShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (onConfirm(amount, kind, tag)) onDismiss() else error = "Enter a valid amount greater than 0"
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun Double.roundedToOneDecimal(): Double = round(this * 10.0) / 10.0
