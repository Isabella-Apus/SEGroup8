import base64
import hashlib
import hmac
import json
import time
from http.server import BaseHTTPRequestHandler, HTTPServer

SECRET = b"acceptance-jwt-secret-must-have-at-least-thirty-two-bytes"
ACCOUNTS = {
    "admin": (1, "ADMIN"),
    "seller": (2, "OFFICIAL_SELLER"),
    "user": (3, "USER"),
}

def encode(value):
    return base64.urlsafe_b64encode(json.dumps(value, separators=(",", ":")).encode()).rstrip(b"=").decode()

def token_for(username, user_id, role):
    head = encode({"alg": "HS256", "typ": "JWT"})
    body = encode({"sub": str(user_id), "uid": user_id, "username": username, "role": role, "exp": int(time.time()) + 600})
    signature = base64.urlsafe_b64encode(hmac.new(SECRET, f"{head}.{body}".encode(), hashlib.sha256).digest()).rstrip(b"=").decode()
    return f"{head}.{body}.{signature}"

class Handler(BaseHTTPRequestHandler):
    def respond(self, status, body):
        encoded = json.dumps(body).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def do_POST(self):
        if self.path != "/api/auth/login":
            self.respond(404, {"code": 404, "message": "not found", "data": None})
            return
        payload = json.loads(self.rfile.read(int(self.headers.get("Content-Length", "0"))) or b"{}")
        username = payload.get("username", "")
        account = ACCOUNTS.get(username)
        if not account:
            self.respond(401, {"code": 401, "message": "invalid", "data": None})
            return
        user_id, role = account
        user = {"id": user_id, "userId": user_id, "username": username, "nickname": username, "role": role, "status": "NORMAL", "categoryId": 1}
        self.respond(200, {"code": 0, "message": "success", "data": {"token": token_for(username, user_id, role), "role": role, "user": user}})

    def do_GET(self):
        if self.path == "/health":
            self.respond(200, {"status": "UP"})
            return
        try:
            token = self.headers.get("Authorization", "").split(" ", 1)[1]
            payload = token.split(".")[1]
            payload += "=" * (-len(payload) % 4)
            claims = json.loads(base64.urlsafe_b64decode(payload))
            body = {"code": 0, "message": "success", "data": {
                "id": int(claims["uid"]), "userId": int(claims["uid"]),
                "username": claims["username"], "role": claims["role"], "status": "NORMAL"}}
            status = 200
        except Exception:
            body = {"code": 401, "message": "invalid", "data": None}
            status = 401
        self.respond(status, body)
    def log_message(self, *_):
        return

HTTPServer(("0.0.0.0", 8080), Handler).serve_forever()
