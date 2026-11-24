package com.kelompok10.canteencare.domain.model

import java.time.OffsetDateTime

enum class OrderStatus(val label: String) {
    MENUNGGU("Menunggu Konfirmasi"),
    SEDANG_DIMASAK("Sedang Dimasak"),
    SIAP_DIAMBIL("Siap Diambil"),
    SELESAI("Selesai"),
    DITOLAK("Ditolak");

    /** Pesanan masih berjalan dan perlu dipantau pembeli atau penjual. */
    val isActive: Boolean
        get() = this == MENUNGGU || this == SEDANG_DIMASAK || this == SIAP_DIAMBIL

    /** Pesanan sudah final dan tampil di tab Riwayat. */
    val isHistory: Boolean
        get() = this == SELESAI || this == DITOLAK

    /** Pesanan yang sudah diterima penjual dan dihitung ke gizi harian. */
    val isCounted: Boolean
        get() = this == SEDANG_DIMASAK || this == SIAP_DIAMBIL || this == SELESAI

    companion object {
        /**
         * Membaca status dari JSON. DIKONFIRMASI adalah nama lama sebelum alur
         * Dalam Proses, jadi tetap dipetakan ke SEDANG_DIMASAK.
         */
        fun fromApi(value: String): OrderStatus = when (value) {
            "MENUNGGU" -> MENUNGGU
            "SEDANG_DIMASAK", "DIKONFIRMASI" -> SEDANG_DIMASAK
            "SIAP_DIAMBIL" -> SIAP_DIAMBIL
            "SELESAI", "DONE" -> SELESAI
            "DITOLAK" -> DITOLAK
            else -> MENUNGGU
        }
    }
}

enum class PaymentMethod(val label: String) {
    TUNAI("Tunai"),
    QRIS("QRIS"),
}

/** Item pesanan: snapshot nama, harga, dan gizi per porsi saat pesanan dibuat. */
data class OrderItem(
    val menuId: Int,
    val menuName: String,
    override val price: Int,
    override val quantity: Int,
    override val nutrition: Nutrition,
) : LineItem

/**
 * Pesanan. Sengaja TIDAK menyimpan total harga dan total gizi dari server:
 * total selalu dihitung ulang di aplikasi oleh NutritionCalculator dari [items].
 */
data class Order(
    val id: Int,
    val orderNumber: String,
    val buyerName: String,
    val vendorId: Int,
    val vendorName: String,
    val status: OrderStatus,
    val paymentMethod: PaymentMethod,
    val note: String?,
    val rejectionReason: String?,
    val createdAt: OffsetDateTime,
    val resolvedAt: OffsetDateTime?,
    val items: List<OrderItem>,
)
