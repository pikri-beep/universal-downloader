import os
import re
import subprocess
import sys
import threading
import time
import uuid
from typing import Any, Dict, List, Optional
import yt_dlp
from yt_dlp.extractor.instagram import InstagramIE
from yt_dlp.extractor.twitter import TwitterIE
from yt_dlp.utils import traverse_obj
import requests

from config import get_download_dir, sanitize_filename, get_ffmpeg_path

# -------------------------------------------------------------
# Patch InstagramIE to support Instagram Photos & Photo Carousels
# -------------------------------------------------------------
_orig_instagram_extract = InstagramIE._real_extract

def _patched_instagram_extract(self, url):
    orig_raise = self.raise_no_formats

    def dummy_raise(msg, *args, **kwargs):
        if 'There is no video in this post' in str(msg):
            return
        return orig_raise(msg, *args, **kwargs)

    self.raise_no_formats = dummy_raise
    res = _orig_instagram_extract(self, url)

    # Attach photo formats if formats is empty
    if not res.get('formats') and res.get('thumbnails'):
        best_thumb = res['thumbnails'][-1]['url']
        res['formats'] = [{
            'url': best_thumb,
            'ext': 'jpg',
            'format_id': 'photo',
            'vcodec': 'jpg',
            'acodec': 'none',
        }]

    # Handle carousel entries if any item is a photo
    if res.get('entries'):
        for entry in res['entries']:
            if not entry.get('formats') and entry.get('thumbnails'):
                best_thumb = entry['thumbnails'][-1]['url']
                entry['formats'] = [{
                    'url': best_thumb,
                    'ext': 'jpg',
                    'format_id': 'photo',
                    'vcodec': 'jpg',
                    'acodec': 'none',
                }]

    return res

InstagramIE._real_extract = _patched_instagram_extract

# -------------------------------------------------------------
# Patch TwitterIE to support Twitter / X Photos & Galleries
# -------------------------------------------------------------
_orig_twitter_extract = TwitterIE._real_extract

def _patched_twitter_extract(self, url):
    twid = self._match_id(url)
    try:
        return _orig_twitter_extract(self, url)
    except yt_dlp.utils.ExtractorError as e:
        if 'No video could be found in this tweet' not in str(e):
            raise

        status = self._extract_status(twid)
        media_list = traverse_obj(status, ((None, 'quoted_status'), 'extended_entities', 'media', lambda _, m: m.get('type') == 'photo', {dict}))
        if not media_list:
            media_list = traverse_obj(status, ((None, 'quoted_status'), 'entities', 'media', lambda _, m: m.get('type') == 'photo', {dict}))

        if not media_list:
            raise

        uploader = traverse_obj(status, ('user', 'screen_name', {str})) or 'Twitter'
        uploader_id = traverse_obj(status, ('user', 'id_str', {str}))
        full_text = traverse_obj(status, ('full_text', {str})) or traverse_obj(status, ('text', {str})) or ''
        
        # Clean title: if it's only a t.co link, use a readable title
        if full_text.startswith('http') or not full_text.strip():
            t_title = f'Foto Post @{uploader}'
        else:
            t_title = full_text[:100].strip()

        base_info = {
            'id': twid,
            'title': t_title,
            'uploader': uploader,
            'uploader_id': uploader_id,
            'channel': uploader,
            'webpage_url': f'https://x.com/{uploader}/status/{twid}',
        }

        entries = []
        for idx, m in enumerate(media_list):
            m_url = m.get('media_url_https') or m.get('media_url')
            if not m_url:
                continue
            orig_img = m_url.split('?')[0] + '?name=orig'
            entries.append({
                **base_info,
                'id': m.get('id_str') or f'{twid}_{idx+1}',
                'title': f'{t_title} [{idx+1}]' if len(media_list) > 1 else t_title,
                'thumbnail': orig_img,
                'thumbnails': [{'url': orig_img}],
                'formats': [{
                    'url': orig_img,
                    'ext': 'jpg',
                    'format_id': 'photo',
                    'vcodec': 'jpg',
                    'acodec': 'none',
                }]
            })

        if len(entries) == 1:
            return entries[0]
        return self.playlist_result(entries, playlist_id=twid, **base_info)

TwitterIE._real_extract = _patched_twitter_extract

