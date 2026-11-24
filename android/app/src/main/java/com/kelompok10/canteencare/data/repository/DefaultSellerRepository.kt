package com.kelompok10.canteencare.data.repository

import com.kelompok10.canteencare.core.network.ApiResult
import com.kelompok10.canteencare.core.network.ApiService
import com.kelompok10.canteencare.core.network.safeApiCall
import com.kelompok10.canteencare.data.remote.dto.RejectRequestDto
import com.kelompok10.canteencare.data.remote.dto.StockRequestDto
import com.kelompok10.canteencare.data.remote.dto.toDomain
import com.kelompok10.canteencare.data.remote.dto.toRequest
import com.kelompok10.canteencare.domain.model.Menu
import com.kelompok10.canteencare.domain.model.MenuDraft
import com.kelompok10.canteencare.domain.model.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSellerRepository @Inject constructor(
    private val api: ApiService,
) : SellerRepository {

    override suspend fun getOrders(): ApiResult<List<Order>> =
        safeApiCall { api.getSellerOrders().map { it.toDomain() } }

    // Endpoint detail pesanan sama dengan pembeli; server mengizinkan penjual stan terkait
    override suspend fun getOrder(orderId: Int): ApiResult<Order> =
        safeApiCall { api.getOrder(orderId).toDomain() }

    override suspend fun confirmOrder(orderId: Int): ApiResult<Order> =
        safeApiCall { api.confirmOrder(orderId).toDomain() }

    override suspend fun rejectOrder(orderId: Int, reason: String): ApiResult<Order> =
        safeApiCall { api.rejectOrder(orderId, RejectRequestDto(reason)).toDomain() }

    override suspend fun markOrderReady(orderId: Int): ApiResult<Order> =
        safeApiCall { api.markOrderReady(orderId).toDomain() }

    override suspend fun completeOrder(orderId: Int): ApiResult<Order> =
        safeApiCall { api.completeOrder(orderId).toDomain() }

    override suspend fun getMenus(): ApiResult<List<Menu>> =
        safeApiCall { api.getSellerMenus().map { it.toDomain() } }

    override suspend fun createMenu(draft: MenuDraft): ApiResult<Menu> =
        safeApiCall { api.createMenu(draft.toRequest()).toDomain() }

    override suspend fun updateStock(menuId: Int, stock: Int): ApiResult<Menu> =
        safeApiCall { api.updateStock(menuId, StockRequestDto(stock)).toDomain() }

    override suspend fun archiveMenu(menuId: Int): ApiResult<Menu> =
        safeApiCall { api.archiveMenu(menuId).toDomain() }
}
