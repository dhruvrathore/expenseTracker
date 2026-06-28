package com.expensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.expensetracker.domain.CategorySummary
import com.expensetracker.util.asCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryLimitsScreen(
    categories: List<CategorySummary>,
    onOpenDrawer: () -> Unit,
    onSetCategoryLimit: (category: String, input: String) -> String?
) {
    var editing by remember { mutableStateOf<CategorySummary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Category limits") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(categories, key = { it.category }) { summary ->
                CategoryRow(summary = summary, onClick = { editing = summary })
                HorizontalDivider()
            }
        }
    }

    editing?.let { summary ->
        AmountInputDialog(
            title = "${summary.category} limit",
            initialValue = summary.limit?.toString() ?: "",
            label = "Monthly limit",
            onConfirm = { input ->
                val error = onSetCategoryLimit(summary.category, input)
                if (error == null) editing = null
                error
            },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun CategoryRow(summary: CategorySummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = summary.category,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (summary.limit == null) {
                Text(
                    text = "Tap to set limit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "${summary.remaining!!.asCurrency()} left",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (summary.isOverBudget) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        }

        if (summary.limit != null) {
            val fraction = if (summary.limit > 0.0) {
                (summary.spent / summary.limit).coerceIn(0.0, 1.0).toFloat()
            } else {
                1f
            }
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                color = if (summary.isOverBudget) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Spent ${summary.spent.asCurrency()} of ${summary.limit.asCurrency()}" +
                    if (summary.isOverBudget) " · over budget" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Spent ${summary.spent.asCurrency()} · no limit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
