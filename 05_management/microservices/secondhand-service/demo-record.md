# MS-04 演示记录

## 本地已完成

- [x] 独立 Maven 构建与 26 项自动测试（security-contract 5 + secondhand-service 21）
- [x] MySQL 8.4 Flyway 与跨库写拒绝
- [x] UC16-UC20 Compose + Playwright 5/5
- [x] order-service 超时、恢复、达到上限后解冻的自动化演示
- [x] 非 root Docker 镜像构建
- [x] Helm lint 与 template
- [x] HPA Day8 本地预实验：1→3→1，峰值 Ready 3，14,814 请求零错误
- [x] HPA Day9 正式本地实验：唯一当前镜像，1→4→1，峰值 Ready 3，10,692 请求零错误
- [x] 二手拍卖出价：单体与微服务各三轮，同机同数据同脚本，六轮均零 HTTP/业务失败
- [x] 真实 HTTP 订单依赖停止、恢复和重试耗尽：readiness 保持 UP，无重复请求或脏状态
- [x] 本地 Kubernetes 错误镜像部署、events/describe/history 排查和 Helm 自动回滚

## PR/CI 已完成

- [x] 历史 GitHub Actions 失败阻断：[Microservices CI/CD #33297309271](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33297309271)
- [x] 历史修复后流水线通过：[Microservices CI/CD #33298170339](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33298170339)、[Kinda Goods CI/CD #33298170436](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33298170436)
- [ ] 当前 HPA 与独立服务验收整合提交的 Actions（推送后填写）

正式 HPA 原始证据：`04_tests/microservices/secondhand-service/evidence/hpa/20260830-234945-*`。
二手拍卖正式数据：`04_tests/performance/results/20260830-224711-formal-*`。商品搜索和新品下单仍需 B/C 的
目标微服务与统一数据环境，不能由 D 使用替代实现提前关闭。

## 集群阶段待完成

- [ ] ACR `sha-<git-sha>` 镜像 digest
- [ ] Kubernetes rollout 和探针结果
- [ ] 全队真实 order-service 停止/恢复演示（D 已完成独立 HTTP 契约依赖演练）
- [ ] 生产环境错误镜像/回滚截图（本地隔离 Kubernetes 原始日志已完成）
- [ ] 非作者评审结论

演示负责人：Chazeynnn。集群演示时间与评审人由组长安排。
