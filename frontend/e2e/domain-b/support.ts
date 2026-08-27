import { expect, type APIRequestContext, type Page } from "@playwright/test";

export const accounts = {
  admin: { username: "admin", password: "admin123" },
  seller: { username: "seller", password: "seller123" },
  user: { username: "user", password: "user123" },
} as const;

export async function login(page: Page, account: { username: string; password: string }) {
  await page.goto("/login");
  await page.getByPlaceholder("请输入账号").fill(account.username);
  await page.getByPlaceholder("请输入密码").fill(account.password);
  await page.getByRole("button", { name: "登录", exact: true }).click();
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/);
}

export async function apiToken(request: APIRequestContext, account: { username: string; password: string }) {
  const response = await request.post("/api/auth/login", { data: account });
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  expect(body.code).toBe(0);
  return String(body.data.token);
}

export function bearer(token: string) {
  return { Authorization: `Bearer ${token}` };
}

export async function expectBusinessSuccess(response: { ok(): boolean; json(): Promise<any> }) {
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  expect(body.code).toBe(0);
  return body.data;
}

export function uniqueName(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 10_000)}`;
}
