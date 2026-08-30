# secondhand-service 部署失败排查说明

一次典型失败是把数据库 Secret 的用户名写错。流水线执行 Helm 原子升级后，新 Pod 启动，但 readiness 一直不通过，rollout 超时，Helm 自动回滚。

排查时先打开部署 job 上传的诊断 artifact：`helm history` 显示新 revision 为 failed，`kubectl describe deployment/pod` 显示 readiness 失败；再看当前和 previous 容器日志，JSON 日志中的根异常是 MySQL `Access denied`。此时 `/actuator/info` 仍显示旧提交号，证明回滚已生效、线上仍运行旧版本。

修复 Secret 后重新部署，依次确认 rollout 成功、liveness/readiness 为 `UP`、`/actuator/info` 的 commit 等于本次完整 SHA、公开商品列表返回 `code=0`。诊断脚本只读取 Secret 是否存在和日志，不输出 Secret 内容。

这段文字说明的是可复现的排查路径；在生产集群实际演练前，状态仍标记为 `NOT_RUN`，不能用文档代替集群证据。
