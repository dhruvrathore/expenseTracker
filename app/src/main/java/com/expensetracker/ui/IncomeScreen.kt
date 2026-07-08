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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
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
import com.expensetracker.domain.BudgetCalculator
import com.expensetracker.domain.CategorySummary
import com.expensetracker.util.asCurrency
import kotlin.math.round

/** Read-only breakdown of each category's limit and spend as a percentage of the entered monthly income. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(
    income: Double?,
    categories: List<CategorySummary>,
    onOpenDrawer: () -> Unit,
    onSetIncome: (String) -> String?
) {
    var showIncomeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Income") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                    }
                },
                actions = {
                    IconButton(onClick = { showIncomeDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Set income")
                    }
                }
            )
        }
    ) { padding ->
        if (income == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No income set for this month", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showIncomeDialog = true }) {
                        Text("Set income")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { IncomeCard(income) }
                item {
                    Text(
                        "Categories as % of income",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(categories, key = { it.category }) { summary ->
                    CategoryPercentRow(summary = summary, income = income)
                    HorizontalDivider()
                }
            }
        }
    }

    if (showIncomeDialog) {
        AmountInputDialog(
            title = "Monthly income",
            initialValue = income?.toString() ?: "",
            label = "Income",
            onConfirm = { input ->
                val error = onSetIncome(input)
                if (error == null) showIncomeDialog = false
                error
            },
            onDismiss = { showIncomeDialog = false }
        )
    }
}

@Composable
private fun IncomeCard(income: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                "Monthly income",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                income.asCurrency(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CategoryPercentRow(summary: CategorySummary, income: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(summary.category, style = MaterialTheme.typography.bodyLarge)
            val limitLabel = if (summary.limit != null) "Limit ${summary.limit.asCurrency()}" else "No limit set"
            Text(
                "$limitLabel · Spent ${summary.spent.asCurrency()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            val limitPercent = BudgetCalculator.percentOfIncome(summary.limit, income)
            Text(
                if (limitPercent != null) "${limitPercent.roundedToOneDecimal()}% limit" else "— limit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = if (limitPercent != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            val spentPercent = BudgetCalculator.percentOfIncome(summary.spent, income)
            Text(
                if (spentPercent != null) "${spentPercent.roundedToOneDecimal()}% spent" else "— spent",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Double.roundedToOneDecimal(): Double = round(this * 10.0) / 10.0