# -------------------------------------------------------------
# Clean Error Messages (Remove ANSI colors & yt-dlp boilerplate)
# -------------------------------------------------------------
def clean_error_message(err_str: str) -> str:
    cleaned = re.sub(r'\x1b\[[0-9;]*m', '', str(err_str))
    cleaned = re.sub(r'[\x00-\x1f\x7f-\x9f]', '', cleaned)
    cleaned = re.sub(r';\s*please report this issue on\s+https://github.com/yt-dlp/yt-dlp/issues.*', '', cleaned, flags=re.DOTALL)
    cleaned = re.sub(r'Confirm you are on the latest version using\s+yt-dlp\s+-U.*', '', cleaned, flags=re.DOTALL)
    cleaned = re.sub(r'^(ERROR:\s*)+', '', cleaned.strip())
    return cleaned.strip()

# In-memory store for download task progress
tasks_lock = threading.Lock()
tasks: Dict[str, Dict[str, Any]] = {}

def get_task_status(task_id: str) -> Optional[Dict[str, Any]]:
    with tasks_lock:
        return tasks.get(task_id, None)

def update_task_status(task_id: str, **kwargs):
    with tasks_lock:
        if task_id in tasks:
            tasks[task_id].update(kwargs)
            tasks[task_id]["updated_at"] = time.time()

def format_duration(seconds: Optional[int]) -> str:
    if not seconds:
        return ""
    m, s = divmod(int(seconds), 60)
    h, m = divmod(m, 60)
    if h > 0:
        return f"{h:02d}:{m:02d}:{s:02d}"
    return f"{m:02d}:{s:02d}"

def format_bytes(bytes_val: Optional[float]) -> str:
    if not bytes_val:
        return ""
    for unit in ['B', 'KB', 'MB', 'GB']:
        if bytes_val < 1024.0:
            return f"{bytes_val:.1f} {unit}"
        bytes_val /= 1024.0
    return f"{bytes_val:.1f} TB"

def inspect_tiktok_fallback(url: str) -> Optional[Dict[str, Any]]:
    """Fallback extractor for TikTok sensitive/age-restricted videos via public API."""
    try:
        r = requests.post("https://www.tikwm.com/api/", data={"url": url}, timeout=15)
        res = r.json()
        if res.get("code") != 0 or not res.get("data"):
            return None
        data = res["data"]

        images = data.get("images") or []
        is_carousel = len(images) > 1
        slides = []
        if is_carousel:
            for idx, img in enumerate(images):
                slides.append({
                    "index": idx,
                    "id": f"{data.get('id')}_{idx+1}",
                    "title": f"Slide {idx+1}",
                    "thumbnail": img,
                    "url": img,
                    "is_video": False,
                    "duration": ""
                })

        is_photo = bool(images and not data.get("play"))

        return {
            "id": str(data.get("id") or uuid.uuid4())[:12],
            "title": data.get("title") or "TikTok Media",
            "author": data.get("author", {}).get("nickname") or "TikTok Creator",
            "duration": format_duration(data.get("duration")),
            "duration_seconds": data.get("duration"),
            "thumbnail": data.get("cover") or data.get("origin_cover") or (images[0] if images else ""),
            "extractor": "TikTok",
            "webpage_url": url,
            "is_carousel": is_carousel,
            "is_photo": is_photo,
            "slides": slides,
            "video_resolutions": ["1080p", "720p", "Original (No-WM)"],
            "audio_bitrates": ["320k", "192k", "128k"],
            "has_subtitles": False,
            "_direct_play_url": data.get("play"),
            "_direct_music_url": data.get("music"),
        }
    except Exception:
        return None

