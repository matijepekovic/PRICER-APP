package com.example.pricer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight // For the row indicator
import androidx.compose.material.icons.outlined.FolderZip // Example icon for empty state
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember // Import remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pricer.data.model.ProspectRecord
import com.example.pricer.data.model.ProspectStatus // <<< IMPORT ProspectStatus
import com.example.pricer.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
// Optional: import androidx.compose.ui.platform.LocalLifecycleOwner
// Optional: import androidx.lifecycle.Lifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProspectsScreen(
    viewModel: MainViewModel,
    onNavigateBackToCatalog: () -> Unit,
    onProspectClick: (String) -> Unit // Callback with ProspectRecord ID
) {
    // --- Corrected State Collection ---
    // The StateFlow in ViewModel is initialized with emptyList(), so initialValue here might be redundant
    // but can help with immediate composition if the flow is slow to emit for the first time.
    val allProspectRecords by viewModel.prospectRecords
        .collectAsStateWithLifecycle(initialValue = emptyList<ProspectRecord>()) // Correct parameter name

    // Filter for actual prospects and sort; wrap in remember to avoid re-calculation on every recomposition
    val prospectList = remember(allProspectRecords) {
        allProspectRecords
            .filter { it.status == ProspectStatus.PROSPECT } // Use imported enum
            .sortedByDescending { it.dateCreated }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prospects") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBackToCatalog) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Catalog")
                    }
                }
                // TODO: Add actions like filter, sort for prospects later
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (prospectList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) { // Added padding
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Inbox, // Example icon
                            contentDescription = "No Prospects",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("No prospects saved yet.", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp)) // Added spacer
                        Text(
                            "Finalize quotes to create prospect records.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center // Added textAlign for better look
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp) // Added some bottom padding
                ) {
                    items(items = prospectList, key = { it.id }) { prospect ->
                        ProspectRow(
                            prospectRecord = prospect,
                            onClick = { onProspectClick(prospect.id) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ProspectRow(prospectRecord: ProspectRecord, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                prospectRecord.customerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp)) // Small spacer
            Text(
                "Created: ${dateFormat.format(Date(prospectRecord.dateCreated))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            prospectRecord.customerEmail?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(2.dp)) // Small spacer
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        // TODO: Add an icon or indicator for reminder status if set (e.g., Alarm icon)
        Icon(Icons.Default.ChevronRight, contentDescription = "View Details")
    }
}