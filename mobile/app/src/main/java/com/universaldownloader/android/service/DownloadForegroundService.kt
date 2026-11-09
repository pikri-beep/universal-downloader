package com.universaldownloader.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.universaldownloader.android.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class DownloadForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var observerJob: Job? = null
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val ACTION_START = "com.universaldownloader.android.action.START"
        const val ACTION_STOP = "com.universaldownloader.android.action.STOP"

        private const val ONGOING_NOTIFICATION_ID = 1001
        private const val CHANNEL_PROGRESS_ID = "downloader_channel_progress"
        private const val CHANNEL_COMPLETE_ID = "downloader_channel_complete"

        fun notifyTaskFinished(context: Context, task: DownloadTask) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val file = task.filePath?.let { File(it) }

            val contentIntent = if (file != null && file.exists()) {
                val uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (e: Exception) {
                    Uri.fromFile(file)
                }
                val mimeType = when (task.type) {
                    "audio" -> "audio/mpeg"
                    "thumbnail", "image" -> "image/jpeg"
                    else -> "video/mp4"
                }
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                PendingIntent.getActivity(
                    context,
                    task.id.hashCode(),
                    viewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else null

            val notif = NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Selesai Mengunduh")
                .setContentText(task.title)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            nm.notify(task.id.hashCode(), notif)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForegroundService()
            return START_NOT_STICKY
        }

        startForegroundWithNotification()
        observeQueueState()
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildOngoingNotification("Menyiapkan unduhan...", 0, "")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    ONGOING_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(ONGOING_NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        }
    }

    private fun observeQueueState() {
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            DownloadQueueManager.currentTask.collect { task ->
                if (task != null && task.status == TaskStatus.DOWNLOADING) {
                    val progressInt = task.progress.toInt()
                    val details = "${task.speedStr} ${task.etaStr}".trim()
                    val notif = buildOngoingNotification(task.title, progressInt, details)
                    notificationManager.notify(ONGOING_NOTIFICATION_ID, notif)
                } else {
                    // Check if all tasks in queue are done
                    val hasActiveTasks = DownloadQueueManager.tasks.value.any {
                        it.status == TaskStatus.QUEUED || it.status == TaskStatus.DOWNLOADING
                    }
                    if (!hasActiveTasks) {
                        stopForegroundService()
                    }
                }
            }
        }
    }

    private fun buildOngoingNotification(title: String, progress: Int, details: String): Notification {
        val appIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_PROGRESS_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(if (details.isNotEmpty()) "$progress% • $details" else "$progress%")
            .setProgress(100, progress, progress <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun stopForegroundService() {
        observerJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS_ID,
                "Progres Unduhan",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menampilkan status dan kemajuan unduhan yang sedang berjalan"
                setShowBadge(false)
            }

            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETE_ID,
                "Unduhan Selesai",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pemberitahuan saat file media berhasil disimpan ke galeri"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(completeChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observerJob?.cancel()
        super.onDestroy()
    }
}
