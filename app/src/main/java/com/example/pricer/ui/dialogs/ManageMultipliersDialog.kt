package com.example.pricer.ui.dialogs

import android.util.Log // Import Log if needed for debugging inside
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Add, Edit, Delete, Save, Check, Close, Cancel
import androidx.compose.material.icons.outlined.Info // For empty list
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pricer.data.model.Multiplier
import com.example.pricer.data.model.MultiplierType
import com.example.pricer.util.formatCurrency
import com.example.pricer.util.formatPercentage
import java.util.UUID

// --- Unique ID marker for Add Mode ---
private const val ADD_MODE_ID = "---ADD_NEW_MULTIPLIER---"

/**
 * Dialog composable for managing global Multipliers (Add, View, Edit, Delete).
 * Includes discountable toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMultipliersDialog(
    multipliers: List<Multiplier>,
    onDismiss: () -> Unit,
    onAddMultiplier: (Multiplier) -> Unit,
    onUpdateMultiplier: (Multiplier) -> Unit,
    onDeleteMultiplier: (String) -> Unit
) {
    // --- State Management within the Dialog ---
    var editingMultiplierId by remember { mutableStateOf<String?>(null) } // ID or ADD_MODE_ID or null
    // State for the input fields - these are now primarily managed *within* EditMultiplierFields,
    // but we need temporary holders to pass initial values and receive updates if needed,
    // or we can load directly in LaunchedEffect within EditMultiplierFields.
    // Let's keep temporary holders here for simplicity of passing initial state.
    var editName by remember { mutableStateOf("") }
    var editValueString by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf(MultiplierType.PERCENTAGE) }
    var editIsDiscountable by remember { mutableStateOf(true) }

    // State to control confirmation dialog for delete
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }

    // Effect to load data into temporary holders when starting edit/add
    LaunchedEffect(editingMultiplierId) {
        val isAdding = editingMultiplierId == ADD_MODE_ID
        val multToEdit = if (!isAdding) multipliers.find { it.id == editingMultiplierId } else null

        editName = if (isAdding) "" else multToEdit?.name ?: ""
        editValueString = if (isAdding) "" else multToEdit?.value?.takeIf { it != 0.0 }?.toString() ?: ""
        editType = if (isAdding) MultiplierType.PERCENTAGE else multToEdit?.type ?: MultiplierType.PERCENTAGE
        editIsDiscountable = multToEdit?.isDiscountable ?: true // Load initial discountable state
    }

    // --- Main Dialog Structure ---
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Manage Multipliers",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // --- Add/Edit Section ---
                // Conditionally display the Edit/Add fields based on editingMultiplierId
                if (editingMultiplierId != null) {
                    EditMultiplierFields(
                        isAdding = editingMultiplierId == ADD_MODE_ID,
                        initialName = editName, // Pass initial values
                        initialValueString = editValueString,
                        initialType = editType,
                        initialIsDiscountable = editIsDiscountable,
                        onSave = { name, value, type, isDiscountableFlag -> // Receive all values back
                            val multiplier = Multiplier(
                                id = if (editingMultiplierId == ADD_MODE_ID) UUID.randomUUID().toString() else editingMultiplierId!!,
                                name = name, value = value, type = type,
                                isDiscountable = isDiscountableFlag // Include flag
                            )
                            if (editingMultiplierId == ADD_MODE_ID) {
                                onAddMultiplier(multiplier)
                            } else {
                                onUpdateMultiplier(multiplier)
                            }
                            editingMultiplierId = null // Exit edit/add mode
                        },
                        onCancel = { editingMultiplierId = null } // Exit edit/add mode
                    )
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                }

                // --- Show List and Add Button only when NOT adding/editing ---
                if (editingMultiplierId == null) {
                    // Button to Enter Add Mode
                    Button(
                        onClick = { editingMultiplierId = ADD_MODE_ID }, // Set special ID
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Add New Multiplier")
                    }
                    Spacer(Modifier.height(12.dp))

                    // List of Existing Multipliers
                    Text("Existing Multipliers:", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (multipliers.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                            Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            Text("No multipliers defined yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 250.dp).fillMaxWidth()) {
                            items(items = multipliers, key = { it.id }) { multiplier ->
                                MultiplierListItem( // Use the helper
                                    multiplier = multiplier,
                                    onEditClick = { editingMultiplierId = multiplier.id }, // Set ID to edit
                                    onDeleteClick = { showDeleteConfirmation = multiplier.id } // Trigger confirmation
                                )
                                Divider()
                            }
                        }
                    }

                    // Dialog Close Button
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Close") }
                    }
                } // End if (not editing/adding)

            } // End Main Column
        } // End Card

        // --- Delete Confirmation Dialog ---
        if (showDeleteConfirmation != null) {
            val multiplierToDelete = multipliers.find { it.id == showDeleteConfirmation }
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = null },
                icon = { Icon(Icons.Default.Warning, null) },
                title = { Text("Confirm Deletion") },
                text = { Text("Delete multiplier \"${multiplierToDelete?.name ?: ""}\"? This cannot be undone.") },
                confirmButton = { Button(onClick = { if(showDeleteConfirmation != null) onDeleteMultiplier(showDeleteConfirmation!!); showDeleteConfirmation = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete", color = MaterialTheme.colorScheme.onError) } },
                dismissButton = { TextButton(onClick = { showDeleteConfirmation = null }) { Text("Cancel") } }
            )
        } // End Delete Confirmation

    } // End Dialog
} // End ManageMultipliersDialog


// --- Helper Composable for the Add/Edit Input Fields ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMultiplierFields(
    isAdding: Boolean,
    initialName: String,
    initialValueString: String,
    initialType: MultiplierType,
    initialIsDiscountable: Boolean,
    onSave: (name: String, value: Double, type: MultiplierType, isDiscountable: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    // Internal state for the fields, initialized from parameters
    var name by remember(initialName) { mutableStateOf(initialName) }
    var valueString by remember(initialValueString) { mutableStateOf(initialValueString) }
    var type by remember(initialType) { mutableStateOf(initialType) }
    var isDiscountable by remember(initialIsDiscountable) { mutableStateOf(initialIsDiscountable) }

    // Validation state
    var nameError by remember { mutableStateOf<String?>(null) }
    var valueError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    Column {
        Text(if(isAdding) "Add New Multiplier Details" else "Edit Multiplier", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        // Name Field
        OutlinedTextField( value = name, onValueChange = { name = it; nameError = null }, label = { Text("Multiplier Name*") }, isError = nameError != null, supportingText = nameError?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, capitalization = KeyboardCapitalization.Words) )
        Spacer(Modifier.height(8.dp))

        // Type Selector
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Text("Type:", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 8.dp))
            MultiplierType.entries.forEach { multiplierType ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { type = multiplierType }) {
                    RadioButton( selected = type == multiplierType, onClick = { type = multiplierType } )
                    Text(multiplierType.name.lowercase().replaceFirstChar { it.titlecase() }, modifier = Modifier.padding(end = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Value Field
        OutlinedTextField( value = valueString, onValueChange = { newValue -> if (newValue.matches(Regex("^\\d*\\.?\\d*\$")) || newValue.isEmpty()) { valueString = newValue; valueError = null } }, label = { Text("Value*") }, isError = valueError != null, supportingText = valueError?.let { { Text(it) } }, leadingIcon = if (type == MultiplierType.FIXED_PER_UNIT) ({ Text("$") }) else null, trailingIcon = if (type == MultiplierType.PERCENTAGE) ({ Text("%") }) else null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth(), singleLine = true )
        Spacer(Modifier.height(8.dp))

        // Discountable Switch
        Row( modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween ) {
            Text("Multiplier is Discountable?", style = MaterialTheme.typography.bodyMedium)
            Switch( checked = isDiscountable, onCheckedChange = { isDiscountable = it } )
        }
        Spacer(Modifier.height(16.dp))

        // Save/Cancel Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Cancel") } // Calls the cancel lambda
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                // Validation
                val valueDouble = valueString.toDoubleOrNull()
                var isValid = true
                if (name.isBlank()){ nameError = "Name cannot be empty"; isValid = false } else { nameError = null}
                if (valueDouble == null || valueDouble < 0) { valueError = "Enter a valid value (0+)"; isValid = false } else {valueError = null}

                if(isValid) {
                    focusManager.clearFocus() // Clear focus before saving
                    onSave(name.trim(), valueDouble!!, type, isDiscountable) // Call save lambda with all values
                }
            }) {
                Icon(Icons.Filled.Save, null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Save")
            }
        }
    } // End Column for edit fields
} // End EditMultiplierFields


// --- Helper Composable for displaying a single multiplier in the list ---
@Composable
private fun MultiplierListItem(
    multiplier: Multiplier,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(multiplier.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            val valueText = when (multiplier.type) {
                MultiplierType.PERCENTAGE -> formatPercentage(multiplier.value)
                MultiplierType.FIXED_PER_UNIT -> "${formatCurrency(multiplier.value)} / unit"
            }
            Row(verticalAlignment = Alignment.CenterVertically) { // Row for type/value and discountable status
                Text( "${multiplier.type.name.lowercase().replaceFirstChar { it.titlecase() }} ($valueText)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant )
                // Display discountable status
                if (!multiplier.isDiscountable) {
                    Spacer(Modifier.width(4.dp))
                    Text( "(Not Discountable)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline )
                }
            }
        }
        IconButton(onClick = onEditClick) { Icon(Icons.Filled.Edit, "Edit ${multiplier.name}") }
        IconButton(onClick = onDeleteClick) { Icon(Icons.Filled.DeleteOutline, "Delete ${multiplier.name}", tint = MaterialTheme.colorScheme.error) }
    }
}