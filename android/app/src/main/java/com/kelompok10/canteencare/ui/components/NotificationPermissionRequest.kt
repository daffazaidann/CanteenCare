package com.kelompok10.canteencare.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.kelompok10.canteencare.domain.model.Role

/**
 * Meminta izin notifikasi (Android 13+) setelah user login, dengan penjelasan singkat lebih dulu.
 * Ditanyakan sekali per sesi aplikasi; jika ditolak, aplikasi tetap berjalan normal.
 */
@Composable
fun NotificationPermissionRequest(role: Role) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || role == Role.ADMIN) return

    val context = LocalContext.current
    var alreadyAsked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    if (granted || alreadyAsked) return

    val reason = if (role == Role.PENJUAL) {
        "Izinkan notifikasi agar kamu langsung tahu saat ada pesanan baru masuk ke stanmu."
    } else {
        "Izinkan notifikasi agar kamu langsung tahu saat pesananmu diterima, siap diambil, atau ditolak penjual."
    }

    AlertDialog(
        onDismissRequest = { alreadyAsked = true },
        icon = { Icon(Icons.Filled.NotificationsActive, contentDescription = null) },
        title = { Text("Aktifkan notifikasi?") },
        text = { Text(reason) },
        confirmButton = {
            TextButton(onClick = {
                alreadyAsked = true
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text("Izinkan") }
        },
        dismissButton = { TextButton(onClick = { alreadyAsked = true }) { Text("Nanti") } },
    )
}
