"""GH-147: serves the phone-fit site on 127.0.0.1 and appends each POST /result body to a file.

Usage: serve.py <site dir> <results file> [port]
"""
import http.server
import os
import sys


def handler(results: str):
    class Handler(http.server.SimpleHTTPRequestHandler):
        def do_POST(self):
            length = int(self.headers.get("Content-Length", 0))
            with open(results, "a", encoding="utf-8") as out:
                out.write(self.rfile.read(length).decode() + "\n")
            self.send_response(204)
            self.end_headers()

        def log_message(self, *args):
            pass

    return Handler


def main() -> None:
    site, results = sys.argv[1], os.path.abspath(sys.argv[2])
    port = int(sys.argv[3]) if len(sys.argv) > 3 else 8765
    os.chdir(site)
    http.server.ThreadingHTTPServer(("127.0.0.1", port), handler(results)).serve_forever()


if __name__ == "__main__":
    main()
