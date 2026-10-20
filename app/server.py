"""Local-only HTTP server for the experimental dashboard."""
from __future__ import annotations

import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit

from app.assistant import handle
from app.telemetry import alerts, build_source

ROOT = Path(__file__).resolve().parent.parent
STATIC = ROOT / "web"
MIME = {"/": ("index.html", "text/html; charset=utf-8"),
        "/index.html": ("index.html", "text/html; charset=utf-8"),
        "/app.js": ("app.js", "text/javascript; charset=utf-8"),
        "/style.css": ("style.css", "text/css; charset=utf-8")}


class Handler(BaseHTTPRequestHandler):
    source = None
    threshold = 110.0

    def _headers(self, status: int, content_type: str, size: int) -> None:
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(size))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("X-Frame-Options", "DENY")
        self.send_header("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; media-src 'self' blob:; object-src 'none'; base-uri 'none'; frame-ancestors 'none'")
        self.end_headers()

    def _send(self, status: int, payload: bytes, content_type="application/json; charset=utf-8") -> None:
        self._headers(status, content_type, len(payload))
        self.wfile.write(payload)

    def _json(self, status: int, data: dict) -> None:
        self._send(status, json.dumps(data, ensure_ascii=False).encode("utf-8"))

    def do_GET(self) -> None:
        path = urlsplit(self.path).path
        if path == "/api/telemetry":
            snapshot = self.source.snapshot()
            snapshot["alerts"] = alerts(snapshot, self.threshold)
            self._json(200, snapshot)
        elif path in MIME:
            filename, mime = MIME[path]
            self._send(200, (STATIC / filename).read_bytes(), mime)
        else:
            self._json(404, {"error": "Not found"})

    def do_POST(self) -> None:
        if urlsplit(self.path).path != "/api/assistant":
            self._json(404, {"error": "Not found"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if not 0 < length <= 2048:
                self._json(413, {"error": "Input size invalid"})
                return
            request_data = json.loads(self.rfile.read(length))
            msg = request_data.get("text", "")
            if not isinstance(msg, str) or len(msg) > 400:
                self._json(400, {"error": "Invalid text"})
                return
        except (ValueError, TypeError, json.JSONDecodeError):
            self._json(400, {"error": "Invalid JSON"})
            return
        try:
            self._json(200, handle(msg, self.source.snapshot(), self.threshold))
        except Exception:
            self._json(503, {"error": "Telemetry temporarily unavailable"})


def main() -> None:
    Handler.source = build_source()
    Handler.threshold = float(os.getenv("MAX_COOLANT_C", "110"))
    host = os.getenv("HOST", "127.0.0.1")
    port = int(os.getenv("PORT", "8787"))
    print(f"OpenCar Copilot (experimental) → http://{host}:{port}", flush=True)
    ThreadingHTTPServer((host, port), Handler).serve_forever()


if __name__ == "__main__":
    main()
