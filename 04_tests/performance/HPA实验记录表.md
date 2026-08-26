# UC16-UC20 HPA 实验记录表

本表只定义 Day3/Day4 后续性能实验需要记录的字段。本轮 `monolith-start` 仅验证脚本和数据重置，不填写微服务扩缩容结论。

## 固定实验条件

| 字段 | 记录值 |
|---|---|
| 测试机器 | Windows 11 + Docker Desktop |
| k6 运行方式 | `grafana/k6` Docker 镜像 |
| 数据重置 | `04_tests/performance/reset-performance-data.ps1` |
| 单体基线 | Git 标签 `monolith-start` |
| 结果目录 | `04_tests/performance/results/` |

## 实验记录

| 版本 | 接口 | VUS | Duration | Pod 初始数 | Pod 峰值 | Pod 回落数 | RPS | Avg | P95 | 错误率 | CPU 峰值 | 内存峰值 | 原始结果 |
|---|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| monolith-start | product-search | 2 | 10s | N/A | N/A | N/A | 7.649 | 157.55 ms | 381.26 ms | 0% HTTP / 0% business | 未采集峰值 | 未采集峰值 | `results/20260826-monolith-product-search-run1-*` |
| monolith-start | new-order-create | 2 | 10s | N/A | N/A | N/A | 8.384 | 38.91 ms | 54.35 ms | 0% HTTP / 0% business | 未采集峰值 | 未采集峰值 | `results/20260826-monolith-new-order-create-run1-*` |
| monolith-start | secondhand-buy | 2 | 10s | N/A | N/A | N/A | 8.383 | 35.65 ms | 57.76 ms | 0% HTTP / 86.21% 业务保护 | 未采集峰值 | 未采集峰值 | `results/20260826-monolith-secondhand-buy-run1-*` |
| monolith-start | secondhand-auction-bid | 2 | 10s | N/A | N/A | N/A | 8.069 | 46.25 ms | 58.86 ms | 0% HTTP / 0% business | 未采集峰值 | 未采集峰值 | `results/20260826-monolith-secondhand-auction-bid-run1-*` |
| microservice | product-search |  |  |  |  |  |  |  |  |  |  |  | Day6 后填写 |
| microservice | new-order-create |  |  |  |  |  |  |  |  |  |  |  | Day6 后填写 |
| microservice | secondhand-buy / bid |  |  |  |  |  |  |  |  |  |  |  | Day6 后填写 |

## HPA 观察点

- 每 5 秒记录一次 Pod 数量、CPU 和内存，覆盖升压、稳定和回落阶段。
- 单体基线没有 Pod，相关字段统一写 `N/A`，不伪造 HPA 数据。
- 微服务实验必须记录 HPA 配置、`minReplicas`、`maxReplicas`、目标 CPU、首次扩容时间和回落时间。
- 单体与微服务使用相同脚本、数据规模、VUS、持续时间和测试机器后才能比较。
- 当前 CPU/内存文件只是压测后的即时样本，不冒充峰值；正式三轮实验需要增加连续采样。
