package com.kelompok10.canteencare.ui.admin.menus

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.kelompok10.canteencare.core.util.Formatters
import com.kelompok10.canteencare.domain.model.Menu
import com.kelompok10.canteencare.domain.model.MenuCategory
import com.kelompok10.canteencare.domain.model.MenuStatus
import com.kelompok10.canteencare.domain.model.Nutrition
import com.kelompok10.canteencare.ui.components.CanteenTopAppBar
import com.kelompok10.canteencare.ui.components.EmptyView
import com.kelompok10.canteencare.ui.components.ErrorView
import com.kelompok10.canteencare.ui.components.FoodImage
import com.kelompok10.canteencare.ui.components.LoadingView
import com.kelompok10.canteencare.ui.components.MenuStatusChip
import com.kelompok10.canteencare.ui.components.VerifiedBadge
import com.kelompok10.canteencare.ui.components.imageKind
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme

@Composable
fun AdminPendingRoute(onMenuClick: (Int) -> Unit, viewModel: AdminPendingViewModel = hiltViewModel()) {
    AdminMenuListRoute(
        title = "Verifikasi Menu",
        emptyMessage = "Tidak ada menu yang menunggu verifikasi.",
        showFilter = false,
        viewModel = viewModel,
        onMenuClick = onMenuClick,
    )
}

@Composable
fun AdminMenusRoute(onMenuClick: (Int) -> Unit, viewModel: AdminMenusViewModel = hiltViewModel()) {
    AdminMenuListRoute(
        title = "Semua Menu",
        emptyMessage = "Tidak ada menu dengan status ini.",
        showFilter = true,
        viewModel = viewModel,
        onMenuClick = onMenuClick,
    )
}

@Composable
private fun AdminMenuListRoute(
    title: String,
    emptyMessage: String,
    showFilter: Boolean,
    viewModel: AdminMenuListViewModel,
    onMenuClick: (Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onScreenResumed()
        onPauseOrDispose { }
    }
    AdminMenuListScreen(
        title = title,
        emptyMessage = emptyMessage,
        showFilter = showFilter,
        uiState = uiState,
        onFilterSelected = viewModel::onFilterSelected,
        onRefresh = viewModel::onRefresh,
        onRetry = viewModel::onScreenResumed,
        onMenuClick = onMenuClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMenuListScreen(
    title: String,
    emptyMessage: String,
    showFilter: Boolean,
    uiState: AdminMenuListUiState,
    onFilterSelected: (MenuStatus?) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onMenuClick: (Int) -> Unit,
) {
    Scaffold(topBar = { CanteenTopAppBar(title = title) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (showFilter) StatusFilterChips(uiState.filter, onFilterSelected)
            when {
                uiState.isLoading -> LoadingView()
                uiState.errorMessage != null && uiState.menus.isEmpty() -> ErrorView(uiState.errorMessage, onRetry)
                else -> PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (uiState.menus.isEmpty()) {
                            item { EmptyView(emptyMessage, Icons.Filled.Verified, Modifier.fillParentMaxSize()) }
                        }
                        items(uiState.menus, key = { it.id }) { menu ->
                            AdminMenuCard(menu, onClick = { onMenuClick(menu.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusFilterChips(selected: MenuStatus?, onSelected: (MenuStatus?) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = selected == null, onClick = { onSelected(null) }, label = { Text("Semua") })
        MenuStatus.entries.forEach { status ->
            FilterChip(selected = selected == status, onClick = { onSelected(status) }, label = { Text(status.label) })
        }
    }
}

@Composable
private fun AdminMenuCard(menu: Menu, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            FoodImage(menu.imageUrl, menu.category.imageKind(), Modifier.size(64.dp))
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(menu.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${menu.vendorName} · ${Formatters.rupiah(menu.price)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MenuStatusChip(menu.status)
                    VerifiedBadge(menu.isNutritionVerified)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminMenuListScreenPreview() {
    val menus = listOf(
        Menu(
            5, 1, "Dapur Gapura", "Nasi Ayam Geprek", null, MenuCategory.MAKANAN, 18000, 10, null,
            MenuStatus.PENDING, Nutrition(590.0, 55.0, 38.0, 22.0, 1150.0, 4.0), false,
        ),
    )
    CanteenCareTheme {
        AdminMenuListScreen(
            title = "Semua Menu", emptyMessage = "", showFilter = true,
            uiState = AdminMenuListUiState(menus = menus, isLoading = false),
            onFilterSelected = {}, onRefresh = {}, onRetry = {}, onMenuClick = {},
        )
    }
}
