import os
import sys
import io
import time
import socket
import threading
import webbrowser
import traceback
import multiprocessing
from pathlib import Path

# Fix for PyInstaller --noconsole (where sys.stdout / sys.stderr are None)
class DummyStream:
    def write(self, s):
        pass
    def flush(self):
        pass
    def isatty(self):
        return False

if sys.stdout is None:
    sys.stdout = DummyStream()
if sys.stderr is None:
    sys.stderr = DummyStream()

# Add backend directory to sys.path
BASE_DIR = Path(__file__).resolve().parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

import config

LOG_FILE = (
    Path(sys.executable).parent / "desktop_debug.log"
    if getattr(sys, "frozen", False)
    else BASE_DIR / "desktop_debug.log"
)

def log(msg: str):
    try:
        with open(LOG_FILE, "a", encoding="utf-8") as f:
            f.write(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] {msg}\n")
    except Exception:
        pass

def find_free_port(start_port: int = 8765) -> int:
    """Finds an available local TCP port."""
    for port in range(start_port, start_port + 50):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            try:
                s.bind(("127.0.0.1", port))
                return port
            except OSError:
                continue
    return start_port

def run_uvicorn(port: int):
    """Runs the FastAPI server silently in background."""
    try:
        log(f"Starting uvicorn on 127.0.0.1:{port}...")
        import uvicorn
        from main import app
        log("Imported main:app successfully.")

        # Crucial: log_config=None prevents uvicorn from crashing on sys.stdout.isatty() in frozen mode
        server_config = uvicorn.Config(
            app,
            host="127.0.0.1",
            port=port,
            log_config=None,
            access_log=False
        )
        server = uvicorn.Server(server_config)
        log("Calling server.run()...")
        server.run()
        log("Uvicorn server.run() exited normally.")
    except Exception as e:
        log(f"UVICORN EXCEPTION: {traceback.format_exc()}")

def main():
    multiprocessing.freeze_support()
    log("=== Universal Downloader Starting ===")
    log(f"Executable: {sys.executable}, Frozen: {getattr(sys, 'frozen', False)}")
    
    port = find_free_port(8765)
    log(f"Using port: {port}")
    
    # Start server in background thread
    server_thread = threading.Thread(target=run_uvicorn, args=(port,), daemon=True)
    server_thread.start()
    
    # Wait for server initialization (up to 15 seconds)
    app_url = f"http://127.0.0.1:{port}"
    server_ready = False
    for i in range(150):
        try:
            with socket.create_connection(("127.0.0.1", port), timeout=0.1):
                server_ready = True
                log(f"Server is responding on port {port} after {i * 0.1:.1f}s!")
                break
        except (OSError, ConnectionRefusedError):
            time.sleep(0.1)

    if not server_ready:
        log("ERROR: Server failed to respond within 15 seconds!")

    # Try PyWebView (Edge Chromium WebView2) first for native desktop experience
    use_browser = "--browser" in sys.argv
    webview_success = False

    if not use_browser and server_ready:
        try:
            log("Attempting to initialize pywebview...")
            import webview
            
            icon_path = None
            for candidate in [
                config.EXE_DIR / "icon.ico",
                BASE_DIR / "icon.ico",
                (Path(sys._MEIPASS) / "icon.ico" if hasattr(sys, "_MEIPASS") else None)
            ]:
                if candidate and candidate.exists():
                    icon_path = str(candidate)
                    break

            log(f"Creating webview window pointing to {app_url}...")
            window = webview.create_window(
                title="Universal Media Downloader",
                url=app_url,
                width=1140,
                height=800,
                min_size=(880, 620),
                background_color="#FFFDF7",
                text_select=True,
                zoomable=True
            )
            
            log("Starting webview with gui='edgechromium'...")
            webview.start(gui="edgechromium", debug=False)
            log("Webview closed by user. Exiting...")
            webview_success = True
            os._exit(0)
        except Exception as e:
            log(f"PYWEBVIEW EXCEPTION: {traceback.format_exc()}")
            log("Falling back to default browser...")

    if not webview_success:
        log(f"Opening browser for URL: {app_url}")
        webbrowser.open(app_url)
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            log("Process terminated by user.")
            os._exit(0)

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        log(f"FATAL MAIN EXCEPTION: {traceback.format_exc()}")
        os._exit(1)
