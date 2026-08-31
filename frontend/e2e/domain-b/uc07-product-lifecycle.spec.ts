import { expect, test } from "../fixtures";
import path from "node:path";
import { accounts, apiToken, bearer, expectBusinessSuccess, login, uniqueName } from "./support";

test.describe("@DOMAIN_B @UC07 real seller product lifecycle", () => {
  test("creates, edits, shelves, adjusts stock and deletes a product with persistence checks", async ({ page, request }) => {
    const productName = uniqueName("E2E商品");
    await login(page, accounts.seller);
    await page.goto("/merchant/seller-products/edit");

    await page.getByLabel("商品名称").fill(productName);
    await page.getByLabel("商品描述").fill("Domain-B Playwright lifecycle fixture");
    await page.getByRole("spinbutton", { name: /价格/ }).fill("88.50");
    await page.getByRole("spinbutton", { name: /库存/ }).fill("6");
    await page.getByRole("group", { name: /商品分类/ }).locator(".el-select").click();
    await page.locator(".el-select-dropdown:visible .el-select-dropdown__item").first().click();
    await page.locator("input[type=file]").setInputFiles(path.resolve("e2e/domain-b/fixtures/product.svg"));
    await expect(page.getByText("图片上传成功")).toBeVisible();
    await page.getByRole("main").getByRole("button", { name: "发布商品", exact: true }).click();
    await expect(page).toHaveURL(/\/merchant(?:\/seller-products)?(?:\?.*)?$/);

    const row = page.locator(".el-table__row").filter({ hasText: productName });
    await expect(row).toBeVisible();
    await row.getByRole("button", { name: "编辑" }).click();
    const nameInput = page.getByLabel("商品名称");
    await expect(nameInput).toHaveValue(productName);
    await nameInput.fill(`${productName}-edited`);
    await page.getByRole("button", { name: "保存修改" }).click();
    await expect(page.locator(".el-table__row").filter({ hasText: `${productName}-edited` })).toBeVisible();

    const sellerToken = await apiToken(request, accounts.seller);
    const list = await expectBusinessSuccess(await request.get(`/api/product/seller/list?keyword=${encodeURIComponent(productName)}`, { headers: bearer(sellerToken) }));
    const product = list.records.find((item: any) => item.name === `${productName}-edited`);
    expect(product).toBeTruthy();

    const stock = await expectBusinessSuccess(await request.post(`/api/product/seller/${product.id}/stock/adjust`, {
      headers: bearer(sellerToken), data: { delta: 4 },
    }));
    expect(stock.stock).toBe(10);
    const negative = await request.post(`/api/product/seller/${product.id}/stock/adjust`, {
      headers: bearer(sellerToken), data: { delta: -999 },
    });
    expect((await negative.json()).code).not.toBe(0);

    await page.reload();
    const editedRow = page.locator(".el-table__row").filter({ hasText: `${productName}-edited` });
    await editedRow.getByRole("button", { name: "下架" }).click();
    await expect(editedRow).toContainText("已下架");
    await editedRow.getByRole("button", { name: "删除" }).click();
    await page.getByRole("dialog", { name: "提示" }).getByRole("button", { name: "OK" }).click();
    await expect(page.locator(".el-table__row").filter({ hasText: `${productName}-edited` })).toHaveCount(0);
    const gone = await request.get(`/api/product/seller/${product.id}`, { headers: bearer(sellerToken) });
    expect((await gone.json()).code).not.toBe(0);
  });

  test("denies the buyer access to the seller workbench", async ({ page }) => {
    await login(page, accounts.user);
    await page.goto("/merchant/seller-products");
    await expect(page).toHaveURL(/\/$/);
    await expect(page.getByText("无访问权限")).toBeVisible();
  });
});
