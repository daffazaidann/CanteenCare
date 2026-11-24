package com.kelompok10.canteencare.ui.pembeli.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok10.canteencare.core.network.ApiResult
import com.kelompok10.canteencare.core.util.WibTime
import com.kelompok10.canteencare.data.repository.NutritionRepository
import com.kelompok10.canteencare.domain.calculator.NutritionCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    /** Dipanggil setiap layar tampil, misalnya setelah target diubah atau pesanan diterima. */
    fun onScreenResumed() = load(pullToRefresh = false)

    fun onRefresh() = load(pullToRefresh = true)

    private fun load(pullToRefresh: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // "Hari ini" selalu menurut WIB, sama seperti filter tanggal di backend
            val today = WibTime.today()
            _uiState.update {
                it.copy(
                    date = today,
                    isLoading = !pullToRefresh && it.progress.isEmpty(),
                    isRefreshing = pullToRefresh,
                    errorMessage = null,
                )
            }

            // Target dan pesanan hari ini diambil bersamaan
            val targetCall = async { nutritionRepository.getTarget() }
            val ordersCall = async { nutritionRepository.getOrdersForDate(today) }
            val target = targetCall.await()
            val orders = ordersCall.await()

            when {
                target is ApiResult.Error -> showError(target.message)
                orders is ApiResult.Error -> showError(orders.message)
                target is ApiResult.Success && orders is ApiResult.Success -> {
                    // Fungsi 3: (1) jumlahkan gizi pesanan yang sudah diterima, (2) bandingkan dengan target
                    val consumed = NutritionCalculator.sumNutrition(orders.data)
                    val progress = NutritionCalculator.calculateProgress(consumed, target.data)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            progress = progress,
                            countedOrderCount = orders.data.count { order -> order.status.isCounted },
                        )
                    }
                }
            }
        }
    }

    private fun showError(message: String) = _uiState.update {
        it.copy(isLoading = false, isRefreshing = false, errorMessage = message)
    }
}
