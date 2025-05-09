package com.example.pricer.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pricer.data.model.Product
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (Product, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var unitType by remember { mutableStateOf("unit") }
    var basePriceString by remember { mutableStateOf("") }
    var quantityString by remember { mutableStateOf("1") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }
    var isDiscountable by remember { mutableStateOf(true) }

    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Add Custom Item",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text("Product Name*") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Item is Discountable?", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isDiscountable,
                        onCheckedChange = { isDiscountable = it }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = false,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = basePriceString,
                        onValueChange = { newValue ->
                            if (newValue.matches(Regex("^\\d*\\.?\\d*$")) || newValue.isEmpty()) {
                                basePriceString = newValue; priceError = null
                            }
                        },
                        label = { Text("Price*") },
                        isError = priceError != null,
                        supportingText = priceError?.let { { Text(it) } },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        leadingIcon = { Text(text = "$") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = unitType,
                        onValueChange = { unitType = it },
                        label = { Text("Unit Type") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = quantityString,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                            quantityString = newValue; quantityError = null
                        }
                    },
                    label = { Text("Quantity*") },
                    isError = quantityError != null,
                    supportingText = quantityError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // Validation
                            var isValid = true

                            if (name.isBlank()) {
                                nameError = "Name cannot be empty"
                                isValid = false
                            }

                            val price = basePriceString.toDoubleOrNull()
                            if (price == null || price < 0) {
                                priceError = "Enter a valid price"
                                isValid = false
                            }

                            val quantity = quantityString.toIntOrNull()
                            if (quantity == null || quantity <= 0) {
                                quantityError = "Enter a valid quantity"
                                isValid = false
                            }

                            if (isValid) {
                                focusManager.clearFocus()

                                val customProduct = Product(
                                    id = UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    description = description.trim(),
                                    unitType = unitType.trim().ifBlank { "unit" },
                                    basePrice = price!!,
                                    category = "Custom", // Mark as custom category
                                    isDiscountable = isDiscountable
                                )

                                onConfirm(customProduct, quantity!!)
                            }
                        }
                    ) {
                        Text("Add to Quote")
                    }
                }
            }
        }
    }
}