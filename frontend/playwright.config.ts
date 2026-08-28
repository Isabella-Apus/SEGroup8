import { defineConfig, devices } from "@playwright/test";
import { fileURLToPath } from "node:url";
import { resolve } from "node:path";

const frontendRoot = fileURLToPath(new URL(".", import.meta.url));
const repositoryRoot = resolve(frontendRoot, "..");
const defaultEvidenceRoot = resolve(
    repositoryRoot,
    "04_tests",
    "platform-e2e",
    "evidence",
);
const evidenceRoot = resolve(
    process.env.E2E_OUTPUT_DIR || defaultEvidenceRoot,
);
const isCi = process.env.CI === "true";

export default defineConfig({
    testDir: resolve(frontendRoot, "e2e"),
    testMatch: "**/*.spec.ts",
    timeout: 30_000,
    expect: {
        timeout: 10_000,
    },
    fullyParallel: true,
    forbidOnly: isCi,
    retries: isCi ? 2 : 0,
    workers: isCi ? 1 : undefined,
    outputDir: resolve(evidenceRoot, "test-results"),
    reporter: [
        ["list"],
        ["html", {
            outputFolder: resolve(evidenceRoot, "playwright-report"),
            open: "never",
        }],
        ["json", {
            outputFile: resolve(evidenceRoot, "playwright-results.json"),
        }],
        ["junit", {
            outputFile: resolve(evidenceRoot, "playwright-results.xml"),
        }],
    ],
    use: {
        baseURL: process.env.E2E_BASE_URL || "http://127.0.0.1:8088",
        headless: true,
        actionTimeout: 10_000,
        navigationTimeout: 30_000,
        trace: "retain-on-failure",
        screenshot: "only-on-failure",
        video: "retain-on-failure",
        ...devices["Desktop Chrome"],
        ...(process.env.E2E_BROWSER_CHANNEL
            ? { channel: process.env.E2E_BROWSER_CHANNEL }
            : {}),
    },
});
