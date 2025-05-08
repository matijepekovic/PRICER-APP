package com.example.pricer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Functions // Example icon for "assign functions/multipliers"
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.* // Needed for basic compose state functions if used internally
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pricer.data.model.Product
import com.example.pricer.util.formatCurrency // Import the formatting util

/**
 * A Composable function that displays a single row for a Product in the catalog list.
 * It includes product details, a quantity input field, and interaction buttons.
 *
 * @param product The data object for the product to display.
 * @param quantity The current quantity entered for this product (as a String for TextField).
 * @param assignedMultiplierSummary A string summarizing assigned multipliers (e.g., "Discount, Surcharge"). Null or empty if none.
 * @param onQuantityChange Callback invoked when the quantity input value changes. Passes the new String value.
 * @param onAssignMultiplierClick Callback invoked when the assign multiplier button is clicked. Passes the product.
 * @param onRowClick Callback invoked when the main body of the row is clicked. Typically used to edit the product. Passes the product.
 * @param modifier Optional Modifier for customizing the row container.
 */
@OptIn(ExperimentalMaterial3Api::class) // For OutlinedTextField, Card potentially
@Composable
fun ProductRow(
    product: Product,
    quantity: String, // Use String for TextField compatibility
    category: String,
    assignedMultiplierSummary: String?, // TODO: Populate this based on ViewModel state later
    onQuantityChange: (String) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onAssignMultiplierClick: (Product) -> Unit,
    onRowClick: (Product) -> Unit, // For editing
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            // Make the whole card clickable for editing the product
            .clickable { onRowClick(product) },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), // Subtle elevation
        shape = MaterialTheme.shapes.medium // Use standard shape

    ) {
        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically // Center items vertically within the Row

        ) {


            // --- Product Details Column (Takes flexible space) ---
            Column(
                modifier = Modifier
                    .weight(1f) // *Crucial:* This column takes up all available space LEFT-TO-RIGHT first
                    .padding(end = 8.dp) // Space between details and quantity
            ) {
                // Product Name

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp)) // Small space

                // Category
                if (category.isNotBlank() && category != "Default") {
                    Text(
                        text = "Category: $category",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Description
                if (product.description.isNotBlank()) {
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp)) // Slightly more space before price
                }

                // Price / Unit
                Text(
                    text = "${formatCurrency(product.basePrice)} / ${product.unitType}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal
                )

                // Modifiers Summary
                if (!assignedMultiplierSummary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Modifiers: $assignedMultiplierSummary",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

            } // End Column (Weight 1f)
// Delete Product Button
            IconButton(
                onClick = { onDeleteProduct(product.id) }, // Calls the callback passed in
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline, // The trash can icon
                    contentDescription = "Delete ${product.name}", // For accessibility
                    tint = MaterialTheme.colorScheme.error // Make it red
                )
            }
            // --- Quantity Input Field (Fixed Width) ---
            // This now comes AFTER the weighted column
            OutlinedTextField(
                value = quantity,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) { onQuantityChange(newValue) }
                },
                label = { Text("Qty") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .width(90.dp) // Keep fixed width
                    .padding(start = 4.dp, end = 0.dp), // Add padding *before* it
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            )

            // --- Assign Multiplier Button (Default Size) ---
            // This also comes AFTER the weighted column
            IconButton(
                onClick = { onAssignMultiplierClick(product) },
                // Modifier.padding(start=4.dp) // Optional padding if needed
            ) {
                Icon(
                    imageVector = Icons.Filled.Functions,
                    contentDescription = "Assign Multipliers to ${product.name}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            }
            }}