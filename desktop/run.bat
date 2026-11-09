@echo off
setlocal
title Universal Media Downloader Launcher

echo Memeriksa status server...

:: Cek apakah port 8000 sudah berjalan (Single-Instance Check)
netstat -ano | findstr :8000 | findstr LISTENING >nul 2>nul
if %errorlevel% equ 0 (
    echo Server Universal Downloader sudah aktif di port 8000.
    echo Membuka tab aplikasi di browser...
    start http://127.0.0.1:8000
    ping 127.0.0.1 -n 2 >nul
    exit /b 0
)

:: Jika belum berjalan, nyalakan backend di background (minimized)
echo Menyalakan backend engine...
if exist "%~dp0backend\venv\Scripts\python.exe" (
    start "UniversalDownloaderBackend" /min "%~dp0backend\venv\Scripts\python.exe" -m uvicorn main:app --app-dir "%~dp0backend" --host 127.0.0.1 --port 8000
) else (
    echo [ERROR] Virtual environment tidak ditemukan. Harap jalankan setup.bat terlebih dahulu.
    pause
    exit /b 1
)

:: Tunggu 2 detik agar FastAPI siap
ping 127.0.0.1 -n 3 >nul

:: Buka browser default
echo Membuka antarmuka di browser...
start http://127.0.0.1:8000
exit /b 0
