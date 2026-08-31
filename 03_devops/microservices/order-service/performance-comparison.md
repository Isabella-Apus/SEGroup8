# 改造前后性能对比方案

同一 MySQL 8.4 数据规模、同一 k6 脚本、相同 CPU/内存 limit，分别测 `monolith-start` 和本分支 merge SHA。每版本预热 1 次、正式运行至少 3 次；记录 RPS、p50/p95/p99、错误率、CPU、内存、连接池和数据库慢查询。

判定门槛：错误率 < 1%，p95 不劣化超过 20%，无超卖/重复订单；若吞吐提升但尾延迟或错误率退化，不判定通过。原始 JSON 写入 `evidence/performance/`，三轮中位数写入 `performance-report.md`。

当前状态：未运行。原因是本地没有启动同规模 MySQL、k6 和两个可部署版本；不得用估算值替代实测。
