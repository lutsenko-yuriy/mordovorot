"""GH-147: serves the harness site on 127.0.0.1 and appends each POST /result body to a file.

GH-262: three more POSTs stand in for the network, for the offline worker's checks:
  /offline       every later GET answers 503, as a device with no connection would
  /online        GETs are served again
  /next-version  offline-worker.js is served with one more comment line each time, so the browser
                 finds a new version of the worker from the same files, without a second build

Usage: serve.py <site dir> <results file> [port]
"""
import http.server
import os
import sys


def handler(results: str, site: str = "."):
    class Handler(http.server.SimpleHTTPRequestHandler):
        offline = False
        version = 0

        def __init__(self, *args, **kwargs):
            super().__init__(*args, directory=site, **kwargs)

        def do_GET(self):
            if Handler.offline:
                self.send_error(503)
            elif Handler.version and self.path.split("?")[0].endswith("/offline-worker.js") and os.path.isfile(self.translate_path(self.path.split("?")[0])):
                self.send_next_version()
            else:
                super().do_GET()

        def send_next_version(self):
            with open(self.translate_path(self.path.split("?")[0]), "rb") as source:
                body = source.read() + f"\n// next-version {Handler.version}\n".encode()
            self.send_response(200)
            self.send_header("Content-Type", "text/javascript")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def do_POST(self):
            if self.path == "/offline":
                Handler.offline = True
            elif self.path == "/online":
                Handler.offline = False
            elif self.path == "/next-version":
                Handler.version += 1
            else:
                length = int(self.headers.get("Content-Length", 0))
                with open(results, "a", encoding="utf-8") as out:
                    out.write(self.rfile.read(length).decode() + "\n")
            self.send_response(204)
            self.end_headers()

        def log_message(self, *args):
            pass

    return Handler


def main() -> None:
    site, results = os.path.abspath(sys.argv[1]), os.path.abspath(sys.argv[2])
    port = int(sys.argv[3]) if len(sys.argv) > 3 else 8765
    http.server.ThreadingHTTPServer(("127.0.0.1", port), handler(results, site)).serve_forever()


if __name__ == "__main__":
    main()
