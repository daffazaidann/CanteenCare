package com.kelompok10.canteencare.core.network

import com.kelompok10.canteencare.data.remote.dto.AuthResponseDto
import com.kelompok10.canteencare.data.remote.dto.CreateMenuRequestDto
import com.kelompok10.canteencare.data.remote.dto.CreateOrderRequestDto
import com.kelompok10.canteencare.data.remote.dto.CreateSellerRequestDto
import com.kelompok10.canteencare.data.remote.dto.FcmTokenRequestDto
import com.kelompok10.canteencare.data.remote.dto.LoginRequestDto
import com.kelompok10.canteencare.data.remote.dto.MenuDto
import com.kelompok10.canteencare.data.remote.dto.MessageDto
import com.kelompok10.canteencare.data.remote.dto.NutritionRequestDto
import com.kelompok10.canteencare.data.remote.dto.NutritionTargetDto
import com.kelompok10.canteencare.data.remote.dto.OrderDto
import com.kelompok10.canteencare.data.remote.dto.RegisterRequestDto
import com.kelompok10.canteencare.data.remote.dto.RejectRequestDto
import com.kelompok10.canteencare.data.remote.dto.SellerDto
import com.kelompok10.canteencare.data.remote.dto.StockRequestDto
import com.kelompok10.canteencare.data.remote.dto.UpdateMenuRequestDto
import com.kelompok10.canteencare.data.remote.dto.UserDto
import com.kelompok10.canteencare.data.remote.dto.VendorDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Daftar endpoint backend untuk Retrofit (base URL: BuildConfig.BASE_URL).
 * Setiap fungsi adalah `suspend fun` sehingga dipanggil dari coroutine.
 * Endpoint ditambahkan bertahap sesuai fase.
 */
interface ApiService {

    // ---------- Auth ----------
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): AuthResponseDto

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): AuthResponseDto

    @GET("auth/me")
    suspend fun getMe(): UserDto

    @PUT("users/me/fcm-token")
    suspend fun updateFcmToken(@Body body: FcmTokenRequestDto): MessageDto

    // ---------- Katalog ----------
    @GET("vendors")
    suspend fun getVendors(): List<VendorDto>

    @GET("vendors/{id}")
    suspend fun getVendor(@Path("id") vendorId: Int): VendorDto

    @GET("vendors/{id}/menus")
    suspend fun getVendorMenus(@Path("id") vendorId: Int): List<MenuDto>

    @GET("menus")
    suspend fun searchMenus(@Query("search") search: String): List<MenuDto>

    /** Admin bisa melihat menu dengan status apa pun. */
    @GET("menus/{id}")
    suspend fun getMenu(@Path("id") menuId: Int): MenuDto

    // ---------- Pesanan (pembeli) ----------
    @POST("orders")
    suspend fun createOrder(@Body body: CreateOrderRequestDto): OrderDto

    /** [date] format YYYY-MM-DD, artinya tanggal WIB saat pesanan dibuat. */
    @GET("orders/me")
    suspend fun getMyOrders(
        @Query("status") status: String? = null,
        @Query("date") date: String? = null,
    ): List<OrderDto>

    @GET("orders/{id}")
    suspend fun getOrder(@Path("id") orderId: Int): OrderDto

    // ---------- Target gizi (pembeli) ----------
    @GET("nutrition-target")
    suspend fun getNutritionTarget(): NutritionTargetDto

    @PUT("nutrition-target")
    suspend fun updateNutritionTarget(@Body body: NutritionTargetDto): NutritionTargetDto

    // ---------- Penjual (selalu untuk stan milik sendiri) ----------
    @GET("seller/orders")
    suspend fun getSellerOrders(): List<OrderDto>

    @POST("seller/orders/{id}/confirm")
    suspend fun confirmOrder(@Path("id") orderId: Int): OrderDto

    @POST("seller/orders/{id}/reject")
    suspend fun rejectOrder(@Path("id") orderId: Int, @Body body: RejectRequestDto): OrderDto

    @POST("seller/orders/{id}/ready")
    suspend fun markOrderReady(@Path("id") orderId: Int): OrderDto

    @POST("seller/orders/{id}/complete")
    suspend fun completeOrder(@Path("id") orderId: Int): OrderDto

    @GET("seller/menus")
    suspend fun getSellerMenus(): List<MenuDto>

    @POST("seller/menus")
    suspend fun createMenu(@Body body: CreateMenuRequestDto): MenuDto

    @PATCH("seller/menus/{id}/stock")
    suspend fun updateStock(@Path("id") menuId: Int, @Body body: StockRequestDto): MenuDto

    /** Mengarsipkan menu (status ARCHIVED), bukan menghapus permanen. */
    @DELETE("seller/menus/{id}")
    suspend fun archiveMenu(@Path("id") menuId: Int): MenuDto

    // ---------- Admin ----------
    @GET("admin/menus")
    suspend fun getAdminMenus(@Query("status") status: String? = null): List<MenuDto>

    @PUT("admin/menus/{id}")
    suspend fun updateMenu(@Path("id") menuId: Int, @Body body: UpdateMenuRequestDto): MenuDto

    /** Menyimpan nilai gizi sekaligus menandainya terverifikasi oleh admin. */
    @PUT("admin/menus/{id}/nutrition")
    suspend fun verifyNutrition(@Path("id") menuId: Int, @Body body: NutritionRequestDto): MenuDto

    @POST("admin/menus/{id}/approve")
    suspend fun approveMenu(@Path("id") menuId: Int): MenuDto

    @POST("admin/menus/{id}/reject")
    suspend fun rejectMenu(@Path("id") menuId: Int): MenuDto

    @GET("admin/sellers")
    suspend fun getSellers(): List<SellerDto>

    @POST("admin/sellers")
    suspend fun createSeller(@Body body: CreateSellerRequestDto): SellerDto
}
