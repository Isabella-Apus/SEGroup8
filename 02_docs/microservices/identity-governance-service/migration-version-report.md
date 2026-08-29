# 改造前后版本记录

## 改造前

- 只读基线：`monolith-start` = `2d39751cbda8d4e6d6b4a10565a9f9f9e266f119`（禁止移动或覆盖）。
- 当前任务起点：`main` 的 `bb72290c`。
- Domain A 路径：单体 `backend` 控制器、Service、Mapper 和共享 `schema.sql`。
- 基准 E2E：`frontend/e2e/domain-a/uc01-uc05*.spec.ts`，本次没有复制或改写。

## 改造后

- 分支：`feature/ms-identity-governance`。
- 独立模块：`microservices/identity-governance-service`。
- 独立命令：`mvn -B -f microservices/pom.xml -pl identity-governance-service -am clean verify`。
- 流量切换：`NOT_RUN`；根 Compose/Nginx 仍指向单体，避免未完成的其他五个目标服务被误报为已迁移。
- `microservices-v1` tag、PR、非作者 Review、镜像推送与发布：`NOT_RUN`，由全组六服务验收后统一完成。

数据迁移采用一次性导入加对账，切流后停止单体对身份治理表的写入；禁止长期双写。
