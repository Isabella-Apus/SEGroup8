# UC21 测试计划

目标：验证店铺券和平台券的创建、修改、关闭、删除、归属、权限与参数边界。

| 层级 | 场景 | 自动化入口 | 当前状态 |
|---|---|---|---|
| Unit | 优惠金额、门槛和角色规则 | `VoucherServiceTest`、`UC21VoucherLifecycleRulesTest` | 已执行 |
| API/Integration | 卖家生命周期、管理员平台券、越权 | `VoucherLifecycleUc21IntegrationTest` | 已执行 |
| Browser E2E | 卖家创建→查询→修改→关闭→刷新；买家越权 | `frontend/e2e/domain-e/uc21-voucher-lifecycle.spec.ts` | 已执行 |

边界断言包括：其他卖家不能修改不属于自己的券、已使用券不能删除、关闭券不能领取、普通买家不能调用管理员创建接口。

运行命令：

```bash
cd backend && mvn -B --no-transfer-progress -Dgroups=DOMAIN_E test
E2E_EVIDENCE_ROOT=04_tests/UC21/evidence \
E2E_OUTPUT_DIR=04_tests/UC21/evidence/raw-reports/playwright \
scripts/e2e/run-compose-e2e.sh e2e/domain-e/uc21-voucher-lifecycle.spec.ts --workers=1
```
