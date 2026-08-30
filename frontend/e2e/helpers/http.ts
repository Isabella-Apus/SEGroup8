import { expect, type APIRequestContext, type APIResponse, type Page } from "@playwright/test";

export async function waitForApiResponse(
    page: Page,
    urlPart: string,
    status = 200,
    method = "GET",
) {
    return page.waitForResponse(
        (response) =>
            response.url().includes(urlPart) &&
            response.request().method() === method &&
            response.status() === status,
        { timeout: 20_000 },
    );
}

export async function assertHttpHealth(
    request: APIRequestContext,
    url: string,
): Promise<void> {
    const response = await request.get(url, { timeout: 10_000 });
    expect(response.ok(), "Health endpoint was not ready: " + url).toBeTruthy();
}

export async function assertNoVisibleError(page: Page): Promise<void> {
    await expect(page.locator(".el-message--error")).toHaveCount(0);
}

/**
 * The migrated order-service replays a completed receive request as the same
 * successful result. The legacy monolith predates the idempotency contract and
 * reports its already-completed state as business code 400. Shared UC specs run
 * against both deployments, so accept only these two explicitly verified
 * outcomes; any other error still fails the test.
 */
export async function assertIdempotentReceiveReplay(
    response: APIResponse,
    expectedOrderId: number,
): Promise<void> {
    expect(response.ok()).toBeTruthy();
    const payload = await response.json();
    if (Number(payload?.code) === 0) {
        expect(Number(payload?.data?.id)).toBe(expectedOrderId);
        expect(Number(payload?.data?.orderStatus)).toBe(3);
        return;
    }

    expect(payload?.code).toBe(400);
    expect(String(payload?.message || "")).toMatch(/仅待收货订单可确认收货|已确认收货/);
}
