import { createServer } from "node:http";

const port = Number(process.env.ORDER_STUB_PORT || 18085);
const expectedToken = process.env.INTERNAL_SERVICE_TOKEN || "acceptance-internal-token";
const orders = new Map();
let sequence = 9000;

function send(response, status, payload) {
    response.writeHead(status, { "content-type": "application/json; charset=utf-8" });
    response.end(JSON.stringify(payload));
}

async function readJson(request) {
    const chunks = [];
    for await (const chunk of request) chunks.push(chunk);
    return JSON.parse(Buffer.concat(chunks).toString("utf8") || "{}");
}

const server = createServer(async (request, response) => {
    if (request.headers["x-internal-service-token"] !== expectedToken) {
        send(response, 401, { code: 401, message: "invalid service token", data: null });
        return;
    }

    const address = request.url?.match(/^\/internal\/users\/(\d+)\/addresses\/(\d+)$/);
    const shipping = request.url?.match(/^\/internal\/users\/(\d+)\/shipping-address$/);
    if (request.method === "GET" && (address || shipping)) {
        const userId = Number((address || shipping)[1]);
        send(response, 200, { code: 0, message: "success", data: {
            addressId: address ? Number(address[2]) : 100, userId, receiverName: "Acceptance Buyer",
            receiverPhone: "13800138000", province: "Guangdong", city: "Shenzhen",
            detailAddress: "Nanshan Acceptance Road"
        }});
        return;
    }

    if (request.method === "POST" && request.url === "/internal/orders/secondhand") {
        const body = await readJson(request);
        const key = String(request.headers["idempotency-key"] || body.tradeId || "");
        if (!key) {
            send(response, 400, { error: "missing idempotency key" });
            return;
        }
        if (!orders.has(key)) {
            sequence += 1;
            orders.set(key, { id: sequence, orderNo: `E2E${sequence}`, orderStatus: "PENDING_PAY" });
        }
        send(response, 200, orders.get(key));
        return;
    }

    const lookup = request.url?.match(/^\/internal\/orders\/by-business-key\/(.+)$/);
    if (request.method === "GET" && lookup) {
        const order = orders.get(decodeURIComponent(lookup[1]));
        send(response, order ? 200 : 404, order || { error: "not found" });
        return;
    }

    send(response, 404, { code: 404, message: "not found", data: null });
});

server.listen(port, "0.0.0.0", () => {
    process.stdout.write(`secondhand order contract stub listening on ${port}\n`);
});

for (const signal of ["SIGINT", "SIGTERM"]) {
    process.on(signal, () => server.close(() => process.exit(0)));
}
