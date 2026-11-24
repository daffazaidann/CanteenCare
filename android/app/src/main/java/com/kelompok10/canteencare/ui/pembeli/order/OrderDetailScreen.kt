package com.kelompok10.canteencare.ui.pembeli.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kelompok10.canteencare.core.util.WibTime
import com.kelompok10.canteencare.domain.calculator.NutritionCalculator
import com.kelompok10.canteencare.domain.model.Nutrition
import com.kelompok10.canteencare.domain.model.Order
import com.kelompok10.canteencare.domain.model.OrderItem
import com.kelompok10.canteencare.domain.model.OrderStatus
import com.kelompok10.canteencare.domain.model.PaymentMethod
import com.kelompok10.canteencare.ui.components.ErrorView
import com.kelompok10.canteencare.ui.components.InfoLine
import com.kelompok10.canteencare.ui.components.NoticeCard
import com.kelompok10.canteencare.ui.components.OrderItemsCard
import com.kelompok10.canteencare.ui.components.LoadingView
import com.kelompok10.canteencare.ui.components.NutritionTable
import com.kelompok10.canteencare.ui.components.OrderProgressSteps
import com.kelompok10.canteencare.ui.components.StatusChip
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme
import kotlinx.coroutines.delay
import java.time.OffsetDateTime

@Composable
fun OrderDetailRoute(
    onBack: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-refresh tiap 10 detik selama pesanan masih aktif dan layar sedang terlihat.
    // Begitu status menjadi Selesai atau Ditolak, effect ini berhenti sendiri.
    LaunchedEffect(uiState.order?.status) {
        if (uiState.order?.status?.isActive != true) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(AUTO_REFRESH_MILLIS)
                viewModel.refresh()
            }
        }
    }

    OrderDetailScreen(uiState = uiState, onBack = onBack, onRetry = viewModel::load)
}

private const val AUTO_REFRESH_MILLIS = 10_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(uiState: OrderDetailUiState, onBack: () -> Unit, onRetry: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Pesanan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        val order = uiState.order
        when {
            uiState.isLoading && order == null -> LoadingView(modifier)
            order == null -> ErrorView(uiState.errorMessage ?: "Pesanan tidak ditemukan.", onRetry, modifier)
            else -> OrderDetailContent(order, uiState.totalPrice, uiState.totalNutrition, modifier)
        }
    }
}

@Composable
private fun OrderDetailContent(order: Order, totalPrice: Int, totalNutrition: Nutrition, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Ringkasan: status, nomor, stan, waktu
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusChip(order.status)
            Text(order.orderNumber, style = MaterialTheme.typography.headlineSmall)
            Text(order.vendorName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            InfoLine("Dipesan", WibTime.formatDateTime(order.createdAt))
            order.resolvedAt?.let {
                InfoLine(
                    if (order.status == OrderStatus.DITOLAK) "Ditolak" else "Selesai",
                    WibTime.formatDateTime(it),
                )
            }
            InfoLine("Pembayaran", order.paymentMethod.label)
            order.note?.let { InfoLine("Catatan", it) }
        }

        if (order.status != OrderStatus.DITOLAK) {
            OrderProgressSteps(order.status)
        }

        when (order.status) {
            OrderStatus.MENUNGGU -> NoticeCard(
                "Menunggu konfirmasi penjual. Kamu akan mendapat notifikasi saat pesanan diterima."
            )
            OrderStatus.SEDANG_DIMASAK -> NoticeCard("Pesanan diterima dan sedang dimasak.")
            OrderStatus.SIAP_DIAMBIL -> NoticeCard("Pesanan siap! Silakan ambil di ${order.vendorName}.")
            OrderStatus.SELESAI -> NoticeCard("Pesanan selesai. Terima kasih!")
            OrderStatus.DITOLAK -> NoticeCard("Alasan ditolak: ${order.rejectionReason ?: "-"}", isError = true)
        }

        OrderItemsCard(order.items, totalPrice)

        Text("Total Gizi Pesanan", style = MaterialTheme.typography.titleMedium)
        NutritionTable(totalNutrition)
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun OrderDetailScreenPreview() {
    val items = listOf(
        OrderItem(1, "Mie Ayam Solo", 20000, 1, Nutrition(650.0, 85.0, 30.0, 25.0, 1800.0, 6.0)),
        OrderItem(4, "Es Teh Manis", 4000, 2, Nutrition(120.0, 30.0, 0.0, 0.0, 10.0, 30.0)),
    )
    val order = Order(
        1, "CC-20261118-A3F9", "Daffa", 1, "Dapur Gapura", OrderStatus.SIAP_DIAMBIL, PaymentMethod.TUNAI,
        "Sambal dipisah", null, OffsetDateTime.parse("2026-11-18T09:05:00Z"), null, items,
    )
    CanteenCareTheme {
        OrderDetailScreen(
            uiState = OrderDetailUiState(
                isLoading = false,
                order = order,
                totalPrice = NutritionCalculator.calculateTotalPrice(items),
                totalNutrition = NutritionCalculator.calculateOrderNutrition(items),
            ),
            onBack = {}, onRetry = {},
        )
    }
}
