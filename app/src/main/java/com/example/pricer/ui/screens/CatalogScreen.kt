package com.example.pricer.ui.screens

// ... (Keep all existing imports) ...
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Ensure LocalContext is imported
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pricer.data.model.DialogState
import com.example.pricer.data.model.Product
import com.example.pricer.data.model.ProductSortCriteria
import com.example.pricer.ui.components.ProductRow // Import the row component
import com.example.pricer.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.Toast // Ensure Toast is imported

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CatalogScreen(
    viewModel: MainViewModel,
    onNavigateToQuotePreview: () -> Unit,
) {
    // --- State Collection ---
    val quantities by viewModel.itemQuantities.collectAsStateWithLifecycle()
    val currentCatalog by viewModel.activeCatalog.collectAsStateWithLifecycle()
    val sortCriteria by viewModel.productSortCriteria.collectAsStateWithLifecycle()
    val productAssignments by viewModel.productMultiplierAssignments.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val groupedProducts by viewModel.groupedSortedFilteredProducts.collectAsStateWithLifecycle()

    // --- UI Helpers ---
    val focusManager = LocalFocusManager.current
    var showSortMenu by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { categories.size })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var productToDeleteConfirm by remember { mutableStateOf<Product?>(null) } // State for delete confirmation

    // --- LaunchedEffect ---
    LaunchedEffect(categories) { /* ... Pager reset logic ... */
        if (categories.isNotEmpty()) { val targetPage = pagerState.currentPage.coerceIn(0, categories.size - 1);
            if (pagerState.currentPage != targetPage) { Log.d("CatalogScreen", "Snapping pager from ${pagerState.currentPage} to $targetPage."); pagerState.scrollToPage(targetPage) }
        }
        else if (pagerState.currentPage != 0) { Log.d("CatalogScreen", "Snapping pager to 0."); pagerState.scrollToPage(0) }
    }

    Scaffold(
        topBar = { /* ... TopAppBar with ClearAll button correctly placed ... */
            TopAppBar(
                title = {
                    Text(currentCatalog?.name ?: "Catalog") },
                modifier = Modifier.padding(start = 28.dp),
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true
                        }
                        )
                        {
                            Icon(Icons.Default.Sort, "Sort")};
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false })
                    {
                        ProductSortCriteria.entries.forEach { criteria -> DropdownMenuItem( text={Text(criteria.name.replace("_"," ").lowercase().replaceFirstChar{it.titlecase()})},
                        onClick = {viewModel.setSortCriteria(criteria); showSortMenu = false},
                        leadingIcon=if(sortCriteria==criteria)
                        {
                            {
                                Icon(Icons.Default.Check,"Selected") } } else null) } } };
                    IconButton(onClick =
                        {
                            viewModel.clearAllQuantitiesAndAssignments(); coroutineScope.launch { Toast.makeText(context,
                        "Quantities Cleared", Toast.LENGTH_SHORT).show() } })
                    {
                        Icon(Icons.Default.RestartAlt, "Clear Quantities") };
                    IconButton(onClick =
                        { focusManager.clearFocus(); viewModel.showDialog(DialogState.MANAGE_CATALOGS)}) {
                        Icon(Icons.Default.FolderCopy, "Manage Catalogs")};
                    IconButton(onClick = { focusManager.clearFocus(); viewModel.showDialog(DialogState.MANAGE_MULTIPLIERS)}) {
                        Icon(Icons.Filled.Functions, "Manage Multipliers")} } )
            IconButton(onClick = {
                focusManager.clearFocus()
                viewModel.showProspectsScreen() // Tell ViewModel to change mode
            },
                modifier = Modifier.padding(top = 8.dp,)
                ) { Icon(Icons.Default.People, contentDescription = "View Prospects") // Example icon }
            }
        },

        floatingActionButton = { /* ... FABs ... */
            Row(horizontalArrangement=Arrangement.spacedBy(16.dp),
                verticalAlignment=Alignment.CenterVertically){ FloatingActionButton(onClick={focusManager.clearFocus();viewModel.showDialog(DialogState.ADD_EDIT_PRODUCT,null)}, elevation=FloatingActionButtonDefaults.elevation(6.dp)){ Icon(Icons.Default.Add,"Add Product")}; val hasQ=quantities.any{(_,q)->(q.toIntOrNull()?:0)>0}; if(hasQ){ ExtendedFloatingActionButton(icon={Icon(Icons.Default.ReceiptLong,null)},text={Text("Preview Quote")},onClick={focusManager.clearFocus();onNavigateToQuotePreview()}, expanded=true) } }
        }
    ) { scaffoldPadding ->

        Column( modifier = Modifier.padding(scaffoldPadding).fillMaxSize() ) {
            // --- Search Field ---
            OutlinedTextField( value = searchQuery, onValueChange = { viewModel.onSearchQueryChange(it) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), label = { Text("Search Products") }, placeholder = { Text("Enter product name...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if(searchQuery.isNotEmpty()){ IconButton(onClick={viewModel.onSearchQueryChange("")}){Icon(Icons.Default.Clear, "Clear Search")}} }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch={focusManager.clearFocus()}), singleLine = true, shape = MaterialTheme.shapes.extraLarge )

            // --- Tab Row & Pager Section ---
            val currentSelectedTabIndex = remember(pagerState.currentPage, categories.size) { if (categories.isEmpty()) 0 else pagerState.currentPage.coerceIn(0, categories.size - 1) }

            if (categories.isNotEmpty()) {
                // --- Tab Row ---
                ScrollableTabRow(selectedTabIndex = currentSelectedTabIndex, edgePadding = 0.dp, modifier = Modifier.fillMaxWidth()) {
                    categories.forEachIndexed { index, categoryName ->
                        Tab(selected = currentSelectedTabIndex == index, onClick = { if (index < pagerState.pageCount) { coroutineScope.launch { pagerState.animateScrollToPage(index) } } }, text = { Text(categoryName) })
                    }
                }
                Divider()

                // --- Pager ---
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f), key = { pageIndex -> categories.getOrNull(pageIndex) ?: pageIndex }) { pageIndex ->
                    val categoryNameForPage = categories.getOrNull(pageIndex)
                    val productsInCategory = categoryNameForPage?.let { groupedProducts[it] } ?: emptyList()

                    if (categoryNameForPage == null) { Box(Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center){ Text("Loading page...")} }
                    else if (productsInCategory.isEmpty()) {
                        // Empty state for this category page
                        Box(Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val emptyMsg = if (searchQuery.isNotBlank()) "No matches for \"$searchQuery\" in $categoryNameForPage." else "No products in category \"$categoryNameForPage\"."
                                Icon(imageVector = if(searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Outlined.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text(emptyMsg, style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        // LazyColumn with products
                        LazyColumn(Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp)) {
                            items(items = productsInCategory,
                                key = { p -> "cat_${categoryNameForPage}_${p.id}" }) { product ->
                                val assignments = productAssignments[product.id]
                                val summary = assignments?.keys?.mapNotNull { mid->currentCatalog?.multipliers?.find{it.id==mid}?.name}?.joinToString(", ")

                                // --- *** Call ProductRow with onDeleteProduct *** ---
                                ProductRow(
                                    product = product,
                                    quantity = quantities[product.id] ?: "",
                                    category = product.category,
                                    assignedMultiplierSummary = summary,
                                    onQuantityChange = { qty -> viewModel.updateQuantity(product.id, qty) },
                                    onAssignMultiplierClick = { p -> focusManager.clearFocus(); viewModel.showDialog(DialogState.ASSIGN_MULTIPLIER, p) },
                                    onRowClick = { p -> focusManager.clearFocus(); viewModel.showDialog(DialogState.ADD_EDIT_PRODUCT, p) },
                                    onDeleteProduct = { productToDeleteConfirm = product } // Pass the callback
                                )
                                // --- *** End ProductRow Call *** ---
                            } // End items
                        } // End LazyColumn
                    } // End else (products exist)
                } // End Pager
            } else {
                // --- Handle case where catalog is loaded but has NO categories ---
                Box(modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (searchQuery.isNotBlank()) {
                            Icon(Icons.Default.SearchOff, null, Modifier.size(48.dp),
                                tint=MaterialTheme.colorScheme.onSurfaceVariant);
                            Spacer(Modifier.height(16.dp));
                            Text("No products found matching \"$searchQuery\".",
                                style=MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center)

                        } else {
                            Icon(Icons.Outlined.Info, null, Modifier.size(48.dp),
                                tint=MaterialTheme.colorScheme.onSurfaceVariant);
                            Spacer(Modifier.height(16.dp)); Text("Catalog is empty.",
                                style=MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center); Text("Tap '+' to add a product.",
                                style=MaterialTheme.typography.bodyMedium,
                                color=MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center)
                        }
                    }
                }
            } // End else (categories exist check)

        } // End Main Column

    } // End Scaffold

    // --- Delete Product Confirmation Dialog --- (Keep as is)
    if (productToDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { productToDeleteConfirm = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete product \"${productToDeleteConfirm?.name ?: ""}\"? This cannot be undone.") },
            confirmButton = {
                Button( onClick = { viewModel.deleteProduct(productToDeleteConfirm!!.id); productToDeleteConfirm = null; Toast.makeText(context, "Product deleted", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) ) { Text("Delete", color = MaterialTheme.colorScheme.onError) }
            },
            dismissButton = { TextButton(onClick = { productToDeleteConfirm = null }) { Text("Cancel") } }
        )
    }

} // End CatalogScreen Composable