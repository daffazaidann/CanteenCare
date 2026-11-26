package com.kelompok10.canteencare.domain.model

/**
 * Enam nilai gizi. Bisa berarti gizi per porsi, total satu pesanan, atau total satu hari.
 * Satuan: kalori kkal, garam mg natrium, sisanya gram.
 */
data class Nutrition(
    val calories: Double,
    val carbs: Double,
    val protein: Double,
    val fat: Double,
    val salt: Double,
    val sugar: Double,
) {
    /** Ambil nilai untuk satu jenis gizi, berguna saat menampilkan keenamnya dalam perulangan. */
    fun valueOf(nutrient: Nutrient): Double = when (nutrient) {
        Nutrient.CALORIES -> calories
        Nutrient.CARBS -> carbs
        Nutrient.PROTEIN -> protein
        Nutrient.FAT -> fat
        Nutrient.SALT -> salt
        Nutrient.SUGAR -> sugar
    }

    operator fun plus(other: Nutrition) = Nutrition(
        calories = calories + other.calories,
        carbs = carbs + other.carbs,
        protein = protein + other.protein,
        fat = fat + other.fat,
        salt = salt + other.salt,
        sugar = sugar + other.sugar,
    )

    operator fun times(factor: Int) = Nutrition(
        calories = calories * factor,
        carbs = carbs * factor,
        protein = protein * factor,
        fat = fat * factor,
        salt = salt * factor,
        sugar = sugar * factor,
    )

    companion object {
        val ZERO = Nutrition(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }
}

/**
 * Jenis target gizi:
 * - GOAL: sebaiknya dicapai (makin dekat 100% makin baik).
 * - LIMIT: batas maksimal (sebaiknya tidak dilewati).
 */
enum class NutrientKind { GOAL, LIMIT }

/** Jenis gizi beserta label, satuan, dan jenis targetnya. */
enum class Nutrient(val label: String, val unit: String, val kind: NutrientKind) {
    CALORIES("Kalori", "kkal", NutrientKind.GOAL),
    CARBS("Karbohidrat", "g", NutrientKind.GOAL),
    PROTEIN("Protein", "g", NutrientKind.GOAL),
    FAT("Lemak", "g", NutrientKind.LIMIT),
    SALT("Garam (natrium)", "mg", NutrientKind.LIMIT),
    SUGAR("Gula", "g", NutrientKind.LIMIT),
}
