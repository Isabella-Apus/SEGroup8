import http from 'node:http';

const port = Number(process.env.STUB_PORT ?? 18085);
const expectedToken = process.env.STUB_EXPECTED_TOKEN ?? '';
const requests = [];
const server = http.createServer((req, res) => {
  const entry = { method: req.method, path: req.url, hasBearer: (req.headers.authorization ?? '').startsWith('Bearer '), tokenAccepted: expectedToken ? req.headers.authorization === `Bearer ${expectedToken}` : (req.headers.authorization ?? '').startsWith('Bearer ') };
  requests.push(entry);
  const valid = req.method === 'GET' && /^\/api\/report-block\/block\/(check|blocked-by)\/1002$/.test(req.url ?? '') && entry.hasBearer && entry.tokenAccepted;
  if (!valid) { res.writeHead(400, { 'content-type': 'application/json' }); res.end(JSON.stringify({ code: 'STUB_CONTRACT_VIOLATION' })); return; }
  res.writeHead(200, { 'content-type': 'application/json' }); res.end(JSON.stringify({ code: 0, data: false }));
});
server.listen(port, '0.0.0.0', () => console.log(JSON.stringify({ ready: true, port, contract: 'GET block check/blocked-by/1002 with bearer' })));
process.on('SIGTERM', () => { console.log(JSON.stringify({ requests })); server.close(() => process.exit(0)); });
process.on('SIGINT', () => { console.log(JSON.stringify({ requests })); server.close(() => process.exit(0)); });
