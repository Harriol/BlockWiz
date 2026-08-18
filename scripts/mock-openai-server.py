#!/usr/bin/env python3
"""BlockWiz 连接测试用假 OpenAI 兼容服务端（仅本地测试用）。

用法:
    python scripts/mock-openai-server.py [--port 端口]
配置页 Base URL 填: http://127.0.0.1:<端口>/v1 ，模型随意（如 mock-model）。
默认端口 18080；预设场景可另起实例，如 Ollama 预设的 11434:
    python scripts/mock-openai-server.py --port 11434

故障演练（在 Base URL 后拼接查询参数）:
    ?status=401   鉴权失败
    ?status=429   被限流
    ?status=500   服务端错误
    ?badjson=1    返回非 JSON
    ?timeout=10   挂起 10 秒（配合配置里的请求超时 < 10 秒可复现超时）
"""
import argparse
import json
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

DEFAULT_PORT = 18080


class MockOpenAiHandler(BaseHTTPRequestHandler):
    """处理 POST /v1/chat/completions，按查询参数模拟不同响应。"""

    def do_POST(self):
        query = parse_qs(urlparse(self.path).query)

        if "timeout" in query:
            time.sleep(int(query["timeout"][0]))

        status = int(query.get("status", ["200"])[0])
        if status != 200:
            body = json.dumps({"error": {"message": "mock failure", "type": "mock_error"}}).encode("utf-8")
        elif "badjson" in query:
            body = b"this is not json at all"
        else:
            body = json.dumps({"choices": [{"message": {"content": "pong"}}]}).encode("utf-8")

        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt, *args):
        print("[mock-openai] %s" % (fmt % args))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="BlockWiz 假 OpenAI 服务端")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help="监听端口（默认 %d）" % DEFAULT_PORT)
    args = parser.parse_args()
    server = ThreadingHTTPServer(("127.0.0.1", args.port), MockOpenAiHandler)
    print("BlockWiz mock OpenAI server 已启动:")
    print("  http://127.0.0.1:%d/v1/chat/completions" % args.port)
    print("  演练: ?status=401 | ?status=429 | ?status=500 | ?badjson=1 | ?timeout=10")
    server.serve_forever()
