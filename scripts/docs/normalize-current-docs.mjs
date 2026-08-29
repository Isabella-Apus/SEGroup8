import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(process.cwd());
if (path.basename(root) !== 'SEGroup8' || !fs.existsSync(path.join(root, '.git'))) {
  throw new Error(`Run from SEGroup8 root, got ${root}`);
}

const read = (p) => fs.readFileSync(p, 'utf8').replaceAll('\r\n', '\n');
const write = (p, text) => fs.writeFileSync(p, text.trimEnd() + '\n', 'utf8');
const specFor = (n) => {
  const digits = String(n).padStart(2, '0');
  const domain = n <= 5 ? 'a' : n <= 10 ? 'b' : n <= 15 ? 'c' : n <= 20 ? 'd' : 'e';
  const dir = path.join(root, 'frontend', 'e2e', `domain-${domain}`);
  const file = fs.readdirSync(dir).find((name) => name.startsWith(`uc${digits}-`) && name.endsWith('.spec.ts'));
  if (!file) throw new Error(`Missing UC${digits} spec`);
  return `frontend/e2e/domain-${domain}/${file}`;
};

for (let n = 1; n <= 25; n += 1) {
  const digits = String(n).padStart(2, '0');
  const uc = `UC${digits}`;
  const dir = path.join(root, '02_docs', uc);
  for (const name of fs.readdirSync(dir).filter((file) => file.endsWith('.md'))) {
    const p = path.join(dir, name);
    let text = read(p)
      .replaceAll(`04_tests/${uc}/test-plan.md`, `02_docs/${uc}/test-plan.md`)
      .replaceAll(`04_tests/${uc}/test-report.md`, `02_docs/${uc}/test-report.md`)
      .replaceAll(`../../04_tests/${uc}/test-plan.md`, `test-plan.md`)
      .replaceAll(`../../04_tests/${uc}/test-report.md`, `test-report.md`)
      .replaceAll(`INT-UC${digits}-`, `INT-TC${digits}-`)
      .replaceAll(`E2E-UC${digits}-`, `E2E-TC${digits}-`)
      .replaceAll(`API-UC${digits}-`, `API-TC${digits}-`);
    text = text.replace(new RegExp(`(UNIT|INT|API|E2E)-TC${digits}-(\\d{2})(?!\\d)`, 'g'), (_, kind, id) => `${kind}-TC${digits}-0${id}`);

    if (name === 'traceability.md') {
      const lines = text.split('\n');
      const row = lines.findIndex((line) => line.startsWith(`| REQ${digits} / ${uc} `));
      if (row >= 0) {
        const cells = lines[row].split('|');
        if (cells.length >= 7 && !cells[4].includes(specFor(n))) {
          cells[4] = `${cells[4].trimEnd()}；E2E-TC${digits}-001 \`${specFor(n)}\` `;
        }
        cells[4] = cells[4].replaceAll('（待补）', '').replaceAll('(待补)', '');
        lines[row] = cells.join('|');
        text = lines.join('\n');
      }
    }
    write(p, text);
  }
}

const matrixPath = path.join(root, '02_docs', 'specifications', 'requirements-traceability-matrix.md');
let matrix = read(matrixPath);
for (let n = 1; n <= 25; n += 1) {
  const digits = String(n).padStart(2, '0');
  matrix = matrix
    .replaceAll(`INT-UC${digits}-`, `INT-TC${digits}-`)
    .replaceAll(`E2E-UC${digits}-`, `E2E-TC${digits}-`)
    .replaceAll(`API-UC${digits}-`, `API-TC${digits}-`)
    .replace(new RegExp(`(UNIT|INT|API|E2E)-TC${digits}-(\\d{2})(?!\\d)`, 'g'), (_, kind, id) => `${kind}-TC${digits}-0${id}`);
}
const matrixLines = matrix.split('\n');
for (let n = 1; n <= 25; n += 1) {
  const digits = String(n).padStart(2, '0');
  const row = matrixLines.findIndex((line) => line.startsWith(`| REQ${digits} / UC${digits} `));
  if (row < 0) throw new Error(`Missing matrix row UC${digits}`);
  const cells = matrixLines[row].split('|');
  if (!cells[4].includes(specFor(n))) cells[4] = `${cells[4].trimEnd()}；E2E-TC${digits}-001 \`${specFor(n)}\` `;
  cells[4] = cells[4].replaceAll('（待补）', '').replaceAll('(待补)', '');
  matrixLines[row] = cells.join('|');
}
write(matrixPath, matrixLines.join('\n'));

const overviewPath = path.join(root, '02_docs', 'specifications', 'software-architecture-design.md');
write(overviewPath, read(overviewPath)
  .replaceAll('`microservice-boundaries.md`', '`../architecture/microservice-boundaries.md`')
  .replaceAll('`service-api-catalog.md`', '`../architecture/service-api-catalog.md`')
  .replaceAll('`database-ownership.md`', '`../architecture/database-ownership.md`'));

