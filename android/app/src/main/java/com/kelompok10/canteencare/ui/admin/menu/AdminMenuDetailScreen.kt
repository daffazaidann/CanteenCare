package com.kelompok10.canteencare.ui.admin.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelompok10.canteencare.domain.model.Menu
import com.kelompok10.canteencare.domain.model.MenuCategory
import com.kelompok10.canteencare.domain.model.MenuStatus
import com.kelompok10.canteencare.domain.model.Nutrient
import com.kelompok10.canteencare.domain.model.Nutrition
import com.kelompok10.canteencare.domain.validator.MenuField
import com.kelompok10.canteencare.ui.components.CanteenTopAppBar
import com.kelompok10.canteencare.ui.components.ErrorView
import com.kelompok10.canteencare.ui.components.FormTextField
import com.kelompok10.canteencare.ui.components.LoadingView
import com.kelompok10.canteencare.ui.components.MenuStatusChip
import com.kelompok10.canteencare.ui.components.VerifiedBadge
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme
import kotlinx.coroutines.launch

@Composable
fun AdminMenuDetailRoute(
    onBack: () -> Unit,
    viewModel: AdminMenuDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            viewModel.onMessageShown()
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    AdminMenuDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = AdminMenuActions(
            onBack = onBack,
            onRetry = viewModel::load,
            onNameChange = viewModel::onNameChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onCategoryChange = viewModel::onCategoryChange,
            onPriceChange = viewModel::onPriceChange,
            onImageUrlChange = viewModel::onImageUrlChange,
            onSaveMenu = viewModel::saveMenuData,
            onNutritionChange = viewModel::onNutritionChange,
            onVerifyNutrition = viewModel::verifyNutrition,
            onApprove = viewModel::approve,
            onReject = viewModel::onRejectClick,
        ),
    )

    if (uiState.showRejectDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissRejectDialog,
            title = { Text("Tolak menu ini?") },
            text = { Text("Menu tidak akan tampil untuk pembeli. Penjual bisa mengajukan menu baru.") },
            confirmButton = { TextButton(onClick = viewModel::reject) { Text("Tolak") } },
            dismissButton = { TextButton(onClick = viewModel::onDismissRejectDialog) { Text("Batal") } },
        )
    }
}

