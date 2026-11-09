package com.universaldownloader.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.universaldownloader.android.engine.MediaMetadata
import com.universaldownloader.android.engine.YoutubeDLEngine
import com.universaldownloader.android.ui.components.TactileButton
import com.universaldownloader.android.ui.theme.FunimatedBg
import com.universaldownloader.android.ui.theme.FunimatedBorder
import com.universaldownloader.android.ui.theme.FunimatedGreen
import com.universaldownloader.android.ui.theme.FunimatedRed
import com.universaldownloader.android.ui.theme.FunimatedSurface
import com.universaldownloader.android.ui.theme.FunimatedYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickDownloadBottomSheet(
    url: String,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onConfirmDownload: (metadata: MediaMetadata, type: String, quality: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var metadata by remember { mutableStateOf<MediaMetadata?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedType by remember { mutableStateOf("video") } // "video", "audio", "thumbnail", "photo"
    var selectedQuality by remember { mutableStateOf("1080p") }

    LaunchedEffect(url) {
        isLoading = true
        errorMessage = null
        val res = YoutubeDLEngine.inspectUrl(context, url)
        res.onSuccess {
            metadata = it
            isLoading = false
            if (it.isPhotoPost) {
                selectedType = "photo"
                selectedQuality = "orig"
            }
        }.onFailure {
            errorMessage = it.localizedMessage ?: "Gagal membaca tautan media."
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FunimatedBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(FunimatedYellow, shape = RoundedCornerShape(10.dp))
                            .border(2.dp, FunimatedBorder, shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = FunimatedBorder,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Unduh Cepat",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = FunimatedBorder
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(FunimatedSurface, shape = CircleShape)
                        .border(1.5.dp, FunimatedBorder, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = FunimatedBorder,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = FunimatedRed)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Memeriksa & menganalisis media...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = FunimatedBorder
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sedang menyiapkan kualitas terbaik ⚡",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEBEE), shape = RoundedCornerShape(16.dp))
                        .border(2.dp, FunimatedRed, shape = RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Gagal Membaca Media",
                            color = FunimatedRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage ?: "Terjadi kesalahan",
                            color = Color(0xFFC62828),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            } else if (metadata != null) {
                val data = metadata!!

                // Media Preview Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FunimatedSurface, shape = RoundedCornerShape(16.dp))
                        .border(2.dp, FunimatedBorder, shape = RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!data.thumbnail.isNullOrEmpty()) {
                        AsyncImage(
                            model = data.thumbnail,
                            contentDescription = data.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, FunimatedBorder, shape = RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = data.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = FunimatedBorder,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = data.author,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Type Tabs
                if (data.isPhotoPost) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypeTabButton(
                            text = "Foto (${data.directImages.size})",
                            icon = Icons.Default.Photo,
                            selected = selectedType == "photo",
                            onClick = {
                                selectedType = "photo"
                                selectedQuality = "orig"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TypeTabButton(
                            text = "Audio MP3",
                            icon = Icons.Default.MusicNote,
                            selected = selectedType == "audio",
                            onClick = {
                                selectedType = "audio"
                                selectedQuality = "320k"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypeTabButton(
                            text = "Video MP4",
                            icon = Icons.Default.Videocam,
                            selected = selectedType == "video",
                            onClick = {
                                selectedType = "video"
                                selectedQuality = "1080p"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TypeTabButton(
                            text = "Audio MP3",
                            icon = Icons.Default.MusicNote,
                            selected = selectedType == "audio",
                            onClick = {
                                selectedType = "audio"
                                selectedQuality = "320k"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TypeTabButton(
                            text = "Cover",
                            icon = Icons.Default.Photo,
                            selected = selectedType == "thumbnail",
                            onClick = {
                                selectedType = "thumbnail"
                                selectedQuality = "orig"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Additional Info / Quality Selector Chips
                if (data.isPhotoPost && selectedType == "photo") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp))
                            .border(1.5.dp, FunimatedGreen, shape = RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "📸 Siap mengunduh ${data.directImages.size} foto berkualitas asli langsung ke Galeri Foto.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32)
                        )
                    }
                } else if (selectedType == "video") {
                    if (data.directVideoUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp))
                                .border(1.5.dp, FunimatedGreen, shape = RoundedCornerShape(12.dp))
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("1080p", "720p", "480p").forEach { res ->
                                QualityChip(
                                    label = res,
                                    selected = selectedQuality == res,
                                    onClick = { selectedQuality = res },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else if (selectedType == "audio") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("320k (HD)", "192k (Standard)", "128k").forEach { bitrate ->
                            val code = bitrate.split(" ").first()
                            QualityChip(
                                label = bitrate,
                                selected = selectedQuality == code,
                                onClick = { selectedQuality = code },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button
                val buttonText = when {
                    data.isPhotoPost && selectedType == "photo" -> "Unduh Semua ${data.directImages.size} Foto 🚀"
                    selectedType == "audio" -> "Unduh Audio Musik MP3 🎵"
                    selectedType == "thumbnail" -> "Unduh Cover Gambar 🖼️"
                    else -> "Unduh Video Sekarang 🚀"
                }

                TactileButton(
                    text = buttonText,
                    onClick = {
                        onConfirmDownload(data, selectedType, selectedQuality)
                    },
                    backgroundColor = FunimatedYellow,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TypeTabButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                if (selected) FunimatedYellow else FunimatedSurface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                2.dp,
                if (selected) FunimatedBorder else Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) FunimatedBorder else Color.Gray
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) FunimatedBorder else Color.Gray
            )
        }
    }
}

@Composable
private fun QualityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                if (selected) FunimatedGreen else FunimatedSurface,
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                1.5.dp,
                if (selected) FunimatedBorder else Color.LightGray,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = FunimatedBorder
        )
    }
}
