package com.kelompok10.canteencare.domain.model

/** Target gizi harian pembeli. Semua nilai harus lebih dari 0. */
data class NutritionTarget(
    val calories: Double,
    val carbs: Double,
    val protein: Double,
    val fat: Double,
    val salt: Double,
    val sugar: Double,
) {
    fun valueOf(nutrient: Nutrient): Double = when (nutrient) {
        Nutrient.CALORIES -> calories
        Nutrient.CARBS -> carbs
        Nutrient.PROTEIN -> protein
        Nutrient.FAT -> fat
        Nutrient.SALT -> salt
        Nutrient.SUGAR -> sugar
    }

    companion object {
        /** Sama dengan default di backend untuk pembeli baru. */
        val DEFAULT = NutritionTarget(
            calories = 2150.0,
            carbs = 325.0,
            protein = 60.0,
            fat = 67.0,
            salt = 2000.0,
            sugar = 50.0,
        )

        fun fromValues(values: Map<Nutrient, Double>) = NutritionTarget(
            calories = values.getValue(Nutrient.CALORIES),
            carbs = values.getValue(Nutrient.CARBS),
            protein = values.getValue(Nutrient.PROTEIN),
            fat = values.getValue(Nutrient.FAT),
            salt = values.getValue(Nutrient.SALT),
            sugar = values.getValue(Nutrient.SUGAR),
        )
    }
}

/** Status progres satu jenis gizi dibanding targetnya. [tone] menentukan warna di layar. */
enum class NutrientStatus(val label: String, val tone: StatusTone) {
    // Untuk GOAL (kalori, karbohidrat, protein)
    KURANG("Kurang", StatusTone.WARNING),
    TERCUKUPI("Tercukupi", StatusTone.GOOD),
    BERLEBIH("Berlebih", StatusTone.DANGER),

    // Untuk LIMIT (lemak, garam, gula)
    AMAN("Aman", StatusTone.GOOD),
    MENDEKATI_BATAS("Mendekati batas", StatusTone.WARNING),
    MELEBIHI_BATAS("Melebihi batas", StatusTone.DANGER),
}

/** Hijau = baik, kuning = perlu perhatian, merah = berlebih/melewati batas. */
enum class StatusTone { GOOD, WARNING, DANGER }

/**
 * Dampak satu pesanan terhadap target harian untuk satu jenis gizi.
 * [orderPercentOfTarget] = porsi pesanan ini terhadap target; [projected] = progres harian setelah pesanan ini ditambahkan ke asupan hari ini.
 */
data class NutrientImpact(
    val nutrient: Nutrient,
    val orderAmount: Double,
    /** Persen pesanan thdp target harian, dibulat ke satu desimal */
    val orderPercentOfTarget: Double,
    val projected: NutrientProgress,
) {
    /** True jika pesanan ini membuat/memperparah asupan harian melewati target/batas. */
    val exceedsTarget: Boolean get() = orderAmount > 0 && projected.excess > 0
}

/** Hasil perbandingan asupan hari ini dengan target untuk satu jenis gizi. */
data class NutrientProgress(
    val nutrient: Nutrient,
    val consumed: Double,
    val target: Double,
    val unit: String,
    /** Persen terhadap target, dibulatkan satu desimal. Bisa lebih dari 100. */
    val percentage: Double,
    /** max(0, target − konsumsi) */
    val remaining: Double,
    /** max(0, konsumsi − target) */
    val excess: Double,
    val kind: NutrientKind,
    val status: NutrientStatus,
)
