package com.universaldownloader.android.service

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

object MediaStoreHelper {

    private const val TAG = "MediaStoreHelper"
    private const val APP_FOLDER = "UniversalDownloader"

    fun getDownloadDirectory(type: String): File {
        val publicDir = when (type) {
            "audio" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            "thumbnail", "image", "photo" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        }
        val targetFolder = File(publicDir, APP_FOLDER)
        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        }
        return targetFolder
    }

    /**
     * Registers a downloaded file into the Android MediaStore so it immediately
     * appears in the system Gallery and music players with correct metadata.
     */
    fun scanAndRegisterMedia(
        context: Context,
        file: File,
        mimeType: String,
        title: String,
        onComplete: ((Uri?) -> Unit)? = null
    ) {
        if (!file.exists()) {
            Log.e(TAG, "File does not exist for media scanning: ${file.absolutePath}")
            onComplete?.invoke(null)
            return
        }

        // Trigger MediaScannerConnection for immediate gallery indexing
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(mimeType)
        ) { path, uri ->
            Log.d(TAG, "Media indexed into MediaStore: path=$path, uri=$uri")
            onComplete?.invoke(uri)
        }
    }
}
