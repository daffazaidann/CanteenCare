package com.kelompok10.canteencare.ui.pembeli.target

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelompok10.canteencare.domain.model.Nutrient
import com.kelompok10.canteencare.domain.model.NutrientKind
import com.kelompok10.canteencare.domain.model.NutritionTarget
import com.kelompok10.canteencare.domain.validator.TargetValidator
import com.kelompok10.canteencare.ui.components.ErrorView
import com.kelompok10.canteencare.ui.components.LoadingView
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme

@Composable
fun NutritionTargetRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: NutritionTargetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    NutritionTargetScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::load,
        onValueChange = viewModel::onValueChange,
        onResetToDefault = viewModel::onResetToDefault,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionTargetScreen(
    uiState: NutritionTargetUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onValueChange: (Nutrient, String) -> Unit,
    onResetToDefault: () -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(bottomStart = 23.dp, bottomEnd = 23.dp),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "Target Gizi Harian",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        },
        bottomBar = {
            if (!uiState.isLoading && uiState.loadError == null) {
                SaveBar(uiState.isSaving, onSave)
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when {
            uiState.isLoading -> LoadingView(modifier)
            uiState.loadError != null -> ErrorView(uiState.loadError, onRetry, modifier)
            else -> Column(
                modifier = modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionHeader("Target yang ingin dicapai", "Usahakan asupan hari ini mendekati angka ini.")
                Nutrient.entries.filter { it.kind == NutrientKind.GOAL }.forEach { nutrient ->
                    TargetField(nutrient, uiState, onValueChange)
                }
                SectionHeader("Batas maksimal", "Usahakan asupan hari ini tidak melewati angka ini.")
                Nutrient.entries.filter { it.kind == NutrientKind.LIMIT }.forEach { nutrient ->
                    TargetField(nutrient, uiState, onValueChange)
                }
                uiState.saveError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = onResetToDefault) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Kembalikan ke default", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TargetField(nutrient: Nutrient, uiState: NutritionTargetUiState, onValueChange: (Nutrient, String) -> Unit) {
    val isInvalid = nutrient in uiState.invalidFields
    OutlinedTextField(
        value = uiState.inputs[nutrient].orEmpty(),
        onValueChange = { onValueChange(nutrient, it) },
        label = { Text(nutrient.label) },
        suffix = { Text(nutrient.unit) },
        isError = isInvalid,
        supportingText = if (isInvalid) {
            { Text("Isi dengan angka lebih dari 0") }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SaveBar(isSaving: Boolean, onSave: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Button(
            onClick = onSave,
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
                Text("Simpan Target")
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun NutritionTargetScreenPreview() {
    CanteenCareTheme {
        NutritionTargetScreen(
            uiState = NutritionTargetUiState(
                isLoading = false,
                inputs = Nutrient.entries.associateWith { TargetValidator.toInput(NutritionTarget.DEFAULT.valueOf(it)) } +
                    (Nutrient.SUGAR to "0"),
                invalidFields = setOf(Nutrient.SUGAR),
            ),
            onBack = {}, onRetry = {}, onValueChange = { _, _ -> }, onResetToDefault = {}, onSave = {},
        )
    }
}
