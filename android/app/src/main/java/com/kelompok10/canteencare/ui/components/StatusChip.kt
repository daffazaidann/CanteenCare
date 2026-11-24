package com.kelompok10.canteencare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kelompok10.canteencare.domain.model.OrderStatus
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme
import com.kelompok10.canteencare.ui.theme.StatusAmber
import com.kelompok10.canteencare.ui.theme.StatusGreen
import com.kelompok10.canteencare.ui.theme.StatusOrange
import com.kelompok10.canteencare.ui.theme.StatusRed

/** Label berwarna untuk setiap tahap pesanan, dari menunggu sampai selesai. */
@Composable
fun StatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val (color, icon) = statusStyle(status)
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(status.label, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

private fun statusStyle(status: OrderStatus): Pair<Color, ImageVector> = when (status) {
    OrderStatus.MENUNGGU -> StatusAmber to Icons.Filled.Schedule
    OrderStatus.SEDANG_DIMASAK -> StatusOrange to Icons.Filled.Restaurant
    OrderStatus.SIAP_DIAMBIL -> StatusGreen to Icons.Filled.NotificationsActive
    OrderStatus.SELESAI -> StatusGreen to Icons.Filled.CheckCircle
    OrderStatus.DITOLAK -> StatusRed to Icons.Filled.Cancel
}

@Preview(showBackground = true)
@Composable
private fun StatusChipPreview() {
    CanteenCareTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OrderStatus.entries.forEach { StatusChip(it) }
        }
    }
}
