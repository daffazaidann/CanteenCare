package com.kelompok10.canteencare.ui.penjual.order

import com.kelompok10.canteencare.domain.model.Order

/**
 * State detail pesanan untuk penjual. Dialog tolak dan alasannya disimpan di sini,
 * jadi tidak hilang saat layar diputar.
 */
data class SellerOrderDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val order: Order? = null,
    val totalPrice: Int = 0,
    val isProcessing: Boolean = false,
    val showRejectDialog: Boolean = false,
    val rejectReason: String = "",
    val showCompleteDialog: Boolean = false,
    /** Pesan singkat untuk snackbar, misalnya hasil konfirmasi atau error stok. */
    val message: String? = null,
)
