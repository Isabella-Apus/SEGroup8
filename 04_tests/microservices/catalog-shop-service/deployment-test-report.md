# 部署测试报告

Helm 模板、原子发布步骤、readiness/liveness/info 与失败演练已交付。当前本地环境没有目标 K3s 发布上下文，未伪造集群结果。发布负责人须记录 release/revision、镜像 digest、rollout、三个 actuator smoke、失败 revision 的自动回滚和修复 revision。
