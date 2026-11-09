# Roadmap & Sprint Execution Plan: Fase 1 (Desktop Windows)

**Project:** Universal Media Downloader  
**Fase:** 1 (Desktop Windows MVP)  
**Dokumen Terkait:** [PRD.md](file:///d:/Project/universal-downloader/PRD.md), [Design Spec](file:///d:/Project/universal-downloader/docs/superpowers/specs/2026-11-08-universal-media-downloader-desktop-design.md)

---

## Ringkasan Alur Sprint

```text
[ Sprint 1 ] Backend Core & Engine (FastAPI + yt-dlp + SSE)
     │
     ▼
[ Sprint 2 ] Frontend Funimated UI (React + Tailwind + Carousel Selector)
     │
     ▼
[ Sprint 3 ] Unified Integration & Windows Automation (Static Mount + Batch Scripts)
     │
     ▼
[ Sprint 4 ] Verification, Polish & Error Hardening (Real Platform Testing)
```

---

## 🏃 Sprint 1: Backend Core & Engine (`desktop/backend/`)
**Target:** Mesin ekstraksi metadata dan download berkecepatan tinggi dengan pelaporan real-time via Server-Sent Events (SSE).

### Task List:
- [x] **1.1 Dependensi & Konfigurasi:**
  - Siapkan `requirements.txt` (`fastapi`, `uvicorn[standard]`, `yt-dlp`, `requests`).
  - Buat `config.py` untuk mengelola direktori download default (`%USERPROFILE%\Downloads\UniversalDownloader`), port `8000`, dan utilitas sanitasi nama file Windows.
- [x] **1.2 Metadata Extractor (`downloader.py`):**
  - Implementasi fungsi `inspect_url()` untuk mengambil metadata media (judul, thumbnail, author/channel, durasi, platform).
  - Tambahkan deteksi konten multi-slide (Instagram Carousel / multi-item) agar menghasilkan daftar `slides` (thumbnail, preview, tipe image/video).
  - Ekstraksi format video (resolusi 1080p, 720p, 480p, 360p) dan audio (320k, 192k, 128k).
- [x] **1.3 Download Engine & Progress Hook (`downloader.py`):**
  - Buat runner download non-blocking dengan thread pool / async queue.
  - Implementasi `progress_hook` yt-dlp yang memetakan persentase (0–100%), kecepatan transfer (MB/s), ETA (sisa waktu), dan status (downloading, merging FFmpeg, complete).
  - Dukungan download untuk video MP4 (auto-merge video + audio via FFmpeg), audio MP3 (ekstraksi + ID3 tagging), cover image, dan subtitle.
  - Dukungan pengunduhan per-slide atau "Unduh Semua" pada carousel.
- [x] **1.4 API Endpoints (`main.py`):**
  - `POST /api/inspect`: Inspeksi URL dan mengembalikan info metadata/slides.
  - `POST /api/download`: Memicu tugas unduhan dan menghasilkan `task_id`.
  - `GET /api/progress/{task_id}`: Stream SSE event kemajuan unduhan.
  - `POST /api/open-folder`: Membuka Windows Explorer pada folder unduhan atau menyorot file spesifik via `explorer.exe /select,path`.
  - `POST /api/update-engine`: Update `yt-dlp` ke rilis terbaru.
  - `POST /api/shutdown`: Graceful shutdown server FastAPI.

---

## 🎨 Sprint 2: Frontend Funimated UI (`desktop/frontend/`)
**Target:** Antarmuka fisik taktil (Skeuomorphic-Lite) dengan Cartoon Physics, animasi kenyal Framer Motion, dan selektor carousel interaktif.

### Task List:
- [x] **2.1 Inisialisasi Proyek & Design Tokens:**
  - Setup Vite + React di `desktop/frontend`.
  - Instal dependensi visual: `tailwindcss`, `lucide-react`, `framer-motion`, `clsx`, `tailwind-merge`.
  - Konfigurasi `tailwind.config.js` dan `src/index.css` dengan token Funimated:
    - Efek tombol 3D taktil (`border-2 border-b-4 active:border-b-2 active:translate-y-[2px]`).
    - Palet pastel kontras tinggi: Coral/Peach (Primary), Mint (Audio), Sky Blue (Info).
    - Spring physics animation configs (`damping: 15, stiffness: 250`).
- [x] **2.2 Komponen Header (`Header.jsx`):**
  - Brand header berkarakter Funimated.
  - Indikator status engine & tombol 1-klik "Update Engine".
  - Tombol cepat "Buka Folder Unduhan" dan tombol "Matikan Aplikasi".
- [x] **2.3 Komponen Input URL (`UrlInputCard.jsx`):**
  - Textfield taktil besar dengan auto-focus.
  - Tombol "Tempel dari Clipboard" (satu klik membaca clipboard pengguna).
  - Tombol aksi "Periksa URL" dengan animasi squash & stretch saat loading inspeksi.
- [x] **2.4 Komponen Media Preview & Carousel (`MediaPreviewCard.jsx`):**
  - Tampilan cover thumbnail berbingkai halus (`rounded-2xl`), judul, uploader, dan badge platform.
  - **Multi-Slide Carousel Selector:** Grid thumbnail scrollable untuk postingan slide (Instagram carousel), dilengkapi checkbox interaktif per slide dan tombol "Pilih Semua".
  - Tab selector format: **[ Video MP4 ]**, **[ Audio MP3 ]**, **[ Ekstra (Cover/Subtitle) ]**.
  - Pilihan kualitas (1080p, 720p, 480p, dll.) dalam bentuk chip tombol taktil.
  - Tombol besar kenyal "Mulai Mengunduh".
- [x] **2.5 Komponen Realtime Progress (`ProgressCard.jsx`):**
  - Hook SSE untuk mendengarkan progress streaming `/api/progress/{task_id}`.
  - Progress bar taktil bergelombang animasi persentase (0–100%).
  - Tampilan kecepatan download, sisa waktu, dan status pemrosesan FFmpeg.
  - Tombol aksi sukses: "Buka File di Folder".

---

## ⚙️ Sprint 3: Integrasi Sistem & Otomasi Windows (`desktop/`)
**Target:** Menghubungkan frontend statis ke dalam backend FastAPI tunggal dan menyediakan launcher 1-klik yang mudah bagi pengguna.

### Task List:
- [x] **3.1 Static Asset Serving:**
  - Build frontend React ke `desktop/frontend/dist`.
  - Mount direktori statis pada root `/` di FastAPI dengan handler SPA fallback ke `index.html`.
- [x] **3.2 Setup Script (`desktop/setup.bat`):**
  - Validasi ketersediaan Python dan Node.js di sistem.
  - Otomatis membuat virtual environment Python `desktop/backend/venv`.
  - Instal seluruh dependensi Python dan frontend.
  - Compile frontend ke `dist/`.
  - Generate shortcut Windows Desktop `.lnk` yang mengarah ke `run.bat`.
- [x] **3.3 Launcher Script (`desktop/run.bat`):**
  - Cek port 8000: jika server sudah hidup, langsung buka tab browser tanpa menduplikasi proses.
  - Jika belum hidup, jalankan FastAPI di background via virtual environment.
  - Otomatis membuka `http://127.0.0.1:8000` di browser default pengguna.
- [x] **3.4 Graceful Stop Script (`desktop/stop.bat`):**
  - Script utilitas untuk menghentikan proses backend di port 8000 dengan aman jika dibutuhkan.

---

## 🧪 Sprint 4: Verifikasi, Polish & Uji Coba Lapangan
**Target:** Memastikan keandalan aplikasi pada berbagai platform nyata dan menangani berbagai kondisi error dengan ramah pengguna.

### Task List:
- [x] **4.1 Pengujian Platform Nyata:**
  - Uji download video & audio YouTube (termasuk verifikasi hasil muxing FFmpeg).
  - Uji download video TikTok tanpa watermark.
  - Uji download Instagram Carousel (memilih slide individual dan batch download).
  - Uji download Twitter/X dan platform universal lainnya.
- [x] **4.2 Hardening & Penanganan Error:**
  - URL tidak valid atau format tidak didukung (menampilkan toast/modal taktil yang bersahabat).
  - Video privat atau pembatasan usia (age-restricted).
  - Sanitasi karakter ilegal Windows pada judul file (`\ / : * ? " < > |`).
- [x] **4.3 Final Checklist & Review:**
  - Verifikasi efisiensi konsumsi memori (~30-40MB RAM).
  - Pastikan tidak ada sisa proses zombie saat aplikasi dimatikan.