write(path.join(root, '02_docs', 'test-summary.md'), `# Kinda Goods 测试结果聚合摘要

> 基线：main \`09db0eed\`，2026-08-28。状态按“测试入口、仓库内原始证据、远端 CI”分层，不用静态文件存在替代运行通过。

## Domain 汇总

| Domain | UC 范围 | 静态 Playwright 入口 | 仓库内逐 UC Playwright JSON | 最新 main CI |
|---|---:|---:|---:|---|
| A | UC01–UC05 | 5/5 | 5/5 | PASS |
| B | UC06–UC10 | 5/5 | 0/5；服务/API 原始结果存在 | PASS |
| C | UC11–UC15 | 5/5 | 5/5 | PASS |
| D | UC16–UC20 | 5/5 | 5/5 | PASS |
| E | UC21–UC25 | 5/5 | 5/5 | PASS |

最新 main CI：<https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952>。其中后端、前端、Domain A–E、UC01–UC25 覆盖门禁、全 UC Playwright、镜像发布和 K3s 部署 jobs 成功；Release job 因非发布触发而 skipped。

## 结论

- 25/25 用例均有规范命名的 Playwright spec；这是静态覆盖 PASS。
- 最新 main 的“Playwright smoke and all UC E2E tests” job 成功；这是远端 CI E2E PASS。
- UC01–UC05、UC11–UC25 在仓库内各有 1/1 通过的 Playwright JSON；这是逐 UC 本地证据归档 PASS。
- UC06–UC10 缺少仓库内逐 UC Playwright JSON，状态为 \`CI_E2E_PASS / LOCAL_ARTIFACT_MISSING\`，不能写成本地证据完整。
- 微服务完成度与用例回归是不同维度。Domain B 四个原型模块存在，但六个目标服务仍未全部独立实现、构建和部署。
`);

const docs = path.join(root, '02_docs');
const domains = path.join(docs, 'domains');
write(path.join(domains, 'README.md'), `# Domain A-E 跨用例索引

Domain 文档只维护跨用例边界、依赖和测试汇总；每个 UC 的需求、三层模型、追溯和测试文档以 \`../UCxx/\` 为唯一事实来源。

- [A-identity](A-identity/README.md)：UC01-UC05
- [B-catalog-shop](B-catalog-shop/README.md)：UC06-UC10
- [C-order-fulfillment](C-order-fulfillment/README.md)：UC11-UC15
- [D-secondhand](D-secondhand/domain-design.md)：UC16-UC20
- [E-engagement-finance](E-engagement-finance/README.md)：UC21-UC25
`);
write(path.join(domains, 'B-catalog-shop', 'README.md'), `# Domain B - 商品、店铺、风险与行为

当前实现：\`catalog-service\`、\`shop-service\`、\`risk-service\`、\`behavior-service\` 四个可独立构建/测试的原型模块。目标 \`catalog-shop-service\` 尚未作为单一独立部署单元完成。

| 用例 | 单 UC 文档 | 浏览器测试 | 当前证据 |
|---|---|---|---|
${[6,7,8,9,10].map((n) => { const d=String(n).padStart(2,'0'); return `| UC${d} | [入口](../../UC${d}/README.md) | \`${specFor(n)}\` | CI E2E PASS；仓库缺逐 UC Playwright JSON |`; }).join('\n')}

Domain B 最新 CI job：<https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98896984854>。
`);
write(path.join(domains, 'C-order-fulfillment', 'README.md'), `# Domain C - 订单、物流、售后与评价

| 用例 | 单 UC 文档 | 浏览器测试 | 当前证据 |
|---|---|---|---|
${[11,12,13,14,15].map((n) => { const d=String(n).padStart(2,'0'); return `| UC${d} | [入口](../../UC${d}/README.md) | \`${specFor(n)}\` | LOCAL_E2E_PASS；CI PASS |`; }).join('\n')}

Domain C 最新 CI job：<https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98896984984>。目标 \`order-service\` 仍为 TARGET，当前主要运行实现仍在单体后端。
`);
write(path.join(domains, 'D-secondhand', 'domain-design.md'), `# Domain D - 二手交易跨用例设计

UC16-UC20 共享二手商品、议价、拍卖和二手订单履约边界。单 UC 需求与三层模型分别位于 \`../../UC16/\` 至 \`../../UC20/\`。

## 跨用例规则

- UC16 管理二手商品发布、编辑和状态；
- UC17 直接购买创建待付款订单；
- UC18 议价接受后创建待付款订单，拒绝或重复处理必须保持幂等；
- UC19 拍卖出价、结束和结算必须处理并发与重复结算；
- UC20 负责成交后的发货、物流、收货和结算；
- 通知失败不得回滚核心成交，失败事件需重试或补偿。

## 当前状态

UC16-UC20 均已有真实 Compose + MySQL + Playwright 独立 JSON，且最新 main Domain D 与全 UC E2E jobs 成功。目标 \`secondhand-service\` 尚未从单体完整抽取为独立部署单元。
`);
write(path.join(domains, 'D-secondhand', 'traceability.md'), `# Domain D 测试追溯汇总

| 用例 | 单 UC 追溯 | Playwright | 状态 |
|---|---|---|---|
${[16,17,18,19,20].map((n) => { const d=String(n).padStart(2,'0'); return `| UC${d} | [traceability](../../UC${d}/traceability.md) | \`${specFor(n)}\` | LOCAL_E2E_PASS / CI PASS |`; }).join('\n')}

