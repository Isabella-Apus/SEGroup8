import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(process.cwd());
if (path.basename(root) !== 'SEGroup8' || !fs.existsSync(path.join(root, '.git'))) {
  throw new Error(`Run from SEGroup8 root, got ${root}`);
}

const stories = [
  '作为访客或已注册用户，我希望完成注册、登录并按角色访问系统，以便安全使用授权功能。',
  '作为已登录用户，我希望维护个人资料和收货地址，以便使用准确身份和配送信息完成交易。',
  '作为普通用户，我希望提交商家申请并获知审核结果，以便在获批后经营店铺。',
  '作为管理员，我希望封禁或解禁违规用户并保留审计记录，以便控制账号风险。',
  '作为平台用户和管理员，我希望完成举报、拉黑、审核与信用治理，以便降低恶意交互风险。',
  '作为游客或买家，我希望搜索、筛选并查看商品详情，以便快速找到可购买商品。',
  '作为官方卖家，我希望管理商品的新增、编辑、审核、上下架和库存，以便持续经营商品。',
  '作为店主和访客，我希望维护并查看店铺信息与装修，以便展示和识别真实店铺。',
  '作为管理员，我希望审核商品风险并回写处理结果，以便阻止不合规商品进入交易。',
  '作为已登录用户和运营人员，我希望管理浏览/搜索历史并统计热词，以便改善检索和运营分析。',
  '作为买家，我希望从购物车结算并生成正确拆分的订单，以便购买新品或二手商品。',
  '作为买家，我希望安全支付或取消待付款订单，以便完成交易或及时撤销未付款订单。',
  '作为买卖双方，我希望完成发货、物流跟踪和确认收货，以便闭环履约。',
  '作为买家、卖家或管理员，我希望处理退款、退货和争议仲裁，以便解决售后问题。',
  '作为买家和卖家，我希望提交评价、追评和回复，以便形成可信的交易反馈。',
  '作为二手卖家，我希望发布并管理自己的闲置商品，以便控制商品可售状态。',
  '作为二手买家，我希望直接购买他人正在出售的商品，以便快速形成待履约订单。',
  '作为二手买卖双方，我希望发起并处理议价，以便按双方确认价格成交。',
  '作为二手卖家和竞拍买家，我希望创建拍卖、合法出价并自动结算，以便公平完成竞价交易。',
  '作为二手买卖双方，我希望完成成交后的发货、物流和收货，以便闭环二手订单。',
  '作为官方卖家或管理员，我希望创建和管理优惠券规则，以便开展可控营销活动。',
  '作为买家，我希望领取并在结算时使用符合条件的优惠券，以便获得正确优惠。',
  '作为用户、卖家和结算模块，我希望充值、查询流水并完成账户结算，以便保证资金账实一致。',
  '作为买卖双方，我希望创建会话、保存并接收消息，以便围绕交易持续沟通。',
  '作为登录用户，我希望接收、查询并标记实时通知，以便及时了解业务状态变化。',
];

const read = (p) => fs.readFileSync(p, 'utf8').replaceAll('\r\n', '\n');
const write = (p, text) => fs.writeFileSync(p, text.trimEnd() + '\n', 'utf8');
const escCell = (s) => s.replaceAll('|', '\\|').replaceAll('\n', '<br/>');
const splitItems = (s) => s.split('；').map((item) => item.trim()).filter(Boolean);

