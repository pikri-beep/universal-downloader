# Product Requirements Document (PRD)
## Universal Media Downloader — Desktop Edition (Windows)

**Project Name:** Universal Media Downloader  
**Version:** 1.0.0 (Desktop MVP)  
**Author:** Pair Programming Team  
**Design Philosophy:** Funimated (Tactile Skeuomorphic-Lite & Cartoon Physics) + Anti-Slop Core  

---

### 1. Product Vision & Goals

Universal Media Downloader adalah aplikasi desktop Windows mandiri, tanpa iklan, dan tanpa batasan kuota untuk mengunduh konten multimedia (video, audio, thumbnail, subtitle) dari berbagai platform sosial media terpopuler langsung ke penyimpanan lokal.

#### Core Value Proposition:
- **Bebas Blokir IP:** Berjalan langsung di jaringan lokal/rumah pengguna (IP residensial), sehingga tidak terkena blokir bot YouTube / rate-limit yang dialami server cloud gratisan.
- **1-Time Setup & Pakai Selamanya:** Sekali klik instalasi, aplikasi siap dijalankan dari Desktop shortcut kapan pun tanpa perlu membuka terminal atau mengetik perintah.
- **Hemat Daya & Sumber Daya:** Tidak menjalankan Node.js dev server yang berat. Memakai arsitektur single-process Python (~40MB RAM, 0% CPU idle) untuk menyajikan UI statis dan memproses media.
- **Pengalaman Visual Berkarakter (Funimated + Anti-Slop):** Antarmuka fisik taktil (hardware feel) dengan tombol 3D kenyal (*squash & stretch*), palet pastel kontras tinggi, dan tanpa tampilan template AI generik kusam.

---

### 2. Supported Platforms & Formats

| Platform | Capabilities | Output Formats |
|---|---|---|
| **YouTube** | Video hingga 4K/1080p (video+audio auto-merge via FFmpeg), Audio Only, Subtitles, Thumbnail HD | MP4, WEBM, MP3 (320k/192k/128k), M4A, SRT, JPG |
| **TikTok** | Video tanpa watermark, Audio original | MP4 (Clean), MP3 |
| **Instagram** | Reels, Video Posts, Multi-slide Carousel, Stories publik | MP4, JPG |
| **Twitter / X** | Video, GIF animasi | MP4 |
| **Facebook** | Video publik HD / SD | MP4 |
| **Universal Direct** | Semua 1000+ platform yang didukung yt-dlp (SoundCloud, Vimeo, Reddit, dll.) | Auto-detect |

---

### 3. Feature Specifications

#### 3.1. Instant URL Inspector & Preview
- **Auto-Paste / Quick Input:** Textfield taktil besar dengan tombol *Paste from Clipboard* satu klik.
- **Metadata Card:** Saat URL dimasukkan dan di-inspect:
  - Menampilkan thumbnail cover dengan sudut membulat (`rounded-2xl`).
  - Judul media, pembuat konten (author/channel), durasi, dan ikon platform.
  - Tab seleksi cepat: **[ Video ]**, **[ Audio MP3 ]**, **[ Lainnya (Cover / Subtitle) ]**.

#### 3.2. Detailed Quality & Format Selector
- **Video Tab:**
  - Pilihan resolusi: 1080p, 720p, 480p, 360p (dengan badge ukuran estimasi file jika tersedia).
  - Format container: MP4 (kompatibel universal).
- **Audio Tab:**
  - Pilihan bitrate: 320 kbps (High Quality), 192 kbps (Standard), 128 kbps (Compact).
  - Ekstraksi otomatis via FFmpeg ke file MP3 dengan tag judul & cover album otomatis tersemat.
- **Extras Tab:**
  - Download gambar thumbnail resolusi maksimal.
  - Download subtitle (.srt / .vtt) jika video menyediakannya.

#### 3.3. Realtime Download Engine (SSE Streaming)
- Tampilan progress bar taktil yang bergerak mulus dengan animasi persentase (0% - 100%).
- Status pemrosesan transparan:
  - `Mengunduh stream video...`
  - `Menggabungkan audio & video (FFmpeg)...`
  - `Selesai! File disimpan di folder Downloads.`
