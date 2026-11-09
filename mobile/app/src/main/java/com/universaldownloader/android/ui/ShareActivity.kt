package com.universaldownloader.android.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import com.universaldownloader.android.service.DownloadQueueManager
import com.universaldownloader.android.ui.theme.UniversalDownloaderTheme

class ShareActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawText = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val extractedUrl = extractUrl(rawText)

        if (extractedUrl.isEmpty()) {
            Toast.makeText(this, "Tidak ada tautan media yang ditemukan.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            UniversalDownloaderTheme {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                QuickDownloadBottomSheet(
                    url = extractedUrl,
                    sheetState = sheetState,
                    onDismiss = { finish() },
                    onConfirmDownload = { metadata, type, quality ->
                        val directUrl = when (type) {
                            "video" -> metadata.directVideoUrl
                            "audio" -> metadata.directAudioUrl
                            else -> null
                        }
                        val directImages = if (type == "photo") metadata.directImages else emptyList()

                        DownloadQueueManager.enqueueTask(
                            url = metadata.originalUrl,
                            title = metadata.title,
                            author = metadata.author,
                            thumbnail = metadata.thumbnail,
                            type = type,
                            quality = quality,
                            directUrl = directUrl,
                            directImages = directImages
                        )
                        Toast.makeText(
                            this@ShareActivity,
                            "Unduhan ditambahkan ke antrean 🚀",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                )
            }
        }
    }

    private fun extractUrl(text: String): String {
        val urlRegex = Regex("""https?://[^\s]+""")
        return urlRegex.find(text)?.value?.trim() ?: text.trim()
    }
}
