package com.kelompok10.canteencare.ui.pembeli.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.kelompok10.canteencare.ui.components.CanteenTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelompok10.canteencare.core.util.Formatters
import com.kelompok10.canteencare.domain.model.Menu
import com.kelompok10.canteencare.domain.model.MenuCategory
import com.kelompok10.canteencare.domain.model.MenuStatus
import com.kelompok10.canteencare.domain.model.Nutrition
import com.kelompok10.canteencare.domain.model.Vendor
import com.kelompok10.canteencare.ui.components.CartButton
import com.kelompok10.canteencare.ui.components.EmptyView
import com.kelompok10.canteencare.ui.components.ErrorView
import com.kelompok10.canteencare.ui.components.FoodImage
import com.kelompok10.canteencare.ui.components.ImageKind
import com.kelompok10.canteencare.ui.components.LoadingView
import com.kelompok10.canteencare.ui.components.imageKind
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme

@Composable
fun HomeRoute(
    onVendorClick: (vendorId: Int) -> Unit,
    onMenuClick: (vendorId: Int, menuId: Int) -> Unit,
    onCartClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onRetryVendors = viewModel::loadVendors,
        onRetrySearch = viewModel::retrySearch,
        onVendorClick = onVendorClick,
        onMenuClick = onMenuClick,
        onCartClick = onCartClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onQueryChange: (String) -> Unit,
    onRetryVendors: () -> Unit,
    onRetrySearch: () -> Unit,
    onVendorClick: (Int) -> Unit,
    onMenuClick: (vendorId: Int, menuId: Int) -> Unit,
    onCartClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            CanteenTopAppBar(
                title = {
                    Column {
                        Text("Selamat datang, ${uiState.userName}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Mau makan apa hari ini?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = { CartButton(itemCount = uiState.cartCount, onClick = onCartClick) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(query = uiState.query, onQueryChange = onQueryChange)
            if (uiState.isSearchMode) {
                SearchResults(uiState, onRetrySearch, onMenuClick)
            } else {
                VendorGrid(uiState, onRetryVendors, onVendorClick)
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Cari menu di semua stan") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Hapus pencarian")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun VendorGrid(uiState: HomeUiState, onRetry: () -> Unit, onVendorClick: (Int) -> Unit) {
    when {
        uiState.isLoadingVendors -> LoadingView()
        uiState.vendorsError != null -> ErrorView(uiState.vendorsError, onRetry)
        uiState.vendors.isEmpty() -> EmptyView("Belum ada stan yang buka.", Icons.Filled.Storefront)
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Stan Kantin", style = MaterialTheme.typography.titleMedium)
            }
            items(uiState.vendors, key = { it.id }) { vendor ->
                VendorCard(vendor = vendor, onClick = { onVendorClick(vendor.id) })
            }
        }
    }
}

@Composable
private fun VendorCard(vendor: Vendor, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        FoodImage(
            imageUrl = vendor.imageUrl,
            kind = ImageKind.STALL,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
        )
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(vendor.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                vendor.description.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            vendor.openHours?.let { hours ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        hours,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onMenuClick: (vendorId: Int, menuId: Int) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (uiState.isSearching) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        when {
            uiState.searchError != null -> ErrorView(uiState.searchError, onRetry)
            uiState.showNoResults ->
                EmptyView("Menu \"${uiState.query.trim()}\" tidak ditemukan.", Icons.Filled.SearchOff)
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.searchResults, key = { it.id }) { menu ->
                    MenuSearchItem(menu = menu, onClick = { onMenuClick(menu.vendorId, menu.id) })
                }
            }
        }
    }
}

@Composable
private fun MenuSearchItem(menu: Menu, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FoodImage(menu.imageUrl, menu.category.imageKind(), Modifier.size(64.dp))
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(menu.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    menu.vendorName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${Formatters.rupiah(menu.price)} · ${Formatters.nutrient(menu.nutrition?.calories ?: 0.0, "kkal")}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private val previewVendors = listOf(
    Vendor(1, "Dapur Gapura", "Aneka mie dan nasi rumahan", null, "07.00 - 16.00"),
    Vendor(2, "Warung Bu Sri", "Masakan Jawa dan lauk pauk", null, "07.00 - 15.00"),
)

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    CanteenCareTheme {
        HomeScreen(
            uiState = HomeUiState(userName = "Daffa", cartCount = 3, isLoadingVendors = false, vendors = previewVendors),
            onQueryChange = {}, onRetryVendors = {}, onRetrySearch = {},
            onVendorClick = {}, onMenuClick = { _, _ -> }, onCartClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeSearchPreview() {
    CanteenCareTheme {
        HomeScreen(
            uiState = HomeUiState(
                userName = "Daffa",
                isLoadingVendors = false,
                query = "es",
                searchResults = listOf(
                    Menu(
                        4, 1, "Dapur Gapura", "Es Teh Manis", null, MenuCategory.MINUMAN, 4000, 50, null,
                        MenuStatus.APPROVED, Nutrition(120.0, 30.0, 0.0, 0.0, 10.0, 30.0), true,
                    ),
                ),
            ),
            onQueryChange = {}, onRetryVendors = {}, onRetrySearch = {},
            onVendorClick = {}, onMenuClick = { _, _ -> }, onCartClick = {},
        )
    }
}
