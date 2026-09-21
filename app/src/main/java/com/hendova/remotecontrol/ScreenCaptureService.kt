package com.hendova.remotecontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val resultCode = intent?.getIntExtra(
            "resultCode",
            -1
        ) ?: -1

        val data = intent?.getParcelableExtra<Intent>(
            "data"
        ) ?: return START_NOT_STICKY

        val manager = getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        mediaProjection = manager.getMediaProjection(
            resultCode,
            data
        )

        createNotificationChannel()

        val notification = Notification.Builder(
            this,
            "hendova_screen"
        )
            .setContentTitle("HENDOVA")
            .setContentText("Berbagi layar sedang aktif")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()

        startForeground(
            1001,
            notification
        )

        return START_STICKY
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                "hendova_screen",
                "HENDOVA Screen Sharing",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mediaProjection?.stop()
        mediaProjection = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
