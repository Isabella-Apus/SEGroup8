import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(process.cwd());
if (path.basename(root) !== 'SEGroup8' || !fs.existsSync(path.join(root, '.git'))) {
  throw new Error(`Run from the SEGroup8 repository root, got: ${root}`);
}

const apply = process.argv.includes('--apply');
const changed = [];
const rel = (p) => path.relative(root, p).replaceAll('\\', '/');
const inside = (p) => {
  const resolved = path.resolve(p);
  if (resolved !== root && !resolved.startsWith(`${root}${path.sep}`)) {
    throw new Error(`Refusing path outside repository: ${resolved}`);
  }
  return resolved;
};
const mkdir = (p) => {
  p = inside(p);
  if (apply) fs.mkdirSync(p, { recursive: true });
};
const write = (p, content) => {
  p = inside(p);
  mkdir(path.dirname(p));
  if (apply) fs.writeFileSync(p, content.replaceAll('\r\n', '\n').trimEnd() + '\n', 'utf8');
  changed.push(`WRITE ${rel(p)}`);
};
const move = (from, to) => {
  from = inside(from);
  to = inside(to);
  if (!fs.existsSync(from)) return false;
  if (fs.existsSync(to)) throw new Error(`Target already exists: ${rel(to)}`);
  mkdir(path.dirname(to));
  if (apply) fs.renameSync(from, to);
  changed.push(`MOVE  ${rel(from)} -> ${rel(to)}`);
  return true;
};
const read = (p) => fs.readFileSync(inside(p), 'utf8').replaceAll('\r\n', '\n');

const docs = path.join(root, '02_docs');
const tests = path.join(root, '04_tests');
const architecture = path.join(docs, 'architecture');
const specifications = path.join(docs, 'specifications');
const archive = path.join(docs, 'archive');

const sourceSpecs = {
  requirements: path.join(architecture, '软件需求说明书.md'),
  overview: path.join(architecture, '软件概要设计说明书.md'),
  detail: path.join(architecture, '软件详细设计说明书.md'),
  matrix: path.join(architecture, '需求追溯矩阵.md'),
};
for (const [kind, p] of Object.entries(sourceSpecs)) {
  if (!fs.existsSync(p)) throw new Error(`Missing ${kind} source: ${rel(p)}`);
}

let requirementsText = read(sourceSpecs.requirements);
const overviewText = read(sourceSpecs.overview);
const detailText = read(sourceSpecs.detail);
let matrixText = read(sourceSpecs.matrix);

