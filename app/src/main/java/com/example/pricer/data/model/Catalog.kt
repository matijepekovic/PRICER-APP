package com.example.pricer.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

// Represents a complete catalog, containing products and global multipliers.
// Multiple catalogs might be managed by the app eventually.
@Serializable
data class Catalog(
    // Unique identifier for this specific catalog
    val id: String = UUID.randomUUID().toString(),

    // User-defined name for the catalog (e.g., "Standard Materials", "Client X Services")
    var name: String = "Default Catalog", // 'var' to allow renaming

    // Company name associated with this catalog
    var companyName: String = "", // Add company name field

    // The list of all product definitions belonging to this catalog
    val products: List<Product> = emptyList(),

    // The list of all global multiplier definitions belonging to this catalog
    val multipliers: List<Multiplier> = emptyList()

    // Note: We don't store *product-specific* multiplier assignments here.
    // That link is managed dynamically, potentially in the ViewModel or when building a Quote,
    // to avoid excessive data duplication within the core catalog structure.
)