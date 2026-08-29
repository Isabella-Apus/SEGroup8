import { access, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const manifestPath = path.join(scriptDirectory, "UC16-UC20-ui-run-result.json");
const requiredUseCases = ["UC16", "UC17", "UC18", "UC19", "UC20"];

function fail(message) {
    console.error(`Evidence validation failed: ${message}`);
    process.exitCode = 1;
}

async function requireFile(relativePath, label) {
    if (typeof relativePath !== "string" || relativePath.trim() === "") {
        fail(`${label} has no file path`);
        return;
    }

    const absolutePath = path.resolve(scriptDirectory, relativePath);
    const evidenceRoot = path.resolve(scriptDirectory, "..", "..", "..", "UC16-UC20-screenshots");
    const relativeToRoot = path.relative(evidenceRoot, absolutePath);

    if (relativeToRoot.startsWith("..") || path.isAbsolute(relativeToRoot)) {
        fail(`${label} points outside the screenshot evidence directory: ${relativePath}`);
        return;
    }

    try {
        await access(absolutePath);
    } catch {
        fail(`${label} references a missing file: ${relativePath}`);
    }
}

let manifest;
try {
    manifest = JSON.parse(await readFile(manifestPath, "utf8"));
} catch (error) {
    fail(`cannot parse ${path.basename(manifestPath)}: ${error.message}`);
    process.exit();
}

if (!Array.isArray(manifest.records) || manifest.records.length === 0) {
    fail("records must be a non-empty array");
} else {
    const names = new Set();
    const files = new Set();

    for (const [index, record] of manifest.records.entries()) {
        const label = `records[${index}]`;
        if (!requiredUseCases.includes(record.uc)) {
            fail(`${label} has unsupported use case: ${record.uc}`);
        }
        if (typeof record.name !== "string" || record.name.trim() === "") {
            fail(`${label} has no name`);
        } else if (names.has(record.name)) {
            fail(`${label} duplicates record name: ${record.name}`);
        } else {
            names.add(record.name);
        }
        if (files.has(record.file)) {
            fail(`${label} duplicates screenshot path: ${record.file}`);
        } else {
            files.add(record.file);
        }
        await requireFile(record.file, label);
    }

    for (const useCase of requiredUseCases) {
        if (!manifest.records.some((record) => record.uc === useCase)) {
            fail(`no evidence record found for ${useCase}`);
        }
    }
}

if (!Array.isArray(manifest.issues)) {
    fail("issues must be an array");
}

if (!Array.isArray(manifest.resolvedIssues)) {
    fail("resolvedIssues must be an array");
} else {
    for (const [index, issue] of manifest.resolvedIssues.entries()) {
        await requireFile(issue.evidence, `resolvedIssues[${index}].evidence`);
        await requireFile(issue.regressionEvidence, `resolvedIssues[${index}].regressionEvidence`);
    }
}

if (!process.exitCode) {
    console.log(
        `Evidence validation passed: ${manifest.records.length} records cover ${requiredUseCases.join(", ")}.`,
    );
}
