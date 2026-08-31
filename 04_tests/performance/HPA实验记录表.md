# UC16-UC20 HPA 与性能实验记录

本记录同时保留 Day8 HPA 预实验、Day9 本地 HPA 正式实验，以及 D 域可独立完成的二手拍卖单体/微服务三轮对比。
商品搜索和新品下单依赖 B/C 微服务及全队统一数据环境，仍单独标记为共享待办。

## 固定实验条件

| 字段 | 记录值 |
|---|---|
| 测试机器 | Windows 11 + Docker Desktop，同一主机顺序执行 |
| k6 运行方式 | `grafana/k6` Docker 镜像 |
| 单体基线 | `monolith-start@2d39751c` |
| 微服务验证提交 | `137f2293edd24eb07ad6a7ec229082b1f4940d0d` |
| 拍卖数据重置 | `reset-performance-data.ps1` / `reset-secondhand-service-performance-data.ps1` |
| HPA 环境 | Docker Desktop Kubernetes v1.34.3 + Metrics Server v0.9.0 |
| 正式 HPA 配置 | CPU request 100m；目标 70%；min 1；max 4；缩容稳定窗口 60s |
| 结果目录 | `04_tests/performance/results/` 与 `04_tests/microservices/secondhand-service/evidence/hpa/` |

## HPA 记录

| 阶段 | 接口 | VUS | Duration | 初始 | 峰值 | 峰值 Ready | 最终 Ready | RPS | Avg | P95 | 错误率 | 采样峰值 | 证据 |
|---|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---|---|
| Day8 预实验 | secondhand-list | 15 | 150s | 1 | 3 | 3 | 1 | 102.337 | 148.99 ms | 303.16 ms | 0% HTTP/business/server | 551m CPU；208 MiB | `../microservices/secondhand-service/evidence/hpa/20260830-181116-*` |
| Day9 正式本地实验 | secondhand-list | 15 | 150s | 1 | 4 | 3 | 1 | 71.564 | 208.57 ms | 413.30 ms | 0% HTTP/business/server | 约 506m/Pod；220 MiB | `../microservices/secondhand-service/evidence/hpa/20260830-234945-*` |

正式实验使用唯一镜像 `segroup8/secondhand:hpa-20260830-234945`，由验证提交构建后显式导入 Kubernetes
节点，Deployment 也记录了同一标签和运行时 image ID。HPA 的 Deployment 副本完成 1→4→1；本机高负载时
新增 JVM 启动较慢，负载窗口内峰值 Ready 为 3/4，因此报告不写成 4/4 Ready。全程 10,692 个请求和
32,076 项断言成功，k6 退出码为 0。

## 二手拍卖三轮正式对比

两个版本使用相同脚本、10 VU、30 秒、10 个独立拍卖和同一台主机；每个版本先预热 5 秒，预热不计入正式结果。

| 版本 | 轮次 | 请求数 | RPS | Avg | P95 | Max | HTTP 失败 | 业务成功 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| monolith-start | 1 | 832 | 27.54 | 159.15 ms | 285.46 ms | 556.03 ms | 0% | 100% |
| monolith-start | 2 | 1,042 | 34.48 | 87.03 ms | 146.60 ms | 370.81 ms | 0% | 100% |
| monolith-start | 3 | 1,066 | 35.19 | 80.47 ms | 152.68 ms | 461.61 ms | 0% | 100% |
| secondhand-service | 1 | 1,105 | 36.58 | 70.15 ms | 179.09 ms | 334.44 ms | 0% | 100% |
| secondhand-service | 2 | 1,237 | 40.91 | 41.37 ms | 62.73 ms | 195.78 ms | 0% | 100% |
| secondhand-service | 3 | 1,246 | 41.25 | 39.64 ms | 62.55 ms | 145.67 ms | 0% | 100% |

| 版本 | 三轮平均 RPS | 三轮平均 Avg | 三轮平均 P95 | HTTP 失败 | 业务成功 |
|---|---:|---:|---:|---:|---:|
| monolith-start | 32.40 | 108.88 ms | 194.91 ms | 0% | 100% |
| secondhand-service | 39.58 | 50.38 ms | 101.46 ms | 0% | 100% |

完整分析为 `secondhand-auction-formal-comparison.md`；聚合 CSV/JSON、每轮 metadata、summary、console 和
压缩 raw JSON 使用前缀 `results/20260830-224711-formal-*`。

## 共享待办

| 接口 | 当前状态 | 为什么不能由 D 单独关闭 |
|---|---|---|
| product-search | 待 B 的 catalog-shop-service 和统一数据环境 | 必须比较真实目标微服务，不能拿单体或二手服务代替 |
| new-order-create | 待 C 的 order-service 和统一数据环境 | UC20 履约归 order-service，D 不复制订单状态机 |
| 全队汇总结论 | 待上述两条各完成两个版本三轮 | 三条接口齐全后才能写平台层结论 |

## 结论边界

二手拍卖数据支持描述本机固定条件下的吞吐和延迟差异，但不能外推为整个平台性能。HPA 数据证明控制器能够
扩容并在停压后缩容，同时也暴露本机 JVM 副本 Ready 较慢这一观察；两类实验均保留原始数据，不删除异常现象，
也不在数据不支持时宣称整体性能提升。
