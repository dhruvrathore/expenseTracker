package com.expensetracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.Categories
import com.expensetracker.domain.CategorySlice
import com.expensetracker.domain.CategorySummary
import com.expensetracker.domain.ChartData
import com.expensetracker.domain.Transaction
import com.expensetracker.domain.WeeklyTotal
import com.expensetracker.util.asCurrency
import kotlin.math.abs
import kotlin.math.roundToInt

private val ChartPalette = listOf(
    Color(0xFF2E7D32), Color(0xFF1565C0), Color(0xFFEF6C00), Color(0xFF6A1B9A),
    Color(0xFFC62828), Color(0xFF00838F), Color(0xFFF9A825), Color(0xFF4E342E),
    Color(0xFFAD1457)
)

private fun categoryColor(category: String): Color {
    val index = Categories.DEFAULTS.indexOf(category)
    val key = if (index >= 0) index else abs(category.hashCode())
    return ChartPalette[key % ChartPalette.size]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    categories: List<CategorySummary>,
    transactions: List<Transaction>,
    daysInMonth: Int,
    onOpenDrawer: () -> Unit
) {
    val slices = ChartData.categorySlices(categories)
    val weeks = ChartData.weeklyTotals(transactions, daysInMonth)
    val topTransactions = ChartData.topTransactions(transactions)
    val totalSpent = categories.sumOf { it.spent }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Charts") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                    }
                }
            )
        }
    ) { padding ->
        if (totalSpent <= 0.0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No spending yet this month. Add a transaction to see charts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Spending by category") {
                DonutChart(slices = slices, total = totalSpent)
                Spacer(Modifier.height(16.dp))
                Legend(slices = slices)
            }

            SectionCard("Weekly spending") {
                WeeklySpending(weeks = weeks)
            }

            if (topTransactions.isNotEmpty()) {
                SectionCard("Top transactions") {
                    TopTransactions(transactions = topTransactions)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DonutChart(slices: List<CategorySlice>, total: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokeWidth = 42.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = (slice.fraction * 360f).toFloat()
                drawArc(
                    color = categoryColor(slice.category),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                total.asCurrency(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "spent",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Legend(slices: List<CategorySlice>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slices.forEach { slice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(categoryColor(slice.category))
                )
                Spacer(Modifier.width(10.dp))
                Text(slice.category, modifier = Modifier.weight(1f))
                Text(
                    "${slice.amount.asCurrency()}  ·  ${(slice.fraction * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeeklySpending(weeks: List<WeeklyTotal>) {
    val maxAmount = weeks.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        weeks.forEach { week ->
            ValueBar(
                title = "Week ${week.week}",
                subtitle = "${week.startDay}–${week.endDay}",
                amount = week.amount,
                fraction = (week.amount / maxAmount).toFloat(),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TopTransactions(transactions: List<Transaction>) {
    val maxAmount = transactions.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        transactions.forEach { txn ->
            ValueBar(
                title = txn.description.ifBlank { txn.category },
                subtitle = txn.category,
                amount = txn.amount,
                fraction = (txn.amount / maxAmount).toFloat(),
                color = categoryColor(txn.category)
            )
        }
    }
}

/** A labeled horizontal bar: title/subtitle left, amount right, proportional track below. */
@Composable
private fun ValueBar(
    title: String,
    subtitle: String?,
    amount: Double,
    fraction: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                amount.asCurrency(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            color = color,
            trackColor = color.copy(alpha = 0.18f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}
