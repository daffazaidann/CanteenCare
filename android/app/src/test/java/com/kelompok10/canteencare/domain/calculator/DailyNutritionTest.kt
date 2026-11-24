package com.kelompok10.canteencare.domain.calculator

import com.kelompok10.canteencare.domain.model.Nutrient
import com.kelompok10.canteencare.domain.model.NutrientKind
import com.kelompok10.canteencare.domain.model.NutrientStatus
import com.kelompok10.canteencare.domain.model.Nutrition
import com.kelompok10.canteencare.domain.model.NutritionTarget
import com.kelompok10.canteencare.domain.model.Order
import com.kelompok10.canteencare.domain.model.OrderItem
import com.kelompok10.canteencare.domain.model.OrderStatus
import com.kelompok10.canteencare.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.OffsetDateTime

/** Tes Fungsi 3: pelacakan asupan gizi harian terhadap target. */
class DailyNutritionTest {

    private val mieAyamSolo = Nutrition(650.0, 85.0, 30.0, 25.0, 1800.0, 6.0)
    private val esTehManis = Nutrition(120.0, 30.0, 0.0, 0.0, 10.0, 30.0)

    private fun order(status: OrderStatus, vararg items: Pair<Nutrition, Int>) = Order(
        id = 1, orderNumber = "CC-20261118-0001", buyerName = "Daffa", vendorId = 1, vendorName = "Dapur Gapura",
        status = status, paymentMethod = PaymentMethod.TUNAI, note = null, rejectionReason = null,
        createdAt = OffsetDateTime.parse("2026-11-18T05:00:00Z"), resolvedAt = null,
        items = items.map { (nutrition, qty) -> OrderItem(1, "Menu", 10000, qty, nutrition) },
    )

    private fun progressOf(consumed: Nutrition, target: NutritionTarget, nutrient: Nutrient) =
        NutritionCalculator.calculateProgress(consumed, target).first { it.nutrient == nutrient }

    // ---------- Langkah 1: sumNutrition ----------

    @Test
    fun `hanya pesanan yang sudah diterima yang dijumlahkan`() {
        val orders = listOf(
            order(OrderStatus.SEDANG_DIMASAK, mieAyamSolo to 1, esTehManis to 2), // 890 kkal
            order(OrderStatus.SIAP_DIAMBIL, esTehManis to 1), // 120 kkal
            order(OrderStatus.SELESAI, mieAyamSolo to 1), // 650 kkal
            order(OrderStatus.MENUNGGU, mieAyamSolo to 3), // tidak dihitung
            order(OrderStatus.DITOLAK, mieAyamSolo to 1), // tidak dihitung
        )

        val total = NutritionCalculator.sumNutrition(orders)

        assertEquals(1660.0, total.calories, DELTA)
        assertEquals(102.0, total.sugar, DELTA) // 6 + 2×30 + 30 + 6
    }

    @Test
    fun `tanpa pesanan asupan nol`() {
        assertEquals(Nutrition.ZERO, NutritionCalculator.sumNutrition(emptyList()))
    }

    // ---------- Langkah 2: calculateProgress (contoh dari brief) ----------

    @Test
    fun `kalori 1370 dari 2150 adalah 63,7 persen KURANG sisa 780`() {
        val progress = progressOf(Nutrition.ZERO.copy(calories = 1370.0), NutritionTarget.DEFAULT, Nutrient.CALORIES)

        assertEquals(63.7, progress.percentage, DELTA)
        assertEquals(NutrientStatus.KURANG, progress.status)
        assertEquals(780.0, progress.remaining, DELTA)
        assertEquals(0.0, progress.excess, DELTA)
        assertEquals(NutrientKind.GOAL, progress.kind)
        assertEquals("kkal", progress.unit)
    }

    @Test
    fun `gula 76 dari batas 50 adalah 152 persen MELEBIHI_BATAS lebih 26`() {
        val progress = progressOf(Nutrition.ZERO.copy(sugar = 76.0), NutritionTarget.DEFAULT, Nutrient.SUGAR)

        assertEquals(152.0, progress.percentage, DELTA)
        assertEquals(NutrientStatus.MELEBIHI_BATAS, progress.status)
        assertEquals(26.0, progress.excess, DELTA)
        assertEquals(0.0, progress.remaining, DELTA)
        assertEquals(NutrientKind.LIMIT, progress.kind)
    }

    @Test
    fun `progres selalu berisi enam gizi sesuai urutan`() {
        val list = NutritionCalculator.calculateProgress(Nutrition.ZERO, NutritionTarget.DEFAULT)
        assertEquals(Nutrient.entries.toList(), list.map { it.nutrient })
    }

    @Test
    fun `belum makan apa pun semua nol persen`() {
        val list = NutritionCalculator.calculateProgress(Nutrition.ZERO, NutritionTarget.DEFAULT)

        list.forEach { assertEquals(0.0, it.percentage, DELTA) }
        assertEquals(NutrientStatus.KURANG, list.first { it.nutrient == Nutrient.PROTEIN }.status)
        assertEquals(NutrientStatus.AMAN, list.first { it.nutrient == Nutrient.SALT }.status)
    }

    // ---------- Batas status 80% dan 100% ----------

    @Test
    fun `status GOAL di sekitar batas 80 dan 100 persen`() {
        assertEquals(NutrientStatus.KURANG, NutritionCalculator.statusOf(NutrientKind.GOAL, 79.9))
        assertEquals(NutrientStatus.TERCUKUPI, NutritionCalculator.statusOf(NutrientKind.GOAL, 80.0))
        assertEquals(NutrientStatus.TERCUKUPI, NutritionCalculator.statusOf(NutrientKind.GOAL, 100.0))
        assertEquals(NutrientStatus.BERLEBIH, NutritionCalculator.statusOf(NutrientKind.GOAL, 100.1))
    }

    @Test
    fun `status LIMIT di sekitar batas 80 dan 100 persen`() {
        assertEquals(NutrientStatus.AMAN, NutritionCalculator.statusOf(NutrientKind.LIMIT, 79.9))
        assertEquals(NutrientStatus.MENDEKATI_BATAS, NutritionCalculator.statusOf(NutrientKind.LIMIT, 80.0))
        assertEquals(NutrientStatus.MENDEKATI_BATAS, NutritionCalculator.statusOf(NutrientKind.LIMIT, 100.0))
        assertEquals(NutrientStatus.MELEBIHI_BATAS, NutritionCalculator.statusOf(NutrientKind.LIMIT, 100.1))
    }

    @Test
    fun `alur lengkap dari pesanan seed sampai progres`() {
        // Pesanan seed hari ini: 1 Mie Ayam Solo + 2 Es Teh Manis
        val consumed = NutritionCalculator.sumNutrition(
            listOf(order(OrderStatus.SIAP_DIAMBIL, mieAyamSolo to 1, esTehManis to 2)),
        )

        val salt = progressOf(consumed, NutritionTarget.DEFAULT, Nutrient.SALT) // 1820 dari 2000 mg

        assertEquals(91.0, salt.percentage, DELTA)
        assertEquals(NutrientStatus.MENDEKATI_BATAS, salt.status)
        assertEquals(180.0, salt.remaining, DELTA)
    }

    private companion object {
        const val DELTA = 0.001
    }
}
