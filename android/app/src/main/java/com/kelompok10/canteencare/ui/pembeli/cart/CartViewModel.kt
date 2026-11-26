package com.kelompok10.canteencare.ui.pembeli.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok10.canteencare.core.network.ApiResult
import com.kelompok10.canteencare.data.repository.CartRepository
import com.kelompok10.canteencare.core.util.WibTime
import com.kelompok10.canteencare.data.repository.CatalogRepository
import com.kelompok10.canteencare.data.repository.NutritionRepository
import com.kelompok10.canteencare.data.repository.OrderRepository
import com.kelompok10.canteencare.domain.calculator.NutritionCalculator
import com.kelompok10.canteencare.domain.model.CartItem
import com.kelompok10.canteencare.domain.model.Nutrition
import com.kelompok10.canteencare.domain.model.NutritionTarget
import com.kelompok10.canteencare.domain.model.PaymentMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val catalogRepository: CatalogRepository,
    private val nutritionRepository: NutritionRepository,
) : ViewModel() {

    /** Target harian dan asupan hari ini, dipakai untuk membandingkan dengan isi keranjang. */
    private data class DailyContext(val target: NutritionTarget, val consumed: Nutrition)

    /** Null selama belum termuat atau gagal dimuat (misalnya offline); keranjang tetap bisa dipakai. */
    private val daily = MutableStateFlow<DailyContext?>(null)

    /** Isian form yang hanya ada di layar ini: metode bayar, catatan, status kirim. */
    private data class FormState(
        val paymentMethod: PaymentMethod = PaymentMethod.TUNAI,
        val note: String = "",
        val isPlacingOrder: Boolean = false,
        val errorMessage: String? = null,
        val placedOrderId: Int? = null,
    )

    private val form = MutableStateFlow(FormState())

    /**
     * Fungsi 1 dan 2 secara reaktif:
     * setiap kali isi keranjang di Room berubah (misalnya jumlah ditambah), Flow memancarkan
     * daftar item baru, lalu total harga dan total gizi langsung dihitung ulang oleh NutritionCalculator.
     */
    val uiState: StateFlow<CartUiState> = combine(cartRepository.observeCart(), form, daily) { items, form, daily ->
        val totalNutrition = NutritionCalculator.calculateOrderNutrition(items)
        CartUiState(
            isLoaded = true,
            items = items,
            totalPrice = NutritionCalculator.calculateTotalPrice(items),
            totalNutrition = totalNutrition,
            dailyImpact = daily?.let {
                NutritionCalculator.calculateOrderImpact(totalNutrition, it.consumed, it.target)
            }.orEmpty(),
            paymentMethod = form.paymentMethod,
            note = form.note,
            isPlacingOrder = form.isPlacingOrder,
            errorMessage = form.errorMessage,
            placedOrderId = form.placedOrderId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    init {
        loadDailyContext()
    }

    private fun loadDailyContext() {
        viewModelScope.launch {
            val targetCall = async { nutritionRepository.getTarget() }
            val ordersCall = async { nutritionRepository.getOrdersForDate(WibTime.today()) }
            val target = targetCall.await()
            val orders = ordersCall.await()
            if (target is ApiResult.Success && orders is ApiResult.Success) {
                daily.value = DailyContext(target.data, NutritionCalculator.sumNutrition(orders.data))
            }
        }
    }

    /** Tambah satu porsi, maksimal sebanyak stok. */
    fun onIncrease(item: CartItem) {
        if (item.quantity >= item.stock) return
        viewModelScope.launch { cartRepository.updateQuantity(item.menuId, item.quantity + 1) }
    }

    /** Kurangi satu porsi; jika menjadi 0, item otomatis dihapus dari keranjang. */
    fun onDecrease(item: CartItem) {
        viewModelScope.launch { cartRepository.updateQuantity(item.menuId, item.quantity - 1) }
    }

    fun onRemove(item: CartItem) {
        viewModelScope.launch { cartRepository.removeItem(item.menuId) }
    }

    fun onPaymentMethodChange(method: PaymentMethod) = form.update { it.copy(paymentMethod = method) }

    fun onNoteChange(note: String) = form.update { it.copy(note = note) }

    fun onErrorShown() = form.update { it.copy(errorMessage = null) }

    fun onOrderOpened() = form.update { it.copy(placedOrderId = null) }

    fun placeOrder() {
        val state = uiState.value
        if (state.isEmpty || state.isPlacingOrder) return
        val vendorId = state.items.first().vendorId

        viewModelScope.launch {
            form.update { it.copy(isPlacingOrder = true, errorMessage = null) }
            when (val result = orderRepository.placeOrder(vendorId, state.paymentMethod, state.note, state.items)) {
                is ApiResult.Success -> {
                    // Pesanan terkirim: keranjang dikosongkan, lalu buka detail pesanan
                    cartRepository.clear()
                    form.update { FormState(placedOrderId = result.data.id) }
                }
                is ApiResult.Error -> {
                    // Contoh pesan server: "Stok Mie Ayam Solo tidak mencukupi, tersisa 2."
                    form.update { it.copy(isPlacingOrder = false, errorMessage = result.message) }
                    if (result.code in STOCK_ERROR_CODES) refreshStock(vendorId)
                }
            }
        }
    }

    /** Ambil stok terbaru dari server lalu sesuaikan isi keranjang. */
    private suspend fun refreshStock(vendorId: Int) {
        val menus = catalogRepository.getVendorMenus(vendorId)
        if (menus is ApiResult.Success) cartRepository.syncStock(menus.data)
    }

    private companion object {
        val STOCK_ERROR_CODES = setOf("INSUFFICIENT_STOCK", "MENU_NOT_AVAILABLE")
    }
}
