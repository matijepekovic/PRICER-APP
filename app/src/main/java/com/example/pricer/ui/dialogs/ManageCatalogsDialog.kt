package com.example.pricer.ui.dialogs

// Imports
import android.util.Log
import androidx.compose.animation.AnimatedVisibility // Added
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Add, Delete, Edit, Save, Check, Close, Cancel, CheckCircle, Warning, Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow // Added
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pricer.data.model.Catalog
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.Business

/**
 * Dialog for managing multiple Catalogs: Selecting the active one, adding new ones,
 * renaming existing ones, deleting them, and sharing them.
 *
 * @param catalogs The map of all available catalogs (ID -> Catalog object).
 * @param activeCatalogId The ID of the currently active catalog.
 * @param onDismiss Lambda to dismiss the dialog.
 * @param onSelectCatalog Lambda called when a catalog is selected. Passes the catalog ID.
 * @param onAddCatalog Lambda called to add a new catalog. Passes the desired name.
 * @param onRenameCatalog Lambda called to rename a catalog. Passes the catalog ID and the new name.
 * @param onDeleteCatalog Lambda called to delete a catalog. Passes the catalog ID.
 * @param onShareCatalog Lambda called to request sharing a catalog. Passes the catalog ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun ManageCatalogsDialog(
    catalogs: Map<String, Catalog>,
    activeCatalogId: String,
    onDismiss: () -> Unit,
    onSelectCatalog: (String) -> Unit,
    onAddCatalog: (String) -> Unit,
    onRenameCatalog: (id: String, newName: String) -> Unit,
    onUpdateCompanyName: (id: String, newCompanyName: String) -> Unit, // Add new callback
    onDeleteCatalog: (id: String) -> Unit,
    onShareCatalog: (id: String) -> Unit // Added share callback
    // TODO: Add Save/Load All callbacks if needed
) {
    // --- State within the Dialog ---
    var showAddCatalogInput by remember { mutableStateOf(false) }
    var newCatalogName by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf(false) }
    var renamingCatalogId by remember { mutableStateOf<String?>(null) }
    var renameCatalogNewName by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }
    val addFocusRequester = remember { FocusRequester() }
    val renameFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Request focus when add input appears
    LaunchedEffect(showAddCatalogInput) {
        if (showAddCatalogInput) { delay(100); addFocusRequester.requestFocus() }
    }
    // Request focus when rename input appears
    LaunchedEffect(renamingCatalogId) {
        if (renamingCatalogId != null) { delay(100); renameFocusRequester.requestFocus() }
    }


    // --- Main Dialog ---
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp)) {
                // --- Title ---
                Text("Manage Catalogs", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 12.dp))

                // --- Add New Catalog Section (Conditional) ---
                AnimatedVisibility(visible = showAddCatalogInput) {
                    Column {
                        OutlinedTextField(
                            value = newCatalogName,
                            onValueChange = { newCatalogName = it; addError = false },
                            label = { Text("New Catalog Name") },
                            modifier = Modifier.fillMaxWidth().focusRequester(addFocusRequester).padding(bottom = 4.dp),
                            isError = addError,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Words),
                            keyboardActions = KeyboardActions(onDone = { if (newCatalogName.isNotBlank()) { onAddCatalog(newCatalogName.trim()); newCatalogName = ""; showAddCatalogInput = false; focusManager.clearFocus() } else { addError = true } }),
                            singleLine = true,
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { if (newCatalogName.isNotBlank()) { onAddCatalog(newCatalogName.trim()); newCatalogName = ""; showAddCatalogInput = false; focusManager.clearFocus() } else { addError = true } }, enabled = newCatalogName.isNotBlank()) {
                                        Icon(Icons.Filled.Check, contentDescription = "Confirm Add", tint=MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { showAddCatalogInput = false; newCatalogName = ""; addError = false; focusManager.clearFocus() }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Cancel Add")
                                    }
                                }
                            }
                        )
                        if(addError) { Text("Name cannot be empty", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                    } // End Column for Add input
                } // End AnimatedVisibility

                // Show "Add New Catalog" Button only if not currently adding
                if (!showAddCatalogInput) {
                    Button( onClick = { showAddCatalogInput = true; renamingCatalogId = null }, modifier = Modifier.fillMaxWidth() ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Add New Catalog")
                    }
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                }

                // --- List of Existing Catalogs ---
                Text("Available Catalogs:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                val sortedCatalogs = remember(catalogs) { catalogs.values.sortedBy { it.name.lowercase() } }

                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(items = sortedCatalogs, key = { it.id }) { catalog ->
                        val isRenamingThis = renamingCatalogId == catalog.id
                        val canDelete = catalog.id != activeCatalogId && catalogs.size > 1
                        val interactionEnabled = renamingCatalogId == null && !showAddCatalogInput

                        CatalogListItem( // Pass all needed parameters and callbacks
                            catalog = catalog,
                            isActive = catalog.id == activeCatalogId,
                            isRenaming = isRenamingThis,
                            canDelete = canDelete,
                            renameValue = if (isRenamingThis) renameCatalogNewName else "",
                            isErrorRenaming = renameError && isRenamingThis,
                            interactionEnabled = interactionEnabled,
                            renameFocusRequester = renameFocusRequester,
                            onSelect = { onSelectCatalog(catalog.id) },
                            onStartRename = { renamingCatalogId = catalog.id; renameCatalogNewName = catalog.name; renameError = false },
                            onRenameValueChange = { renameCatalogNewName = it; renameError = false },
                            onConfirmRename = { if (renameCatalogNewName.isNotBlank()){ onRenameCatalog(catalog.id, renameCatalogNewName.trim()); renamingCatalogId = null } else { renameError = true } },
                            onCancelRename = { renamingCatalogId = null; renameError = false },
                            onDelete = { showDeleteConfirmation = catalog.id },
                            onShare = { onShareCatalog(catalog.id) }, // *** PASS SHARE CALLBACK ***
                            onUpdateCompanyName = onUpdateCompanyName // *** ADD THIS NEW PARAMETER ***
                        )
                        Divider()
                    } // End items loop
                } // End LazyColumn

                // --- Dialog Close Button ---
                if(renamingCatalogId == null && !showAddCatalogInput) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End){
                        TextButton(onClick = onDismiss) { Text("Close") }
                    }
                }
            } // End Main Column
        } // End Card

        // --- Delete Confirmation Dialog ---
        if (showDeleteConfirmation != null) {
            val catalogToDelete = catalogs[showDeleteConfirmation]
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = null },
                icon = { Icon(Icons.Default.Warning, contentDescription=null) },
                title = { Text("Confirm Deletion") },
                text = { Text("Delete catalog \"${catalogToDelete?.name ?: ""}\"?\nThis deletes all its products and multipliers and cannot be undone.") },
                confirmButton = { Button( onClick = { if(showDeleteConfirmation != null) onDeleteCatalog(showDeleteConfirmation!!); showDeleteConfirmation = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete", color = MaterialTheme.colorScheme.onError) } },
                dismissButton = { TextButton(onClick = { showDeleteConfirmation = null }) { Text("Cancel") } }
            )
        } // End Delete Confirmation
    } // End Main Dialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCompanyNameDialog(
    catalog: Catalog,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var companyName by remember { mutableStateOf(catalog.companyName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Edit Company Name", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                    Button(onClick = { onConfirm(companyName.trim()) }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
// --- Helper Composable for a single Catalog row in the list ---
@Composable
private fun CatalogListItem(
    catalog: Catalog,
    isActive: Boolean,
    isRenaming: Boolean,
    canDelete: Boolean,
    renameValue: String,
    isErrorRenaming: Boolean,
    interactionEnabled: Boolean,
    renameFocusRequester: FocusRequester,
    // Callbacks
    onSelect: () -> Unit,
    onStartRename: () -> Unit,
    onRenameValueChange: (String) -> Unit,
    onConfirmRename: () -> Unit,
    onCancelRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit, // *** ADDED onShare Callback ***
    onUpdateCompanyName: (String, String) -> Unit // *** ADD THIS NEW PARAMETER ***
) {
    val focusManager = LocalFocusManager.current
    val rowModifier = Modifier
        .fillMaxWidth()
        .clickable(enabled = !isRenaming && interactionEnabled) { onSelect() }
        .padding(vertical = 8.dp, horizontal = 4.dp)
    var showCompanyNameDialog by remember { mutableStateOf(false) }
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        // Active Indicator
        if (isActive) { Icon(Icons.Filled.CheckCircle, "Active Catalog", tint=MaterialTheme.colorScheme.primary, modifier=Modifier.size(24.dp)) }
        else { Spacer(modifier = Modifier.width(24.dp)) }
        Spacer(Modifier.width(8.dp))

        // Name / Rename TextField
        // Name / Rename TextField
        if (isRenaming) {
            OutlinedTextField( value = renameValue, onValueChange = onRenameValueChange, modifier = Modifier.weight(1f).focusRequester(renameFocusRequester),
                isError = isErrorRenaming, singleLine = true, label = { Text("New Name") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Words),
                keyboardActions = KeyboardActions(onDone = { onConfirmRename(); focusManager.clearFocus() }),
                trailingIcon = { Row { IconButton(onClick = onConfirmRename, enabled = renameValue.isNotBlank()) { Icon(Icons.Filled.Check, "Confirm Rename") }; IconButton(onClick = onCancelRename) { Icon(Icons.Filled.Cancel, "Cancel Rename") } } } )
        } else {
            Text( text = catalog.name, modifier = Modifier.weight(1f), fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis )

            // --- ACTION BUTTONS (Show only when not renaming) ---
            var showCompanyNameDialog by remember { mutableStateOf(false) }

            // Share Button
            IconButton(onClick = onShare, enabled = interactionEnabled) {
                Icon(Icons.Filled.Share, contentDescription = "Share ${catalog.name}")
            }

            // Company Name Button
            IconButton(onClick = { showCompanyNameDialog = true }, enabled = interactionEnabled) {
                Icon(Icons.Filled.Business, contentDescription = "Edit Company Name for ${catalog.name}")
            }

            // Rename Button
            IconButton(onClick = onStartRename, enabled = interactionEnabled) {
                Icon(Icons.Filled.Edit, "Rename ${catalog.name}")
            }

            // Delete Button
            IconButton(
                onClick = onDelete,
                enabled = canDelete && interactionEnabled,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete ${catalog.name}"
                )
            }

            // Company Name Edit Dialog
            if (showCompanyNameDialog) {
                EditCompanyNameDialog(
                    catalog = catalog,
                    onDismiss = { showCompanyNameDialog = false },
                    onConfirm = { newCompanyName ->
                        onUpdateCompanyName(catalog.id, newCompanyName)
                        showCompanyNameDialog = false
                    }
                )
            }
        } // End else block/ End Row
} // Company Name Edit Dialog
        if (showCompanyNameDialog) {
            EditCompanyNameDialog(
                catalog = catalog,
                onDismiss = { showCompanyNameDialog = false },
                onConfirm = { newCompanyName ->
                    onUpdateCompanyName(catalog.id, newCompanyName)
                    showCompanyNameDialog = false
                }
            )
        }}