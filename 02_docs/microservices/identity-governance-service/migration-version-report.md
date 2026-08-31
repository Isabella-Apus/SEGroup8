# 改造前后版本记录

## 改造前

- 只读基线：`monolith-start` = `2d39751cbda8d4e6d6b4a10565a9f9f9e266f119`（禁止移动或覆盖）。
- 当前任务起点：`main` 的 `bb72290c`。
- Domain A 路径：单体 `backend` 控制器、Service、Mapper 和共享 `schema.sql`。
- 基准 E2E：`frontend/e2e/domain-a/uc01-uc05*.spec.ts`，本次没有复制或改写；2026-08-30 在独立单体栈复跑 5/5 PASS（9.9 秒）。

## 改造后

- 分支：`feature/ms-identity-governance`。
- 实现提交：`cdb09c19`（服务源码、测试、Docker 与第一版完整交付）。
- 独立模块：`microservices/identity-governance-service`。
- 独立命令：`mvn -B -f microservices/pom.xml -pl identity-governance-service -am clean verify`。
- 流量切换：`NOT_RUN`；根 Compose/Nginx 仍指向单体，避免未完成的其他五个目标服务被误报为已迁移。
- K3s/Helm 与自动发布：已接入完整系统主流水线；PR 阶段只验证，合并到 `main` 后才推送 ACR 并部署，当前远端 run 状态以 PR 检查为准。
- `microservices-v1` tag、merge commit 与非作者 Review：`NOT_RUN`，由全组六服务验收后统一完成。

数据迁移采用一次性导入加对账，切流后停止单体对身份治理表的写入；禁止长期双写。
