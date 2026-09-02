# 云原生与性能实验最终报告

## 1. 最终结论

正式复测基于 `main` 提交 `b622e6bbb0447d6823b50e7789e4777f7131eb9b`，在单节点 K3s 服务器完成。生产环境当时运行前端、兼容后端、MySQL 和六个微服务，镜像标签均为同一提交的 `sha-b622e6...`。

| 项目 | 结果 | 正式证据 |
|---|---|---|
| 完整系统 HPA | 通过：`2 → 4（4 Ready）→ 2`，六个计量窗口错误率均为 0 | `04_tests/cloud-native-experiments/20260902-system-hpa-b622e6bb/` |
| Order 依赖故障 | 通过：故障期 HTTP 202/`RETRY`、探针 UP，恢复后自动 `CREATED` 且恰好一单 | `04_tests/cloud-native-experiments/20260902-order-fault-b622e6bb/` |
| 改造前后性能 | 完成：3 个接口、两种架构、每个接口各 3 轮 | `04_tests/cloud-native-experiments/20260830-2300-ef71f1fe/performance/` |

旧二手微服务 HPA、失败调参轮次和被新提交替代的恢复轮次已移至仓库外 `selfwork`，不再与正式结论并列。

## 2. 完整系统 HPA

HPA 不能绑定“整个系统”这个抽象对象，必须绑定一个 Deployment。本实验绑定共享系统计算层 `segroup8-backend`，明确禁止把二手微服务作为 HPA 目标。负载从 frontend Service/Nginx 进入共享兼容后端与 MySQL，并混合访问首页、商品、分类、搜索和二手列表；公开入口、全部生产工作负载、节点资源和三层日志一并记录。

正式参数：CPU 目标 60%，`minReplicas=2`、`maxReplicas=4`；扩容每 15 秒最多增加 2 个 Pod，缩容稳定窗口 60 秒。固定 2 副本和 HPA 2..4 都经过预热并使用相同负载。

| 模式 | 并发 | 吞吐 req/s | 平均 ms | P95 ms | 错误率 |
|---|---:|---:|---:|---:|---:|
| 固定 2 | 5 | 19.30 | 256.66 | 1205.14 | 0% |
| 固定 2 | 10 | 28.02 | 354.04 | 2093.66 | 0% |
| 固定 2 | 20 | 37.90 | 520.12 | 3543.86 | 0% |
| HPA 2..4 | 5 | 24.41 | 202.24 | 1089.20 | 0% |
| HPA 2..4 | 10 | 29.27 | 338.39 | 1859.69 | 0% |
| HPA 2..4 | 20 | 33.33 | 584.84 | 3235.61 | 0% |

扩容峰值和 Ready 峰值都为 4，停止负载后回到 2。实验以 `KEEP_OPTIMIZED_HPA=false` 执行，因此退出处理恢复生产原状：后端 1/1 Ready，生产命名空间无 HPA。

`minReplicas=2` 是最终配置建议。改为 1 可节省一个常驻 Pod，但会失去 Pod 级冗余，并把滚动更新、Pod 重启和突发流量的冷启动风险暴露给唯一实例；单节点条件下它不能解决整机故障。

## 3. Order 依赖故障与影响范围

故障只注入隔离命名空间 `segroup8-cloud-exp-20260902-fault-b622e6bb`，将其中 Order 从 1 缩为 0。生产 `segroup8-order` 始终保持 endpoints。

实验结果：

- 二手购买返回 HTTP 202，`requestStatus=RETRY`；
- 二手 liveness、readiness 都为 `UP`，无关列表仍可读；
- 恢复 Order 后后台请求自动进入 `CREATED`；
- 重复购买返回 200，数据库中匹配订单数量仍为 1；
- Order 保存了身份治理返回的收件地址快照；
- 故障窗口内生产 backend、catalog-shop、identity、order、secondhand、messaging、finance、frontend 健康检查全部为 HTTP 200。

