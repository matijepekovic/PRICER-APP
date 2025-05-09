package com.example.pricer.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pricer.data.model.Phase
import com.example.pricer.data.model.Subcontractor
import com.example.pricer.data.model.SubcontractorAssignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignSubcontractorDialog(
    subcontractors: List<Subcontractor>,
    phases: List<Phase>,
    currentAssignments: List<SubcontractorAssignment>,
    onDismiss: () -> Unit,
    onSave: (List<SubcontractorAssignment>) -> Unit
) {
    // Track which subcontractors are assigned to which phases
    val assignmentState = remember {
        mutableStateMapOf<String, String?>().apply {
            // Initialize with current assignments
            subcontractors.forEach { subcontractor ->
                val assignment = currentAssignments.find { it.subcontractorId == subcontractor.id }
                this[subcontractor.id] = assignment?.phaseId
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Assign Subcontractors",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (subcontractors.isEmpty()) {
                    Text(
                        text = "No subcontractors available. Add subcontractors first.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(subcontractors) { subcontractor ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
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

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Assignment options
                                    Text(
                                        text = "Assign to:",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // All phases option
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = assignmentState[subcontractor.id] == null,
                                            onClick = {
                                                assignmentState[subcontractor.id] = null
                                            }
                                        )

                                        Text(
                                            text = "All Phases",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    // Individual phase options
                                    phases.forEach { phase ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = assignmentState[subcontractor.id] == phase.id,
                                                onClick = {
                                                    assignmentState[subcontractor.id] = phase.id
                                                }
                                            )

                                            Text(
                                                text = phase.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }

                                    // Not assigned option
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = assignmentState[subcontractor.id] == "none",
                                            onClick = {
                                                assignmentState[subcontractor.id] = "none"
                                            }
                                        )

                                        Text(
                                            text = "Not Assigned",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            // Convert the current state to assignments
                            val newAssignments = assignmentState.entries
                                .filter { it.value != "none" }
                                .map { (subcontractorId, phaseId) ->
                                    SubcontractorAssignment(
                                        subcontractorId = subcontractorId,
                                        phaseId = phaseId, // null means assigned to all phases
                                        assignedAt = System.currentTimeMillis()
                                    )
                                }

                            onSave(newAssignments)
                        }
                    ) {
                        Text("Save Assignments")
                    }
                }
            }
        }
    }
}