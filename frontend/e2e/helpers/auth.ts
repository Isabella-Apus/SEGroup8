import type { Page } from "@playwright/test";

export type TestRole =
    | "USER"
    | "ADMIN"
    | "OFFICIAL_SELLER"
    | "SELLER"
    | "BUYER"
    | "THIRD_PARTY";

export interface TestAccount {
    username: string;
    password: string;
    role: TestRole;
}

function envKey(role: TestRole, field: "USERNAME" | "PASSWORD"): string {
    return "E2E_" + role + "_" + field;
}

/**
 * Resolve credentials from the process environment. The default USER account
 * is the non-production account created by docker/mysql/02-seed.sql.
 */
export function getTestAccount(role: TestRole = "USER"): TestAccount {
    const username =
        process.env[envKey(role, "USERNAME")] ||
        (role === "USER" ? process.env.E2E_USERNAME : undefined);
    const password =
        process.env[envKey(role, "PASSWORD")] ||
        (role === "USER" ? process.env.E2E_PASSWORD : undefined);

    if (!username || !password) {
        throw new Error(
            "[E2E] Missing credentials for " + role +
            ". Set " + envKey(role, "USERNAME") + " and " +
            envKey(role, "PASSWORD") +
            " (or E2E_USERNAME/E2E_PASSWORD for USER).",
        );
    }

    return { username, password, role };
}

export async function login(page: Page, account: TestAccount): Promise<void> {
    await page.goto("/login");
    await fillLoginField(page, "login-username", account.username, 0);
    await fillLoginField(page, "login-password", account.password, 1);
    await Promise.all([
        page.waitForURL((url) => url.pathname !== "/login", {
            timeout: 15_000,
        }),
        page.getByTestId("login-submit").click(),
    ]);
}

async function fillLoginField(
    page: Page,
    testId: string,
    value: string,
    fallbackIndex: number,
): Promise<void> {
    const target = page.getByTestId(testId);
    if (await target.count()) {
        const nestedInput = target.locator("input");
        if (await nestedInput.count()) {
            await nestedInput.fill(value);
            return;
        }
        await target.fill(value);
        return;
    }
    await page.getByRole("textbox").nth(fallbackIndex).fill(value);
}

export async function logout(page: Page): Promise<void> {
    await page.context().clearCookies();
    await page.goto("/login");
    await page.evaluate(() => {
        window.localStorage.clear();
        window.sessionStorage.clear();
    });
}
