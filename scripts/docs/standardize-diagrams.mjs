import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const read = (p) => fs.readFileSync(p, 'utf8').replaceAll('\r\n', '\n').trim();
const write = (p, text) => fs.writeFileSync(p, `${text.trim()}\n`, 'utf8');

const concepts = [
  ['User', 'Credential', 'Role'], ['UserProfile', 'Address'], ['MerchantApplication', 'User', 'Shop'],
  ['User', 'BanRecord', 'AdminAuditLog'], ['UserReport', 'UserBlock', 'CreditScoreLog'],
  ['Product', 'Category', 'Shop'], ['Product', 'Inventory', 'RiskAudit'], ['Shop', 'ShopDecoration', 'Product'],
  ['ProductRiskAudit', 'Product', 'AdminAuditLog'], ['BrowseHistory', 'SearchHistory', 'HotKeyword'],
  ['CartItem', 'OrderInfo', 'OrderItem'], ['OrderInfo', 'PaymentRecord', 'Inventory'],
  ['OrderInfo', 'Shipment', 'LogisticsTrace'], ['AfterSaleRequest', 'OrderInfo', 'RefundRecord'],
  ['Review', 'ReviewAppend', 'SellerReply'], ['SecondhandProduct', 'Category', 'RiskAudit'],
  ['SecondhandProduct', 'OrderInfo', 'OrderItem'], ['Negotiation', 'SecondhandProduct', 'OrderInfo'],
  ['Auction', 'BidRecord', 'OrderInfo'], ['OrderInfo', 'Shipment', 'ReceiptRecord'],
  ['VoucherTemplate', 'VoucherRule', 'Merchant'], ['VoucherTemplate', 'UserVoucher', 'OrderInfo'],
  ['WalletAccount', 'FundTransaction', 'SettlementRecord'], ['Conversation', 'Message', 'ConversationMember'],
  ['Notification', 'NotificationRead', 'RealtimeDelivery'],
];

const participantList = (text) => [...text.matchAll(/^\s*(?:actor|participant)\s+(\w+)\s+as\s+(.+)$/gm)]
  .map((m) => ({ id: m[1], label: m[2].trim() }));
const messages = (text) => [...text.matchAll(/^\s*(\w+)\s*(-->>|->>|-->|->)\s*(\w+)\s*:\s*(.+)$/gm)]
  .map((m) => ({ from: m[1], arrow: m[2], to: m[3], label: m[4].trim(), isReturn: m[2].startsWith('--') }));
const stateToSequence = (text) => {
  if (!text.startsWith('stateDiagram-v2')) return text;
  const transitions = [...text.matchAll(/^\s*(\w+)\s*-->\s*(\w+)\s*:\s*(.+)$/gm)]
    .map((m) => ({ from: m[1], to: m[2], label: m[3].trim() }));
  const nodes = [...new Set(transitions.flatMap((edge) => [edge.from, edge.to]).filter((id) => id !== '[*]'))];
  return `sequenceDiagram\n${nodes.map((id) => `  participant ${id}`).join('\n')}\n${transitions.filter((edge) => edge.from !== '[*]' && edge.to !== '[*]').map((edge) => `  ${edge.from}->>${edge.to}: ${edge.label}`).join('\n')}`;
};
const addAutonumber = (text, id) => {
  if (!text.startsWith('sequenceDiagram')) return `%% ${id}\n${text}`;
  if (/^\s*autonumber\s*$/m.test(text)) return `%% ${id}\n${text}`;
  return `%% ${id}\n${text.replace(/^sequenceDiagram\s*\n/, 'sequenceDiagram\n  autonumber\n')}`;
};
const safeLabel = (s) => s.replaceAll('"', "'");
const shortLabel = (s) => safeLabel(s).replace(/\([^)]*\)/g, '').slice(0, 24).trim() || '调用';
const kind = (label) => {
  if (/\.vue|页面|UI/i.test(label)) return 'boundary';
  if (/Controller|API|网关/i.test(label)) return 'controller';
  if (/Service|服务|Scheduler|Job/i.test(label)) return 'service';
  if (/Mapper|DB|表|Repository|Outbox/i.test(label)) return 'repository';
  return 'collaborator';
};

function componentStructure(seq, digits) {
  const participants = participantList(seq);
  const edges = messages(seq);
  if (!participants.length) {
    const nodes = [...new Set([...seq.matchAll(/^\s*(\w+)\s*--?>\s*(\w+)/gm)].flatMap((m) => [m[1], m[2]]))];
    participants.push(...nodes.map((id) => ({ id, label: id })));
  }
  const known = new Set(participants.map((p) => p.id));
  const unique = new Map();
  for (const edge of edges) {
    if (edge.isReturn || edge.from === edge.to || !known.has(edge.from) || !known.has(edge.to)) continue;
    unique.set(`${edge.from}->${edge.to}`, edge);
  }
  if (!unique.size) {
    for (let i = 0; i + 1 < participants.length; i += 1) {
      unique.set(`${participants[i].id}->${participants[i + 1].id}`, { from: participants[i].id, to: participants[i + 1].id, label: '业务调用' });
    }
  }
  return `%% COMP-STRUCT${digits}\nflowchart LR\n  subgraph Boundary["Kinda Goods 组件边界"]\n${participants.map((p) => `    ${p.id}["«${kind(p.label)}»<br/>${safeLabel(p.label)}"]`).join('\n')}\n  end\n${[...unique.values()].map((e) => `  ${e.from} -->|"${shortLabel(e.label)}"| ${e.to}`).join('\n')}`;
}

