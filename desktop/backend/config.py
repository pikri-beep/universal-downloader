import os
import sys
import re
from pathlib import Path

# Detect if running as frozen executable (PyInstaller)
IS_FROZEN = getattr(sys, "frozen", False)

if IS_FROZEN:
    # In PyInstaller, sys._MEIPASS holds the bundled temporary/internal files
    BUNDLE_DIR = Path(getattr(sys, "_MEIPASS", Path(sys.executable).parent))
    EXE_DIR = Path(sys.executable).parent
    FRONTEND_DIST = BUNDLE_DIR / "frontend" / "dist"
    if not FRONTEND_DIST.exists():
        FRONTEND_DIST = EXE_DIR / "frontend" / "dist"
else:
    BASE_DIR = Path(__file__).resolve().parent
    EXE_DIR = BASE_DIR
    FRONTEND_DIST = BASE_DIR.parent / "frontend" / "dist"

# Default Download Directory: ~/Downloads/UniversalDownloader
DOWNLOAD_DIR = Path.home() / "Downloads" / "UniversalDownloader"

# Server Settings
HOST = "127.0.0.1"
PORT = 8000

def get_download_dir() -> Path:
    """Return download directory and ensure it exists."""
    DOWNLOAD_DIR.mkdir(parents=True, exist_ok=True)
    return DOWNLOAD_DIR

def get_ffmpeg_path() -> str | None:
    """Find bundled or system ffmpeg binary."""
    candidates = [
        EXE_DIR / "ffmpeg.exe",
        EXE_DIR / "bin" / "ffmpeg.exe",
        (Path(sys._MEIPASS) / "ffmpeg.exe" if hasattr(sys, "_MEIPASS") else None),
        (Path(sys._MEIPASS) / "bin" / "ffmpeg.exe" if hasattr(sys, "_MEIPASS") else None),
    ]
    for c in candidates:
        if c and c.exists():
            return str(c)
    # Check if ffmpeg is in PATH
    import shutil
    sys_ffmpeg = shutil.which("ffmpeg")
    return sys_ffmpeg

def sanitize_filename(name: str) -> str:
    """Remove illegal characters for Windows filenames."""
    if not name:
        return "media"
    cleaned = re.sub(r'[\\/*?:"<>|]', "_", name)
    cleaned = cleaned.strip(". ")
    return cleaned[:200] if len(cleaned) > 200 else cleaned
