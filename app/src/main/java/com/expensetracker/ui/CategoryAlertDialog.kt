package com.expensetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.AlertLevel
import com.expensetracker.domain.CategoryAlert
import com.expensetracker.util.asCurrency
import kotlin.math.roundToInt

/** Styled alert shown whenever a category's spending nears or exceeds its limit. */
@Composable
fun CategoryAlertDialog(alert: CategoryAlert, onDismiss: () -> Unit) {
    val percent = (alert.spent / alert.limit * 100).roundToInt()
    val over = alert.level == AlertLevel.OVER
    val accent = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        iconContentColor = accent,
        titleContentColor = accent,
        icon = {
            Icon(
                if (over) Icons.Filled.Warning else Icons.Filled.NotificationsActive,
                contentDescription = null
            )
        },
        title = {
            Text(if (over) "${alert.category} budget exceeded" else "Approaching ${alert.category} budget")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "You've spent ${alert.spent.asCurrency()} of your " +
                        "${alert.limit.asCurrency()} ${alert.category} budget."
                )
                LinearProgressIndicator(
                    progress = { (alert.spent / alert.limit).coerceIn(0.0, 1.0).toFloat() },
                    color = accent,
                    trackColor = accent.copy(alpha = 0.20f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                )
                Text(
                    "$percent% used",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Got it") }
        }
    )
}
