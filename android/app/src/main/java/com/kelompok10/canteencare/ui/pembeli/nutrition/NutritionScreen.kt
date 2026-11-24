package com.kelompok10.canteencare.ui.pembeli.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelompok10.canteencare.core.util.WibTime
import com.kelompok10.canteencare.domain.calculator.NutritionCalculator
import com.kelompok10.canteencare.domain.model.Nutrition
import com.kelompok10.canteencare.domain.model.NutritionTarget
import com.kelompok10.canteencare.ui.components.ErrorView
import com.kelompok10.canteencare.ui.components.LoadingView
import com.kelompok10.canteencare.ui.components.NutrientProgressBar
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme
import java.time.LocalDate

@Composable
fun NutritionRoute(
    onEditTarget: () -> Unit,
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Muat ulang setiap kali layar tampil, supaya target baru dan pesanan baru langsung terhitung
    LifecycleResumeEffect(Unit) {
        viewModel.onScreenResumed()
        onPauseOrDispose { }
    }

    NutritionScreen(
        uiState = uiState,
        onRefresh = viewModel::onRefresh,
        onRetry = viewModel::onScreenResumed,
        onEditTarget = onEditTarget,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    uiState: NutritionUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onEditTarget: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gizi Hari Ini")
                        uiState.date?.let {
                            Text(
                                WibTime.formatLongDate(it),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when {
            uiState.isLoading -> LoadingView(modifier)
            uiState.errorMessage != null && uiState.progress.isEmpty() ->
                ErrorView(uiState.errorMessage, onRetry, modifier)
            else -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item { SummaryCard(uiState.countedOrderCount, onEditTarget) }
                    items(uiState.progress, key = { it.nutrient }) { progress ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                            NutrientProgressBar(progress, Modifier.padding(16.dp))
                        }
                    }
                    item { LegendNote() }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(countedOrderCount: Int, onEditTarget: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    if (countedOrderCount == 0) "Belum ada pesanan yang diterima hari ini"
                    else "$countedOrderCount pesanan diterima hari ini",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            OutlinedButton(onClick = onEditTarget, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Ubah Target Gizi", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun LegendNote() {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            "Kalori, karbohidrat, dan protein adalah target yang sebaiknya dicapai. " +
                "Lemak, garam, dan gula adalah batas maksimal yang sebaiknya tidak dilewati. " +
                "Hanya pesanan yang sudah diterima penjual yang dihitung.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun NutritionScreenPreview() {
    val consumed = Nutrition(1370.0, 200.0, 55.0, 45.0, 1820.0, 76.0)
    CanteenCareTheme {
        NutritionScreen(
            uiState = NutritionUiState(
                date = LocalDate.of(2026, 11, 18),
                isLoading = false,
                progress = NutritionCalculator.calculateProgress(consumed, NutritionTarget.DEFAULT),
                countedOrderCount = 2,
            ),
            onRefresh = {}, onRetry = {}, onEditTarget = {},
        )
    }
}
