package com.kelompok10.canteencare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kelompok10.canteencare.domain.model.OrderStatus
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme

/**
 * Empat langkah pesanan: Menunggu, Dimasak, Siap Diambil, Selesai.
 * Pesanan yang ditolak tidak menampilkan langkah, karena alurnya berhenti.
 */
@Composable
fun OrderProgressSteps(status: OrderStatus, modifier: Modifier = Modifier) {
    if (status == OrderStatus.DITOLAK) return

    val steps = listOf(
        OrderStatus.MENUNGGU,
        OrderStatus.SEDANG_DIMASAK,
        OrderStatus.SIAP_DIAMBIL,
        OrderStatus.SELESAI,
    )
    val currentIndex = steps.indexOf(status).coerceAtLeast(0)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        steps.forEachIndexed { index, step ->
            val reached = index <= currentIndex
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = if (reached) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (index < currentIndex) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (reached) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = step.label,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = if (reached) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OrderProgressStepsPreview() {
    CanteenCareTheme {
        OrderProgressSteps(OrderStatus.SIAP_DIAMBIL, Modifier.padding(16.dp))
    }
}
