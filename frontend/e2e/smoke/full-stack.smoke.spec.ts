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

        expect(productResponse.url()).toContain("/api/product/list");
        expect(productResponse.ok()).toBeTruthy();
        await expect(
            page.getByRole("article").filter({
                hasText: "Container Demo Keyboard",
            }),
        ).toBeVisible();
    });
});
