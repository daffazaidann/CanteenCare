package com.kelompok10.canteencare.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.kelompok10.canteencare.domain.model.Role
import com.kelompok10.canteencare.ui.theme.CanteenCareTheme

/** Satu tab di bottom bar. */
data class BottomTab(val route: String, val label: String, val icon: ImageVector)

/** Tab bottom bar berbeda untuk tiap peran. */
fun tabsFor(role: Role): List<BottomTab> = when (role) {
    Role.PEMBELI -> listOf(
        BottomTab(Routes.HOME, "Beranda", Icons.Filled.Home),
        BottomTab(Routes.NUTRITION, "Gizi", Icons.Filled.MonitorHeart),
        BottomTab(Routes.HISTORY, "Pesanan", Icons.AutoMirrored.Filled.ReceiptLong),
        BottomTab(Routes.PROFILE, "Profil", Icons.Filled.Person),
    )
    Role.PENJUAL -> listOf(
        BottomTab(Routes.SELLER_ORDERS, "Pesanan", Icons.AutoMirrored.Filled.ReceiptLong),
        BottomTab(Routes.SELLER_MENUS, "Menu & Stok", Icons.Filled.Inventory2),
        BottomTab(Routes.PROFILE, "Profil", Icons.Filled.Person),
    )
    Role.ADMIN -> listOf(
        BottomTab(Routes.ADMIN_PENDING, "Verifikasi", Icons.Filled.Verified),
        BottomTab(Routes.ADMIN_MENUS, "Semua Menu", Icons.Filled.RestaurantMenu),
        BottomTab(Routes.ADMIN_SELLERS, "Penjual", Icons.Filled.Storefront),
        BottomTab(Routes.PROFILE, "Profil", Icons.Filled.Person),
    )
}

@Composable
fun RoleBottomBar(
    tabs: List<BottomTab>,
    currentRoute: String?,
    onTabClick: (BottomTab) -> Unit,
) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab.route == currentRoute,
                onClick = { onTabClick(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}

@Preview
@Composable
private fun RoleBottomBarPreview() {
    CanteenCareTheme {
        RoleBottomBar(tabs = tabsFor(Role.PEMBELI), currentRoute = Routes.HOME, onTabClick = {})
    }
}
