# ⚡ Universal Media Downloader

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Windows%20%7C%20Android-FACC15?style=for-the-badge&logo=windows&logoColor=black" alt="Platforms" />
  <img src="https://img.shields.io/badge/License-MIT-4ADE80?style=for-the-badge" alt="License" />
  <img src="https://img.shields.io/badge/Architecture-Dual--Platform%20Native-38BDF8?style=for-the-badge" alt="Architecture" />
  <img src="https://img.shields.io/badge/Anti--Slop-100%25%20Verified-F43F5E?style=for-the-badge" alt="Anti-Slop" />
</p>

**Universal Media Downloader** adalah aplikasi pengunduh media multi-platform (Windows & Android) yang dirancang dengan estetika **Funimated (Tactile Skeuomorphic-Lite)** dan filosofi **Anti-Slop**:
- 🚫 **Tanpa Iklan & Tanpa Pelacak**
- 🔒 **100% On-Device / Client-Side Privacy** (Semua pemrosesan media langsung dari perangkat pengguna, tanpa relay cloud)
- ⚡ **Super Cepat & Anti-Blokir** (Dukungan emulasi peramban TLS `curl_cffi` & endpoint inspeksi instan)
- 📸 **Dukungan Postingan Foto / Slide Lengkap** (Bukan hanya video!)

---

## 🚀 Fitur Utama & Matriks Platform

| Platform Media | Video | Audio (MP3 320k) | Foto / Carousel Slides | Subtitles |
| :--- | :---: | :---: | :---: | :---: |
| **YouTube** | 4K, 1440p, 1080p, 720p | ✅ | Thumbnail HD | ✅ Multi-bahasa |
| **TikTok** | HD Tanpa Watermark | ✅ | ✅ Download Semua Foto Sekaligus | - |
| **Instagram** | Reels & Feed MP4 | ✅ | ✅ Carousel Postingan Foto | - |
| **Twitter / X** | Video HD MP4 | - | ✅ Multi-Image Sets | - |

---

## 📦 Unduh Rilis Siap Pakai

Kunjungi halaman [**GitHub Releases**](../../releases) untuk mengunduh versi terbaru:

### 1. 🖥️ Windows Desktop
- **Paket:** `UniversalDownloader-Windows.zip`
- **Cara Pakai:** Ekstrak zip, lalu jalankan **`UniversalDownloader.exe`**.
- **Fitur Khusus:**
  - Membuka jendela aplikasi desktop native (Edge Chromium WebView2) tanpa membuka browser tab.
  - Sudah menyertakan binary **`ffmpeg.exe`** dan **`ffprobe.exe`** di dalam paket zip (langsung siap menggabungkan video 1080p/4K dan convert MP3).
  - Portabel, tanpa perlu menginstal Python atau Node.js.

### 2. 📱 Android Mobile
- **Paket:**
  - `UniversalDownloader-arm64.apk` *(Rekomendasi untuk 95%+ smartphone Android modern)*
  - `UniversalDownloader-universal.apk` *(Kompatibel untuk semua perangkat Android)*
- **Fitur Khusus:**
  - **Quick Share Sheet:** Cukup tekan tombol *"Share"* di TikTok / IG / YouTube, pilih Universal Downloader, lalu unduh dalam 1 klik!
  - **Auto-Galeri:** File video otomatis tersimpan di `Movies/UniversalDownloader` dan foto di `Pictures/UniversalDownloader` (langsung muncul di galeri HP).
  - **Background Service:** Unduhan tetap berjalan di latar belakang dengan bilah progres taktil dan notifikasi real-time.

---

## 🛠️ Panduan Developer (Local Build)

### 🖥️ Desktop (Windows)
**Prasyarat:** Python 3.10+, Node.js 18+.

1. **Jalankan Mode Pengembangan:**
   ```bash
   cd desktop/frontend
   npm install
   npm run dev
   ```
   Di terminal lain:
   ```bash
   cd desktop/backend
   python -m venv venv
   venv\Scripts\activate
   pip install -r requirements.txt
   python main.py
   ```

2. **Kompilasi Standalone `.exe` (1-Klik):**
   Cukup klik dua kali atau jalankan:
   ```cmd
   desktop\build_exe.bat
   ```
   Skrip akan otomatis:
   - Mengompilasi frontend React Vite (`npm run build`).
   - Memeriksa atau mengunduh binary FFmpeg static.
   - Menjalankan PyInstaller dan menghasilkan zip rilis di `desktop/dist/UniversalDownloader-Windows.zip`.

---

### 📱 Mobile (Android)
**Prasyarat:** Android Studio / Android SDK, JDK 17 LTS.

1. **Buka Proyek:**
   Buka direktori `mobile/` di Android Studio.
2. **Kompilasi APK Debug / Release:**
   ```bash
   cd mobile
   ./gradlew assembleDebug
   ```
   APK akan tersedia di `mobile/app/build/outputs/apk/debug/`.

---

## 🤖 CI/CD Automated Releases

Repositori ini dilengkapi dengan **GitHub Actions** (`.github/workflows/release.yml`):
- Setiap kali Git Tag dibuat (contoh: `git tag v1.0.0 && git push origin v1.0.0`), CI/CD secara otomatis:
  1. Mengompilasi paket Desktop Windows `.exe` + FFmpeg di runner `windows-latest`.
  2. Mengompilasi paket Android APK (`arm64` & `universal`) di runner `ubuntu-latest`.
  3. Mempublikasikan GitHub Release baru lengkap dengan semua file unduhan dan changelog otomatis.

---

## 📜 Lisensi
Proyek ini dilisensikan di bawah lisensi [MIT](LICENSE).
