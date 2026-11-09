import asyncio
import json
import os
import subprocess
import sys
import threading
from typing import List, Optional

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, JSONResponse, StreamingResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

import config
from downloader import (
    clean_error_message,
    get_task_status,
    inspect_url,
    start_download_task,
    update_yt_dlp
)

app = FastAPI(title="Universal Media Downloader API", version="1.0.0")

# Enable CORS for local Vite dev & static frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Request Models
class InspectRequest(BaseModel):
    url: str

class DownloadRequest(BaseModel):
    url: str
    type: str = "video"  # "video", "audio", "thumbnail", "subtitles"
    quality: str = "1080p"
    is_carousel: bool = False
    slide_indices: Optional[List[int]] = None

class OpenFolderRequest(BaseModel):
    file_path: Optional[str] = None

class OpenFileRequest(BaseModel):
    file_path: Optional[str] = None


@app.post("/api/inspect")
async def handle_inspect(req: InspectRequest):
    if not req.url or not req.url.strip():
        raise HTTPException(status_code=400, detail="URL tidak boleh kosong.")
    try:
        data = await asyncio.to_thread(inspect_url, req.url.strip())
        return JSONResponse(content={"success": True, "data": data})
    except Exception as e:
        raise HTTPException(status_code=400, detail=clean_error_message(str(e)))


@app.post("/api/download")
async def handle_download(req: DownloadRequest):
    if not req.url or not req.url.strip():
        raise HTTPException(status_code=400, detail="URL tidak boleh kosong.")
    try:
        task_id = start_download_task(req.model_dump())
        return JSONResponse(content={"success": True, "task_id": task_id})
    except Exception as e:
        raise HTTPException(status_code=500, detail=clean_error_message(str(e)))


@app.get("/api/progress/{task_id}")
async def handle_progress(task_id: str, request: Request):
    async def event_generator():
        last_data_str = ""
        while True:
            if await request.is_disconnected():
                break

            status = get_task_status(task_id)
            if not status:
                data = json.dumps({"status": "not_found", "message": "Task tidak ditemukan."})
                yield f"data: {data}\n\n"
                break

            current_data_str = json.dumps(status)
            if current_data_str != last_data_str:
                yield f"data: {current_data_str}\n\n"
                last_data_str = current_data_str

            if status.get("status") in ["completed", "failed"]:
                break

            await asyncio.sleep(0.35)

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        }
    )


@app.post("/api/open-file")
async def handle_open_file(req: OpenFileRequest):
    if not req or not req.file_path:
        raise HTTPException(status_code=400, detail="Path file tidak boleh kosong.")
    
    file_path = os.path.normpath(req.file_path)
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="File tidak ditemukan di disk.")

    try:
        if sys.platform == "win32":
            os.startfile(file_path)
        else:
            subprocess.Popen(["xdg-open", file_path])
        return JSONResponse(content={"success": True, "path": file_path})
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Gagal membuka file: {str(e)}")


@app.post("/api/open-folder")
async def handle_open_folder(req: OpenFolderRequest = None):
    download_dir = str(config.get_download_dir())
    target_path = req.file_path if req and req.file_path else download_dir

    try:
        if sys.platform == "win32":
            if os.path.isfile(target_path):
                # Highlight file in Windows Explorer
                subprocess.Popen(f'explorer /select,"{os.path.normpath(target_path)}"')
            else:
                subprocess.Popen(f'explorer "{os.path.normpath(download_dir)}"')
        else:
            # Fallback for non-windows
            subprocess.Popen(["xdg-open", download_dir])
        return JSONResponse(content={"success": True, "path": target_path})
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Gagal membuka folder: {str(e)}")


@app.post("/api/update-engine")
async def handle_update_engine():
    res = await asyncio.to_thread(update_yt_dlp)
    return JSONResponse(content=res)


@app.post("/api/shutdown")
async def handle_shutdown():
    def kill_soon():
        import time
        time.sleep(0.8)
        os._exit(0)

    threading.Thread(target=kill_soon, daemon=True).start()
    return JSONResponse(content={"success": True, "message": "Server sedang dimatikan..."})


# Mount Static Frontend
dist_path = config.FRONTEND_DIST
if dist_path.exists() and (dist_path / "index.html").exists():
    app.mount("/", StaticFiles(directory=str(dist_path), html=True), name="static")
else:
    @app.get("/")
    def index_fallback():
        return HTMLResponse(
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Universal Media Downloader</title>
                <style>
                    body { font-family: system-ui, sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; background: #F6F8FA; color: #1E293B; }
                    .card { background: white; padding: 40px; border-radius: 24px; border: 2px solid #E2E8F0; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.05); text-align: center; max-width: 480px; }
                    h2 { color: #0F172A; margin-top: 0; }
                    code { background: #F1F5F9; padding: 4px 8px; border-radius: 6px; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h2>🚀 Universal Media Downloader Backend Aktif</h2>
                    <p>API Server berjalan di <code>http://127.0.0.1:8000</code>.</p>
                    <p>Frontend belum di-build. Jalankan <code>desktop/setup.bat</code> atau <code>npm run build</code> di direktori <code>desktop/frontend</code>.</p>
                </div>
            </body>
            </html>
            """
        )
