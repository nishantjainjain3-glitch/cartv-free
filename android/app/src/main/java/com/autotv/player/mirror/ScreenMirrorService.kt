package com.autotv.player.mirror

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ScreenMirrorService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var streamServer: LocalStreamServer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopMirroring()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode == Activity.RESULT_OK && resultData != null) {
            startForegroundNotification()
            startStreaming(resultCode, resultData)
        }

        return START_STICKY
    }

    private fun startStreaming(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        streamServer = LocalStreamServer(port = 8080, context = applicationContext).apply {
            start()
        }

        val metrics = resources.displayMetrics
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "CarMirrorDisplay",
            metrics.widthPixels / 2,
            metrics.heightPixels / 2,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            streamServer?.surface,
            null,
            null
        )
    }

    private fun stopMirroring() {
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null
        streamServer?.stop()
        streamServer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundNotification() {
        val channelId = "screen_mirror_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screen Mirroring",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, ScreenMirrorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("CarTV Screen Mirror Active")
            .setContentText("Broadcasting screen to car dashboard")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        stopMirroring()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 2001
        const val ACTION_STOP = "com.autotv.player.ACTION_STOP"
        const val EXTRA_RESULT_CODE = "extra_code"
        const val EXTRA_RESULT_DATA = "extra_data"
    }
}
