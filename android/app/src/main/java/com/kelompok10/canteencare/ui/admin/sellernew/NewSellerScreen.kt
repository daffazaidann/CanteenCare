package com.kelompok10.canteencare.ui.admin.sellernew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Scaffold
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
import com.kelompok10.canteencare.domain.validator.SellerField
import com.kelompok10.canteencare.ui.components.CanteenTopAppBar
import com.kelompok10.canteencare.ui.components.FormTextField
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme

@Composable
fun NewSellerRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: NewSellerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }
    NewSellerScreen(
        uiState = uiState,
        onBack = onBack,
        onFullNameChange = viewModel::onFullNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onVendorNameChange = viewModel::onVendorNameChange,
        onVendorDescriptionChange = viewModel::onVendorDescriptionChange,
        onOpenHoursChange = viewModel::onOpenHoursChange,
        onSubmit = viewModel::submit,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSellerScreen(
    uiState: NewSellerUiState,
    onBack: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onVendorNameChange: (String) -> Unit,
    onVendorDescriptionChange: (String) -> Unit,
    onOpenHoursChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Scaffold(
        topBar = {
            CanteenTopAppBar(
                title = "Penjual Baru",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSaving,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .height(52.dp),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Buat Akun Penjual")
                    }
                }
            }
        },
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
            Text("Akun Penjual", style = MaterialTheme.typography.titleMedium)
            FormTextField(uiState.fullName, onFullNameChange, "Nama pemilik", SellerField.FULL_NAME in uiState.invalidFields, "Wajib diisi")
            FormTextField(
                uiState.email, onEmailChange, "Email", SellerField.EMAIL in uiState.invalidFields,
                "Format email tidak valid", KeyboardType.Email,
            )
            FormTextField(
                uiState.password, onPasswordChange, "Password awal", SellerField.PASSWORD in uiState.invalidFields,
                "Minimal 6 karakter", KeyboardType.Password,
            )
            Text(
                "Berikan email dan password ini kepada penjual untuk login.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Data Stan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            FormTextField(uiState.vendorName, onVendorNameChange, "Nama stan", SellerField.VENDOR_NAME in uiState.invalidFields, "Wajib diisi")
            FormTextField(uiState.vendorDescription, onVendorDescriptionChange, "Deskripsi stan (opsional)")
            FormTextField(uiState.openHours, onOpenHoursChange, "Jam buka (opsional), contoh 07.00 - 15.00")

            uiState.saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NewSellerScreenPreview() {
    CanteenCareTheme {
        NewSellerScreen(
            uiState = NewSellerUiState(fullName = "Pak Joko", invalidFields = setOf(SellerField.EMAIL)),
            onBack = {}, onFullNameChange = {}, onEmailChange = {}, onPasswordChange = {},
            onVendorNameChange = {}, onVendorDescriptionChange = {}, onOpenHoursChange = {}, onSubmit = {},
        )
    }
}
