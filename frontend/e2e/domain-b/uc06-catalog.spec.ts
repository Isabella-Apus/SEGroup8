import { expect, test } from "@playwright/test";
import { accounts, expectBusinessSuccess, login } from "./support";

test.describe("UC06 real catalog search flow", () => {
  test("searches, filters, opens matching detail and confirms persisted API fields", async ({ page, request }) => {
    await login(page, accounts.user);
    await page.goto("/product");

    await page.getByPlaceholder("搜索商品名、描述").fill("Container Demo Keyboard");
    await page.getByRole("main").getByRole("button", { name: "搜索", exact: true }).click();
    const card = page.locator(".product-card").filter({ hasText: "Container Demo Keyboard" });
    await expect(card).toHaveCount(1);
    await card.locator(".title-button").click();

    await expect(page.getByRole("heading", { name: "Container Demo Keyboard" })).toBeVisible();
    await expect(page.getByText("¥299.00").first()).toBeVisible();
    const persisted = await expectBusinessSuccess(await request.get("/api/product/detail/1"));
    expect(persisted).toMatchObject({ id: 1, name: "Container Demo Keyboard", stock: 80 });
  });

  test("shows a real empty result and rejects invalid pagination at the API boundary", async ({ page, request }) => {
    await login(page, accounts.user);
    await page.goto("/product");
    await page.getByPlaceholder("搜索商品名、描述").fill(`missing-${Date.now()}`);
    await page.getByRole("main").getByRole("button", { name: "搜索", exact: true }).click();
    await expect(page.getByText("暂无商品")).toBeVisible();

    const invalid = await request.get("/api/product/list?pageNum=0&pageSize=101");
    expect(invalid.ok()).toBeTruthy();
    expect((await invalid.json()).code).not.toBe(0);
  });
});
