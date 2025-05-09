package com.example.pricer.viewmodel

// --- Android/Framework Imports ---
import android.app.Application
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.compositionLocalOf
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

// --- Coroutines & Flow Imports ---
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- Serialization Imports ---
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// --- Project-Specific Imports ---
import com.example.pricer.data.model.*
import com.example.pricer.ui.dialogs.CustomerInfo
import com.example.pricer.util.PdfGenerator
import com.example.pricer.util.ReminderManager

// --- Java Imports ---
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Main ViewModel for the Pricer application.
 * Handles data and business logic for catalogs, products, quotes, and prospects.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // =============================================
    // Constants & Basic Configuration
    // =============================================
    // Add these functions to your MainViewModel.kt class


    private val LocalProspectDetailViewModel = compositionLocalOf<MainViewModel> {
        error("No MainViewModel provided")
    }
    private companion object {
        const val TAG = "MainViewModel"
        const val DEFAULT_CATALOG_ID = "default_catalog_001"
        const val CATALOG_FILE_NAME = "pricer_catalogs.json"
        private const val PROSPECTS_FILE_NAME = "prospects_data.json"
    }

    private val appContext: Context
        get() = getApplication<Application>().applicationContext

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    // =============================================
// Project Image Management
// =============================================
// Add these to your MainViewModel class

    // Image upload event handler
    private val _imageUploadEvent = MutableSharedFlow<Pair<String, Boolean>>()
    val imageUploadEvent: SharedFlow<Pair<String, Boolean>> = _imageUploadEvent.asSharedFlow()

    // Request functions
    fun requestBeforeImageUpload(prospectId: String) {
        viewModelScope.launch {
            _imageUploadEvent.emit(Pair(prospectId, true))
        }
    }

    fun requestAfterImageUpload(prospectId: String) {
        viewModelScope.launch {
            _imageUploadEvent.emit(Pair(prospectId, false))
        }
    }

    // Save image
    fun saveImageToProspect(prospectId: String, imageUri: Uri, isBeforeImage: Boolean) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            var updatedRecord: ProspectRecord? = null

            _prospectRecords.update { currentRecords ->
                currentRecords.map { record ->
                    if (record.id == prospectId) {
                        val result = if (isBeforeImage) {
                            // Add to before images
                            val newPair = Pair(imageUri.toString(), timestamp)
                            val updatedUris = record.beforeImageUris + newPair

                            record.copy(
                                beforeImageUris = updatedUris,
                                // Also update legacy field if it was the first image
                                beforeImageUriString = record.beforeImageUriString ?: imageUri.toString(),
                                beforeImageTimestamp = record.beforeImageTimestamp ?: timestamp,
                                dateUpdated = timestamp
                            )
                        } else {
                            // Add to after images
                            val newPair = Pair(imageUri.toString(), timestamp)
                            val updatedUris = record.afterImageUris + newPair

                            record.copy(
                                afterImageUris = updatedUris,
                                // Also update legacy field if it was the first image
                                afterImageUriString = record.afterImageUriString ?: imageUri.toString(),
                                afterImageTimestamp = record.afterImageTimestamp ?: timestamp,
                                dateUpdated = timestamp
                            )
                        }

                        updatedRecord = result
                        result
                    } else {
                        record
                    }
                }
            }

            // Update selected prospect record if needed
            updatedRecord?.let { updateSelectedProspect(it) }

            saveProspectRecords()
            _snackbarMessage.emit("Image added successfully")
        }
    }

    fun removeBeforeImage(prospectId: String, imageUri: String) {
        viewModelScope.launch {
            var updatedRecord: ProspectRecord? = null

            _prospectRecords.update { currentRecords ->
                currentRecords.map { record ->
                    if (record.id == prospectId) {
                        val updatedUris = record.beforeImageUris.filterNot { it.first == imageUri }

                        // Update legacy fields if needed
                        val newLegacyUri = if (record.beforeImageUriString == imageUri) {
                            updatedUris.firstOrNull()?.first
                        } else {
                            record.beforeImageUriString
                        }

                        val newLegacyTimestamp = if (record.beforeImageUriString == imageUri) {
                            updatedUris.firstOrNull()?.second
                        } else {
                            record.beforeImageTimestamp
                        }

                        val result = record.copy(
                            beforeImageUris = updatedUris,
                            beforeImageUriString = newLegacyUri,
                            beforeImageTimestamp = newLegacyTimestamp,
                            dateUpdated = System.currentTimeMillis()
                        )

                        updatedRecord = result
                        result
                    } else {
                        record
                    }
                }
            }

            // Update selected prospect if needed
            updatedRecord?.let { updateSelectedProspect(it) }

            saveProspectRecords()
            _snackbarMessage.emit("Before image removed")
        }
    }

    fun removeAfterImage(prospectId: String, imageUri: String) {
        viewModelScope.launch {
            var updatedRecord: ProspectRecord? = null

            _prospectRecords.update { currentRecords ->
                currentRecords.map { record ->
                    if (record.id == prospectId) {
                        val updatedUris = record.afterImageUris.filterNot { it.first == imageUri }

                        // Update legacy fields if needed
                        val newLegacyUri = if (record.afterImageUriString == imageUri) {
                            updatedUris.firstOrNull()?.first
                        } else {
                            record.afterImageUriString
                        }

                        val newLegacyTimestamp = if (record.afterImageUriString == imageUri) {
                            updatedUris.firstOrNull()?.second
                        } else {
                            record.afterImageTimestamp
                        }

                        val result = record.copy(
                            afterImageUris = updatedUris,
                            afterImageUriString = newLegacyUri,
                            afterImageTimestamp = newLegacyTimestamp,
                            dateUpdated = System.currentTimeMillis()
                        )

                        updatedRecord = result
                        result
                    } else {
                        record
                    }
                }
            }

            // Update selected prospect if needed
            updatedRecord?.let { updateSelectedProspect(it) }

            saveProspectRecords()
            _snackbarMessage.emit("After image removed")
        }
    }

    fun updateCatalogCompanyName(catalogId: String, newCompanyName: String) {
        var changed = false
        _catalogs.update { map ->
            map[catalogId]?.takeIf { it.companyName != newCompanyName }?.let {
                changed = true
                map.toMutableMap().apply { this[catalogId] = it.copy(companyName = newCompanyName) }
            } ?: map
        }
        if(changed) saveCatalogs()
    }

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Array<String>>
    private var currentImageUploadInfo: Pair<String, Boolean>? = null // Pair<ProspectId, IsBeforeImage>


    // =============================================
    // Core UI State
    // =============================================

    private val _uiMode = MutableStateFlow(UiMode.CATALOG)
    val uiMode: StateFlow<UiMode> = _uiMode.asStateFlow()

    private val _currentDialog = MutableStateFlow(DialogState.NONE)
    val currentDialog: StateFlow<DialogState> = _currentDialog.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // =============================================
    // Catalog & Product State
    // =============================================
    private val _subcontractors = MutableStateFlow<List<Subcontractor>>(emptyList())
    val subcontractors: StateFlow<List<Subcontractor>> = _subcontractors.asStateFlow()

    private val _globalPhases = MutableStateFlow<List<Phase>>(emptyList())
    val globalPhases: StateFlow<List<Phase>> = _globalPhases.asStateFlow()

    private val _selectedPhaseIndex = MutableStateFlow(0)
    val selectedPhaseIndex: StateFlow<Int> = _selectedPhaseIndex.asStateFlow()

    private val _catalogs = MutableStateFlow<Map<String, Catalog>>(emptyMap())
    val catalogs: StateFlow<Map<String, Catalog>> = _catalogs.asStateFlow()

    private val _activeCatalogId = MutableStateFlow(DEFAULT_CATALOG_ID)
    val activeCatalog: StateFlow<Catalog?> = combine(_catalogs, _activeCatalogId) { catalogsMap, activeId ->
        catalogsMap[activeId]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _productSortCriteria = MutableStateFlow(ProductSortCriteria.NAME_ASC)
    val productSortCriteria: StateFlow<ProductSortCriteria> = _productSortCriteria.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _shareCatalogEvent = MutableSharedFlow<Uri>()
    val shareCatalogEvent: SharedFlow<Uri> = _shareCatalogEvent.asSharedFlow()

    // Product-specific state
    private val _productToEdit = MutableStateFlow<Product?>(null)
    val productToEdit: StateFlow<Product?> = _productToEdit.asStateFlow()

    private val _itemQuantities = MutableStateFlow<Map<String, String>>(emptyMap())
    val itemQuantities: StateFlow<Map<String, String>> = _itemQuantities.asStateFlow()

    // =============================================
    // Multiplier State
    // =============================================

    private val _multiplierToEdit = MutableStateFlow<Multiplier?>(null)
    val multiplierToEdit: StateFlow<Multiplier?> = _multiplierToEdit.asStateFlow()

    private val _productMultiplierAssignments = MutableStateFlow<Map<String, Map<String, Int>>>(emptyMap())
    val productMultiplierAssignments: StateFlow<Map<String, Map<String, Int>>> = _productMultiplierAssignments.asStateFlow()

    private val _selectedMultipliersForAssignment = MutableStateFlow<Map<String, String>>(emptyMap())
    val selectedMultipliersForAssignment: StateFlow<Map<String, String>> = _selectedMultipliersForAssignment.asStateFlow()

    private val _productForMultiplierAssignment = MutableStateFlow<Product?>(null)
    val productForMultiplierAssignment: StateFlow<Product?> = _productForMultiplierAssignment.asStateFlow()

    // =============================================
    // Quote State
    // =============================================

    private val _currentQuote = MutableStateFlow<Quote?>(null)
    val currentQuote: StateFlow<Quote?> = _currentQuote.asStateFlow()

    private val _taxRate = MutableStateFlow(0.0)
    val taxRate: StateFlow<Double> = _taxRate.asStateFlow()

    private val _globalDiscountRate = MutableStateFlow(0.0)
    val globalDiscountRate: StateFlow<Double> = _globalDiscountRate.asStateFlow()

    private val _quoteCompanyName = MutableStateFlow("")
    val quoteCompanyName: StateFlow<String> = _quoteCompanyName.asStateFlow()

    private val _quoteCustomMessage = MutableStateFlow("")
    val quoteCustomMessage: StateFlow<String> = _quoteCustomMessage.asStateFlow()

    private val _quoteCustomerName = MutableStateFlow("")

    private val _quoteCustomerEmail = MutableStateFlow("")
    val quoteCustomerEmail: StateFlow<String> = _quoteCustomerEmail.asStateFlow()

    private val _quoteCustomerPhone = MutableStateFlow("")
    val quoteCustomerPhone: StateFlow<String> = _quoteCustomerPhone.asStateFlow()

    // =============================================
    // Prospect/Customer State
    // =============================================
    private val _prospectsSearchQuery = MutableStateFlow("")
    val prospectsSearchQuery: StateFlow<String> = _prospectsSearchQuery.asStateFlow()
    private val _prospectRecords = MutableStateFlow<List<ProspectRecord>>(emptyList())
    val prospectRecords: StateFlow<List<ProspectRecord>> = _prospectRecords.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingMessage = MutableStateFlow<String?>(null)
    val loadingMessage: StateFlow<String?> = _loadingMessage.asStateFlow()
    private val _selectedProspectRecord = MutableStateFlow<ProspectRecord?>(null)
    val selectedProspectRecord: StateFlow<ProspectRecord?> = _selectedProspectRecord.asStateFlow()
    private val _selectedProspectId = MutableStateFlow<String?>(null)
    private val _taskPhaseId = MutableStateFlow<String?>(null)
    val taskPhaseId: StateFlow<String?> = _taskPhaseId.asStateFlow()
    fun showEditTaskDialog(taskId: String) {
        val selectedProspect = _selectedProspectRecord.value
        val task = selectedProspect?.tasks?.find { it.id == taskId }

        if (task != null) {
            _taskToEdit.value = task
            _currentDialog.value = DialogState.EDIT_TASK
            Log.d(TAG, "Showing EDIT_TASK dialog for task: ${task.title}")
        } else {
            Log.e(TAG, "Cannot edit task: Task with ID $taskId not found")
        }
    }

    fun deleteTask(prospectId: String, taskId: String) {
        Log.d(TAG, "Deleting task $taskId from prospect $prospectId")

        _prospectRecords.update { records ->
            records.map { record ->
                if (record.id == prospectId) {
                    val updatedTasks = record.tasks.filter { it.id != taskId }
                    record.copy(
                        tasks = updatedTasks,
                        dateUpdated = System.currentTimeMillis()
                    )
                } else {
                    record
                }
            }
        }

        // Update selected prospect if needed
        updateSelectedProspect(_prospectRecords.value.find { it.id == prospectId })

        // Save changes
        saveProspectRecords()

        // Show confirmation
        viewModelScope.launch {
            _snackbarMessage.emit("Task deleted")
        }
    }

    fun updateTask(prospectId: String, updatedTask: Task) {
        Log.d(TAG, "Updating task ${updatedTask.id} in prospect $prospectId")

        _prospectRecords.update { records ->
            records.map { record ->
                if (record.id == prospectId) {
                    val updatedTasks = record.tasks.map { task ->
                        if (task.id == updatedTask.id) updatedTask else task
                    }
                    record.copy(
                        tasks = updatedTasks,
                        dateUpdated = System.currentTimeMillis()
                    )
                } else {
                    record
                }
            }
        }

        // Update selected prospect if needed
        updateSelectedProspect(_prospectRecords.value.find { it.id == prospectId })

        // Save changes
        saveProspectRecords()

        // Show confirmation
        viewModelScope.launch {
            _snackbarMessage.emit("Task updated")
        }
    }

    // =============================================
    // Notes State
    // =============================================
    private val _phaseToEdit = MutableStateFlow<Phase?>(null)
    val phaseToEdit: StateFlow<Phase?> = _phaseToEdit.asStateFlow()
    private val _importConfirmEvent = MutableSharedFlow<Catalog>()
    val importConfirmEvent: SharedFlow<Catalog> = _importConfirmEvent.asSharedFlow()
    private val _taskToEdit = MutableStateFlow<Task?>(null)
    val taskToEdit: StateFlow<Task?> = _taskToEdit.asStateFlow()
    private val _nameConflictEvent = MutableSharedFlow<Pair<Catalog, String>>()
    val nameConflictEvent: SharedFlow<Pair<Catalog, String>> = _nameConflictEvent.asSharedFlow()
    private val _noteToEdit = MutableStateFlow<Note?>(null)
    val noteToEdit: StateFlow<Note?> = _noteToEdit.asStateFlow()
    fun addPhase(phase: Phase) {
        _globalPhases.update { currentPhases ->
            val maxOrder = if (currentPhases.isEmpty()) -1 else currentPhases.maxOf { it.order }
            val phaseWithOrder = phase.copy(order = maxOrder + 1)
            currentPhases + phaseWithOrder
        }
        savePhases()
    }

    fun updatePhase(phase: Phase) {
        _globalPhases.update { currentPhases ->
            currentPhases.map {
                if (it.id == phase.id) phase else it
            }
        }
        savePhases()
    }

    fun deletePhase(phaseId: String) {
        _globalPhases.update { currentPhases ->
            currentPhases.filter { it.id != phaseId }
        }

        // Also update the order of remaining phases
        _globalPhases.update { currentPhases ->
            currentPhases.mapIndexed { index, phase ->
                phase.copy(order = index)
            }
        }

        savePhases()
    }

    fun showManagePhasesDialog() {
        _currentDialog.value = DialogState.MANAGE_PHASES
        Log.d(TAG, "Showing MANAGE_PHASES dialog")
    }

    fun showAddPhaseDialog() {
        _phaseToEdit.value = null
        _currentDialog.value = DialogState.ADD_PHASE
        Log.d(TAG, "Showing ADD_PHASE dialog")
    }

    fun showEditPhaseDialog(phase: Phase) {
        _phaseToEdit.value = phase
        _currentDialog.value = DialogState.EDIT_PHASE
        Log.d(TAG, "Showing EDIT_PHASE dialog with phase: ${phase.name}")
    }

    fun showAddTaskDialog(phaseId: String) {
        Log.d(TAG, "Setting task phase ID to: $phaseId")
        _taskPhaseId.value = phaseId
        _currentDialog.value = DialogState.ADD_TASK

    }
    // =============================================
    // Derived State
    // =============================================
    val filteredProspectRecords: StateFlow<List<ProspectRecord>> = combine(
        _prospectRecords,
        prospectsSearchQuery
    ) { records, query ->
        if (query.isBlank()) {
            records
        } else {
            val searchTerms = query.lowercase().trim().split(" ")
            records.filter { record ->
                searchTerms.all { term ->
                    record.customerName.lowercase().contains(term) ||
                            record.customerEmail?.lowercase()?.contains(term) == true ||
                            record.customerPhone?.contains(term) == true
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    // Add this near your other SharedFlow declarations

    // Then add this method to trigger the event
    // Add this function to update the search query
    fun updateProspectsSearchQuery(query: String) {
        _prospectsSearchQuery.value = query
    }
    val sortedProducts: StateFlow<List<Product>> = combine(
        activeCatalog,
        _productSortCriteria,
        _searchQuery
    ) { catalog, sortCriteria, query ->
        val baseProducts = catalog?.products ?: emptyList()
        val filteredProducts = if (query.isBlank()) {
            baseProducts
        } else {
            baseProducts.filter { product ->
                product.name.contains(query, ignoreCase = true)
            }
        }
        sortProducts(filteredProducts, sortCriteria)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categories: StateFlow<List<String>> = activeCatalog.map { catalog ->
        (catalog?.products?.map { it.category }?.distinct()?.sorted() ?: emptyList()).let { cats ->
            when {
                cats.isEmpty() -> listOf("Default")
                cats.contains("Default") -> listOf("Default") + cats.filter { it != "Default" }
                else -> cats
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Default"))

    val groupedSortedFilteredProducts: StateFlow<Map<String, List<Product>>> = combine(
        activeCatalog,
        _productSortCriteria,
        searchQuery
    ) { catalog, sortCriteria, query ->
        val baseProducts = catalog?.products ?: emptyList()
        val filteredProducts = if (query.isBlank()) baseProducts else {
            baseProducts.filter { product ->
                product.name.contains(query, ignoreCase = true) ||
                        product.category.contains(query, ignoreCase = true)
            }
        }
        filteredProducts
            .groupBy { it.category }
            .mapValues { (_, productList) ->
                sortProducts(productList, sortCriteria)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // =============================================
    // Initialization
    // =============================================

    init {
        Log.i(TAG, "ViewModel Initialized.")
        loadCatalogs()
        loadProspectRecords()
    }

    // =============================================
    // UI Mode Navigation
    // =============================================
    fun addSubcontractor(subcontractor: Subcontractor) {
        _subcontractors.update { current ->
            current + subcontractor
        }
        saveSubcontractors()
    }

    fun updateSubcontractor(subcontractor: Subcontractor) {
        _subcontractors.update { current ->
            current.map {
                if (it.id == subcontractor.id) subcontractor else it
            }
        }
        saveSubcontractors()
    }

    fun deleteSubcontractor(subcontractorId: String) {
        _subcontractors.update { current ->
            current.filter { it.id != subcontractorId }
        }
        saveSubcontractors()
    }

    // Add methods to manage global phases
    fun saveGlobalPhases(phases: List<Phase>) {
        _globalPhases.update { phases }
        savePhases()

        // Update any prospect records to include these phases
        _prospectRecords.update { records ->
            records.map { record ->
                if (record.status == ProspectStatus.CUSTOMER) {
                    // For customers, ensure they have all phases
                    val existingPhaseIds = record.phases.map { it.id }
                    val missingPhases = phases.filter { it.id !in existingPhaseIds }

                    if (missingPhases.isNotEmpty()) {
                        record.copy(phases = record.phases + missingPhases)
                    } else {
                        record
                    }
                } else {
                    record
                }
            }
        }

        saveProspectRecords()
    }

    fun setSelectedPhaseIndex(index: Int) {
        _selectedPhaseIndex.value = index.coerceIn(0, _globalPhases.value.size)
    }

    // Add methods for tasks
    fun addTask(prospectId: String, task: Task) {
        Log.d(TAG, "Adding task '${task.title}' to prospect $prospectId with phaseId ${task.phaseId}")

        // Make a copy to ensure we're not modifying the original
        val newTask = task.copy(id = UUID.randomUUID().toString())
        _prospectRecords.update { records ->
            records.map { record ->
                if (record.id == prospectId) {
                    val updatedTasks = record.tasks + newTask
                    record.copy(
                        tasks = updatedTasks,
                        dateUpdated = System.currentTimeMillis()
                    )
                } else {
                    record
                }
            }
        }

        _selectedProspectRecord.update { currentRecord ->
            if (currentRecord?.id == prospectId) {
                val updatedTasks = currentRecord.tasks + newTask
                currentRecord.copy(
                    tasks = updatedTasks,
                    dateUpdated = System.currentTimeMillis()
                )
            } else {
                currentRecord
            }
        }

        // Save changes to storage
        saveProspectRecords()

        // Clear task phase ID after adding
        _taskPhaseId.value = null

        // Emit a snackbar message to confirm the task was added
        viewModelScope.launch {
            _snackbarMessage.emit("Task '${task.title}' added")
        }
    }

    fun toggleTaskCompletion(prospectId: String, taskId: String) {
        _prospectRecords.update { records ->
            records.map { record ->
                if (record.id == prospectId) {
                    val updatedTasks = record.tasks.map {
                        if (it.id == taskId) it.copy(isCompleted = !it.isCompleted) else it
                    }
                    record.copy(
                        tasks = updatedTasks,
                        dateUpdated = System.currentTimeMillis()
                    )
                } else {
                    record
                }
            }
        }

        // Also update the selected prospect if it's the one being modified
        _selectedProspectRecord.update { currentRecord ->
            if (currentRecord?.id == prospectId) {
                val updatedTasks = currentRecord.tasks.map {
                    if (it.id == taskId) it.copy(isCompleted = !it.isCompleted) else it
                }
                currentRecord.copy(
                    tasks = updatedTasks,
                    dateUpdated = System.currentTimeMillis()
                )
            } else {
                currentRecord
            }
        }

        saveProspectRecords()
    }

    // Methods for documents
    fun addDocument(prospectId: String, document: Document) {
        _prospectRecords.update { records ->
            records.map { record ->
                if (record.id == prospectId) {
                    val updatedDocs = record.documents + document
                    record.copy(
                        documents = updatedDocs,
                        dateUpdated = System.currentTimeMillis()
                    )
                } else {
                    record
                }
            }
        }
        saveProspectRecords()
    }

    fun deleteDocument(prospectId: String, documentId: String) {
        _prospectRecords.update { records ->
            records.map { record ->
                if (record.id == prospectId) {
                    val updatedDocs = record.documents.filter { it.id != documentId }
                    record.copy(
                        documents = updatedDocs,
                        dateUpdated = System.currentTimeMillis()
                    )
                } else {
                    record
                }
            }
        }
        saveProspectRecords()
    }

    // Methods for phase images
    fun addPhaseImage(prospectId: String, phaseId: String, imageUri: String) {
        _prospectRecords.update { records ->
            records.map { record ->
                if (record.id == prospectId) {
                    // Find existing phase image entry or create new one
                    val timestamp = System.currentTimeMillis()
                    val existingEntry = record.phaseImages.find { it.phaseId == phaseId }

                    val updatedPhaseImages = if (existingEntry != null) {
                        // Update existing entry
                        record.phaseImages.map {
                            if (it.phaseId == phaseId) {
                                it.copy(imageUris = it.imageUris + Pair(imageUri, timestamp))
                            } else {
                                it
                            }
                        }
                    } else {
                        // Create new entry
                        record.phaseImages + PhaseImage(
                            phaseId = phaseId,
                            imageUris = listOf(Pair(imageUri, timestamp))
                        )
                    }

                    record.copy(
                        phaseImages = updatedPhaseImages,
                        dateUpdated = timestamp
                    )
                } else {
                    record
                }
            }
        }
        saveProspectRecords()
    }


    // Methods for subcontractor assignments
    fun updateSubcontractorAssignments(prospectId: String, assignments: List<SubcontractorAssignment>) {
        _prospectRecords.update { records ->
            records.map { record ->
                if (record.id == prospectId) {
                    record.copy(
                        subcontractorAssignments = assignments,
                        dateUpdated = System.currentTimeMillis()
                    )
                } else {
                    record
                }
            }
        }
        saveProspectRecords()
    }

    // Add to the initialization section
    init {
        loadSubcontractors()
        loadPhases()
    }

    // Add methods for persistence
    private fun saveSubcontractors() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = json.encodeToString(_subcontractors.value)
                val file = File(appContext.filesDir, "subcontractors.json")
                file.writeText(jsonString)
                Log.i(TAG, "Subcontractors saved successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving subcontractors", e)
            }
        }
    }
    // Function to navigate to subcontractor details
    fun showSubcontractorDetails(subcontractorId: String) {
        // This could navigate to a subcontractor detail screen
        // For now, it just shows a snackbar with the info
        viewModelScope.launch {
            val subcontractor = _subcontractors.value.find { it.id == subcontractorId }
            if (subcontractor != null) {
                _snackbarMessage.emit("Subcontractor: ${subcontractor.name}, Phone: ${subcontractor.phone}")
            }
        }
    }

    // Function to remove a subcontractor assignment
    fun removeSubcontractorAssignment(prospectId: String, assignmentToRemove: SubcontractorAssignment) {
        _prospectRecords.update { records ->
            records.map { record ->
                if (record.id == prospectId) {
                    val updatedAssignments = record.subcontractorAssignments.filter {
                        it.subcontractorId != assignmentToRemove.subcontractorId ||
                                it.phaseId != assignmentToRemove.phaseId
                    }
                    record.copy(
                        subcontractorAssignments = updatedAssignments,
                        dateUpdated = System.currentTimeMillis()
                    )
                } else {
                    record
                }
            }
        }
        saveProspectRecords()

        viewModelScope.launch {
            _snackbarMessage.emit("Subcontractor assignment removed")
        }
    }
    private fun loadSubcontractors() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(appContext.filesDir, "subcontractors.json")
            if (file.exists()) {
                try {
                    val jsonString = file.readText()
                    val subcontractors: List<Subcontractor> = json.decodeFromString(jsonString)
                    withContext(Dispatchers.Main) {
                        _subcontractors.value = subcontractors
                        Log.i(TAG, "Loaded ${subcontractors.size} subcontractors")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading subcontractors", e)
                    withContext(Dispatchers.Main) {
                        _subcontractors.value = emptyList()
                    }
                }
            } else {
                Log.i(TAG, "No subcontractors file found. Starting with empty list.")
                withContext(Dispatchers.Main) {
                    _subcontractors.value = emptyList()
                }
            }
        }
    }

    private fun savePhases() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = json.encodeToString(_globalPhases.value)
                val file = File(appContext.filesDir, "phases.json")
                file.writeText(jsonString)
                Log.i(TAG, "Phases saved successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving phases", e)
            }
        }
    }

    private fun loadPhases() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(appContext.filesDir, "phases.json")
            if (file.exists()) {
                try {
                    val jsonString = file.readText()
                    val phases: List<Phase> = json.decodeFromString(jsonString)
                    withContext(Dispatchers.Main) {
                        _globalPhases.value = phases
                        Log.i(TAG, "Loaded ${phases.size} phases")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading phases", e)
                    withContext(Dispatchers.Main) {
                        _globalPhases.value = emptyList()
                    }
                }
            } else {
                // Initialize with default phases
                val defaultPhases = listOf(
                    Phase(
                        id = UUID.randomUUID().toString(),
                        name = "Planning",
                        order = 0,
                        description = "Initial planning phase"
                    ),
                    Phase(
                        id = UUID.randomUUID().toString(),
                        name = "In Progress",
                        order = 1,
                        description = "Work is currently in progress"
                    ),
                    Phase(
                        id = UUID.randomUUID().toString(),
                        name = "Review",
                        order = 2,
                        description = "Work is complete and ready for review"
                    ),
                    Phase(
                        id = UUID.randomUUID().toString(),
                        name = "Complete",
                        order = 3,
                        description = "Project completed"
                    )
                )

                Log.i(TAG, "No phases file found. Initializing with default phases.")
                withContext(Dispatchers.Main) {
                    _globalPhases.value = defaultPhases
                }
                savePhases()
            }
        }
    }
    fun updatePhaseStatus(prospectId: String, phaseId: String, newStatus: PhaseStatus) {
        _prospectRecords.update { records ->
            records.map { record ->
                if (record.id == prospectId) {
                    val updatedPhases = record.phases.map { phase ->
                        if (phase.id == phaseId) phase.copy(status = newStatus) else phase
                    }
                    record.copy(
                        phases = updatedPhases,
                        dateUpdated = System.currentTimeMillis()
                    )
                } else {
                    record
                }
            }
        }
        saveProspectRecords()
    }


    // Add a method to show the Subcontractors screen
    fun showSubcontractorsScreen() {
        _uiMode.value = UiMode.SUBCONTRACTORS
    }
    fun showContactsScreen() {
        _selectedProspectRecord.value = null // Clear selection when viewing list
        _uiMode.value = UiMode.CONTACTS
        Log.d(TAG, "Switching to Contacts screen (Prospects/Customers tabs)")
    }
    fun catalogNameExists(name: String): Boolean {
        return _catalogs.value.values.any { it.name.equals(name, ignoreCase = true) }
    }

    fun importCatalogWithNewName(catalog: Catalog, newName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Importing catalog with new name..."

            try {
                val trimmedName = newName.trim()

                if (trimmedName.isBlank()) {
                    _snackbarMessage.emit("Import canceled: Name cannot be empty")
                    return@launch
                }

                // Check if new name also exists
                if (catalogNameExists(trimmedName)) {
                    _snackbarMessage.emit("A catalog with the name '$trimmedName' already exists. Please try another name.")
                    return@launch
                }

                // Continue with import using new name
                val catalogToAdd = catalog.copy(
                    id = UUID.randomUUID().toString(),
                    name = trimmedName
                )

                _catalogs.update { currentCatalogs ->
                    currentCatalogs + (catalogToAdd.id to catalogToAdd)
                }

                saveCatalogs()
                _snackbarMessage.emit("Catalog '$trimmedName' imported successfully!")
            } finally {
                _isLoading.value = false
                _loadingMessage.value = null
            }
        }
    }
// Then modify the existing showProspectsScreen function:

    fun showProspectsScreen() {
        _selectedProspectRecord.value = null // Clear selection when viewing list
        _uiMode.value = UiMode.CONTACTS // Changed from UiMode.PROSPECTS to UiMode.CONTACTS
        Log.d(TAG, "Switching to Contacts screen (Prospects/Customers tabs)")
    }
    fun showCatalogView() {
        _uiMode.value = UiMode.CATALOG
    }

    fun showQuotePreview() {
        buildQuote()
        if (_currentQuote.value?.items?.isNotEmpty() == true) {
            if(_currentQuote.value?.customerName.isNullOrBlank()) {
                _quoteCustomerName.value = ""
                showDialog(DialogState.CUSTOMER_DETAILS)
            } else {
                _uiMode.value = UiMode.QUOTE_PREVIEW
            }
        } else {
            Log.w(TAG, "Cannot show Quote Preview: No valid items.")
        }
    }


    fun showProspectDetail(prospectId: String) {
        val record = _prospectRecords.value.find { it.id == prospectId }
        if (record != null) {
            _selectedProspectRecord.value = record
            _uiMode.value = UiMode.PROSPECT_DETAIL
            Log.d(TAG, "Showing detail for prospect: ${record.customerName}")
        } else {
            Log.e(TAG, "Prospect record with ID $prospectId not found.")
            viewModelScope.launch { _snackbarMessage.emit("Error: Could not find prospect details.") }
        }
    }

    fun proceedToQuotePreview(customerInfo: CustomerInfo) {
        _quoteCustomerName.value = customerInfo.name
        _quoteCustomerEmail.value = customerInfo.email
        _quoteCustomerPhone.value = customerInfo.phone
        _currentQuote.update {
            it?.copy(
                customerName = customerInfo.name,
                customerEmail = customerInfo.email,
                customerPhone = customerInfo.phone
            )
        }
        _uiMode.value = UiMode.QUOTE_PREVIEW
        dismissDialog()
    }

    // =============================================
    // Dialog Management
    // =============================================

    fun showDialog(dialog: DialogState, data: Any? = null) {
        Log.d(TAG, "Showing Dialog: $dialog")
        when (dialog) {
            DialogState.ADD_EDIT_PRODUCT -> _productToEdit.value = data as? Product
            DialogState.MANAGE_MULTIPLIERS -> _multiplierToEdit.value = data as? Multiplier
            DialogState.ASSIGN_MULTIPLIER -> {
                val product = data as? Product
                _productForMultiplierAssignment.value = product
                val currentIntAssignments = _productMultiplierAssignments.value[product?.id] ?: emptyMap()
                _selectedMultipliersForAssignment.value = currentIntAssignments.mapValues { (_, qty) -> qty.toString() }
                Log.d(TAG, "Preparing AssignMultiplierDialog for ${product?.name}. Initial assignments (as String): ${_selectedMultipliersForAssignment.value}")
            }
            DialogState.QUOTE_DETAILS -> {
                _quoteCompanyName.value = _currentQuote.value?.companyName ?: ""
                _quoteCustomMessage.value = _currentQuote.value?.customMessage ?: ""
            }
            DialogState.CUSTOMER_DETAILS -> {
                _quoteCustomerName.value = _currentQuote.value?.customerName ?: ""
                _quoteCustomerEmail.value = _currentQuote.value?.customerEmail ?: ""
                _quoteCustomerPhone.value = _currentQuote.value?.customerPhone ?: ""
            }
            DialogState.EDIT_NOTE -> {
                _noteToEdit.value = data as? Note
            }
            else -> { /* No prep needed */ }
        }
        _currentDialog.value = dialog
    }

    fun dismissDialog() {
        Log.d(TAG, "Dismissing Dialog: ${_currentDialog.value}")
        _productToEdit.value = null
        _multiplierToEdit.value = null
        _productForMultiplierAssignment.value = null
        _selectedMultipliersForAssignment.value = emptyMap()
        _quoteCustomerName.value = ""
        _noteToEdit.value = null
        _currentDialog.value = DialogState.NONE
    }

    // =============================================
    // Product Management
    // =============================================

    fun addOrUpdateProduct(product: Product) {
        var changed = false
        _catalogs.update { currentCatalogs ->
            val activeId = _activeCatalogId.value
            val currentActiveCatalog = currentCatalogs[activeId] ?: return@update currentCatalogs
            val index = currentActiveCatalog.products.indexOfFirst { it.id == product.id }
            val updatedProducts = if (index != -1) {
                changed = true
                currentActiveCatalog.products.toMutableList().apply { this[index] = product }
            } else {
                changed = true
                currentActiveCatalog.products + product
            }
            currentCatalogs.toMutableMap().apply {
                this[activeId] = currentActiveCatalog.copy(products = updatedProducts.toList())
            }
        }
        if(changed) saveCatalogs()
        dismissDialog()
    }

    fun deleteProduct(productId: String) {
        var changed = false
        _catalogs.update { currentCatalogs ->
            val activeId = _activeCatalogId.value
            val catalog = currentCatalogs[activeId] ?: return@update currentCatalogs
            val updated = catalog.products.filterNot { it.id == productId }
            changed = updated.size < catalog.products.size
            if(changed) currentCatalogs.toMutableMap().apply {
                this[activeId] = catalog.copy(products = updated)
            } else currentCatalogs
        }
        if(changed) {
            _itemQuantities.update { it - productId }
            _productMultiplierAssignments.update { it - productId }
            saveCatalogs()
        }
    }

    fun updateQuantity(productId: String, quantityString: String) {
        _itemQuantities.update { it.toMutableMap().apply { this[productId] = quantityString } }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setSortCriteria(criteria: ProductSortCriteria) {
        _productSortCriteria.value = criteria
    }

    fun clearAllQuantitiesAndAssignments() {
        Log.i(TAG, "Clearing all item quantities, multiplier assignments, and customer information.")
        _itemQuantities.value = emptyMap()
        _productMultiplierAssignments.value = emptyMap()

        // Reset customer information and quote
        _quoteCustomerName.value = ""
        _quoteCustomerEmail.value = ""
        _quoteCustomerPhone.value = ""
        _quoteCompanyName.value = ""
        _quoteCustomMessage.value = ""
        _currentQuote.value = null

        // Reset tax and discount rates (optional)
        _taxRate.value = 0.0
        _globalDiscountRate.value = 0.0
    }

    private fun sortProducts(products: List<Product>, criteria: ProductSortCriteria): List<Product> {
        return when (criteria) {
            ProductSortCriteria.NAME_ASC -> products.sortedBy { it.name.lowercase() }
            ProductSortCriteria.NAME_DESC -> products.sortedByDescending { it.name.lowercase() }
            ProductSortCriteria.PRICE_ASC -> products.sortedBy { it.basePrice }
            ProductSortCriteria.PRICE_DESC -> products.sortedByDescending { it.basePrice }
            ProductSortCriteria.CATEGORY_ASC -> products.sortedBy { it.category.lowercase() }
            ProductSortCriteria.CATEGORY_DESC -> products.sortedByDescending { it.category.lowercase() }
        }
    }

    // =============================================
    // Multiplier Management
    // =============================================

    fun setMultiplierToEdit(multiplier: Multiplier?) {
        _multiplierToEdit.value = multiplier
    }

    fun addOrUpdateMultiplier(multiplier: Multiplier) {
        var changed = false
        _catalogs.update { catalogsMap ->
            val id = _activeCatalogId.value
            val cat = catalogsMap[id] ?: return@update catalogsMap
            val index = cat.multipliers.indexOfFirst { it.id == multiplier.id }
            changed = true
            val updated = if (index != -1) {
                cat.multipliers.toMutableList().apply { this[index] = multiplier }
            } else {
                cat.multipliers + multiplier
            }
            catalogsMap.toMutableMap().apply { this[id] = cat.copy(multipliers = updated.toList()) }
        }
        if(changed) saveCatalogs()
        _multiplierToEdit.value = null
    }

    fun deleteMultiplier(multiplierId: String) {
        var changed = false
        _catalogs.update { catalogsMap ->
            val id = _activeCatalogId.value
            val cat = catalogsMap[id] ?: return@update catalogsMap
            val updated = cat.multipliers.filterNot { it.id == multiplierId }
            changed = updated.size < cat.multipliers.size
            if(changed) catalogsMap.toMutableMap().apply {
                this[id] = cat.copy(multipliers = updated)
            } else catalogsMap
        }
        if (changed) {
            _productMultiplierAssignments.update { paMap ->
                paMap.mapValues { (_, assigns) -> assigns - multiplierId }.toMutableMap()
            }
            if (_multiplierToEdit.value?.id == multiplierId) _multiplierToEdit.value = null
            saveCatalogs()
        }
    }

    fun confirmMultiplierAssignment(productId: String, assignments: Map<String, String>) {
        Log.d(TAG, "Confirming assignments for Product $productId: $assignments")

        val validAssignments = assignments.mapNotNull { (multId, qtyString) ->
            val quantity = qtyString.toIntOrNull()
            if (quantity != null && quantity > 0) {
                multId to quantity
            } else {
                null
            }
        }.toMap()

        val totalItemQty = _itemQuantities.value[productId]?.toIntOrNull() ?: 0
        val assignedSum = validAssignments.values.sum()
        if (assignedSum > totalItemQty && totalItemQty > 0) {
            Log.w(TAG, "Assigned multiplier quantities ($assignedSum) exceed total item quantity ($totalItemQty) for product $productId. Calculation will apply multipliers independently.")
        }

        _productMultiplierAssignments.update { currentAssignments ->
            currentAssignments.toMutableMap().apply {
                this[productId] = validAssignments
            }
        }
        Log.d(TAG, "Stored valid assignments for $productId: $validAssignments")
        dismissDialog()
    }

    // =============================================
    // Quote Management
    // =============================================

    fun setGlobalDiscount(rate: Double) {
        val validatedRate = rate.coerceIn(0.0, 100.0)
        if (_globalDiscountRate.value == validatedRate) return
        _globalDiscountRate.value = validatedRate
        Log.d(TAG, "Global discount rate set to: $validatedRate%")

        if (_uiMode.value == UiMode.QUOTE_PREVIEW) {
            buildQuote()
        }
    }

    fun setTaxRate(newRate: Double) {
        val rate = if(newRate >= 0) newRate else 0.0
        if (_taxRate.value == rate) return dismissDialog()
        _taxRate.value = rate
        if (_uiMode.value == UiMode.QUOTE_PREVIEW && _currentQuote.value != null) {
            _currentQuote.update { it?.copy(taxRate = rate) }
        }
        dismissDialog()
    }

    fun removeQuoteItem(itemId: String) {
        _currentQuote.update { q ->
            q?.copy(items = q.items.filterNot { it.id == itemId })
                .takeIf { it?.items?.isNotEmpty() == true }
        }
        if (_currentQuote.value == null) showCatalogView()
    }

    fun updateQuoteDetails(companyName: String, customMessage: String) {
        _quoteCompanyName.value = companyName
        _quoteCustomMessage.value = customMessage
        _currentQuote.update { q -> q?.copy(companyName = companyName, customMessage = customMessage) }
        dismissDialog()
    }
    // Add a custom item directly to the quote without saving to catalog
    fun addCustomItemToQuote(product: Product, quantity: Int) {
        Log.i(TAG, "Adding custom item to quote: ${product.name}, Qty: $quantity")

        // Update item quantities
        _itemQuantities.update { current ->
            current.toMutableMap().apply {
                this[product.id] = quantity.toString()
            }
        }

        // Temporarily add the product to active catalog for quote generation
        _catalogs.update { catalogsMap ->
            val id = _activeCatalogId.value
            val cat = catalogsMap[id] ?: return@update catalogsMap
            val updatedProducts = cat.products + product
            catalogsMap.toMutableMap().apply {
                this[id] = cat.copy(products = updatedProducts)
            }
        }

        // Build an updated quote
        buildQuote()

        // If we're not already in quote preview, switch to it
        if (_uiMode.value != UiMode.QUOTE_PREVIEW) {
            if(_currentQuote.value?.customerName.isNullOrBlank()) {
                _quoteCustomerName.value = ""
                showDialog(DialogState.CUSTOMER_DETAILS)
            } else {
                _uiMode.value = UiMode.QUOTE_PREVIEW
            }
        }

        dismissDialog()
    }
    // Add a voucher to the quote (appears as a negative value item)
    fun addVoucherToQuote(voucherProduct: Product) {
        Log.i(TAG, "Adding voucher to quote: ${voucherProduct.name}, Amount: ${-voucherProduct.basePrice}")

        // Vouchers always have quantity of 1
        _itemQuantities.update { current ->
            current.toMutableMap().apply {
                this[voucherProduct.id] = "1"
            }
        }

        // Temporarily add the voucher product to active catalog for quote generation
        _catalogs.update { catalogsMap ->
            val id = _activeCatalogId.value
            val cat = catalogsMap[id] ?: return@update catalogsMap
            val updatedProducts = cat.products + voucherProduct
            catalogsMap.toMutableMap().apply {
                this[id] = cat.copy(products = updatedProducts)
            }
        }

        // Build an updated quote
        buildQuote()

        // If we're not already in quote preview, switch to it
        if (_uiMode.value != UiMode.QUOTE_PREVIEW) {
            if(_currentQuote.value?.customerName.isNullOrBlank()) {
                _quoteCustomerName.value = ""
                showDialog(DialogState.CUSTOMER_DETAILS)
            } else {
                _uiMode.value = UiMode.QUOTE_PREVIEW
            }
        }

        dismissDialog()
    }
    private fun buildQuote() {
        Log.i(TAG,"Building Quote with partial multiplier quantities...")
        val catalog = activeCatalog.value ?: return Unit.also { Log.e(TAG,"QF:No cat") }
        val quantitiesStateMap = _itemQuantities.value
        val productAssignmentsMap = _productMultiplierAssignments.value

        val quoteItems = catalog.products.mapNotNull { product ->
            val totalQuantity = quantitiesStateMap[product.id]?.toIntOrNull() ?: 0
            Log.d(TAG, "--- Processing Product for Quote: ${product.name} (ID: ${product.id}) ---")
            Log.d(TAG, "Raw Total Qty String: ${quantitiesStateMap[product.id]}, Parsed Total Qty: $totalQuantity")
            Log.d(TAG, "Product Base Price: ${product.basePrice}")

            if (totalQuantity > 0) {
                val itemMultiplierQuantities = productAssignmentsMap[product.id] ?: emptyMap()
                Log.d(TAG, "Partial Multiplier Quantities for ${product.name}: $itemMultiplierQuantities")

                val appliedMultiplierDetails = itemMultiplierQuantities.keys.mapNotNull { multId ->
                    catalog.multipliers.find { it.id == multId }?.let { globalMultiplier ->
                        Log.d(TAG, "   -> Mapping Global Multiplier: ${globalMultiplier.name}, Value: ${globalMultiplier.value}, Type: ${globalMultiplier.type}, Discountable: ${globalMultiplier.isDiscountable}")
                        AppliedMultiplier(
                            multiplierId = multId,
                            name = globalMultiplier.name,
                            type = globalMultiplier.type,
                            appliedValue = globalMultiplier.value,
                            isDiscountable = globalMultiplier.isDiscountable
                        )
                    }
                }
                Log.d(TAG, "Applied Multiplier Details for ${product.name}: $appliedMultiplierDetails")

                val newQuoteItem = QuoteItem(
                    product = product.copy(),
                    quantity = totalQuantity,
                    partialMultiplierQuantities = itemMultiplierQuantities,
                    appliedMultipliers = appliedMultiplierDetails,
                    isBaseItemDiscountable = product.isDiscountable
                )
                Log.d(TAG, "Created QuoteItem for ${product.name} with Qty: ${newQuoteItem.quantity}, BaseDiscountable: ${newQuoteItem.isBaseItemDiscountable}")
                Log.d(TAG, "   QuoteItem Partial Qty Map: ${newQuoteItem.partialMultiplierQuantities}")
                Log.d(TAG, "   QuoteItem Applied Mults: ${newQuoteItem.appliedMultipliers}")
                Log.d(TAG, "   --- Triggering QuoteItem.lineTotalBeforeDiscount calculation now ---")
                Log.d(TAG, "   QuoteItem calculated lineTotalBeforeDiscount: ${newQuoteItem.lineTotalBeforeDiscount}")

                newQuoteItem
            } else {
                Log.d(TAG, "Skipping ${product.name} for quote, total quantity is 0.")
                null
            }
        }

        if (quoteItems.isNotEmpty()) {
            val existingQuote = _currentQuote.value

            val catalogCompanyName = catalog.companyName

            _currentQuote.value = Quote(
                id = existingQuote?.id ?: UUID.randomUUID().toString(),
                customerName = existingQuote?.customerName ?: _quoteCustomerName.value,
                customerEmail = existingQuote?.customerEmail ?: _quoteCustomerEmail.value,
                customerPhone = existingQuote?.customerPhone ?: _quoteCustomerPhone.value,
                companyName = existingQuote?.companyName ?: catalogCompanyName, // Use catalog company name
                customMessage = existingQuote?.customMessage ?: _quoteCustomMessage.value,
                items = quoteItems,
                taxRate = _taxRate.value,
                globalDiscountRate = _globalDiscountRate.value
            )
            Log.i(TAG,"Quote built/updated: ${quoteItems.size} items. Cust: ${_currentQuote.value?.customerName}, Email: ${_currentQuote.value?.customerEmail}, Phone: ${_currentQuote.value?.customerPhone}, Discount: ${_currentQuote.value?.globalDiscountRate}%")
        } else {
            _currentQuote.value = null
            Log.i(TAG,"Quote Empty")
        }
    }

    // =============================================
    // Catalog Management
    // =============================================

    fun createCatalog(name: String) {
        val trimmed = name.trim().ifBlank { return }
        val newId = UUID.randomUUID().toString()
        val newCat = Catalog(id = newId, name = trimmed)
        _catalogs.update { it + (newId to newCat) }
        saveCatalogs()
        loadCatalog(newId)
    }

    fun renameCatalog(catalogId: String, newName: String) {
        val trimmed = newName.trim().ifBlank { return }
        var changed = false
        _catalogs.update { map ->
            map[catalogId]?.takeIf { it.name != trimmed }?.let {
                changed = true
                map.toMutableMap().apply { this[catalogId] = it.copy(name = trimmed) }
            } ?: map
        }
        if(changed) saveCatalogs()
    }

    fun deleteCatalog(catalogId: String) {
        Log.i(TAG, "[DELETE] Attempting delete: $catalogId. Active is: ${_activeCatalogId.value}")

        if (catalogId == _activeCatalogId.value) {
            Log.w(TAG, "[DELETE] Cannot delete the currently active catalog.")
            return
        }

        if (_catalogs.value.size <= 1) {
            Log.w(TAG, "[DELETE] Cannot delete the last remaining catalog.")
            return
        }

        var changed = false
        val originalMap = _catalogs.value
        _catalogs.update { currentMap ->
            if(currentMap.containsKey(catalogId)){
                Log.d(TAG, "[DELETE] Update block: Removing $catalogId")
                changed = true
                currentMap - catalogId
            } else {
                Log.w(TAG, "[DELETE] Update block: ID $catalogId not found.")
                currentMap
            }
        }

        if(changed){
            val mapAfterUpdate = _catalogs.value
            Log.i(TAG, "[DELETE] StateFlow updated. Size Before: ${originalMap.size}, Size After: ${mapAfterUpdate.size}. Triggering save.")
            if (mapAfterUpdate.containsKey(catalogId)) {
                Log.e(TAG, "[DELETE] *** ERROR: Catalog $catalogId still present in map AFTER update! ***")
            }
            if (_activeCatalogId.value == catalogId) {
                Log.i(TAG,"[DELETE] Deleted was active, switching to default.")
                loadCatalog(DEFAULT_CATALOG_ID)
            }
            saveCatalogs()
        } else {
            Log.w(TAG, "[DELETE] Delete finished, no changes made.")
        }
    }

    fun loadCatalog(catalogId: String) {
        if (_catalogs.value.containsKey(catalogId)) {
            if (_activeCatalogId.value != catalogId) {
                _activeCatalogId.value = catalogId
                _itemQuantities.value = emptyMap()
                _productMultiplierAssignments.value = emptyMap()
                Log.i(TAG, "Switched active catalog to: $catalogId")
            }
            dismissDialog()
        } else {
            Log.w(TAG, "Attempted load non-existent catalog: $catalogId")
        }
    }

    fun requestShareCatalog(catalogId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Preparing catalog '$catalogId' for sharing...")
            val catalogToShare = _catalogs.value[catalogId]
            if (catalogToShare == null) {
                Log.e(TAG, "Cannot share catalog '$catalogId': Not found.")
                return@launch
            }
            try {
                val jsonString = json.encodeToString(catalogToShare)

                val safeName = catalogToShare.name.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(50)
                val fileName = "catalog_${safeName}.json"
                val shareDir = File(appContext.cacheDir, "shared_catalogs")
                shareDir.mkdirs()
                val tempFile = File(shareDir, fileName)

                tempFile.writeText(jsonString)
                Log.d(TAG, "Saved temporary catalog JSON to: ${tempFile.absolutePath}")

                val authority = "${appContext.packageName}.fileprovider"
                val contentUri: Uri = FileProvider.getUriForFile(
                    appContext,
                    authority,
                    tempFile
                )
                Log.d(TAG, "Generated content URI for sharing: $contentUri")

                withContext(Dispatchers.Main) {
                    _shareCatalogEvent.emit(contentUri)
                }
            } catch (e: IOException) {
                Log.e(TAG, "IOError preparing catalog '$catalogId' for sharing", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error preparing catalog '$catalogId' for sharing", e)
            }
        }
    }
    fun importRenamedCatalog(catalog: Catalog, newName: String) {
        viewModelScope.launch {
            val trimmedName = newName.trim()

            if (trimmedName.isBlank()) {
                _snackbarMessage.emit("Import canceled: Name cannot be empty")
                return@launch
            }

            // Check if the new name also exists
            val nameExists = _catalogs.value.values.any {
                it.name.equals(trimmedName, ignoreCase = true)
            }

            if (nameExists) {
                //_nameConflictEvent.emit(Pair(catalog, trimmedName))
                Log.w(TAG, "New catalog name '$trimmedName' also exists. Alerting user again.")
                return@launch
            }

            // Proceed with import using new name
            val catalogToAdd = catalog.copy(
                id = UUID.randomUUID().toString(),
                name = trimmedName
            )

            _catalogs.update { currentCatalogs ->
                Log.i(TAG, "Adding renamed imported catalog '$trimmedName' with new ID: ${catalogToAdd.id}")
                currentCatalogs + (catalogToAdd.id to catalogToAdd)
            }

            saveCatalogs()
            _snackbarMessage.emit("Catalog '$trimmedName' imported successfully!")
        }
    }
    fun importCatalog(importedCatalog: Catalog?) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Importing catalog..."

            try {
                if (importedCatalog == null) {
                    Log.e(TAG, "Import failed: Parsed catalog data was null.")
                    _snackbarMessage.emit("Import failed: Invalid file content.")
                    return@launch
                }

                // Create a new catalog with a fresh ID
                val catalogToAdd = importedCatalog.copy(id = UUID.randomUUID().toString())

                _catalogs.update { currentCatalogs ->
                    Log.i(TAG, "Adding imported catalog '${catalogToAdd.name}' with new ID: ${catalogToAdd.id}")
                    currentCatalogs + (catalogToAdd.id to catalogToAdd)
                }

                saveCatalogs()
                _snackbarMessage.emit("Catalog '${catalogToAdd.name}' imported successfully!")
            } finally {
                _isLoading.value = false
                _loadingMessage.value = null
            }
        }
    }

    // =============================================
    // Prospect & Notes Management
    // =============================================

    fun toggleProspectStatus(prospectId: String) {
        viewModelScope.launch {
            var finalRecordState: ProspectRecord? = null

            _prospectRecords.update { currentList ->
                val index = currentList.indexOfFirst { it.id == prospectId }
                if (index != -1) {
                    val record = currentList[index]
                    val newStatus = if (record.status == ProspectStatus.PROSPECT) ProspectStatus.CUSTOMER else ProspectStatus.PROSPECT

                    val updatedRecord = record.copy(
                        status = newStatus,
                        dateUpdated = System.currentTimeMillis()
                    )
                    finalRecordState = updatedRecord
                    Log.i(TAG, "Toggling status for ${record.customerName} to $newStatus")

                    currentList.toMutableList().apply { this[index] = updatedRecord }
                } else {
                    Log.e(TAG, "Cannot toggle status, prospect ID $prospectId not found.")
                    currentList
                }
            }

            if (finalRecordState != null) {
                saveProspectRecords()

                if (_selectedProspectRecord.value?.id == prospectId) {
                    _selectedProspectRecord.value = finalRecordState
                    Log.d(TAG,"Updated _selectedProspectRecord state after status toggle.")
                }

                _snackbarMessage.emit("${finalRecordState!!.customerName} marked as ${finalRecordState!!.status.name.lowercase()}.")
            }
        }
    }

    fun createOrUpdateProspectRecord(quote: Quote, externalPdfUriString: String?) {
        viewModelScope.launch {
            val newRecord = ProspectRecord(
                customerName = quote.customerName,
                customerEmail = quote.customerEmail,
                customerPhone = quote.customerPhone,
                quoteSnapshot = quote.copy(),
                externalPdfUriString = externalPdfUriString,
                status = ProspectStatus.PROSPECT,
                notes = emptyList(),
                dateUpdated = System.currentTimeMillis()
            )

            _prospectRecords.update { currentList ->
                Log.i(TAG, "Creating new ProspectRecord for: ${newRecord.customerName} (Quote ID: ${quote.id})")
                listOf(newRecord) + currentList
            }
            saveProspectRecords()
            _snackbarMessage.emit("Prospect created for ${newRecord.customerName}")
        }
    }

    // =============================================
    // Note Management
    // =============================================

    fun addNoteToProspect(prospectId: String, content: String, type: NoteType = NoteType.MANUAL) {
        if (content.isBlank()) return

        viewModelScope.launch {
            var customerNameForFeedback: String? = null
            var updatedRecordForSelection: ProspectRecord? = null

            _prospectRecords.update { currentList ->
                val index = currentList.indexOfFirst { it.id == prospectId }
                if (index != -1) {
                    val record = currentList[index]
                    customerNameForFeedback = record.customerName

                    val newNote = Note(
                        content = content.trim(),
                        timestamp = System.currentTimeMillis(),
                        type = type
                    )

                    val updatedRecord = record.copy(
                        notes = listOf(newNote) + record.notes,
                        dateUpdated = System.currentTimeMillis()
                    )
                    updatedRecordForSelection = updatedRecord

                    Log.i(TAG, "Adding note to ${record.customerName}: '$content' (Type: $type)")
                    currentList.toMutableList().apply { this[index] = updatedRecord }
                } else {
                    Log.e(TAG, "Cannot add note, prospect ID $prospectId not found.")
                    currentList
                }
            }

            if (customerNameForFeedback != null) {
                saveProspectRecords()

                if (_selectedProspectRecord.value?.id == prospectId && updatedRecordForSelection != null) {
                    _selectedProspectRecord.value = updatedRecordForSelection
                    Log.d(TAG,"Updated _selectedProspectRecord with new note.")
                }

                _snackbarMessage.emit("Note added for $customerNameForFeedback.")
            }
        }
    }

    fun editNote(prospectId: String, note: Note, newContent: String) {
        if (newContent.isBlank()) return

        viewModelScope.launch {
            var customerNameForFeedback: String? = null
            var updatedRecordForSelection: ProspectRecord? = null

            _prospectRecords.update { currentList ->
                val recordIndex = currentList.indexOfFirst { it.id == prospectId }
                if (recordIndex != -1) {
                    val record = currentList[recordIndex]
                    customerNameForFeedback = record.customerName

                    val noteIndex = record.notes.indexOfFirst { it.id == note.id }

                    if (noteIndex != -1) {
                        val updatedNotes = record.notes.toMutableList()

                        // Create updated note that preserves the original type
                        updatedNotes[noteIndex] = note.copy(
                            content = newContent.trim(),
                            timestamp = System.currentTimeMillis()
                            // Note type remains the same as the original
                        )

                        val updatedRecord = record.copy(
                            notes = updatedNotes,
                            dateUpdated = System.currentTimeMillis()
                        )
                        updatedRecordForSelection = updatedRecord

                        Log.i(TAG, "Editing ${note.type} note for ${record.customerName}: '$newContent'")
                        currentList.toMutableList().apply { this[recordIndex] = updatedRecord }
                    } else {
                        Log.e(TAG, "Cannot edit note, note not found")
                        currentList
                    }
                } else {
                    Log.e(TAG, "Cannot edit note, prospect ID $prospectId not found.")
                    currentList
                }
            }

            if (customerNameForFeedback != null) {
                saveProspectRecords()

                if (_selectedProspectRecord.value?.id == prospectId && updatedRecordForSelection != null) {
                    _selectedProspectRecord.value = updatedRecordForSelection
                    Log.d(TAG, "Updated _selectedProspectRecord with edited note.")
                }

                _snackbarMessage.emit("Note updated for $customerNameForFeedback.")
            }
        }
    }
    fun logInteraction(prospectId: String, interactionType: String) {
        val noteType = when (interactionType) {
            "Called" -> NoteType.CALL
            "Emailed" -> NoteType.EMAIL
            "Texted" -> NoteType.TEXT
            else -> NoteType.SYSTEM
        }

        val timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
        val content = "$interactionType customer on $timestamp"

        addNoteToProspect(prospectId, content, noteType)
    }

    // =============================================
    // PDF Generation
    // =============================================

    fun generatePdfToUri(context: Context, quote: Quote?, dirUri: Uri, fileName: String): Uri? {
        _isLoading.value = true
        _loadingMessage.value = "Generating PDF..."
        if (quote == null || quote.customerName.isBlank() || fileName.isBlank()) {
            Log.e(TAG, "generatePdfToUri preconditions failed.")
            viewModelScope.launch { _snackbarMessage.emit("PDF: Missing data.") }
            return null
        }

        Log.i(TAG, "Generating PDF (OpenDir) to Dir: ${dirUri.path}, File: $fileName")
        val parentDocument = DocumentFile.fromTreeUri(context, dirUri)
        if (parentDocument == null || !parentDocument.canWrite()) {
            Log.e(TAG, "Cannot write to directory URI: $dirUri")
            viewModelScope.launch { _snackbarMessage.emit("PDF: Folder access error.") }
            return null
        }

        var tempPdfFile: DocumentFile? = null
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var currentPage: PdfDocument.Page
        var canvas: Canvas
        var yPos: Float
        val itemsToDraw = quote.items.toList()
        var itemIndex = 0

        try {
            tempPdfFile = parentDocument.createFile("application/pdf", fileName)
                ?: throw IOException("Failed to create DocumentFile: '$fileName'.")
            Log.d(TAG, "DocumentFile created: ${tempPdfFile.uri}")
            if (!tempPdfFile.canWrite()) {
                throw IOException("Created DocumentFile not writable.")
            }

            context.contentResolver.openOutputStream(tempPdfFile.uri)?.use { outputStream ->
                Log.d(TAG, "OutputStream obtained. Starting PDF content generation...")

                while (itemIndex < itemsToDraw.size) {
                    currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(
                        PdfGenerator.PAGE_WIDTH, PdfGenerator.PAGE_HEIGHT, pageNumber).create())
                    canvas = currentPage.canvas
                    yPos = PdfGenerator.MARGIN
                    Log.d(TAG,"VM: Starting Page $pageNumber")

                    if (pageNumber == 1) {
                        yPos = PdfGenerator.drawHeader(canvas, quote, yPos)
                        yPos += 10f
                    }
                    yPos = PdfGenerator.drawTableHeader(canvas, yPos)

                    var itemsDrawnThisPage = 0
                    while (itemIndex < itemsToDraw.size) {
                        val item = itemsToDraw[itemIndex]
                        val layoutInfo = PdfGenerator.calculateItemLayoutInfo(item)
                        val totalRowNeeded = layoutInfo.rowHeight
                        val pageContentBottom = PdfGenerator.PAGE_HEIGHT - PdfGenerator.MARGIN - PdfGenerator.FOOTER_AREA_HEIGHT

                        if (yPos + totalRowNeeded > pageContentBottom) {
                            Log.d(TAG, "VM: Item '${item.product.name}' needs page break from page $pageNumber.")
                            break
                        }
                        PdfGenerator.drawSingleItemRow(canvas, item, layoutInfo, yPos)
                        yPos += totalRowNeeded
                        itemsDrawnThisPage++
                        itemIndex++
                    }
                    pdfDocument.finishPage(currentPage)
                    Log.d(TAG,"VM: Finished Page $pageNumber, drew $itemsDrawnThisPage items. Final Y: $yPos")

                    if (itemIndex < itemsToDraw.size) { pageNumber++ }
                }

                pageNumber++
                Log.d(TAG,"VM: Starting dedicated page $pageNumber for Totals/Footer.")
                currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(
                    PdfGenerator.PAGE_WIDTH, PdfGenerator.PAGE_HEIGHT, pageNumber).create())
                canvas = currentPage.canvas
                yPos = PdfGenerator.MARGIN + 20f

                canvas.drawLine(PdfGenerator.MARGIN, yPos, PdfGenerator.PAGE_WIDTH - PdfGenerator.MARGIN,
                    yPos, PdfGenerator.linePaint.apply{ strokeWidth = 0.8f })
                yPos += 10f
                yPos = PdfGenerator.drawTotals(canvas, quote, yPos)
                yPos += 30f
                PdfGenerator.bodyTextPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("Thank you!", (PdfGenerator.PAGE_WIDTH / 2f), yPos, PdfGenerator.bodyTextPaint)
                PdfGenerator.bodyTextPaint.textAlign = Paint.Align.LEFT
                pdfDocument.finishPage(currentPage)
                Log.d(TAG,"VM: Finished Totals Page $pageNumber.")

                pdfDocument.writeTo(outputStream)
                outputStream.flush()
                Log.i(TAG, "Finished writing PDF content via OutputStream")
            } ?: throw IOException("Failed to open OutputStream for ${tempPdfFile.uri}")

            pdfDocument.close()
            Log.i(TAG, "PDF generation successful (OpenDir): ${tempPdfFile.uri}")
            return tempPdfFile.uri

        } catch (e: Exception) {
            Log.e(TAG, "Error during PDF file creation or writing (OpenDir)", e)
            viewModelScope.launch { _snackbarMessage.emit("PDF Error: ${e.localizedMessage ?: "Unknown"}") }
            try {
                tempPdfFile?.delete()
            } catch (delEx: Exception) {
                Log.e(TAG, "Error deleting partial file", delEx)
            }
            pdfDocument.close()
            return null
        } finally {
            // Add this finally block to reset loading state
            _isLoading.value = false
            _loadingMessage.value = null}
    }

    // =============================================
    // Persistence
    // =============================================
    private fun updateSelectedProspect(updatedProspect: ProspectRecord?) {
        // Update the selected prospect if it's the one being modified and not null
        if (updatedProspect != null && _selectedProspectRecord.value?.id == updatedProspect.id) {
            _selectedProspectRecord.value = updatedProspect
        }
    }
    private fun saveCatalogs() {
        val catalogsToSave = _catalogs.value
        viewModelScope.launch(Dispatchers.IO) {
            if (catalogsToSave.isEmpty()){
                Log.w(TAG,"[SAVE] Skip save empty")
                return@launch
            }

            Log.d(TAG, "[SAVE] Starting save. Map size to save: ${catalogsToSave.size}. Keys: ${catalogsToSave.keys}")
            try {
                val jsonString = json.encodeToString(catalogsToSave)
                val file = File(appContext.filesDir, CATALOG_FILE_NAME)
                file.writeText(jsonString)
                Log.i(TAG, "[SAVE] File write completed successfully for $CATALOG_FILE_NAME.")
            } catch (e: IOException) {
                Log.e(TAG, "[SAVE] IOException during save", e)
            } catch (e: Exception) {
                Log.e(TAG, "[SAVE] Serialization/Other error during save", e)
            }
            Log.d(TAG, "[SAVE] saveCatalogs coroutine finished.")
        }
    }

    private fun saveProspectRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            val recordsToSave = _prospectRecords.value
            Log.d(TAG, "[SAVE_PROSPECTS] Saving ${recordsToSave.size} prospect records...")
            try {
                val jsonString = json.encodeToString(recordsToSave)
                val file = File(appContext.filesDir, PROSPECTS_FILE_NAME)
                file.writeText(jsonString)
                Log.i(TAG, "[SAVE_PROSPECTS] Prospect records saved successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "[SAVE_PROSPECTS] Error saving prospect records", e)
            }
        }
    }

    private fun loadCatalogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(appContext.filesDir, CATALOG_FILE_NAME)
            if (file.exists() && file.length() > 0) {
                Log.d(TAG, "Loading catalogs from $CATALOG_FILE_NAME...")
                try {
                    val jsonString = file.readText()
                    val loadedCatalogs: Map<String, Catalog> = json.decodeFromString(jsonString)
                    if (loadedCatalogs.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _catalogs.value = loadedCatalogs
                            if (!loadedCatalogs.containsKey(_activeCatalogId.value)) {
                                _activeCatalogId.value = loadedCatalogs.keys.first()
                                _itemQuantities.value = emptyMap()
                                _productMultiplierAssignments.value = emptyMap()
                            }
                            Log.i(TAG, "Catalogs loaded successfully. Count: ${loadedCatalogs.size}. Active ID: ${_activeCatalogId.value}")
                        }
                    } else {
                        Log.w(TAG, "Loaded catalog file empty/invalid. Initializing default.")
                        initializeDefaultCatalog()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading/decoding catalog file", e)
                    initializeDefaultCatalog()
                }
            } else {
                Log.i(TAG, "Catalog file not found or empty. Initializing default.")
                initializeDefaultCatalog()
            }
        }
    }
    private fun loadProspectRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(appContext.filesDir, PROSPECTS_FILE_NAME)
            if (file.exists() && file.length() > 0) {
                Log.d(TAG, "[LOAD_PROSPECTS] Loading prospect records...")
                try {
                    val jsonString = file.readText()

                    // Try loading with the new Note structure first
                    try {
                        val loadedRecords: List<ProspectRecord> = json.decodeFromString(jsonString)

                        // Migrate image data after loading
                        val migratedRecords = loadedRecords.map { record ->
                            val updatedRecord = record.copy()

                            // Migrate single images to lists if needed
                            val beforeImages = mutableListOf<Pair<String, Long>>()
                            val afterImages = mutableListOf<Pair<String, Long>>()

                            // Add legacy "before" image if it exists and list is empty
                            if (updatedRecord.beforeImageUriString != null &&
                                updatedRecord.beforeImageTimestamp != null &&
                                updatedRecord.beforeImageUris.isEmpty()) {
                                beforeImages.add(Pair(updatedRecord.beforeImageUriString!!,
                                    updatedRecord.beforeImageTimestamp!!))
                            }

                            // Add legacy "after" image if it exists and list is empty
                            if (updatedRecord.afterImageUriString != null &&
                                updatedRecord.afterImageTimestamp != null &&
                                updatedRecord.afterImageUris.isEmpty()) {
                                afterImages.add(Pair(updatedRecord.afterImageUriString!!,
                                    updatedRecord.afterImageTimestamp!!))
                            }

                            // Update with migrated data if necessary
                            if (beforeImages.isNotEmpty() || afterImages.isNotEmpty()) {
                                updatedRecord.copy(
                                    beforeImageUris = updatedRecord.beforeImageUris + beforeImages,
                                    afterImageUris = updatedRecord.afterImageUris + afterImages
                                )
                            } else {
                                updatedRecord
                            }
                        }

                        withContext(Dispatchers.Main) {
                            _prospectRecords.value = migratedRecords
                            Log.i(TAG, "[LOAD_PROSPECTS] ${migratedRecords.size} prospect records loaded and migrated.")
                        }
                    } catch (e: Exception) {
                        // Handle migration from old format
                        Log.w(TAG, "[LOAD_PROSPECTS] Error loading with new structure, attempting migration", e)
                        migrateOldProspectRecords(jsonString)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "[LOAD_PROSPECTS] Error loading/decoding prospect records", e)
                    withContext(Dispatchers.Main) {
                        _prospectRecords.value = emptyList()
                    }
                }
            } else {
                Log.i(TAG, "[LOAD_PROSPECTS] No prospect records file found. Starting with empty list.")
                withContext(Dispatchers.Main) {
                    _prospectRecords.value = emptyList()
                }
            }
        }
    }

    /**
     * Sets or updates a reminder for a prospect
     */
    fun setProspectReminder(prospectId: String, reminderDateTime: Long, reminderNote: String?) {
        viewModelScope.launch {
            var customerNameForFeedback: String? = null
            var updatedRecordForSelection: ProspectRecord? = null
            var previousReminderTime: Long? = null

            _prospectRecords.update { currentList ->
                val index = currentList.indexOfFirst { it.id == prospectId }
                if (index != -1) {
                    val record = currentList[index]
                    customerNameForFeedback = record.customerName
                    previousReminderTime = record.reminderDateTime

                    val updatedRecord = record.copy(
                        reminderDateTime = reminderDateTime,
                        reminderNote = reminderNote,
                        dateUpdated = System.currentTimeMillis()
                    )
                    updatedRecordForSelection = updatedRecord

                    Log.i(TAG, "Setting reminder for ${record.customerName} at ${Date(reminderDateTime)}")
                    currentList.toMutableList().apply { this[index] = updatedRecord }
                } else {
                    Log.e(TAG, "Cannot set reminder, prospect ID $prospectId not found.")
                    currentList
                }
            }

            if (customerNameForFeedback != null) {
                saveProspectRecords()

                // Schedule new reminder via AlarmHelper
                ReminderManager.scheduleReminder(
                    context = appContext,
                    prospectId = prospectId,
                    customerName = customerNameForFeedback!!,
                    reminderTime = reminderDateTime,
                    reminderNote = reminderNote
                )

                // Cancel previous reminder if it was different
                if (previousReminderTime != null && previousReminderTime != reminderDateTime) {
                    ReminderManager.cancelReminder(appContext, prospectId)
                }

                if (_selectedProspectRecord.value?.id == prospectId && updatedRecordForSelection != null) {
                    _selectedProspectRecord.value = updatedRecordForSelection
                    Log.d(TAG, "Updated _selectedProspectRecord with new reminder.")
                }

                val date = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                    .format(Date(reminderDateTime))
                _snackbarMessage.emit("Reminder set for $customerNameForFeedback on $date")
            }
        }
    }
    fun deleteProspectRecord(prospectId: String) {
        viewModelScope.launch {
            var prospectName: String? = null

            _prospectRecords.update { currentList ->
                val index = currentList.indexOfFirst { it.id == prospectId }
                if (index != -1) {
                    val prospect = currentList[index]
                    prospectName = prospect.customerName
                    Log.i(TAG, "Deleting prospect record for ${prospect.customerName} (ID: $prospectId)")

                    // Remove the record from the list
                    currentList.toMutableList().apply {
                        removeAt(index)
                    }
                } else {
                    Log.e(TAG, "Cannot delete prospect, ID $prospectId not found.")
                    currentList
                }
            }

            // Save updated records list
            saveProspectRecords()

            // Show confirmation message
            prospectName?.let {
                _snackbarMessage.emit("Record for $it deleted.")
            }

            // Go back to prospects list
            showProspectsScreen()
        }
    }
    /**
     * Clears a reminder for a prospect
     */
    fun clearProspectReminder(prospectId: String) {
        viewModelScope.launch {
            var customerNameForFeedback: String? = null
            var updatedRecordForSelection: ProspectRecord? = null

            _prospectRecords.update { currentList ->
                val index = currentList.indexOfFirst { it.id == prospectId }
                if (index != -1) {
                    val record = currentList[index]
                    customerNameForFeedback = record.customerName

                    val updatedRecord = record.copy(
                        reminderDateTime = null,
                        reminderNote = null,
                        dateUpdated = System.currentTimeMillis()
                    )
                    updatedRecordForSelection = updatedRecord

                    Log.i(TAG, "Clearing reminder for ${record.customerName}")
                    currentList.toMutableList().apply { this[index] = updatedRecord }
                } else {
                    Log.e(TAG, "Cannot clear reminder, prospect ID $prospectId not found.")
                    currentList
                }
            }

            if (customerNameForFeedback != null) {
                saveProspectRecords()

                // Cancel reminder via AlarmHelper
                ReminderManager.cancelReminder(appContext, prospectId)

                if (_selectedProspectRecord.value?.id == prospectId && updatedRecordForSelection != null) {
                    _selectedProspectRecord.value = updatedRecordForSelection
                    Log.d(TAG, "Updated _selectedProspectRecord after clearing reminder.")
                }

                _snackbarMessage.emit("Reminder for $customerNameForFeedback has been cleared")
            }
        }
    }
    private fun migrateOldProspectRecords(jsonString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Define a class matching the old structure with string notes
                @kotlinx.serialization.Serializable
                data class OldProspectRecord(
                    val id: String = UUID.randomUUID().toString(),
                    val customerName: String,
                    val customerEmail: String?,
                    val customerPhone: String?,
                    val quoteSnapshot: Quote,
                    val externalPdfUriString: String?,
                    var status: ProspectStatus = ProspectStatus.PROSPECT,
                    var notes: List<String> = emptyList(),
                    var reminderDateTime: Long? = null,
                    var reminderNote: String? = null,
                    val dateCreated: Long = System.currentTimeMillis(),
                    var dateUpdated: Long = System.currentTimeMillis()
                )

                // Parse with old structure
                val oldRecords: List<OldProspectRecord> = json.decodeFromString(jsonString)

                // Convert to new structure
                val migratedRecords = oldRecords.map { oldRecord ->
                    ProspectRecord(
                        id = oldRecord.id,
                        customerName = oldRecord.customerName,
                        customerEmail = oldRecord.customerEmail,
                        customerPhone = oldRecord.customerPhone,
                        quoteSnapshot = oldRecord.quoteSnapshot,
                        externalPdfUriString = oldRecord.externalPdfUriString,
                        status = oldRecord.status,
                        // Convert string notes to Note objects
                        notes = oldRecord.notes.map { noteContent ->
                            val type = when {
                                noteContent.contains("Called") -> NoteType.CALL
                                noteContent.contains("Emailed") -> NoteType.EMAIL
                                noteContent.contains("Texted") -> NoteType.TEXT
                                else -> NoteType.MANUAL
                            }
                            Note(
                                content = noteContent,
                                timestamp = oldRecord.dateUpdated,
                                type = type
                            )
                        },
                        reminderDateTime = oldRecord.reminderDateTime,
                        reminderNote = oldRecord.reminderNote,
                        dateCreated = oldRecord.dateCreated,
                        dateUpdated = oldRecord.dateUpdated
                    )
                }

                Log.i(TAG, "[MIGRATE] Successfully migrated ${migratedRecords.size} records from old format to new Note structure")

                withContext(Dispatchers.Main) {
                    _prospectRecords.value = migratedRecords
                }

                // Save the migrated records in the new format
                saveProspectRecords()

            } catch (e: Exception) {
                Log.e(TAG, "[MIGRATE] Failed to migrate old prospect records", e)
                withContext(Dispatchers.Main) {
                    _prospectRecords.value = emptyList()
                }
            }
        }
    }

    private fun initializeDefaultCatalog() {
        viewModelScope.launch(Dispatchers.Main) {
            val defaultCatalog = Catalog(id = DEFAULT_CATALOG_ID, name = "Default Catalog")
            _catalogs.value = mapOf(DEFAULT_CATALOG_ID to defaultCatalog)
            _activeCatalogId.value = DEFAULT_CATALOG_ID
            _itemQuantities.value = emptyMap()
            _productMultiplierAssignments.value = emptyMap()
            Log.i(TAG, "Initialized with default catalog.")
            saveCatalogs()
        }
    }
    fun requestPhaseImageUpload(prospectId: String, phaseId: String) {
        viewModelScope.launch {
            _imageUploadEvent.emit(Pair("${prospectId}:${phaseId}", true))
        }
    }

    // Add this function to handle phase image upload
    fun savePhaseImage(prospectId: String, phaseId: String, imageUri: Uri) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()

            _prospectRecords.update { currentRecords ->
                currentRecords.map { record ->
                    if (record.id == prospectId) {
                        // Find existing phase image entry or create new one
                        val existingEntry = record.phaseImages.find { it.phaseId == phaseId }

                        val updatedPhaseImages = if (existingEntry != null) {
                            // Update existing entry
                            record.phaseImages.map {
                                if (it.phaseId == phaseId) {
                                    it.copy(imageUris = it.imageUris + Pair(imageUri.toString(), timestamp))
                                } else {
                                    it
                                }
                            }
                        } else {
                            // Create new entry
                            record.phaseImages + PhaseImage(
                                phaseId = phaseId,
                                imageUris = listOf(Pair(imageUri.toString(), timestamp))
                            )
                        }

                        record.copy(
                            phaseImages = updatedPhaseImages,
                            dateUpdated = timestamp
                        )
                    } else {
                        record
                    }
                }
            }

            saveProspectRecords()
            _snackbarMessage.emit("Phase image added successfully")
        }
    }


    // Add this function to remove phase image
    fun removePhaseImage(prospectId: String, phaseId: String, imageUri: String) {
        viewModelScope.launch {
            _prospectRecords.update { currentRecords ->
                currentRecords.map { record ->
                    if (record.id == prospectId) {
                        // Update the phase images
                        val updatedPhaseImages = record.phaseImages.map { phaseImage ->
                            if (phaseImage.phaseId == phaseId) {
                                phaseImage.copy(
                                    imageUris = phaseImage.imageUris.filterNot { it.first == imageUri }
                                )
                            } else {
                                phaseImage
                            }
                        }.filter { it.imageUris.isNotEmpty() } // Remove empty entries

                        record.copy(
                            phaseImages = updatedPhaseImages,
                            dateUpdated = System.currentTimeMillis()
                        )
                    } else {
                        record
                    }
                }
            }

            saveProspectRecords()
            _snackbarMessage.emit("Phase image removed")
        }
    }
}