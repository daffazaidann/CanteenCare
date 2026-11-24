package com.kelompok10.canteencare.ui.pembeli.nutrition

import com.kelompok10.canteencare.domain.model.NutrientProgress
import java.time.LocalDate

/** State layar Gizi Hari Ini. [progress] berisi enam baris hasil NutritionCalculator.calculateProgress. */
data class NutritionUiState(
    val date: LocalDate? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val progress: List<NutrientProgress> = emptyList(),
    val countedOrderCount: Int = 0,
)