def inspect_url(url: str) -> Dict[str, Any]:
    """Inspects a media URL using yt-dlp and returns normalized metadata."""
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
        'extract_flat': 'in_playlist',
        'skip_download': True,
        'format': 'all/best',
    }

    info = None
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            if not info:
                raise ValueError("Tidak dapat mengambil informasi dari link ini.")
    except Exception as e:
        err_msg = str(e)
        if "tiktok.com" in url or "TikTok" in err_msg:
            fb = inspect_tiktok_fallback(url)
            if fb:
                return fb
        cleaned_err = clean_error_message(err_msg)
        raise ValueError(cleaned_err)

    if not info:
        if "tiktok.com" in url:
            fb = inspect_tiktok_fallback(url)
            if fb:
                return fb
        raise ValueError("Tidak dapat mengambil informasi dari link ini.")

    # Check if this is a playlist / multi-slide carousel
    entries = info.get("entries")
    is_carousel = bool(entries and len(entries) > 1)

    slides: List[Dict[str, Any]] = []
    if is_carousel:
        for idx, entry in enumerate(entries):
            slide_title = entry.get("title") or f"Slide {idx + 1}"
            slide_thumb = entry.get("thumbnail") or (entry.get("thumbnails", [{}])[-1].get("url") if entry.get("thumbnails") else "")
            slide_url = entry.get("url") or entry.get("webpage_url") or url
            is_vid = bool(entry.get("duration") or (entry.get("vcodec") and entry.get("vcodec") not in ("none", "jpg")) or entry.get("ext") == "mp4")
            slides.append({
                "index": idx,
                "id": entry.get("id") or str(idx),
                "title": slide_title,
                "thumbnail": slide_thumb,
                "url": slide_url,
                "is_video": is_vid,
                "duration": format_duration(entry.get("duration"))
            })

    # Available video resolution options
    formats = info.get("formats", [])
    heights_available = set()
    has_video = False
    for f in formats:
        h = f.get("height")
        if h and isinstance(h, int):
            heights_available.add(h)
        if f.get("vcodec") and f.get("vcodec") not in ("none", "jpg"):
            has_video = True

    # Standard resolution choices
    resolutions = []
    for r in [1080, 720, 480, 360]:
        if heights_available:
            if any(h >= r for h in heights_available) or r in [720, 480, 360]:
                resolutions.append(f"{r}p")
        else:
            resolutions.append(f"{r}p")

    if not resolutions:
        resolutions = ["1080p", "720p", "480p", "Best"]

    thumbnail_url = info.get("thumbnail") or (info.get("thumbnails", [{}])[-1].get("url") if info.get("thumbnails") else "")

    # Clean title
    raw_title = info.get("title") or "Media Download"
    if raw_title.startswith("Video by ") and info.get("uploader"):
        raw_title = f"Postingan {info.get('uploader')}"

    return {
        "id": info.get("id") or str(uuid.uuid4())[:8],
        "title": raw_title,
        "author": info.get("uploader") or info.get("channel") or info.get("uploader_id") or "Media Author",
        "duration": format_duration(info.get("duration")),
        "duration_seconds": info.get("duration"),
        "thumbnail": thumbnail_url,
        "extractor": info.get("extractor_key") or info.get("extractor") or "Universal",
        "webpage_url": info.get("webpage_url") or url,
        "is_carousel": is_carousel,
        "is_photo": not has_video and bool(thumbnail_url or formats),
        "slides": slides,
        "video_resolutions": resolutions,
        "audio_bitrates": ["320k", "192k", "128k"],
        "has_subtitles": bool(info.get("subtitles") or info.get("automatic_captions"))
    }


def _create_progress_hook(task_id: str):
    def hook(d):
        status = d.get("status")
        if status == "downloading":
            total_bytes = d.get("total_bytes") or d.get("total_bytes_estimate") or 0
            downloaded = d.get("downloaded_bytes") or 0
            percent = (downloaded / total_bytes * 100) if total_bytes > 0 else 0
            
            speed = d.get("speed")
            speed_str = f"{format_bytes(speed)}/s" if speed else ""
            
            eta = d.get("eta")
            eta_str = f"{eta}s" if eta else ""
            
            filename = os.path.basename(d.get("filename", ""))
            
            update_task_status(
                task_id,
                status="downloading",
                progress=round(percent, 1),
                speed_str=speed_str,
                eta_str=eta_str,
                current_file=filename,
                message=f"Mengunduh... {percent:.1f}% ({speed_str})"
            )
        elif status == "finished":
            filename = os.path.basename(d.get("filename", ""))
            update_task_status(
                task_id,
                status="merging",
                progress=98.0,
                current_file=filename,
                message="Memproses file..."
            )
    return hook


