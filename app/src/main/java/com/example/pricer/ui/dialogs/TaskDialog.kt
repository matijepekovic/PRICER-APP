package com.example.pricer.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pricer.data.model.Task
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(
    task: Task?,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    val isEditing = task != null

    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var dueDateString by remember { mutableStateOf(task?.dueDate?.let { formatDate(it) } ?: "") }

    var titleError by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (isEditing) "Edit Task" else "Add Task",
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
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = dueDateString,
                    onValueChange = { dueDateString = it; dateError = false },
                    label = { Text("Due Date (MM/DD/YYYY)") },
                    isError = dateError,
                    supportingText = if (dateError) { { Text("Invalid date format") } } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isEditing) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task?.isCompleted ?: false,
                            onCheckedChange = null
                        )

                        Text(
                            text = "Mark as completed",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

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

                            val dueDate = if (dueDateString.isBlank()) {
                                null
                            } else {
                                try {
                                    dateFormat.parse(dueDateString)?.time
                                } catch (e: Exception) {
                                    dateError = true
                                    return@Button
                                }
                            }

                            val newTask = Task(
                                id = task?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                description = description.trim(),
                                isCompleted = task?.isCompleted ?: false,
                                dueDate = dueDate,
                                createdAt = task?.createdAt ?: System.currentTimeMillis()
                            )

                            onSave(newTask)
                        }
                    ) {
                        Text(if (isEditing) "Update" else "Add")
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    return sdf.format(Date(timestamp))
}