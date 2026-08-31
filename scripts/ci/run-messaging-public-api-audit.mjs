import crypto from 'node:crypto';

const base = process.env.MESSAGING_BASE_URL ?? 'http://127.0.0.1:18084';
const jwtSecret = process.env.AUDIT_JWT_SECRET;
const serviceToken = process.env.AUDIT_SERVICE_TOKEN;
const operationsToken = process.env.AUDIT_OPERATIONS_TOKEN;
if (!jwtSecret || !serviceToken || !operationsToken) throw new Error('audit credentials must be injected via environment');

const b64 = value => Buffer.from(value).toString('base64url');
function jwt(uid, username) {
  const header = b64(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = b64(JSON.stringify({ uid, username, role: 'USER', iat: Math.floor(Date.now() / 1000), exp: Math.floor(Date.now() / 1000) + 3600 }));
  const input = `${header}.${payload}`;
  const signature = crypto.createHmac('sha256', jwtSecret).update(input).digest('base64url');
  return `${input}.${signature}`;
}

const buyer = jwt(1001, 'auditbuyer');
const seller = jwt(1002, 'auditseller');
const result = [];
async function call(name, method, path, options = {}) {
  const headers = { ...(options.auth ? { Authorization: `Bearer ${options.auth}` } : {}), ...(options.service ? { 'X-Internal-Service-Token': options.service, 'X-Service-Identity': 'final-acceptance-audit' } : {}), 'X-Trace-Id': `final-audit-${name}`, ...(options.body ? { 'Content-Type': 'application/json' } : {}) };
  const response = await fetch(`${base}${path}`, { method, headers, body: options.body ? JSON.stringify(options.body) : undefined });
  const text = await response.text();
  let body; try { body = JSON.parse(text); } catch { body = {}; }
  result.push({ name, method, path, status: response.status, code: body.code ?? null });
  const expectedStatus = options.expectedStatus ?? 200;
  if (response.status !== expectedStatus) {
    throw new Error(`${name}: expected HTTP ${expectedStatus}, received ${response.status}`);
  }
  return { response, body };
}

await call('chat-list', 'GET', '/api/chat/conversations', { auth: buyer });
await call('chat-create', 'POST', '/api/chat/conversations', { auth: buyer, body: { targetUserId: 1002, sourceType: 'DIRECT', sourceId: 1, sourceTitle: 'Audit conversation' } });
await call('chat-messages', 'GET', '/api/chat/conversations/9001/messages', { auth: buyer });
await call('chat-send', 'POST', '/api/chat/conversations/9001/messages', { auth: buyer, body: { content: 'audit message' } });
await call('notification-list', 'GET', '/api/notifications', { auth: buyer });
await call('notification-read', 'POST', '/api/notifications/9001/read', { auth: buyer });
await call('notification-read-all', 'POST', '/api/notifications/read-all', { auth: buyer });
const eventId = `final-audit-${Date.now()}`;
const eventDedupe = `final-audit-dedupe-${Date.now()}`;
await call('internal-event', 'POST', '/internal/events', { service: serviceToken, body: { eventId, eventType: 'NotificationRequested.v1', eventVersion: 1, producer: 'final-audit', aggregateType: 'audit', aggregateId: eventId, occurredAt: new Date().toISOString(), traceId: 'final-audit-event', payload: { recipientUserId: 1001, title: 'Audit event', content: 'Audit event content', dedupeKey: eventDedupe, notificationType: 'AUDIT', businessType: 'TEST', businessId: eventId, targetPath: '/chat' } } });
const httpDedupe = `final-audit-http-${Date.now()}`;
await call('internal-notification', 'POST', '/internal/notifications', { service: serviceToken, body: { recipientUserId: 1001, title: 'Audit internal', content: 'Audit internal content', notificationType: 'AUDIT', businessType: 'TEST', businessId: eventId, targetPath: '/chat', scope: 'buyer', dedupeKey: httpDedupe, traceId: 'final-audit-http' } });
await call('internal-delivery', 'GET', `/internal/delivery/${httpDedupe}`, { service: serviceToken });
await call('internal-replay', 'POST', `/internal/events/replay/${eventId}?reason=final-audit`, { service: operationsToken });
await call('unauthenticated', 'GET', '/api/chat/conversations', { expectedStatus: 401 });
await call('invalid-jwt', 'GET', '/api/chat/conversations', { auth: 'not-a-jwt', expectedStatus: 401 });
await call('nonparticipant', 'GET', '/api/chat/conversations/9001/messages', { auth: jwt(1003, 'outsider'), expectedStatus: 403 });
await call('invalid-message', 'POST', '/api/chat/conversations/9001/messages', { auth: buyer, body: { content: '' }, expectedStatus: 400 });
await call('missing-notification', 'POST', '/api/notifications/999999/read', { auth: buyer, expectedStatus: 404 });
await call('internal-missing-auth', 'POST', '/internal/notifications', { body: { recipientUserId: 1001, title: 'x', content: 'x', dedupeKey: 'bad', traceId: 'bad' }, expectedStatus: 401 });
await call('replay-wrong-token-type', 'POST', `/internal/events/replay/${eventId}`, { service: serviceToken, expectedStatus: 401 });
console.log(JSON.stringify({ base, results: result }, null, 2));