/** Kumpulan callback layar, supaya parameter fungsi screen tidak terlalu panjang. */
data class AdminMenuActions(
    val onBack: () -> Unit,
    val onRetry: () -> Unit,
    val onNameChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onCategoryChange: (MenuCategory) -> Unit,
    val onPriceChange: (String) -> Unit,
    val onImageUrlChange: (String) -> Unit,
    val onSaveMenu: () -> Unit,
    val onNutritionChange: (Nutrient, String) -> Unit,
    val onVerifyNutrition: () -> Unit,
    val onApprove: () -> Unit,
    val onReject: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMenuDetailScreen(
    uiState: AdminMenuDetailUiState,
    snackbarHostState: SnackbarHostState,
    actions: AdminMenuActions,
) {
    val menu = uiState.menu
    Scaffold(
        topBar = {
            CanteenTopAppBar(
                title = "Detail Menu",
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
        bottomBar = {
            if (menu != null && (uiState.canApprove || uiState.canReject || menu.status == MenuStatus.PENDING)) {
                DecisionBar(uiState, actions.onApprove, actions.onReject)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when {
            uiState.isLoading && menu == null -> LoadingView(modifier)
            menu == null -> ErrorView(uiState.loadError ?: "Menu tidak ditemukan.", actions.onRetry, modifier)
            else -> Column(
                modifier = modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Header(menu)
                MenuDataCard(uiState, actions)
                NutritionCard(uiState, actions)
            }
        }
    }
}

@Composable
private fun Header(menu: Menu) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(menu.name, style = MaterialTheme.typography.headlineSmall)
        Text(menu.vendorName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuStatusChip(menu.status)
            VerifiedBadge(menu.isNutritionVerified)
        }
        Text("Stok saat ini: ${menu.stock} (diatur oleh penjual)", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuDataCard(uiState: AdminMenuDetailUiState, actions: AdminMenuActions) {
    SectionCard("Data Menu") {
        FormTextField(uiState.name, actions.onNameChange, "Nama menu", MenuField.NAME in uiState.invalidFields, "Wajib diisi")
        OutlinedTextField(
            value = uiState.description,
            onValueChange = actions.onDescriptionChange,
            label = { Text("Deskripsi") },
            minLines = 2,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            MenuCategory.entries.forEachIndexed { index, category ->
                SegmentedButton(
                    selected = uiState.category == category,
                    onClick = { actions.onCategoryChange(category) },
                    shape = SegmentedButtonDefaults.itemShape(index, MenuCategory.entries.size),
                ) { Text(category.label) }
            }
        }
        FormTextField(
            uiState.price, actions.onPriceChange, "Harga (Rp)", MenuField.PRICE in uiState.invalidFields,
            "Bilangan bulat ≥ 0", KeyboardType.Number,
        )
        FormTextField(
            uiState.imageUrl, actions.onImageUrlChange, "URL gambar (opsional)", MenuField.IMAGE_URL in uiState.invalidFields,
            "Harus diawali http:// atau https://", KeyboardType.Uri,
        )
        FilledTonalButton(
            onClick = actions.onSaveMenu,
            enabled = !uiState.isSavingMenu,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (uiState.isSavingMenu) "Menyimpan..." else "Simpan Data Menu") }
    }
}

@Composable
private fun NutritionCard(uiState: AdminMenuDetailUiState, actions: AdminMenuActions) {
    SectionCard("Gizi per Porsi") {
        Text(
            if (uiState.menu?.isNutritionVerified == true) {
                "Gizi sudah diverifikasi. Ubah lalu simpan lagi jika perlu koreksi."
            } else {
                "Nilai di bawah adalah estimasi penjual. Periksa, koreksi jika perlu, lalu verifikasi."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Nutrient.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { nutrient ->
                    FormTextField(
                        value = uiState.nutritionInputs[nutrient].orEmpty(),
                        onValueChange = { actions.onNutritionChange(nutrient, it) },
                        label = "${nutrient.label} (${nutrient.unit})",
                        isError = nutrient in uiState.invalidNutrients,
                        errorText = "Angka ≥ 0",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        FilledTonalButton(
            onClick = actions.onVerifyNutrition,
            enabled = !uiState.isSavingNutrition,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (uiState.isSavingNutrition) "Menyimpan..." else "Simpan & Verifikasi Gizi") }
    }
}

@Composable
private fun DecisionBar(uiState: AdminMenuDetailUiState, onApprove: () -> Unit, onReject: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.menu?.isNutritionVerified == false) {
                Text(
                    "Verifikasi gizi terlebih dahulu sebelum menyetujui menu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = uiState.canReject,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) { Text("Tolak") }
                Button(
                    onClick = onApprove,
                    enabled = uiState.canApprove,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    if (uiState.isChangingStatus) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Setujui")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun AdminMenuDetailScreenPreview() {
    val menu = Menu(
        5, 1, "Dapur Gapura", "Nasi Ayam Geprek", null, MenuCategory.MAKANAN, 18000, 10, null,
        MenuStatus.PENDING, Nutrition(590.0, 55.0, 38.0, 22.0, 1150.0, 4.0), false,
    )
    CanteenCareTheme {
        AdminMenuDetailScreen(
            uiState = AdminMenuDetailUiState(
                isLoading = false, menu = menu, name = menu.name, price = "18000",
                nutritionInputs = Nutrient.entries.associateWith { "0" },
            ),
            snackbarHostState = SnackbarHostState(),
            actions = AdminMenuActions({}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}),
        )
    }
}
