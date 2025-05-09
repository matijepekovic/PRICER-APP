package com.example.pricer.ui.screens

import android.content.ActivityNotFoundException
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Shape
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pricer.data.model.DialogState
import com.example.pricer.data.model.Note
import com.example.pricer.data.model.NoteType
import com.example.pricer.data.model.ProspectRecord
import com.example.pricer.data.model.ProspectStatus
import com.example.pricer.viewmodel.MainViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import com.example.pricer.data.model.Phase
import com.example.pricer.data.model.PhaseStatus
/**
 * Screen for viewing and managing details of a single prospect/customer.
 * Displays contact info, notes, and provides actions like sharing PDFs and changing status.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhasesPager(
    prospect: ProspectRecord,
    globalPhases: List<Phase>,
    onPhaseSelect: (Int) -> Unit,
    selectedPhaseIndex: Int,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = selectedPhaseIndex, pageCount = { globalPhases.size })

    // Keep selected index in sync with pager
    LaunchedEffect(pagerState.currentPage) {
        onPhaseSelect(pagerState.currentPage)
    }

    Column(modifier = modifier) {
        // Phase tabs
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 8.dp
        ) {
            globalPhases.forEachIndexed { index, phase ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        onPhaseSelect(index)
                    },
                    text = {
                        Text(phase.name)
                    }
                )
            }
        }

        // Phase content pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val currentPhase = globalPhases.getOrNull(page)
            if (currentPhase != null) {
                PhaseContent(
                    prospect = prospect,
                    phase = currentPhase,
                    subcontractors = subcontractors
                )
            }
        }
    }
}

@Composable
fun PhaseContent(
    prospect: ProspectRecord,
    phase: Phase,
    subcontractors: List<Subcontractor>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Phase details header
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = phase.name,
                    style = MaterialTheme.typography.titleLarge
                )

                if (phase.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = phase.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status: ",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Chip(
                        onClick = { /* Toggle status */ },
                        colors = ChipDefaults.chipColors(
                            containerColor = when (phase.status) {
                                PhaseStatus.NOT_STARTED -> MaterialTheme.colorScheme.errorContainer
                                PhaseStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
                                PhaseStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                            }
                        )
                    ) {
                        Text(
                            text = when (phase.status) {
                                PhaseStatus.NOT_STARTED -> "Not Started"
                                PhaseStatus.IN_PROGRESS -> "In Progress"
                                PhaseStatus.COMPLETED -> "Completed"
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Phase images section
        SectionTitle("Phase Images")

        val phaseImages = prospect.phaseImages.find { it.phaseId == phase.id }?.imageUris ?: emptyList()

        if (phaseImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No images for this phase",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(phaseImages) { (uri, timestamp) ->
                    ProjectImageItem(
                        imageUri = uri,
                        timestamp = timestamp,
                        onRemoveClick = { viewModel.removePhaseImage(prospect.id, phase.id, uri) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.requestPhaseImageUpload(prospect.id, phase.id) }
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Add Phase Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Assigned subcontractors for this phase
        SectionTitle("Assigned Subcontractors")

        val phaseAssignments = prospect.subcontractorAssignments.filter {
            it.phaseId == phase.id || it.phaseId == null // Show specific phase assignments + global assignments
        }

        if (phaseAssignments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No subcontractors assigned to this phase",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                phaseAssignments.forEach { assignment ->
                    val subcontractor = subcontractors.find { it.id == assignment.subcontractorId }
                    if (subcontractor != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = subcontractor.name,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                if (subcontractor.specialty.isNotBlank()) {
                                    Text(
                                        text = "Specialty: ${subcontractor.specialty}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Text(
                                    text = if (assignment.phaseId == null)
                                        "Assigned to all phases"
                                    else
                                        "Assigned to this phase",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (subcontractor.phone.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = subcontractor.phone,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }

                                    if (subcontractor.email.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = subcontractor.email,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
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
                actions = {
                    IconButton(onClick = { viewModel.showSubcontractorsScreen() }) {
                        Icon(Icons.Default.Person, contentDescription = "Manage Subcontractors")
                    }
                    selectedProspect?.let { prospect ->
                        if (!prospect.externalPdfUriString.isNullOrBlank()) {
                            IconButton(onClick = {
                                sharePdf(context, fileProviderAuthority, prospect.externalPdfUriString, prospect.customerName)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share PDF")
                            }
                        }
                    }
                }
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
                DetailRow(
                    label = "Status:",
                    value = prospect.status.name.lowercase().replaceFirstChar { it.titlecase() }
                )
                DetailRow(label = "Created:", value = dateFormat.format(Date(prospect.dateCreated)))
                DetailRow(label = "Updated:", value = dateFormat.format(Date(prospect.dateUpdated)))

                ListSpacer()

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
                            openPdf(context, prospect.externalPdfUriString, fileProviderAuthority, prospect.customerName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("View Quote PDF")
                    }
                }

                ListSpacer()

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
                                    // Allow editing ALL note types
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

                ListSpacer()


// --- Reminder Section ---
                SectionTitle("Reminder")

                val prospect = selectedProspect!!
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
// Add after your "Set/Edit Reminder" button
// Button to set/edit reminder
                OutlinedButton(
                    onClick = {
                        viewModel.showDialog(
                            DialogState.SET_REMINDER,
                            prospect // Pass the whole prospect record as data
                        )
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
                // --- Images Section ---
                SectionTitle("Project Images")

                val hasBeforeImages = !prospect.beforeImageUris.isNullOrEmpty()
                val hasAfterImages = !prospect.afterImageUris.isNullOrEmpty()

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
}

// ===========================================
// Helper Composables
// ===========================================
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
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ListSpacer() {
    Spacer(Modifier.height(16.dp))
    Divider()
    Spacer(Modifier.height(16.dp))
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
// Intent and File Helper Functions
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

/**
 * Copies content from a source URI to a temp file in the app's cache.
 */
private fun copyUriToTempCacheFile(context: Context, sourceUri: Uri, prospectId: String, customerName: String): File? {
    val safeName = customerName.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(50)
    val tempFileName = "view_${safeName}_${prospectId}.pdf"
    val cacheSubDir = "view_cache"
    val cacheDirFile = File(context.cacheDir, cacheSubDir).apply { mkdirs() }
    val tempCachedPdf = File(cacheDirFile, tempFileName)

    try {
        Log.d("FileUtils", "Copying $sourceUri -> ${tempCachedPdf.absolutePath}")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(tempCachedPdf).use { output -> input.copyTo(output) }
        } ?: throw IOException("InputStream null for $sourceUri")

        val success = tempCachedPdf.exists() && tempCachedPdf.length() > 0

        if (success) {
            Log.i("FileUtils", "Copy successful to ${tempCachedPdf.name}")
            return tempCachedPdf
        } else {
            Log.w("FileUtils", "Copy seemed successful but file empty/missing")
            return null
        }
    } catch (e: Exception) {
        Log.e("FileUtils", "Error during copy to cache", e)
        tempCachedPdf.delete()
        return null
    }
}

/**
 * Launches an Intent to share a PDF with other apps.
 */
private fun sharePdf(context: Context, authority: String, pdfUriString: String?, customerName: String?) {
    if (pdfUriString.isNullOrBlank()) {
        Toast.makeText(context, "No PDF URI to share.", Toast.LENGTH_SHORT).show()
        return
    }

    var tempFile: File? = null
    try {
        val originalUri = Uri.parse(pdfUriString)
        tempFile = copyUriToTempCacheFile(
            context,
            originalUri,
            "share_${UUID.randomUUID()}",
            customerName ?: "shared"
        ) ?: throw IOException("Failed to prepare temporary file for sharing.")

        val shareableUri = FileProvider.getUriForFile(context, authority, tempFile)
        Log.i("ProspectDetailScreen", "Sharing PDF using FileProvider URI: $shareableUri")

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, shareableUri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, "Quote for ${customerName ?: "Prospect"}")
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share PDF via...")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        Log.e("ProspectDetailScreen", "Error sharing PDF: $pdfUriString", e)
        Toast.makeText(context, "Could not share PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        tempFile?.delete()
    }
}
private fun openPdf(context: Context, pdfUriString: String?, authority: String, customerName: String?) {
    if (pdfUriString.isNullOrBlank()) {
        Toast.makeText(context, "No PDF available to open", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val originalUri = Uri.parse(pdfUriString)
        val tempFile = copyUriToTempCacheFile(
            context,
            originalUri,
            "view_${UUID.randomUUID()}",
            customerName ?: "quote"
        ) ?: throw IOException("Failed to prepare temporary file for viewing.")

        val viewableUri = FileProvider.getUriForFile(context, authority, tempFile)

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(viewableUri, "application/pdf")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(viewIntent, "Open PDF with..."))
    } catch (e: Exception) {
        Log.e("ProspectDetailScreen", "Error opening PDF: $pdfUriString", e)
        Toast.makeText(context, "Could not open PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}@Composable
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
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(imageUri))
                        .build()
                ),
                contentDescription = "Project image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
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
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = CircleShape
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