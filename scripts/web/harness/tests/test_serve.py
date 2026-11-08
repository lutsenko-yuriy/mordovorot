"""serve.py (GH-262): the /offline, /online and /next-version switches and the result log.

Run: python -m unittest discover -s scripts/web/harness/tests -t scripts/web/harness
"""

import http.server
import tempfile
import threading
import unittest
import urllib.error
import urllib.request
from pathlib import Path

import serve


class ServeTest(unittest.TestCase):

    def setUp(self):
        site = tempfile.TemporaryDirectory()
        self.addCleanup(site.cleanup)
        self.results = Path(site.name) / "results.txt"
        (Path(site.name) / "page.html").write_text("page")
        (Path(site.name) / "offline-worker.js").write_text("worker")
        server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), serve.handler(str(self.results), site.name))
        threading.Thread(target=server.serve_forever, daemon=True).start()
        self.addCleanup(server.server_close)
        self.addCleanup(server.shutdown)
        self.base = f"http://127.0.0.1:{server.server_address[1]}"

    def get(self, path):
        with urllib.request.urlopen(self.base + path) as response:
            return response.read().decode()

    def post(self, path, body=b""):
        request = urllib.request.Request(self.base + path, data=body, method="POST")
        with urllib.request.urlopen(request) as response:
            return response.status

    def test_serves_files(self):
        self.assertEqual("page", self.get("/page.html"))

    def test_result_posts_are_logged(self):
        self.assertEqual(204, self.post("/result", b"OK a"))
        self.post("/result", b"OK b")

        self.assertEqual("OK a\nOK b\n", self.results.read_text())

    def test_offline_answers_503_to_every_get(self):
        self.assertEqual(204, self.post("/offline"))

        for path in ("/page.html", "/offline-worker.js"):
            with self.assertRaises(urllib.error.HTTPError) as caught:
                self.get(path)
            self.assertEqual(503, caught.exception.code)

    def test_offline_still_takes_results(self):
        self.post("/offline")

        self.post("/result", b"OK a")

        self.assertEqual("OK a\n", self.results.read_text())

    def test_online_serves_again(self):
        self.post("/offline")
        self.post("/online")

        self.assertEqual("page", self.get("/page.html"))

    def test_the_worker_is_unchanged_until_a_next_version(self):
        self.assertEqual("worker", self.get("/offline-worker.js"))

    def test_next_version_changes_only_the_worker(self):
        self.post("/next-version")

        self.assertEqual("worker\n// next-version 1\n", self.get("/offline-worker.js"))
        self.assertEqual("page", self.get("/page.html"))

    def test_each_next_version_differs(self):
        self.post("/next-version")
        first = self.get("/offline-worker.js")
        self.post("/next-version")

        self.assertNotEqual(first, self.get("/offline-worker.js"))

    def test_a_variant_without_the_worker_still_answers_404_after_a_next_version(self):
        self.post("/next-version")

        with self.assertRaises(urllib.error.HTTPError) as caught:
            self.get("/other/offline-worker.js")

        self.assertEqual(404, caught.exception.code)

    def test_the_worker_with_a_query_gets_the_next_version_too(self):
        self.post("/next-version")

        self.assertIn("next-version 1", self.get("/offline-worker.js?x=1"))


if __name__ == "__main__":
    unittest.main()
