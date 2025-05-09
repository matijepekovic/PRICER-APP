package com.example.pricer.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
fun AddVoucherDialog(
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit
) {
    var voucherName by remember { mutableStateOf("Voucher") }
    var voucherCode by remember { mutableStateOf("") }
    var amountString by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Add Voucher",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = voucherName,
                    onValueChange = { voucherName = it; nameError = null },
                    label = { Text("Voucher Description*") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = voucherCode,
                    onValueChange = { voucherCode = it },
                    label = { Text("Voucher Code (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountString,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d*\\.?\\d*$")) || newValue.isEmpty()) {
                            amountString = newValue; amountError = null
                        }
                    },
                    label = { Text("Amount*") },
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    leadingIcon = { Text(text = "$") },
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

                            if (voucherName.isBlank()) {
                                nameError = "Description cannot be empty"
                                isValid = false
                            }

                            val amount = amountString.toDoubleOrNull()
                            if (amount == null || amount <= 0) {
                                amountError = "Enter a valid amount greater than 0"
                                isValid = false
                            }

                            if (isValid) {
                                focusManager.clearFocus()

                                // Build description with code if provided
                                val description = if (voucherCode.isBlank()) ""
                                else "Code: $voucherCode"

                                // Create a product with negative price to represent the voucher
                                val voucherProduct = Product(
                                    id = "voucher_" + UUID.randomUUID().toString(),
                                    name = voucherName.trim(),
                                    description = description,
                                    unitType = "voucher",
                                    basePrice = -amount!!, // Negative amount to reduce total
                                    category = "Voucher",
                                    isDiscountable = false // Vouchers shouldn't be discounted
                                )

                                onConfirm(voucherProduct)
                            }
                        }
                    ) {
                        Text("Apply Voucher")
                    }
                }
            }
        }
    }
}