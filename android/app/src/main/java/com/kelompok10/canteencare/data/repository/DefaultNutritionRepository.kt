package com.kelompok10.canteencare.data.repository

import com.kelompok10.canteencare.core.network.ApiResult
import com.kelompok10.canteencare.core.network.ApiService
import com.kelompok10.canteencare.core.network.safeApiCall
import com.kelompok10.canteencare.data.remote.dto.toDomain
import com.kelompok10.canteencare.data.remote.dto.toDto
import com.kelompok10.canteencare.domain.model.NutritionTarget
import com.kelompok10.canteencare.domain.model.Order
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultNutritionRepository @Inject constructor(
    private val api: ApiService,
) : NutritionRepository {

    override suspend fun getTarget(): ApiResult<NutritionTarget> =
        safeApiCall { api.getNutritionTarget().toDomain() }

    override suspend fun updateTarget(target: NutritionTarget): ApiResult<NutritionTarget> =
        safeApiCall { api.updateNutritionTarget(target.toDto()).toDomain() }

    override suspend fun getOrdersForDate(date: LocalDate): ApiResult<List<Order>> = safeApiCall {
        // LocalDate.toString() menghasilkan format YYYY-MM-DD sesuai kebutuhan backend.
        // Status tidak difilter di sini; NutritionCalculator yang memilih pesanan yang dihitung.
        api.getMyOrders(date = date.toString()).map { it.toDomain() }
    }
}