Domain D 最新 CI job：<https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98896984973>。
`);
write(path.join(domains, 'D-secondhand', 'interfaces-and-data.md'), `# Domain D 接口与数据边界

当前权威接口和表归属分别见：

- [服务 API 清单](../../architecture/service-api-catalog.md)
- [数据库表归属](../../architecture/database-ownership.md)
- [微服务边界](../../architecture/microservice-boundaries.md)

二手域只管理二手商品、议价和拍卖事实；成交后通过幂等接口请求订单域创建订单，不直接写其他服务的业务表。当前代码仍有单体内调用，目标隔离状态为 \`NOT_IMPLEMENTED\`。
`);

const devopsDomains = path.join(root, '03_devops', 'domains');
const domainJobs = {
  A: ['UC01-UC05', '98896985040', 'identity-governance-service', '当前业务实现仍在单体'],
  B: ['UC06-UC10', '98896984854', 'catalog-shop-service', '四个 Domain B 原型模块已实现；目标聚合部署单元未完成'],
  C: ['UC11-UC15', '98896984984', 'order-service', '当前业务实现仍在单体'],
  D: ['UC16-UC20', '98896984973', 'secondhand-service', '当前业务实现仍在单体'],
  E: ['UC21-UC25', '98896984886', 'benefits-finance-service / messaging-service', '当前业务实现仍在单体'],
};
for (const [letter, [range, job, target, current]] of Object.entries(domainJobs)) {
  write(path.join(devopsDomains, `domain-${letter.toLowerCase()}.md`), `# Domain ${letter} DevOps 状态

| 项目 | 状态 |
|---|---|
| 用例范围 | ${range} |
| 最新 main Domain job | PASS：<https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/${job}> |
| 全 UC Playwright job | PASS：<https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98897601611> |
| 目标服务 | \`${target}\` |
| 独立微服务完成度 | TARGET / NOT_IMPLEMENTED：${current} |

该 CI 结果证明当前 main 的对应 Domain 测试通过，不证明目标服务已经完成独立 schema、镜像、Helm、权限隔离和故障恢复验收。
`);
}

const management = path.join(root, '05_management');
const managementArchive = path.join(management, 'archive');
const historicalFiles = [
  'domain-a-delivery-plan-2026-08-27.md',
  'PLATFORM-E2E-D0-ISSUE.md',
  'UC01-UC05-测试缺口Issue草案.md',
  'UC16-UC20-PR说明.md',
  'UC16-UC20-评审与演示证据.md',
  '成员E-UC21-UC25-测试缺口Issues.md',
];
fs.mkdirSync(managementArchive, { recursive: true });
for (const name of historicalFiles) {
  const from = path.join(management, name);
  const to = path.join(managementArchive, name);
  if (fs.existsSync(from) && !fs.existsSync(to)) fs.renameSync(from, to);
}
const memberB = path.join(management, 'member-B', 'uc07-remote-audit.md');
const memberBTarget = path.join(managementArchive, 'uc07-remote-audit.md');
if (fs.existsSync(memberB) && !fs.existsSync(memberBTarget)) fs.renameSync(memberB, memberBTarget);
write(path.join(management, 'README.md'), `# 05_management - 项目管理材料

- \`archive/\`：已结束的计划、Issue 草案、PR 说明和阶段性远程审计，仅作历史追溯；
- \`UC16-UC20-screenshots/\`：历史人工走查截图，不替代真实 E2E；
- 当前完成状态以 GitHub Project、最新 CI 和 \`midterm-audit.md\` 为准。

归档文件中的旧路径和 \`PENDING\` 描述保留当时语境，不作为当前状态。
`);

const rootReadme = path.join(root, 'README.md');
let readme = read(rootReadme);
if (!readme.includes('## 中期检查交付入口')) {
  readme = readme.replace('## 功能概览', `## 中期检查交付入口

- [中期交付说明](MIDTERM_DELIVERY.md)
- [文档索引](02_docs/README.md)
- [课程对照审计](05_management/midterm-audit.md)
- [测试与原始证据](04_tests/README.md)

当前六个微服务是目标架构，不等同于全部实现；最新完成状态以课程对照审计为准。

## 功能概览`);
}
write(rootReadme, readme);

console.log('Normalized current UC docs, Domain/DevOps status, management archive, and delivery indexes.');
