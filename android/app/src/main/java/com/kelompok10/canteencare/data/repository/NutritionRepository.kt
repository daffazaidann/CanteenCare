package com.kelompok10.canteencare.data.repository

import com.kelompok10.canteencare.core.network.ApiResult
import com.kelompok10.canteencare.domain.model.NutritionTarget
import com.kelompok10.canteencare.domain.model.Order
import java.time.LocalDate

/** Data untuk Fungsi 3: target gizi harian dan pesanan pada tanggal tertentu. */
interface NutritionRepository {
    suspend fun getTarget(): ApiResult<NutritionTarget>

    suspend fun updateTarget(target: NutritionTarget): ApiResult<NutritionTarget>

    /** Semua pesanan milik user pada tanggal WIB tertentu. */
    suspend fun getOrdersForDate(date: LocalDate): ApiResult<List<Order>>
}
