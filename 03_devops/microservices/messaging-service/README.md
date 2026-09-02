# messaging-service 运维入口

- 构建：`mvn -B -f microservices/pom.xml -pl messaging-service -am clean verify`
- 日常部署、探针、日志、Inbox/DLQ 处理和回滚：[operations-runbook.md](operations-runbook.md)

运行时 Secret 包含数据库、JWT、内部服务令牌和独立 operations token；`REALTIME_ALLOWED_ORIGIN_PATTERNS` 必须显式配置。生产镜像只使用 `messaging:sha-<完整提交号>`，由独立流水线验证后原样推送并通过共享 Helm release 原子部署。
