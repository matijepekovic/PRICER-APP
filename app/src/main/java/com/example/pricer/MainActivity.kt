package com.example.pricer

// Standard Android/Activity Imports
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels

// Compose UI imports
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.* // Wildcard for Material 3 is okay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

// Lifecycle imports
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.ComponentActivity
// Your Project's imports
import com.example.pricer.data.model.*
import com.example.pricer.ui.dialogs.*
import com.example.pricer.ui.screens.CatalogScreen
import com.example.pricer.ui.screens.ProspectDetailScreen // Import for new screen
import com.example.pricer.ui.screens.ProspectsScreen
import com.example.pricer.ui.screens.QuotePreviewScreen
import com.example.pricer.ui.theme.PricerTheme
import com.example.pricer.viewmodel.MainViewModel
// Inside MainActivity class (Add this helper function)
import java.io.InputStream // Add at top if needed
import java.io.OutputStream // Add at top if needed
import java.io.File // Add at top if needed
import java.io.FileOutputStream // Add at top if needed
import android.content.Context // Add at top if needed
// Java Util imports
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.IOException

class MainActivity : ComponentActivity() {
    private fun copyUriToFile(context: Context, sourceUri: Uri, destinationFile: File): Boolean {
        Log.d("FileUtils", "Copying $sourceUri -> ${destinationFile.absolutePath}")
        var success = false
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream) // Use Kotlin's extension function for simplicity
                    success = true
                }
            } ?: Log.e("FileUtils", "InputStream was null for $sourceUri")
        } catch (e: Exception) {
            Log.e("FileUtils", "Error copying URI to File", e)
            destinationFile.delete() // Clean up partial file on error
            success = false
        }
        if(success) Log.i("FileUtils", "Copy successful to ${destinationFile.name}")
        return success}
    private val viewModel: MainViewModel by viewModels()
    private val instanceId = UUID.randomUUID().toString().substring(0, 5)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("PricerAppLifecycle", "MainActivity ($instanceId) onCreate. ViewModel hash: ${viewModel.hashCode()}. Intent: $intent, Action: ${intent?.action}, Data: ${intent?.data}, Flags: ${intent?.flags}")

        // Process initial intent IF it's a VIEW action AND has data
        val currentInitialIntent = intent
        if (currentInitialIntent != null &&
            currentInitialIntent.action == Intent.ACTION_VIEW &&
            currentInitialIntent.data != null
        ) {
            handleIncomingIntent(currentInitialIntent)
        }

        setContent {
            PricerTheme {
                val context = LocalContext.current
                val uiMode by viewModel.uiMode.collectAsStateWithLifecycle()
                val currentDialog by viewModel.currentDialog.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                // var catalogToConfirmImport by remember { mutableStateOf<Catalog?>(null) } // Removed for direct import

                // Collect Snackbar Messages
                LaunchedEffect(Unit) {
                    viewModel.snackbarMessage.collect { message ->
                        snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                    }
                }

                // PDF Directory Launcher
                val requestDirectoryLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree(),
                    onResult = { dirUri: Uri? ->
                        if (dirUri != null) {
                            Log.d("MainActivity", "Directory selected: $dirUri")

                            // --- *** TAKE PERSISTABLE PERMISSIONS *** ---
                            try {
                                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                context.contentResolver.takePersistableUriPermission(dirUri, takeFlags)
                                Log.i("MainActivity", "Persistable permissions taken for directory: $dirUri")
                            } catch (e: SecurityException) {
                                Log.e("MainActivity", "Failed to take persistable permissions for directory", e)
                                Toast.makeText(context,"Could not get long-term access to folder.", Toast.LENGTH_SHORT).show()
                                // Proceed without persisted permissions? Or stop here? Depends on requirements.
                                // For now, let's continue, but viewing/sharing might fail later if permission is lost.
                            }
                            val quote = viewModel.currentQuote.value
                            if (quote == null || quote.customerName.isBlank()) {
                                Toast.makeText(context, "Quote data or customer name missing.", Toast.LENGTH_LONG).show()
                                return@rememberLauncherForActivityResult
                            }
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val safeName = quote.customerName.replace(Regex("[^A-Za-z0-9]"), "_").take(30)
                            val fileName = "Quote_${safeName}_$timestamp.pdf"
                            val savedExternalPdfUri: Uri? = viewModel.generatePdfToUri(context, quote, dirUri, fileName)

                            if (savedExternalPdfUri != null) {
                                Toast.makeText(context, "PDF saved successfully!", Toast.LENGTH_SHORT).show()
                                viewModel.createOrUpdateProspectRecord(quote, savedExternalPdfUri.toString())
                            } else {
                                Toast.makeText(context, "Failed to save PDF. Prospect record not created.", Toast.LENGTH_LONG).show()
                            }
                        } else { Toast.makeText(context, "Save operation cancelled.", Toast.LENGTH_SHORT).show() }
                    }
                )
                fun launchPdfGeneration() { requestDirectoryLauncher.launch(null) }

                // Share Event Collector
                LaunchedEffect(Unit) {
                    viewModel.shareCatalogEvent.collect { shareableUri ->
                        Log.d("MainActivity", "Share Event URI: $shareableUri")
                        val catName = viewModel.activeCatalog.value?.name ?: "Pricer Catalog"
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_STREAM, shareableUri); type = "application/json"
                            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain")); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            putExtra(Intent.EXTRA_SUBJECT, "Pricer Catalog: $catName"); putExtra(Intent.EXTRA_TEXT, "Attached: '$catName'")
                        }
                        try { context.startActivity(Intent.createChooser(shareIntent, "Share '$catName' Via...")) }
                        catch (e: Exception) { Log.e("MainActivity", "Share Chooser fail", e); Toast.makeText(context, "No share app?", Toast.LENGTH_SHORT).show() }
                    }
                }

                val handleShareRequest = { catalogId: String -> viewModel.requestShareCatalog(catalogId) }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { scaffoldPadding ->
                    MainContent(
                        modifier = Modifier.padding(scaffoldPadding),
                        uiMode = uiMode,
                        viewModel = viewModel,
                        onNavigateToQuotePreview = { viewModel.showQuotePreview() },
                        onNavigateBack = { viewModel.showCatalogView() }, // This is for QuotePreview -> Catalog
                        onGeneratePdfClick = { launchPdfGeneration() }
                    )
                    HandleDialogs(
                        dialogState = currentDialog,
                        viewModel = viewModel,
                        onShareRequested = handleShareRequest
                    )
                    // Confirmation dialog for import removed for now to test direct import first
                } // End Scaffold Content
            } // End PricerTheme
        } // End setContent
    } // End onCreate

    // --- Corrected onNewIntent ---
    override fun onNewIntent(intent: Intent) { // Parameter MUST be 'Intent?'
        super.onNewIntent(intent)
        Log.i("PricerAppLifecycle", "MainActivity ($instanceId) onNewIntent. ViewModel hash: ${viewModel.hashCode()}. New Intent: $intent, Action: ${intent?.action}, Data: ${intent?.data}, Flags: ${intent?.flags}")
        setIntent(intent) // Update the activity's current intent with the new one

        // Process the new intent IF it's a VIEW action AND has data
        if (intent != null &&
            intent.action == Intent.ACTION_VIEW &&
            intent.data != null) {
            handleIncomingIntent(intent) // Pass the non-null intent
        } else {
            Log.w("PricerAppLifecycle", "MainActivity ($instanceId) onNewIntent - Received intent is null, has no data, or is not ACTION_VIEW.")
        }
    }

    // --- Helper function to Process Incoming Intents ---
    private fun handleIncomingIntent(intent: Intent) { // Expects non-null from callers
        Log.i("PricerAppLifecycle", "MainActivity ($instanceId) handleIncomingIntent. Action: ${intent.action}, Data: ${intent.data}")
        val uri: Uri = intent.data!! // Safe '!!' because intent.data is checked by the caller
        val mimeType = contentResolver.getType(uri)
        Log.d("PricerAppLifecycle", "Intent URI: $uri, MIME: $mimeType")

        // Basic check for potential JSON catalog files
        if (mimeType == "application/json" ||
            mimeType == "text/json" ||
            mimeType == "text/plain" ||
            mimeType == "application/octet-stream" ||
            uri.path?.endsWith(".json", ignoreCase = true) == true) {
            Log.i("PricerAppLifecycle", "Will attempt import from URI: $uri")
            readAndImportCatalog(uri)
        } else {
            Log.w("PricerAppLifecycle", "Ignoring intent. Unsupported MIME: $mimeType for URI: $uri")
            Toast.makeText(this, "Cannot open this file type with Pricer.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Helper Function to Read and Import Catalog from URI ---
    private fun readAndImportCatalog(uri: Uri) {
        Log.i("PricerAppLifecycle", "MainActivity ($instanceId) readAndImportCatalog. URI: $uri")
        var jsonString: String? = null
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                jsonString = inputStream.bufferedReader().use { it.readText() }
            } ?: throw IOException("Could not open InputStream for URI: $uri")

            if (jsonString.isNullOrBlank()) { throw IOException("File content is empty.") }
            Log.d("PricerAppLifecycle", "Read JSON (len: ${jsonString!!.length})") // Safe !!

            val parsedCatalog = try {
                Json { ignoreUnknownKeys = true }.decodeFromString<Catalog>(jsonString!!) // Safe !!
            } catch (e: Exception) {
                Log.e("PricerAppLifecycle", "JSON Parsing failed for imported file.", e)
                null
            }

            if (parsedCatalog != null) {
                Log.i("PricerAppLifecycle", "MainActivity ($instanceId) - Calling viewModel.importCatalog for '${parsedCatalog.name}'")
                viewModel.importCatalog(parsedCatalog) // ViewModel shows Snackbar feedback
            } else {
                Toast.makeText(this, "Failed to understand catalog file format.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("PricerAppLifecycle", "Error reading/importing catalog from URI: $uri", e)
            Toast.makeText(this, "Error importing catalog. Invalid file or permission issue?", Toast.LENGTH_LONG).show()
        }
    }

    // --- MainContent Helper Composable ---
    @Composable
    private fun MainContent(
        modifier: Modifier = Modifier,
        uiMode: UiMode,
        viewModel: MainViewModel,
        onNavigateToQuotePreview: () -> Unit,
        onNavigateBack: () -> Unit, // This is for QuotePreview -> Catalog
        onGeneratePdfClick: () -> Unit
    ) {
        Box(modifier = modifier) {
            when (uiMode) {
                UiMode.CATALOG -> CatalogScreen(
                    viewModel = viewModel,
                    onNavigateToQuotePreview = onNavigateToQuotePreview
                )
                UiMode.QUOTE_PREVIEW -> QuotePreviewScreen(
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBack, // Navigates to Catalog
                    onGeneratePdfClick = onGeneratePdfClick
                )
                UiMode.PROSPECTS -> ProspectsScreen(
                    viewModel = viewModel,
                    onNavigateBackToCatalog = { viewModel.showCatalogView() }, // Navigates to Catalog
                    onProspectClick = { prospectId ->
                        viewModel.showProspectDetail(prospectId)
                    }
                )
                UiMode.PROSPECT_DETAIL -> ProspectDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { viewModel.showProspectsScreen() } // Navigates back to Prospects LIST
                )
            }
        }
    }

    // --- HandleDialogs Helper Composable ---
    @Composable
    private fun HandleDialogs(dialogState: DialogState, viewModel: MainViewModel, onShareRequested: (catalogId: String) -> Unit) {
        val productToEdit by viewModel.productToEdit.collectAsStateWithLifecycle()
        val productForMultiplier by viewModel.productForMultiplierAssignment.collectAsStateWithLifecycle()
        val currentTaxRate by viewModel.taxRate.collectAsStateWithLifecycle()
        val catalogs by viewModel.catalogs.collectAsStateWithLifecycle()
        val activeCatalog by viewModel.activeCatalog.collectAsStateWithLifecycle()
        val assignedMultipliersState by viewModel.selectedMultipliersForAssignment.collectAsStateWithLifecycle()
        val companyName by viewModel.quoteCompanyName.collectAsStateWithLifecycle()
        val customMessage by viewModel.quoteCustomMessage.collectAsStateWithLifecycle()
        val globalDiscountRate by viewModel.globalDiscountRate.collectAsStateWithLifecycle()
        val multipliers = activeCatalog?.multipliers ?: emptyList()
        val selectedProspectId by viewModel.selectedProspectRecord.collectAsStateWithLifecycle()
        val noteToEdit by viewModel.noteToEdit.collectAsStateWithLifecycle()
        val selectedProspect by viewModel.selectedProspectRecord.collectAsStateWithLifecycle()
        when(dialogState){
            DialogState.NONE->{}
            DialogState.ADD_EDIT_PRODUCT->{AddEditProductDialog(productToEdit=productToEdit,onDismiss={viewModel.dismissDialog()},onConfirm={viewModel.addOrUpdateProduct(it)})}
            DialogState.MANAGE_MULTIPLIERS->{ManageMultipliersDialog(multipliers=multipliers,onDismiss={viewModel.dismissDialog()},onAddMultiplier={viewModel.addOrUpdateMultiplier(it)},onUpdateMultiplier={viewModel.addOrUpdateMultiplier(it)},onDeleteMultiplier={viewModel.deleteMultiplier(it)})}
            DialogState.ASSIGN_MULTIPLIER->{productForMultiplier?.let{p->val tqs=viewModel.itemQuantities.value[p.id]?:"0"; val tqi=tqs.toIntOrNull()?:0; AssignMultiplierDialog(product=p,totalQuantity=tqi,availableMultipliers=multipliers,initialAssignments=assignedMultipliersState,onDismiss={viewModel.dismissDialog()},onConfirm={assigns->viewModel.confirmMultiplierAssignment(p.id,assigns)})}?:LaunchedEffect(dialogState){Log.e("HandleDialogs","AssignMult No Product"); viewModel.dismissDialog()}}
            DialogState.SET_TAX->{SetTaxDialog(initialTaxRate=currentTaxRate,onDismiss={viewModel.dismissDialog()},onConfirm={viewModel.setTaxRate(it)})}
            DialogState.MANAGE_CATALOGS->{ManageCatalogsDialog(catalogs=catalogs,activeCatalogId=activeCatalog?.id?:"",onDismiss={viewModel.dismissDialog()},onSelectCatalog={viewModel.loadCatalog(it)},onAddCatalog={viewModel.createCatalog(it)},onRenameCatalog={id,name->viewModel.renameCatalog(id,name)},onDeleteCatalog={viewModel.deleteCatalog(it)},onShareCatalog=onShareRequested)}
            DialogState.ADD_NOTE -> {
                selectedProspect?.let { prospect ->
                    NoteDialog(
                        initialNote = null, // Null for add mode
                        onDismiss = { viewModel.dismissDialog() },
                        onConfirm = { content ->
                            viewModel.addNoteToProspect(prospect.id, content)
                            viewModel.dismissDialog()
                        }
                    )
                } ?: run {
                    LaunchedEffect(dialogState) {
                        Log.e("HandleDialogs", "ADD_NOTE dialog shown with no selected prospect")
                        viewModel.dismissDialog()
                    }
                }
            }
            DialogState.EDIT_NOTE -> {
                val prospect = selectedProspect
                val note = noteToEdit

                if (prospect != null && note != null) {
                    NoteDialog(
                        initialNote = note,
                        onDismiss = { viewModel.dismissDialog() },
                        onConfirm = { content ->
                            viewModel.editNote(prospect.id, note, content)
                            viewModel.dismissDialog()
                        }
                    )
                } else {
                    LaunchedEffect(dialogState) {
                        Log.e("HandleDialogs", "EDIT_NOTE dialog shown with missing data")
                        viewModel.dismissDialog()
                    }
                }
            }
            DialogState.CUSTOMER_DETAILS->{CustomerDetailsDialog(onDismiss={viewModel.dismissDialog();viewModel.showCatalogView()},onConfirm={viewModel.proceedToQuotePreview(it)})}
            DialogState.QUOTE_DETAILS->{QuoteDetailsDialog(initialCompanyName=companyName,initialCustomMessage=customMessage,onDismiss={viewModel.dismissDialog()},onConfirm={comp,msg->viewModel.updateQuoteDetails(comp,msg)})}
            DialogState.SET_DISCOUNT->{SetDiscountDialog(initialDiscountRate=globalDiscountRate,onDismiss={viewModel.dismissDialog()},onConfirm={viewModel.setGlobalDiscount(it)})}
            DialogState.ADD_NOTE->{
                selectedProspectId?.let { prospectRecord ->
                    AddNoteDialog(
                        onDismiss = { viewModel.dismissDialog() },
                        onConfirm = { note ->
                            viewModel.addNoteToProspect(prospectRecord.id, note)
                            viewModel.dismissDialog()
                        }
                    )
                } ?: run {
                    // If somehow we got here without a selected prospect, dismiss the dialog
                    LaunchedEffect(dialogState) {
                        Log.e("HandleDialogs", "ADD_NOTE dialog shown with no selected prospect")
                        viewModel.dismissDialog()
                    }
                }
            }
        }
    }

} // End MainActivity Class