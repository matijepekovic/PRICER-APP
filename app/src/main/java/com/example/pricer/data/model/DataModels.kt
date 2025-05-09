package com.example.pricer.data.model

import kotlinx.serialization.Serializable // Make sure you have the serialization dependency added in build.gradle.kts
import java.util.UUID

// Represents a single product in the catalog
@Serializable
data class Product(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "", // Made vars temporarily if needed for direct editing in some dialog implementations, review later
    var description: String = "",
    var unitType: String = "unit",
    var basePrice: Double = 0.0,
    var category: String = "Default", // For sorting/filtering
    var isDiscountable: Boolean = true
)

// Defines the types of multipliers available
@Serializable
enum class MultiplierType {
    PERCENTAGE, // Applied as a percentage of the base price
    FIXED_PER_UNIT, // Applied as a fixed amount per unit

}
@Serializable
data class Note(
    val id: String = UUID.randomUUID().toString(),
    var content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: NoteType = NoteType.MANUAL
)

@Serializable
enum class NoteType {
    MANUAL,    // User-added note
    CALL,      // Call interaction
    EMAIL,     // Email interaction
    TEXT,      // Text/SMS interaction
    SYSTEM     // Other system-generated notes
}
// Represents a global multiplier definition (e.g., "Discount", "Material Surcharge")
@Serializable
data class Multiplier(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var type: MultiplierType = MultiplierType.PERCENTAGE,
    var value: Double = 0.0, // The percentage value (e.g., 10.0 for 10%) or the fixed amount
    var isDiscountable: Boolean = true
)

// Represents a multiplier *as it is applied* to a specific quote item.
// This allows for potential overrides of the global multiplier value for that specific item.
@Serializable
data class AppliedMultiplier(
    val multiplierId: String,        // Links back to the global Multiplier definition
    val name: String,                // Copied name for display convenience in the quote
    val type: MultiplierType,        // Copied type for calculation convenience
    val appliedValue: Double,         // The actual value used (global value OR an overridden value)
    var isDiscountable: Boolean = true
)

// Enum to define the criteria for sorting products in the catalog view
enum class ProductSortCriteria {
    NAME_ASC, NAME_DESC,
    PRICE_ASC, PRICE_DESC,
    CATEGORY_ASC, CATEGORY_DESC
}

// Enum to represent the different main UI states (screens)
enum class UiMode {
    CATALOG,
    QUOTE_PREVIEW,
    CONTACTS,
    PROSPECTS,
    PROSPECT_DETAIL,
    SUBCONTRACTORS
}

// Enum to manage which dialog is currently open (if any)
enum class DialogState {
    NONE, // No dialog is open
    ADD_EDIT_PRODUCT,
    MANAGE_MULTIPLIERS,
    ASSIGN_MULTIPLIER,
    SET_TAX,
    MANAGE_CATALOGS,
    CUSTOMER_DETAILS,
    QUOTE_DETAILS,
    SET_DISCOUNT,
    ADD_NOTE,
    EDIT_NOTE,
    SET_REMINDER,
    ADD_CUSTOM_ITEM,
    ADD_VOUCHER,
    MANAGE_PHASES,       // Add this new state
    ASSIGN_SUBCONTRACTOR
}
@Serializable
enum class ProspectStatus { PROSPECT, CUSTOMER }

@Serializable
data class ProspectRecord(
    val id: String = UUID.randomUUID().toString(),
    val customerName: String,
    val customerEmail: String?,
    val customerPhone: String?,
    val quoteSnapshot: Quote,
    val externalPdfUriString: String?,
    var status: ProspectStatus = ProspectStatus.PROSPECT,
    var notes: List<Note> = emptyList(),
    var reminderDateTime: Long? = null,
    var reminderNote: String? = null,
    // Add these properties for multiple images
    var beforeImageUris: List<Pair<String, Long>> = emptyList(),
    var afterImageUris: List<Pair<String, Long>> = emptyList(),
    // Keep old properties for backward compatibility
    var beforeImageUriString: String? = null,
    var afterImageTimestamp: Long? = null,
    var beforeImageTimestamp: Long? = null,
    var afterImageUriString: String? = null,
    // New fields for phases, tasks, documents, subcontractors
    var phases: List<Phase> = emptyList(),
    var phaseImages: List<PhaseImage> = emptyList(),
    var tasks: List<Task> = emptyList(),
    var documents: List<Document> = emptyList(),
    var subcontractorAssignments: List<SubcontractorAssignment> = emptyList(),
    val dateCreated: Long = System.currentTimeMillis(),
    var dateUpdated: Long = System.currentTimeMillis()

)
@Serializable
data class Subcontractor(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val specialty: String = "",
    val contactName: String = "",
    val phone: String = "",
    val email: String = "",
    val notes: String = ""
)

@Serializable
data class Phase(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val order: Int,
    val description: String = "",
    var status: PhaseStatus = PhaseStatus.NOT_STARTED
)

@Serializable
enum class PhaseStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

@Serializable
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    var isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class PhaseImage(
    val phaseId: String,
    val imageUris: List<Pair<String, Long>> = emptyList() // URI and timestamp
)

@Serializable
data class Document(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val uriString: String,
    val fileType: String = "",
    val uploadTimestamp: Long = System.currentTimeMillis()
)

@Serializable
data class SubcontractorAssignment(
    val subcontractorId: String,
    val phaseId: String? = null, // null means assigned to all phases
    val assignedAt: Long = System.currentTimeMillis()
)