如果实际停止生产 Order，各微服务进程不会一起崩溃，但依赖 Order 的业务能力会受限：

| 服务 | 仍可运行 | 受影响能力 |
|---|---|---|
| identity-governance | 身份、用户、地址、治理 | 无直接同步依赖 |
| catalog-shop | 浏览、搜索、店铺、商品、库存 | 无法完成新建单；发往 Order 的库存事件等待重试 |
| benefits-finance | 钱包、优惠券和自身请求 | Order 不再发起新的支付、退款、结算 |
| messaging | 聊天、已有通知、WebSocket | 停机期间没有新的订单事件通知 |
| secondhand | 列表、详情、发布、议价 | 直接购买、拍卖结算进入重试，恢复后补建单 |

课程只要求主动停止一个依赖服务完成一次典型故障实验，不需要机械重复所有依赖对；但所有跨服务调用仍必须有超时、幂等/补偿、鉴权、契约测试和调用说明。

## 4. 改造前后性能

保留的正式性能实验使用同一节点、同一 MySQL 实例的不同 schema、相同 500 条数据、相同 CPU 限额和同一压测器。每个接口在单体和微服务版本各运行 3 次，采用三轮中位数；错误率按总请求加权。

| 接口 | 架构 | 吞吐 req/s | 平均 ms | P95 ms | 错误率 |
|---|---|---:|---:|---:|---:|
| 普通列表 | 单体 | 1.00 | 2971.30* | 4263.64* | 55.13% |
| 普通列表 | 微服务 | 23.69 | 210.63 | 302.73 | 0% |
| 关键字+价格排序 | 单体 | 2.09 | 2355.80 | 2884.43 | 0% |
| 关键字+价格排序 | 微服务 | 47.64 | 104.86 | 185.21 | 0% |
| 详情 | 单体 | 27.29 | 183.08 | 301.28 | 0% |
| 详情 | 微服务 | 205.79 | 24.27 | 77.20 | 0% |

`*` 普通列表的单体前两轮完全超时，平均/P95 仅来自成功样本，所以该行重点看错误率，不把延迟当作三轮中位数。最有代表性的结果是：详情吞吐约 7.54 倍、详情平均延迟下降约 86.74%、关键字查询吞吐约 22.74 倍、关键字查询 P95 下降约 93.58%。这些结论只适用于本次代码、数据、单节点和压力参数。

## 5. 一次部署失败如何定位

环境准备第一次失败时，命名空间尚未创建。先用 `kubectl get namespace` 确认失败发生在写入集群前，再定位脚本停止在读取本地镜像摘要的管道。脚本启用了 `set -euo pipefail`，原来的 `awk` 找到目标后提前退出，使仍在输出的 `ctr images list` 收到 SIGPIPE，整条管道因此返回失败。将 `awk` 改为读到结尾后输出摘要，第二次运行成功创建命名空间，MySQL、单体、身份、Order 和二手依次通过 rollout/readiness。该过程体现“症状 → 失败边界 → 根因 → 修复 → 复验”，不是简单写成重试成功。

## 6. 复现命令

```bash
# 完整系统 HPA；答辩结束恢复原状态
KEEP_OPTIMIZED_HPA=false \
bash scripts/experiments/cloud-native/reproduce_system_hpa_demo.sh

# 隔离 Order 故障
GIT_COMMIT=<commit> \
bash scripts/experiments/cloud-native/prepare_environment.sh <run-id> <host-root>
bash scripts/experiments/cloud-native/run_dependency_fault_experiment.sh \
  <host-root>/state.env dependency-fault-order
bash scripts/experiments/cloud-native/cleanup_environment.sh <host-root>/state.env
```

实验 JAR 放在 `<host-root>/jars/`。Secret、JWT、数据库密码和渲染后清单留在服务器，仓库只保留结构化结果、必要日志、资源清单与脱敏诊断材料。
