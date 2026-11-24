package com.kelompok10.canteencare.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/** Notification channel aplikasi. Wajib ada sejak Android 8 agar notifikasi bisa tampil. */
object NotificationChannels {
    /** Harus sama dengan ANDROID_CHANNEL_ID di backend (notification_service.py). */
    const val ORDERS = "orders"

    fun create(context: Context) {
        val channel = NotificationChannel(ORDERS, "Status Pesanan", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Pesanan baru, sedang dimasak, siap diambil, selesai, dan ditolak"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
