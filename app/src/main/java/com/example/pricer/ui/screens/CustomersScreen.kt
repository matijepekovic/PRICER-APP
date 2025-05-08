package com.example.pricer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pricer.data.model.ProspectRecord
import com.example.pricer.data.model.ProspectStatus
import com.example.pricer.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: MainViewModel,
    onNavigateBackToCatalog: () -> Unit,
    onCustomerClick: (String) -> Unit
) {
    // Use the filtered prospects instead of all prospects
    val filteredProspects by viewModel.filteredProspectRecords
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Get the search query from ViewModel
    val searchQuery by viewModel.prospectsSearchQuery
        .collectAsStateWithLifecycle()

    // Filter for CUSTOMER status only and sort by date
    val customersList = remember(filteredProspects) {
        filteredProspects
            .filter { it.status == ProspectStatus.CUSTOMER }
            .sortedByDescending { it.dateUpdated }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Back to Catalog") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBackToCatalog) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Catalog")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateProspectsSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateProspectsSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            if (customersList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Inbox,
                            contentDescription = "No Customers",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotBlank())
                                "No customers matching \"$searchQuery\""
                            else
                                "No customers saved yet.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        if (searchQuery.isBlank()) {
                            Text(
                                "Convert prospects to customers to see them here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                "Try using different search terms.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(items = customersList, key = { it.id }) { customer ->
                        CustomerRow(
                            customerRecord = customer,
                            onClick = { onCustomerClick(customer.id) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerRow(customerRecord: ProspectRecord, onClick: () -> Unit) {
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
                customerRecord.customerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Last updated: ${dateFormat.format(Date(customerRecord.dateUpdated))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            customerRecord.customerEmail?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "View Details")
    }
}