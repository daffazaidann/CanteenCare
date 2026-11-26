package com.kelompok10.canteencare.domain.calculator

import com.kelompok10.canteencare.domain.model.LineItem
import com.kelompok10.canteencare.domain.model.Nutrient
import com.kelompok10.canteencare.domain.model.NutrientImpact
import com.kelompok10.canteencare.domain.model.NutrientKind
import com.kelompok10.canteencare.domain.model.NutrientProgress
import com.kelompok10.canteencare.domain.model.NutrientStatus
import com.kelompok10.canteencare.domain.model.Nutrition
import com.kelompok10.canteencare.domain.model.NutritionTarget
import com.kelompok10.canteencare.domain.model.Order
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Inti logika CanteenCare. Semua perhitungan Fungsi 1, 2, dan 3 di aplikasi HANYA ada di sini.
 *
 * Kelas ini murni Kotlin tanpa ketergantungan Android, sehingga mudah diuji dengan JUnit
 * dan dipakai oleh ViewModel mana pun (keranjang, detail pesanan, gizi harian).
 */
object NutritionCalculator {

    /** Batas bawah zona "cukup/mendekati batas", dalam persen. */
    private const val NEAR_TARGET_PERCENT = 80.0
    private const val FULL_PERCENT = 100.0

    /** Subtotal satu baris: harga satuan × jumlah porsi. */
    fun calculateSubtotal(price: Int, quantity: Int): Int = price * quantity

    /**
     * Fungsi 1 — Total harga pesanan.
     * Rumus: Σ (harga × jumlah) untuk setiap item.
     */
    fun calculateTotalPrice(items: List<LineItem>): Int =
        items.sumOf { item -> calculateSubtotal(item.price, item.quantity) }

    /**
     * Fungsi 2 — Total gizi pesanan.
     * Untuk setiap jenis gizi: Σ (gizi per porsi × jumlah porsi).
     * Contoh: 1 Mie Ayam (650 kkal) + 2 Es Teh (120 kkal) = 650 + 2 × 120 = 890 kkal.
     */
    fun calculateOrderNutrition(items: List<LineItem>): Nutrition =
        items.fold(Nutrition.ZERO) { total, item -> total + calculateItemNutrition(item) }
    fun calculateItemNutrition(item: LineItem): Nutrition = item.nutrition * item.quantity
    fun calculateOrderImpact(
        order: Nutrition,
        consumedToday: Nutrition,
        target: NutritionTarget,
    ): List<NutrientImpact> =
        calculateProgress(consumedToday + order, target).map { projected ->
            val amount = order.valueOf(projected.nutrient)
            val goal = target.valueOf(projected.nutrient)
            NutrientImpact(
                nutrient = projected.nutrient,
                orderAmount = amount,
                orderPercentOfTarget = if (goal > 0) roundToOneDecimal(amount / goal * 100) else 0.0,
                projected = projected,
            )
        }

    /**
     * Fungsi 3, langkah 1 — Total asupan gizi dari daftar pesanan.
     * Pesanan yang dihitung adalah pesanan yang sudah diterima penjual, yaitu
     * SEDANG_DIMASAK, SIAP_DIAMBIL, dan SELESAI. Pesanan MENUNGGU dan DITOLAK tidak dihitung.
     * Total tiap pesanan dihitung ulang dari itemnya dengan [calculateOrderNutrition].
     */
    fun sumNutrition(orders: List<Order>): Nutrition {
        val counted = orders.filter { it.status.isCounted }
        val perOrder = counted.map { calculateOrderNutrition(it.items) }
        return Nutrition(
            calories = perOrder.sumOf { it.calories },
            carbs = perOrder.sumOf { it.carbs },
            protein = perOrder.sumOf { it.protein },
            fat = perOrder.sumOf { it.fat },
            salt = perOrder.sumOf { it.salt },
            sugar = perOrder.sumOf { it.sugar },
        )
    }

    /**
     * Fungsi 3, langkah 2 — Progres asupan terhadap target untuk keenam jenis gizi.
     *
     * Untuk setiap gizi:
     * - persentase = konsumsi ÷ target × 100
     * - sisa = max(0, target − konsumsi), kelebihan = max(0, konsumsi − target)
     * - status ditentukan oleh jenisnya (GOAL atau LIMIT), lihat [statusOf].
     *
     * Contoh: kalori 1370 dari target 2150 → 63,7%, KURANG, sisa 780.
     */
    fun calculateProgress(consumed: Nutrition, target: NutritionTarget): List<NutrientProgress> =
        Nutrient.entries.map { nutrient ->
            val eaten = consumed.valueOf(nutrient)
            val goal = target.valueOf(nutrient)
            val rawPercentage = if (goal > 0) eaten / goal * 100 else 0.0
            NutrientProgress(
                nutrient = nutrient,
                consumed = eaten,
                target = goal,
                unit = nutrient.unit,
                percentage = roundToOneDecimal(rawPercentage),
                remaining = max(0.0, goal - eaten),
                excess = max(0.0, eaten - goal),
                kind = nutrient.kind,
                status = statusOf(nutrient.kind, rawPercentage),
            )
        }

    /**
     * Aturan status:
     * - GOAL : < 80% KURANG, 80–100% TERCUKUPI, > 100% BERLEBIH
     * - LIMIT: < 80% AMAN,   80–100% MENDEKATI_BATAS, > 100% MELEBIHI_BATAS
     */
    fun statusOf(kind: NutrientKind, percentage: Double): NutrientStatus = when (kind) {
        NutrientKind.GOAL -> when {
            percentage < NEAR_TARGET_PERCENT -> NutrientStatus.KURANG
            percentage <= FULL_PERCENT -> NutrientStatus.TERCUKUPI
            else -> NutrientStatus.BERLEBIH
        }
        NutrientKind.LIMIT -> when {
            percentage < NEAR_TARGET_PERCENT -> NutrientStatus.AMAN
            percentage <= FULL_PERCENT -> NutrientStatus.MENDEKATI_BATAS
            else -> NutrientStatus.MELEBIHI_BATAS
        }
    }

    private fun roundToOneDecimal(value: Double): Double = (value * 10).roundToLong() / 10.0
}
