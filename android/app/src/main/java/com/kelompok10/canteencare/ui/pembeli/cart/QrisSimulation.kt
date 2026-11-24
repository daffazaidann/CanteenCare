package com.kelompok10.canteencare.ui.pembeli.cart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kelompok10.canteencare.core.util.Formatters
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme
import kotlin.random.Random

/**
 * Kartu QRIS simulasi. Gambar QR digambar sendiri dengan Canvas (bukan QR asli),
 * karena pembayaran di aplikasi ini hanya simulasi.
 */
@Composable
fun QrisSimulationCard(totalPrice: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("QRIS CanteenCare", style = MaterialTheme.typography.titleMedium)
            Canvas(
                modifier = Modifier
                    .size(168.dp)
                    .background(Color.White)
                    .padding(8.dp),
            ) { drawFakeQr() }
            Text(
                "Simulasi pembayaran · ${Formatters.rupiah(totalPrice)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Tidak ada uang yang ditarik. Pesanan tetap diproses oleh penjual.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Menggambar kotak-kotak acak (seed tetap) dengan tiga penanda sudut agar mirip QR. */
private fun DrawScope.drawFakeQr() {
    val cells = 25
    val cell = size.width / cells
    val random = Random(seed = 10)
    for (row in 0 until cells) {
        for (col in 0 until cells) {
            if (isInFinder(row, col, cells)) continue
            if (random.nextBoolean()) {
                drawRect(Color.Black, Offset(col * cell, row * cell), Size(cell, cell))
            }
        }
    }
    listOf(0 to 0, 0 to cells - 7, cells - 7 to 0).forEach { (row, col) ->
        drawFinder(Offset(col * cell, row * cell), cell)
    }
}

private fun isInFinder(row: Int, col: Int, cells: Int): Boolean {
    val nearTop = row < 8
    val nearLeft = col < 8
    val nearRight = col >= cells - 8
    val nearBottom = row >= cells - 8
    return (nearTop && nearLeft) || (nearTop && nearRight) || (nearBottom && nearLeft)
}

private fun DrawScope.drawFinder(topLeft: Offset, cell: Float) {
    drawRect(Color.Black, topLeft, Size(cell * 7, cell * 7))
    drawRect(Color.White, topLeft + Offset(cell, cell), Size(cell * 5, cell * 5))
    drawRect(Color.Black, topLeft + Offset(cell * 2, cell * 2), Size(cell * 3, cell * 3))
}

@Preview(showBackground = true)
@Composable
private fun QrisSimulationCardPreview() {
    CanteenCareTheme { QrisSimulationCard(totalPrice = 28000, modifier = Modifier.padding(16.dp)) }
}
