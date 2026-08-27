import { test as base, expect } from "@playwright/test";
import {
    getTestAccount,
    login,
    type TestAccount,
    type TestRole,
} from "../helpers/auth";

type PlatformFixtures = {
    testAccount: TestAccount;
    loginAs: (role?: TestRole) => Promise<TestAccount>;
};

export const test = base.extend<PlatformFixtures>({
    testAccount: async ({}, use) => {
        const role = (process.env.E2E_ROLE || "USER") as TestRole;
        await use(getTestAccount(role));
    },
    loginAs: async ({ page }, use) => {
        await use(async (role: TestRole = "USER") => {
            const account = getTestAccount(role);
            await login(page, account);
            return account;
        });
    },
});

export { expect };
