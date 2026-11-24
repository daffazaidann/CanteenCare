package com.kelompok10.canteencare.ui.pembeli.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelompok10.canteencare.core.util.Formatters
import com.kelompok10.canteencare.core.util.WibTime
import com.kelompok10.canteencare.domain.calculator.NutritionCalculator
import com.kelompok10.canteencare.domain.model.Nutrition
import com.kelompok10.canteencare.domain.model.Order
import com.kelompok10.canteencare.domain.model.OrderItem
import com.kelompok10.canteencare.domain.model.OrderStatus
import com.kelompok10.canteencare.domain.model.PaymentMethod
import com.kelompok10.canteencare.ui.components.EmptyView
import com.kelompok10.canteencare.ui.components.ErrorView
import com.kelompok10.canteencare.ui.components.LoadingView
import com.kelompok10.canteencare.ui.components.StatusChip
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme
import java.time.OffsetDateTime

@Composable
fun HistoryRoute(
    onOrderClick: (orderId: Int) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Muat ulang setiap kali layar kembali tampil, misalnya setelah membuka detail pesanan
    LifecycleResumeEffect(Unit) {
        viewModel.onScreenResumed()
        onPauseOrDispose { }
    }

    HistoryScreen(
        uiState = uiState,
        onSectionSelected = viewModel::onSectionSelected,
        onFilterSelected = viewModel::onFilterSelected,
        onRefresh = viewModel::onRefresh,
        onRetry = viewModel::onScreenResumed,
        onOrderClick = onOrderClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onSectionSelected: (HistorySection) -> Unit,
    onFilterSelected: (OrderStatus?) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOrderClick: (Int) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Pesanan Saya") }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            PrimaryTabRow(selectedTabIndex = uiState.selectedSection.ordinal) {
                HistorySection.entries.forEach { section ->
                    Tab(
                        selected = uiState.selectedSection == section,
                        onClick = { onSectionSelected(section) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = { Text(section.label) },
                    )
                }
            }
            if (uiState.selectedSection == HistorySection.HISTORY) {
                StatusFilterChips(uiState.selectedStatus, onFilterSelected)
            }
            when {
                uiState.isLoading -> LoadingView()
                uiState.errorMessage != null && uiState.orders.isEmpty() -> ErrorView(uiState.errorMessage, onRetry)
                else -> PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (uiState.visibleOrders.isEmpty()) {
                            // Di dalam LazyColumn supaya layar kosong tetap bisa ditarik untuk refresh
                            item {
                                EmptyView(
                                    emptyMessage(uiState.selectedSection, uiState.selectedStatus),
                                    Icons.AutoMirrored.Filled.ReceiptLong,
                                    Modifier.fillParentMaxSize(),
                                )
                            }
                        }
                        items(uiState.visibleOrders, key = { it.id }) { order ->
                            OrderHistoryCard(order = order, onClick = { onOrderClick(order.id) })
                        }
                    }
                }
            }
        }
    }
}

private fun emptyMessage(section: HistorySection, status: OrderStatus?): String = when {
    section == HistorySection.ACTIVE -> "Belum ada pesanan yang sedang diproses."
    status == null -> "Belum ada riwayat pesanan."
    else -> "Tidak ada pesanan berstatus ${status.label.lowercase()}."
}

@Composable
private fun StatusFilterChips(selected: OrderStatus?, onSelected: (OrderStatus?) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = selected == null, onClick = { onSelected(null) }, label = { Text("Semua") })
        listOf(OrderStatus.SELESAI, OrderStatus.DITOLAK).forEach { status ->
            FilterChip(selected = selected == status, onClick = { onSelected(status) }, label = { Text(status.label) })
        }
    }
}

@Composable
private fun OrderHistoryCard(order: Order, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(order.vendorName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        order.orderNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(order.status)
            }
            Text(
                order.items.joinToString { "${it.quantity}× ${it.menuName}" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    WibTime.formatDateTime(order.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    Formatters.rupiah(NutritionCalculator.calculateTotalPrice(order.items)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    val items = listOf(OrderItem(1, "Mie Ayam Solo", 20000, 1, Nutrition(650.0, 85.0, 30.0, 25.0, 1800.0, 6.0)))
    val orders = listOf(
        Order(
            2, "CC-20261118-9308", "Daffa", 1, "Dapur Gapura", OrderStatus.SIAP_DIAMBIL, PaymentMethod.QRIS,
            null, null, OffsetDateTime.parse("2026-11-18T09:56:00Z"), null, items,
        ),
        Order(
            1, "CC-20261117-1A2B", "Daffa", 2, "Warung Bu Sri", OrderStatus.DITOLAK, PaymentMethod.TUNAI,
            null, "Bahan habis", OffsetDateTime.parse("2026-11-17T05:00:00Z"), null, items,
        ),
    )
    CanteenCareTheme {
        HistoryScreen(
            uiState = HistoryUiState(
                orders = orders,
                isLoading = false,
                selectedSection = HistorySection.HISTORY,
            ),
            onSectionSelected = {}, onFilterSelected = {}, onRefresh = {}, onRetry = {}, onOrderClick = {},
        )
    }
}
