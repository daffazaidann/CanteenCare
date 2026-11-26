package com.kelompok10.canteencare.ui.admin.sellers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelompok10.canteencare.domain.model.Seller
import com.kelompok10.canteencare.domain.model.Vendor
import com.kelompok10.canteencare.ui.components.CanteenTopAppBar
import com.kelompok10.canteencare.ui.components.EmptyView
import com.kelompok10.canteencare.ui.components.ErrorView
import com.kelompok10.canteencare.ui.components.FoodImage
import com.kelompok10.canteencare.ui.components.ImageKind
import com.kelompok10.canteencare.ui.components.LoadingView
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme

@Composable
fun AdminSellersRoute(
    onAddSeller: () -> Unit,
    viewModel: AdminSellersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }
    AdminSellersScreen(uiState = uiState, onRetry = viewModel::load, onAddSeller = onAddSeller)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSellersScreen(uiState: AdminSellersUiState, onRetry: () -> Unit, onAddSeller: () -> Unit) {
    Scaffold(
        topBar = { CanteenTopAppBar(title = "Penjual & Stan") },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSeller,
                icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                text = { Text("Tambah Penjual") },
            )
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when {
            uiState.isLoading -> LoadingView(modifier)
            uiState.errorMessage != null && uiState.sellers.isEmpty() -> ErrorView(uiState.errorMessage, onRetry, modifier)
            uiState.sellers.isEmpty() -> EmptyView("Belum ada penjual.", Icons.Filled.Storefront, modifier)
            else -> LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.sellers, key = { it.id }) { SellerCard(it) }
            }
        }
    }
}

@Composable
private fun SellerCard(seller: Seller) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FoodImage(seller.vendor.imageUrl, ImageKind.STALL, Modifier.size(56.dp))
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(seller.vendor.name, style = MaterialTheme.typography.titleMedium)
                Text("${seller.fullName} · ${seller.email}", style = MaterialTheme.typography.bodySmall)
                seller.vendor.openHours?.let {
                    Text(
                        "Buka $it",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminSellersScreenPreview() {
    CanteenCareTheme {
        AdminSellersScreen(
            uiState = AdminSellersUiState(
                isLoading = false,
                sellers = listOf(
                    Seller(2, "Pak Gapura", "gapura@canteencare.id", Vendor(1, "Dapur Gapura", null, null, "07.00 - 16.00")),
                ),
            ),
            onRetry = {}, onAddSeller = {},
        )
    }
}
