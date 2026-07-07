package com.expensetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.Categories
import com.expensetracker.sms.CategoryMatcher
import com.expensetracker.sms.ParsedTransaction

private val sheetFieldShape = RoundedCornerShape(14.dp)

/**
 * Bottom sheet shown when a transaction is detected in an incoming SMS. The amount, description and
 * category are pre-filled from the parsed message but fully editable, so the user confirms (and
 * tweaks) before anything is saved. [onSave] mirrors the add-transaction contract: it returns true
 * when the entry was accepted and the sheet should close.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsConfirmSheet(
    transaction: ParsedTransaction,
    onSave: (amount: String, description: String, category: String, tag: String) -> Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Keyed on the transaction so a newly detected SMS resets the fields if the sheet is reused.
    var amount by remember(transaction) {
        mutableStateOf(transaction.amount.toString().removeSuffix(".0"))
    }
    var description by remember(transaction) {
        mutableStateOf(if (transaction.merchant == "Unknown") "" else transaction.merchant)
    }
    var category by remember(transaction) {
        mutableStateOf(CategoryMatcher.categorize(transaction.merchant))
    }
    var tag by remember(transaction) { mutableStateOf("") }
    var categoryExpanded by remember(transaction) { mutableStateOf(false) }
    var amountError by remember(transaction) { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Sms,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "  Transaction detected",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                "Found this in an SMS. Review and save it as an expense.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    amountError = false
                },
                label = { Text("Amount") },
                prefix = { Text("₹ ") },
                singleLine = true,
                isError = amountError,
                shape = sheetFieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = {
                    Text(if (amountError) "Enter an amount greater than 0" else "Amount in rupees")
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("What was it for?") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                singleLine = true,
                shape = sheetFieldShape,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    shape = sheetFieldShape,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    Categories.DEFAULTS.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                category = option
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = tag,
                onValueChange = { tag = it },
                label = { Text("Tag (optional)") },
                placeholder = { Text("e.g. Ooty trip") },
                leadingIcon = { Icon(Icons.Filled.Label, contentDescription = null) },
                singleLine = true,
                shape = sheetFieldShape,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (onSave(amount, description, category, tag)) onDismiss() else amountError = true
                },
                shape = sheetFieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    "  Save expense",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = onDismiss,
                shape = sheetFieldShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dismiss")
            }
        }
    }
}
