package com.kelompok10.canteencare.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.kelompok10.canteencare.ui.components.CanteenTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme

@Composable
fun RegisterRoute(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isRegistered) {
        if (uiState.isRegistered) {
            onRegistered()
            viewModel.onNavigated()
        }
    }

    RegisterScreen(
        uiState = uiState,
        onFullNameChange = viewModel::onFullNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onRegisterClick = viewModel::register,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    uiState: RegisterUiState,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            CanteenTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthHeader(
                title = "Buat Akun Pembeli",
                subtitle = "Daftar untuk mulai memesan dan memantau gizimu",
            )
            AuthTextField(
                value = uiState.fullName,
                onValueChange = onFullNameChange,
                label = "Nama lengkap",
                leadingIcon = Icons.Filled.Person,
            )
            AuthTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = "Email",
                leadingIcon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email,
            )
            PasswordField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = "Password (minimal 6 karakter)",
                leadingIcon = Icons.Filled.Lock,
            )
            PasswordField(
                value = uiState.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Ulangi password",
                leadingIcon = Icons.Filled.LockReset,
                imeAction = ImeAction.Done,
                onImeAction = onRegisterClick,
            )
            AuthErrorText(uiState.errorMessage)
            AuthSubmitButton(text = "Daftar", isLoading = uiState.isLoading, onClick = onRegisterClick)
            TextButton(onClick = onBack, enabled = !uiState.isLoading) {
                Text("Sudah punya akun? Masuk")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    CanteenCareTheme {
        RegisterScreen(
            uiState = RegisterUiState(fullName = "Budi", errorMessage = "Konfirmasi password tidak sama."),
            onFullNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onRegisterClick = {},
            onBack = {},
        )
    }
}
