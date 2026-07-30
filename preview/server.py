#!/usr/bin/env python3
import http.server
import socketserver
import webbrowser
import os
import sys

PORT = 8080
DIRECTORY = os.path.dirname(os.path.abspath(__file__))

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

def run_server():
    os.chdir(DIRECTORY)
    handler = Handler
    
    # Find free port if 8080 is busy
    port = PORT
    for try_port in range(8080, 8095):
        try:
            with socketserver.TCPServer(("", try_port), handler) as httpd:
                print(f"\n========================================================")
                print(f"  CFHC LIVE PLAYER PROFILE UI PREVIEW RUNNING AT:")
                print(f"  http://localhost:{try_port}/index.html")
                print(f"========================================================\n")
                httpd.serve_forever()
                break
        except OSError:
            continue

if __name__ == "__main__":
    run_server()
