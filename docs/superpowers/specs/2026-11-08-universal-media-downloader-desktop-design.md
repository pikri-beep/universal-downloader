# Design Document: Universal Media Downloader (Desktop Edition)

**Date:** 2026-11-08  
**Topic:** Universal Media Downloader Desktop MVP  
**Status:** In Review (Brainstorming Phase)  

---

## 1. Intent & The Why

Universal Media Downloader adalah aplikasi desktop Windows mandiri untuk mengunduh konten multimedia (video, audio MP3, thumbnail, subtitle) dari berbagai platform sosial media (YouTube, TikTok, Instagram, Twitter/X, Facebook, dll.) langsung ke penyimpanan lokal komputer pengguna.

### Why:
- Solusi web downloader pihak ketiga di internet saat ini dipenuhi iklan pop-up berbahaya, malware, limitasi resolusi berbayar, dan rawan diblokir oleh platform video karena memakai IP cloud/datacenter bersama.
- Menjalankan downloader langsung di komputer lokal pengguna (IP residensial) memanfaatkan kekuatan `yt-dlp` tanpa risiko blokir IP cloud dan tanpa batas kuota/kecepatan.
- Mengedepankan antarmuka taktil *Funimated* (Skeuomorphic-Lite dengan Cartoon Physics) dan *Anti-Slop* agar aplikasi terasa menyenangkan, responsif, dan bernilai seperti alat perkakas fisik yang solid.

---

## 2. Goals, Non-Goals, and Anti-Goals

### Goals:
- **Zero Configuration Run:** Pengguna cukup menjalankan `run.bat` untuk menjalankan backend FastAPI dan otomatis membuka antarmuka di tab browser default (`http://127.0.0.1:8000`).
- **One-Click Inspector:** Pengguna memasukkan atau paste URL media, sistem mengambil metadata (judul, thumbnail, author, durasi, platform) dalam hitungan detik.
- **Multi-Slide Carousel Support:** Untuk postingan multi-slide (seperti Instagram Carousel), sistem mendeteksi daftar item/slide dan menyajikan selektor visual per-slide di UI sehingga pengguna dapat memilih slide tertentu atau mengunduh semuanya.
- **Flexible Media Output:**
  - Video MP4 (1080p, 720p, 480p, 360p) dengan penggabungan video + audio otomatis via FFmpeg.
  - Audio MP3 berkualitas tinggi (320kbps, 192kbps, 128kbps) dengan metadata ID3 otomatis.
  - Media ekstra: download thumbnail resolusi tinggi dan subtitle (SRT/VTT).
- **Real-time SSE Progress:** Menampilkan kemajuan download (persentase, kecepatan, status proses) secara halus dan responsif via Server-Sent Events (SSE).
- **Direct Local Access:** Tombol "Buka Folder" langsung membuka lokasi file unduhan di Windows Explorer (`Downloads/UniversalDownloader`).
- **Self-Updating Engine:** Tombol pembaruan 1-klik untuk update `yt-dlp` ke rilis terbaru.

### Non-Goals:
- **Mobile Android:** Fase 1 berfokus penuh pada Windows Desktop; Android Native (Kotlin + Compose) adalah Fase 2.
- **Cloud Account / Multi-user Auth:** Tidak memerlukan akun, login cloud, atau database relasional eksternal. Aplikasi bersifat lokal single-user.
- **Torrents / P2P:** Hanya mendukung media web yang didukung oleh extractor `yt-dlp`.

### Anti-Goals:
- **No Generic AI Slop:** Menghindari tampilan template AI klise (gradien ungu gelap generik, teks copywriting bombastis tanpa makna).
- **No Heavy Background Runtimes:** Tidak membiarkan Node.js dev server berjalan di background saat pemakaian harian.
- **No Intrusive Adware:** Bersih 100% tanpa analitik pelacak atau monetisasi iklan.

---

## 3. Constraints

- **OS Target:** Windows 10 / 11.
- **Pre-installed System Tools:** Python 3.11.x, FFmpeg, dan Node.js (untuk initial frontend build).
- **Port Jaringan Lokal:** Default port `8000` (dengan single-instance check).
- **Browser Execution:** Dibuka otomatis pada peramban default pengguna (Edge, Chrome, Firefox, dll.).

---

## 4. Approaches Considered

### Pendekatan 1: Unified Single-Process (FastAPI Static Host + REST/SSE) — **DIPILIH**
- **Deskripsi:** Frontend React + Tailwind + Framer Motion di-build sekali menjadi aset statis (`dist`), kemudian disajikan langsung oleh server FastAPI melalui static mount di root `/`, sementara rute API dan SSE ditangani di `/api/*`.
- **Kelebihan:** 
  - Konsumsi memori sangat hemat (~30–40MB RAM).
  - Satu klik `run.bat` langsung menyalakan server Python dan membuka peramban tanpa perlu Node.js berjalan.
  - Stabil dan tidak bergantung pada dev-server port ganda.
