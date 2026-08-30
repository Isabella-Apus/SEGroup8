# MS-04 非作者 Review 清单

评审人：Isabella-Apus（已请求 Review，不得为 Chazeynnn）
评审日期：待填写
PR：[#213](https://github.com/Isabella-Apus/SEGroup8/pull/213)

- [ ] 服务只拥有 `secondhand_db`，无跨库 Mapper 或 SQL
- [ ] UC20 履约仍由 order-service 负责
- [ ] `tradeType + tradeId` 和 `orderBusinessKey` 幂等契约正确
- [ ] 直购、议价、拍卖的 CAS 与失败解冻逻辑可读且有测试
- [ ] 风险待审不会错误上架
- [ ] 订单状态事件按 `eventId` 幂等
- [ ] JWT 身份来自验签 token，内部接口校验 service token
- [ ] Docker 非 root，Helm 探针、资源限制和 Secret/ConfigMap 分离正确
- [ ] CI 在 Maven/API/E2E 失败时停止，成功后才发布不可变镜像
- [ ] 未提交 token、密码、私钥或生产 Secret
- [ ] Maven 26/26、独立服务 E2E 4/4 与 Domain D E2E 5/5 证据可打开
- [ ] HPA、三轮拍卖性能对比、订单依赖故障和 Helm 回滚证据结论与原始文件一致
- [ ] 至少提出一条有效评审意见或明确记录“无阻断问题”

评审结论：待非作者 Review 后填写。
