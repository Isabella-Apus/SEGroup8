# UC22 测试计划

目标：验证领券边界、店铺门槛、订单金额、支付核销和刷新持久化。

| 层级 | 场景 | 自动化入口 | 执行条件 |
|---|---|---|---|
| Unit | 店铺小计低于门槛时拒绝并保持用户券不变 | `UC22VoucherThresholdTest` | Maven test profile |
| Integration | 正常领取、重复领取和不可领取状态 | `VoucherClaimUc22IntegrationTest` | H2 |
| Integration | 门槛失败、创建订单、取消释放和支付核销 | `VoucherCheckoutUc22IntegrationTest` | H2 |
| Browser E2E | 页面领券、商品页选券、下单支付、刷新已使用 | `uc22-claim-checkout.spec.ts` | Compose Nginx + Spring Boot + MySQL |

浏览器测试使用 `seller` 创建一次性测试券，使用 `user` 在页面完成业务动作。测试通过环境变量读取账号，不在仓库保存密码或 token。

运行命令：

```bash
cd backend && mvn -B --no-transfer-progress -Dgroups=UC22 test
E2E_EVIDENCE_ROOT=04_tests/UC22/evidence \
E2E_OUTPUT_DIR=04_tests/UC22/evidence/raw-reports/playwright \
scripts/e2e/run-compose-e2e.sh e2e/domain-e/uc22-claim-checkout.spec.ts --workers=1
```
