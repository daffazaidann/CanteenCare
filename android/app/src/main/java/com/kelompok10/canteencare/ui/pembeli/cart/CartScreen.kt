package com.kelompok10.canteencare.ui.pembeli.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.kelompok10.canteencare.ui.components.CanteenTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelompok10.canteencare.core.util.Formatters
import com.kelompok10.canteencare.domain.calculator.NutritionCalculator
import com.kelompok10.canteencare.domain.model.CartItem
import com.kelompok10.canteencare.domain.model.Nutrition
import com.kelompok10.canteencare.domain.model.PaymentMethod
import com.kelompok10.canteencare.ui.components.EmptyView
import com.kelompok10.canteencare.ui.components.LoadingView
import com.kelompok10.canteencare.ui.components.NutritionTable
import com.kelompok10.canteencare.ui.components.QuantityStepper
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme
import kotlinx.coroutines.launch

@Composable
fun CartRoute(
    onBack: () -> Unit,
    onOrderPlaced: (orderId: Int) -> Unit,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // Scope ini tidak ikut dibatalkan saat LaunchedEffect di bawah berganti key
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.placedOrderId) {
        uiState.placedOrderId?.let { orderId ->
            viewModel.onOrderOpened()
            onOrderPlaced(orderId)
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            viewModel.onErrorShown()
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    CartScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onIncrease = viewModel::onIncrease,
        onDecrease = viewModel::onDecrease,
        onRemove = viewModel::onRemove,
        onPaymentMethodChange = viewModel::onPaymentMethodChange,
        onNoteChange = viewModel::onNoteChange,
        onPlaceOrder = viewModel::placeOrder,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    uiState: CartUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onIncrease: (CartItem) -> Unit,
    onDecrease: (CartItem) -> Unit,
    onRemove: (CartItem) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onNoteChange: (String) -> Unit,
    onPlaceOrder: () -> Unit,
) {
    Scaffold(
        topBar = {
            CanteenTopAppBar(
                title = "Keranjang",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
        bottomBar = {
            if (!uiState.isEmpty) {
                CheckoutBar(uiState.totalPrice, uiState.isPlacingOrder, onPlaceOrder)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when {
            !uiState.isLoaded -> LoadingView(modifier)
            uiState.isEmpty -> EmptyView(
                "Keranjang masih kosong. Pilih menu dari stan di Beranda.",
                Icons.Filled.RemoveShoppingCart,
                modifier,
            )
            else -> LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Pesanan dari ${uiState.vendorName}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                items(uiState.items, key = { it.menuId }) { item ->
                    CartItemCard(item, onIncrease, onDecrease, onRemove)
                }
                item {
                    SectionTitle("Total Gizi Pesanan")
                    Text(
                        "Gizi seluruh isi keranjang, berubah otomatis saat jumlah porsi diubah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    NutritionTable(uiState.totalNutrition)
                }
                item {
                    SectionTitle("Metode Pembayaran")
                    PaymentMethodSelector(uiState.paymentMethod, onPaymentMethodChange)
                }
                if (uiState.paymentMethod == PaymentMethod.QRIS) {
                    item { QrisSimulationCard(uiState.totalPrice) }
                }
                item {
                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = onNoteChange,
                        label = { Text("Catatan untuk penjual (opsional)") },
                        placeholder = { Text("Contoh: sambal dipisah") },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
private fun CartItemCard(
    item: CartItem,
    onIncrease: (CartItem) -> Unit,
    onDecrease: (CartItem) -> Unit,
    onRemove: (CartItem) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${Formatters.rupiah(item.price)} · ${Formatters.nutrient(item.nutrition.calories, "kkal")} per porsi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onRemove(item) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Hapus ${item.name}", tint = MaterialTheme.colorScheme.error)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    Formatters.rupiah(NutritionCalculator.calculateSubtotal(item.price, item.quantity)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                QuantityStepper(
                    quantity = item.quantity,
                    onDecrease = { onDecrease(item) },
                    onIncrease = { onIncrease(item) },
                    canIncrease = item.quantity < item.stock,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodSelector(selected: PaymentMethod, onSelected: (PaymentMethod) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        PaymentMethod.entries.forEachIndexed { index, method ->
            SegmentedButton(
                selected = method == selected,
                onClick = { onSelected(method) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = PaymentMethod.entries.size),
            ) { Text(method.label) }
        }
    }
}

@Composable
private fun CheckoutBar(totalPrice: Int, isPlacingOrder: Boolean, onPlaceOrder: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Total harga", style = MaterialTheme.typography.labelMedium)
                Text(
                    Formatters.rupiah(totalPrice),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Button(
                onClick = onPlaceOrder,
                enabled = !isPlacingOrder,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.height(52.dp),
            ) {
                if (isPlacingOrder) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Pesan Sekarang")
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun CartScreenPreview() {
    val items = listOf(
        CartItem(1, 1, "Dapur Gapura", "Mie Ayam Solo", 20000, 25, 1, Nutrition(650.0, 85.0, 30.0, 25.0, 1800.0, 6.0)),
        CartItem(4, 1, "Dapur Gapura", "Es Teh Manis", 4000, 50, 2, Nutrition(120.0, 30.0, 0.0, 0.0, 10.0, 30.0)),
    )
    CanteenCareTheme {
        CartScreen(
            uiState = CartUiState(
                isLoaded = true,
                items = items,
                totalPrice = NutritionCalculator.calculateTotalPrice(items),
                totalNutrition = NutritionCalculator.calculateOrderNutrition(items),
                paymentMethod = PaymentMethod.QRIS,
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onIncrease = {}, onDecrease = {}, onRemove = {},
            onPaymentMethodChange = {}, onNoteChange = {}, onPlaceOrder = {},
        )
    }
}