- **Trade-off:** Memerlukan step build frontend saat instalasi awal atau pembaruan kode UI.

### Pendekatan 2: Dual-Process (FastAPI + Vite Dev Server)
- **Deskripsi:** `run.bat` menjalankan FastAPI (port 8000) dan Vite dev server (port 5173) secara paralel.
- **Kelebihan:** Mendukung hot-reload instan selama proses penulisan kode antarmuka.
- **Trade-off:** Memboroskan RAM (menjalankan runtime Node.js + Python sekaligus) dan memperlambat startup pemakaian harian. Ditolak sesuai arahan efisiensi di PRD.

### Pendekatan 3: Dedicated Native Desktop Window (PyWebView / WebView2)
- **Deskripsi:** Membungkus web frontend ke dalam jendela native Windows terpisah tanpa navigasi browser.
- **Trade-off:** Dieliminasi karena pengguna secara eksplisit memilih pengalaman tab browser default yang lebih ringan dan langsung.

---

## 5. Architectural & Technical Decisions

### 5.1 Backend (Python FastAPI)
- **Lokasi:** `desktop/backend/`
- **File Inti:**
  - `main.py`: Inisialisasi FastAPI, static file mounting (`../frontend/dist`), CORS, endpoint metadata `/api/inspect` (mengembalikan single item atau list `slides` untuk carousel), download `/api/download` (menerima index slide spesifik atau `all`), SSE progress `/api/progress/{task_id}`, folder explorer `/api/open-folder`, update `/api/update-engine`, dan graceful shutdown `/api/shutdown`.
  - `downloader.py`: Wrapper `yt_dlp.YoutubeDL` dengan `progress_hook` yang mengirimkan event ke thread-safe queue untuk disalurkan ke SSE, serta ekstraksi entri playlist/carousel.
  - `config.py`: Penentuan lokasi folder download default (`%USERPROFILE%/Downloads/UniversalDownloader`) dan opsi konfigurasi umum.
  - `requirements.txt`: `fastapi`, `uvicorn[standard]`, `yt-dlp`.

### 5.2 Frontend (React + Vite + Tailwind + Framer Motion)
- **Lokasi:** `desktop/frontend/`
- **Gaya Visual (Funimated + Anti-Slop):**
  - Card putih bersih dengan border 2px beraksen fisik dan rounded besar (`rounded-3xl` / `rounded-2xl`).
  - Tombol taktil 3D dengan ketebalan fisik (`border-b-4 active:border-b-2 active:translate-y-[2px]`).
  - Header dengan tombol "Matikan Aplikasi" (memanggil `/api/shutdown` lalu menutup tab) dan indikator status server.
  - Preview & Carousel Selector: Jika media memiliki banyak slide, ditampilkan thumbnail strip/grid taktil yang dapat dipilih satuan atau "Pilih Semua".
  - Animasi elastis (*spring physics*, *squash & stretch*) saat card preview media muncul dan progress bar berjalan.
  - Palet pastel berkarakter: Coral/Peach untuk aksi unduh, Mint untuk audio, Sky Blue untuk badge info.

### 5.3 Runner & Setup Scripts
- `setup.bat`:
  - Membuat virtual environment Python (`desktop/backend/venv`).
  - Menginstal dependensi Python dari `requirements.txt`.
  - Menginstal dependensi frontend via `npm install` dan menjalankan `npm run build`.
  - Membuat shortcut desktop "Universal Downloader" yang mengarah ke `run.bat`.
- `run.bat`:
  - Melakukan single-instance check (jika port 8000 sudah berjalan, langsung buka browser tanpa menduplikasi proses Python).
  - Menjalankan backend FastAPI via Python virtual environment.
  - Membuka URL `http://127.0.0.1:8000` di browser default secara otomatis.
- `stop.bat`:
  - Utilitas pendukung untuk menghentikan proses backend FastAPI jika diperlukan dari file explorer.

---

## 6. What's Left to the Builder

- Implementasi detail sanitasi nama file unduhan agar aman di sistem file Windows (menghapus karakter ilegal seperti `\ / : * ? " < > |`).
- Pengaturan penanganan error jika URL tidak valid, video berstatus privat, atau pembatasan usia YouTube.
- Logika threading asinkron pada FastAPI agar download berukuran besar tidak memblokir event loop server utama.
