# 迁移版本报告

## 版本

- V1：建立规范表、外键和查询索引；可从空 Schema 启动。
- V2（MySQL）：从预先重命名的 `legacy_*` 原型表一次性导入规范表；clean install 会安全跳过。
- V6：为规范商品表补齐 `cover` 和 `images` 媒体字段，保留旧商品可编辑所需的封面数据。
- 切换：停止旧四服务写入 → 备份 → 重命名原型表 → 执行 Flyway → 数量/金额/库存校验 → 切换 Gateway → 观察 → 移除旧 Deployment。禁止长期双写。

## 执行时填写的核对表

| 实体 | 源数量 | 目标数量 | 关键字段校验 | 抽样主键 | 结论 |
|---|---:|---:|---|---|---|
| shop | 待目标环境采集 | 待采集 | seller/name/status | 待采集 | 待签字 |
| product | 待目标环境采集 | 待采集 | price/stock/status | 待采集 | 待签字 |
| audit/history/keyword | 待采集 | 待采集 | 状态/时间/计数 | 待采集 | 待签字 |

改造前 tag 要求为 `monolith-start`，改造后 tag 要求为 `microservices-v1`。仓库目前的实际 SHA/tag 必须由发布负责人在合并时填写，本文不伪造尚未产生的 PR、merge SHA 或 tag。
