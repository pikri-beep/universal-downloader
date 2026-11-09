package com.universaldownloader.android

import android.app.Application
import com.universaldownloader.android.engine.YoutubeDLEngine
import com.universaldownloader.android.service.DownloadQueueManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UniversalDownloaderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DownloadQueueManager.init(this)
        CoroutineScope(Dispatchers.IO).launch {
            YoutubeDLEngine.ensureInitialized(this@UniversalDownloaderApp)
        }
    }
}
