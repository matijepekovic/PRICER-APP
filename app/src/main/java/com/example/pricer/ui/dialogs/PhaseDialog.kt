package com.example.pricer.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pricer.data.model.Phase
import com.example.pricer.data.model.PhaseStatus
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePhasesDialog(
    phases: List<Phase>,
    onDismiss: () -> Unit,
    onSave: (List<Phase>) -> Unit
) {
    var workingPhases by remember { mutableStateOf(phases) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var phaseToEdit by remember { mutableStateOf<Phase?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<Phase?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Manage Project Phases",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (workingPhases.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No phases defined yet. Add your first phase.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(workingPhases, key = { _, phase -> phase.id }) { index, phase ->
                            PhaseListItem(
                                phase = phase,
                                onEdit = { phaseToEdit = phase; showAddEditDialog = true },
                                onDelete = { showDeleteConfirmation = phase },
                                onMoveUp = {
                                    if (index > 0) {
                                        val newList = workingPhases.toMutableList()
                                        val prev = newList[index - 1].copy(order = index)
                                        val current = newList[index].copy(order = index - 1)
                                        newList[index - 1] = current
                                        newList[index] = prev
                                        workingPhases = newList.sortedBy { it.order }
                                    }
                                },
                                onMoveDown = {
                                    if (index < workingPhases.size - 1) {
                                        val newList = workingPhases.toMutableList()
                                        val next = newList[index + 1].copy(order = index)
                                        val current = newList[index].copy(order = index + 1)
                                        newList[index + 1] = current
                                        newList[index] = next
                                        workingPhases = newList.sortedBy { it.order }
                                    }
                                },
                                canMoveUp = index > 0,
                                canMoveDown = index < workingPhases.size - 1
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { phaseToEdit = null; showAddEditDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Add Phase")
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onSave(workingPhases) }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // Add/Edit dialog
    if (showAddEditDialog) {
        AddEditPhaseDialog(
            phase = phaseToEdit,
            existingPhases = workingPhases,
            onDismiss = { showAddEditDialog = false; phaseToEdit = null },
            onSave = { phase ->
                workingPhases = if (phaseToEdit != null) {
                    // Update existing phase
                    workingPhases.map { if (it.id == phase.id) phase else it }
                } else {
                    // Add new phase with next order number
                    val nextOrder = workingPhases.maxOfOrNull { it.order }?.plus(1) ?: 0
                    workingPhases + phase.copy(order = nextOrder)
                }.sortedBy { it.order }

                showAddEditDialog = false
                phaseToEdit = null
            }
        )
    }

    // Delete confirmation
    if (showDeleteConfirmation != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Confirm Deletion") },
            text = {
                Text("Are you sure you want to delete the phase '${showDeleteConfirmation?.name}'?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        workingPhases = workingPhases
                            .filter { it.id != showDeleteConfirmation?.id }
                            .mapIndexed { index, phase -> phase.copy(order = index) }
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
fun PhaseListItem(
    phase: Phase,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = phase.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (phase.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = phase.description,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up"
                        )
                    }

                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down"
                        )
                    }

                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit"
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPhaseDialog(
    phase: Phase?,
    existingPhases: List<Phase>,
    onDismiss: () -> Unit,
    onSave: (Phase) -> Unit
) {
    val isEditing = phase != null

    var name by remember { mutableStateOf(phase?.name ?: "") }
    var description by remember { mutableStateOf(phase?.description ?: "") }

    var nameError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (isEditing) "Edit Phase" else "Add Phase",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Phase Name*") },
                    isError = nameError,
                    supportingText = if (nameError) { { Text("Name is required") } } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                                return@Button
                            }

                            // Check for duplicate names
                            val duplicateName = existingPhases.any {
                                it.name.equals(name.trim(), ignoreCase = true) &&
                                        it.id != (phase?.id ?: "")
                            }

                            if (duplicateName) {
                                nameError = true
                                return@Button
                            }

                            val newPhase = Phase(
                                id = phase?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                description = description.trim(),
                                order = phase?.order ?: existingPhases.size,
                                status = phase?.status ?: PhaseStatus.NOT_STARTED
                            )

                            onSave(newPhase)
                        }
                    ) {
                        Text(if (isEditing) "Update" else "Add")
                    }
                }
            }
        }
    }
}