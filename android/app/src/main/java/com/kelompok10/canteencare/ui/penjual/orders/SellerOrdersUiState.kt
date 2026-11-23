package com.kelompok10.canteencare.ui.penjual.orders

import com.kelompok10.canteencare.domain.model.Order
import com.kelompok10.canteencare.domain.model.OrderStatus

/**
 * Tab layar Pesanan Masuk. Tab Riwayat menggabungkan pesanan Selesai dan Ditolak,
 * karena penjual hanya perlu melihat dua status final itu tanpa aksi lanjutan.
 */
enum class SellerOrderTab(val label: String, val statuses: Set<OrderStatus>) {
    MASUK("Masuk", setOf(OrderStatus.MENUNGGU)),
    DIMASAK("Dimasak", setOf(OrderStatus.SEDANG_DIMASAK)),
    SIAP("Siap Diambil", setOf(OrderStatus.SIAP_DIAMBIL)),
    RIWAYAT("Riwayat", setOf(OrderStatus.SELESAI, OrderStatus.DITOLAK));

    /** Tab ini berisi pesanan yang masih berjalan dan perlu auto-refresh. */
    val isActive: Boolean get() = statuses.any { it.isActive }
}

/**
 * State layar Pesanan Masuk. Semua pesanan stan diambil sekaligus, lalu dibagi per tab,
 * sehingga badge jumlah pesanan masuk selalu akurat di tab mana pun.
 */
data class SellerOrdersUiState(
    val selectedTab: SellerOrderTab = SellerOrderTab.MASUK,
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
) {
    val visibleOrders: List<Order> get() = orders.filter { it.status in selectedTab.statuses }
    val pendingCount: Int get() = orders.count { it.status == OrderStatus.MENUNGGU }
    val readyCount: Int get() = orders.count { it.status == OrderStatus.SIAP_DIAMBIL }
}
