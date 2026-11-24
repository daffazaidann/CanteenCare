package com.kelompok10.canteencare.ui.penjual.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import kotlinx.coroutines.delay
import java.time.OffsetDateTime

private const val AUTO_REFRESH_MILLIS = 10_000L

@Composable
fun SellerOrdersRoute(
    onOrderClick: (orderId: Int) -> Unit,
    viewModel: SellerOrdersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LifecycleResumeEffect(Unit) {
        viewModel.onScreenResumed()
        onPauseOrDispose { }
    }

    // Auto-refresh tiap 10 detik selama tab berisi pesanan aktif dan layar terlihat
    LaunchedEffect(uiState.selectedTab) {
        if (!uiState.selectedTab.isActive) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(AUTO_REFRESH_MILLIS)
                viewModel.autoRefresh()
            }
        }
    }

    SellerOrdersScreen(
        uiState = uiState,
        onTabSelected = viewModel::onTabSelected,
        onRefresh = viewModel::onRefresh,
        onRetry = viewModel::onScreenResumed,
        onOrderClick = onOrderClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerOrdersScreen(
    uiState: SellerOrdersUiState,
    onTabSelected: (SellerOrderTab) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOrderClick: (Int) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Pesanan Masuk") }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            PrimaryTabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                SellerOrderTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.label)
                                val badgeCount = when (tab) {
                                    SellerOrderTab.MASUK -> uiState.pendingCount
                                    SellerOrderTab.SIAP -> uiState.readyCount
                                    else -> 0
                                }
                                if (badgeCount > 0) {
                                    Badge(Modifier.padding(start = 6.dp)) { Text(badgeCount.toString()) }
                                }
                            }
                        },
                    )
                }
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
                            item {
                                EmptyView(
                                    "Tidak ada pesanan di tab ${uiState.selectedTab.label.lowercase()}.",
                                    Icons.AutoMirrored.Filled.ReceiptLong,
                                    Modifier.fillParentMaxSize(),
                                )
                            }
                        }
                        items(uiState.visibleOrders, key = { it.id }) { order ->
                            SellerOrderCard(order, onClick = { onOrderClick(order.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerOrderCard(order: Order, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(order.buyerName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${order.orderNumber} · ${WibTime.formatDateTime(order.createdAt)}",
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
                overflow = TextOverflow.Ellipsis,
            )
            order.note?.let {
                Text("Catatan: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.paymentMethod.label, style = MaterialTheme.typography.labelMedium)
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
private fun SellerOrdersScreenPreview() {
    val items = listOf(OrderItem(1, "Mie Ayam Solo", 20000, 2, Nutrition(650.0, 85.0, 30.0, 25.0, 1800.0, 6.0)))
    val orders = listOf(
        Order(
            3, "CC-20261118-9308", "Daffa", 1, "Dapur Gapura", OrderStatus.MENUNGGU, PaymentMethod.QRIS,
            "Sambal dipisah", null, OffsetDateTime.parse("2026-11-18T09:56:00Z"), null, items,
        ),
    )
    CanteenCareTheme {
        SellerOrdersScreen(
            uiState = SellerOrdersUiState(
                orders = orders,
                isLoading = false,
                selectedTab = SellerOrderTab.MASUK,
            ),
            onTabSelected = {}, onRefresh = {}, onRetry = {}, onOrderClick = {},
        )
    }
}