// The migration is safe to rerun after the per-UC files have already been
// standardized. In that case only normalize links inside the aggregate copy.
const firstRequirement = path.join(root, '02_docs', 'UC01', 'requirement.md');
if (/^#\s+REQ01\s*\/\s*US01\s*\/\s*UC01\b/m.test(read(firstRequirement))) {
  const masterPath = path.join(root, '02_docs', 'specifications', 'software-requirements.md');
  let master = read(masterPath);
  const detailHeading = '## 5 详细用例说明';
  const detailIndex = master.indexOf(detailHeading);
  if (detailIndex < 0) throw new Error('Detailed use-case section missing from aggregate requirements');
  const aggregateDetails = [];
  for (let n = 1; n <= 25; n += 1) {
    const digits = String(n).padStart(2, '0');
    const prefix = `../UC${digits}/`;
    const detail = read(path.join(root, '02_docs', `UC${digits}`, 'requirement.md'))
      .replace(/^#\s+/, '## ')
      .replaceAll('](system.mmd)', `](${prefix}system.mmd)`)
      .replaceAll('](concept.mmd)', `](${prefix}concept.mmd)`)
      .replaceAll('](component.mmd)', `](${prefix}component.mmd)`)
      .replaceAll('](component-sequence.mmd)', `](${prefix}component-sequence.mmd)`)
      .replaceAll('](object.mmd)', `](${prefix}object.mmd)`)
      .replaceAll('](object-sequence.mmd)', `](${prefix}object-sequence.mmd)`)
      .replaceAll('](traceability.md)', `](${prefix}traceability.md)`);
    aggregateDetails.push(detail);
  }
  master = `${master.slice(0, detailIndex)}${detailHeading}\n\n${aggregateDetails.join('\n\n---\n\n')}`;
  write(masterPath, master);
  console.log('Requirements were already standardized; aggregate model links normalized.');
  process.exit(0);
}

const parsed = [];
for (let n = 1; n <= 25; n += 1) {
  const digits = String(n).padStart(2, '0');
  const p = path.join(root, '02_docs', `UC${digits}`, 'requirement.md');
  const text = read(p);
  const heading = text.match(/^#\s+REQ\d+\s*\/\s*UC\d+\s+(.+)$/m);
  if (!heading) throw new Error(`Legacy requirement heading missing: ${p}`);
  const values = {};
  for (const match of text.matchAll(/^\|\s*(参与者|触发与前置条件|主成功流程|备选\/异常流程|可验证结果)\s*\|\s*(.*?)\s*\|$/gm)) {
    values[match[1]] = match[2];
  }
  for (const key of ['参与者', '触发与前置条件', '主成功流程', '备选/异常流程', '可验证结果']) {
    if (!values[key]) throw new Error(`${key} missing in UC${digits}`);
  }
  parsed.push({ n, digits, title: heading[1].trim(), ...values });
}

const primaryActor = (actors) => actors.split('、')[0].trim();
const secondaryActors = (actors) => actors.split('、').slice(1).join('、') || '无';
const triggerAndPreconditions = (text) => {
  const items = splitItems(text);
  return { trigger: items[0], preconditions: items.slice(1).length ? items.slice(1) : ['参与者身份、相关资源和依赖服务处于可用状态'] };
};
const specialRequirements = (n) => {
  const security = '- 安全性：所有受保护操作必须校验 JWT、角色和资源归属；禁止仅信任客户端传入的用户标识。';
  const audit = '- 可追溯性：关键状态变化必须保留操作者、时间、结果和 traceId，失败也要留下可定位记录。';
  if ([6, 8, 10, 24, 25].includes(n)) {
    return [security, '- 性能与可用性：查询应分页或限制结果规模；实时能力失败时保留可回读数据，不丢失已提交事实。', audit];
  }
  return [security, '- 一致性与幂等：写操作在事务边界内完成；重复请求、并发冲突和下游超时不得造成重复写入或半完成状态。', audit];
};

const renderRequirement = (item, linkPrefix = '') => {
  const uc = `UC${item.digits}`;
  const req = `REQ${item.digits}`;
  const us = `US${item.digits}`;
  const actor = primaryActor(item.参与者);
  const secondary = secondaryActors(item.参与者);
  const { trigger, preconditions } = triggerAndPreconditions(item.触发与前置条件);
  const mainClauses = splitItems(item.主成功流程);
  const alternatives = splitItems(item['备选/异常流程']);
  const results = splitItems(item.可验证结果);
  const mainSteps = [
    `${actor} 触发用例：${trigger}。`,
    '系统校验参与者身份、角色、输入参数、资源归属和用例前置条件。',
    ...mainClauses.map((clause) => clause.replace(/[。；]$/, '') + '。'),
    '系统提交有效变更，返回业务结果和可追踪状态。',
  ];
  const altSteps = alternatives.map((alt, index) => {
    const anchor = Math.min(2 + index, Math.max(2, mainSteps.length - 1));
    return `- **${anchor}${String.fromCharCode(97 + index)}.** ${alt.replace(/[。；]$/, '')}；系统返回明确原因，不保留不一致的业务变更。`;
  });
  const acceptance = results.map((result, index) => `| \`AC${item.digits}-${String(index + 1).padStart(3, '0')}\` | ${escCell(result)} |`);
  return `# ${req} / ${us} / ${uc} ${item.title}

## 用户故事与用例标识

| 项目 | 内容 |
|---|---|
| 需求编号 | \`${req}\` |
| 用户故事编号 | \`${us}\` |
| 用户故事 | ${stories[item.n - 1]} |
| 用例编号 | \`${uc}\` |
| 用例名称 | ${item.title} |

## 用例说明

| 项目 | 内容 |
|---|---|
| 主要参与者 | ${actor} |
| 次要参与者/协作系统 | ${secondary} |
| 基本描述 | ${stories[item.n - 1].replace(/^作为.+?，我希望/, '').replace(/，以便/, '，从而')} |
| 触发条件 | ${trigger} |

### 前置条件

${preconditions.map((condition) => `- ${condition.replace(/[。；]$/, '')}。`).join('\n')}

### 后置条件

${results.map((result) => `- ${result.replace(/[。；]$/, '')}。`).join('\n')}

### 基本事件流

${mainSteps.map((step, index) => `${index + 1}. ${step}`).join('\n')}

### 备选/异常事件流

${altSteps.join('\n')}

### 特殊需求

${specialRequirements(item.n).join('\n')}

### 验收标准

| 验收编号 | 可验证结果 |
|---|---|
${acceptance.join('\n')}

## 系统级模型

- 模型编号：\`SYS-*${item.digits}\`
- Mermaid 源码：[system.mmd](${linkPrefix}system.mmd)
- 组件级模型：[component.mmd](${linkPrefix}component.mmd)
- 对象级模型：[object.mmd](${linkPrefix}object.mmd)
- 追溯矩阵：[traceability.md](${linkPrefix}traceability.md)
`;
};

for (const item of parsed) {
  write(path.join(root, '02_docs', `UC${item.digits}`, 'requirement.md'), renderRequirement(item));
}

const actorDiagram = `flowchart LR
  Guest["<<actor>><br/>游客"]
  User["<<actor>><br/>普通用户/买家"]
  Seller["<<actor>><br/>官方卖家/二手卖家"]
  Admin["<<actor>><br/>管理员"]
  Clock["<<external>><br/>系统时钟/外部事件"]

  subgraph KG["Kinda Goods 系统边界"]
    direction TB
    subgraph A["Domain A 身份与治理"]
      UC01(("UC01 注册登录鉴权"))
      UC02(("UC02 资料与地址"))
      UC03(("UC03 商家申请"))
      UC04(("UC04 封禁解禁审计"))
      UC05(("UC05 举报拉黑信用"))
    end
    subgraph B["Domain B 商品与店铺"]
      UC06(("UC06 搜索筛选详情"))
      UC07(("UC07 商品生命周期"))
      UC08(("UC08 店铺设置装修"))
      UC09(("UC09 商品风险审核"))
      UC10(("UC10 浏览搜索热词"))
    end
    subgraph C["Domain C 订单履约"]
      UC11(("UC11 购物车结算"))
      UC12(("UC12 支付取消"))
      UC13(("UC13 发货收货"))
      UC14(("UC14 售后仲裁"))
      UC15(("UC15 评价回复"))
    end
    subgraph D["Domain D 二手交易"]
      UC16(("UC16 二手发布管理"))
      UC17(("UC17 二手直购"))
      UC18(("UC18 议价"))
      UC19(("UC19 拍卖"))
      UC20(("UC20 二手履约"))
    end
    subgraph E["Domain E 权益财务与消息"]
      UC21(("UC21 优惠券管理"))
      UC22(("UC22 领券核销"))
      UC23(("UC23 钱包结算"))
      UC24(("UC24 会话消息"))
      UC25(("UC25 通知推送"))
    end
  end

  Guest --- UC01
  Guest --- UC06
  Guest --- UC08
  User --- UC02
  User --- UC03
  User --- UC05
  User --- UC10
  User --- UC11
  User --- UC12
  User --- UC13
  User --- UC14
  User --- UC15
  User --- UC16
  User --- UC17
  User --- UC18
  User --- UC19
  User --- UC20
  User --- UC22
  User --- UC23
  User --- UC24
  User --- UC25
  Seller --- UC07
  Seller --- UC08
  Seller --- UC09
  Seller --- UC13
  Seller --- UC14
  Seller --- UC15
  Seller --- UC16
  Seller --- UC18
  Seller --- UC19
  Seller --- UC20
  Seller --- UC21
  Seller --- UC23
  Seller --- UC24
  Admin --- UC03
  Admin --- UC04
  Admin --- UC05
  Admin --- UC09
  Admin --- UC14
  Admin --- UC21
  Admin --- UC23
  Clock --- UC13
  Clock --- UC14
  Clock --- UC19
  Clock --- UC21
  Clock --- UC25`;

write(path.join(root, '02_docs', 'diagrams', 'system-use-case.mmd'), actorDiagram);

const indexRows = parsed.map((item) => {
  const actor = primaryActor(item.参与者);
  return `| \`REQ${item.digits}\` | \`US${item.digits}\` | \`UC${item.digits}\` | ${actor} | [${item.title}](../UC${item.digits}/requirement.md) |`;
}).join('\n');
const detailed = parsed.map((item) => {
  const body = renderRequirement(item, `../UC${item.digits}/`).replace(/^#\s+/, '## ');
  return body;
}).join('\n\n---\n\n');

write(path.join(root, '02_docs', 'specifications', 'software-requirements.md'), `# Kinda Goods 软件需求说明书

> 文档基线：2026-08-28，代码基线 \`09db0eed\`
> 验收范围：\`REQ01\`–\`REQ25\` / \`US01\`–\`US25\` / \`UC01\`–\`UC25\`
> 权威入口：单用例材料位于 \`../UC01/\`–\`../UC25/\`；本文件是便于中期检查的汇总视图。

## 1 编号和写作规则

- \`REQxx\`：稳定需求编号，表示必须交付的业务能力；
- \`USxx\`：用户故事编号，采用“作为……我希望……以便……”格式说明价值；
- \`UCxx\`：完整业务用例编号，一个用例从参与者触发到产生可验证结果；
- \`ACxx-yyy\`：验收标准编号，必须能追到测试编号和运行结果；
- \`SYS-*xx / COMP-*xx / OBJ-*xx\`：系统级、组件级和对象级模型编号。

页面、按钮、单个 API 或数据库操作不是独立业务用例。备选和异常路径使用与基本事件流对应的步骤号，例如 \`2a\`、\`3a\`。

## 2 项目范围与参与者

Kinda Goods 面向校园和社区交易，包含官方商城、个人闲置交易、卖家工作台和管理后台。主要参与者为游客、普通用户/买家、官方卖家/二手卖家和管理员；系统时钟、支付/结算和实时通道作为协作系统或外部事件源。

## 3 系统用例图

图中参与者位于系统边界外，椭圆节点表示端到端业务目标。源码同时保存在 [system-use-case.mmd](../diagrams/system-use-case.mmd)。

\`\`\`mermaid
${actorDiagram}
\`\`\`

## 4 需求、用户故事与用例索引

| 需求编号 | 用户故事编号 | 用例编号 | 主要参与者 | 用例名称与说明 |
|---|---|---|---|---|
${indexRows}

## 5 详细用例说明

${detailed}
`);

console.log('Standardized REQ/US/UC/AC numbering, system use-case diagram, and 25 detailed use-case descriptions.');
