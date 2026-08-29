# 部署失败排查演练（本地容器版）

Kubernetes/Helm 故障演练本次 `OUT_OF_SCOPE`。本地等价演练用于证明 readiness 能阻止错误配置被误判为可用。

1. 记录健康基线和当前镜像 SHA。
2. 临时把 `DB_PASSWORD` 改成错误值并重新创建服务容器。
3. 期望服务启动失败或 readiness 为 DOWN；保存容器状态和 JSON 日志，确认日志不含口令。
4. 恢复环境变量中的正确密码，重新创建容器。
5. 等待 readiness 为 UP，运行注册/登录 smoke。

本文件只是可重复步骤。未实际执行前，`deployment-test-report.md` 必须写 `NOT_RUN`，不得把步骤当结果。
