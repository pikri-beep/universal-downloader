package com.universaldownloader.android

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.universaldownloader.android.engine.MediaMetadata
import com.universaldownloader.android.engine.YoutubeDLEngine
import com.universaldownloader.android.service.DownloadQueueManager
import com.universaldownloader.android.service.DownloadTask
import com.universaldownloader.android.service.TaskStatus
import com.universaldownloader.android.ui.components.TactileButton
import com.universaldownloader.android.ui.theme.FunimatedBg
import com.universaldownloader.android.ui.theme.FunimatedBorder
import com.universaldownloader.android.ui.theme.FunimatedGray
import com.universaldownloader.android.ui.theme.FunimatedGreen
import com.universaldownloader.android.ui.theme.FunimatedRed
import com.universaldownloader.android.ui.theme.FunimatedSurface
import com.universaldownloader.android.ui.theme.FunimatedYellow
import com.universaldownloader.android.ui.theme.UniversalDownloaderTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UniversalDownloaderTheme {
                MainAppStudioScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppStudioScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf("download") } // "download", "history"
    var urlInput by remember { mutableStateOf("") }
    var isInspecting by remember { mutableStateOf(false) }
    var inspectedMetadata by remember { mutableStateOf<MediaMetadata?>(null) }
    var inspectError by remember { mutableStateOf<String?>(null) }

    var selectedType by remember { mutableStateOf("video") }
    var selectedQuality by remember { mutableStateOf("1080p") }

    var isUpdatingEngine by remember { mutableStateOf(false) }

    val allTasks by DownloadQueueManager.tasks.collectAsState()
    val currentTask by DownloadQueueManager.currentTask.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(FunimatedYellow, shape = RoundedCornerShape(10.dp))
                                .border(2.dp, FunimatedBorder, shape = RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Universal Downloader",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = FunimatedBorder
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!isUpdatingEngine) {
                                isUpdatingEngine = true
                                scope.launch {
                                    val res = YoutubeDLEngine.updateEngine(context)
                                    isUpdatingEngine = false
                                    res.onSuccess { status ->
                                        Toast.makeText(
                                            context,
                                            "Engine yt-dlp: $status",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }.onFailure { err ->
                                        Toast.makeText(
                                            context,
                                            "Update gagal: ${err.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(end = 14.dp)
                            .size(38.dp)
                            .background(FunimatedSurface, shape = CircleShape)
                            .border(1.5.dp, FunimatedBorder, shape = CircleShape)
                    ) {
                        if (isUpdatingEngine) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = FunimatedBorder
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Perbarui Engine",
                                tint = FunimatedBorder,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FunimatedBg
                )
            )
        },
        containerColor = FunimatedBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Main Switch Tabs: Unduh Baru vs Riwayat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FunimatedGray, shape = RoundedCornerShape(14.dp))
                    .border(2.dp, FunimatedBorder, shape = RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabNavButton(
                    text = "📥 Unduh Baru",
                    selected = activeTab == "download",
                    onClick = { activeTab = "download" },
                    modifier = Modifier.weight(1f)
                )
                TabNavButton(
                    text = "📂 Riwayat (${allTasks.size})",
                    selected = activeTab == "history",
                    onClick = { activeTab = "history" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activeTab == "download") {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Active Download Progress Card
                    if (currentTask != null && currentTask!!.status == TaskStatus.DOWNLOADING) {
                        item {
                            ActiveDownloadCard(task = currentTask!!)
                        }
                    }

                    // Input Card
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FunimatedSurface, shape = RoundedCornerShape(20.dp))
                                .border(2.dp, FunimatedBorder, shape = RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Masukkan Tautan Media",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = FunimatedBorder
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                placeholder = { Text("Tempel link YouTube, TikTok, IG, X...", color = Color.Gray, fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                        if (clip.isNotEmpty()) urlInput = clip
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Tempel",
                                            tint = FunimatedBorder
                                        )
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = FunimatedBg,
                                    unfocusedContainerColor = FunimatedBg,
                                    focusedIndicatorColor = FunimatedBorder,
                                    unfocusedIndicatorColor = Color.LightGray
                                ),
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            TactileButton(
                                text = if (isInspecting) "Memeriksa Media ⚡..." else "Periksa & Siapkan ➔",
                                onClick = {
                                    if (urlInput.isNotBlank() && !isInspecting) {
                                        isInspecting = true
                                        inspectError = null
                                        scope.launch {
                                            val res = YoutubeDLEngine.inspectUrl(context, urlInput.trim())
                                            isInspecting = false
                                            res.onSuccess {
                                                inspectedMetadata = it
                                                if (it.isPhotoPost) {
                                                    selectedType = "photo"
                                                    selectedQuality = "orig"
                                                } else {
                                                    selectedType = "video"
                                                    selectedQuality = "1080p"
                                                }
                                            }.onFailure {
                                                inspectError = it.localizedMessage ?: "Gagal membaca URL."
                                            }
                                        }
                                    }
                                },
                                backgroundColor = FunimatedYellow,
                                enabled = urlInput.isNotBlank() && !isInspecting,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Inspection Result Card
                    if (inspectError != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFEBEE), shape = RoundedCornerShape(16.dp))
                                    .border(2.dp, FunimatedRed, shape = RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Gagal Membaca Tautan",
                                        color = FunimatedRed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = inspectError!!,
                                        color = Color(0xFFC62828),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    if (inspectedMetadata != null) {
                        val meta = inspectedMetadata!!
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FunimatedSurface, shape = RoundedCornerShape(20.dp))
                                    .border(2.dp, FunimatedBorder, shape = RoundedCornerShape(20.dp))
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!meta.thumbnail.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = meta.thumbnail,
                                            contentDescription = meta.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.5.dp, FunimatedBorder, shape = RoundedCornerShape(12.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = meta.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FunimatedBorder,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = meta.author,
                                            fontSize = 13.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Format Tabs
                                if (meta.isPhotoPost) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StudioTabChip(
                                            label = "Foto (${meta.directImages.size})",
                                            icon = Icons.Default.Photo,
                                            selected = selectedType == "photo",
                                            onClick = { selectedType = "photo"; selectedQuality = "orig" },
                                            modifier = Modifier.weight(1f)
                                        )
                                        StudioTabChip(
                                            label = "Audio MP3",
                                            icon = Icons.Default.MusicNote,
                                            selected = selectedType == "audio",
                                            onClick = { selectedType = "audio"; selectedQuality = "320k" },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (selectedType == "photo") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(10.dp))
                                                .border(1.5.dp, FunimatedGreen, shape = RoundedCornerShape(10.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "📸 Akan mengunduh semua ${meta.directImages.size} foto berkualitas asli langsung ke Galeri Foto.",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StudioTabChip(
                                            label = "Video MP4",
                                            icon = Icons.Default.Videocam,
                                            selected = selectedType == "video",
                                            onClick = { selectedType = "video"; selectedQuality = "1080p" },
                                            modifier = Modifier.weight(1f)
                                        )
                                        StudioTabChip(
                                            label = "Audio MP3",
                                            icon = Icons.Default.MusicNote,
                                            selected = selectedType == "audio",
                                            onClick = { selectedType = "audio"; selectedQuality = "320k" },
                                            modifier = Modifier.weight(1f)
                                        )
                                        StudioTabChip(
                                            label = "Cover HD",
                                            icon = Icons.Default.Photo,
                                            selected = selectedType == "thumbnail",
                                            onClick = { selectedType = "thumbnail"; selectedQuality = "orig" },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Quality Options
                                    if (selectedType == "video") {
                                        if (meta.directVideoUrl != null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(10.dp))
                                                    .border(1.5.dp, FunimatedGreen, shape = RoundedCornerShape(10.dp))
                                                    .padding(10.dp)
                                            ) {
                                                Text(
                                                    text = "✨ Video Full HD Tanpa Watermark (Unduhan Instan)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        } else {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf("1080p", "720p", "480p").forEach { q ->
                                                    QualityOptionBadge(
                                                        text = q,
                                                        selected = selectedQuality == q,
                                                        onClick = { selectedQuality = q },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    } else if (selectedType == "audio") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf("320k", "192k", "128k").forEach { q ->
                                                QualityOptionBadge(
                                                    text = q,
                                                    selected = selectedQuality == q,
                                                    onClick = { selectedQuality = q },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                val actionButtonLabel = when {
                                    meta.isPhotoPost && selectedType == "photo" -> "Unduh Semua ${meta.directImages.size} Foto 🚀"
                                    selectedType == "audio" -> "Unduh Audio MP3 🎵"
                                    selectedType == "thumbnail" -> "Unduh Cover Gambar 🖼️"
                                    else -> "Mulai Unduh Sekarang ⚡"
                                }

                                TactileButton(
                                    text = actionButtonLabel,
                                    onClick = {
                                        val directUrl = when (selectedType) {
                                            "video" -> meta.directVideoUrl
                                            "audio" -> meta.directAudioUrl
                                            else -> null
                                        }
                                        val directImages = if (selectedType == "photo") meta.directImages else emptyList()

                                        DownloadQueueManager.enqueueTask(
                                            url = meta.originalUrl,
                                            title = meta.title,
                                            author = meta.author,
                                            thumbnail = meta.thumbnail,
                                            type = selectedType,
                                            quality = selectedQuality,
                                            directUrl = directUrl,
                                            directImages = directImages
                                        )
                                        Toast.makeText(context, "Unduhan dimulai di background!", Toast.LENGTH_SHORT).show()
                                        inspectedMetadata = null
                                        urlInput = ""
                                    },
                                    backgroundColor = FunimatedGreen,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            } else {
                // History List Tab
                if (allTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📂", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Belum ada riwayat unduhan.",
                                fontWeight = FontWeight.Bold,
                                color = FunimatedBorder
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(allTasks, key = { it.id }) { task ->
                            HistoryItemCard(task = task, context = context)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveDownloadCard(task: DownloadTask) {
    val animatedProgress by animateFloatAsState(
        targetValue = (task.progress / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "progressAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FunimatedSurface, shape = RoundedCornerShape(16.dp))
            .border(2.dp, FunimatedBorder, shape = RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(FunimatedYellow, shape = RoundedCornerShape(8.dp))
                        .border(1.5.dp, FunimatedBorder, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⚡ Mengunduh",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = FunimatedBorder
                    )
                }
            }
            Text(
                text = "${task.progress.toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = FunimatedBorder
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = task.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = FunimatedBorder,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Funimated Tactile Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(FunimatedBg, shape = RoundedCornerShape(7.dp))
                .border(2.dp, FunimatedBorder, shape = RoundedCornerShape(7.dp))
                .clip(RoundedCornerShape(7.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(FunimatedYellow)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val statusText = when {
                task.speedStr.contains("Menggabungkan") -> "⚙️ " + task.speedStr
                task.speedStr.contains("Foto") -> "📸 " + task.speedStr
                task.speedStr.isNotEmpty() -> "⬇️ " + task.speedStr
                else -> "Menyiapkan data..."
            }
            Text(
                text = statusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = FunimatedBorder
            )
            if (task.etaStr.isNotEmpty()) {
                Text(
                    text = "ETA: ${task.etaStr}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun HistoryItemCard(task: DownloadTask, context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FunimatedSurface, shape = RoundedCornerShape(14.dp))
            .border(1.5.dp, FunimatedBorder, shape = RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!task.thumbnail.isNullOrEmpty()) {
            AsyncImage(
                model = task.thumbnail,
                contentDescription = task.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, FunimatedBorder, shape = RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = FunimatedBorder,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (task.status) {
                    TaskStatus.COMPLETED -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FunimatedGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Tersimpan di Galeri", fontSize = 11.sp, color = FunimatedGreen)
                    }
                    TaskStatus.FAILED -> {
                        Icon(Icons.Default.Error, contentDescription = null, tint = FunimatedRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Gagal", fontSize = 11.sp, color = FunimatedRed)
                    }
                    else -> {
                        Text(text = "${task.progress.toInt()}%", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        if (task.status == TaskStatus.COMPLETED && !task.filePath.isNullOrEmpty()) {
            IconButton(
                onClick = {
                    val file = File(task.filePath)
                    if (file.exists()) {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val mime = when (task.type) {
                            "audio" -> "audio/*"
                            "thumbnail", "image", "photo" -> "image/*"
                            else -> "video/*"
                        }
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mime)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier
                    .size(34.dp)
                    .background(FunimatedYellow, shape = CircleShape)
                    .border(1.dp, FunimatedBorder, shape = CircleShape)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Buka", tint = FunimatedBorder, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    val file = File(task.filePath)
                    if (file.exists()) {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "*/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Media"))
                    }
                },
                modifier = Modifier
                    .size(34.dp)
                    .background(FunimatedSurface, shape = CircleShape)
                    .border(1.dp, FunimatedBorder, shape = CircleShape)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Bagikan", tint = FunimatedBorder, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun TabNavButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(if (selected) FunimatedSurface else Color.Transparent, shape = RoundedCornerShape(10.dp))
            .border(if (selected) 1.5.dp else 0.dp, if (selected) FunimatedBorder else Color.Transparent, shape = RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) FunimatedBorder else Color.Gray
        )
    }
}

@Composable
fun StudioTabChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(if (selected) FunimatedYellow else FunimatedSurface, shape = RoundedCornerShape(10.dp))
            .border(1.5.dp, if (selected) FunimatedBorder else Color.LightGray, shape = RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (selected) FunimatedBorder else Color.Gray)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) FunimatedBorder else Color.Gray)
        }
    }
}

@Composable
fun QualityOptionBadge(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(if (selected) FunimatedGreen else FunimatedSurface, shape = RoundedCornerShape(8.dp))
            .border(1.5.dp, if (selected) FunimatedBorder else Color.LightGray, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = FunimatedBorder)
    }
}
