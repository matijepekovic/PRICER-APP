package com.example.pricer.ui.dialogs

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.* // remember, mutableStateOf, etc.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pricer.data.model.Product
import java.util.UUID

/**
 * A Dialog composable for adding a new product or editing an existing one.
 * Includes fields for name, description, unit type, base price, and category.
 * Performs basic validation before confirming.
 *
 * @param productToEdit The [Product] object to edit. If null, the dialog is in 'Add' mode.
 * @param onDismiss Lambda function to be invoked when the dialog should be dismissed.
 * @param onConfirm Lambda function to be invoked when the Add/Save button is clicked. Passes the new or updated [Product].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    productToEdit: Product?,
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit
) {
    // --- State Management ---
    var name by remember(productToEdit) { mutableStateOf(productToEdit?.name ?: "") }
    var description by remember(productToEdit) { mutableStateOf(productToEdit?.description ?: "") }
    var unitType by remember(productToEdit) { mutableStateOf(productToEdit?.unitType ?: "unit") }
    var basePriceString by remember(productToEdit) { mutableStateOf(productToEdit?.basePrice?.takeIf { it != 0.0 }?.toString() ?: "") }
    var category by remember(productToEdit) { mutableStateOf(productToEdit?.category ?: "Default") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var isDiscountable by remember(productToEdit) { mutableStateOf(productToEdit?.isDiscountable ?: true) }
    // --- Controllers & Requesters ---
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val saveButtonFocusRequester = remember { FocusRequester() } // For moving focus on Done

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // --- Title ---
                Text(
                    text = if (productToEdit == null) "Add New Product" else "Edit Product",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )



                // --- Product Name Input ---
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text("Product Name*") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next // ACTION: NEXT
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Item is Discountable?", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isDiscountable,
                        onCheckedChange = { isDiscountable = it }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp)) // Adjust spacing before buttons
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), // Give some space
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text, // Specify Text type
                        imeAction = ImeAction.Default // Use Default for multi-line Enter key
                    ),
                    singleLine = false, // Allow multi-line
                    maxLines = 90 // Limit visible expansion
                )
                Spacer(modifier = Modifier.height(8.dp))
                // --- *** END DESCRIPTION FIELD BLOCK *** ---
                // --- Price and Unit Type Row ---
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {


                    // --- Base Price Input ---


                    OutlinedTextField(
                        value = basePriceString,
                        onValueChange = { newValue -> if (newValue.matches(Regex("^\\d*\\.?\\d*\$")) || newValue.isEmpty()) { basePriceString = newValue; priceError = null } },
                        label = { Text("Base Price*") },
                        isError = priceError != null,
                        supportingText = priceError?.let { { Text(it) } },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next // ACTION: NEXT
                        ),
                        leadingIcon = { Text(text = "$") },
                        singleLine = true
                    )
                    // --- Unit Type Input ---
                    OutlinedTextField(
                        value = unitType,
                        onValueChange = { unitType = it },
                        label = { Text("Unit Type") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Next // ACTION: NEXT
                        ),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // --- Category Input ---
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        // ACTION: DONE
                    ),
                    // --- CHANGE KEYBOARD ACTION ---
                    keyboardActions = KeyboardActions(
                        onNext = { // <<< CHANGE to onNext instead of onDone
                            Log.d("AddEditProductDialog", "onNext on Category field: Requesting focus on Save Button.")
                            keyboardController?.hide()// Move focus to the Save button
                            // DO NOT call keyboardController.hide() here - let focus change handle it
                    // --- END CHANGE ---
                        }
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))

                // --- Action Buttons ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    // Cancel Button
                    TextButton(onClick = { keyboardController?.hide(); onDismiss() }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Save/Add Button
                    // --- Confirm Button (Add or Save) ---
                    Button(
                        onClick = { // Start of onClick lambda
                            Log.d("AddEditProductDialog", "Save/Add button clicked.")

                            // --- 1. Validation ---
                            val priceDouble = basePriceString.toDoubleOrNull()
                            var isValid = true // Assume valid initially

                            // Validate Name
                            if (name.isBlank()) {
                                nameError = "Name cannot be empty"
                                isValid = false
                            } else {
                                nameError = null
                            }

                            // Validate Price
                            if (priceDouble == null || priceDouble < 0) {
                                priceError = "Enter a valid price (e.g., 0 or more)"
                                isValid = false
                            } else {
                                priceError = null
                            }
                            // --- End Validation ---


                            // --- 2. Process if Valid ---
                            if (isValid) {
                                Log.d("AddEditProductDialog", "Validation passed. Processing and confirming.")

                                // --- 2a. Process Defaults ---
                                val finalUnitType = unitType.trim().ifBlank { "unit" }
                                val categoryToSave = category.trim().ifBlank { "Default" } // Declare and process HERE

                                // --- 2b. Create Final Product Object ---
                                val finalProduct = Product(
                                    id = productToEdit?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    description = description.trim(),
                                    unitType = finalUnitType, // Use processed value
                                    basePrice = priceDouble!!, // Safe due to validation
                                    category = categoryToSave, // Use processed value
                                    isDiscountable = isDiscountable // Use state variable
                                )

                                // --- 2c. Hide Keyboard ---
                                keyboardController?.hide()

                                // --- 2d. Call Confirmation Callback ---
                                onConfirm(finalProduct)

                            } else {
                                Log.d("AddEditProductDialog", "Validation failed.")
                                // Don't hide keyboard if validation fails
                            }
                        }, // <<< End of onClick lambda

                        modifier = Modifier.focusRequester(saveButtonFocusRequester) // Modifier applied correctly here

                    ) { // Start of Button content lambda
                        Text(if (productToEdit == null) "Add" else "Save")
                    } // End of Button// End Button Row
            } // End Column
        } // End Card
    } // End Dialog
}}