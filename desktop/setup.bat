@echo off
setlocal enabledelayedexpansion
title Setup Universal Media Downloader

echo =========================================================
echo   UNIVERSAL MEDIA DOWNLOADER - SETUP WIZARD (WINDOWS)
echo =========================================================
echo.

:: 1. Check Python
echo [1/5] Memeriksa Python...
where python >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Python tidak ditemukan di sistem. Harap install Python 3.11+ dan centang 'Add to PATH'.
    pause
    exit /b 1
)
python --version

:: 2. Check FFmpeg
echo.
echo [2/5] Memeriksa FFmpeg...
where ffmpeg >nul 2>nul
if %errorlevel% neq 0 (
    echo [WARNING] FFmpeg tidak ditemukan di PATH. Penggabungan stream 1080p/MP3 mungkin terbatas.
) else (
    echo FFmpeg terdeteksi OK.
)

:: 3. Setup Python Virtual Environment
echo.
echo [3/5] Menyiapkan Python Virtual Environment (backend)...
cd /d "%~dp0backend"
if not exist "venv" (
    echo Membuat virtual environment di %~dp0backend\venv ...
    python -m venv venv
)
echo Menginstal dependensi Python...
call venv\Scripts\pip install -r requirements.txt --quiet
if %errorlevel% neq 0 (
    echo [ERROR] Gagal menginstal dependensi Python.
    pause
    exit /b 1
)
echo Dependensi backend berhasil diinstal.

:: 4. Build Frontend Static Assets
echo.
echo [4/5] Memeriksa dan mem-build Frontend UI...
cd /d "%~dp0frontend"
where npm >nul 2>nul
if %errorlevel% neq 0 (
    echo [WARNING] Node.js/npm tidak ditemukan. Melewati build frontend.
) else (
    echo Menginstal dependensi frontend...
    call npm install --silent
    echo Mem-build aset statis ke folder dist...
    call npm run build
)

:: 5. Create Desktop Shortcut
echo.
echo [5/5] Membuat shortcut di Desktop Windows...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ws = New-Object -ComObject WScript.Shell; " ^
  "$s = $ws.CreateShortcut([System.IO.Path]::Combine([Environment]::GetFolderPath('Desktop'), 'Universal Media Downloader.lnk')); " ^
  "$s.TargetPath = [System.IO.Path]::Combine('%~dp0', 'run.bat'); " ^
  "$s.WorkingDirectory = '%~dp0'; " ^
  "$s.Description = 'Universal Media Downloader (FastAPI + yt-dlp)'; " ^
  "$s.Save()"

echo Shortcut 'Universal Media Downloader' berhasil dibuat di Desktop!
echo.
echo =========================================================
echo   INSTALASI SELESAI!
echo   Kamu bisa langsung menjalankan 'run.bat' atau klik
echo   shortcut di Desktop untuk mulai mengunduh.
echo =========================================================
echo.
pause
