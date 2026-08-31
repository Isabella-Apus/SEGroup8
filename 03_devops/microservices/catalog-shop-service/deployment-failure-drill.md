# 部署失败演练

目标：证明错误 readiness/迁移导致 Helm 原子回滚，并能定位、修复、重发。

1. 在演练分支将 `DB_URL` 指向不存在的 Schema（不要修改真实 Secret），部署一个唯一测试 tag。
2. 保存 `helm status`、`kubectl describe pod`、`kubectl logs --previous`、Flyway 异常到 `04_tests/.../evidence/logs/`。
3. 验证 `helm history` 显示失败 revision，前一正常 revision 仍服务请求。
4. 恢复正确 URL，重新执行 `--atomic --wait`，记录 rollout、readiness 和版本 smoke。
5. 在 `deployment-test-report.md` 填写时间、操作者、revision、根因、恢复耗时和证据相对路径。

本文件是安全演练步骤，不声称尚未在目标 K3s 中执行。
