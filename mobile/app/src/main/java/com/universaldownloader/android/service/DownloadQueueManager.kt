package com.universaldownloader.android.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.universaldownloader.android.engine.DownloadTaskSpec
import com.universaldownloader.android.engine.YoutubeDLEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class TaskStatus {
    QUEUED, DOWNLOADING, COMPLETED, FAILED
}

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val author: String = "",
    val thumbnail: String? = null,
    val type: String = "video", // "video", "audio", "thumbnail", "photo"
    val quality: String = "1080p",
    val status: TaskStatus = TaskStatus.QUEUED,
    val progress: Float = 0f,
    val speedStr: String = "",
    val etaStr: String = "",
    val filePath: String? = null,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val directUrl: String? = null,
    val directImages: List<String> = emptyList()
)

object DownloadQueueManager {

    private const val TAG = "DownloadQueueManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queueChannel = Channel<DownloadTask>(Channel.UNLIMITED)

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val _currentTask = MutableStateFlow<DownloadTask?>(null)
    val currentTask: StateFlow<DownloadTask?> = _currentTask.asStateFlow()

    private var isWorkerRunning = false
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        startQueueWorker()
    }

    fun enqueueTask(
        url: String,
        title: String,
        author: String = "",
        thumbnail: String? = null,
        type: String = "video",
        quality: String = "1080p",
        directUrl: String? = null,
        directImages: List<String> = emptyList()
    ): DownloadTask {
        val task = DownloadTask(
            url = url,
            title = title,
            author = author,
            thumbnail = thumbnail,
            type = type,
            quality = quality,
            directUrl = directUrl,
            directImages = directImages
        )

        _tasks.update { listOf(task) + it }
        queueChannel.trySend(task)

        // Start Foreground Service if not already active
        appContext?.let { ctx ->
            val intent = Intent(ctx, DownloadForegroundService::class.java).apply {
                action = DownloadForegroundService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        return task
    }

    private fun startQueueWorker() {
        if (isWorkerRunning) return
        isWorkerRunning = true

        scope.launch {
            for (task in queueChannel) {
                processSingleTask(task)
            }
        }
    }

    private suspend fun processSingleTask(task: DownloadTask) {
        val context = appContext ?: return
        val targetDir = MediaStoreHelper.getDownloadDirectory(task.type)

        updateTask(task.id) {
            it.copy(status = TaskStatus.DOWNLOADING, progress = 0f, speedStr = "Menyiapkan unduhan...")
        }

        val spec = DownloadTaskSpec(
            id = task.id,
            url = task.url,
            type = task.type,
            quality = task.quality,
            outputDir = targetDir,
            titleHint = task.title,
            directUrl = task.directUrl,
            directImages = task.directImages
        )

        val result = YoutubeDLEngine.download(context, spec) { progress, etaSeconds, line ->
            val speedMatch = Regex("""at\s+([\d.]+\s*[KMGT]?i?B/s)""").find(line)?.groupValues?.get(1)
            val displaySpeed = when {
                speedMatch != null -> speedMatch
                line.isNotEmpty() -> line
                else -> ""
            }
            val etaFormatted = if (etaSeconds > 0) "${etaSeconds}s" else ""

            updateTask(task.id) {
                it.copy(
                    progress = progress,
                    speedStr = displaySpeed,
                    etaStr = etaFormatted
                )
            }
        }

        result.onSuccess { savedFile ->
            val mimeType = when (task.type) {
                "audio" -> "audio/mpeg"
                "thumbnail", "image", "photo" -> "image/jpeg"
                else -> "video/mp4"
            }

            MediaStoreHelper.scanAndRegisterMedia(context, savedFile, mimeType, task.title)

            updateTask(task.id) {
                it.copy(
                    status = TaskStatus.COMPLETED,
                    progress = 100f,
                    filePath = savedFile.absolutePath,
                    speedStr = "",
                    etaStr = ""
                )
            }

            // Notify Foreground Service of item completion
            DownloadForegroundService.notifyTaskFinished(context, task.copy(filePath = savedFile.absolutePath))
        }.onFailure { error ->
            Log.e(TAG, "Download failed for task ${task.id}", error)
            updateTask(task.id) {
                it.copy(
                    status = TaskStatus.FAILED,
                    errorMessage = error.localizedMessage ?: "Gagal mengunduh berkas",
                    speedStr = "",
                    etaStr = ""
                )
            }
        }
    }

    private fun updateTask(taskId: String, transform: (DownloadTask) -> DownloadTask) {
        _tasks.update { currentList ->
            currentList.map { task ->
                if (task.id == taskId) {
                    val updated = transform(task)
                    _currentTask.value = if (updated.status == TaskStatus.DOWNLOADING) updated else null
                    updated
                } else {
                    task
                }
            }
        }
    }
}
