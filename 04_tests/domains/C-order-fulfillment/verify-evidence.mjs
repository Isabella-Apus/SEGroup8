import { existsSync, readFileSync, readdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const suiteArgument = process.argv.find((value) => value.startsWith("--suite="));
const suite = (suiteArgument?.slice("--suite=".length) || "DOMAIN_C").toUpperCase();
if (!/^(DOMAIN_C|PLATFORM|UC1[1-5])$/.test(suite)) {
  throw new Error(`Unsupported Domain-C evidence suite: ${suite}`);
}
const repositoryRoot = path.resolve(scriptDir, "../../..");
const evidenceRoot = suite.startsWith("UC")
  ? path.join(repositoryRoot, "04_tests", suite, "evidence")
  : path.join(scriptDir, "evidence");
const summaryPath = path.join(evidenceRoot, "result-summary.json");
const reportsRoot = path.join(evidenceRoot, "raw-reports", "surefire");
const logPath = path.join(evidenceRoot, "logs", "backend-domain-c.log");

function expectedTagExpression(value) {
  return value === "DOMAIN_C" ? "DOMAIN_C" : `DOMAIN_C & ${value}`;
}

function parseSuiteAttributes(xml) {
  const openingTag = xml.match(/<testsuite\b[^>]*>/)?.[0] ?? "";
  const readNumber = (name) => {
    const match = openingTag.match(new RegExp(`${name}="(\\d+)"`));
    return match ? Number(match[1]) : 0;
  };
  return {
    tests: readNumber("tests"),
    failures: readNumber("failures"),
    errors: readNumber("errors"),
    skipped: readNumber("skipped"),
  };
}

function findReports(root, extension) {
  if (!existsSync(root)) return [];
  return readdirSync(root, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = path.join(root, entry.name);
    if (entry.isDirectory()) return findReports(entryPath, extension);
    return entry.name.endsWith(extension) ? [entryPath] : [];
  });
}

const errors = [];
if (!existsSync(summaryPath)) errors.push("missing result-summary.json");
if (!existsSync(logPath)) errors.push("missing backend-domain-c.log");
const xmlReports = findReports(reportsRoot, ".xml");
const textReports = findReports(reportsRoot, ".txt");
if (xmlReports.length === 0) errors.push("missing Surefire XML reports");

if (existsSync(summaryPath)) {
  const summary = JSON.parse(readFileSync(summaryPath, "utf8"));
  const totals = { tests: 0, failures: 0, errors: 0, skipped: 0 };
  for (const report of xmlReports) {
    const values = parseSuiteAttributes(readFileSync(report, "utf8"));
    for (const key of Object.keys(totals)) totals[key] += values[key];
  }

  if (summary.evidenceFormatVersion !== 1) {
    errors.push(`unsupported evidence format ${summary.evidenceFormatVersion}`);
  }
  if (summary.suite !== suite) errors.push(`summary suite is ${summary.suite}`);
  if (summary.tagExpression !== expectedTagExpression(suite)) {
    errors.push(`summary tag expression is ${summary.tagExpression}`);
  }
  if (summary.result !== "PASS") errors.push(`summary result is ${summary.result}`);
  if (summary.mavenExitCode !== 0) errors.push(`Maven exit code is ${summary.mavenExitCode}`);
  if (!(summary.tests > 0)) errors.push("summary has zero tests");
  if (summary.failures !== 0 || summary.errors !== 0) {
    errors.push("summary contains failures or errors");
  }
  if (!summary.reportSuffix || !/^domain-c-[a-z0-9_]+-\d+$/.test(summary.reportSuffix)) {
    errors.push("summary has invalid report suffix");
  }
  const reportFiles = [...xmlReports, ...textReports];
  if (summary.reportFiles !== reportFiles.length) {
    errors.push(`summary report count is ${summary.reportFiles}, found ${reportFiles.length}`);
  }
  if (xmlReports.length !== textReports.length) {
    errors.push(`Surefire XML/TXT count differs (${xmlReports.length}/${textReports.length})`);
  }
  if (summary.reportSuffix && reportFiles.some((file) => !path.basename(file).includes(summary.reportSuffix))) {
    errors.push("reports do not all belong to the summary run");
  }
  for (const key of Object.keys(totals)) {
    if (summary[key] !== totals[key]) {
      errors.push(`summary ${key} is ${summary[key]}, reports contain ${totals[key]}`);
    }
  }
}

if (errors.length > 0) {
  console.error(`Domain-C evidence validation failed: ${errors.join("; ")}`);
  process.exit(1);
}
console.log(`Domain-C evidence valid for ${suite}: ${xmlReports.length} XML report(s)`);