function conceptClass(items, digits) {
  const lines = [`%% CONCEPT-CLASS${digits}`, 'classDiagram'];
  for (const name of items) {
    lines.push(`  class ${name} {`);
    lines.push('    +identifier: String');
    lines.push('    +state: String');
    lines.push('  }');
  }
  if (items[1]) lines.push(`  ${items[0]} "1" --> "0..*" ${items[1]} : 关联`);
  if (items[2]) lines.push(`  ${items[1]} "0..*" --> "1" ${items[2]} : 产生/归属`);
  return lines.join('\n');
}

function designClass(objectSeq, digits) {
  const participants = participantList(objectSeq);
  const edges = messages(objectSeq);
  const lines = [`%% DESIGN-CLASS${digits}`, 'classDiagram'];
  for (const p of participants) {
    const role = kind(p.label);
    lines.push(`  class ${p.id}["${safeLabel(p.label)}"] {`);
    lines.push(`    <<${role}>>`);
    if (role === 'boundary') lines.push('    +submit()');
    else if (role === 'controller') lines.push('    +handleRequest()');
    else if (role === 'service') lines.push('    +executeUseCase()');
    else if (role === 'repository') lines.push('    +load()', '    +save()');
    else lines.push('    +applyBusinessRule()');
    lines.push('  }');
  }
  const seen = new Set();
  for (const edge of edges) {
    const key = `${edge.from}->${edge.to}`;
    if (!edge.isReturn && edge.from !== edge.to && !seen.has(key)) {
      lines.push(`  ${edge.from} --> ${edge.to} : ${shortLabel(edge.label)}`);
      seen.add(key);
    }
  }
  return lines.join('\n');
}

for (let n = 1; n <= 25; n += 1) {
  const digits = String(n).padStart(2, '0');
  const dir = path.join(root, '02_docs', `UC${digits}`);
  const systemPath = path.join(dir, 'system.mmd');
  const componentPath = path.join(dir, 'component.mmd');
  const objectPath = path.join(dir, 'object.mmd');
  const componentSequencePath = path.join(dir, 'component-sequence.mmd');
  const objectSequencePath = path.join(dir, 'object-sequence.mmd');

  const system = read(systemPath).replace(/^%% SYS-[^\n]*\n/, '');
  const componentSequenceSource = fs.existsSync(componentSequencePath) ? read(componentSequencePath).replace(/^%% COMP-[^\n]*\n/, '') : read(componentPath);
  const componentSequence = stateToSequence(componentSequenceSource);
  const objectSequence = fs.existsSync(objectSequencePath) ? read(objectSequencePath).replace(/^%% OBJ-[^\n]*\n/, '') : read(objectPath);

  write(systemPath, addAutonumber(system, `SYS-BEH${digits}`));
  write(path.join(dir, 'concept.mmd'), conceptClass(concepts[n - 1], digits));
  write(componentSequencePath, addAutonumber(componentSequence, `COMP-SEQ${digits}`));
  write(componentPath, componentStructure(componentSequence, digits));
  write(objectSequencePath, addAutonumber(objectSequence, `OBJ-SEQ${digits}`));
  write(objectPath, designClass(objectSequence, digits));

  const reqPath = path.join(dir, 'requirement.md');
  let req = read(reqPath)
    .replace('- 模型编号：`SYS-*', '- 模型编号：`SYS-BEH')
    .replace('- Mermaid 源码：[system.mmd](system.mmd)', '- 系统行为模型：[system.mmd](system.mmd)\n- 概念类图：[concept.mmd](concept.mmd)')
    .replace('- 组件级模型：[component.mmd](component.mmd)', '- 组件结构图：[component.mmd](component.mmd)\n- 组件顺序图：[component-sequence.mmd](component-sequence.mmd)')
    .replace('- 对象级模型：[object.mmd](object.mmd)', '- 详细设计类图：[object.mmd](object.mmd)\n- 对象顺序图：[object-sequence.mmd](object-sequence.mmd)');
  write(reqPath, req);

  const readmePath = path.join(dir, 'README.md');
  let readme = read(readmePath)
    .replace('需求、三层设计、追溯和测试文档', '需求、六类图模型、追溯和测试文档')
    .replace(/\n- \[系统级模型\]\(system\.mmd\)\n- \[组件级模型\]\(component\.mmd\)\n- \[对象级模型\]\(object\.mmd\)/, '');
  if (!readme.includes('concept.mmd')) {
    readme += `\n\n## 图模型（按参考文档分层）\n\n- [系统行为模型](system.mmd)\n- [概念类图](concept.mmd)\n- [组件结构图](component.mmd)\n- [组件顺序图](component-sequence.mmd)\n- [详细设计类图](object.mmd)\n- [对象顺序图](object-sequence.mmd)\n`;
  }
  write(readmePath, readme);

  const tracePath = path.join(dir, 'traceability.md');
  let trace = read(tracePath)
    .replace('| 需求 / 用例 | 三层模型 |', '| 需求 / 用例 | 六类图模型 |')
    .replace(/SYS-[A-Z]+\d{2}\s*\/\s*COMP-[A-Z]+\d{2}\s*\/\s*OBJ-[A-Z]+\d{2}/, `SYS-BEH${digits} / CONCEPT-CLASS${digits} / COMP-STRUCT${digits} / COMP-SEQ${digits} / DESIGN-CLASS${digits} / OBJ-SEQ${digits}`)
    .replace('- 系统级模型：[system.mmd](system.mmd)\n- 组件级模型：[component.mmd](component.mmd)\n- 对象级模型：[object.mmd](object.mmd)', '- 系统行为模型：[system.mmd](system.mmd)\n- 概念类图：[concept.mmd](concept.mmd)\n- 组件结构图：[component.mmd](component.mmd)\n- 组件顺序图：[component-sequence.mmd](component-sequence.mmd)\n- 详细设计类图：[object.mmd](object.mmd)\n- 对象顺序图：[object-sequence.mmd](object-sequence.mmd)');
  write(tracePath, trace);
}

