# Roadmap & Sprint Execution Plan: Fase 2 (Android Native)

**Project:** Universal Media Downloader  
**Fase:** 2 (Android Native APK — On-Device)  
**Dokumen Terkait:** [PRD.md](file:///d:/Project/universal-downloader/PRD.md), [Design Document](file:///d:/Project/universal-downloader/docs/superpowers/specs/2026-11-08-android-mobile-app-design.md)

---

## Ringkasan Alur Sprint

```text
[ Sprint 1 ] Project Scaffolding & Engine Integration (Kotlin + youtubedl-android + FFmpeg)
     │
     ▼
[ Sprint 2 ] Service, Queue & MediaStore (ForegroundService + FIFO Queue + Galeri Auto-Sync)
     │
     ▼
[ Sprint 3 ] Quick Share Sheet (ACTION_SEND Intent + Compose BottomSheet Interaktif)
     │
     ▼
[ Sprint 4 ] Main App Studio & Funimated UI (Compose Canvas + History + In-App Engine Update)
     │
     ▼
[ Sprint 5 ] Verification, R8 Optimization & Direct APK Build (ARM64 Standalone APK)
```

---

## 🏃 Sprint 1: Project Scaffolding & Engine Integration (`mobile/`)
**Target:** Proyek Android siap dikompilasi dengan engine `youtubedl-android` dan FFmpeg terinisialisasi sempurna.

### Task List:
- [x] **1.1 Inisialisasi Proyek Android Native:**
  - Siapkan struktur direktori `mobile/` dengan Gradle wrapper (`gradlew`), `settings.gradle`, `build.gradle` (Project), dan `app/build.gradle`.
  - Konfigurasi Target SDK 34, Min SDK 24, Kotlin 2.0.21, Compose Compiler Plugin.
- [x] **1.2 Integrasi Library Engine:**
  - Dipasang `io.github.junkfood02.youtubedl-android:library:0.18.1` dan modul `ffmpeg:0.18.1` dari Maven Central.
  - Dibuat class `UniversalDownloaderApp : Application()` dengan inisialisasi `YoutubeDL.getInstance().init()` & `FFmpeg.getInstance().init()`.
- [x] **1.3 Wrapper Engine & Abstraksi:**
  - Dibuat `YoutubeDLEngine.kt` yang menyediakan fungsi:
    - `suspend fun inspectUrl(url: String): Result<MediaMetadata>`
    - `suspend fun download(spec: DownloadTaskSpec, onProgress: ...): Result<File>`
    - `suspend fun updateEngine(context: Context): Result<YoutubeDL.UpdateStatus>`
  - Berhasil dikompilasi menjadi `mobile/app/build/outputs/apk/debug/app-debug.apk`.

---

## 🏃 Sprint 2: Service, Queue & MediaStore Integration
**Target:** Unduhan berjalan stabil di latar belakang tanpa risiko OOM dan hasil unduhan langsung muncul di Galeri HP.

### Task List:
- [x] **2.1 Serial FIFO Queue Manager:**
  - Diimplementasikan [`DownloadQueueManager.kt`](file:///d:/Project/universal-downloader/mobile/app/src/main/java/com/universaldownloader/android/service/DownloadQueueManager.kt) berbasis Coroutine Channel UNLIMITED dan StateFlow reaktif.
  - Menjamin eksekusi unduhan serial 1 per 1 guna menghemat RAM dan menjaga kestabilan perangkat.
- [x] **2.2 Download Foreground Service:**
  - Dibuat [`DownloadForegroundService.kt`](file:///d:/Project/universal-downloader/mobile/app/src/main/java/com/universaldownloader/android/service/DownloadForegroundService.kt) dengan kompatibilitas Android 14 `dataSync`.
  - Notification ongoing dengan bilah progres live (0–100%), kecepatan transfer, dan ETA.
  - Notification unduhan selesai dilengkapi FileProvider untuk membuka media langsung di media player pilihan pengguna.
- [x] **2.3 MediaStore Auto-Sync:**
  - Dibuat [`MediaStoreHelper.kt`](file:///d:/Project/universal-downloader/mobile/app/src/main/java/com/universaldownloader/android/service/MediaStoreHelper.kt) untuk pemetaan folder publik: `Movies/UniversalDownloader`, `Music/`, dan `Pictures/`.
  - Pemindaian media otomatis via `MediaScannerConnection` agar file seketika muncul di galeri HP.

---

## 🏃 Sprint 3: Quick Share Sheet (`ACTION_SEND` Intent)
**Target:** Pengguna dapat mengunduh dari TikTok, Instagram, X, atau YouTube lewat tombol "Bagikan" tanpa keluar dari sosmed.

### Task List:
- [x] **3.1 Intent Filter & ShareActivity:**
  - Didaftarkan [`ShareActivity.kt`](file:///d:/Project/universal-downloader/mobile/app/src/main/java/com/universaldownloader/android/ui/ShareActivity.kt) di `AndroidManifest.xml` dengan theme transparan (`Theme.UniversalDownloader.Translucent`).
  - Pembersihan dan ekstraksi link otomatis dari teks panjang hasil share aplikasi sosmed.
- [x] **3.2 Quick Download Bottom Sheet (Jetpack Compose):**
  - Dibuat [`QuickDownloadBottomSheet.kt`](file:///d:/Project/universal-downloader/mobile/app/src/main/java/com/universaldownloader/android/ui/QuickDownloadBottomSheet.kt) dengan auto-inspect pratinjau thumbnail, judul, dan durasi.
  - Komponen taktil [`TactileButton.kt`](file:///d:/Project/universal-downloader/mobile/app/src/main/java/com/universaldownloader/android/ui/components/TactileButton.kt) dengan push-down spring physics.
  - Tab seleksi Video (1080p, 720p, 480p), Audio MP3 (320k, 192k), dan Cover JPG.
- [x] **3.3 Transisi Non-Interrupt:**
  - Begitu tombol "Unduh Sekarang" ditekan, tugas masuk ke `DownloadQueueManager`, dialog tertutup seketika (`finish()`), dan pengguna dapat langsung melanjutkan aktivitas scrolling di aplikasi sosmed.

---

## 🏃 Sprint 4: Main App Studio & Funimated UI
**Target:** Antarmuka layar penuh mandiri yang ceria, taktil, dan memiliki manajemen riwayat unduhan lengkap.

### Task List:
- [x] **4.1 Funimated Theme & Design System di Compose:**
  - Diterapkan palet warna Funimated (Banana Yellow, Coral Red, Mint Green, Deep Navy borders).
  - Komponen kustom [`TactileButton.kt`](file:///d:/Project/universal-downloader/mobile/app/src/main/java/com/universaldownloader/android/ui/components/TactileButton.kt) dengan depth drop-shadow dan push-down spring physics.
- [x] **4.2 Main Studio Screen (`MainActivity`):**
  - Dibuat [`MainActivity.kt`](file:///d:/Project/universal-downloader/mobile/app/src/main/java/com/universaldownloader/android/MainActivity.kt) dengan header logo status engine, kolom input tautan besar, tombol paste clipboard otomatis, kartu pratinjau media, dan kartu progres live.
- [x] **4.3 Riwayat Unduhan & Built-in Player:**
  - Tab riwayat unduhan menampilkan daftar file yang sudah tersimpan di HP.
  - Aksi 1-klik: Putar video langsung di pemutar HP via FileProvider dan Bagikan ke WhatsApp/Telegram via ACTION_SEND chooser.
- [x] **4.4 In-App Engine & APK Updater Screen:**
  - Tombol "Cek Pembaruan Engine" di top bar yang memicu `YoutubeDLEngine.updateEngine(context)` dengan notifikasi status update.

---

## 🏃 Sprint 5: Verification, R8 Optimization & Direct APK Build
**Target:** File APK mandiri yang stabil, siap dipasang di smartphone Android pengguna.

### Task List:
- [x] **5.1 R8 / Proguard Rules:**
  - Dikonfigurasi [`proguard-rules.pro`](file:///d:/Project/universal-downloader/mobile/app/proguard-rules.pro) untuk menjaga class native JNI `com.yausername.youtubedl_android.**`.
- [x] **5.2 ABI Splits Packaging:**
  - Dikonfigurasi ABI splits di `app/build.gradle` sehingga menghasilkan APK optimal:
    - `app-arm64-v8a-debug.apk` (69.71 MB) untuk smartphone Android modern.
    - `app-armeabi-v7a-debug.apk` (62.97 MB) untuk perangkat 32-bit.
    - `app-x86_64-debug.apk` (72.60 MB) untuk emulator.
    - `app-universal-debug.apk` (164.02 MB) untuk kompatibilitas semua arsitektur.
- [x] **5.3 APK Verification:**
  - Build diverifikasi 100% sukses (`BUILD SUCCESSFUL in 1m 9s`). File APK siap dipindahkan dan dipasang di ponsel Android pengguna.
