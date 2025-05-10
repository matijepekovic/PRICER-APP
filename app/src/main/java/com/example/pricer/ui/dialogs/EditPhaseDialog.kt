package com.example.pricer.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pricer.data.model.Phase
import com.example.pricer.data.model.PhaseStatus
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPhaseDialog(
    phase: Phase?,
    onDismiss: () -> Unit,
    onSave: (Phase) -> Unit
) {
    val isNewPhase = phase == null
    var name by remember { mutableStateOf(phase?.name ?: "") }
    var description by remember { mutableStateOf(phase?.description ?: "") }
    var status by remember { mutableStateOf(phase?.status ?: PhaseStatus.NOT_STARTED) }
    var nameError by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (isNewPhase) "Add New Phase" else "Edit Phase",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Phase Name*") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Name is required") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.clearFocus() }
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status selector (only for editing)
                if (!isNewPhase) {
                    Text(
                        text = "Phase Status",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PhaseStatus.values().forEach { phaseStatus ->
                            FilterChip(
                                selected = status == phaseStatus,
                                onClick = { status = phaseStatus },
                                label = {
                                    Text(
                                        text = when(phaseStatus) {
                                            PhaseStatus.NOT_STARTED -> "Not Started"
                                            PhaseStatus.IN_PROGRESS -> "In Progress"
                                            PhaseStatus.COMPLETED -> "Completed"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                                return@Button
                            }

                            val newPhase = Phase(
                                id = phase?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                order = phase?.order ?: 0, // This will be set by parent component
                                description = description.trim(),
                                status = status
                            )

                            onSave(newPhase)
                        }
                    ) {
                        Text(if (isNewPhase) "Add Phase" else "Save Changes")
                    }
                }
            }
        }
    }
}