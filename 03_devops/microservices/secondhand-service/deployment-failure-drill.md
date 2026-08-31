# secondhand-service 部署失败与回滚实测

执行时间：2026-08-30 23:39-23:45（UTC+08:00）
环境：Docker Desktop Kubernetes，隔离命名空间 `segroup8-rollback-20260830-233919`

## 演练步骤

1. 安装共享基础 chart，但先关闭 secondhand；创建独立 `secondhand_db`。
2. 构建并导入唯一基线镜像 `segroup8/secondhand:rollback-20260830-232712`。
3. 启用 secondhand，确认 Pod Ready、readiness `UP`、版本和 commit 正确。
4. 将镜像升级为不存在的 `segroup8/secondhand:missing-20260830-233919`，并使用
   `imagePullPolicy=Never`，稳定复现 `ErrImageNeverPull`。
5. 使用 Helm 原子升级和失败回滚，保存退出码、events、describe、resources、status 与 history。
6. 验证旧镜像恢复、readiness `UP`，再删除隔离命名空间。

## 结果

- 失败升级按预期返回退出码 1。
- Helm revision 3 为 `failed`，revision 4 为 `deployed: Rollback to 2`。
- 回滚后镜像恢复为唯一基线镜像，运行时 image ID 为
  `sha256:e352a2c43d10cf7c5f8ab9e4f249351c9f963c32d690b96ec1585d4e58f5dbae`。
- readiness 为 `UP`，版本为 `rollback-baseline`，commit 为
  `137f2293edd24eb07ad6a7ec229082b1f4940d0d`。
- Secret 只使用本次随机测试值，证据不保存明文 Secret。

汇总：`04_tests/microservices/secondhand-service/evidence/deployment-drill/20260830-233919-summary.json`。

## 结论边界

本地真实 Kubernetes 的错误镜像诊断和自动回滚已经完成。生产 ACR 镜像、共享命名空间、生产 Helm revision
和线上健康检查仍必须由 main 流水线生成；本报告不把本地 revision 当作生产部署结果。
