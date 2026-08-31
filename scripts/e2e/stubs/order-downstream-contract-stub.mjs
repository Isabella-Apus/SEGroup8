import { createServer } from "node:http";

const port = Number(process.env.ORDER_STUB_PORT || 18086);
const host = process.env.ORDER_STUB_HOST || "127.0.0.1";
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
    if (request.method === "POST" && url.pathname === "/internal/inventory/reservations") {
      const body = await readJson(request);
      return json(response, 200, {
        reservationId: body.reservationId,
        items: (body.items || []).map((item) => ({
          productId: item.productId,
          productName: `Acceptance product ${item.productId}`,
          price: 100.0,
          quantity: item.quantity,
          sellerUserId: 2,
          shopId: 20,
        })),
      });
    }
    if (request.method === "POST" && /^\/internal\/inventory\/reservations\/[^/]+\/(confirm|release)$/.test(url.pathname)) {
      response.writeHead(204);
      return response.end();
    }
    if (request.method === "POST" && url.pathname === "/internal/checkout/quote") {
      const body = await readJson(request);
      return json(response, 200, {
        payableAmount: body.amount,
        discountAmount: 0,
      });
    }
    if (request.method === "POST" && /^\/internal\/(payments\/(debit|refund)|settlements|vouchers\/release)$/.test(url.pathname)) {
      await readJson(request);
      return json(response, 200, { status: "SUCCEEDED" });
    }
    if (request.method === "GET" && /^\/internal\/payments\/[^/]+$/.test(url.pathname)) {
      return json(response, 200, { status: "SUCCEEDED" });
    }
    return json(response, 404, { error: `unstubbed ${request.method} ${url.pathname}` });
  } catch (error) {
    return json(response, 500, { error: String(error) });
  }
});

server.listen(port, host, () => {
  console.log(`order downstream contract stub listening on ${host}:${port}`);
});