- Tombol **"Buka Folder"** langsung membuka file yang baru saja didownload di Windows Explorer.

#### 3.4. Maintenance-Free & Self-Updating Engine
- Platform video sering memperbarui API anti-scraping mereka.
- Disediakan tombol **"Update Engine (1-Klik)"** di header aplikasi yang langsung mengeksekusi pembaruan `yt-dlp` ke rilis terbaru secara aman tanpa perlu install ulang.

---

### 4. Design System & UI Specifications (Funimated + Anti-Slop)

Mematuhi pedoman visual `funimated` dan `antislop`:

1. **Surface & Palette:**
   - Background: Clean Daylight Grey-White (`#F6F8FA`).
   - Cards & Modules: Pure White (`#FFFFFF`) dengan border `border-2 border-slate-100` dan soft drop-shadow `shadow-[0_8px_20px_-6px_rgba(15,23,42,0.06)]`.
   - Semantic Pastel Accents:
     - Primary Action (Download): Pastel Peach/Coral (`bg-[#FFE4EC]` border `#FF8DA1` teks `#9D174D`) dengan efek hover cerah.
     - Audio Action: Pastel Mint (`bg-[#DCFCE7]` border `#86EFAC` teks `#166534`).
     - Info/Platform Badge: Pastel Sky (`bg-[#E0F2FE]` border `#7DD3FC` teks `#0369A1`).
     - Teks selalu menggunakan rasio kontras tinggi di atas latar belakang pastel.
2. **Tactile Hardware Components:**
   - Tombol memiliki ketebalan fisik 3D: `border-2 border-b-4 border-slate-200 active:border-b-2 active:translate-y-[2px]`.
   - Sudut melengkung ekstra membulat: `rounded-3xl` (24px) untuk container utama, `rounded-2xl` untuk card modul.
3. **Cartoon Physics Motion:**
   - Animasi transisi menggunakan Framer Motion dengan efek pegas kenyal (*squash & stretch*, *spring damping 15, stiffness 250*).
   - Card preview muncul dengan efek melar elastis yang ekspresif.
4. **Anti-Slop Directives:**
   - Bebas dari gradien ungu klise AI.
   - Tanpa teks copywriting generik (*"Welcome to the next-gen AI downloader experience"*). Copywriting to the point, bersahabat, dan jelas.

---

### 5. Technical Architecture

```
                    +------------------------------------+
                    |   Desktop App Window (WebView2)    |
                    |   Frontend: React + Tailwind +      |
                    |   Framer Motion (Static Build)     |
                    +-----------------+------------------+
                                      | Local REST / SSE
                                      v
                    +------------------------------------+
                    |   FastAPI Single-Process Backend   |
                    |   - Serves Static Web UI           |
                    |   - Handles /api/inspect           |
                    |   - Handles /api/download (SSE)    |
                    |   - Handles /api/update-engine     |
                    +-----------------+------------------+
                                      |
                      +---------------+---------------+
                      |                               |
                      v                               v
             +-----------------+             +-----------------+
             |     yt-dlp      |             |     FFmpeg      |
             | Stream Extractor|             | Audio/Video Mux |
             +-----------------+             +-----------------+
                      |                               |
                      +---------------+---------------+
                                      |
                                      v
                         Local PC Downloads Folder
```

- **Runtime:** Python 3.11 (Native Windows)
- **Engine:** `yt-dlp` (Python API & CLI) + `ffmpeg` (sudah terinstal di sistem)
- **API Framework:** `FastAPI` + `Uvicorn`
- **Frontend Stack:** React 18, Vite, Tailwind CSS, Lucide React Icons, Framer Motion
- **Distribusi:** File `run.bat` (1-klik jalankan) dan `setup.bat` (1-klik instalasi dependensi & buat shortcut desktop)

---

### 6. Phase 2: Android Native Roadmap

Setelah versi Desktop stabil dan terverifikasi:
- Codebase Android Native di `mobile/` menggunakan **Kotlin + Jetpack Compose**.
- Memanfaatkan library `youtubedl-android` untuk embed engine `yt-dlp` dan `ffmpeg` di arsitektur ARM64.
- Desain antarmuka Android mereplikasi tema Funimated yang sama menggunakan Compose Canvas & spring animations.
