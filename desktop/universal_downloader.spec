# -*- mode: python ; coding: utf-8 -*-
import os
import sys
from pathlib import Path

block_cipher = None

SPEC_DIR = Path(SPECPATH).resolve()
BACKEND_DIR = SPEC_DIR / "backend"
FRONTEND_DIST = SPEC_DIR / "frontend" / "dist"

added_files = []
if FRONTEND_DIST.exists():
    added_files.append((str(FRONTEND_DIST), 'frontend/dist'))

# Optional Icon
icon_file = None
if (SPEC_DIR / "icon.ico").exists():
    icon_file = str(SPEC_DIR / "icon.ico")
elif (BACKEND_DIR / "icon.ico").exists():
    icon_file = str(BACKEND_DIR / "icon.ico")

hidden_imports = [
    'uvicorn',
    'uvicorn.logging',
    'uvicorn.loops',
    'uvicorn.loops.auto',
    'uvicorn.protocols',
    'uvicorn.protocols.http',
    'uvicorn.protocols.http.auto',
    'uvicorn.protocols.websockets',
    'uvicorn.protocols.websockets.auto',
    'uvicorn.lifespans',
    'uvicorn.lifespans.on',
    'fastapi',
    'pydantic',
    'starlette',
    'curl_cffi',
    'curl_cffi.requests',
    'yt_dlp',
    'yt_dlp.extractor',
    'webview',
    'webview.platforms.winforms',
    'webview.platforms.edgechromium',
    'clr',
    'pythonnet',
]

a = Analysis(
    [str(BACKEND_DIR / 'desktop_app.py')],
    pathex=[str(BACKEND_DIR)],
    binaries=[],
    datas=added_files,
    hiddenimports=hidden_imports,
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=['tkinter', 'matplotlib', 'scipy', 'numpy', 'test', 'unittest'],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    [],
    exclude_binaries=True,
    name='UniversalDownloader',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    console=False,
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
    icon=icon_file,
)

coll = COLLECT(
    exe,
    a.binaries,
    a.zipfiles,
    a.datas,
    strip=False,
    upx=True,
    upx_exclude=[],
    name='UniversalDownloader',
)
