package com.example.pricer.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pricer.data.model.QuoteItem
import kotlinx.coroutines.launch

/**
 * Dialog to get quantities for splitting a QuoteItem into two lines.
 * One line retains original multipliers, the new line has none.
 *
 * @param originalItem The QuoteItem being split.
 * @param onDismiss Lambda to dismiss the dialog.
 * @param onConfirm Lambda called with the quantity for the first part (original item modified)
 *                  and the quantity for the second part (new item created).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitItemDialog(
    originalItem: QuoteItem,
    onDismiss: () -> Unit,
    onConfirm: (qtyFirstPart: Int, qtySecondPart: Int) -> Unit
) {
    val totalOriginalQty = originalItem.quantity
    // State for input fields - use mutableStateOf for Int parsing
    var qty1String by remember { mutableStateOf("") }
    var qty2String by remember { mutableStateOf("") } // Display only

    // State for validation errors
    var error by remember { mutableStateOf<String?>(null) }
    var isQty1Valid by remember { mutableStateOf(false) } // Track validity of first input

    // Calculate remaining quantity automatically based on first input
    // Use LaunchedEffect to react to qty1String changes
    LaunchedEffect(qty1String, totalOriginalQty) {
        val qty1 = qty1String.toIntOrNull()
        if (qty1 != null && qty1 >= 0 && qty1 <= totalOriginalQty) {
            // Valid input for qty1
            val qty2 = totalOriginalQty - qty1
            qty2String = qty2.toString()
            error = null // Clear error
            isQty1Valid = true
        } else {
            // Invalid or empty input for qty1
            qty2String = "" // Clear second field
            isQty1Valid = false
            if (qty1String.isNotBlank()) { // Show error only if not blank
                error = "Enter 0 to $totalOriginalQty"
            } else {
                error = null // No error if blank
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp)) {
                // --- Title ---
                Text("Split Item: ${originalItem.product.name}", style = MaterialTheme.typography.headlineSmall)
                Text("Original Total Quantity: $totalOriginalQty", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                // --- Input for Quantity Part 1 ---
                Text("Quantity for first line (keeps multipliers):", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = qty1String,
                    onValueChange = { newValue ->
                        // Allow only digits
                        if (newValue.all { it.isDigit() }) {
                            qty1String = newValue
                            // Validation happens in LaunchedEffect
                        }
                    },
                    label = { Text("Qty - Part 1*") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done // Use Done on the only editable field
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        // Try to confirm if valid when keyboard Done is pressed
                        val qty1 = qty1String.toIntOrNull()
                        val qty2 = qty2String.toIntOrNull()
                        if (isQty1Valid && qty1 != null && qty2 != null) {
                            onConfirm(qty1, qty2)
                        } else {
                            error = "Enter a valid quantity (0-$totalOriginalQty)"
                        }
                    }),
                    isError = error != null && qty1String.isNotBlank(), // Show error state if invalid and not blank
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp)) // More space

                // --- Display for Quantity Part 2 ---
                Text("Quantity for second line (no multipliers):", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = qty2String, // Display calculated value
                    onValueChange = { /* Read Only */ },
                    label = { Text("Qty - Part 2") },
                    readOnly = true, // Make second field read-only
                    modifier = Modifier.fillMaxWidth(),
                    // Use disabled colors to indicate read-only visually
                    colors = TextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        // Add other disabled states if needed (background, etc.)
                        disabledContainerColor = Color.Transparent // Or slightly different background
                    ),
                    enabled = false // Explicitly disable interaction
                )

                // Display Error Message
                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(24.dp))

                // --- Action Buttons ---
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // Re-validate on button click
                            val qty1 = qty1String.toIntOrNull()
                            val qty2 = qty2String.toIntOrNull()
                            if (isQty1Valid && qty1 != null && qty2 != null) { // isQty1Valid implies sum is correct
                                onConfirm(qty1, qty2)
                            } else {
                                error = "Enter a valid quantity (0-$totalOriginalQty) for Part 1"
                            }
                        },
                        // Enable confirm only if first quantity is valid
                        enabled = isQty1Valid
                    ) {
                        Text("Confirm Split")
                    }
                } // End Button Row
            } // End Column
        } // End Card
    } // End Dialog
}