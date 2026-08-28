import { appendFile, mkdir, readdir, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, relative, resolve } from "node:path";

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(scriptDirectory, "..", "..");
const e2eRoot = resolve(repositoryRoot, "frontend", "e2e");

const domainRanges = [
    { domain: "A", directory: "domain-a", first: 1, last: 5 },
    { domain: "B", directory: "domain-b", first: 6, last: 10 },
    { domain: "C", directory: "domain-c", first: 11, last: 15 },
    { domain: "D", directory: "domain-d", first: 16, last: 20 },
    { domain: "E", directory: "domain-e", first: 21, last: 25 },
];

const expectedByUc = new Map();
for (const range of domainRanges) {
    for (let number = range.first; number <= range.last; number += 1) {
        const uc = `UC${String(number).padStart(2, "0")}`;
        expectedByUc.set(uc, range);
    }
}

async function listSpecFiles(directory) {
    let entries;
    try {
        entries = await readdir(directory, { withFileTypes: true });
    } catch (error) {
        if (error?.code === "ENOENT") {
            return [];
        }
        throw error;
    }

    const files = [];
    for (const entry of entries) {
        const entryPath = resolve(directory, entry.name);
        if (entry.isDirectory()) {
            files.push(...await listSpecFiles(entryPath));
        } else if (entry.isFile() && entry.name.endsWith(".spec.ts")) {
            files.push(entryPath);
        }
    }
    return files;
}

const coverage = new Map([...expectedByUc.keys()].map((uc) => [uc, []]));
const invalidFiles = [];
const misplacedFiles = [];

for (const range of domainRanges) {
    const domainRoot = resolve(e2eRoot, range.directory);
    for (const file of await listSpecFiles(domainRoot)) {
        const repositoryPath = relative(repositoryRoot, file).replaceAll("\\", "/");
        const match = /^uc(\d{2})-.+\.spec\.ts$/i.exec(file.split(/[\\/]/).at(-1));
        if (!match) {
            continue; // Platform smoke/health specs are intentionally not UC evidence.
        }

        const uc = `UC${match[1]}`;
        const expected = expectedByUc.get(uc);
        if (!expected) {
            invalidFiles.push(repositoryPath);
            continue;
        }
        if (expected.directory !== range.directory) {
            misplacedFiles.push({ uc, expected: expected.directory, actual: range.directory, file: repositoryPath });
            continue;
        }
        coverage.get(uc).push(repositoryPath);
    }
}

const rows = [...expectedByUc.entries()].map(([uc, expected]) => ({
    uc,
    domain: expected.domain,
    expectedDirectory: `frontend/e2e/${expected.directory}`,
    files: coverage.get(uc).sort(),
    status: coverage.get(uc).length > 0 ? "COVERED" : "MISSING",
}));
const missing = rows.filter((row) => row.status === "MISSING").map((row) => row.uc);
const passed = missing.length === 0 && invalidFiles.length === 0 && misplacedFiles.length === 0;

const report = {
    generatedAt: new Date().toISOString(),
    rule: "Each UC01-UC25 must have at least one correctly placed ucXX-*.spec.ts file.",
    passed,
    coveredCount: rows.length - missing.length,
    expectedCount: rows.length,
    missing,
    invalidFiles,
    misplacedFiles,
    rows,
};

console.log("UC E2E coverage manifest");
for (const row of rows) {
    const evidence = row.files.length > 0 ? row.files.join(", ") : "NO SPEC";
    console.log(`${row.status.padEnd(7)} ${row.uc} Domain ${row.domain}: ${evidence}`);
}

if (process.env.UC_E2E_COVERAGE_REPORT) {
    const reportPath = resolve(repositoryRoot, process.env.UC_E2E_COVERAGE_REPORT);
    await mkdir(dirname(reportPath), { recursive: true });
    await writeFile(reportPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
    console.log(`JSON report: ${relative(repositoryRoot, reportPath)}`);
}

if (process.env.GITHUB_STEP_SUMMARY) {
    const summary = [
        "## UC01-UC25 browser E2E coverage",
        "",
        `Result: **${passed ? "PASS" : "FAIL"}** (${report.coveredCount}/${report.expectedCount} UC covered)`,
        "",
        "| UC | Domain | Status | Spec files |",
        "|---|---|---|---|",
        ...rows.map((row) => `| ${row.uc} | ${row.domain} | ${row.status} | ${row.files.join("<br>") || "-"} |`),
        "",
    ].join("\n");
    await appendFile(process.env.GITHUB_STEP_SUMMARY, summary, "utf8");
}

if (!passed) {
    if (missing.length > 0) {
        console.error(`Missing UC specs: ${missing.join(", ")}`);
    }
    for (const item of misplacedFiles) {
        console.error(`Misplaced ${item.uc}: ${item.file}; expected ${item.expected}`);
    }
    if (invalidFiles.length > 0) {
        console.error(`Out-of-range UC specs: ${invalidFiles.join(", ")}`);
    }
    process.exitCode = 1;
}
