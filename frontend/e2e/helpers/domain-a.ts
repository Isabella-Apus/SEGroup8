import type { APIRequestContext, APIResponse } from "@playwright/test";

export interface DomainAccount {
    username: string;
    password: string;
    role: string;
    id?: number;
    token?: string;
}

export function uniqueAccount(prefix: string): DomainAccount {
    const suffix = `${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
    return {
        username: `${prefix}-${suffix}`.slice(0, 50),
        password: "secret123",
        role: "USER",
    };
}

export function adminAccount(): DomainAccount {
    return {
        username: process.env.E2E_ADMIN_USERNAME || "admin",
        password: process.env.E2E_ADMIN_PASSWORD || "admin123",
        role: "ADMIN",
    };
}

export async function json(response: APIResponse): Promise<any> {
    const body = await response.json();
    if (typeof body?.code !== "number") {
        throw new Error(`[E2E] unexpected API response: ${JSON.stringify(body)}`);
    }
    return body;
}

export async function registerAccount(
    request: APIRequestContext,
    account: DomainAccount,
): Promise<DomainAccount> {
    const response = await request.post("/api/auth/register", {
        data: {
            username: account.username,
            password: account.password,
            nickname: account.username,
            phone: "13800138000",
            email: `${account.username}@example.com`,
        },
    });
    const body = await json(response);
    if (body.code !== 0) {
        throw new Error(`[E2E] registration failed: ${JSON.stringify(body)}`);
    }
    return account;
}

export async function loginApi(
    request: APIRequestContext,
    account: DomainAccount,
): Promise<{ account: DomainAccount; body: any }> {
    const response = await request.post("/api/auth/login", {
        data: { username: account.username, password: account.password },
    });
    const body = await json(response);
    return {
        account: { ...account, id: body.data?.user?.id, token: body.data?.token },
        body,
    };
}

export function authHeaders(token: string) {
    return { Authorization: `Bearer ${token}` };
}

export async function apiGet(request: APIRequestContext, path: string, token: string) {
    return json(await request.get(path, { headers: authHeaders(token) }));
}

export async function apiPost(
    request: APIRequestContext,
    path: string,
    token: string,
    data?: unknown,
) {
    return json(await request.post(path, { headers: authHeaders(token), data }));
}

export async function apiPut(
    request: APIRequestContext,
    path: string,
    token: string,
    data?: unknown,
) {
    return json(await request.put(path, { headers: authHeaders(token), data }));
}

export async function apiDelete(request: APIRequestContext, path: string, token: string) {
    return json(await request.delete(path, { headers: authHeaders(token) }));
}
