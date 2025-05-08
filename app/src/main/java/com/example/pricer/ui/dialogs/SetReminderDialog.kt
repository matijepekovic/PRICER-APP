package com.example.pricer.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for setting or editing a reminder for a prospect.
 * Uses TextFields with validation for date and time entry.
 *
 * @param initialReminderDateTime The current reminder timestamp (null if no reminder is set)
 * @param initialReminderNote The current reminder note (null if no note)
 * @param onDismiss Lambda to call when dialog is dismissed
 * @param onConfirm Lambda to call when a reminder is set/updated, passes the timestamp and note
 * @param onClear Lambda to call when a reminder is cleared
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetReminderDialog(
    initialReminderDateTime: Long? = null,
    initialReminderNote: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (reminderDateTime: Long, reminderNote: String?) -> Unit,
    onClear: () -> Unit
) {
    // Date formatter for display and parsing
    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US) // 12-hour format with AM/PM

    // Set initial values from timestamp or defaults
    val calendar = remember {
        Calendar.getInstance().apply {
            initialReminderDateTime?.let {
                timeInMillis = it
            } ?: run {
                // Default to tomorrow at 9:00 AM if no existing reminder
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
    }

    // Text field state
    var dateText by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    var timeText by remember { mutableStateOf(timeFormat.format(calendar.time)) }
    var reminderNote by remember { mutableStateOf(initialReminderNote ?: "") }

    // Error state
    var dateError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Dialog Title
                Text(
                    text = if (initialReminderDateTime == null) "Set Reminder" else "Edit Reminder",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Date Input Field
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {
                        dateText = it
                        dateError = null
                    },
                    label = { Text("Date (MM/DD/YYYY)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isError = dateError != null,
                    supportingText = dateError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Time Input Field
                OutlinedTextField(
                    value = timeText,
                    onValueChange = {
                        timeText = it
                        timeError = null
                    },
                    label = { Text("Time (hh:mm AM/PM)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isError = timeError != null,
                    supportingText = timeError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text, // Changed to Text to allow AM/PM
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Note Field
                OutlinedTextField(
                    value = reminderNote,
                    onValueChange = { reminderNote = it },
                    label = { Text("Reminder Note (Optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    singleLine = false,
                    maxLines = 3
                )

                // Validation message about inputs
                Text(
                    text = "Enter date as MM/DD/YYYY and time as HH:MM AM/PM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (initialReminderDateTime != null) {
                        // Show Clear button only when editing an existing reminder
                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                onClear()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                            Text("Clear Reminder")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()

                            // Validate date
                            val validDate = try {
                                calendar.time = dateFormat.parse(dateText)
                                true
                            } catch (e: ParseException) {
                                dateError = "Invalid date format"
                                false
                            }

                            // Validate time
                            val validTime = try {
                                val parsedTime = timeFormat.parse(timeText)
                                val timeCalendar = Calendar.getInstance().apply {
                                    time = parsedTime
                                }

                                if (validDate) {
                                    // Only update hour and minute, keep date from above
                                    calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                                    calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                                    calendar.set(Calendar.SECOND, 0)
                                    calendar.set(Calendar.MILLISECOND, 0)
                                }

                                true
                            } catch (e: ParseException) {
                                timeError = "Invalid time format. Use format like '9:00 AM' or '2:30 PM'"
                                false
                            }

                            // Confirm if both valid
                            if (validDate && validTime) {
                                // Check if reminder is in the past
                                if (calendar.timeInMillis < System.currentTimeMillis()) {
                                    dateError = "Reminder time cannot be in the past"
                                } else {
                                    onConfirm(
                                        calendar.timeInMillis,
                                        reminderNote.takeIf { it.isNotBlank() }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (initialReminderDateTime == null) "Set Reminder" else "Update")
                    }
                }

                // Cancel button in a separate row
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}