import { expect, test } from "../fixtures";
import { accounts, apiToken, bearer, expectBusinessSuccess, login } from "./support";

test.describe("@DOMAIN_B @UC08 real shop persistence and authorization flow", () => {
  test("publishes decoration, reloads it and confirms the persisted JSON", async ({ page, request }) => {
    const token = await apiToken(request, accounts.seller);
    await expectBusinessSuccess(await request.put("/api/shop/seller/decoration", {
      headers: bearer(token),
      data: { decorationJson: JSON.stringify({ components: [] }) },
    }));
    await login(page, accounts.seller);
    await page.goto("/merchant/shop-decoration");
    await page.locator(".component-item", { hasText: "Banner 图片" }).click();
    await expect(page.getByText(/已添加 1 个组件/)).toBeVisible();
    await page.getByRole("button", { name: /保存发布/ }).click();
    await expect(page.getByText("店铺装修已保存发布！")).toBeVisible();

    await page.reload();
    await expect(page.getByText(/已添加 1 个组件/)).toBeVisible();
    const shop = await expectBusinessSuccess(await request.get("/api/shop/seller/current", { headers: bearer(token) }));
    const decoration = JSON.parse(shop.decorationJson);
    expect(decoration.components).toHaveLength(1);
    expect(decoration.components[0].type).toBe("banner");
  });

  test("keeps shop maintenance inaccessible to a different role", async ({ page, request }) => {
    await login(page, accounts.user);
    await page.goto("/merchant/shop-decoration");
    await expect(page).toHaveURL(/\/$/);
    const token = await apiToken(request, accounts.user);
    const denied = await request.put("/api/shop/seller/decoration", {
      headers: bearer(token), data: { decorationJson: "{}" },
    });
    expect((await denied.json()).code).toBe(403);
  });
});
