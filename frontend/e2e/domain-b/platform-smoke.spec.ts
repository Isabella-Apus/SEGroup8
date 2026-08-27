import { expect, test } from "@playwright/test";

test("real frontend, backend and database are connected", async ({ page, request }) => {
  const loginResponse = await request.post("/api/auth/login", {
    data: { username: "user", password: "user123" },
  });
  expect(loginResponse.ok()).toBeTruthy();
  const body = await loginResponse.json();
  expect(body.code).toBe(0);
  expect(body.data.token).toBeTruthy();

  await page.goto("/login");
  await expect(page).toHaveTitle("Kinda Goods");
  await expect(page.getByRole("heading", { name: "登录 Kinda Goods" })).toBeVisible();
});
