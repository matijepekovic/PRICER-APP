package com.example.pricer

// Standard Android/Activity Imports
import kotlinx.serialization.decodeFromString
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.icons.filled.FileDownload
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import android.provider.Settings
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.launch
// Compose UI imports
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.serialization.decodeFromString
import com.example.pricer.data.model.Catalog
import com.example.pricer.ui.screens.SubcontractorsScreen

import com.example.pricer.ui.dialogs.AssignSubcontractorDialog

// Lifecycle imports
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.ComponentActivity
// Your Project's imports
import com.example.pricer.data.model.*
import com.example.pricer.ui.dialogs.*
import com.example.pricer.ui.screens.CatalogScreen
import com.example.pricer.ui.screens.ProspectDetailScreen
import com.example.pricer.ui.screens.ProspectsScreen
import com.example.pricer.ui.screens.QuotePreviewScreen
import com.example.pricer.ui.theme.PricerTheme
import com.example.pricer.viewmodel.MainViewModel
// Inside MainActivity class (Add this helper function)
import java.io.InputStream
import java.io.OutputStream
import java.io.File
import java.io.FileOutputStream
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
// Java Util imports
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.IOException
import android.Manifest
import android.os.Environment
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import com.example.pricer.ui.screens.ProspectsTabsContainer
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary

class MainActivity : ComponentActivity() {
    // Keep state at class level

    private var catalogToConfirmImport by mutableStateOf<Catalog?>(null)
    private var catalogNameConflict by mutableStateOf<Pair<Catalog, String>?>(null)
    private var isLoading by mutableStateOf(false)
    private var loadingMessage by mutableStateOf<String?>(null)
    private var showPermissionDialog by mutableStateOf(false)
    private var showImageSourceDialog by mutableStateOf(false)

    // Add these variables for image upload functionality
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private var currentImageUploadInfo: Pair<String, Boolean>? = null // Pair<ProspectId, IsBeforeImage>
    private var currentPhotoUri: Uri? = null

