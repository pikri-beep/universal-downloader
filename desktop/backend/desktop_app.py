import os
import sys
import time
import socket
import threading
import webbrowser
from pathlib import Path

# Add backend directory to sys.path
BASE_DIR = Path(__file__).resolve().parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

import config

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
    import uvicorn
    from main import app
    
    server_config = uvicorn.Config(
        app,
        host="127.0.0.1",
        port=port,
        log_level="warning",
        access_log=False
    )
    server = uvicorn.Server(server_config)
    server.run()

def main():
    port = find_free_port(8765)
    
    # Start server in background thread
    server_thread = threading.Thread(target=run_uvicorn, args=(port,), daemon=True)
    server_thread.start()
    
    # Wait briefly for server initialization
    app_url = f"http://127.0.0.1:{port}"
    for _ in range(30):
        try:
            with socket.create_connection(("127.0.0.1", port), timeout=0.1):
                break
        except (OSError, ConnectionRefusedError):
            time.sleep(0.1)

    # Try PyWebView (Edge Chromium WebView2) first for native desktop experience
    use_browser = "--browser" in sys.argv
    if not use_browser:
        try:
            import webview
            
            # Find custom icon if exists
            icon_path = None
            for candidate in [
                config.EXE_DIR / "icon.ico",
                BASE_DIR / "icon.ico",
                (Path(sys._MEIPASS) / "icon.ico" if hasattr(sys, "_MEIPASS") else None)
            ]:
                if candidate and candidate.exists():
                    icon_path = str(candidate)
                    break

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
            
            # Start WebView using Microsoft Edge Chromium engine
            webview.start(gui="edgechromium", debug=False)
            os._exit(0)
        except Exception as e:
            # Fallback to browser if webview encounters system error
            print(f"[Warning] PyWebView could not start ({e}). Falling back to browser...")

    # Fallback: Launch in default browser
    webbrowser.open(app_url)
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        os._exit(0)

if __name__ == "__main__":
    main()