def execute_download(task_id: str, payload: Dict[str, Any]):
    """Background worker function executing the download."""
    url = payload.get("url")
    download_type = payload.get("type", "video") # 'video', 'audio', 'thumbnail', 'subtitles', 'image'
    quality = payload.get("quality", "1080p")
    slide_indices = payload.get("slide_indices", None)
    
    download_dir = str(get_download_dir())
    outtmpl = os.path.join(download_dir, "%(title).150s [%(id)s].%(ext)s")
    
    saved_files = []
    def post_hook(d):
        if d.get("status") == "finished":
            filepath = d.get("info_dict", {}).get("_filename") or d.get("filename")
            if filepath and filepath not in saved_files:
                saved_files.append(filepath)

    hook = _create_progress_hook(task_id)
    
    # Direct Fallback for TikTok (bypasses sensitive/age-restriction block)
    if "tiktok.com" in url:
        fb_data = inspect_tiktok_fallback(url)
        if fb_data and (fb_data.get("_direct_play_url") or fb_data.get("_direct_music_url")):
            try:
                target_stream = None
                file_ext = "mp4"
                label = "Mengunduh video TikTok (No-WM)"

                if download_type == "audio":
                    target_stream = fb_data.get("_direct_music_url") or fb_data.get("_direct_play_url")
                    file_ext = "mp3"
                    label = "Mengunduh audio TikTok"
                elif download_type in ("image", "thumbnail"):
                    target_stream = fb_data.get("thumbnail")
                    file_ext = "jpg"
                    label = "Mengunduh cover TikTok"
                else:
                    target_stream = fb_data.get("_direct_play_url")
                    file_ext = "mp4"
                    label = "Mengunduh video TikTok"

                if target_stream:
                    filename = f"{sanitize_filename(fb_data['title'])} [{fb_data['id']}].{file_ext}"
                    save_path = os.path.join(download_dir, filename)

                    update_task_status(task_id, status="downloading", progress=10.0, message=f"{label}...")
                    r = requests.get(target_stream, stream=True, headers={"User-Agent": "Mozilla/5.0"}, timeout=30)
                    r.raise_for_status()
                    total_bytes = int(r.headers.get("content-length") or 0)
                    downloaded = 0
                    start_time = time.time()

                    with open(save_path, "wb") as f:
                        for chunk in r.iter_content(chunk_size=65536):
                            if chunk:
                                f.write(chunk)
                                downloaded += len(chunk)
                                if total_bytes > 0:
                                    pct = (downloaded / total_bytes) * 100
                                    elapsed = max(0.1, time.time() - start_time)
                                    speed = downloaded / elapsed
                                    eta = int((total_bytes - downloaded) / max(1, speed))
                                    update_task_status(
                                        task_id,
                                        status="downloading",
                                        progress=round(pct, 1),
                                        speed_str=f"{format_bytes(speed)}/s",
                                        eta_str=f"{eta}s",
                                        message=f"{label}... {pct:.1f}% ({format_bytes(speed)}/s)"
                                    )

                    update_task_status(
                        task_id,
                        status="completed",
                        progress=100.0,
                        file_path=save_path,
                        file_name=filename,
                        message="Selesai! File berhasil diunduh."
                    )
                    return
            except Exception:
                pass

    ydl_opts: Dict[str, Any] = {
        "outtmpl": outtmpl,
        "progress_hooks": [hook],
        "postprocessor_hooks": [post_hook],
        "quiet": True,
        "no_warnings": True,
        "windowsfilenames": True,
    }
    ffmpeg_bin = get_ffmpeg_path()
    if ffmpeg_bin:
        ydl_opts["ffmpeg_location"] = str(Path(ffmpeg_bin).parent)

    # Handle Direct Thumbnail / Photo Download
    if download_type in ("thumbnail", "image", "photo"):
        label = "cover HD" if download_type == "thumbnail" else "foto"
        try:
            update_task_status(task_id, status="downloading", progress=30, message=f"Mengunduh {label}...")
            info = inspect_url(url)
            img_url = info.get("thumbnail")
            if not img_url:
                raise ValueError(f"{label.capitalize()} tidak ditemukan.")
            
            r = requests.get(img_url, stream=True, timeout=20)
            r.raise_for_status()
            
            filename = f"{sanitize_filename(info['title'])} [{info['id']}].jpg"
            save_path = os.path.join(download_dir, filename)
            
            with open(save_path, "wb") as f:
                for chunk in r.iter_content(chunk_size=8192):
                    f.write(chunk)
                    
            update_task_status(
                task_id,
                status="completed",
                progress=100.0,
                file_name=filename,
                file_path=save_path,
                message=f"{label.capitalize()} berhasil disimpan!"
            )
            return
        except Exception as e:
            update_task_status(task_id, status="failed", error=clean_error_message(str(e)), message=f"Gagal mengunduh {label}.")
            return

    # Handle Subtitles
    if download_type == "subtitles":
        ydl_opts.update({
            "skip_download": True,
            "writesubtitles": True,
            "writeautomaticsub": True,
            "subtitleslangs": ["all"],
            "subtitlesformat": "srt/vtt/best",
        })

    # Handle Audio Only
    elif download_type == "audio":
        bitrate = quality.replace("k", "") if "k" in quality else "320"
        ydl_opts.update({
            "format": "bestaudio/best",
            "postprocessors": [
                {
                    "key": "FFmpegExtractAudio",
                    "preferredcodec": "mp3",
                    "preferredquality": bitrate,
                },
                {"key": "FFmpegMetadata"},
            ]
        })

    # Handle Standard Video
    else:
        res_height = "1080"
        if "p" in quality:
            res_height = quality.replace("p", "")
        
        ydl_opts.update({
            "format": f"bestvideo[height<={res_height}]+bestaudio/best[height<={res_height}]/best/all",
            "merge_output_format": "mp4",
        })

    # Handle Carousel Slide Selection
    if slide_indices is not None and isinstance(slide_indices, list) and len(slide_indices) > 0:
        playlist_items_str = ",".join(str(i + 1) for i in slide_indices)
        ydl_opts["playlist_items"] = playlist_items_str
    elif payload.get("is_carousel") is False:
        ydl_opts["noplaylist"] = True

    try:
        update_task_status(task_id, status="downloading", progress=5.0, message="Menyiapkan koneksi media...")
        
        result_info = None
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            result_info = ydl.extract_info(url, download=True)
            
        final_path = None
        if result_info:
            req_downloads = result_info.get("requested_downloads")
            if req_downloads and isinstance(req_downloads, list):
                final_path = req_downloads[-1].get("filepath")
            if not final_path:
                final_path = result_info.get("_filename")

        if final_path and not os.path.exists(final_path):
            base, _ = os.path.splitext(final_path)
            for ext in [".mp4", ".mp3", ".m4a", ".webm", ".jpg", ".png"]:
                if os.path.exists(base + ext):
                    final_path = base + ext
                    break

        if not final_path or not os.path.exists(final_path):
            if saved_files and os.path.exists(saved_files[-1]):
                final_path = saved_files[-1]
            else:
                # Find most recently modified file in download_dir
                try:
                    all_files = [os.path.join(download_dir, f) for f in os.listdir(download_dir)]
                    all_files = [f for f in all_files if os.path.isfile(f)]
                    if all_files:
                        all_files.sort(key=os.path.getmtime, reverse=True)
                        final_path = all_files[0]
                except Exception:
                    pass

        if not final_path:
            final_path = download_dir

        update_task_status(
            task_id,
            status="completed",
            progress=100.0,
            file_path=final_path,
            file_name=os.path.basename(final_path) if final_path != download_dir else "Media Files",
            message="Selesai! File berhasil diunduh."
        )
    except Exception as e:
        cleaned = clean_error_message(str(e))
        update_task_status(
            task_id,
            status="failed",
            error=cleaned,
            message=cleaned
        )


