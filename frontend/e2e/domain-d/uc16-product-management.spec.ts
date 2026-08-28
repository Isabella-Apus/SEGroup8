import path from "node:path";
import { fileURLToPath } from "node:url";
import { test, expect } from "../fixtures";
import { assertNoVisibleError } from "../helpers/http";
import {
    apiToken,
    bearer,
    captureEvidence,
    expectBusinessFailure,
    expectBusinessSuccess,
    loginAsDomainD,
    uniqueName,
} from "./support";

const domainRoot = path.dirname(fileURLToPath(import.meta.url));
const productFixture = path.resolve(domainRoot, "../domain-b/fixtures/product.svg");
const responseTimeout = 30_000;

test.describe("@DOMAIN_D @UC16 secondhand product management", () => {
    test.describe.configure({ timeout: 90_000 });

    test("persists publish, edit, shelf changes and delete while rejecting a non-owner", async ({
        page,
        request,
    }, testInfo) => {
        const sellerToken = await apiToken(request, "seller");
        const thirdToken = await apiToken(request, "third");
        await loginAsDomainD(page, "seller");

        const productName = uniqueName("UC16-E2E");
        const editedDescription = `UC16 persisted description ${Date.now()}`;

        const categoriesPromise = page.waitForResponse(
            (response) => response.url().includes("/api/category/tree")
                && response.url().includes("scene=SECONDHAND")
                && response.request().method() === "GET",
            { timeout: responseTimeout },
        );
        await page.goto("/secondhand/publish");
        const categoriesResponse = await categoriesPromise;
        const categoryTree = await expectBusinessSuccess<any[]>(categoriesResponse);
        expect(categoryTree.length).toBeGreaterThan(0);
        const parentCategory = categoryTree.find((item) => item?.children?.length);
        expect(parentCategory, "SECONDHAND category tree needs a real child category").toBeTruthy();
        const childCategory = parentCategory.children[0];

        await page.getByPlaceholder("例如：九成新办公椅").fill(productName);
        await page.getByPlaceholder("请选择二级分类").click();
        await page.locator(".el-cascader-node__label", {
            hasText: String(parentCategory.name),
        }).first().click();
        await page.locator(".el-cascader-node__label", {
            hasText: String(childCategory.name),
        }).last().click();

        const uploadPromise = page.waitForResponse(
            (response) => response.url().includes("/api/upload/image")
                && response.request().method() === "POST",
            { timeout: responseTimeout },
        );
        await page.locator('input[type="file"]').setInputFiles(productFixture);
        await expectBusinessSuccess(await uploadPromise);
        await expect(page.getByText("图片上传成功", { exact: true })).toBeVisible();

        await page.locator(".el-form-item").filter({ hasText: "原价" })
            .getByRole("spinbutton").fill("399");
        await page.locator(".el-form-item").filter({ hasText: "售价" })
            .getByRole("spinbutton").fill("219");
        await page.getByPlaceholder("写清楚使用情况、配件、瑕疵和交易方式")
            .fill("UC16 Compose real-stack product");

        const createPromise = page.waitForResponse(
            (response) => response.url().endsWith("/api/secondhand/seller")
                && response.request().method() === "POST",
            { timeout: responseTimeout },
        );
        await page.getByRole("button", { name: "发布二手", exact: true }).click();
        const created = await expectBusinessSuccess<any>(await createPromise);
        const productId = Number(created.id);
        expect(productId).toBeGreaterThan(0);
        expect(Number(created.categoryId)).toBe(Number(parentCategory.id));
        expect(Number(created.subCategoryId)).toBe(Number(childCategory.id));

        const sellerListPromise = page.waitForResponse(
            (response) => response.url().includes("/api/secondhand/seller/list")
                && response.request().method() === "GET",
            { timeout: responseTimeout },
        );
        await page.goto("/secondhand/mine");
        await sellerListPromise;

        const productRow = () => page.locator(".el-table__row").filter({ hasText: productName });
        await expect(productRow()).toBeVisible();
        await productRow().getByRole("button", { name: "修改描述" }).click();
        const editDialog = page.getByRole("dialog", { name: "修改商品描述" });
        await editDialog.getByRole("textbox").fill(editedDescription);
        const updatePromise = page.waitForResponse(
            (response) => response.url().endsWith(`/api/secondhand/seller/${productId}`)
                && response.request().method() === "PUT",
            { timeout: responseTimeout },
        );
        await editDialog.getByRole("button", { name: "提交修改并重新审核" }).click();
        const updated = await expectBusinessSuccess<any>(await updatePromise);
        expect(updated.description).toBe(editedDescription);

        const sellerHeaders = bearer(sellerToken);
        const persistedAfterEdit = await expectBusinessSuccess<any>(
            await request.get("/api/secondhand/seller/list", {
                headers: sellerHeaders,
                params: { pageNum: 1, pageSize: 20, keyword: productName },
            }),
        );
        expect(persistedAfterEdit.records).toEqual(expect.arrayContaining([
            expect.objectContaining({ id: productId, description: editedDescription }),
        ]));

        const changeStatus = async (buttonName: "上架" | "下架", expectedStatus: number) => {
            const responsePromise = page.waitForResponse(
                (response) => response.url().endsWith(`/api/secondhand/seller/${productId}/status`)
                    && response.request().method() === "POST",
                { timeout: responseTimeout },
            );
            await productRow().getByRole("button", { name: buttonName, exact: true }).click();
            await page.getByRole("dialog", { name: "操作确认" })
                .getByRole("button", { name: "确认", exact: true }).click();
            const changed = await expectBusinessSuccess<any>(await responsePromise);
            expect(Number(changed.status)).toBe(expectedStatus);
            await expect(productRow()).toContainText(expectedStatus === 1 ? "在售" : "下架");
        };

        await changeStatus("下架", 2);
        await changeStatus("上架", 1);
        await page.reload();
        await expect(productRow()).toContainText("在售");
        await changeStatus("下架", 2);
        await captureEvidence(page, testInfo, "uc16-product-persisted-off-shelf");

        const forbiddenDelete = await request.delete(
            `/api/secondhand/seller/${productId}`,
            { headers: bearer(thirdToken) },
        );
        const forbiddenPayload = await expectBusinessFailure(forbiddenDelete);
        expect(String(forbiddenPayload.message || "")).toMatch(/无权|本人/);

        const deletePromise = page.waitForResponse(
            (response) => response.url().endsWith(`/api/secondhand/seller/${productId}`)
                && response.request().method() === "DELETE",
            { timeout: responseTimeout },
        );
        await productRow().getByRole("button", { name: "删除", exact: true }).click();
        await page.getByRole("dialog", { name: "删除确认" })
            .getByRole("button", { name: "确认删除", exact: true }).click();
        await expectBusinessSuccess(await deletePromise);
        await expect(productRow()).toHaveCount(0);

        const persistedAfterDelete = await expectBusinessSuccess<any>(
            await request.get("/api/secondhand/seller/list", {
                headers: sellerHeaders,
                params: { pageNum: 1, pageSize: 20, keyword: productName },
            }),
        );
        expect(persistedAfterDelete.records).toEqual([]);
        await assertNoVisibleError(page);
        await captureEvidence(page, testInfo, "uc16-product-deleted-after-refresh");
    });
});