const masterTracePath = path.join(root, '02_docs', 'specifications', 'requirements-traceability-matrix.md');
let masterTrace = read(masterTracePath)
  .replace('| 需求 / 用例 | 三层模型 |', '| 需求 / 用例 | 六类图模型 |')
  .replace('UC --> SYS[SYS-*xx 系统级模型]\n  UC --> COMP[COMP-*xx 组件级模型]\n  UC --> OBJ[OBJ-*xx 对象级模型]\n  OBJ --> CODE', 'UC --> SYS[SYS-BEHxx 系统行为]\n  UC --> CONCEPT[CONCEPT-CLASSxx 概念类图]\n  UC --> COMP[COMP-STRUCT/SEQxx 组件模型]\n  UC --> DESIGN[DESIGN-CLASSxx 详细类图]\n  UC --> OBJ[OBJ-SEQxx 对象顺序]\n  OBJ --> CODE');
for (let n = 1; n <= 25; n += 1) {
  const digits = String(n).padStart(2, '0');
  masterTrace = masterTrace.replace(/SYS-[A-Z]+\d{2}\s*\/\s*COMP-[A-Z]+\d{2}\s*\/\s*OBJ-[A-Z]+\d{2}/, `SYS-BEH${digits} / CONCEPT-CLASS${digits} / COMP-STRUCT${digits} / COMP-SEQ${digits} / DESIGN-CLASS${digits} / OBJ-SEQ${digits}`);
}
write(masterTracePath, masterTrace);

const overviewPath = path.join(root, '02_docs', 'specifications', 'software-architecture-design.md');
let overview = read(overviewPath);
for (let n = 1; n <= 25; n += 1) {
  const digits = String(n).padStart(2, '0');
  const seq = read(path.join(root, '02_docs', `UC${digits}`, 'component-sequence.mmd')).replace(/^%%[^\n]*\n/, '');
  const pattern = new RegExp(`(## UC${digits}[^\\n]*\\n\\n)### COMP-[A-Z]+${digits}\\n\\n\\x60\\x60\\x60mermaid\\n[\\s\\S]*?\\n\\x60\\x60\\x60`);
  overview = overview.replace(pattern, `$1### COMP-SEQ${digits}\n\n\x60\x60\x60mermaid\n${seq}\n\x60\x60\x60`);
}
write(overviewPath, overview);

const detailPath = path.join(root, '02_docs', 'specifications', 'software-detailed-design.md');
let detail = read(detailPath);
for (let n = 1; n <= 25; n += 1) {
  const digits = String(n).padStart(2, '0');
  const seq = read(path.join(root, '02_docs', `UC${digits}`, 'object-sequence.mmd')).replace(/^%%[^\n]*\n/, '');
  const pattern = new RegExp(`(## UC${digits}[^\\n]*\\n\\n)### OBJ-[A-Z]+${digits}\\n\\n\\x60\\x60\\x60mermaid\\n[\\s\\S]*?\\n\\x60\\x60\\x60`);
  detail = detail.replace(pattern, `$1### OBJ-SEQ${digits}\n\n\x60\x60\x60mermaid\n${seq}\n\x60\x60\x60`);
}
write(detailPath, detail);

console.log('Standardized 25 UC diagram sets: system behavior, concept class, component structure/sequence, design class, object sequence.');
