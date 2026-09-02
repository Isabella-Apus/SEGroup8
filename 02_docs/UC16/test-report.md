# UC16 测试报告

## 执行状态

| 验证项 | 当前结果 | 原始证据 |
|---|---|---|
| H2 Integration | PASS：5/5，失败 0，错误 0，跳过 0，23.57 秒 | `evidence/raw-reports/surefire/` |
| MySQL 8 Integration | PASS：5/5，失败 0，错误 0，跳过 0，22.873 秒 | `../../04_tests/UC16/evidence/raw-reports/mysql/` |
| Compose + Playwright | PASS：1/1，失败 0，跳过 0，47.718 秒 | 当前 Actions artifact、关键截图 |

## 已验证结论

Integration 已在 H2 和 Compose MySQL 上分别通过，覆盖真实分类、创建/编辑持久化、上下架、公开可见性、删除、所有权、已售状态和 11 组输入边界。MySQL 执行使用 `secondhand_product`、`category` 与 `product_risk_audit` 真实表，并直接断言数据库结果。

浏览器场景使用真实 Nginx 前端、Spring Boot API、MySQL seed 账号及真实图片上传，不使用 route mock。统一 runner 从空数据库卷开始构建并依次等待 database、backend、frontend 健康，随后完成发布、编辑、上下架、刷新持久化、第三方删除拒绝和本人删除；Playwright 真实退出码为 `0`。

## 回归结果

| 回归项 | 结果 | 日志 |
|---|---|---|
| Domain-D 定向测试 | PASS：25/25，失败 0，错误 0，跳过 0 | Actions artifact |
| 后端全量 `clean verify` | PASS：100/100，失败 0，错误 0，跳过 0 | Actions artifact |
| 前端 `build:real` | PASS：2421 modules transformed | Actions artifact |
| 当前完整系统与服务 E2E | PASS：UC16–UC20 均通过 | Secondhand 流水线 33526387403、完整系统流水线 33526387696 |

## 执行命令

```powershell
Push-Location backend
mvn -B --no-transfer-progress '-Dtest=SecondhandProductManagementIntegrationTest' test
mvn -B --no-transfer-progress '-Dgroups=DOMAIN_D' test
mvn -B --no-transfer-progress clean verify
Pop-Location

Push-Location frontend
npm run build:real
Pop-Location

$env:DOMAIN_D_SUITE = 'UC16'
$env:E2E_EVIDENCE_ROOT = '04_tests/UC16/evidence'
$env:E2E_OUTPUT_DIR = '04_tests/UC16/evidence/raw-reports/playwright'
.\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase -- e2e/domain-d/uc16-product-management.spec.ts --workers=1

```

最终状态以保留的 Surefire XML、结构化 Playwright 结果和当前 Actions 运行一致结论为准。
