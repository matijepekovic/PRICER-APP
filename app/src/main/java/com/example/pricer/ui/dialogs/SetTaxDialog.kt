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
 * A Dialog composable for setting the global tax rate percentage.
 *
 * @param initialTaxRate The currently set tax rate to pre-fill the input field.
 * @param onDismiss Lambda function invoked when the dialog should be dismissed.
 * @param onConfirm Lambda function invoked when the "Set" button is clicked. Passes the new validated tax rate (Double).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetTaxDialog(
    initialTaxRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    // State for the input field (using String for TextField)
    var taxRateString by remember { mutableStateOf(initialTaxRate.toString()) }
    // State for validation error message
    var error by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally // Center title maybe?
            ) {
                // --- Dialog Title ---
                Text(
                    text = "Set Tax Rate",
                    style = MaterialTheme.typography.headlineSmall, // Dialog title style
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // --- Tax Rate Input ---
                OutlinedTextField(
                    value = taxRateString,
                    onValueChange = { newValue ->
                        // Allow only valid decimal input
                        if (newValue.matches(Regex("^\\d*\\.?\\d*\$"))) {
                            taxRateString = newValue
                            error = null // Clear error on valid change
                        } else if (newValue.isEmpty()) {
                            taxRateString = ""
                            error = null
                        }
                    },
                    label = { Text("Tax Rate") },
                    trailingIcon = { Text("%") }, // Show percent sign inside field
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal, // Use decimal keyboard
                        imeAction = ImeAction.Done // Use 'Done' as it's the only field
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() } // Hide keyboard on Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null, // Highlight field if error exists
                    supportingText = error?.let { { Text(it) } }, // Show error message
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- Action Buttons ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End // Align buttons to the right
                ) {
                    // Cancel Button
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp)) // Space between buttons

                    // Set Button
                    Button(onClick = {
                        // --- Validation ---
                        val rate = taxRateString.toDoubleOrNull()
                        if (rate != null && rate >= 0) {
                            // Valid rate, call confirm callback
                            onConfirm(rate)
                        } else {
                            // Invalid input, show error message
                            error = "Enter a valid rate (e.g., 0 or more)"
                        }
                        // --- End Validation ---
                    }) {
                        Text("Set")
                    }
                } // End Button Row
            } // End Column
        } // End Card
    } // End Dialog
}