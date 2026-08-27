import { test, expect } from "../fixtures";
import { login } from "../helpers/auth";
import { waitForApiResponse } from "../helpers/http";

test.describe("platform full-stack smoke", () => {
    test("logs in and renders a product seeded in MySQL", async ({
        page,
        testAccount,
    }) => {
        await login(page, testAccount);

        const productResponsePromise = waitForApiResponse(
            page,
            "/api/product/list",
        );
        await page.goto("/product");
        const productResponse = await productResponsePromise;
        const payload = await productResponse.json();
        const records = payload?.data?.records || [];

        expect(productResponse.url()).toContain("/api/product/list");
        expect(payload?.code).toBe(0);
        expect(records).toEqual(
            expect.arrayContaining([
                expect.objectContaining({ name: "Container Demo Keyboard" }),
            ]),
        );
        await expect(
            page.getByRole("article").filter({
                hasText: "Container Demo Keyboard",
            }),
        ).toBeVisible();
    });
});
