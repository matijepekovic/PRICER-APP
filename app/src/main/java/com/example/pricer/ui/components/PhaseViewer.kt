package com.example.pricer.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.material.ripple.rememberRipple

/**
 * A swipeable phase viewer component that shows one phase at a time
 * with the ability to swipe between phases and toggle their status.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhaseViewer(
    phases: List<Phase>,
    tasks: List<Task>,
    currentPhaseIndex: Int,
    onPhaseSwipe: (Int) -> Unit,
    onTogglePhaseStatus: (String, PhaseStatus) -> Unit,
    onAddTask: (String) -> Unit,
    onTaskStatusChange: (String, Boolean) -> Unit,
    onEditTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onEditPhase: (Phase) -> Unit,
    onManagePhases: () -> Unit,
    onAddPhase: () -> Unit
) {
    if (phases.isEmpty()) {
        // No phases state
        EmptyPhasesState(onAddPhase = onAddPhase)
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = currentPhaseIndex.coerceIn(0, phases.size - 1),
        pageCount = { phases.size }
    )

    // When pager state changes, notify the parent
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentPhaseIndex) {
            onPhaseSwipe(pagerState.currentPage)
        }
    }

    // When currentPhaseIndex changes from parent, update pager
    LaunchedEffect(currentPhaseIndex) {
        if (currentPhaseIndex in 0 until phases.size &&
            currentPhaseIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(currentPhaseIndex)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Title with phase count and management button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Project Phases",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onManagePhases) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Manage Phases"
                )
            }
        }

        // Phase indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            phases.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }

        // Horizontal pager for phases
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val phase = phases[page]
            val phaseTasks = tasks.filter { it.phaseId == phase.id }

            PhaseCard(
                phase = phase,
                tasks = phaseTasks,
                onToggleStatus = { newStatus -> onTogglePhaseStatus(phase.id, newStatus) },
                onAddTask = { onAddTask(phase.id) },
                onTaskStatusChange = onTaskStatusChange,
                onEditTask = onEditTask,
                onDeleteTask = onDeleteTask,
                onEditPhase = { onEditPhase(phase) }
            )
        }

        // Center add phase button (removed navigation arrows)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = onAddPhase,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Phase"
                )
            }
        }
    }
}

@Composable
fun PhaseCard(
    phase: Phase,
    tasks: List<Task>,
    onToggleStatus: (PhaseStatus) -> Unit,
    onAddTask: () -> Unit,
    onTaskStatusChange: (String, Boolean) -> Unit,
    onEditTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onEditPhase: () -> Unit
) {
    // Add a key to force recomposition when tasks change
    val tasksKey = remember(tasks) { tasks.hashCode() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        text = phase.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (phase.description.isNotBlank()) {
                        Text(
                            text = phase.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status toggle - using a Button for better touch feedback
                PhaseStatusSelector(
                    currentStatus = phase.status,
                    onStatusChange = onToggleStatus
                )
            }


            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Tasks section
            Text(
                text = "Tasks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            key(tasksKey) {
                if (tasks.isEmpty()) {
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
                        items(tasks, key = { it.id }) { task ->
                            TaskItem(
                                task = task,
                                onStatusChange = { isCompleted ->
                                    onTaskStatusChange(task.id, isCompleted)
                                },
                                onEditTask = {
                                    onEditTask(task.id)
                                },
                                onDeleteTask = {
                                    onDeleteTask(task.id)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add task button
            OutlinedButton(
                onClick = onAddTask,
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
private fun PhaseStatusSelector(
    currentStatus: PhaseStatus,
    onStatusChange: (PhaseStatus) -> Unit
) {
    val statusColor = when(currentStatus) {
        PhaseStatus.NOT_STARTED -> MaterialTheme.colorScheme.error
        PhaseStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        PhaseStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
    }

    // Status display that is also a button
    Button(
        onClick = {
            // Cycle to the next status when clicked
            val nextStatus = when(currentStatus) {
                PhaseStatus.NOT_STARTED -> PhaseStatus.IN_PROGRESS
                PhaseStatus.IN_PROGRESS -> PhaseStatus.COMPLETED
                PhaseStatus.COMPLETED -> PhaseStatus.NOT_STARTED
            }
            onStatusChange(nextStatus)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = statusColor.copy(alpha = 0.15f),
            contentColor = statusColor
        ),
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Text(
            text = when(currentStatus) {
                PhaseStatus.NOT_STARTED -> "Not Started"
                PhaseStatus.IN_PROGRESS -> "In Progress"
                PhaseStatus.COMPLETED -> "Completed"
            },
            style = MaterialTheme.typography.labelMedium
        )
    }
}
@Composable
fun TaskItem(
    task: Task,
    onStatusChange: (Boolean) -> Unit,
    onEditTask: () -> Unit,
    onDeleteTask: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

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
                .clickable { showOptions = !showOptions }
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

            // Task options (edit/delete) that show when clicked
            AnimatedVisibility(visible = showOptions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onEditTask,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Task",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }

                    TextButton(
                        onClick = onDeleteTask,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Task",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
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