package com.kelompok10.canteencare.ui.penjual.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok10.canteencare.core.network.ApiResult
import com.kelompok10.canteencare.data.repository.SellerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SellerOrdersViewModel @Inject constructor(
    private val sellerRepository: SellerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerOrdersUiState())
    val uiState: StateFlow<SellerOrdersUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun onScreenResumed() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { fetch(pullToRefresh = false) }
    }

    fun onRefresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { fetch(pullToRefresh = true) }
    }

    /** Dipanggil tiap 10 detik selama tab aktif terbuka, tanpa spinner. */
    suspend fun autoRefresh() = fetch(pullToRefresh = false)

    fun onTabSelected(tab: SellerOrderTab) = _uiState.update { it.copy(selectedTab = tab) }

    private suspend fun fetch(pullToRefresh: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = !pullToRefresh && it.orders.isEmpty(),
                isRefreshing = pullToRefresh,
                errorMessage = null,
            )
        }
        when (val result = sellerRepository.getOrders()) {
            is ApiResult.Success -> _uiState.update {
                it.copy(isLoading = false, isRefreshing = false, orders = result.data)
            }
            is ApiResult.Error -> _uiState.update {
                it.copy(isLoading = false, isRefreshing = false, errorMessage = result.message)
            }
        }
    }
}
