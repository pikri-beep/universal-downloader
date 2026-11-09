package com.universaldownloader.android.engine

import android.content.Context
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class MediaMetadata(
    val id: String,
    val title: String,
    val author: String,
    val durationSeconds: Long,
    val thumbnail: String?,
    val originalUrl: String,
    val videoResolutions: List<String> = listOf("1080p", "720p", "480p", "360p"),
    val audioBitrates: List<String> = listOf("320k", "192k", "128k"),
    val directVideoUrl: String? = null,
    val directAudioUrl: String? = null,
    val directImages: List<String> = emptyList(),
    val isPhotoPost: Boolean = false
)

data class DownloadTaskSpec(
    val id: String,
    val url: String,
    val type: String, // "video", "audio", "thumbnail", "photo"
    val quality: String, // "1080p", "720p", "320k", "original"
    val outputDir: File,
    val titleHint: String = "media",
    val directUrl: String? = null,
    val directImages: List<String> = emptyList()
)

object YoutubeDLEngine {

    private const val TAG = "YoutubeDLEngine"
    private val initMutex = Mutex()
    @Volatile
    private var isInitialized = false

    suspend fun ensureInitialized(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext Result.success(Unit)
        initMutex.withLock {
            if (isInitialized) return@withContext Result.success(Unit)
            try {
                Log.d(TAG, "Initializing YoutubeDL and FFmpeg...")
                YoutubeDL.getInstance().init(context.applicationContext)
                try {
                    FFmpeg.getInstance().init(context.applicationContext)
                } catch (fe: Exception) {
                    Log.w(TAG, "FFmpeg initialization warning: ${fe.message}")
                }
                isInitialized = true
                Log.d(TAG, "YoutubeDL engine successfully initialized.")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize YoutubeDL engine", e)
                Result.failure(e)
            }
        }
    }

    private fun isTikTokUrl(url: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        return lower.contains("tiktok.com") || lower.contains("douyin.com")
    }

