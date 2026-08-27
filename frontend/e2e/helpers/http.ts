import { expect, type APIRequestContext, type Page } from "@playwright/test";

export async function waitForApiResponse(
    page: Page,
    urlPart: string,
    status = 200,
) {
    return page.waitForResponse(
        (response) =>
            response.url().includes(urlPart) &&
            response.request().method() === "GET" &&
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
