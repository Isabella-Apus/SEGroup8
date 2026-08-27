import { expect, test } from "@playwright/test";
import { accounts, apiToken, bearer, expectBusinessSuccess, login, uniqueName } from "./support";

test.describe("UC09 real administrator risk audit flow", () => {
  test("lists and rejects a deterministic high-risk product, then persists the decision", async ({ page, request }) => {
    const sellerToken = await apiToken(request, accounts.seller);
    const sellerProfile = await expectBusinessSuccess(await request.get("/api/user/profile", { headers: bearer(sellerToken) }));
    const categoryTree = await expectBusinessSuccess(await request.get("/api/category/tree?scene=NEW", { headers: bearer(sellerToken) }));
    const categoryId = Number(sellerProfile.categoryId ?? sellerProfile.category);
    const category = categoryTree.find((item: any) => Number(item.id) === categoryId);
    expect(category?.children?.length).toBeGreaterThan(0);
    const productName = uniqueName("高仿正品商品");
    await expectBusinessSuccess(await request.post("/api/product/seller", {
      headers: bearer(sellerToken),
      data: {
        name: productName, description: "短描述", price: 12.5, stock: 1,
        categoryId, subCategoryId: category.children[0].id, images: [], status: 0,
      },
    }));

    await login(page, accounts.admin);
    await page.goto("/admin/product-risk-audits");
    await page.getByPlaceholder("商品名").fill(productName);
    await page.getByRole("button", { name: "查询" }).click();
    const row = page.locator(".el-table__row").filter({ hasText: productName });
    await expect(row).toContainText(/高\s*·\s*90/);
    await row.getByRole("button", { name: "驳回" }).click();
    await page.getByPlaceholder("填写管理员处理意见").fill("E2E 高风险驳回");
    await page.getByRole("button", { name: "确认", exact: true }).click();
    await page.getByRole("dialog", { name: "提示" }).getByRole("button", { name: "OK" }).click();
    await expect(page.getByText("处理成功")).toBeVisible();

    const adminToken = await apiToken(request, accounts.admin);
    const audits = await expectBusinessSuccess(await request.get(`/api/admin/product-risk-audits?keyword=${encodeURIComponent(productName)}&auditStatus=REJECTED`, { headers: bearer(adminToken) }));
    expect(audits.records).toHaveLength(1);
    expect(audits.records[0]).toMatchObject({ productName, auditStatus: "REJECTED" });
  });

  test("rejects a forged ordinary-user administrator request", async ({ request }) => {
    const token = await apiToken(request, accounts.user);
    const denied = await request.get("/api/admin/product-risk-audits", { headers: bearer(token) });
    expect((await denied.json()).code).toBe(403);
  });
});
