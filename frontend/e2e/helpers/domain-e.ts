import { mkdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import {
    expect,
    type APIRequestContext,
    type APIResponse,
    type Page,
    type TestInfo,
} from "@playwright/test";
import { getTestAccount, login, type TestRole } from "./auth";

const supportRoot = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(supportRoot, "../../..");

export async function loginAsDomainE(page: Page, role: TestRole): Promise<void> {
    await login(page, getTestAccount(role));
}

export async function domainEToken(
    request: APIRequestContext,
    role: TestRole,
): Promise<string> {
    const account = getTestAccount(role);
    const response = await request.post("/api/auth/login", {
        data: { username: account.username, password: account.password },
    });
    expect(response.ok(), `${role} login must reach the real backend`).toBeTruthy();
    const payload = await response.json();
    expect(payload?.code).toBe(0);
    expect(payload?.data?.token).toBeTruthy();
    return String(payload.data.token);
}

export function bearer(token: string): Record<string, string> {
    return { Authorization: `Bearer ${token}` };
}

export async function expectBusinessSuccess<T = any>(
    response: APIResponse,
): Promise<T> {
    expect(response.ok()).toBeTruthy();
    const payload = await response.json();
    expect(payload?.code).toBe(0);
    return payload.data as T;
}

export async function expectBusinessFailure(response: APIResponse): Promise<any> {
    expect(response.ok()).toBeTruthy();
    const payload = await response.json();
    expect(Number(payload?.code)).not.toBe(0);
    return payload;
}

export function uniqueName(prefix: string): string {
    return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 10_000)}`;
}

export async function captureDomainEEvidence(
    page: Page,
    testInfo: TestInfo,
    uc: "UC21" | "UC22" | "UC23" | "UC24" | "UC25",
    name: string,
): Promise<string> {
    if (!/^[a-z0-9][a-z0-9-]*$/.test(name)) {
        throw new Error(`[E2E] Invalid Domain-E evidence name: ${name}`);
    }
    const target = path.join(
        repositoryRoot,
        "04_tests",
        uc,
        "evidence",
        "screenshots",
        `${name}.png`,
    );
    mkdirSync(path.dirname(target), { recursive: true });
    await page.waitForTimeout(300);
    await page.screenshot({ path: target, fullPage: true, animations: "disabled" });
    await testInfo.attach(name, { path: target, contentType: "image/png" });
    return target;
}
