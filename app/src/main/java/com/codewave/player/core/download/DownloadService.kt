package com.codewave.player.core.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.codewave.player.CodeWaveApplication
import com.codewave.player.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var observeJob: Job? = null

    companion object {
        const val CHANNEL_ID = "codewave_downloads_channel"
        const val NOTIFICATION_ID = 2001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialNotification = buildNotification("Initializing downloads...", 0, "")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, initialNotification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        observeJob?.cancel()
        observeJob = serviceScope.launch {
            val app = application as CodeWaveApplication
            val downloadManager = app.container.downloadManager

            downloadManager.activeDownloads.collectLatest { activeList ->
                if (activeList.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    val current = activeList.first()
                    val progressPercent = current.progressPercent
                    val speedText = current.speedFormatted
                    val contentText = "${current.artist} — ${current.title} ($speedText)"

                    val notification = buildNotification(contentText, progressPercent, current.title)
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        observeJob?.cancel()
        super.onDestroy()
    }

    private fun buildNotification(text: String, progress: Int, title: String): android.app.Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(if (title.isNotBlank()) "CodeWave Downloader: $title" else "Downloading Audio...")
            .setContentText(text)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time download speed and progress for lossless audio files"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
