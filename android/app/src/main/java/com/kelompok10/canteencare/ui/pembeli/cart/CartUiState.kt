package com.kelompok10.canteencare.ui.pembeli.cart

import com.kelompok10.canteencare.domain.model.CartItem
import com.kelompok10.canteencare.domain.model.NutrientImpact
import com.kelompok10.canteencare.domain.model.Nutrition
import com.kelompok10.canteencare.domain.model.PaymentMethod

/**
 * State layar Keranjang.
 * [totalPrice] dan [totalNutrition] dihitung NutritionCalculator dari [items],
 * dan otomatis dihitung ulang setiap kali isi keranjang berubah.
 */
data class CartUiState(
    val isLoaded: Boolean = false,
    val items: List<CartItem> = emptyList(),
    val totalPrice: Int = 0,
    val totalNutrition: Nutrition = Nutrition.ZERO,
    val dailyImpact: List<NutrientImpact> = emptyList(),
    val paymentMethod: PaymentMethod = PaymentMethod.TUNAI,
    val note: String = "",
    val isPlacingOrder: Boolean = false,
    val errorMessage: String? = null,
    /** Terisi setelah pesanan berhasil dibuat, menjadi tanda untuk membuka detail pesanan. */
    val placedOrderId: Int? = null,
) {
    val vendorName: String? get() = items.firstOrNull()?.vendorName
    val isEmpty: Boolean get() = items.isEmpty()
}
