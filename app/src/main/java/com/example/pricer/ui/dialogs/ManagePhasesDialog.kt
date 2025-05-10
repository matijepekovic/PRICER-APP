package com.example.pricer.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pricer.data.model.Phase
import com.example.pricer.data.model.PhaseStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePhasesDialog(
    phases: List<Phase>,
    onDismiss: () -> Unit,
    onSavePhases: (List<Phase>) -> Unit,
    onEditPhase: (Phase) -> Unit,
    onDeletePhase: (String) -> Unit
) {
    var currentPhases by remember { mutableStateOf(phases) }
    var showAddPhaseDialog by remember { mutableStateOf(false) }
    var phaseToDelete by remember { mutableStateOf<Phase?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 550.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Manage Project Phases",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Phase list
                if (currentPhases.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No phases defined yet",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentPhases.sortedBy { it.order }, key = { it.id }) { phase ->
                            PhaseListItem(
                                phase = phase,
                                onEdit = { onEditPhase(phase) },
                                onDelete = { phaseToDelete = phase }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add Phase Button
                Button(
                    onClick = { showAddPhaseDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Add New Phase")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Close")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onSavePhases(currentPhases)
                            onDismiss()
                        }
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }

    // Add Phase Dialog
    if (showAddPhaseDialog) {
        AddEditPhaseDialog(
            phase = null,
            onDismiss = { showAddPhaseDialog = false },
            onSave = { newPhase ->
                val nextOrder = if (currentPhases.isEmpty()) 0 else currentPhases.maxOf { it.order } + 1
                val phaseWithOrder = newPhase.copy(order = nextOrder)
                currentPhases = currentPhases + phaseWithOrder
                showAddPhaseDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (phaseToDelete != null) {
        AlertDialog(
            onDismissRequest = { phaseToDelete = null },
            title = { Text("Delete Phase?") },
            text = { Text("Are you sure you want to delete the phase \"${phaseToDelete?.name}\"? All associated tasks will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        val phase = phaseToDelete!!
                        currentPhases = currentPhases.filter { it.id != phase.id }
                        onDeletePhase(phase.id)
                        phaseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { phaseToDelete = null }
                ) {
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
    onDelete: () -> Unit
) {
    val statusColor = when(phase.status) {
        PhaseStatus.NOT_STARTED -> MaterialTheme.colorScheme.error
        PhaseStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        PhaseStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Phase number indicator
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${phase.order + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Phase details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = phase.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (phase.description.isNotBlank()) {
                    Text(
                        text = phase.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status indicator
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(statusColor.copy(alpha = 0.1f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when(phase.status) {
                            PhaseStatus.NOT_STARTED -> "Not Started"
                            PhaseStatus.IN_PROGRESS -> "In Progress"
                            PhaseStatus.COMPLETED -> "Completed"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }

            // Action buttons
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Phase",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Phase",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}