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
import { login, type TestAccount } from "../helpers/auth";

const supportRoot = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(supportRoot, "../../..");

const seedAccounts: Record<"seller" | "buyer" | "third", TestAccount> = {
    seller: {
        username: process.env.E2E_OFFICIAL_SELLER_USERNAME || "seller",
        password: process.env.E2E_OFFICIAL_SELLER_PASSWORD || "seller123",
        role: "OFFICIAL_SELLER",
    },
    buyer: {
        username: process.env.E2E_BUYER_USERNAME || "user",
        password: process.env.E2E_BUYER_PASSWORD || "user123",
        role: "BUYER",
    },
    third: {
        username: process.env.E2E_THIRD_PARTY_USERNAME || "third",
        password: process.env.E2E_THIRD_PARTY_PASSWORD || "third123",
        role: "THIRD_PARTY",
    },
};

export function domainDAccount(name: keyof typeof seedAccounts): TestAccount {
    return seedAccounts[name];
}

export async function loginAsDomainD(
    page: Page,
    name: keyof typeof seedAccounts,
): Promise<void> {
    await login(page, domainDAccount(name));
}

export async function apiToken(
    request: APIRequestContext,
    name: keyof typeof seedAccounts,
): Promise<string> {
    const account = domainDAccount(name);
    const response = await request.post("/api/auth/login", {
        data: { username: account.username, password: account.password },
    });
    expect(response.ok(), `${name} login must reach the real backend`).toBeTruthy();
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
    const payload = await response.json();
    expect(Number(payload?.code)).not.toBe(0);
    return payload;
}

export function uniqueName(prefix: string): string {
    return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 10_000)}`;
}

export async function captureEvidence(
    page: Page,
    testInfo: TestInfo,
    name: string,
): Promise<string> {
    const suite = process.env.DOMAIN_D_SUITE || "domains/D-secondhand";
    if (!/^(domains\/D-secondhand|UC1[6-9]|UC20)$/.test(suite)) {
        throw new Error(`[E2E] Unsupported Domain-D evidence suite: ${suite}`);
    }
    if (!/^[a-z0-9][a-z0-9-]*$/.test(name)) {
        throw new Error(`[E2E] Invalid evidence screenshot name: ${name}`);
    }

    const target = path.join(
        repositoryRoot,
        "04_tests",
        suite,
        "evidence",
        "screenshots",
        `${name}.png`,
    );
    mkdirSync(path.dirname(target), { recursive: true });
    await page.waitForTimeout(300);
    await page.screenshot({
        path: target,
        fullPage: true,
        animations: "disabled",
    });
    await testInfo.attach(name, { path: target, contentType: "image/png" });
    return target;
}
