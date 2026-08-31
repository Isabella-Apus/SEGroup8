import { readdir, readFile } from 'node:fs/promises';
import { join, relative } from 'node:path';

const root = join(process.cwd(), 'microservices', 'messaging-service', 'src', 'main', 'java');
const forbiddenImport = /import\s+[^;]*(?:\.mapper\.|\.repository\.|\.entity\.|com\.segroup8\.platform)/i;
const forbiddenSchema = /\b(?:from|join|insert\s+into|update|delete\s+from)\s+`?(?:user|product|shop|secondhand_product|order_info|order_item|payment|refund|merchant_application|user_block)`?(?=\s|[);,]|$)/i;
const forbiddenSchemaName = /segroup8_platform\s*\./i;
const forbiddenIdentityHeader = /x-(?:user|seller|admin)-id/i;

async function walk(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) files.push(...await walk(path));
    else if (entry.isFile() && path.endsWith('.java')) files.push(path);
  }
  return files;
}

const violations = [];
for (const file of await walk(root)) {
  const source = await readFile(file, 'utf8');
  const checks = [
    [forbiddenImport, 'foreign Mapper/Repository/Entity import'],
    [forbiddenSchema, 'foreign table query/write'],
    [forbiddenSchemaName, 'cross-schema reference'],
    [forbiddenIdentityHeader, 'trusted identity header'],
  ];
  for (const [pattern, description] of checks) {
    if (pattern.test(source)) violations.push(`${relative(process.cwd(), file)}: ${description}`);
  }
}

if (violations.length) {
  console.error(violations.join('\n'));
  process.exit(1);
}
console.log(`Messaging source boundary PASS (${(await walk(root)).length} Java files scanned)`);
