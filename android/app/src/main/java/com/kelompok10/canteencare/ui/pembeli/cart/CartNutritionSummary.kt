package com.kelompok10.canteencare.ui.pembeli.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kelompok10.canteencare.core.util.Formatters
import com.kelompok10.canteencare.domain.calculator.NutritionCalculator
import com.kelompok10.canteencare.domain.model.Nutrient
import com.kelompok10.canteencare.domain.model.NutrientImpact
import com.kelompok10.canteencare.domain.model.NutrientKind
import com.kelompok10.canteencare.domain.model.Nutrition
import com.kelompok10.canteencare.domain.model.NutritionTarget
import com.kelompok10.canteencare.ui.components.color
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme

/**
 * Ringkasan total gizi pesanan di layar Keranjang.
 *
 * Kalori tampil besar di atas, lima gizi lain dalam kartu dua kolom. Jika [impact] tersedia,
 * setiap kartu juga menunjukkan berapa persen target harian yang dipakai pesanan ini dan
 * bar progres harian setelah pesan; peringatan muncul jika ada gizi yang melewati target/batas.
 */
@Composable
fun CartNutritionSummary(
    total: Nutrition,
    impact: List<NutrientImpact>,
    modifier: Modifier = Modifier,
) {
    val impactByNutrient = impact.associateBy { it.nutrient }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NutrientTile(Nutrient.CALORIES, total, impactByNutrient[Nutrient.CALORIES], highlight = true)
            Nutrient.entries.filter { it != Nutrient.CALORIES }.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { nutrient ->
                        NutrientTile(nutrient, total, impactByNutrient[nutrient], modifier = Modifier.weight(1f))
                    }
                    // Baris terakhir hanya berisi satu gizi (gula): sisakan ruang agar lebarnya sama
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
            val warnings = impact.filter { it.exceedsTarget }
            if (warnings.isNotEmpty()) WarningBox(warnings)
            Text(
                if (impact.isEmpty()) {
                    "Perbandingan dengan target harian belum tersedia."
                } else {
                    "Persen menunjukkan bagian pesanan ini dari target harian. " +
                        "Bar menunjukkan total hari ini setelah pesanan ini."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NutrientTile(
    nutrient: Nutrient,
    total: Nutrition,
    impact: NutrientImpact?,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    val color = impact?.projected?.status?.tone?.color() ?: MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                nutrient.label + if (nutrient.kind == NutrientKind.LIMIT) " (batas)" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                Formatters.nutrient(total.valueOf(nutrient), nutrient.unit),
                style = if (highlight) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (impact != null) {
                LinearProgressIndicator(
                    progress = { (impact.projected.percentage / 100).toFloat().coerceIn(0f, 1f) },
                    color = color,
                    trackColor = color.copy(alpha = 0.18f),
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                )
                Text(
                    "${Formatters.number(impact.orderPercentOfTarget)}% dari target. Total hari ini: " +
                        "${Formatters.number(impact.projected.percentage)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun WarningBox(warnings: List<NutrientImpact>) {
    val danger: Color = MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(danger.copy(alpha = 0.10f), MaterialTheme.shapes.medium)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = danger, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            warnings.forEach { impact ->
                val progress = impact.projected
                val limitWord = if (progress.kind == NutrientKind.LIMIT) "batas" else "target"
                Text(
                    "${progress.nutrient.label} hari ini jadi ${Formatters.nutrient(progress.consumed, progress.unit)}, " +
                        "melebihi $limitWord ${Formatters.nutrient(progress.target, progress.unit)}.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CartNutritionSummaryPreview() {
    val order = Nutrition(890.0, 145.0, 30.0, 25.0, 1820.0, 66.0)
    val consumedToday = Nutrition(500.0, 60.0, 20.0, 15.0, 200.0, 10.0)
    CanteenCareTheme {
        Column(Modifier.padding(16.dp)) {
            CartNutritionSummary(
                total = order,
                impact = NutritionCalculator.calculateOrderImpact(order, consumedToday, NutritionTarget.DEFAULT),
            )
        }
    }
}
