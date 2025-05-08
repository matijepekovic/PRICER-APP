package com.example.pricer.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pricer.data.model.Multiplier
import com.example.pricer.data.model.Product
import com.example.pricer.util.formatCurrency
import com.example.pricer.util.formatPercentage
import android.util.Log // For logging
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.pricer.data.model.MultiplierType

/**
 * Dialog for assigning global multipliers to a specific product and specifying
 * the quantity each selected multiplier applies to.
 *
 * @param product The product to which multipliers are being assigned.
 * @param totalQuantity The total quantity entered for this product in the catalog screen.
 * @param availableMultipliers The list of all global multipliers defined in the catalog.
 * @param initialAssignments The currently active assignments for this product (Map<MultiplierId, QuantityString>).
 * @param onDismiss Lambda to dismiss the dialog.
 * @param onConfirm Lambda called when confirming assignments. Passes the updated assignment map (Map<MultiplierId, QuantityString>).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignMultiplierDialog(
    product: Product,
    totalQuantity: Int, // Need the total quantity for validation
    availableMultipliers: List<Multiplier>,
    initialAssignments: Map<String, String>, // Map<MultiplierId, QuantityString>
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit // Returns the final selections/quantities
) {
    // Internal state map to track selections and quantities during the dialog session
    val currentAssignments = remember { mutableStateMapOf<String, String>().apply { putAll(initialAssignments) } }
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Title
                Text("Assign Multipliers for:", style = MaterialTheme.typography.labelMedium)
                Text(product.name, style = MaterialTheme.typography.headlineSmall)
                Text("Total Quantity: $totalQuantity", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))


                Divider()

                if (availableMultipliers.isEmpty()) {
                    // Handle No Multipliers Defined
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp)) {
                        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text("No global multipliers defined.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    // Multiplier List
                    Text("Available Multipliers:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 350.dp).fillMaxWidth()) {
                        items(items = availableMultipliers, key = { it.id }) { multiplier ->
                            // State for the quantity input specific to this row
                            var quantityString by remember(multiplier.id, currentAssignments[multiplier.id]) {
                                mutableStateOf(currentAssignments[multiplier.id] ?: "")
                            }
                            val isSelected = currentAssignments.containsKey(multiplier.id)

                            AssignMultiplierQuantityRow(
                                multiplier = multiplier,
                                isSelected = isSelected,
                                quantityString = quantityString,
                                onSelectionChange = { selected ->
                                    if (selected) {
                                        // Add to map, default qty maybe 1 or total remaining? Let's default empty.
                                        currentAssignments[multiplier.id] = ""
                                        quantityString = ""
                                    } else {
                                        currentAssignments.remove(multiplier.id)
                                        quantityString = ""
                                    }
                                },
                                onQuantityChange = { newQtyString ->
                                    if (newQtyString.all { it.isDigit() }) {
                                        quantityString = newQtyString
                                        if (isSelected) { // Only update map if selected
                                            currentAssignments[multiplier.id] = newQtyString
                                        }
                                    }
                                }
                            )
                            Divider()
                        } // End items loop
                    } // End LazyColumn
                } // End else (multipliers exist)

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            Log.d("AssignMultiplierDialog", "Confirm clicked. Clearing focus.")

                            focusManager.clearFocus(force = true) // <-- ADD THIS
                            onConfirm(currentAssignments.toMap()) },
                    ) {
                        Text("Confirm")
                    }
                } // End Button Row
            } // End Column
        } // End Card
    } // End Dialog
}


// --- Helper Composable for a single row in the assignment list ---
@Composable
private fun AssignMultiplierQuantityRow(
    multiplier: Multiplier,
    isSelected: Boolean,
    quantityString: String,
    onSelectionChange: (Boolean) -> Unit,
    onQuantityChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val quantityInt = quantityString.toIntOrNull()
    // Error if selected, field is not blank, but cannot parse to int OR is negative
    val hasError = isSelected && quantityString.isNotBlank() && (quantityInt == null || quantityInt < 0)

    Row(
        modifier = Modifier.fillMaxWidth().clickable { onSelectionChange(!isSelected) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChange
        )
        Spacer(Modifier.width(8.dp))

        // Multiplier Name and Default Value
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(multiplier.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            val valueText = when (multiplier.type) {
                MultiplierType.PERCENTAGE -> "(${formatPercentage(multiplier.value)})"
                MultiplierType.FIXED_PER_UNIT -> "(${formatCurrency(multiplier.value)} / unit)"
            }
            Text("Default: ${multiplier.type.name.lowercase().replaceFirstChar { it.titlecase() }} $valueText",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // --- Quantity Input ---
        OutlinedTextField(
            value = quantityString,
            onValueChange = onQuantityChange,
            label = { Text("Apply Qty") },
            enabled = isSelected, // Only enable if checkbox is selected
            isError = hasError,
            modifier = Modifier.width(100.dp), // Smaller width for quantity
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
        )
    }
}