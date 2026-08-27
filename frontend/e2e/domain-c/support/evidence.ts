import { mkdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import type { Page, TestInfo } from "@playwright/test";

const supportRoot = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(supportRoot, "../../../..");

export async function captureEvidence(
    page: Page,
    testInfo: TestInfo,
    name: string,
): Promise<string> {
    const suite = process.env.DOMAIN_C_SUITE || "domains/C-order-fulfillment";
    if (!/^(domains\/C-order-fulfillment|UC1[1-5])$/.test(suite)) {
        throw new Error(`[E2E] Unsupported Domain-C evidence suite: ${suite}`);
    }
    if (!/^[a-z0-9][a-z0-9-]*$/.test(name)) {
        throw new Error(`[E2E] Invalid evidence screenshot name: ${name}`);
    }
    const screenshotsRoot = path.join(
        repositoryRoot,
        "04_tests",
        suite,
        "evidence",
        "screenshots",
    );
    mkdirSync(screenshotsRoot, { recursive: true });
    const target = path.join(screenshotsRoot, `${name}.png`);
    await page.waitForTimeout(500);
    await page.screenshot({
        path: target,
        fullPage: true,
        animations: "disabled",
    });
    await testInfo.attach(name, {
        path: target,
        contentType: "image/png",
    });
    return target;
}
