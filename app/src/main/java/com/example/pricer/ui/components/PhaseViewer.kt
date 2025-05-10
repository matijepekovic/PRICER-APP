package com.example.pricer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pricer.data.model.Phase
import com.example.pricer.data.model.PhaseStatus
import com.example.pricer.data.model.Task
import com.example.pricer.viewmodel.MainViewModel

/**
 * A swipeable phase viewer component that shows one phase at a time
 * with the ability to swipe between phases and toggle their status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhaseViewer(
    phases: List<Phase>,
    tasks: List<Task>,
    currentPhaseIndex: Int,
    onPhaseSwipe: (Int) -> Unit,
    onTogglePhaseStatus: (String, PhaseStatus) -> Unit,
    onAddTask: (String) -> Unit,
    onTaskStatusChange: (String, Boolean) -> Unit,
    onEditPhase: (Phase) -> Unit
) {
    if (phases.isEmpty()) {
        // No phases state
        EmptyPhasesState(onAddPhase = { /* Show phase creation UI */ })
        return
    }

    val currentPhase = phases.getOrNull(currentPhaseIndex) ?: return

    Box(modifier = Modifier.fillMaxWidth()) {
        // Phase navigation indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Previous phase button
            IconButton(
                onClick = {
                    if (currentPhaseIndex > 0) onPhaseSwipe(currentPhaseIndex - 1)
                },
                enabled = currentPhaseIndex > 0
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Phase",
                    tint = if (currentPhaseIndex > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // Phase indicators
            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                phases.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentPhaseIndex)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Next phase button
            IconButton(
                onClick = {
                    if (currentPhaseIndex < phases.size - 1) onPhaseSwipe(currentPhaseIndex + 1)
                },
                enabled = currentPhaseIndex < phases.size - 1
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Phase",
                    tint = if (currentPhaseIndex < phases.size - 1)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        // Main Phase Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(top = 32.dp), // Space for the navigation controls
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Phase header with name and toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentPhase.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (currentPhase.description.isNotBlank()) {
                            Text(
                                text = currentPhase.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Status / Toggle switch
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        PhaseStatusSelector(
                            currentStatus = currentPhase.status,
                            onStatusChange = { newStatus ->
                                onTogglePhaseStatus(currentPhase.id, newStatus)
                            }
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Tasks section
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val phaseTasks = remember(tasks, currentPhase.id) {
                    tasks.filter { task -> task.phaseId == currentPhase.id }
                }

                if (phaseTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tasks in this phase yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(phaseTasks, key = { it.id }) { task ->
                            TaskItem(
                                task = task,
                                onStatusChange = { isCompleted ->
                                    onTaskStatusChange(task.id, isCompleted)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Add task button
                OutlinedButton(
                    onClick = { onAddTask(currentPhase.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Add Task to This Phase")
                }

                // Edit phase button
                TextButton(
                    onClick = { onEditPhase(currentPhase) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Edit Phase")
                }
            }
        }
    }
}

@Composable
fun EmptyPhasesState(onAddPhase: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Project Phases",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add phases to organize your project into manageable steps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddPhase) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Create First Phase")
            }
        }
    }
}

@Composable
fun PhaseStatusSelector(
    currentStatus: PhaseStatus,
    onStatusChange: (PhaseStatus) -> Unit
) {
    val statusColor = when(currentStatus) {
        PhaseStatus.NOT_STARTED -> MaterialTheme.colorScheme.error
        PhaseStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        PhaseStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
    }

    Column(
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(statusColor.copy(alpha = 0.12f))
                .border(
                    width = 1.dp,
                    color = statusColor.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.small
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = when(currentStatus) {
                    PhaseStatus.NOT_STARTED -> "Not Started"
                    PhaseStatus.IN_PROGRESS -> "In Progress"
                    PhaseStatus.COMPLETED -> "Completed"
                },
                style = MaterialTheme.typography.labelMedium,
                color = statusColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status toggle menu
        var expanded by remember { mutableStateOf(false) }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Change Status"
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                PhaseStatus.values().forEach { status ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when(status) {
                                                PhaseStatus.NOT_STARTED -> MaterialTheme.colorScheme.error
                                                PhaseStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                                                PhaseStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                                            }
                                        )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = when(status) {
                                        PhaseStatus.NOT_STARTED -> "Not Started"
                                        PhaseStatus.IN_PROGRESS -> "In Progress"
                                        PhaseStatus.COMPLETED -> "Completed"
                                    }
                                )
                            }
                        },
                        leadingIcon = if (status == currentStatus) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null
                                )
                            }
                        } else null,
                        onClick = {
                            onStatusChange(status)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onStatusChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = onStatusChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            )

            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (task.dueDate != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "Due: " + formatDate(task.dueDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Helper function to format date
private fun formatDate(timestamp: Long): String {
    val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return dateFormat.format(java.util.Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPhaseDialog(
    phase: Phase,
    onDismiss: () -> Unit,
    onSave: (Phase) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember { mutableStateOf(phase.name) }
    var description by remember { mutableStateOf(phase.description) }
    var status by remember { mutableStateOf(phase.status) }
    var nameError by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Edit Phase",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Phase Name*") },
                    isError = nameError,
                    supportingText = if (nameError) { { Text("Name is required") } } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when(phaseStatus) {
                                                PhaseStatus.NOT_STARTED -> MaterialTheme.colorScheme.error
                                                PhaseStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                                                PhaseStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                                            }
                                        )
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { showDeleteConfirmation = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Delete Phase")
                    }

                    Row {
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

                                val updatedPhase = phase.copy(
                                    name = name.trim(),
                                    description = description.trim(),
                                    status = status
                                )

                                onSave(updatedPhase)
                                onDismiss()
                            }
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Phase?") },
            text = { Text("Are you sure you want to delete this phase? All associated tasks will be deleted as well.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(phase.id)
                        showDeleteConfirmation = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Phase")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    phaseId: String, // Required parameter
    onDismiss: () -> Unit,
    onAddTask: (Task) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var titleError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Add Task",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = false },
                    label = { Text("Task Title*") },
                    isError = titleError,
                    supportingText = if (titleError) { { Text("Title is required") } } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Due date picker would go here
                // ...

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
                            if (title.isBlank()) {
                                titleError = true
                                return@Button
                            }

                            val newTask = Task(
                                id = java.util.UUID.randomUUID().toString(),
                                title = title.trim(),
                                description = description.trim(),
                                isCompleted = false,
                                dueDate = dueDate,
                                // We'd need to add a phaseId field to Task or handle association elsewhere
                                // phaseId = phaseId,
                                createdAt = System.currentTimeMillis()
                            )

                            onAddTask(newTask)
                            onDismiss()
                        }
                    ) {
                        Text("Add Task")
                    }
                }
            }
        }
    }
}

// Add this overloaded version to fix the compilation error
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAddTask: (Task) -> Unit
) {
    // Call the original function with a default empty string for phaseId
    AddTaskDialog(
        phaseId = "",
        onDismiss = onDismiss,
        onAddTask = onAddTask
    )
}