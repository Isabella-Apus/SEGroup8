import http from 'node:http';

const port = Number(process.env.STUB_PORT ?? 18085);
const expectedToken = process.env.STUB_EXPECTED_TOKEN ?? '';
const requests = [];
const server = http.createServer((req, res) => {
  const chunks = [];
  req.on('data', (chunk) => chunks.push(chunk));
  req.on('end', () => {
    const bodyText = Buffer.concat(chunks).toString('utf8');
    const internalToken = req.headers['x-internal-service-token'] ?? '';
    const entry = {
      method: req.method,
      path: req.url,
      hasBearer: Boolean(req.headers.authorization),
      hasInternalToken: Boolean(internalToken),
      tokenAccepted: expectedToken ? internalToken === expectedToken : Boolean(internalToken),
      hasRequestId: Boolean(req.headers['x-request-id']),
      hasIdempotencyKey: Boolean(req.headers['x-idempotency-key']),
      body: bodyText ? JSON.parse(bodyText) : null,
    };
    requests.push(entry);
    const pairs = entry.body?.pairs;
    const valid = req.method === 'POST'
      && req.url === '/internal/blocks/check'
      && !entry.hasBearer
      && entry.hasInternalToken
      && entry.tokenAccepted
      && entry.hasRequestId
      && entry.hasIdempotencyKey
      && Array.isArray(pairs)
      && pairs.length === 2;
    if (!valid) {
      res.writeHead(400, { 'content-type': 'application/json' });
      res.end(JSON.stringify({ code: 'STUB_CONTRACT_VIOLATION' }));
      return;
    }
    res.writeHead(200, { 'content-type': 'application/json' });
    res.end(JSON.stringify({ code: 0, data: pairs.map((pair) => ({
      blockerId: pair.blockerId,
      blockedId: pair.blockedId,
      blocked: false,
    })) }));
  });
});
server.listen(port, '0.0.0.0', () => console.log(JSON.stringify({ ready: true, port, contract: 'POST /internal/blocks/check with service token' })));
process.on('SIGTERM', () => { console.log(JSON.stringify({ requests })); server.close(() => process.exit(0)); });
process.on('SIGINT', () => { console.log(JSON.stringify({ requests })); server.close(() => process.exit(0)); });
