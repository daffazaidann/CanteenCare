package com.kelompok10.canteencare.ui.pembeli.order

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok10.canteencare.core.network.ApiResult
import com.kelompok10.canteencare.data.repository.OrderRepository
import com.kelompok10.canteencare.domain.calculator.NutritionCalculator
import com.kelompok10.canteencare.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val orderId: Int = checkNotNull(savedStateHandle[Routes.ARG_ORDER_ID])

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { fetch() }
    }

    /**
     * Dipanggil berkala (tiap 10 detik) selama status masih aktif.
     * Data lama tetap tampil selama memuat, jadi layar tidak berkedip.
     */
    suspend fun refresh() = fetch()

    private suspend fun fetch() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        when (val result = orderRepository.getOrder(orderId)) {
            is ApiResult.Success -> {
                val order = result.data
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        order = order,
                        // Fungsi 1 dan 2: total dihitung dari snapshot item pesanan
                        totalPrice = NutritionCalculator.calculateTotalPrice(order.items),
                        totalNutrition = NutritionCalculator.calculateOrderNutrition(order.items),
                    )
                }
            }
            is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
        }
    }
}
