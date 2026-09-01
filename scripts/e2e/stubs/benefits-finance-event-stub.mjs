import { createServer } from "node:http";

const port = Number(process.env.FINANCE_EVENT_STUB_PORT || 18087);
const host = process.env.FINANCE_EVENT_STUB_HOST || "127.0.0.1";
const expectedToken = process.env.FINANCE_INTERNAL_SERVICE_TOKEN ||
  "benefits-finance-acceptance-internal-token";
const received = [];

const readJson = async (request) => {
  const chunks = [];
  for await (const chunk of request) chunks.push(chunk);
  return chunks.length ? JSON.parse(Buffer.concat(chunks).toString("utf8")) : {};
};
const json = (response, status, body) => {
  response.writeHead(status, { "content-type": "application/json" });
  response.end(JSON.stringify(body));
};

const server = createServer(async (request, response) => {
  const url = new URL(request.url, `http://${request.headers.host}`);
  try {
    if (request.method === "GET" && url.pathname === "/health") {
      return json(response, 200, { status: "UP" });
    }
    if (request.method === "GET" && url.pathname === "/__received") {
      return json(response, 200, received);
    }
    if (request.method !== "POST" || url.pathname !== "/internal/events") {
      return json(response, 404, { code: "UNSTUBBED_ROUTE", method: request.method, path: url.pathname });
    }
    if (request.headers["x-internal-service-token"] !== expectedToken) {
      return json(response, 403, { code: "SERVICE_IDENTITY_FORBIDDEN" });
    }
    const eventId = request.headers["x-event-id"];
    const eventType = request.headers["x-event-type"];
    if (typeof eventId !== "string" || !eventId || typeof eventType !== "string" || !eventType) {
      return json(response, 400, { code: "EVENT_HEADERS_REQUIRED" });
    }
    const body = await readJson(request);
    if (body.eventId !== eventId || body.eventType !== eventType || body.eventVersion !== 1 ||
      body.producer !== "benefits-finance-service" || body.aggregateType !== "PAYMENT" ||
      typeof body.aggregateId !== "string" || !body.aggregateId || typeof body.traceId !== "string" || !body.traceId ||
      typeof body.occurredAt !== "string" || !body.payload || typeof body.payload !== "object") {
      return json(response, 400, { code: "EVENT_BODY_INVALID" });
    }
    const payload = body.payload;
    if (request.headers["x-trace-id"] !== body.traceId || request.headers["x-request-id"] !== payload.requestId) {
      return json(response, 400, { code: "EVENT_TRACE_HEADERS_INVALID" });
    }
    if (typeof payload.requestId !== "string" || !payload.requestId || typeof payload.transactionId !== "string" ||
      !payload.transactionId || !Number.isSafeInteger(Number(payload.recipientUserId)) ||
      typeof payload.displayTitle !== "string" || !payload.displayTitle || typeof payload.displayText !== "string" ||
      !payload.displayText || typeof payload.dedupeKey !== "string" || !payload.dedupeKey) {
      return json(response, 400, { code: "EVENT_NOTIFICATION_PAYLOAD_INVALID" });
    }
    if (!Number.isSafeInteger(Number(payload.orderId))) {
      return json(response, 400, { code: "EVENT_ORDER_ID_INVALID" });
    }
    received.push({ eventId, eventType, body });
    response.writeHead(204);
    return response.end();
  } catch (error) {
    return json(response, 400, { code: "STUB_VALIDATION_FAILED", message: String(error) });
  }
});

server.listen(port, host, () => {
  console.log(`benefits-finance event contract stub listening on ${host}:${port}`);
});
