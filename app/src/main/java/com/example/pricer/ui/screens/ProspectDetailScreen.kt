package com.example.pricer.ui.screens
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pricer.data.model.*
import com.example.pricer.ui.components.PhaseViewer
import com.example.pricer.viewmodel.MainViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProspectDetailScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val globalPhases by viewModel.globalPhases.collectAsStateWithLifecycle()
    val selectedPhaseIndex by viewModel.selectedPhaseIndex.collectAsStateWithLifecycle()
    val subcontractors by viewModel.subcontractors.collectAsStateWithLifecycle()
    val selectedProspect by viewModel.selectedProspectRecord.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tasksKey = remember(selectedProspect?.tasks) { selectedProspect?.tasks?.hashCode() ?: 0 }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()) }
    val fileProviderAuthority = "${context.packageName}.fileprovider"
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        selectedProspect?.customerName ?: "Prospect Details",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { /* Empty actions section - removed buttons */ }
            )
        }
    ) { paddingValues ->
        if (selectedProspect == null) {
            // Loading state
            Box(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Loading...", Modifier.padding(top = 8.dp))
                }
            }
        } else {
            // Content when prospect data is available
            val prospect = selectedProspect!!

            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // --- Customer Details Section ---
                SectionTitle("Customer Details")
                DetailRow(label = "Name:", value = prospect.customerName, isBoldValue = true)

                // Email row with action
                prospect.customerEmail?.takeIf { it.isNotBlank() }?.let { email ->
                    DetailRowWithIcon(
                        label = "Email:",
                        value = email,
                        icon = Icons.Default.Email
                    ) {
                        launchEmailIntent(context, email, viewModel, prospect.id)
                    }
                }

                // Phone rows with actions
                prospect.customerPhone?.takeIf { it.isNotBlank() }?.let { phone ->
                    DetailRowWithIcon(
                        label = "Phone:",
                        value = phone,
                        icon = Icons.Default.Phone
                    ) {
                        launchDialerIntent(context, phone, viewModel, prospect.id)
                    }

                    DetailRowWithIcon(
                        label = "Text:",
                        value = phone,
                        icon = Icons.Default.Sms
                    ) {
                        launchSmsIntent(context, phone, viewModel, prospect.id)
                    }
                }

                DetailRow(
                    label = "Status:",
                    value = prospect.status.name.lowercase().replaceFirstChar { it.titlecase() }
                )
                DetailRow(label = "Created:", value = dateFormat.format(Date(prospect.dateCreated)))
                DetailRow(label = "Updated:", value = dateFormat.format(Date(prospect.dateUpdated)))

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // --- Associated Quote Section ---
                SectionTitle("Associated Quote")
                if (prospect.externalPdfUriString.isNullOrBlank()) {
                    // If no PDF exists
                    Text(
                        "No PDF quote available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // If PDF exists, show a button to open it
                    OutlinedButton(
                        onClick = {
                            try {
                                val pdfUri = Uri.parse(prospect.externalPdfUriString)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(pdfUri, "application/pdf")
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("View Quote PDF")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // --- Notes Section ---
                SectionTitle("Notes")
                if (prospect.notes.isEmpty()) {
                    Text(
                        "No notes added yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        prospect.notes.forEach { note ->
                            NoteItem(
                                note = note,
                                onEditClick = {
                                    viewModel.showDialog(DialogState.EDIT_NOTE, note)
                                }
                            )
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.showDialog(DialogState.ADD_NOTE) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddComment, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Add Note")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // --- Reminder Section ---
                SectionTitle("Reminder")

                val hasReminder = prospect.reminderDateTime != null
                val reminderDate = prospect.reminderDateTime?.let {
                    SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                        .format(Date(it))
                }

                // Display existing reminder if there is one
                if (hasReminder) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reminder set for:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = reminderDate ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            prospect.reminderNote?.takeIf { it.isNotBlank() }?.let { note ->
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = {
                        viewModel.showDialog(DialogState.SET_REMINDER)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (hasReminder) Icons.Default.Edit else Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(if (hasReminder) "Edit Reminder" else "Set Reminder")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))


                // --- Subcontractors Section ---
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("Subcontractors")

// Show assigned subcontractors or empty state
                if (prospect.subcontractorAssignments.isEmpty()) {
                    // Empty state
                    Text(
                        "No subcontractors assigned to this project.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // List of assigned subcontractors
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        prospect.subcontractorAssignments.forEach { assignment ->
                            // Find the subcontractor by ID
                            val subcontractor = subcontractors.find { sub -> sub.id == assignment.subcontractorId }

                            if (subcontractor != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Subcontractor info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = subcontractor.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        // Specialty or role
                                        if (subcontractor.specialty.isNotBlank()) {
                                            Text(
                                                text = subcontractor.specialty,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Show phase assignment if any
                                        if (assignment.phaseId != null) {
                                            Text(
                                                text = "Assigned to specific phase",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Text(
                                                text = "Assigned to all phases",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Remove button
                                    IconButton(onClick = {
                                        viewModel.removeSubcontractorAssignment(prospect.id, assignment)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Subcontractor",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }

// Button to assign subcontractors
                OutlinedButton(
                    onClick = { viewModel.showDialog(DialogState.ASSIGN_SUBCONTRACTOR) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Assign Subcontractors")
                }
                // --- Project Phases Section ---
                SectionTitle("Project Phases")

// Use the updated PhaseViewer component
                key(tasksKey, globalPhases, selectedPhaseIndex) {
                    PhaseViewer(
                        phases = globalPhases,
                        tasks = prospect.tasks,
                        currentPhaseIndex = selectedPhaseIndex,
                        onPhaseSwipe = { newIndex ->
                            viewModel.setSelectedPhaseIndex(newIndex)
                        },
                        onTogglePhaseStatus = { phaseId, newStatus ->
                            viewModel.updatePhaseStatus(prospect.id, phaseId, newStatus)
                        },
                        onAddTask = { phaseId ->
                            viewModel.showAddTaskDialog(phaseId)
                        },
                        onTaskStatusChange = { taskId, isCompleted ->
                            viewModel.toggleTaskCompletion(prospect.id, taskId)
                        },
                        onEditTask = { taskId ->
                            viewModel.showEditTaskDialog(taskId)
                        },
                        onDeleteTask = { taskId ->
                            viewModel.deleteTask(prospect.id, taskId)
                        },
                        onEditPhase = { phase ->
                            viewModel.showEditPhaseDialog(phase)
                        },
                        onManagePhases = {
                            viewModel.showManagePhasesDialog()
                        },
                        onAddPhase = {
                            viewModel.showAddPhaseDialog()
                        }
                    )
                    // --- Images Section ---
                    SectionTitle("Project Images")
                }
                    val hasBeforeImages = prospect.beforeImageUris.isNotEmpty()
                    val hasAfterImages = prospect.afterImageUris.isNotEmpty()

                // Before Images Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Before Images",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { viewModel.requestBeforeImageUpload(prospect.id) },
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add before image",
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Add Image")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (hasBeforeImages) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(prospect.beforeImageUris) { (uri, timestamp) ->
                                ProjectImageItem(
                                    imageUri = uri,
                                    timestamp = timestamp,
                                    onRemoveClick = { viewModel.removeBeforeImage(prospect.id, uri) }
                                )
                            }
                        }
                    } else {
                        // Placeholder when no images
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .clip(MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "No before images",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "No Images Added",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // After Images Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "After Images",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { viewModel.requestAfterImageUpload(prospect.id) },
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add after image",
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Add Image")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (hasAfterImages) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(prospect.afterImageUris) { (uri, timestamp) ->
                                ProjectImageItem(
                                    imageUri = uri,
                                    timestamp = timestamp,
                                    onRemoveClick = { viewModel.removeAfterImage(prospect.id, uri) }
                                )
                            }
                        }
                    } else {
                        // Placeholder when no images
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .clip(MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "No after images",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "No Images Added",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // --- Actions Section ---
                SectionTitle("Actions")

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Status Toggle Button
                    Button(onClick = { viewModel.toggleProspectStatus(prospect.id) }) {
                        Text(
                            if (prospect.status == ProspectStatus.PROSPECT)
                                "Mark as Customer"
                            else
                                "Mark as Prospect"
                        )
                    }

                    // Delete Button
                    Button(
                        onClick = { showDeleteConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Record")
                    }
                }

            }
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to delete the record for ${selectedProspect?.customerName}? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedProspect?.id?.let { prospectId ->
                                viewModel.deleteProspectRecord(prospectId)
                            }
                            showDeleteConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.onError)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }


// ===========================================
// Helper Composables
// ===========================================
@Composable
private fun SubcontractorItem(
    subcontractor: Subcontractor,
    phaseName: String?,
    onViewDetails: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Subcontractor info
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onViewDetails)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = subcontractor.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Specialty or role
            if (subcontractor.specialty.isNotBlank()) {
                Text(
                    text = subcontractor.specialty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Assigned phase
            Text(
                text = "Working on: $phaseName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Remove button
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove Subcontractor",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
@Composable
private fun NoteItem(
    note: Note,
    onEditClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(note.timestamp) { dateFormat.format(Date(note.timestamp)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timestamp
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Edit button for ALL notes (removed type condition)
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Note",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Note content
        Text(
            text = note.content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Visual indicator for note type
        when (note.type) {
            NoteType.CALL -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Call Log",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            NoteType.EMAIL -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Email Log",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            NoteType.TEXT -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Text Log",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            else -> { /* No indicator for manual or system notes */ }
        }
    }
}

@Composable
private fun ProjectImageItem(
    imageUri: String,
    timestamp: Long?,
    onRemoveClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val formattedDate = timestamp?.let { remember(it) { dateFormat.format(Date(it)) } }

    Box(
        modifier = Modifier
            .width(150.dp)
            .height(200.dp)
    ) {
        // Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(MaterialTheme.shapes.medium)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Uri.parse(imageUri))
                    .build(),
                contentDescription = "Project image",
                modifier = Modifier.fillMaxSize()
            )
        }

        // Date text
        if (formattedDate != null) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 156.dp)
                    .align(Alignment.TopCenter)
            )
        }

        // Remove button
        IconButton(
            onClick = onRemoveClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Remove image",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String, isBoldValue: Boolean = false) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBoldValue) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun DetailRowWithIcon(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// ===========================================
// Intent Helper Functions
// ===========================================

/**
 * Launches an email client with the provided email address.
 */
private fun launchEmailIntent(context: Context, email: String?, viewModel: MainViewModel, prospectId: String) {
    if (email.isNullOrBlank()) return
    try {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val chooserIntent = Intent.createChooser(emailIntent, "Send email via...")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(chooserIntent)

        viewModel.logInteraction(prospectId, "Emailed")
    } catch (e: Exception) {
        Toast.makeText(context, "No Email App Found", Toast.LENGTH_SHORT).show()
        Log.e("ProspectDetailScreen", "Email Intent Error", e)
    }
}

/**
 * Launches the phone dialer with the provided phone number.
 */
private fun launchDialerIntent(context: Context, phone: String?, viewModel: MainViewModel, prospectId: String) {
    if (phone.isNullOrBlank()) return
    try {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        context.startActivity(dialIntent)
        viewModel.logInteraction(prospectId, "Called")
    } catch (e: Exception) {
        Toast.makeText(context, "No Dialer App Found", Toast.LENGTH_SHORT).show()
        Log.e("ProspectDetailScreen", "Dialer Intent Error", e)
    }
}

/**
 * Launches an SMS client with the provided phone number.
 */
private fun launchSmsIntent(context: Context, phone: String?, viewModel: MainViewModel, prospectId: String) {
    if (phone.isNullOrBlank()) return
    try {
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val chooserIntent = Intent.createChooser(smsIntent, "Send SMS via...")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(chooserIntent)

        viewModel.logInteraction(prospectId, "Texted")
    } catch (e: Exception) {
        Toast.makeText(context, "No SMS App Found", Toast.LENGTH_SHORT).show()
        Log.e("ProspectDetailScreen", "SMS Intent Error", e)
    }
}