package com.mondiro.tikgrab

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_PROGRESS, "Đang tải", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Tiến độ tải video"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, "Tải xong", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Báo khi video đã được lưu"
                enableVibration(false)
                setSound(null, null)
            }
        )
    }

    companion object {
        const val CH_PROGRESS = "progress"
        const val CH_DONE = "done"
    }
}
