import { spawn } from "node:child_process";
import {
  copyFileSync,
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  rmSync,
  writeFileSync,
} from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(scriptDir, "../../..");
const backendRoot = path.join(repositoryRoot, "backend");
const testsRoot = path.join(repositoryRoot, "04_tests");

function readOption(name, fallback = "") {
  const exactIndex = process.argv.indexOf(`--${name}`);
  if (exactIndex >= 0) {
    return process.argv[exactIndex + 1] ?? fallback;
  }
  const prefix = `--${name}=`;
  const inline = process.argv.find((argument) => argument.startsWith(prefix));
  return inline ? inline.slice(prefix.length) : fallback;
}

function hasFlag(name) {
  return process.argv.includes(`--${name}`);
}

function sanitizeSuite(value) {
  const suite = String(value || "DOMAIN_C").toUpperCase();
  if (!/^(DOMAIN_C|PLATFORM|UC1[1-5])$/.test(suite)) {
    throw new Error(`Unsupported Domain-C suite: ${value}`);
  }
  return suite;
}

function tagExpression(suite) {
  if (suite === "DOMAIN_C") {
    return "DOMAIN_C";
  }
  return `DOMAIN_C & ${suite}`;
}

function defaultEvidenceRoot(suite) {
  if (suite.startsWith("UC")) {
    return path.join(repositoryRoot, "04_tests", suite, "evidence");
  }
  return path.join(scriptDir, "evidence");
}

