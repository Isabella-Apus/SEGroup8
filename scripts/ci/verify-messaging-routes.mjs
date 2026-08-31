import { readFile, readdir } from 'node:fs/promises';
import { join, relative } from 'node:path';

const repoRoot = process.cwd();
const sourceRoot = join(repoRoot, 'microservices', 'messaging-service', 'src', 'main', 'java');
const reviewedPath = join(repoRoot, '02_docs', 'microservices', 'messaging-service', 'openapi.yaml');
const manifestPath = join(repoRoot, '04_tests', 'microservices', 'messaging-service', 'public-api-route-manifest.json');

const normalize = (method, path) => `${method.toUpperCase()} ${path.replace(/\{[^}]+\}/g, '{}')}`;
const sorted = values => [...new Set(values)].sort();

async function javaFiles(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) files.push(...await javaFiles(path));
    else if (entry.isFile() && path.endsWith('.java')) files.push(path);
  }
  return files;
}

async function controllerRoutes() {
  const routes = [];
  for (const file of await javaFiles(sourceRoot)) {
    const source = await readFile(file, 'utf8');
    const classPrefix = source.match(/@RequestMapping\s*\(\s*["']([^"']+)["']\s*\)[\s\S]*?class\s+\w+Controller/)?.[1] ?? '';
    if (!classPrefix) continue;
    const methods = [...source.matchAll(/@(Get|Post|Put|Delete|Patch)Mapping\s*(?:\(\s*(?:value\s*=\s*)?["']([^"']*)["']\s*\))?/g)];
    for (const match of methods) {
      const suffix = match[2] ?? '';
      routes.push(normalize(match[1], `${classPrefix}${suffix}`));
    }
  }
  // WebSocketConfig registers this public handshake outside MVC controllers.
  const ws = (await javaFiles(sourceRoot)).find(file => file.endsWith('WebSocketConfig.java'));
  if (ws && (await readFile(ws, 'utf8')).includes('/ws/realtime')) routes.push('GET /ws/realtime');
  return sorted(routes);
}

function reviewedRoutes(yaml) {
  const routes = [];
  let path = null;
  for (const line of yaml.split(/\r?\n/)) {
    const pathMatch = line.match(/^  (\/[^:]+):\s*$/);
    if (pathMatch) { path = pathMatch[1]; continue; }
    const methodMatch = line.match(/^    (get|post|put|delete|patch):\s*$/i);
    if (path && methodMatch) routes.push(normalize(methodMatch[1], path));
  }
  return sorted(routes);
}

function assertEqual(name, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) {
    console.error(`${name} mismatch\nExpected: ${e}\nActual:   ${a}`);
    process.exitCode = 1;
  }
}

const manifest = sorted((JSON.parse(await readFile(manifestPath, 'utf8'))).map(value => {
  const [method, ...path] = value.split(' ');
  return normalize(method, path.join(' '));
}));
const reviewed = reviewedRoutes(await readFile(reviewedPath, 'utf8'));
const controllers = await controllerRoutes();
assertEqual('Controller vs reviewed OpenAPI', controllers, reviewed);
assertEqual('Reviewed OpenAPI vs API test manifest', reviewed, manifest);

if (process.argv.includes('--runtime-file')) {
  const runtimeFile = process.argv[process.argv.indexOf('--runtime-file') + 1];
  const runtime = JSON.parse(await readFile(runtimeFile, 'utf8'));
  const runtimeRoutes = sorted(Object.entries(runtime.paths ?? {}).flatMap(([path, item]) =>
    Object.keys(item).filter(method => ['get', 'post', 'put', 'delete', 'patch'].includes(method))
      .map(method => normalize(method, path))));
  assertEqual('Runtime OpenAPI vs reviewed OpenAPI', runtimeRoutes, reviewed);
}

if (!process.exitCode) console.log(`Messaging route contract PASS (${manifest.length} operations; ${relative(repoRoot, reviewedPath)})`);
