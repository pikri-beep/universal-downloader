# Design Document: Universal Media Downloader — Android Native (Fase 2)

**Tanggal:** 2026-11-08  
**Status:** Siap Disetujui (Reviewed & Finalized)  
**Proyek:** Universal Media Downloader  
**Fase:** 2 (Aplikasi Android Mandiri / On-Device)  
**Terkait:** [PRD.md](file:///d:/Project/universal-downloader/PRD.md), [Fase 1 Sprint Roadmap](file:///d:/Project/universal-downloader/docs/phase-1-sprint-roadmap.md)

---

## 1. Intent & The Why

### Mengapa Proyek Ini Dibangun:
Pengguna membutuhkan solusi pengunduh konten media (video, audio, foto) dari platform sosial media utama (TikTok, Instagram, Twitter/X, YouTube, Facebook, dll.) yang berjalan **100% mandiri di dalam smartphone Android**. Versi desktop (Fase 1) telah terbukti sukses, namun pengguna tidak memiliki server rumahan (*home server*) atau VPS cloud berbayar untuk dijadikan backend perantara. 

Oleh karena itu, aplikasi mobile Fase 2 dirancang sebagai aplikasi APK Android native tanpa ketergantungan pada backend eksternal apa pun—seluruh proses inspeksi URL, eksekusi ekstraktor, konversi format audio/video via FFmpeg, hingga penyimpanan langsung ke galeri HP dijalankan oleh prosesor dan memori perangkat secara lokal.

### Siapa Penggunanya:
Pengguna smartphone Android pribadi yang menginginkan alat pengunduh media yang:
1. Bersih, cepat, tanpa iklan yang mengganggu (*ad-free*).
2. Menjaga privasi penuh (tanpa pelacakan cloud atau pengumpulan data).
3. Mudah digunakan saat berselancar di aplikasi sosial media melalui menu *Share*.

---

## 2. Goals, Non-Goals, & Anti-Goals

### Goals:
- **100% On-Device Execution:** Menjalankan runtime `yt-dlp` dan binary FFmpeg ARM64 langsung di dalam aplikasi Android via library native teruji `youtubedl-android`.
- **Unified Experience (Dual Entry Point):**
  1. *Quick Share Sheet (Interaktif):* Menangkap tautan secara otomatis saat pengguna menekan tombol "Share" (Bagikan) di TikTok/IG/X/YouTube, memunculkan bottom sheet melayang taktil untuk melihat thumbnail, memilih format (Video/Audio MP3), lalu memulai unduhan di background.
  2. *Main App Studio:* Antarmuka layar penuh untuk input link manual, browsing riwayat unduhan, pemutar media bawaan, konfigurasi folder, dan pembaruan sistem.
- **Serial FIFO Download Queue:** Antrean unduhan berurutan (1 tugas aktif pada satu waktu) berbasis database lokal (Room/State) agar performa HP tetap dingin, hemat baterai, dan terhindar dari *Out-Of-Memory* (OOM) akibat eksekusi multi-FFmpeg simultan.
- **Direct Gallery Integration (Android MediaStore):** Setiap file yang berhasil diunduh otomatis didaftarkan ke `MediaStore` Android sehingga seketika muncul di album Galeri HP / Google Photos tanpa perlu refresh sistem.
- **Persistent Background Downloads:** Pengunduhan berjalan di `ForegroundService` dengan notifikasi progres persentase yang stabil dan tidak dimatikan oleh sistem penghemat baterai Android.
- **In-App Engine Updater:** Memiliki fitur pembaruan modul `yt-dlp` mandiri dari rilis resmi GitHub melalui API `YoutubeDL.getInstance().updateYoutubeDL()` agar aplikasi tetap berfungsi saat sosmed memperbarui API mereka, tanpa perlu mengompilasi APK baru.
- **In-App APK Updater:** Pemeriksa pembaruan rilis APK baru langsung dari GitHub Releases.
- **Funimated & Anti-Slop UI:** Antarmuka visual fisik taktil (*mechanical spring physics*, tombol bervolume 3D, feedback getaran haptic, dan palet warna kontras tinggi ceria) yang mereplikasi jiwa desain dari versi Desktop.

### Non-Goals:
- **Distribusi Google Play Store:** Google Play Store melarang aplikasi pengunduh YouTube dan pembaruan kode dinamis di luar Play Store; aplikasi didistribusikan secara eksklusif via *Direct APK Sideload* (GitHub Releases / F-Droid).
- **Dukungan iOS:** Apple iOS melarang eksekusi biner dinamis dan kompilasi runtime Python mandiri di sandbox App Store; Fase 2 secara khusus dan sengaja hanya menargetkan ekosistem Android.
- **Otentikasi / Cookie WebView Login:** Menghindari penanaman modul browser login yang rumit; fokus pada ekstraksi konten publik dengan fallback engine (seperti TikWM).
- **Sinkronisasi Akun / Cloud Storage:** Tidak ada sistem login, database cloud, atau upload ke server pihak ketiga.

### Anti-Goals:
- **Tampilan AI-Slop / WebView Lambat:** Menghindari pembungkusan WebView browser yang berat dan lambat. Aplikasi harus terasa gesit (native 60–120 fps).
- **Iklan dan Monetisasi Invasif:** Tanpa banner iklan, pop-up interstitial, atau pelacak analitik latar belakang.

---

## 3. Constraints & Technical Boundaries

- **Target Platform:** Android 7.0 (API Level 24) ke atas hingga Android 14/15 (API Level 34/35).
- **Arsitektur CPU:** `arm64-v8a` (arsitektur utama 99% HP Android modern) dan `armeabi-v7a`, dengan dukungan `x86_64` untuk Android Studio Emulator.
- **Storage Model:** Wajib mematuhi *Scoped Storage* Android modern (penyimpanan publik via MediaStore tanpa meminta izin penyimpanan luas yang berbahaya).
- **Ukuran Distribusi:** Ukuran file APK berkisar antara ~65–85 MB karena membawa binary runtime Python C-API, `yt-dlp`, dan static build FFmpeg.

---

## 4. Approaches Considered & Tradeoffs

| Pendekatan | Cara Kerja | Kelebihan | Kekurangan | Status |
|---|---|---|---|---|
| **1. Kotlin + Jetpack Compose + `youtubedl-android`** | Native Android murni dengan Compose UI dan library wrapper C-JNI teruji untuk yt-dlp & FFmpeg. | Integrasi OS paling mulus (Share Sheet, ForegroundService, MediaStore), performa UI 120fps, hemat RAM (~50MB), ada fitur built-in `updateYoutubeDL()`. | Ukuran APK ~70MB karena bundle native library. | **DIPILIH** (Rekomendasi Utama) |
| **2. Chaquopy Python Runtime Bridge** | Menanamkan interpreter Python standar via Gradle Chaquopy, memanggil `downloader.py` langsung. | Kode Python desktop dapat dipakai ulang hampir tanpa modifikasi. | Build APK lambat, lisensi Chaquopy berbayar untuk komersial, konfigurasi binary FFmpeg rumit. | Ditolak |
| **3. React Native / Flutter Hybrid** | Memakai framework cross-platform dengan bridge native kustom. | Kode React desktop bisa diadaptasi parsial. | Overhead runtime ganda (JS + Python), memory footprint besar, jembatan streaming progres sering tersendat. | Ditolak |

---

## 5. Technical Architecture & Component Design

### 5.1. Alur Quick Share Interaktif (`ShareActivity`)

```text
[ Pengguna di Sosmed (TikTok / IG / YT) ]
              │ (Tekan "Bagikan" / Share)
              ▼
    [ ShareActivity (Transparent Theme) ] ──── Intent: ACTION_SEND
              │
              ▼
    [ QuickDownloadBottomSheet (Jetpack Compose) ]
              │
              ├─► Ekstraksi Ringan Metadata (Judul, Cover, Pilihan Format)
              ├─► Menampilkan Opsi: [ 1080p Video ] [ 720p ] [ Audio MP3 ] [ Foto ]
              │
              ▼ (Pengguna Tekan "Unduh")
    [ Masukkan Tugas ke DownloadQueueManager ]
              │
              ├─► Tutup BottomSheet & ShareActivity.finish()
              │
              ▼
    [ DownloadForegroundService (FIFO Queue) ]
              │
              ├── Eksekusi tugas terdepan via youtubedl-android
              ├── Update Ongoing Notification: "Mengunduh 1/3: [Judul] (45%)"
              ├── Selesai -> Registrasi Android MediaStore (Galeri Langsung)
              └── Lanjut ke antrean berikutnya
```

### 5.2. Layar Aplikasi Utama (MainActivity)
- **Top Bar:** Logo Funimated, status kesiapan engine yt-dlp, tombol Pengaturan & Cek Pembaruan Engine.
- **Input Section:** Kolom input tautan taktil besar dengan tombol *Paste from Clipboard* otomatis mendeteksi URL di papan klip.
- **Preview & Selector Card:** Animasi munculan (*spring transition*) saat URL diinspeksi:
  - Thumbnail cover rounded dengan sudut membulat tebal.
  - Tab seleksi fisik: **Video (MP4)**, **Audio (MP3)**, **Foto/Cover (JPG)**.
  - Grid resolusi dengan badge perkiraan ukuran file.
- **Download History & Queue List:**
  - Tab aktif menampilkan unduhan yang sedang berjalan / antrean.
  - Tab riwayat menampilkan media yang sudah tersimpan dengan opsi 1-klik: *Putar Video (ExoPlayer bawaan)*, *Lihat di Galeri*, *Bagikan*, atau *Hapus*.

### 5.3. Layanan Latar Belakang & Notifikasi (`DownloadForegroundService`)
- Menampilkan *Ongoing Notification* dengan channel ID khusus `downloader_channel`.
- Progress bar notifikasi menunjukkan: Persentase (0–100%), Kecepatan (MB/s), dan Estimasi Waktu (ETA).
- Saat selesai, notifikasi berubah menjadi notifikasi sukses dengan tombol aksi: **"Buka Video"** dan **"Bagikan"**.

### 5.4. Registrasi MediaStore (Auto Galeri)
- **Video:** Disimpan ke direktori standar publik `Environment.DIRECTORY_MOVIES / UniversalDownloader` menggunakan `MediaStore.Video.Media`.
- **Audio:** Disimpan ke `Environment.DIRECTORY_MUSIC / UniversalDownloader` menggunakan `MediaStore.Audio.Media`.
- **Foto/Cover:** Disimpan ke `Environment.DIRECTORY_PICTURES / UniversalDownloader` menggunakan `MediaStore.Images.Media`.

---

## 6. What's Left to the Builder (Keputusan Teknis Pelaksana)

Bagian-bagian berikut diserahkan sepenuhnya kepada pelaksana teknis (*builder*) saat penyusunan implementasi:
1. Struktur package Android (`com.universaldownloader.android`).
2. Versi spesifik dependensi Gradle (Kotlin 1.9+, Compose Compiler 1.5+, Android Gradle Plugin 8.x).
3. Pilihan pemutar video internal di riwayat unduhan (menggunakan Jetpack Media3 ExoPlayer).
4. Penataan konfigurasi repository Maven/JitPack untuk dependensi `com.github.yausername.youtubedl-android`.
