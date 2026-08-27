import { expect, test } from "@playwright/test";
import { accounts, apiToken, bearer, expectBusinessSuccess, login } from "./support";

test.describe("UC10 real behavior history flow", () => {
  test("records browsing and search, views history/hot words, deletes history and persists the deletion", async ({ page, request }) => {
    await login(page, accounts.user);
    await page.goto("/product/1");
    await expect(page.getByRole("heading", { name: "Container Demo Keyboard" })).toBeVisible();
    await page.goto("/product");
    await page.getByPlaceholder("搜索商品名、描述").fill("Container Demo Keyboard");
    await page.getByRole("main").getByRole("button", { name: "搜索", exact: true }).click();

    const token = await apiToken(request, accounts.user);
    const searchHistory = await expectBusinessSuccess(await request.get("/api/search/history", { headers: bearer(token) }));
    expect(searchHistory).toContain("Container Demo Keyboard");
    const hot = await expectBusinessSuccess(await request.get("/api/search/hot", { headers: bearer(token) }));
    expect(hot.some((item: any) => item.keyword === "Container Demo Keyboard")).toBeTruthy();

    await page.goto("/browse-history");
    await expect(page.getByText("Container Demo Keyboard", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "管理" }).click();
    await page.locator(".card-check").click();
    await expect(page.getByRole("button", { name: "删除", exact: true })).toBeEnabled();
    await page.getByRole("button", { name: "删除", exact: true }).click();
    await page.getByRole("dialog", { name: "提示" }).getByRole("button", { name: "OK" }).click();
    await expect(page.getByText("删除成功")).toBeVisible();
    await page.reload();
    await expect(page.getByText("暂无浏览记录，去逛逛吧")).toBeVisible();
    const persisted = await expectBusinessSuccess(await request.get("/api/user/browse-history", { headers: bearer(token) }));
    expect(persisted).toHaveLength(0);
  });

  test("isolates another user's history and requires authentication", async ({ request }) => {
    const userToken = await apiToken(request, accounts.user);
    const sellerToken = await apiToken(request, accounts.seller);
    await expectBusinessSuccess(await request.post("/api/user/browse-history", {
      headers: bearer(userToken), data: { productId: 1, productType: "NEW" },
    }));
    const sellerHistory = await expectBusinessSuccess(await request.get("/api/user/browse-history", { headers: bearer(sellerToken) }));
    expect(sellerHistory).toHaveLength(0);
    const anonymous = await request.get("/api/user/browse-history");
    expect((await anonymous.json()).code).toBe(401);
  });
});
