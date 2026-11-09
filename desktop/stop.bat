@echo off
setlocal
title Stop Universal Media Downloader

echo Menghentikan proses Universal Downloader di port 8000...

set FOUND=0
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8000 ^| findstr LISTENING 2^>nul') do (
    set FOUND=1
    echo Menghentikan proses PID: %%a
    taskkill /f /pid %%a >nul 2>nul
)

if %FOUND% equ 1 (
    echo Server Universal Downloader berhasil dihentikan.
) else (
    echo Tidak ada server Universal Downloader yang sedang aktif.
)

ping 127.0.0.1 -n 2 >nul
exit /b 0
