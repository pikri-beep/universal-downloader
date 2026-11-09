@echo off
setlocal enabledelayedexpansion
title Universal Media Downloader - Standalone Executable Builder

echo =========================================================
echo   Universal Media Downloader - Windows .exe Builder
echo =========================================================
echo.

:: 1. Check Python
where python >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Python tidak ditemukan di PATH. Harap pasang Python 3.10+.
    pause
    exit /b 1
)

:: 2. Check Node & npm
where npm >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Node.js / npm tidak ditemukan di PATH. Harap pasang Node.js 18+.
    pause
    exit /b 1
)

:: 3. Build Frontend React
echo [1/4] Mengompilasi frontend React (Vite)...
cd "%~dp0frontend"
call npm install
call npm run build
if %errorlevel% neq 0 (
    echo [ERROR] Kompilasi frontend gagal!
    cd "%~dp0"
    pause
    exit /b 1
)
cd "%~dp0"

:: 4. Setup Python Virtual Environment & Install Build Dependencies
echo [2/4] Menyiapkan dependensi Python & PyInstaller...
if not exist "%~dp0backend\venv\Scripts\python.exe" (
    echo Membuat virtual environment...
    python -m venv "%~dp0backend\venv"
)

call "%~dp0backend\venv\Scripts\activate.bat"
python -m pip install --upgrade pip
pip install -r "%~dp0backend\requirements.txt"
pip install pyinstaller pywebview

:: 5. Ensure FFmpeg Binaries
echo [3/4] Menyiapkan binary FFmpeg...
if not exist "%~dp0bin" mkdir "%~dp0bin"
if not exist "%~dp0bin\ffmpeg.exe" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "& { $ffmpegPath = (Get-Command ffmpeg -ErrorAction SilentlyContinue).Source; if ($ffmpegPath -and (Test-Path $ffmpegPath)) { Write-Host ('Menyalin ffmpeg dari sistem: ' + $ffmpegPath); Copy-Item $ffmpegPath '%~dp0bin\ffmpeg.exe'; $ffprobePath = Join-Path (Split-Path $ffmpegPath) 'ffprobe.exe'; if (Test-Path $ffprobePath) { Copy-Item $ffprobePath '%~dp0bin\ffprobe.exe' } } else { Write-Host 'Mengunduh static portable FFmpeg dari GitHub...'; $zipUrl = 'https://github.com/yt-dlp/FFmpeg-Builds/releases/download/autobuild-2024-05-31-14-11/ffmpeg-N-115715-g8787090887-win64-gpl.zip'; $zipFile = '%~dp0bin\ffmpeg.zip'; Invoke-WebRequest -Uri $zipUrl -OutFile $zipFile; Expand-Archive -Path $zipFile -DestinationPath '%~dp0bin\temp' -Force; Get-ChildItem -Path '%~dp0bin\temp' -Recurse -Filter 'ffmpeg.exe' | Copy-Item -Destination '%~dp0bin\ffmpeg.exe'; Get-ChildItem -Path '%~dp0bin\temp' -Recurse -Filter 'ffprobe.exe' | Copy-Item -Destination '%~dp0bin\ffprobe.exe'; Remove-Item -Recurse -Force '%~dp0bin\temp'; Remove-Item -Force $zipFile } }"
)

:: 6. Run PyInstaller
echo [4/4] Mengompilasi aplikasi desktop mandiri dengan PyInstaller...
cd "%~dp0"
pyinstaller universal_downloader.spec --clean --noconfirm

:: 7. Copy FFmpeg to dist/UniversalDownloader
if exist "%~dp0bin\ffmpeg.exe" (
    echo Menyalin FFmpeg ke direktori rilis...
    copy /y "%~dp0bin\ffmpeg.exe" "%~dp0dist\UniversalDownloader\" >nul
)
if exist "%~dp0bin\ffprobe.exe" (
    copy /y "%~dp0bin\ffprobe.exe" "%~dp0dist\UniversalDownloader\" >nul
)

:: 8. Create Distribution ZIP
echo Mengompresi paket rilis ke UniversalDownloader-Windows.zip...
"%~dp0backend\venv\Scripts\python.exe" -m zipfile -c "%~dp0dist\UniversalDownloader-Windows.zip" "%~dp0dist\UniversalDownloader"

echo.
echo =========================================================
echo   SUKSES! UniversalDownloader.exe berhasil dikompilasi!
echo   Lokasi folder: desktop\dist\UniversalDownloader\
echo   Lokasi file zip: desktop\dist\UniversalDownloader-Windows.zip
echo =========================================================
echo.