def start_download_task(payload: Dict[str, Any]) -> str:
    """Spawns download task in a separate background thread."""
    task_id = str(uuid.uuid4())
    with tasks_lock:
        tasks[task_id] = {
            "task_id": task_id,
            "status": "pending",
            "progress": 0.0,
            "speed_str": "",
            "eta_str": "",
            "current_file": "",
            "file_path": None,
            "file_name": None,
            "message": "Menyiapkan download...",
            "created_at": time.time(),
            "updated_at": time.time(),
        }

    thread = threading.Thread(target=execute_download, args=(task_id, payload), daemon=True)
    thread.start()
    return task_id


def update_yt_dlp() -> Dict[str, Any]:
    """Updates yt-dlp to latest version."""
    if getattr(sys, "frozen", False):
        return {
            "success": True,
            "output": "Versi Standalone Desktop dikelola otomatis via rilis GitHub."
        }
    try:
        proc = subprocess.run(
            [sys.executable, "-m", "pip", "install", "--upgrade", "yt-dlp", "curl_cffi"],
            capture_output=True,
            text=True,
            timeout=120
        )
        success = proc.returncode == 0
        return {
            "success": success,
            "output": proc.stdout if success else proc.stderr
        }
    except Exception as e:
        return {"success": False, "output": str(e)}
