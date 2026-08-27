import { existsSync, readFileSync, readdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(scriptDir, "../../..");
const suiteArgument = process.argv.find((value) => value.startsWith("--suite="));
const suite = suiteArgument?.slice("--suite=".length) || "domains/C-order-fulfillment";
if (!/^(domains\/C-order-fulfillment|UC1[1-5])$/.test(suite)) {
  throw new Error(`Unsupported Playwright evidence suite: ${suite}`);
}

const evidenceRoot = path.join(repositoryRoot, "04_tests", suite, "evidence");
const playwrightRoot = path.join(evidenceRoot, "raw-reports", "playwright");
const resultsPath = path.join(playwrightRoot, "playwright-results.json");
const reportPath = path.join(playwrightRoot, "playwright-report", "index.html");
const screenshotsRoot = path.join(evidenceRoot, "screenshots");

function collectSpecs(suites) {
  return suites.flatMap((entry) => [
    ...(entry.specs ?? []),
    ...collectSpecs(entry.suites ?? []),
  ]);
}

const screenshotFiles = existsSync(screenshotsRoot)
  ? readdirSync(screenshotsRoot).filter((name) => name.endsWith(".png"))
  : [];
const errors = [];
if (!existsSync(resultsPath)) errors.push("missing Playwright playwright-results.json");
if (!existsSync(reportPath)) errors.push("missing Playwright HTML report");
if (screenshotFiles.length === 0) errors.push("missing browser screenshots");

let expected = 0;
if (existsSync(resultsPath)) {
  const results = JSON.parse(readFileSync(resultsPath, "utf8"));
  expected = results.stats?.expected ?? 0;
  if (expected <= 0) errors.push("Playwright report has zero passed tests");
  if ((results.stats?.unexpected ?? 0) !== 0) {
    errors.push(`Playwright report has ${results.stats.unexpected} unexpected result(s)`);
  }
  if ((results.stats?.flaky ?? 0) !== 0) {
    errors.push(`Playwright report has ${results.stats.flaky} flaky result(s)`);
  }
  if ((results.stats?.skipped ?? 0) !== 0) {
    errors.push(`Playwright report has ${results.stats.skipped} skipped result(s)`);
  }

  const specs = collectSpecs(results.suites ?? []);
  const requiredTags = suite.startsWith("UC") ? ["DOMAIN_C", suite] : ["DOMAIN_C"];
  if (specs.length === 0) errors.push("Playwright report contains no specs");
  for (const spec of specs) {
    for (const tag of requiredTags) {
      if (!(spec.tags ?? []).includes(tag)) {
        errors.push(`Playwright spec '${spec.title}' is missing @${tag}`);
      }
    }
  }

  const attachments = specs.flatMap((spec) =>
    (spec.tests ?? []).flatMap((test) =>
      (test.results ?? []).flatMap((result) => result.attachments ?? []),
    ),
  );
  if (!attachments.some((attachment) => attachment.contentType === "image/png")) {
    errors.push("Playwright report has no attached success screenshot");
  }
}

if (errors.length > 0) {
  console.error(`Playwright evidence validation failed: ${errors.join("; ")}`);
  process.exit(1);
}

console.log(
  `Playwright evidence valid for ${suite}: ${expected} passed test(s), ${screenshotFiles.length} screenshot(s)`,
);
