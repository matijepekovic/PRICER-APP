package com.example.pricer.data.model

import android.util.Log
import kotlinx.serialization.Serializable
import java.util.UUID

// Represents a single line item within a Quote.
@Serializable
data class QuoteItem(
    val id: String = UUID.randomUUID().toString(),
    val product: Product,
    var quantity: Int,
    val partialMultiplierQuantities: Map<String, Int> = emptyMap(),
    val appliedMultipliers: List<AppliedMultiplier> = emptyList(),
    val isBaseItemDiscountable: Boolean = product.isDiscountable // De
) {

    /**
     * Total line item value BEFORE any global discount.
     * Calculation: (Base Product Price * Total Item Quantity) + SUM(Individual Multiplier Adjustments)
     */
    val lineTotalBeforeDiscount: Double
        get() {
            // 1. Calculate the total base cost for all units of the product
            var calculatedTotal = product.basePrice * quantity
            Log.v("QuoteItemCalc", "Item '${product.name}': Base Cost (${quantity} units * ${product.basePrice}) = $calculatedTotal")

            // 2. Add adjustments for EACH applied multiplier
            // The adjustment is calculated based on the multiplier's value and *its own applied quantity* (Mqty)
            partialMultiplierQuantities.forEach { (multiplierId, qtyForThisMultiplier) -> // (multiplierId, Mqty)
                val multiplierDetails = appliedMultipliers.find { it.multiplierId == multiplierId }

                if (multiplierDetails != null && qtyForThisMultiplier > 0) {
                    // Ensure qtyForThisMultiplier does not exceed total item quantity if that's a business rule
                    // For now, we assume it's already validated or multipliers can apply "conceptually" beyond physical item count for certain charge types
                    val effectiveMultiplierQty = qtyForThisMultiplier.coerceAtMost(quantity) // Safety: don't apply a multiplier to more units than exist

                    val adjustmentAmount = when (multiplierDetails.type) {
                        MultiplierType.PERCENTAGE -> {
                            // Percentage of the product's BASE UNIT PRICE, applied over the multiplier's quantity
                            (product.basePrice * (multiplierDetails.appliedValue / 100.0)) * effectiveMultiplierQty
                        }
                        MultiplierType.FIXED_PER_UNIT -> {
                            // Fixed amount per unit, applied over the multiplier's quantity
                            multiplierDetails.appliedValue * effectiveMultiplierQty
                        }
                    }
                    calculatedTotal += adjustmentAmount
                    Log.v("QuoteItemCalc", " -> Adding adjustment for '${multiplierDetails.name}' ($effectiveMultiplierQty units): $adjustmentAmount. New Total: $calculatedTotal")
                }
            }

            Log.i("QuoteItemCalc", "Item '${product.name}' Qty $quantity -> Final Line Total (Before Global Disc): $calculatedTotal")
            return calculatedTotal
        }

    // Discountability logic (if you are keeping the global discount feature)
    // This needs to be re-evaluated based on the new lineTotal logic.
    // Does discount apply to the (Base Price * Total Qty) part?
    // Does discount apply to the (Multiplier Adjustment) parts?


    fun getDiscountableAmount(): Double {
        var discountableValue = 0.0

        // 1. Is the base product cost eligible for discount?
        if (isBaseItemDiscountable) {
            discountableValue += product.basePrice * quantity
            Log.v("QuoteItemCalc", "Discountable: Base cost part = ${product.basePrice * quantity}")
        }

        // 2. Are individual multiplier adjustments eligible for discount?
        partialMultiplierQuantities.forEach { (multiplierId, qtyForThisMultiplier) ->
            val multiplierDetails = appliedMultipliers.find { it.multiplierId == multiplierId }
            if (multiplierDetails != null && qtyForThisMultiplier > 0 && multiplierDetails.isDiscountable) {
                val effectiveMultiplierQty = qtyForThisMultiplier.coerceAtMost(quantity)
                val adjustmentAmount = when (multiplierDetails.type) {
                    MultiplierType.PERCENTAGE -> (product.basePrice * (multiplierDetails.appliedValue / 100.0)) * effectiveMultiplierQty
                    MultiplierType.FIXED_PER_UNIT -> multiplierDetails.appliedValue * effectiveMultiplierQty
                }
                discountableValue += adjustmentAmount
                Log.v("QuoteItemCalc", "Discountable: Adjustment from '${multiplierDetails.name}' = $adjustmentAmount")
            }
        }
        Log.d("QuoteItemCalc", "Item '${product.name}': Total Discountable Amount = $discountableValue")
        return discountableValue
    }

    fun calculateDiscountAmount(globalDiscountRate: Double): Double {
        if (globalDiscountRate <= 0) return 0.0
        return getDiscountableAmount() * (globalDiscountRate / 100.0)
    }

    // Original baseTotal and multiplierAdjustment might need re-evaluation or removal
    // depending on how you want to present them.
    val baseTotal: Double get() = product.basePrice * quantity // Simple base cost
    // MultiplierAdjustment would be the sum of all multiplier effects
    val multiplierAdjustment: Double get() {
        var totalAdjustment = 0.0
        partialMultiplierQuantities.forEach { (multiplierId, qtyForThisMultiplier) ->
            val multiplierDetails = appliedMultipliers.find { it.multiplierId == multiplierId }
            if (multiplierDetails != null && qtyForThisMultiplier > 0) {
                val effectiveMultiplierQty = qtyForThisMultiplier.coerceAtMost(quantity)
                val adjustment = when (multiplierDetails.type) {
                    MultiplierType.PERCENTAGE -> (product.basePrice * (multiplierDetails.appliedValue / 100.0)) * effectiveMultiplierQty
                    MultiplierType.FIXED_PER_UNIT -> multiplierDetails.appliedValue * effectiveMultiplierQty
                }
                totalAdjustment += adjustment
            }
        }
        return totalAdjustment
    }

} // End QuoteItem
@Serializable
data class Quote(
    val id: String = UUID.randomUUID().toString(),
    var customerName: String = "",
    var customerEmail: String = "", // <-- ADD
    var customerPhone: String = "",
    var companyName: String = "",
    var customMessage: String = "",
    val items: List<QuoteItem> = emptyList(),
    val taxRate: Double = 0.0,
    // Store the global discount rate applied to this quote
    val globalDiscountRate: Double = 0.0
) {
    // Subtotal BEFORE global discount
    val subtotalBeforeDiscount: Double
        get() = items.sumOf { it.lineTotalBeforeDiscount }

    // Calculate total discount amount across all items
    val totalDiscountAmount: Double
        get() {
            // Use the stored globalDiscountRate for calculation
            if (globalDiscountRate <= 0) return 0.0
            // Sum the discount calculated for each item
            return items.sumOf { it.calculateDiscountAmount(globalDiscountRate) }
        }

    // Subtotal AFTER global discount
    val subtotalAfterDiscount: Double
        get() = subtotalBeforeDiscount - totalDiscountAmount

    // Tax Amount (calculated on discounted subtotal)
    val taxAmount: Double
        get() = subtotalAfterDiscount * (taxRate / 100.0) // Apply tax AFTER discount

    // Grand Total (Discounted Subtotal + Tax)
    val grandTotal: Double
        get() = subtotalAfterDiscount + taxAmount
}