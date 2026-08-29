# 成员 E：服务韧性、日志、健康与版本规范

- 父用例：UC25（GitHub #64）
- 子任务：GitHub #94
- 适用范围：后端 HTTP、数据库、WebSocket、定时任务和后续拆分出的所有服务

## 1. 超时、熔断与降级

| 调用类型 | 连接超时 | 总超时 | 熔断统计窗 | 打开条件 | 降级结果 |
| --- | ---: | ---: | --- | --- | --- |
| 同步内部 HTTP | 500 ms | 2 s | 最近 20 次 | 至少 10 次且失败率 >= 50% | 返回统一 `503/SERVICE_DEGRADED` |
| 外部 LLM/风控 | 1 s | 12 s | 最近 20 次 | 至少 10 次且失败率 >= 50% | 转人工审核，禁止自动放行 |
| 数据库事务 | 1 s | 3 s | 由连接池监测 | 连续连接失败或连接池耗尽 | 回滚并返回 `503`，不重试写事务 |
| WebSocket 推送 | 500 ms | 1 s | 不阻塞业务事务 | 单连接发送失败即断开该连接 | 保留数据库通知，重连后补拉 |

规则：

1. 熔断器状态统一为 `CLOSED → OPEN → HALF_OPEN → CLOSED`；`OPEN` 30 秒后仅放行 3 个探测请求。
2. 只有幂等读取可在抖动退避后最多重试 2 次；写入、扣款、核销、结算不做通用自动重试。
3. 上游总超时必须大于下游总超时并预留响应时间，禁止无限等待。
4. 降级必须保守：优惠券校验失败不抵扣，账户校验失败不结算，聊天权限校验失败不发送，通知推送失败保留可补拉记录。
5. 恢复后通过半开探测自动回到正常状态；不得靠人工删除业务数据恢复。

## 2. 结构化日志规范

每条日志至少包含：

```text
timestamp level service version environment traceId requestId userId event outcome durationMs errorCode
```

- HTTP 入口生成或透传 `X-Request-Id`，跨服务透传 `traceId`。
- 安全事件增加 `clientIp`、`resourceType`、`resourceId`，权限拒绝记录原因但不暴露目标数据。
- 财务、优惠券和通知状态变化记录业务 ID、旧状态、新状态和结果，不记录卡号、密码、JWT、Cookie、API Key 或完整请求体。
- `INFO` 记录业务状态变化；`WARN` 记录可恢复降级；`ERROR` 记录需要处理的失败并附异常类型。
- 同一失败只在责任边界记录一次堆栈，避免 Controller、Service、Mapper 重复报错。

## 3. 统一健康、就绪、存活和版本

| 类型 | 路径 | 判定 | 编排动作 |
| --- | --- | --- | --- |
| 综合健康 | `/actuator/health` | 服务总体状态 | 仅供内部监控 |
| 存活 | `/actuator/health/liveness` | `livenessState,ping` | 失败后重启容器 |
| 就绪 | `/actuator/health/readiness` | `readinessState,db` | 失败时摘除流量，不重启 |
| 版本 | `/actuator/info` | name/version/commit/build-time | 发布核对与故障定位 |

响应禁止包含数据库 URL、用户名、密码、Secret 值或异常堆栈。所有服务沿用这些路径和字段，Kubernetes 探针参数见部署模板。

## 4. 验收清单

- `application.yml` 暴露且仅暴露 `health,info`。
- liveness 不依赖数据库；readiness 包含数据库。
- 版本字段由构建/部署环境注入，默认值只用于本地开发。
- Secret 示例中的所有值均为占位符。
- 日志与健康响应不泄露敏感配置。

