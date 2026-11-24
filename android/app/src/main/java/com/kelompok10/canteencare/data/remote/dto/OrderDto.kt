package com.kelompok10.canteencare.data.remote.dto

import com.kelompok10.canteencare.domain.model.Order
import com.kelompok10.canteencare.domain.model.OrderItem
import com.kelompok10.canteencare.domain.model.OrderStatus
import com.kelompok10.canteencare.domain.model.PaymentMethod
import java.time.OffsetDateTime

data class OrderItemRequestDto(val menuId: Int, val quantity: Int)

/** Body POST /orders. Total TIDAK dikirim; server menghitungnya sendiri dari database. */
data class CreateOrderRequestDto(
    val vendorId: Int,
    val paymentMethod: String,
    val note: String?,
    val items: List<OrderItemRequestDto>,
)

data class OrderItemDto(
    val menuId: Int,
    val menuName: String,
    val unitPrice: Int,
    val quantity: Int,
    val nutritionPerPortion: NutritionDto,
)

/**
 * Respons pesanan dari server. Field total_price dan total_nutrition sengaja tidak dibaca,
 * karena aplikasi menghitung total sendiri dengan NutritionCalculator (Fungsi 1 dan 2).
 */
data class OrderDto(
    val id: Int,
    val orderNumber: String,
    val buyerName: String,
    val vendorId: Int,
    val vendorName: String,
    val status: String,
    val paymentMethod: String,
    val note: String?,
    val rejectionReason: String?,
    val createdAt: String,
    val resolvedAt: String?,
    val items: List<OrderItemDto>,
)

fun OrderDto.toDomain() = Order(
    id = id,
    orderNumber = orderNumber,
    buyerName = buyerName,
    vendorId = vendorId,
    vendorName = vendorName,
    status = OrderStatus.fromApi(status),
    paymentMethod = PaymentMethod.valueOf(paymentMethod),
    note = note,
    rejectionReason = rejectionReason,
    createdAt = OffsetDateTime.parse(createdAt),
    resolvedAt = resolvedAt?.let(OffsetDateTime::parse),
    items = items.map { item ->
        OrderItem(
            menuId = item.menuId,
            menuName = item.menuName,
            price = item.unitPrice,
            quantity = item.quantity,
            nutrition = item.nutritionPerPortion.toDomain(),
        )
    },
)
