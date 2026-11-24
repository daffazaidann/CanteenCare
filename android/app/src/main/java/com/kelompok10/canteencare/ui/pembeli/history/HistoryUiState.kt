package com.kelompok10.canteencare.ui.pembeli.history

import com.kelompok10.canteencare.domain.model.Order
import com.kelompok10.canteencare.domain.model.OrderStatus

/** Dua bagian layar Pesanan: pesanan yang masih berjalan dan riwayat yang sudah final. */
enum class HistorySection(val label: String) {
    ACTIVE("Dalam Proses"),
    HISTORY("Riwayat"),
}

/**
 * State layar Pesanan.
 * [isLoading] untuk muat pertama, [isRefreshing] untuk tarik-untuk-refresh.
 */
data class HistoryUiState(
    val selectedSection: HistorySection = HistorySection.ACTIVE,
    /** Filter chip di tab Riwayat. null berarti Semua. */
    val selectedStatus: OrderStatus? = null,
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
) {
    val activeOrders: List<Order> get() = orders.filter { it.status.isActive }

    val historyOrders: List<Order>
        get() = orders.filter { it.status.isHistory && (selectedStatus == null || it.status == selectedStatus) }

    val visibleOrders: List<Order>
        get() = if (selectedSection == HistorySection.ACTIVE) activeOrders else historyOrders
}
