package com.example.pricer.util

import java.text.NumberFormat
import java.util.Locale

// Formats a Double value as a currency string based on the user's default locale.
// Example: 1234.56 might become "$1,234.56" (US) or "1.234,56 €" (Germany)
fun formatCurrency(value: Double): String {
    return try {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).format(value)
    } catch (e: IllegalArgumentException) {
        // Fallback in case of issues with locale data
        String.format(Locale.US, "$%.2f", value) // Default to US format
    }
}

// Formats a Double value as a percentage string with two decimal places.
// Example: 12.5 becomes "12.50%"
fun formatPercentage(value: Double): String {
    // Consider locale-specific percentage formatting if needed using NumberFormat.getPercentInstance()
    // For simplicity now, using basic string formatting.
    return String.format(Locale.US, "%.2f%%", value) // Using US locale for consistency, %% escapes %
}