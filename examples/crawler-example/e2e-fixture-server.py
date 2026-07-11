#!/usr/bin/env python3
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


STATIC_PAGE = b"""<!doctype html>
<html><head><title>Nebula E2E Static</title>
<meta name="description" content="nebula_e2e_static_description"></head>
<body><main id="static-marker">nebula_e2e_static_content</main>
<a href="/alpha">Alpha</a><a href="/beta">Beta</a></body></html>"""

SECOND_PAGE = b"""<!doctype html>
<html><head><title>Nebula E2E Batch Two</title></head>
<body><main>nebula_e2e_batch_second</main></body></html>"""

DYNAMIC_PAGE = b"""<!doctype html>
<html><head><title>Nebula E2E Browser</title></head><body>
<main id="dynamic-result">pending</main>
<script>setTimeout(() => {
  const target = document.getElementById('dynamic-result');
  target.textContent = 'nebula_e2e_browser_rendered';
  target.dataset.ready = 'true';
}, 150);</script></body></html>"""


class FixtureHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/slow":
            time.sleep(2)
            self._send_html(b"<html><body>nebula_e2e_slow</body></html>")
        elif self.path == "/static":
            self._send_html(STATIC_PAGE)
        elif self.path == "/static-two":
            self._send_html(SECOND_PAGE)
        elif self.path == "/dynamic":
            self._send_html(DYNAMIC_PAGE)
        elif self.path in ("/alpha", "/beta"):
            self._send_html(b"<html><body>linked</body></html>")
        else:
            self.send_error(404)

    def _send_html(self, body):
        try:
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def log_message(self, fmt, *args):
        sys.stdout.write("%s - %s\n" % (self.address_string(), fmt % args))
        sys.stdout.flush()


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 18085
    ThreadingHTTPServer(("0.0.0.0", port), FixtureHandler).serve_forever()
