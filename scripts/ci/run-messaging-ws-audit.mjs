import crypto from 'node:crypto';
import http from 'node:http';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const { chromium } = require('../../frontend/node_modules/playwright');
const secret = process.env.AUDIT_JWT_SECRET;
if (!secret) throw new Error('AUDIT_JWT_SECRET is required');
const b64 = value => Buffer.from(value).toString('base64url');
const header = b64(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
const payload = b64(JSON.stringify({ uid: 1001, username: 'auditbuyer', role: 'USER', iat: Math.floor(Date.now() / 1000), exp: Math.floor(Date.now() / 1000) + 3600 }));
const input = `${header}.${payload}`;
const token = `${input}.${crypto.createHmac('sha256', secret).update(input).digest('base64url')}`;
const originServer = http.createServer((req, res) => { res.writeHead(200, { 'content-type': 'text/plain' }); res.end('audit origin'); });
await new Promise(resolve => originServer.listen(5174, '127.0.0.1', resolve));
const badOriginServer = http.createServer((req, res) => { res.writeHead(200, { 'content-type': 'text/plain' }); res.end('bad origin'); });
await new Promise(resolve => badOriginServer.listen(5175, '127.0.0.1', resolve));
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
async function probe(origin, suppliedToken) {
  await page.goto(`${origin}/`);
  return page.evaluate(async ({ token }) => new Promise(resolve => {
  const query = token === null ? '' : `?token=${encodeURIComponent(token)}`;
  const socket = new WebSocket(`ws://127.0.0.1:18084/ws/realtime${query}`);
  const timer = setTimeout(() => { socket.close(); resolve({ handshake: false, reason: 'timeout' }); }, 5000);
  socket.onopen = () => { clearTimeout(timer); socket.close(); resolve({ handshake: true }); };
  socket.onerror = () => { clearTimeout(timer); resolve({ handshake: false, reason: 'error' }); };
  socket.onclose = event => { if (event.code !== 1000) { clearTimeout(timer); resolve({ handshake: false, reason: `close-${event.code}` }); } };
}), { token: suppliedToken });
}
const valid = await probe('http://localhost:5174', token);
const invalid = await probe('http://localhost:5174', 'invalid-token');
const missing = await probe('http://localhost:5174', null);
const badOrigin = await probe('http://localhost:5175', token);
await browser.close();
originServer.close();
badOriginServer.close();
console.log(JSON.stringify({ endpoint: 'ws://127.0.0.1:18084/ws/realtime', validOrigin: valid, invalidToken: invalid, missingToken: missing, invalidOrigin: badOrigin }));