    private fun inspectTikTokViaTikWM(url: String): MediaMetadata? {
        var conn: HttpURLConnection? = null
        return try {
            val apiUrl = "https://www.tikwm.com/api/?url=" + URLEncoder.encode(url, "UTF-8")
            val u = URL(apiUrl)
            conn = u.openConnection() as HttpURLConnection
            conn.connectTimeout = 8_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conn.connect()

            if (conn.responseCode != 200) return null
            val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)
            if (json.optInt("code", -1) != 0) return null

            val data = json.getJSONObject("data")
            val id = data.optString("id", System.currentTimeMillis().toString())
            val rawTitle = data.optString("title").trim()
            val title = if (rawTitle.isEmpty()) "TikTok Media" else rawTitle
            val authorObj = data.optJSONObject("author")
            val authorName = authorObj?.optString("nickname")
                ?.ifEmpty { authorObj.optString("unique_id") }
                ?: "TikTok User"
            val duration = data.optLong("duration", 0L)
            val cover = data.optString("cover")
            val play = data.optString("play")
            val music = data.optString("music")

            val imagesArr = data.optJSONArray("images")
            val imagesList = mutableListOf<String>()
            if (imagesArr != null) {
                for (i in 0 until imagesArr.length()) {
                    val imgUrl = imagesArr.optString(i)
                    if (imgUrl.isNotEmpty()) imagesList.add(imgUrl)
                }
            }
            val isPhoto = imagesList.isNotEmpty()

            MediaMetadata(
                id = id,
                title = title,
                author = "@$authorName",
                durationSeconds = duration,
                thumbnail = if (isPhoto) (imagesList.firstOrNull() ?: cover) else cover,
                originalUrl = url,
                videoResolutions = if (isPhoto) emptyList() else listOf("Original HD"),
                audioBitrates = listOf("320k (HD)", "192k (Standard)"),
                directVideoUrl = if (isPhoto) null else play.ifEmpty { null },
                directAudioUrl = music.ifEmpty { null },
                directImages = imagesList,
                isPhotoPost = isPhoto
            )
        } catch (e: Exception) {
            Log.w(TAG, "TikWM fast inspect skipped: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun inspectUrl(context: Context, url: String): Result<MediaMetadata> = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()

        // 1. Instant check for TikTok
        if (isTikTokUrl(cleanUrl)) {
            val tikWmResult = inspectTikTokViaTikWM(cleanUrl)
            if (tikWmResult != null) {
                return@withContext Result.success(tikWmResult)
            }
        }

        // 2. YoutubeDL inspection with suppressed warnings and no-update flag
        val initRes = ensureInitialized(context)
        if (initRes.isFailure) {
            val err = initRes.exceptionOrNull()
            return@withContext Result.failure(err ?: Exception("Gagal inisialisasi engine"))
        }

        try {
            val req = YoutubeDLRequest(cleanUrl).apply {
                addOption("--dump-json")
                addOption("--no-playlist")
                addOption("--no-warnings")
                addOption("--no-update")
            }
            val info: VideoInfo = YoutubeDL.getInstance().getInfo(req)
            val metadata = MediaMetadata(
                id = info.id ?: System.currentTimeMillis().toString(),
                title = info.title ?: "Media Download",
                author = info.uploader ?: "Unknown Author",
                durationSeconds = info.duration.toLong(),
                thumbnail = info.thumbnail,
                originalUrl = cleanUrl
            )
            Result.success(metadata)
        } catch (e: Exception) {
            val friendly = sanitizeErrorMessage(e.message)
            Result.failure(Exception(friendly))
        }
    }

    suspend fun download(
        context: Context,
        spec: DownloadTaskSpec,
        onProgress: (progress: Float, etaSeconds: Long, line: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        // Direct Multiple Photos
        if (spec.type == "photo" || (spec.directImages.isNotEmpty() && spec.type != "audio" && spec.type != "video")) {
            return@withContext downloadDirectPhotos(context, spec, onProgress)
        }

        // Direct Stream (TikTok MP4 / MP3)
        val directStreamUrl = when (spec.type) {
            "audio" -> spec.directUrl
            else -> spec.directUrl
        }
        if (!directStreamUrl.isNullOrEmpty()) {
            val safeTitle = sanitizeFileName(spec.titleHint)
            val ext = if (spec.type == "audio") "mp3" else "mp4"
            val targetFile = File(spec.outputDir, "$safeTitle.$ext")
            return@withContext downloadDirectStream(directStreamUrl, targetFile, onProgress)
        }

        // Standard YoutubeDL Download
        val initRes = ensureInitialized(context)
        if (initRes.isFailure) {
            val err = initRes.exceptionOrNull()
            return@withContext Result.failure(err ?: Exception("Gagal inisialisasi engine"))
        }

        try {
            val req = YoutubeDLRequest(spec.url).apply {
                addOption("--newline")
                addOption("--no-mtime")
                addOption("--windows-filenames")
                addOption("--no-warnings")
                addOption("--no-update")
                addOption("-o", "${spec.outputDir.absolutePath}/%(title).150s [%(id)s].%(ext)s")

                when (spec.type) {
                    "audio" -> {
                        addOption("-x")
                        addOption("--audio-format", "mp3")
                        val bitrate = spec.quality.replace("k", "").filter { it.isDigit() }.ifEmpty { "192" }
                        addOption("--audio-quality", bitrate)
                    }
                    "thumbnail" -> {
                        addOption("--skip-download")
                        addOption("--write-thumbnail")
                        addOption("--convert-thumbnails", "jpg")
                    }
                    else -> { // video
                        val res = spec.quality.replace("p", "").filter { it.isDigit() }.ifEmpty { "1080" }
                        addOption("-f", "bestvideo[height<=$res]+bestaudio/best[height<=$res]/best")
                        addOption("--merge-output-format", "mp4")
                    }
                }
            }

            YoutubeDL.getInstance().execute(req) { progress, etaInSeconds, line ->
                val cleanLine = line ?: ""
                val speedMatch = Regex("""at\s+([\d.]+\s*[KMGT]?i?B/s)""").find(cleanLine)?.groupValues?.get(1) ?: ""
                val isMerging = cleanLine.contains("[Merger]") || cleanLine.contains("[ffmpeg]") || cleanLine.contains("Merging")
                val isExtracting = cleanLine.contains("[ExtractAudio]")

                val reportedProgress = when {
                    isMerging || isExtracting -> 95f
                    progress in 0f..100f -> progress
                    else -> 0f
                }
                val statusMessage = when {
                    isMerging -> "Menggabungkan FFmpeg..."
                    isExtracting -> "Mengekstrak MP3..."
                    speedMatch.isNotEmpty() -> speedMatch
                    else -> ""
                }
                onProgress(reportedProgress, etaInSeconds, statusMessage)
            }

            val downloadedFiles = spec.outputDir.listFiles { file ->
                file.isFile && file.lastModified() >= System.currentTimeMillis() - 120_000
            }?.sortedByDescending { it.lastModified() }

            val targetFile = downloadedFiles?.firstOrNull()
                ?: File(spec.outputDir, "${sanitizeFileName(spec.titleHint)}.mp4")

            Result.success(targetFile)
        } catch (e: Exception) {
            val friendly = sanitizeErrorMessage(e.message)
            Result.failure(Exception(friendly))
        }
    }

    private suspend fun downloadDirectStream(
        streamUrl: String,
        targetFile: File,
        onProgress: (progress: Float, etaSeconds: Long, line: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            var currentUrl = streamUrl
            var connection: HttpURLConnection
            var redirectCount = 0
            while (true) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connection.connect()

                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
                    val loc = connection.getHeaderField("Location")
                    if (loc != null && redirectCount < 5) {
                        currentUrl = loc
                        redirectCount++
                        continue
                    }
                }
                break
            }

            val totalBytes = connection.contentLengthLong
            targetFile.parentFile?.mkdirs()
            if (targetFile.exists()) targetFile.delete()

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesReadTotal = 0L
                    var lastTime = System.currentTimeMillis()
                    var bytesSinceLastTime = 0L

                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesReadTotal += read
                        bytesSinceLastTime += read

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastTime
                        if (elapsed >= 250) {
                            val speedBytesPerSec = if (elapsed > 0) (bytesSinceLastTime * 1000) / elapsed else 0L
                            val speedStr = formatSpeed(speedBytesPerSec)
                            val progress = if (totalBytes > 0) {
                                (bytesReadTotal.toFloat() / totalBytes) * 100f
                            } else 50f
                            val remainingBytes = if (totalBytes > bytesReadTotal) totalBytes - bytesReadTotal else 0L
                            val etaSec = if (speedBytesPerSec > 0) remainingBytes / speedBytesPerSec else 0L

                            onProgress(progress, etaSec, speedStr)

                            lastTime = now
                            bytesSinceLastTime = 0L
                        }
                    }
                }
            }
            onProgress(100f, 0L, "Selesai")
            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Direct stream download failed", e)
            Result.failure(e)
        }
    }

    private suspend fun downloadDirectPhotos(
        context: Context,
        spec: DownloadTaskSpec,
        onProgress: (progress: Float, etaSeconds: Long, line: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val safeTitle = sanitizeFileName(spec.titleHint)
            val total = spec.directImages.size
            var firstFile: File? = null

            spec.directImages.forEachIndexed { index, imgUrl ->
                val targetFile = File(spec.outputDir, "${safeTitle}_${index + 1}.jpg")
                if (firstFile == null) firstFile = targetFile

                onProgress(((index.toFloat()) / total) * 100f, 0L, "Foto ${index + 1}/$total")

                downloadSingleFile(imgUrl, targetFile)

                com.universaldownloader.android.service.MediaStoreHelper.scanAndRegisterMedia(
                    context,
                    targetFile,
                    "image/jpeg",
                    "${spec.titleHint} (${index + 1})"
                )
            }

            onProgress(100f, 0L, "Semua $total foto tersimpan!")
            Result.success(firstFile ?: File(spec.outputDir, "${safeTitle}_1.jpg"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading photo carousel", e)
            Result.failure(e)
        }
    }

    private fun downloadSingleFile(fileUrl: String, targetFile: File) {
        var currentUrl = fileUrl
        var conn: HttpURLConnection
        var redirects = 0
        while (true) {
            conn = URL(currentUrl).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conn.connect()

            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
                val loc = conn.getHeaderField("Location")
                if (loc != null && redirects < 5) {
                    currentUrl = loc
                    redirects++
                    continue
                }
            }
            break
        }

        targetFile.parentFile?.mkdirs()
        conn.inputStream.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output, bufferSize = 32 * 1024)
            }
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .take(100)
            .ifEmpty { "media_${System.currentTimeMillis()}" }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", bytesPerSec.toDouble() / (1024 * 1024))
            bytesPerSec >= 1024 -> String.format(Locale.US, "%.1f KB/s", bytesPerSec.toDouble() / 1024)
            bytesPerSec > 0 -> "$bytesPerSec B/s"
            else -> ""
        }
    }

    private fun sanitizeErrorMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return "Gagal memproses media. Periksa koneksi atau tautan."
        val lines = raw.lines()
            .map { it.trim() }
            .filter { line ->
                !line.startsWith("WARNING:", ignoreCase = true) &&
                !line.startsWith("It is strongly recommended", ignoreCase = true) &&
                !line.startsWith("Run \"yt-dlp", ignoreCase = true) &&
                !line.startsWith("To suppress this warning", ignoreCase = true) &&
                line.isNotBlank()
            }
        val errorLine = lines.find { it.contains("ERROR:", ignoreCase = true) }
            ?: lines.firstOrNull()
            ?: "Terjadi kesalahan saat memproses media."
        return errorLine
            .replace(Regex("(?i)ERROR:\\s*"), "")
            .replace(Regex("(?i)\\[generic\\]\\s*"), "")
            .trim()
    }

    suspend fun updateEngine(context: Context): Result<YoutubeDL.UpdateStatus> = withContext(Dispatchers.IO) {
        val initRes = ensureInitialized(context)
        if (initRes.isFailure) {
            val err = initRes.exceptionOrNull()
            return@withContext Result.failure(err ?: Exception("Gagal inisialisasi engine"))
        }

        try {
            val status = YoutubeDL.getInstance().updateYoutubeDL(context)
                ?: YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE
            Result.success(status)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
