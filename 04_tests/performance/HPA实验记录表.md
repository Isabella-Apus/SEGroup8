# UC16-UC20 HPA 实验记录表

本表记录 `monolith-start` 脚本试跑和 Day8 微服务 HPA 预实验。预实验只验证扩缩容链路，不替代 Day9
三个接口、两个版本、每项三轮的正式性能对比。

## 固定实验条件

| 字段 | 记录值 |
|---|---|
| 测试机器 | Windows 11 + Docker Desktop |
| k6 运行方式 | `grafana/k6` Docker 镜像 |
| 数据重置 | `04_tests/performance/reset-performance-data.ps1` |
| 单体基线 | Git 标签 `monolith-start` |
| 结果目录 | `04_tests/performance/results/` |
| HPA 预实验环境 | Docker Desktop Kubernetes v1.34.3 + Metrics Server v0.9.0 |
| HPA 预实验配置 | CPU request 100m；目标 70%；min 1；max 3；缩容稳定窗口 60s |

## 实验记录

| 版本 | 接口 | VUS | Duration | Pod 初始数 | Pod 峰值 | Pod 回落数 | RPS | Avg | P95 | 错误率 | CPU 峰值 | 内存峰值 | 原始结果 |
|---|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| monolith-start | product-search | 2 | 10s | N/A | N/A | N/A | 7.649 | 157.55 ms | 381.26 ms | 0% HTTP / 0% business | 未采集峰值 | 未采集峰值 | `results/20260826-monolith-product-search-run1-*` |
| monolith-start | new-order-create | 2 | 10s | N/A | N/A | N/A | 8.384 | 38.91 ms | 54.35 ms | 0% HTTP / 0% business | 未采集峰值 | 未采集峰值 | `results/20260826-monolith-new-order-create-run1-*` |
| monolith-start | secondhand-buy | 2 | 10s | N/A | N/A | N/A | 8.383 | 35.65 ms | 57.76 ms | 0% HTTP / 86.21% 业务保护 | 未采集峰值 | 未采集峰值 | `results/20260826-monolith-secondhand-buy-run1-*` |
| monolith-start | secondhand-auction-bid | 2 | 10s | N/A | N/A | N/A | 8.069 | 46.25 ms | 58.86 ms | 0% HTTP / 0% business | 未采集峰值 | 未采集峰值 | `results/20260826-monolith-secondhand-auction-bid-run1-*` |
| microservice（Day8 预实验） | secondhand-list | 15 | 150s | 1 | 3 | 1 | 102.337 | 148.99 ms | 303.16 ms | 0% HTTP / 0% business / 0% server | 单 Pod 551m | 单 Pod 208 MiB | `../microservices/secondhand-service/evidence/hpa/20260830-181116-*` |
| microservice（Day9 正式） | product-search |  |  |  |  |  |  |  |  |  |  |  | 待统一部署后完成三轮 |
| microservice（Day9 正式） | new-order-create |  |  |  |  |  |  |  |  |  |  |  | 待统一部署后完成三轮 |
| microservice（Day9 正式） | secondhand-buy / bid |  |  |  |  |  |  |  |  |  |  |  | 待统一部署后完成三轮 |

## Day8 预实验时间点

- 基线采样：2026-08-30 18:13:23 +08:00，1/1 Ready。
- 首次观察到 Deployment 扩容：18:14:06，约 43 秒后达到 3 个副本。
- 首个新增 Pod Ready：18:15:30；18:15:37 达到 3/3 Ready。
- 停压：18:15:58；最终汇总于 18:17:12 观察到 1/1 Ready。
- 14,814 次请求、44,442 项断言全部成功；k6 真实退出码为 0。

## HPA 观察点

- 每 5 秒记录一次 Pod 数量、CPU 和内存，覆盖升压、稳定和回落阶段。
- 单体基线没有 Pod，相关字段统一写 `N/A`，不伪造 HPA 数据。
- 微服务实验必须记录 HPA 配置、`minReplicas`、`maxReplicas`、目标 CPU、首次扩容时间和回落时间。
- 单体与微服务使用相同脚本、数据规模、VUS、持续时间和测试机器后才能比较。
- 当前 CPU/内存文件只是压测后的即时样本，不冒充峰值；正式三轮实验需要增加连续采样。
