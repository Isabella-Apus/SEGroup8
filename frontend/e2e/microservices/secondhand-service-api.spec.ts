import { createHmac } from "node:crypto";
import { expect, test, type APIRequestContext, type APIResponse } from "@playwright/test";

const secret = "test-jwt-secret-must-have-at-least-thirty-two-bytes";
const internalToken = "acceptance-internal-token";

function token(userId: number, username: string): string {
    const now = Math.floor(Date.now() / 1000);
    const encode = (value: object) => Buffer.from(JSON.stringify(value)).toString("base64url");
    const header = encode({ alg: "HS256", typ: "JWT" });
    const payload = encode({ uid: userId, username, role: "USER", iat: now, exp: now + 3600 });
    const signature = createHmac("sha256", secret).update(`${header}.${payload}`).digest("base64url");
    return `Bearer ${header}.${payload}.${signature}`;
}

async function success(response: APIResponse) {
    expect(response.ok()).toBeTruthy();
    const envelope = await response.json();
    expect(envelope.code).toBe(0);
    return envelope.data;
}

function product(name: string, negotiable = true) {
    return {
        name,
        cover: "/images/acceptance.png",
        images: ["/images/acceptance.png"],
        description: "secondhand-service acceptance flow",
        originPrice: 200,
        salePrice: 80,
        categoryId: 8,
        subCategoryId: 801,
        conditionLevel: "LIKE_NEW",
        isNegotiable: negotiable ? 1 : 0,
        status: 1,
    };
}

async function createApprovedProduct(
    request: APIRequestContext,
    name: string,
    negotiable = true,
): Promise<number> {
    const created = await success(await request.post("/api/secondhand/seller", {
        headers: { Authorization: token(10, "seller") },
        data: product(name, negotiable),
    }));
    await success(await request.post("/internal/events/product-risk-decided", {
        headers: { "X-Internal-Service-Token": internalToken },
        data: { eventId: `risk-${created.id}`, productId: created.id, decision: "APPROVED" },
    }));
    return Number(created.id);
}

test.describe.serial("secondhand-service acceptance", () => {
    test("UC16 product lifecycle reaches the independent service and database", async ({ request }) => {
        const productId = await createApprovedProduct(request, `service-uc16-${Date.now()}`);
        expect((await success(await request.get(`/api/secondhand/detail/${productId}`))).id).toBe(productId);
        expect((await success(await request.get("/api/secondhand/seller-public/10"))).userId).toBe(10);
        expect((await success(await request.get("/api/secondhand/seller-public/10/products"))).total).toBeGreaterThan(0);
        expect((await success(await request.get("/api/secondhand/seller/list", {
            headers: { Authorization: token(10, "seller") },
        }))).total).toBeGreaterThan(0);
        await success(await request.put(`/api/secondhand/seller/${productId}`, {
            headers: { Authorization: token(10, "seller") },
            data: { ...product("service-uc16-updated"), status: 1 },
        }));
        const offShelf = await success(await request.post(`/api/secondhand/seller/${productId}/status`, {
            headers: { Authorization: token(10, "seller") }, data: { status: 2 },
        }));
        expect(offShelf.status).toBe(2);
        await success(await request.delete(`/api/secondhand/seller/${productId}`, {
            headers: { Authorization: token(10, "seller") },
        }));
    });

    test("UC17 direct purchase creates one idempotent order request", async ({ request }) => {
        const productId = await createApprovedProduct(request, `service-uc17-${Date.now()}`);
        const first = await success(await request.post(`/api/secondhand/${productId}/buy`, {
            headers: { Authorization: token(20, "buyer") }, data: { addressId: 100, remark: "acceptance" },
        }));
        const repeated = await success(await request.post(`/api/secondhand/${productId}/buy`, {
            headers: { Authorization: token(20, "buyer") }, data: { addressId: 100, remark: "acceptance" },
        }));
        expect(first.orderBusinessKey).toBe(repeated.orderBusinessKey);
        expect(first.orderId).toBe(repeated.orderId);
    });

    test("UC18 bargain confirmation creates an order through the contract", async ({ request }) => {
        const productId = await createApprovedProduct(request, `service-uc18-${Date.now()}`);
        const negotiation = await success(await request.post("/api/secondhand/trade/bargain/apply", {
            headers: { Authorization: token(20, "buyer") },
            data: { productId, sellerUserId: 10, proposedPrice: 65 },
        }));
        const confirmed = await success(await request.post("/api/secondhand/trade/bargain/confirm", {
            headers: { Authorization: token(10, "seller") },
            data: { negotiationId: negotiation.id, confirmedPrice: 68, createOrder: true },
        }));
        expect(confirmed.status).toBe("ACCEPTED");
        expect(confirmed.orderId).toBeTruthy();
    });

    test("UC19 auction accepts a bid and settles exactly once", async ({ request }) => {
        const productId = await createApprovedProduct(request, `service-uc19-${Date.now()}`, false);
        const auction = await success(await request.post("/api/secondhand/trade/auction", {
            headers: { Authorization: token(10, "seller") },
            data: { productId, startPrice: 50, incrementAmount: 5, durationMinutes: 60 },
        }));
        await success(await request.post(`/api/secondhand/trade/auction/${auction.id}/bid`, {
            headers: { Authorization: token(20, "bidder") }, data: { bidAmount: 50 },
        }));
        const settled = await success(await request.post(`/api/secondhand/trade/auction/${auction.id}/close`, {
            headers: { Authorization: token(10, "seller") },
        }));
        expect(settled.status).toBe("FINISHED");
        expect(settled.settledOrderId).toBeTruthy();
    });
});