function requireEvidencePath(candidate) {
  const relativePath = path.relative(testsRoot, candidate);
  if (
    !relativePath ||
    relativePath.startsWith("..") ||
    path.isAbsolute(relativePath)
  ) {
    throw new Error("Evidence directory must be a child of 04_tests");
  }
  return candidate;
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

function windowsCommandLine(command, args) {
  const quote = (value) => `"${String(value).replaceAll('"', '""')}"`;
  return [command, ...args.map(quote)].join(" ");
}

function run(command, args, cwd, logPath) {
  return new Promise((resolve) => {
    const logChunks = [];
    let child;
    try {
      child = process.platform === "win32"
        ? spawn(windowsCommandLine(command, args), {
            cwd,
            env: process.env,
            shell: true,
          })
        : spawn(command, args, {
            cwd,
            env: process.env,
            shell: false,
          });
    } catch (error) {
      const message = Buffer.from(`Launcher error: ${error.message}\n`);
      writeFileSync(logPath, message);
      process.stderr.write(message);
      resolve(1);
      return;
    }

    const record = (chunk, stream) => {
      logChunks.push(Buffer.from(chunk));
      stream.write(chunk);
    };
    child.stdout.on("data", (chunk) => record(chunk, process.stdout));
    child.stderr.on("data", (chunk) => record(chunk, process.stderr));
    child.on("error", (error) => {
      const message = Buffer.from(`\nLauncher error: ${error.message}\n`);
      logChunks.push(message);
      process.stderr.write(message);
      writeFileSync(logPath, Buffer.concat(logChunks));
      resolve(1);
    });
    child.on("close", (code) => {
      writeFileSync(logPath, Buffer.concat(logChunks));
      resolve(code ?? 1);
    });
  });
}

const suite = sanitizeSuite(readOption("suite", "DOMAIN_C"));
const goal = readOption("goal", "verify");
if (!/^(test|verify)$/.test(goal)) {
  throw new Error(`Unsupported Maven goal: ${goal}`);
}

const configuredEvidenceRoot = readOption("evidence-dir");
const evidenceRoot = requireEvidencePath(
  configuredEvidenceRoot
    ? path.resolve(repositoryRoot, configuredEvidenceRoot)
    : defaultEvidenceRoot(suite),
);
const logsRoot = path.join(evidenceRoot, "logs");
const rawReportsRoot = path.join(evidenceRoot, "raw-reports");
const surefireReportsRoot = path.join(rawReportsRoot, "surefire");
const screenshotsRoot = path.join(evidenceRoot, "screenshots");
const logPath = path.join(logsRoot, "backend-domain-c.log");
const summaryPath = path.join(evidenceRoot, "result-summary.json");
const sourceReportsRoot = path.join(backendRoot, "target", "surefire-reports");

rmSync(logsRoot, { recursive: true, force: true });
rmSync(surefireReportsRoot, { recursive: true, force: true });
rmSync(summaryPath, { force: true });
mkdirSync(logsRoot, { recursive: true });
mkdirSync(surefireReportsRoot, { recursive: true });
mkdirSync(screenshotsRoot, { recursive: true });

const runId = new Date().toISOString().replace(/[-:.TZ]/g, "");
const reportSuffix = `domain-c-${suite.toLowerCase()}-${runId}`;
const mavenExecutable = process.platform === "win32" ? "mvn.cmd" : "mvn";
const mavenArgs = ["-B", "--no-transfer-progress"];
const mavenRepository = readOption("maven-repository");
if (mavenRepository) {
  mavenArgs.push(`-Dmaven.repo.local=${path.resolve(mavenRepository)}`);
}
const testClasses = readOption("test-classes");
if (testClasses) {
  mavenArgs.push(`-Dtest=${testClasses}`);
}
mavenArgs.push(
  "-Pdomain-c",
  `-Ddomain.c.tags=${tagExpression(suite)}`,
  `-Dsurefire.reportNameSuffix=${reportSuffix}`,
);
if (!hasFlag("no-clean")) {
  mavenArgs.push("clean");
}
mavenArgs.push(goal);

const startedAt = new Date();
const mavenExitCode = await run(mavenExecutable, mavenArgs, backendRoot, logPath);

const reportFiles = [];
if (existsSync(sourceReportsRoot)) {
  for (const fileName of readdirSync(sourceReportsRoot)) {
    if (!fileName.includes(reportSuffix) || !/\.(xml|txt)$/.test(fileName)) {
      continue;
    }
    copyFileSync(
      path.join(sourceReportsRoot, fileName),
      path.join(surefireReportsRoot, fileName),
    );
    reportFiles.push(fileName);
  }
}

const totals = { tests: 0, failures: 0, errors: 0, skipped: 0 };
for (const fileName of reportFiles.filter((name) => name.endsWith(".xml"))) {
  const values = parseSuiteAttributes(
    readFileSync(path.join(surefireReportsRoot, fileName), "utf8"),
  );
  for (const key of Object.keys(totals)) {
    totals[key] += values[key];
  }
}

const passed =
  mavenExitCode === 0 &&
  reportFiles.some((name) => name.endsWith(".xml")) &&
  totals.tests > 0 &&
  totals.failures === 0 &&
  totals.errors === 0;
const finishedAt = new Date();
const commandForSummary = [mavenExecutable, ...mavenArgs].map((argument) =>
  String(argument).startsWith("-Dmaven.repo.local=")
    ? "-Dmaven.repo.local=<redacted>"
    : argument,
);
const summary = {
  evidenceFormatVersion: 1,
  suite,
  tagExpression: tagExpression(suite),
  reportSuffix,
  result: passed ? "PASS" : "FAIL",
  startedAt: startedAt.toISOString(),
  finishedAt: finishedAt.toISOString(),
  durationSeconds: Number(((finishedAt - startedAt) / 1000).toFixed(3)),
  command: commandForSummary,
  mavenExitCode,
  reportFiles: reportFiles.length,
  ...totals,
};
writeFileSync(summaryPath, `${JSON.stringify(summary, null, 2)}\n`, "utf8");

console.log(`Domain-C evidence: ${path.relative(repositoryRoot, evidenceRoot)}`);
console.log(
  `Tests=${totals.tests}, Failures=${totals.failures}, Errors=${totals.errors}, Skipped=${totals.skipped}`,
);
console.log(`Domain-C suite ${suite}: ${summary.result}`);
process.exitCode = passed ? 0 : 1;