const sectionFor = (text, headingPattern) => {
  const lines = text.split('\n');
  const start = lines.findIndex((line) => headingPattern.test(line));
  if (start < 0) throw new Error(`Heading not found: ${headingPattern}`);
  let end = lines.length;
  for (let i = start + 1; i < lines.length; i += 1) {
    if (/^##\s+/.test(lines[i])) { end = i; break; }
  }
  return lines.slice(start, end).join('\n').trim();
};
const firstMermaid = (section) => {
  const match = section.match(/```mermaid\s*\n([\s\S]*?)```/);
  if (!match) throw new Error(`Mermaid block missing in section: ${section.slice(0, 80)}`);
  return match[1].trim();
};
const titleFromRequirement = (section, uc) => {
  const line = section.split('\n')[0];
  return line.replace(/^##\s+REQ\d+\s*\/\s*UC\d+\s*/, '').trim() || uc;
};
const domainFor = (n) => n <= 5 ? 'A' : n <= 10 ? 'B' : n <= 15 ? 'C' : n <= 20 ? 'D' : 'E';
const domainSlug = { A: 'A-identity', B: 'B-catalog-shop', C: 'C-order-fulfillment', D: 'D-secondhand', E: 'E-engagement-finance' };
const specFor = (n) => {
  const uc = `uc${String(n).padStart(2, '0')}`;
  const domain = domainFor(n).toLowerCase();
  const dir = path.join(root, 'frontend', 'e2e', `domain-${domain}`);
  const found = fs.readdirSync(dir).find((name) => name.startsWith(`${uc}-`) && name.endsWith('.spec.ts'));
  if (!found) throw new Error(`Missing browser spec for ${uc.toUpperCase()}`);
  return `frontend/e2e/domain-${domain}/${found}`;
};
const playwrightJsonFor = (uc) => {
  const candidates = [
    path.join(tests, uc, 'evidence', 'playwright-results.json'),
    path.join(tests, uc, 'evidence', 'raw-reports', 'playwright', 'playwright-results.json'),
  ];
  return candidates.find((p) => fs.existsSync(p));
};

const ciRun = 'https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952';
const ciJob = 'https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98897601611';

// Refresh the master traceability result column from checked-in evidence and the latest main CI run.
const matrixLines = matrixText.split('\n');
for (let i = 0; i < matrixLines.length; i += 1) {
  const match = matrixLines[i].match(/^\|\s*REQ\d+\s*\/\s*UC(\d{2})\s/);
  if (!match) continue;
  const n = Number(match[1]);
  const uc = `UC${match[1]}`;
  const cells = matrixLines[i].split('|');
  if (cells.length < 7) continue;
  const localJson = playwrightJsonFor(uc);
  cells[cells.length - 2] = localJson
    ? ` **LOCAL_E2E_PASS**：已归档 Playwright JSON，1/1 通过、unexpected 0；路径 \`../../04_tests/${uc}/${rel(localJson).split(`${uc}/`)[1]}\`。最新 main 全量 E2E CI 同时通过：${ciJob} `
    : ` **CI_E2E_PASS / LOCAL_ARTIFACT_MISSING**：服务/API 原始结果已归档；最新 main 的全 UC Playwright Job 通过（${ciJob}），但仓库未保存本用例独立 Playwright JSON。 `;
  matrixLines[i] = cells.join('|');
}
const section3 = matrixLines.findIndex((line) => line === '## 3 测试证据汇总');
if (section3 < 0) throw new Error('Traceability summary section not found');
matrixText = matrixLines.slice(0, section3).join('\n') + `

## 3 测试证据汇总

| 范围 | 当前可核验证据 | 结论 |
|---|---|---|
| UC01–UC05 | 每个 UC 均有独立 Playwright JSON，1/1 通过 | 本地证据归档完整 |
| UC06–UC10 | 服务/API 结果已归档；仓库无独立 Playwright JSON | 最新 main 全 UC E2E CI 通过，但需补下载/归档每 UC 浏览器原始产物 |
| UC11–UC25 | 每个 UC 均有独立 Playwright JSON，1/1 通过 | 本地证据归档完整 |
| 静态覆盖门禁 | 25/25 均存在规范命名的 Playwright spec | 只证明测试入口齐全，不单独证明运行通过 |
| 最新 main CI | \`09db0eed\` 的 Kinda Goods CI/CD run 成功；后端、前端、Domain A–E、全 UC Playwright、镜像发布及 K3s 部署 jobs 成功 | CI 证据：${ciRun} |

## 4 中期检查剩余缺口

| 优先级 | 缺口 | 当前状态 | Done 条件 |
|---|---|---|---|
| P0 | UC06–UC10 的独立 Playwright JSON 未进入仓库证据目录 | \`CI_E2E_PASS / LOCAL_ARTIFACT_MISSING\` | 从成功 CI 下载并按 UC 拆分归档，或在当前基线本地重跑并保留 JSON/日志/截图 |
| P0 | 6 个目标微服务尚未全部成为独立部署单元 | \`TARGET / NOT_IMPLEMENTED\` | 每个服务具备 module/JAR、Dockerfile、镜像、Helm、独立 schema/账号、契约测试和恢复证据 |
| P1 | 旧聚合文档与单 UC 文档并存 | 本分支重构处理中 | 以 \`02_docs/UC01\`–\`UC25\` 为唯一单用例来源，旧文档只读归档 |
| P1 | 中期四份总文档包含旧分支表述 | 本分支更新处理中 | 四份总文档与 \`09db0eed\`、当前测试证据和六服务 TARGET 状态一致 |

> 结论：25 个用例的测试入口和最新 main CI 全量 E2E 均已通过，但“全部任务完成”仍不成立，因为 UC06–UC10 的逐 UC 原始浏览器产物未归档，且六个目标业务微服务尚未全部实现与独立部署。
`;

for (let n = 1; n <= 25; n += 1) {
  const digits = String(n).padStart(2, '0');
  const uc = `UC${digits}`;
  const ucDir = path.join(docs, uc);
  const testDir = path.join(tests, uc);
  mkdir(ucDir);
  mkdir(testDir);

  const reqSection = sectionFor(requirementsText, new RegExp(`^##\\s+REQ${digits}\\s*\\/\\s*UC${digits}\\b`));
  const overviewSection = sectionFor(overviewText, new RegExp(`^##\\s+UC${digits}\\b`));
  const detailSection = sectionFor(detailText, new RegExp(`^##\\s+UC${digits}\\b`));
  const title = titleFromRequirement(reqSection, uc);
  const system = firstMermaid(reqSection);
  const component = firstMermaid(overviewSection);
  const object = firstMermaid(detailSection);

  const oldReadme = path.join(ucDir, 'README.md');
  if (fs.existsSync(oldReadme)) move(oldReadme, path.join(archive, 'uc-design-notes', `${uc}.md`));
  const oldTrace = path.join(ucDir, 'traceability.md');
  if (fs.existsSync(oldTrace)) move(oldTrace, path.join(archive, 'uc-traceability-notes', `${uc}.md`));

  const reqBody = reqSection
    .replace(/^##\s+/, '# ')
    .replace(/```mermaid\s*\n[\s\S]*?```/, `> 系统级模型源码：[system.mmd](system.mmd)`);
  write(path.join(ucDir, 'requirement.md'), reqBody);
  write(path.join(ucDir, 'system.mmd'), system);
  write(path.join(ucDir, 'component.mmd'), component);
  write(path.join(ucDir, 'object.mmd'), object);

  const matrixRow = matrixText.split('\n').find((line) => new RegExp(`^\\|\\s*REQ${digits}\\s*\\/\\s*UC${digits}\\s`).test(line));
  if (!matrixRow) throw new Error(`Traceability row missing for ${uc}`);
  write(path.join(ucDir, 'traceability.md'), `# ${uc} 追溯矩阵

| 需求 / 用例 | 三层模型 | 主要代码模块 | 测试编号与现有测试 | 结果 / 证据 |
|---|---|---|---|---|
${matrixRow}

## 权威材料

- 需求：[requirement.md](requirement.md)
- 系统级模型：[system.mmd](system.mmd)
- 组件级模型：[component.mmd](component.mmd)
- 对象级模型：[object.mmd](object.mmd)
- 测试计划：[test-plan.md](test-plan.md)
- 测试报告：[test-report.md](test-report.md)
- 浏览器测试：\`${specFor(n)}\`
- 原始证据：\`../../04_tests/${uc}/evidence/\`
`);

  const planSource = path.join(testDir, 'test-plan.md');
  if (fs.existsSync(planSource)) move(planSource, path.join(ucDir, 'test-plan.md'));
  const reportCandidates = fs.existsSync(testDir)
    ? fs.readdirSync(testDir).filter((name) => name === 'test-report.md' || name.endsWith('测试报告.md'))
    : [];
  if (reportCandidates.length > 1) throw new Error(`Multiple test reports for ${uc}: ${reportCandidates.join(', ')}`);
  if (reportCandidates.length === 1) move(path.join(testDir, reportCandidates[0]), path.join(ucDir, 'test-report.md'));

  const browserSpec = specFor(n);
  if (!fs.existsSync(path.join(ucDir, 'test-plan.md'))) {
    write(path.join(ucDir, 'test-plan.md'), `# ${uc} 测试计划

## 验收目标

验证“${title}”的主成功流程、至少一个异常或权限分支，以及页面、接口和数据库结果的一致性。

## 自动化范围

| 编号 | 层级 | 入口 | 通过条件 |
|---|---|---|---|
| \`UNIT-TC${digits}-001\` | 单元/服务 | 见 [traceability.md](traceability.md) 中的测试类 | 关键业务规则与异常分支均有断言 |
| \`INT-TC${digits}-001\` | 集成/API | 见 [traceability.md](traceability.md) 中的测试类 | HTTP、数据库状态和权限边界一致 |
| \`E2E-TC${digits}-001\` | Compose + MySQL + Playwright | \`${browserSpec}\` | 完整业务链路成功，失败为非零退出码并保留原始证据 |

## 执行入口

\`\`\`powershell
$env:E2E_EVIDENCE_ROOT = '04_tests/${uc}/evidence'
$env:E2E_OUTPUT_DIR = '04_tests/${uc}/evidence/raw-reports/playwright'
.\\scripts\\e2e\\run-compose-e2e.ps1 -ResetDatabase -- ${browserSpec.replace('frontend/', '')} --workers=1
\`\`\`

测试源码留在框架默认目录；本计划不复制 Java 或 Playwright 源码。只有 Surefire/Playwright 机器结果、日志、截图和真实退出码可以支撑 PASS。
`);
  }
  if (!fs.existsSync(path.join(ucDir, 'test-report.md'))) {
    write(path.join(ucDir, 'test-report.md'), `# ${uc} 测试报告

状态：NOT_RUN。未找到可迁移的专项测试报告；不得以测试文件存在替代运行结果。
`);
  }

  const demoCandidates = fs.existsSync(testDir)
    ? fs.readdirSync(testDir).filter((name) => name.toLowerCase().endsWith('demo.http'))
    : [];
  if (demoCandidates.length === 1 && demoCandidates[0] !== 'demo.http') {
    move(path.join(testDir, demoCandidates[0]), path.join(testDir, 'demo.http'));
  }

  const localJson = playwrightJsonFor(uc);
  const status = localJson ? 'LOCAL_E2E_PASS' : 'CI_E2E_PASS / LOCAL_ARTIFACT_MISSING';
  write(path.join(ucDir, 'README.md'), `# ${uc} ${title}

状态：**${status}**。

本目录是 ${uc} 需求、三层设计、追溯和测试文档的唯一事实来源。

- [需求说明](requirement.md)
- [系统级模型](system.mmd)
- [组件级模型](component.mmd)
- [对象级模型](object.mmd)
- [追溯矩阵](traceability.md)
- [测试计划](test-plan.md)
- [测试报告](test-report.md)
- 浏览器测试：\`${browserSpec}\`
- 原始证据：\`../../04_tests/${uc}/evidence/\`
- Domain：\`${domainSlug[domainFor(n)]}\`

最新 main 全 UC Playwright Job：${ciJob}
`);
  write(path.join(testDir, 'README.md'), `# ${uc} 测试入口与证据

- 测试计划：\`../../02_docs/${uc}/test-plan.md\`
- 测试报告：\`../../02_docs/${uc}/test-report.md\`
- 追溯矩阵：\`../../02_docs/${uc}/traceability.md\`
- Playwright 源码：\`../../${browserSpec}\`
- Evidence：\`evidence/\`
- 当前状态：\`${status}\`

测试源码不复制到本目录。\`04_tests\` 只保存可复现入口、原始机器报告、日志、截图、trace 和视频。
`);
}

// Normalize common historical test ID variants after relocating the documents.
if (apply) {
  for (let n = 1; n <= 25; n += 1) {
    const digits = String(n).padStart(2, '0');
    const ucDir = path.join(docs, `UC${digits}`);
    for (const name of ['test-plan.md', 'test-report.md', 'traceability.md']) {
      const p = path.join(ucDir, name);
      let content = read(p)
        .replaceAll(`INT-UC${digits}-`, `INT-TC${digits}-`)
        .replaceAll(`E2E-UC${digits}-`, `E2E-TC${digits}-`)
        .replaceAll(`API-UC${digits}-`, `API-TC${digits}-`);
      content = content.replace(new RegExp(`(UNIT|INT|API|E2E)-TC${digits}-(\\d{2})(?!\\d)`, 'g'), (_, kind, id) => `${kind}-TC${digits}-0${id}`);
      fs.writeFileSync(p, content, 'utf8');
    }
  }
}

// Move the four authoritative master documents to stable ASCII names.
write(path.join(specifications, 'requirements-traceability-matrix.md'), matrixText);
move(sourceSpecs.requirements, path.join(specifications, 'software-requirements.md'));
move(sourceSpecs.overview, path.join(specifications, 'software-architecture-design.md'));
move(sourceSpecs.detail, path.join(specifications, 'software-detailed-design.md'));
if (apply) fs.unlinkSync(sourceSpecs.matrix);
changed.push(`MOVE  ${rel(sourceSpecs.matrix)} -> ${rel(path.join(specifications, 'requirements-traceability-matrix.md'))}`);

// Preserve old requirement and aggregate documents as read-only historical material.
const requirementRoot = path.join(root, '01_requirements');
if (fs.existsSync(requirementRoot)) {
  for (const name of fs.readdirSync(requirementRoot)) {
    move(path.join(requirementRoot, name), path.join(archive, 'legacy-requirements', name));
  }
}

const rootDocRenames = new Map([
  ['完整用例清单.md', 'use-case-catalog.md'],
  ['测试结果聚合摘要.md', 'test-summary.md'],
  ['需求编号与追溯规则模板.md', 'traceability-conventions.md'],
]);
for (const [from, to] of rootDocRenames) move(path.join(docs, from), path.join(docs, to));

for (const name of fs.readdirSync(docs)) {
  const p = path.join(docs, name);
  if (!fs.statSync(p).isFile()) continue;
  if (['use-case-catalog.md', 'test-summary.md', 'traceability-conventions.md', 'README.md'].includes(name)) continue;
  if (name.startsWith('成员')) move(p, path.join(archive, 'member-handoffs', name));
  else if (/^UC\d{2}/.test(name)) move(p, path.join(archive, 'legacy-aggregates', name));
}

write(path.join(root, '01_source', 'README.md'), `# 01_source - 源码清单

课程提交允许“代码或仓库清单”。为避免破坏 Maven、Vite、Compose 和 CI 的原生路径，本仓库不复制源码；本目录记录唯一源码位置。

| 内容 | 唯一位置 |
|---|---|
| 单体后端与 Java 测试 | \`backend/\` |
| Vue 前端与 Playwright 测试 | \`frontend/\` |
| 已实现微服务原型 | \`microservices/\` |
| 数据库脚本 | \`sql/\`、\`docker/mysql/\` |
| 构建与验收脚本 | \`scripts/\` |
| GitHub Actions | \`.github/workflows/\` |

原系统标签：\`monolith-start\`。当前中期基线：\`09db0eed\`。
`);
write(path.join(root, '06_defense', 'README.md'), `# 06_defense - 答辩与演示材料

中期阶段先保留交付清单，最终答辩前补齐：

- PPT；
- 技术总结与 AI 使用说明；
- 代表性用例演示脚本；
- CI 失败阻断、Kubernetes 部署、扩缩容、故障处理和性能对比演示证据；
- 5–8 分钟备用录屏。

当前状态：\`PLANNED\`，不计为已完成。
`);
write(path.join(docs, 'README.md'), `# 02_docs - 中期文档索引

## 四份权威总文档

- [软件需求说明书](specifications/software-requirements.md)
- [软件概要设计说明书](specifications/software-architecture-design.md)
- [软件详细设计说明书](specifications/software-detailed-design.md)
- [需求追溯矩阵](specifications/requirements-traceability-matrix.md)

## 单用例唯一事实来源

${Array.from({ length: 25 }, (_, i) => `- [UC${String(i + 1).padStart(2, '0')}](UC${String(i + 1).padStart(2, '0')}/README.md)`).join('\n')}

## 跨用例材料

- [Domain A–E](domains/)
- [微服务架构、接口与数据归属](architecture/README.md)
- [用例总清单](use-case-catalog.md)
- [测试汇总](test-summary.md)
- [编号规则](traceability-conventions.md)
- [历史归档](archive/README.md)

旧材料只用于追溯，不再维护当前状态。测试源码保留在框架默认目录，原始证据位于 \`../04_tests/\`。
`);
write(path.join(archive, 'README.md'), `# 历史材料归档

这里保存重构前的聚合文档、成员交接稿和旧需求文件。它们不再作为当前需求、设计或测试状态的事实来源。

当前权威入口：\`../README.md\`。
`);
write(path.join(tests, 'README.md'), `# 04_tests - 自动化入口与原始证据

本目录不复制测试源码。Java 测试保留在 \`backend/src/test\` 或各微服务 \`src/test\`，Playwright 测试保留在 \`frontend/e2e\`。

- \`UC01\`–\`UC25\`：单用例运行入口、Evidence 和原始报告；
- \`domains/\`：跨用例 Domain 汇总与运行器；
- \`performance/\`：k6 等性能脚本和结果；
- \`platform-e2e/\`：共享全栈冒烟证据。

测试计划、测试报告和追溯表统一位于 \`../02_docs/UCxx/\`。
`);
write(path.join(root, 'MIDTERM_DELIVERY.md'), `# Kinda Goods 中期检查交付说明

基线：\`09db0eed\`；整理分支：\`restructure-whole\`；日期：2026-08-28。

## 结论

四份新文档应作为本学期的权威更新版，不需要机械合并回上学期 PDF。上学期资料保留为历史基线；四份新文档必须覆盖最新 25 个用例，并与当前代码、测试和微服务状态一致。

当前已确认：25/25 Playwright spec 静态覆盖；最新 main CI 全 UC Playwright Job 成功；UC01–05、UC11–25 有仓库内逐 UC Playwright JSON。当前未完成：UC06–10 逐 UC 浏览器原始产物未归档；六个目标业务微服务尚未全部实现为独立部署单元。

## 中期入口

- 文档索引：\`02_docs/README.md\`
- 课程对照审计：\`02_docs/midterm-audit.md\`
- 源码清单：\`01_source/README.md\`
- DevOps：\`03_devops/\`
- 测试与证据：\`04_tests/README.md\`
- 管理材料：\`05_management/\`
- 答辩占位：\`06_defense/README.md\`
`);
write(path.join(docs, 'midterm-audit.md'), `# Kinda Goods 中期检查审计

## 总体判定

| 检查项 | 判定 | 证据或缺口 |
|---|---|---|
| 原系统与 Git 标签 | PASS | \`monolith-start\` 标签存在；当前 main 为 \`09db0eed\` |
| 25 个确认用例 | PASS（范围） | \`use-case-catalog.md\` 与 UC01–UC25 目录 |
| 每 UC 需求、系统级图、组件级图、对象级图、追溯 | PASS（文档） | \`UC01\`–\`UC25\` 标准目录 |
| 25 个 Playwright 测试入口 | PASS（静态） | 覆盖门禁 25/25 |
| 最新 main 全 UC E2E | PASS（远端 CI） | ${ciJob} |
| 逐 UC 浏览器原始产物 | PARTIAL | UC01–05、UC11–25 已归档；UC06–10 缺独立 JSON |
| 前端/后端/MySQL 容器与 CI | PASS（最新 main CI） | ${ciRun} |
| 微服务划分、接口、表归属 | PASS（设计） | \`architecture/\` 三份清单 |
| 至少 3 个独立业务微服务 | PARTIAL（实现） | 当前仅 Domain B 四个原型模块；六目标服务未全部实现/部署 |
| Kubernetes、扩缩容、故障、性能对比 | NOT_RUN / 待最终阶段 | 中期不据计划文件宣称完成 |

## 中期提交解释

教师要求的是四类持续更新的工程文档，而不是要求把新内容逐页拼回上学期 PDF。最稳妥的提交方式是：提交本分支中的四份当前权威总文档及可编辑 Mermaid 源文件，同时把上学期完整文档作为“原系统历史基线”保留。若需 PDF，可由这四份当前文档统一导出；不要同时提交两套互相冲突的“最新版”。
`);

console.log(`${apply ? 'Applied' : 'Dry run'}: ${changed.length} operations`);
for (const line of changed) console.log(line);
