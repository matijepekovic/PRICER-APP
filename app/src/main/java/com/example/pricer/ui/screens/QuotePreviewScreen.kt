package com.example.pricer.ui.screens

// Imports (ensure Icons.Default.VerticalSplit is available or choose another like ContentCut)
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.* // Includes VerticalSplit, Info, Percent, PictureAsPdf
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pricer.data.model.DialogState
import com.example.pricer.data.model.MultiplierType
import com.example.pricer.data.model.Quote
import com.example.pricer.data.model.QuoteItem
import com.example.pricer.util.formatCurrency
import com.example.pricer.util.formatPercentage
import com.example.pricer.viewmodel.MainViewModel
import android.util.Log // Keep Log import if needed elsewhere, or remove if unused

/**
 * Screen composable for previewing the generated quote.
 * Shows customer details, line items with multipliers, totals, and allows PDF generation.
 * Includes functionality to split line items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotePreviewScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onGeneratePdfClick: () -> Unit
) {
    val quote by viewModel.currentQuote.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quote Preview") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { viewModel.showDialog(DialogState.QUOTE_DETAILS) }) { Icon(Icons.Default.Info, "Edit Details") }
                    IconButton(onClick = { viewModel.showDialog(DialogState.SET_TAX) }) { Icon(Icons.Default.Percent, "Set Tax") }
                    IconButton(onClick = { viewModel.showDialog(DialogState.SET_DISCOUNT) }) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer, // Or Sell, Loyalty, Percent
                            contentDescription = "Set Global Discount"
                        )
                    }
                    IconButton(onClick = { viewModel.showDialog(DialogState.ADD_CUSTOM_ITEM) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Custom Item"
                        )
                    }
                    IconButton(onClick = { viewModel.showDialog(DialogState.ADD_VOUCHER) }) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber, // Voucher/Coupon icon
                            contentDescription = "Add Voucher"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            val currentQuoteData = quote
            if (currentQuoteData != null && currentQuoteData.items.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    // --- CHANGE ICON AND TEXT ---
                    icon = { Icon(Icons.Filled.SaveAs, contentDescription = null) }, // Example: SaveAs, Task, PersonAdd
                    text = { Text("Save to Prospects") },
                    // --- END CHANGE ---
                    onClick = onGeneratePdfClick // This callback still triggers launchPdfGeneration in MainActivity
                )
            }
        }
    ) { scaffoldPadding ->

        val quoteData = quote

        if (quoteData == null || quoteData.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(scaffoldPadding).padding(16.dp), contentAlignment = Alignment.Center) {
                Text(if (quote == null) "Generating quote..." else "No items in the quote.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(scaffoldPadding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header Section
            item(key = "quote_header") {
                Text("Quote For: ${quoteData.customerName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                if (quoteData.companyName.isNotBlank()) { Text("From: ${quoteData.companyName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(2.dp)) }
                if (quoteData.customMessage.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(quoteData.customMessage, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp)) }
                Divider(Modifier.padding(vertical = 12.dp))
            }

            // Line Items Header Row
            item(key = "line_item_header") {
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Item", Modifier.weight(2.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Text("Qty", Modifier.weight(0.6f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, style = MaterialTheme.typography.labelLarge)
                    Text("Price", Modifier.weight(1.0f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, style = MaterialTheme.typography.labelLarge)
                    Text("Total", Modifier.weight(1.1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, style = MaterialTheme.typography.labelLarge)
                    // Adjust spacer width to account for TWO icon buttons now
                    Spacer(modifier = Modifier.width(40.dp)) // Increased width
                }
                Divider(thickness = 1.dp); Spacer(Modifier.height(8.dp))
            }

            // Quote Items List
            itemsIndexed(
                items = quoteData.items,
                key = { _, item -> "item_${item.id}" }
            ) { index, item ->
                QuoteLineItem( // *** UPDATED CALL ***
                    item = item,
                    onRemoveItem = { viewModel.removeQuoteItem(item.id) } // Pass item to split
                )
                if (index < quoteData.items.lastIndex) {
                    Divider(modifier = Modifier.padding(vertical = 6.dp), thickness = 1.dp) // Make divider slightly thicker
                }
            }

            // Totals Section
            item(key = "quote_totals") {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // --- Use CORRECT property names from Quote data class ---
                TotalsRow(label = "Subtotal:", value = formatCurrency(quoteData.subtotalBeforeDiscount)) // Use BeforeDiscount

                if (quoteData.totalDiscountAmount > 0) {
                    TotalsRow(
                        label = "Discount (${formatPercentage(quoteData.globalDiscountRate)}):",
                        value = "-${formatCurrency(quoteData.totalDiscountAmount)}" // Use totalDiscountAmount
                    )
                    TotalsRow(
                        label = "Subtotal (After Discount):",
                        value = formatCurrency(quoteData.subtotalAfterDiscount), // Use AfterDiscount
                        isBold = false
                    )
                }

                TotalsRow(label = "Tax (${formatPercentage(quoteData.taxRate)}):", value = formatCurrency(quoteData.taxAmount)) // Use taxAmount (already correct)
                TotalsRow(label = "Grand Total:", value = formatCurrency(quoteData.grandTotal), isBold = true) // Use grandTotal (already correct)
            }
        } // End LazyColumn
    } // End Scaffold
} // End QuotePreviewScreen


// --- Helper Composable for a single Quote Line Item ---
@Composable
private fun QuoteLineItem(
    item: QuoteItem,
    onRemoveItem: () -> Unit,

) {
    // Determine if this is a voucher (negative price item)
    val isVoucher = item.product.basePrice < 0 && item.product.category == "Voucher"
    val textColor = if (isVoucher) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Item Name, Description, Multipliers Column
        Column(modifier = Modifier.weight(2.5f).padding(end = 8.dp)) {
            Text( text = item.product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,color = textColor )
            if (item.product.description.isNotEmpty()) {
                // Removed maxLines/overflow to show full description
                Text( text = item.product.description, style = MaterialTheme.typography.bodySmall, color = if (isVoucher) textColor else MaterialTheme.colorScheme.onSurfaceVariant )
            }
            item.appliedMultipliers.forEach { mult ->
                val valueStr = when (mult.type) { MultiplierType.PERCENTAGE -> formatPercentage(mult.appliedValue); MultiplierType.FIXED_PER_UNIT -> "${formatCurrency(mult.appliedValue)}/unit" }
                Text( text = "  + ${mult.name} ($valueStr)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary )
            }
        }
        // Quantity Text
        Text( text = item.quantity.toString(), modifier = Modifier.weight(0.6f).padding(top = 2.dp), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End,  color = textColor )
        // Unit Price Text
        Text( text = formatCurrency(item.product.basePrice), modifier = Modifier.weight(1.0f).padding(top = 2.dp), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End ,  color = textColor)
        // Line Total Text
        Text( text = formatCurrency(item.lineTotalBeforeDiscount), modifier = Modifier.weight(1.1f).padding(top = 2.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End ,  color = textColor)

        // --- Action Buttons Column (to stack them vertically if needed, or keep in Row) ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) { // Stack buttons vertically
            // Remove Item Button
            IconButton(
                onClick = onRemoveItem,
                modifier = Modifier.size(40.dp),// Keep size consistent

            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Remove ${item.product.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        } // End Action Buttons Column
    } // End Main Row
} // End QuoteLineItem


// --- Helper Composable for Totals Row --- (Keep as is)
@Composable
private fun TotalsRow( label: String, value: String, isBold: Boolean = false ) {
    Row( modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically ) {
        Text( text = label, textAlign = TextAlign.End, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal )
        Spacer(modifier = Modifier.width(24.dp))
        Text( text = value, textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal )
    }
}