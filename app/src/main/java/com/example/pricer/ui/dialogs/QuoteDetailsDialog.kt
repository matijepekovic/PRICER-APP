package com.example.pricer.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * A Dialog to add or edit optional details for the current quote, such as
 * the user's company name and a custom message for the customer.
 *
 * @param initialCompanyName The current company name value from the quote/ViewModel state.
 * @param initialCustomMessage The current custom message value from the quote/ViewModel state.
 * @param onDismiss Lambda function invoked when the dialog should be dismissed.
 * @param onConfirm Lambda function invoked when the Save button is clicked. Passes the potentially updated company name and custom message.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailsDialog(
    initialCompanyName: String,
    initialCustomMessage: String,
    onDismiss: () -> Unit,
    onConfirm: (companyName: String, customMessage: String) -> Unit
) {
    // State for the input fields, initialized with values from ViewModel/Quote state
    var companyName by remember { mutableStateOf(initialCompanyName) }
    var customMessage by remember { mutableStateOf(initialCustomMessage) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    // Allow vertical scrolling if content becomes too long
                    .verticalScroll(rememberScrollState())
            ) {
                // --- Dialog Title ---
                Text(
                    text = "Quote Details (Optional)",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // --- Company Name Input ---
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Your Company Name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words, // Capitalize company names
                        imeAction = ImeAction.Next // Move focus to next field
                    ),
                    singleLine = true // Usually company names are single line
                )
                Spacer(modifier = Modifier.height(8.dp))

                // --- Custom Message Input ---
                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { customMessage = it },
                    label = { Text("Custom Message for Customer") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp), // Make message field taller
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences, // Standard sentence capitalization
                        imeAction = ImeAction.Default // Use default action (often newline or done)
                    ),
                    maxLines = 6 // Allow multiple lines
                )
                Spacer(modifier = Modifier.height(24.dp))

                // --- Action Buttons ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End // Align buttons right
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        // No specific validation needed here usually, just pass values back
                        onConfirm(companyName.trim(), customMessage.trim()) // Trim whitespace
                    }) {
                        Text("Save")
                    }
                } // End Button Row
            } // End Column
        } // End Card
    } // End Dialog
}