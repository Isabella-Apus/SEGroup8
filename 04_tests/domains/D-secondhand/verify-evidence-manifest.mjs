import { access, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(scriptDirectory, "..", "..", "..");
const manifestPath = path.join(scriptDirectory, "evidence-manifest.json");
const requiredUseCases = ["UC16", "UC17", "UC18", "UC19", "UC20"];

function fail(message) {
    console.error(`Domain-D evidence validation failed: ${message}`);
    process.exitCode = 1;
}

function resolveRepositoryPath(relativePath, label) {
    if (typeof relativePath !== "string" || relativePath.trim() === "") {
        fail(`${label} has no repository path`);
        return null;
    }

    const absolutePath = path.resolve(repositoryRoot, relativePath);
    const relativeToRoot = path.relative(repositoryRoot, absolutePath);
    if (relativeToRoot.startsWith("..") || path.isAbsolute(relativeToRoot)) {
        fail(`${label} points outside the repository: ${relativePath}`);
        return null;
    }
    return absolutePath;
}

async function requireRepositoryPath(relativePath, label) {
    const absolutePath = resolveRepositoryPath(relativePath, label);
    if (!absolutePath) return null;
    try {
        await access(absolutePath);
        return absolutePath;
    } catch {
        fail(`${label} references a missing path: ${relativePath}`);
        return null;
    }
}

async function readJson(absolutePath, label) {
    try {
        return JSON.parse(await readFile(absolutePath, "utf8"));
    } catch (error) {
        fail(`${label} is not valid JSON: ${error.message}`);
        return null;
    }
}

const manifest = await readJson(manifestPath, "evidence-manifest.json");
if (!manifest) process.exit();

if (manifest.scope !== "DOMAIN_D") fail("scope must be DOMAIN_D");
if (manifest.classifications?.api !== "API_PASS") fail("API classification must be API_PASS");
if (manifest.classifications?.uiWalkthrough !== "UI_WALKTHROUGH_PASS") {
    fail("UI classification must be UI_WALKTHROUGH_PASS");
}
if (manifest.classifications?.e2e !== "E2E_PENDING") fail("E2E classification must remain E2E_PENDING");

const summaryPath = await requireRepositoryPath(manifest.backend?.summary, "backend.summary");
await requireRepositoryPath(manifest.backend?.log, "backend.log");

const suites = manifest.backend?.expectedSuites;
if (!Array.isArray(suites) || suites.length !== 9) {
    fail("backend.expectedSuites must contain exactly nine suites");
} else {
    const classes = new Set();
    for (const [index, suite] of suites.entries()) {
        if (classes.has(suite.class)) fail(`backend.expectedSuites duplicates ${suite.class}`);
        classes.add(suite.class);
        await requireRepositoryPath(suite.report, `backend.expectedSuites[${index}].report`);
    }
}

if (summaryPath) {
    const summary = await readJson(summaryPath, "backend summary");
    if (summary) {
        if (summary.classifications?.api !== "API_PASS") fail("backend summary is not API_PASS");
        if (summary.classifications?.e2e !== "E2E_PENDING") fail("backend summary overstates E2E status");
        if (summary.totals?.expectedClasses !== 9 || summary.totals?.reportedClasses !== 9) {
            fail("backend summary does not report all nine expected classes");
        }
        if ((summary.totals?.failed ?? -1) !== 0 || (summary.totals?.errors ?? -1) !== 0) {
            fail("backend summary contains failures or errors");
        }
    }
}

const walkthroughPath = await requireRepositoryPath(manifest.uiWalkthrough?.records, "uiWalkthrough.records");
await requireRepositoryPath(manifest.uiWalkthrough?.report, "uiWalkthrough.report");
await requireRepositoryPath(manifest.uiWalkthrough?.screenshots, "uiWalkthrough.screenshots");
if (manifest.uiWalkthrough?.status !== "UI_WALKTHROUGH_PASS") fail("walkthrough status is invalid");
if (manifest.uiWalkthrough?.environment !== "Vite mock") fail("walkthrough environment must state Vite mock");

if (walkthroughPath) {
    const walkthrough = await readJson(walkthroughPath, "UI walkthrough records");
    for (const useCase of requiredUseCases) {
        if (!walkthrough?.records?.some((record) => record.uc === useCase)) {
            fail(`UI walkthrough has no record for ${useCase}`);
        }
    }
}

if (!Array.isArray(manifest.realE2e) || manifest.realE2e.length !== requiredUseCases.length) {
    fail("realE2e must contain one pending entry for each Domain-D UC");
} else {
    for (const useCase of requiredUseCases) {
        const entry = manifest.realE2e.find((candidate) => candidate.uc === useCase);
        if (!entry) {
            fail(`realE2e has no entry for ${useCase}`);
            continue;
        }
        if (entry.status !== "E2E_PENDING") fail(`${useCase} must remain E2E_PENDING`);
        if (!Number.isInteger(entry.taskIssue)) fail(`${useCase} has no Task Issue number`);
        if (!entry.expectedSpec?.startsWith("frontend/e2e/domain-d/")) {
            fail(`${useCase} expected spec is outside frontend/e2e/domain-d`);
        }
    }
}

await requireRepositoryPath(manifest.platform?.playwrightConfig, "platform.playwrightConfig");
await requireRepositoryPath(manifest.platform?.compose, "platform.compose");
await requireRepositoryPath(manifest.platform?.runner, "platform.runner");

if (!process.exitCode) {
    console.log("Domain-D evidence validation passed: API_PASS, UI_WALKTHROUGH_PASS, E2E_PENDING.");
}