    private companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 100
        private const val STORAGE_PERMISSION_CODE = 101
        private const val FILE_PICKER_CODE = 123
        private const val CAMERA_PERMISSION_CODE = 102
    }

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
        return success
    }

    private val viewModel: MainViewModel by viewModels()
    private val instanceId = UUID.randomUUID().toString().substring(0, 5)
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var catalogImportLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>

    private val manageAllFilesLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                Environment.isExternalStorageManager()
            ) {
                launchCatalogFilePicker()
            } else {
                Toast.makeText(this,
                    "Please enable All-files access in Settings → Special app access → All files access",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    // Creates a temporary file for camera photos
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(cacheDir, "camera_photos")
        storageDir.mkdirs()
        return File(storageDir, "IMG_${timeStamp}.jpg")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission launcher for storage
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, now launch file picker
                launchCatalogFilePicker()
            } else {
                // Permission denied
                Toast.makeText(this, "Storage permission is required to import catalogs", Toast.LENGTH_LONG).show()
                showPermissionDialog = true
            }
        }

        // Initialize permission launcher for camera
        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Camera permission granted, launch camera
                launchCamera()
            } else {
                // Camera permission denied
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_LONG).show()
            }
        }

        // Initialize gallery launcher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                Log.d("MainActivity", "Selected image from gallery: $uri")

                // Take persistable permissions
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                    Log.d("MainActivity", "Took persistent permissions for gallery image URI")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to take permissions for gallery image: ${e.message}")
                }

                // Process selected image
                currentImageUploadInfo?.let { (prospectId, isBeforeImage) ->
                    viewModel.saveImageToProspect(prospectId, uri, isBeforeImage)
                }
            } else {
                Log.d("MainActivity", "No image selected from gallery")
            }

            // Reset state
            showImageSourceDialog = false
            currentImageUploadInfo = null
        }

        // Initialize camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentPhotoUri != null) {
                Log.d("MainActivity", "Photo captured successfully: $currentPhotoUri")

                // Process captured image
                currentImageUploadInfo?.let { (prospectId, isBeforeImage) ->
                    viewModel.saveImageToProspect(prospectId, currentPhotoUri!!, isBeforeImage)
                }
            } else {
                Log.d("MainActivity", "Failed to capture photo or cancelled")
            }

            // Reset state
            showImageSourceDialog = false
            currentImageUploadInfo = null
            currentPhotoUri = null
        }

        // Initialize notification permission launcher
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            Log.d("MainActivity", "Notification permission: ${if (isGranted) "granted" else "denied"}")
        }

        // Initialize catalog import launcher
        catalogImportLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                Log.d("MainActivity", "Selected file URI: $uri")

                // Take persistable permissions
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                    Log.d("MainActivity", "Took persistent permissions for URI")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to take permissions", e)
                    // Continue anyway - might still work
                }

                // Show loading state
                isLoading = true
                loadingMessage = "Reading catalog file..."

                // Process the file in a background thread
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // Read file content
                        val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.bufferedReader().readText()
                        } ?: throw IOException("Unable to read file content")

                        if (jsonString.isBlank()) {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                loadingMessage = null
                                Toast.makeText(this@MainActivity, "File is empty", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }

                        // Parse catalog
                        val catalog = try {
                            Json { ignoreUnknownKeys = true }.decodeFromString<Catalog>(jsonString)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "JSON parsing failed", e)
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                loadingMessage = null
                                Toast.makeText(this@MainActivity, "Invalid catalog format", Toast.LENGTH_LONG).show()
                            }
                            return@launch
                        }

                        // Validate catalog
                        if (catalog.name.isBlank()) {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                loadingMessage = null
                                Toast.makeText(this@MainActivity, "Invalid catalog: missing name", Toast.LENGTH_LONG).show()
                            }
                            return@launch
                        }

                        // Show confirmation UI on main thread
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            loadingMessage = null

                            // Check for name conflict
                            val nameExists = viewModel.catalogNameExists(catalog.name)
                            if (nameExists) {
                                catalogNameConflict = Pair(catalog, catalog.name)
                            } else {
                                catalogToConfirmImport = catalog
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error processing catalog file", e)
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            loadingMessage = null
                            Toast.makeText(this@MainActivity, "Error importing catalog: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        Log.i("PricerAppLifecycle", "MainActivity ($instanceId) onCreate. ViewModel hash: ${viewModel.hashCode()}. Intent: $intent, Action: ${intent?.action}, Data: ${intent?.data}, Flags: ${intent?.flags}")

        // Request notification permissions for Android 13+
        checkNotificationPermission()

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

                            // Take persistable permissions
                            try {
                                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                context.contentResolver.takePersistableUriPermission(dirUri, takeFlags)
                                Log.i("MainActivity", "Persistable permissions taken for directory: $dirUri")
                            } catch (e: SecurityException) {
                                Log.e("MainActivity", "Failed to take persistable permissions for directory", e)
                                Toast.makeText(context,"Could not get long-term access to folder.", Toast.LENGTH_SHORT).show()
                            }

                            val quote = viewModel.currentQuote.value
                            if (quote == null || quote.customerName.isBlank()) {
                                Toast.makeText(context, "Quote data or customer name missing.", Toast.LENGTH_LONG).show()
                                return@rememberLauncherForActivityResult
                            }

                            isLoading = true
                            loadingMessage = "Generating PDF..."

                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val safeName = quote.customerName.replace(Regex("[^A-Za-z0-9]"), "_").take(30)
                            val fileName = "Quote_${safeName}_$timestamp.pdf"

                            val savedExternalPdfUri: Uri? = viewModel.generatePdfToUri(context, quote, dirUri, fileName)

                            isLoading = false
                            loadingMessage = null

                            if (savedExternalPdfUri != null) {
                                Toast.makeText(context, "PDF saved successfully!", Toast.LENGTH_SHORT).show()
                                viewModel.createOrUpdateProspectRecord(quote, savedExternalPdfUri.toString())
                            } else {
                                Toast.makeText(context, "Failed to save PDF. Prospect record not created.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Save operation cancelled.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                fun launchPdfGeneration() { requestDirectoryLauncher.launch(null) }

                // Share Event Collector
                LaunchedEffect(Unit) {
                    viewModel.shareCatalogEvent.collect { shareableUri ->
                        Log.d("MainActivity", "Share Event URI: $shareableUri")
                        val catName = viewModel.activeCatalog.value?.name ?: "Pricer Catalog"
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, shareableUri)
                            type = "application/json"
                            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain"))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            putExtra(Intent.EXTRA_SUBJECT, "Pricer Catalog: $catName")
                            putExtra(Intent.EXTRA_TEXT, "Attached: '$catName'")
                        }
                        try {
                            context.startActivity(Intent.createChooser(shareIntent, "Share '$catName' Via..."))
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Share Chooser fail", e)
                            Toast.makeText(context, "No share app?", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Image Upload Event Collector
                LaunchedEffect(Unit) {
                    viewModel.imageUploadEvent.collect { (prospectId, isBeforeImage) ->
                        Log.d("MainActivity", "Image upload requested for prospect $prospectId, before image: $isBeforeImage")
                        currentImageUploadInfo = Pair(prospectId, isBeforeImage)
                        showImageSourceDialog = true
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
                        onNavigateBack = { viewModel.showCatalogView() },
                        onGeneratePdfClick = { launchPdfGeneration() }
                    )

                    HandleDialogs(
                        dialogState = currentDialog,
                        viewModel = viewModel,
                        onShareRequested = handleShareRequest
                    )
                }

                // Permission Dialog
                if (showPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = { showPermissionDialog = false },
                        title = { Text("Permission Required") },
                        text = {
                            Text("Storage permission is required to import catalogs. Please go to app settings to enable it.")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showPermissionDialog = false
                                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", packageName, null)
                                    }
                                    startActivity(intent)
                                }
                            ) {
                                Text("Open Settings")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPermissionDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Image Source Dialog
                if (showImageSourceDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showImageSourceDialog = false
                            currentImageUploadInfo = null
                        },
                        title = { Text("Choose Image Source") },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    onClick = { checkCameraPermission() },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text("Take Photo with Camera")
                                }

                                Button(
                                    onClick = {
                                        galleryLauncher.launch("image/*")
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text("Choose from Gallery")
                                }
                            }
                        },
                        confirmButton = { },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showImageSourceDialog = false
                                    currentImageUploadInfo = null
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Catalog Import Confirmation Dialog
                if (catalogToConfirmImport != null) {
                    AlertDialog(
                        onDismissRequest = { catalogToConfirmImport = null },
                        title = { Text("Import Catalog") },
                        text = {
                            Text("Do you want to import catalog '${catalogToConfirmImport?.name}' with " +
                                    "${catalogToConfirmImport?.products?.size ?: 0} products?")
                        },
                        confirmButton = {
                            Button(onClick = {
                                val catalog = catalogToConfirmImport
                                if (catalog != null) {
                                    isLoading = true
                                    loadingMessage = "Importing catalog..."

                                    // Create a fresh ID for the catalog
                                    val catalogToImport = catalog.copy(
                                        id = UUID.randomUUID().toString()
                                    )

                                    viewModel.importCatalog(catalogToImport)
                                    isLoading = false
                                    loadingMessage = null
                                }
                                catalogToConfirmImport = null
                            }) {
                                Text("Import")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { catalogToConfirmImport = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Name Conflict Dialog
                if (catalogNameConflict != null) {
                    val (catalog, conflictingName) = catalogNameConflict!!
                    var newName by remember { mutableStateOf(conflictingName) }
                    var errorMessage by remember { mutableStateOf<String?>(null) }

                    AlertDialog(
                        onDismissRequest = { catalogNameConflict = null },
                        title = { Text("Name Conflict") },
                        text = {
                            Column {
                                Text("A catalog named '$conflictingName' already exists. Please enter a different name:")
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = newName,
                                    onValueChange = {
                                        newName = it
                                        errorMessage = null
                                    },
                                    label = { Text("New Catalog Name") },
                                    isError = errorMessage != null,
                                    supportingText = errorMessage?.let { { Text(it) } },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (newName.trim().isBlank()) {
                                    errorMessage = "Name cannot be empty"
                                } else {
                                    isLoading = true
                                    loadingMessage = "Importing catalog..."

                                    // Create a fresh ID and update the name
                                    val catalogToImport = catalog.copy(
                                        id = UUID.randomUUID().toString(),
                                        name = newName.trim()
                                    )

                                    viewModel.importCatalog(catalogToImport)
                                    isLoading = false
                                    loadingMessage = null
                                    catalogNameConflict = null
                                }
                            }) {
                                Text("Import")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { catalogNameConflict = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Loading Overlay
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.padding(16.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(loadingMessage ?: "Loading...")
                            }
                        }
                    }
                }
            }
        }

        // Handle notifications intent if necessary
        if (intent.getBooleanExtra("open_prospect_detail", false)) {
            val prospectId = intent.getStringExtra("prospect_id")
            if (prospectId != null) {
                Log.i("MainActivity", "Opening prospect detail from notification: $prospectId")
                viewModel.showProspectDetail(prospectId)
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            val photoFile = createImageFile()
            val photoURI = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            currentPhotoUri = photoURI
            cameraLauncher.launch(photoURI)
        } catch (ex: IOException) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", "Error creating image file", ex)
            showImageSourceDialog = false
            currentImageUploadInfo = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.i("PricerAppLifecycle", "MainActivity ($instanceId) onNewIntent. ViewModel hash: ${viewModel.hashCode()}. New Intent: $intent, Action: ${intent?.action}, Data: ${intent?.data}, Flags: ${intent?.flags}")
        setIntent(intent) // Update the activity's current intent with the new one

        // Handle notification intents
        if (intent.getBooleanExtra("open_prospect_detail", false)) {
            val prospectId = intent.getStringExtra("prospect_id")
            if (prospectId != null) {
                Log.i("MainActivity", "Opening prospect detail from notification (onNewIntent): $prospectId")
                viewModel.showProspectDetail(prospectId)
            }
        }

        // Process file open intents
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            handleIncomingIntent(intent)
        } else {
            Log.w("PricerAppLifecycle", "MainActivity ($instanceId) onNewIntent - Received intent is null, has no data, or is not ACTION_VIEW.")
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if permission is already granted
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Launch permission request
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent) {
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

    private fun readAndImportCatalog(uri: Uri) {
        Log.i("PricerAppLifecycle", "MainActivity ($instanceId) readAndImportCatalog. URI: $uri")
        var jsonString: String? = null
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                jsonString = inputStream.bufferedReader().use { it.readText() }
            } ?: throw IOException("Could not open InputStream for URI: $uri")

            if (jsonString.isNullOrBlank()) { throw IOException("File content is empty.") }
            Log.d("PricerAppLifecycle", "Read JSON (len: ${jsonString!!.length})")

            val parsedCatalog = try {
                Json { ignoreUnknownKeys = true }.decodeFromString<Catalog>(jsonString!!)
            } catch (e: Exception) {
                Log.e("PricerAppLifecycle", "JSON Parsing failed for imported file.", e)
                null
            }

            if (parsedCatalog != null) {
                // Check for name conflict
                val existingCatalogs = viewModel.catalogs.value
                val nameExists = existingCatalogs.values.any {
                    it.name.equals(parsedCatalog.name, ignoreCase = true)
                }

                if (nameExists) {
                    // Show name conflict dialog
                    catalogNameConflict = Pair(parsedCatalog, parsedCatalog.name)
                } else {
                    // Show confirmation dialog
                    catalogToConfirmImport = parsedCatalog
                }
            } else {
                Toast.makeText(this, "Failed to understand catalog file format.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("PricerAppLifecycle", "Error reading/importing catalog from URI: $uri", e)
            Toast.makeText(this, "Error importing catalog. Invalid file or permission issue?", Toast.LENGTH_LONG).show()
        }
    }

    @Composable
    private fun MainContent(
        modifier: Modifier = Modifier,
        uiMode: UiMode,
        viewModel: MainViewModel,
        onNavigateToQuotePreview: () -> Unit,
        onNavigateBack: () -> Unit,
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
                    onNavigateBack = onNavigateBack,
                    onGeneratePdfClick = onGeneratePdfClick
                )
                UiMode.CONTACTS -> ProspectsTabsContainer(
                    viewModel = viewModel,
                    onNavigateBackToCatalog = { viewModel.showCatalogView() },
                    onRecordClick = { recordId ->
                        viewModel.showProspectDetail(recordId)
                    }
                )
                UiMode.PROSPECTS -> {
                    // Legacy route, redirect to CONTACTS mode
                    LaunchedEffect(Unit) {
                        viewModel.showContactsScreen()
                    }
                }
                UiMode.PROSPECT_DETAIL -> ProspectDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { viewModel.showContactsScreen() }
                )
                UiMode.SUBCONTRACTORS -> SubcontractorsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { viewModel.showCatalogView() }
                )
            }
        }
    }

    @Composable
    private fun HandleDialogs(
        dialogState: DialogState,
        viewModel: MainViewModel,
        onShareRequested: (catalogId: String) -> Unit
    ) {
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

        when(dialogState) {
            DialogState.NONE -> {}
            DialogState.EDIT_TASK -> {
                val taskToEdit = viewModel.taskToEdit.collectAsStateWithLifecycle().value
                val selectedProspect = viewModel.selectedProspectRecord.collectAsStateWithLifecycle().value

                if (taskToEdit != null && selectedProspect != null) {
                    EditTaskDialog(
                        task = taskToEdit,
                        onDismiss = { viewModel.dismissDialog() },
                        onSave = { updatedTask ->
                            viewModel.updateTask(selectedProspect.id, updatedTask)
                            viewModel.dismissDialog()
                        }
                    )
                } else {
                    // If either value is null, dismiss the dialog
                    LaunchedEffect(Unit) {
                        viewModel.dismissDialog()
                    }
                }
            }

            DialogState.MANAGE_PHASES -> {
                val phases by viewModel.globalPhases.collectAsStateWithLifecycle()

                ManagePhasesDialog(
                    phases = phases,
                    onDismiss = { viewModel.dismissDialog() },
                    onSavePhases = { viewModel.saveGlobalPhases(it) },
                    onEditPhase = { viewModel.showEditPhaseDialog(it) },
                    onDeletePhase = { viewModel.deletePhase(it) }
                )
            }
            DialogState.ADD_PHASE -> {
                AddEditPhaseDialog(
                    phase = null,
                    onDismiss = { viewModel.dismissDialog() },
                    onSave = {
                        viewModel.addPhase(it)
                        viewModel.dismissDialog()
                    }
                )
            }

            DialogState.EDIT_PHASE -> {
                val phaseToEdit by viewModel.phaseToEdit.collectAsStateWithLifecycle()
                if (phaseToEdit != null) {
                    AddEditPhaseDialog(
                        phase = phaseToEdit,
                        onDismiss = { viewModel.dismissDialog() },
                        onSave = {
                            viewModel.updatePhase(it)
                            viewModel.dismissDialog()
                        }
                    )
                }
            }

            DialogState.ADD_TASK -> {
                val taskPhaseId = viewModel.taskPhaseId.collectAsStateWithLifecycle().value
                val selectedProspect = viewModel.selectedProspectRecord.collectAsStateWithLifecycle().value

                if (taskPhaseId != null && selectedProspect != null) {
                    // Log to verify values are correct
                    Log.d("MainActivity", "Showing AddTaskDialog with phaseId: $taskPhaseId for prospect: ${selectedProspect.id}")

                    AddTaskDialog(
                        phaseId = taskPhaseId,
                        onDismiss = { viewModel.dismissDialog() },
                        onAddTask = { task ->
                            // Log the task before adding
                            Log.d("MainActivity", "Adding task: ${task.title} with phaseId: ${task.phaseId} to prospect: ${selectedProspect.id}")
                            viewModel.addTask(selectedProspect.id, task)
                            viewModel.dismissDialog()
                        }
                    )
                } else {
                    // Log the issue
                    Log.e("MainActivity", "Cannot show AddTaskDialog: taskPhaseId=$taskPhaseId, selectedProspect=${selectedProspect?.id}")

                    // If either value is null, dismiss the dialog
                    LaunchedEffect(Unit) {
                        viewModel.dismissDialog()
                    }
                }
            }
            DialogState.ASSIGN_SUBCONTRACTOR -> {
                val prospect = selectedProspect
                if (prospect != null) {
                    val subcontractors by viewModel.subcontractors.collectAsStateWithLifecycle()
                    val phases by viewModel.globalPhases.collectAsStateWithLifecycle()

                    AssignSubcontractorDialog(
                        subcontractors = subcontractors,
                        phases = phases,
                        currentAssignments = prospect.subcontractorAssignments,
                        onDismiss = { viewModel.dismissDialog() },
                        onSave = {
                            viewModel.updateSubcontractorAssignments(prospect.id, it)
                            viewModel.dismissDialog()
                        }
                    )
                }
            }

            DialogState.ADD_EDIT_PRODUCT -> {
                AddEditProductDialog(
                    productToEdit = productToEdit,
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { viewModel.addOrUpdateProduct(it) }
                )
            }

            DialogState.MANAGE_MULTIPLIERS -> {
                ManageMultipliersDialog(
                    multipliers = multipliers,
                    onDismiss = { viewModel.dismissDialog() },
                    onAddMultiplier = { viewModel.addOrUpdateMultiplier(it) },
                    onUpdateMultiplier = { viewModel.addOrUpdateMultiplier(it) },
                    onDeleteMultiplier = { viewModel.deleteMultiplier(it) }
                )
            }

            DialogState.ASSIGN_MULTIPLIER -> {
                productForMultiplier?.let { p ->
                    val tqs = viewModel.itemQuantities.value[p.id] ?: "0"
                    val tqi = tqs.toIntOrNull() ?: 0

                    AssignMultiplierDialog(
                        product = p,
                        totalQuantity = tqi,
                        availableMultipliers = multipliers,
                        initialAssignments = assignedMultipliersState,
                        onDismiss = { viewModel.dismissDialog() },
                        onConfirm = { assigns ->
                            viewModel.confirmMultiplierAssignment(p.id, assigns)
                        }
                    )
                } ?: LaunchedEffect(dialogState) {
                    Log.e("HandleDialogs", "AssignMult No Product")
                    viewModel.dismissDialog()
                }
            }

            DialogState.SET_TAX -> {
                SetTaxDialog(
                    initialTaxRate = currentTaxRate,
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { viewModel.setTaxRate(it) }
                )
            }
            DialogState.ADD_VOUCHER -> {
                AddVoucherDialog(
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { voucherProduct -> viewModel.addVoucherToQuote(voucherProduct) }
                )
            }
            DialogState.MANAGE_CATALOGS -> {
                ManageCatalogsDialog(
                    catalogs = catalogs,
                    activeCatalogId = activeCatalog?.id ?: "",
                    onDismiss = { viewModel.dismissDialog() },
                    onSelectCatalog = { viewModel.loadCatalog(it) },
                    onAddCatalog = { viewModel.createCatalog(it) },
                    onRenameCatalog = { id, name -> viewModel.renameCatalog(id, name) },
                    onUpdateCompanyName = { id, companyName -> viewModel.updateCatalogCompanyName(id, companyName) }, // Add new callback
                    onDeleteCatalog = { viewModel.deleteCatalog(it) },
                    onShareCatalog = onShareRequested
                )
            }
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

            DialogState.SET_REMINDER -> {
                selectedProspect?.let { prospect ->
                    SetReminderDialog(
                        initialReminderDateTime = prospect.reminderDateTime,
                        initialReminderNote = prospect.reminderNote,
                        onDismiss = { viewModel.dismissDialog() },
                        onConfirm = { reminderDateTime, reminderNote ->
                            viewModel.setProspectReminder(
                                prospectId = prospect.id,
                                reminderDateTime = reminderDateTime,
                                reminderNote = reminderNote
                            )
                            viewModel.dismissDialog()
                        },
                        onClear = {
                            viewModel.clearProspectReminder(prospect.id)
                            viewModel.dismissDialog()
                        }
                    )
                } ?: run {
                    LaunchedEffect(dialogState) {
                        Log.e("HandleDialogs", "SET_REMINDER dialog shown with no selected prospect")
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

            DialogState.CUSTOMER_DETAILS -> {
                CustomerDetailsDialog(
                    onDismiss = {
                        viewModel.dismissDialog()
                        viewModel.showCatalogView()
                    },
                    onConfirm = { viewModel.proceedToQuotePreview(it) }
                )
            }

            DialogState.QUOTE_DETAILS -> {
                QuoteDetailsDialog(
                    initialCompanyName = companyName,
                    initialCustomMessage = customMessage,
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { comp, msg -> viewModel.updateQuoteDetails(comp, msg) }
                )
            }
            DialogState.ADD_CUSTOM_ITEM -> {
                AddCustomItemDialog(
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { product, quantity -> viewModel.addCustomItemToQuote(product, quantity) }
                )
            }
            DialogState.SET_DISCOUNT -> {
                SetDiscountDialog(
                    initialDiscountRate = globalDiscountRate,
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = {
                        viewModel.setGlobalDiscount(it)
                        viewModel.dismissDialog()
                        viewModel.showQuotePreview()
                    }
                )
            }
        }
    }
    fun openCatalogImport() {
        // For Android 11+ (API 30+), we need to request MANAGE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // Need to request MANAGE_EXTERNAL_STORAGE permission
                try {
                    Log.d("MainActivity", "Requesting MANAGE_EXTERNAL_STORAGE permission")
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    manageAllFilesLauncher.launch(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error launching settings for MANAGE_EXTERNAL_STORAGE", e)
                    Toast.makeText(this, "Unable to request all files access. Please enable it manually in Settings.", Toast.LENGTH_LONG).show()
                }
                return
            }
            // If we have MANAGE_EXTERNAL_STORAGE, proceed with file picker
            launchCatalogFilePicker()
        }
        // For Android 10 and below, we use READ_EXTERNAL_STORAGE permission
        else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                // We already have permission, launch file picker
                launchCatalogFilePicker()
            }
        }
    }

    private fun launchCatalogFilePicker() {
        try {
            Log.d("MainActivity", "Launching catalogImportLauncher")
            // Use the modern launcher approach with MIME types
            catalogImportLauncher.launch(arrayOf("application/json", "text/plain"))
        } catch (e: Exception) {
            Log.e("MainActivity", "Error launching catalogImportLauncher", e)

            // Fallback to traditional approach
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                        "application/json",
                        "text/plain"
                    ))
                }
                startActivityForResult(intent, FILE_PICKER_CODE)
            } catch (e2: Exception) {
                Log.e("MainActivity", "Error with fallback file picker too", e2)
                Toast.makeText(this, "Cannot open file picker: ${e2.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    Toast.makeText(this, "Storage permission granted, you can now import catalogs", Toast.LENGTH_SHORT).show()
                    launchCatalogFilePicker()
                } else {
                    // Permission denied
                    Toast.makeText(this, "Storage permission is required to import catalogs", Toast.LENGTH_LONG).show()
                    showPermissionDialog = true
                }
            }
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Camera permission granted
                    launchCamera()
                } else {
                    // Camera permission denied
                    Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_LONG).show()
                    showImageSourceDialog = false
                    currentImageUploadInfo = null
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if ((requestCode == FILE_PICKER_CODE || requestCode == 123) && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                Log.d("MainActivity", "Selected file URI: $uri")

                // Take persistable permissions
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to take permissions: ${e.message}")
                    // Continue anyway - might still work
                }

                // Process file in background thread
                isLoading = true
                loadingMessage = "Reading catalog file..."

                Thread {
                    try {
                        // Read file content
                        val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.readBytes().toString(Charsets.UTF_8)
                        }

                        if (jsonString.isNullOrBlank()) {
                            runOnUiThread {
                                isLoading = false
                                loadingMessage = null
                                Toast.makeText(this, "File is empty", Toast.LENGTH_SHORT).show()
                            }
                            return@Thread
                        }

                        // Parse catalog
                        val catalog = try {
                            Json { ignoreUnknownKeys = true }.decodeFromString<Catalog>(jsonString)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "JSON parsing failed", e)
                            runOnUiThread {
                                isLoading = false
                                loadingMessage = null
                                Toast.makeText(this, "Invalid catalog format", Toast.LENGTH_LONG).show()
                            }
                            return@Thread
                        }

                        // Check for name conflict and show confirmation
                        runOnUiThread {
                            isLoading = false
                            loadingMessage = null

                            val nameExists = viewModel.catalogNameExists(catalog.name)
                            if (nameExists) {
                                catalogNameConflict = Pair(catalog, catalog.name)
                            } else {
                                catalogToConfirmImport = catalog
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error processing file", e)
                        runOnUiThread {
                            isLoading = false
                            loadingMessage = null
                            Toast.makeText(this, "Error importing catalog: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
            }
        }
    }
}