package com.kelompok10.canteencare.data.repository

import com.kelompok10.canteencare.core.network.ApiResult
import com.kelompok10.canteencare.domain.model.Menu
import com.kelompok10.canteencare.domain.model.MenuDraft
import com.kelompok10.canteencare.domain.model.Order

/** Fitur penjual. Server membatasi semua aksi hanya untuk stan milik penjual yang login. */
interface SellerRepository {
    /** Semua pesanan masuk ke stan, terbaru dulu. */
    suspend fun getOrders(): ApiResult<List<Order>>

    suspend fun getOrder(orderId: Int): ApiResult<Order>

    /** MENUNGGU → SEDANG_DIMASAK; server mengurangi stok semua item sekaligus. */
    suspend fun confirmOrder(orderId: Int): ApiResult<Order>

    /** MENUNGGU → DITOLAK dengan alasan; stok tidak berubah. */
    suspend fun rejectOrder(orderId: Int, reason: String): ApiResult<Order>

    /** SEDANG_DIMASAK → SIAP_DIAMBIL; pembeli mendapat notifikasi. */
    suspend fun markOrderReady(orderId: Int): ApiResult<Order>

    /** SIAP_DIAMBIL → SELESAI; pesanan pindah ke riwayat. */
    suspend fun completeOrder(orderId: Int): ApiResult<Order>

    suspend fun getMenus(): ApiResult<List<Menu>>

    /** Ajukan menu baru; statusnya PENDING sampai diverifikasi admin. */
    suspend fun createMenu(draft: MenuDraft): ApiResult<Menu>

    suspend fun updateStock(menuId: Int, stock: Int): ApiResult<Menu>

    suspend fun archiveMenu(menuId: Int): ApiResult<Menu>
}
