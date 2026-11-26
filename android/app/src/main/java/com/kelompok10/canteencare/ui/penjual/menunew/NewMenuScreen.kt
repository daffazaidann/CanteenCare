package com.kelompok10.canteencare.ui.penjual.menunew

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
import androidx.compose.material3.Button
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelompok10.canteencare.domain.model.MenuCategory
import com.kelompok10.canteencare.domain.model.Nutrient
import com.kelompok10.canteencare.domain.validator.MenuField
import com.kelompok10.canteencare.ui.components.CanteenTopAppBar
import com.kelompok10.canteencare.ui.components.FormTextField
import com.kelompok10.canteencare.ui.components.NoticeCard
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme

@Composable
fun NewMenuRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: NewMenuViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    NewMenuScreen(
        uiState = uiState,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onCategoryChange = viewModel::onCategoryChange,
        onPriceChange = viewModel::onPriceChange,
        onStockChange = viewModel::onStockChange,
        onImageUrlChange = viewModel::onImageUrlChange,
        onNutritionChange = viewModel::onNutritionChange,
        onSubmit = viewModel::submit,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMenuScreen(
    uiState: NewMenuUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (MenuCategory) -> Unit,
    onPriceChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onNutritionChange: (Nutrient, String) -> Unit,
    onSubmit: () -> Unit,
) {
    Scaffold(
        topBar = {
            CanteenTopAppBar(
                title = { Text("Menu Baru") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
        bottomBar = { SubmitBar(uiState.isSaving, onSubmit) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NoticeCard("Menu akan diverifikasi admin sebelum tampil untuk pembeli.")

            FormTextField(uiState.name, onNameChange, "Nama menu", MenuField.NAME in uiState.invalidFields, "Nama menu wajib diisi")
            OutlinedTextField(
                value = uiState.description,
                onValueChange = onDescriptionChange,
                label = { Text("Deskripsi (opsional)") },
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Kategori", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                MenuCategory.entries.forEachIndexed { index, category ->
                    SegmentedButton(
                        selected = uiState.category == category,
                        onClick = { onCategoryChange(category) },
                        shape = SegmentedButtonDefaults.itemShape(index, MenuCategory.entries.size),
                    ) { Text(category.label) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormTextField(
                    uiState.price, onPriceChange, "Harga (Rp)", MenuField.PRICE in uiState.invalidFields,
                    "Bilangan bulat ≥ 0", KeyboardType.Number, Modifier.weight(1f),
                )
                FormTextField(
                    uiState.stock, onStockChange, "Stok", MenuField.STOCK in uiState.invalidFields,
                    "Bilangan bulat ≥ 0", KeyboardType.Number, Modifier.weight(1f),
                )
            }
            FormTextField(
                uiState.imageUrl, onImageUrlChange, "URL gambar (opsional)", MenuField.IMAGE_URL in uiState.invalidFields,
                "Harus diawali http:// atau https://", KeyboardType.Uri,
            )

            Text("Estimasi gizi per porsi", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            Text(
                "Isi perkiraan terbaikmu. Admin akan memeriksa dan memverifikasi nilainya.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Enam kolom gizi disusun dua per baris
            Nutrient.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { nutrient ->
                        FormTextField(
                            value = uiState.nutritionInputs[nutrient].orEmpty(),
                            onValueChange = { onNutritionChange(nutrient, it) },
                            label = "${nutrient.label} (${nutrient.unit})",
                            isError = nutrient in uiState.invalidNutrients,
                            errorText = "Angka ≥ 0",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            uiState.saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SubmitBar(isSaving: Boolean, onSubmit: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Button(
            onClick = onSubmit,
            enabled = !isSaving,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .height(52.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Ajukan Menu")
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun NewMenuScreenPreview() {
    CanteenCareTheme {
        NewMenuScreen(
            uiState = NewMenuUiState(name = "Nasi Rawon", invalidFields = setOf(MenuField.PRICE), invalidNutrients = setOf(Nutrient.SUGAR)),
            onBack = {}, onNameChange = {}, onDescriptionChange = {}, onCategoryChange = {}, onPriceChange = {},
            onStockChange = {}, onImageUrlChange = {}, onNutritionChange = { _, _ -> }, onSubmit = {},
        )
    }
}
