import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const failures = [];

for (let n = 1; n <= 25; n += 1) {
  const uc = `UC${String(n).padStart(2, '0')}`;
  const required = [
    'README.md',
    'requirement.md',
    'system.mmd',
    'concept.mmd',
    'component.mmd',
    'component-sequence.mmd',
    'object.mmd',
    'object-sequence.mmd',
    'traceability.md',
    'test-plan.md',
    'test-report.md',
  ];
  for (const name of required) {
    const target = path.join(root, '02_docs', uc, name);
    if (!fs.existsSync(target)) failures.push(`missing required file: ${path.relative(root, target)}`);
  }

  const requirementPath = path.join(root, '02_docs', uc, 'requirement.md');
  if (fs.existsSync(requirementPath)) {
    const text = fs.readFileSync(requirementPath, 'utf8');
    for (const token of [`REQ${uc.slice(2)}`, `US${uc.slice(2)}`, uc, `AC${uc.slice(2)}-001`]) {
      if (!text.includes(token)) failures.push(`${path.relative(root, requirementPath)} lacks ${token}`);
    }
    for (const heading of ['主要参与者', '前置条件', '后置条件', '基本事件流', '备选/异常事件流', '特殊需求']) {
      if (!text.includes(heading)) failures.push(`${path.relative(root, requirementPath)} lacks ${heading}`);
    }
  }
}

const roots = ['README.md', 'MIDTERM_DELIVERY.md', '01_source', '02_docs', '03_devops', '04_tests', '05_management', '06_defense'];
const markdown = [];
function walk(target) {
  if (!fs.existsSync(target)) return;
  const stat = fs.statSync(target);
  if (stat.isFile()) {
    if (target.endsWith('.md')) markdown.push(target);
    return;
  }
  for (const entry of fs.readdirSync(target, { withFileTypes: true })) {
    const child = path.join(target, entry.name);
    const rel = path.relative(root, child).replaceAll('\\', '/');
    if (entry.isDirectory() && (rel === '02_docs/archive' || rel.startsWith('02_docs/archive/') || rel === '05_management/archive' || rel.startsWith('05_management/archive/') || rel === '05_management/archive-' || rel.startsWith('05_management/archive-/'))) continue;
    walk(child);
  }
}
for (const entry of roots) walk(path.join(root, entry));

const linkPattern = /!?(?:\[[^\]]*\])\(([^)]+)\)/g;
for (const file of markdown) {
  const text = fs.readFileSync(file, 'utf8');
  for (const match of text.matchAll(linkPattern)) {
    let href = match[1].trim();
    if (href.startsWith('<') && href.endsWith('>')) href = href.slice(1, -1);
    if (!href || href.startsWith('#') || /^(?:https?:|mailto:|data:)/i.test(href)) continue;
    href = href.split('#')[0].replaceAll('%20', ' ');
    const resolved = path.resolve(path.dirname(file), href);
    if (!fs.existsSync(resolved)) failures.push(`broken link: ${path.relative(root, file)} -> ${match[1]}`);
  }
}

const diagrams = [];
walkDiagrams(path.join(root, '02_docs'));
function walkDiagrams(target) {
  if (!fs.existsSync(target)) return;
  for (const entry of fs.readdirSync(target, { withFileTypes: true })) {
    const child = path.join(target, entry.name);
    if (entry.isDirectory()) {
      if (entry.name !== 'archive') walkDiagrams(child);
    } else if (entry.name.endsWith('.mmd')) diagrams.push(child);
  }
}
for (const file of diagrams) {
  const body = fs.readFileSync(file, 'utf8').split(/\r?\n/).filter((line) => !line.trimStart().startsWith('%%')).join('\n');
  const first = body.trimStart().split(/\s+/)[0];
  if (!['flowchart', 'graph', 'sequenceDiagram', 'classDiagram', 'stateDiagram-v2', 'erDiagram'].includes(first)) {
    failures.push(`unsupported/empty Mermaid source: ${path.relative(root, file)} (${first || 'empty'})`);
  }
}

if (failures.length) {
  console.error(`Midterm documentation validation failed (${failures.length}):`);
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log(`PASS: 25/25 UC directories, ${markdown.length} current Markdown files, ${diagrams.length} Mermaid sources.`);
