package com.example.pricer.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * A Dialog composable for setting the global discount rate percentage.
 *
 * @param initialDiscountRate The currently set discount rate to pre-fill the input field.
 * @param onDismiss Lambda function invoked when the dialog should be dismissed.
 * @param onConfirm Lambda function invoked when the "Set" button is clicked. Passes the new validated discount rate (Double).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetDiscountDialog(
    initialDiscountRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    // State for the input field (using String for TextField)
    var discountRateString by remember { mutableStateOf(initialDiscountRate.toString()) }
    // State for validation error message
    var error by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Dialog Title ---
                Text(
                    text = "Set Global Discount",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // --- Discount Rate Input ---
                OutlinedTextField(
                    value = discountRateString,
                    onValueChange = { newValue ->
                        // Allow only valid decimal input (0-100 range checked on confirm)
                        if (newValue.matches(Regex("^\\d*\\.?\\d*\$")) || newValue.isEmpty()) {
                            discountRateString = newValue
                            error = null // Clear error on valid change
                        }
                    },
                    label = { Text("Discount Rate") },
                    trailingIcon = { Text("%") }, // Show percent sign
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- Action Buttons ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        // --- Validation ---
                        val rate = discountRateString.toDoubleOrNull()
                        if (rate != null && rate >= 0 && rate <= 100) { // Validate 0-100 range
                            onConfirm(rate) // Call confirm callback
                        } else {
                            error = "Enter a valid rate (0-100)" // Show error
                        }
                    }) {
                        Text("Set")
                    }
                } // End Button Row
            } // End Column
        } // End Card
    } // End Dialog
}