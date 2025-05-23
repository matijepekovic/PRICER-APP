// In SubcontractorsScreen.kt, replace the entire file with this corrected version:

package com.example.pricer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pricer.data.model.Subcontractor
import com.example.pricer.viewmodel.MainViewModel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.UUID
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubcontractorsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val subcontractors by viewModel.subcontractors.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var subcontractorToEdit by remember { mutableStateOf<Subcontractor?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<Subcontractor?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subcontractors") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Subcontractor")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (subcontractors.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No subcontractors added yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add your first subcontractor by clicking the + button",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subcontractors, key = { it.id }) { subcontractor ->
                        SubcontractorCard(
                            subcontractor = subcontractor,
                            onEdit = { subcontractorToEdit = subcontractor },
                            onDelete = { showDeleteConfirmation = subcontractor }
                        )
                    }
                }
            }
        }
    }

    // Add/Edit Subcontractor Dialog
    if (showAddDialog || subcontractorToEdit != null) {
        SubcontractorDialog(
            subcontractor = subcontractorToEdit,
            onDismiss = {
                showAddDialog = false
                subcontractorToEdit = null
            },
            onSave = { subcontractor ->
                if (subcontractorToEdit != null) {
                    viewModel.updateSubcontractor(subcontractor)
                } else {
                    viewModel.addSubcontractor(subcontractor)
                }
                showAddDialog = false
                subcontractorToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${showDeleteConfirmation?.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubcontractor(showDeleteConfirmation!!.id)
                        showDeleteConfirmation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SubcontractorCard(
    subcontractor: Subcontractor,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subcontractor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (subcontractor.specialty.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Specialty: ${subcontractor.specialty}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (subcontractor.contactName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Contact: ${subcontractor.contactName}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

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

            if (subcontractor.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${subcontractor.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubcontractorDialog(
    subcontractor: Subcontractor?,
    onDismiss: () -> Unit,
    onSave: (Subcontractor) -> Unit
) {
    val isEditing = subcontractor != null
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Create focus requesters for each field to handle keyboard navigation
    val specialtyFocus = remember { FocusRequester() }
    val contactNameFocus = remember { FocusRequester() }
    val phoneFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val notesFocus = remember { FocusRequester() }

    var name by remember { mutableStateOf(subcontractor?.name ?: "") }
    var specialty by remember { mutableStateOf(subcontractor?.specialty ?: "") }
    var contactName by remember { mutableStateOf(subcontractor?.contactName ?: "") }
    var phone by remember { mutableStateOf(subcontractor?.phone ?: "") }
    var email by remember { mutableStateOf(subcontractor?.email ?: "") }
    var notes by remember { mutableStateOf(subcontractor?.notes ?: "") }

    var nameError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {
            keyboardController?.hide()
            onDismiss()
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEditing) "Edit Subcontractor" else "Add Subcontractor",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Name*") },
                    isError = nameError,
                    supportingText = if (nameError) { { Text("Name is required") } } else null,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { specialtyFocus.requestFocus() }
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Specialty field
                OutlinedTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = { Text("Specialty") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(specialtyFocus),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { contactNameFocus.requestFocus() }
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Contact Name field
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Contact Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(contactNameFocus),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { phoneFocus.requestFocus() }
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Phone field
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(phoneFocus),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { emailFocus.requestFocus() }
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(emailFocus),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { notesFocus.requestFocus() }
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Notes field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(notesFocus)
                        .heightIn(min = 100.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            // Optional: automatically submit if all required fields are filled
                            if (name.isNotBlank()) {
                                val newSubcontractor = Subcontractor(
                                    id = subcontractor?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    specialty = specialty.trim(),
                                    contactName = contactName.trim(),
                                    phone = phone.trim(),
                                    email = email.trim(),
                                    notes = notes.trim()
                                )
                                onSave(newSubcontractor)
                            } else {
                                nameError = true
                            }
                        }
                    ),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            keyboardController?.hide()
                            onDismiss()
                        }
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            if (name.isBlank()) {
                                nameError = true
                                return@Button
                            }

                            val newSubcontractor = Subcontractor(
                                id = subcontractor?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                specialty = specialty.trim(),
                                contactName = contactName.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                notes = notes.trim()
                            )

                            onSave(newSubcontractor)
                        }
                    ) {
                        Text(if (isEditing) "Update" else "Add")
                    }
                }
            }
        }
    }
}