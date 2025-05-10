package com.example.pricer.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.pricer.viewmodel.MainViewModel

/**
 * Container that manages tab navigation between ProspectsScreen, CustomersScreen, and SubcontractorsScreen
 */
@Composable
fun ProspectsTabsContainer(
    viewModel: MainViewModel,
    onNavigateBackToCatalog: () -> Unit,
    onRecordClick: (String) -> Unit,
    initialTab: Int = 0
) {
    var selectedTabIndex by remember { mutableIntStateOf(initialTab.coerceIn(0, 2)) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Prospects") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Customers") }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("Contractors") }
            )
        }

        // Content based on selected tab
        when (selectedTabIndex) {
            0 -> {
                ProspectsScreen(
                    viewModel = viewModel,
                    onNavigateBackToCatalog = onNavigateBackToCatalog,
                    onProspectClick = onRecordClick
                )
            }
            1 -> {
                CustomersScreen(
                    viewModel = viewModel,
                    onNavigateBackToCatalog = onNavigateBackToCatalog,
                    onCustomerClick = onRecordClick
                )
            }
            2 -> {
                SubcontractorsScreen(
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBackToCatalog
                )
            }
        }
    }
}