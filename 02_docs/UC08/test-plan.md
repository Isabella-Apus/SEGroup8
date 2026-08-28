# UC08 测试计划

## 验收目标

验证“店铺查看、设置和装修”的主成功流程、至少一个异常或权限分支，以及页面、接口和数据库结果的一致性。

## 自动化范围

| 编号 | 层级 | 入口 | 通过条件 |
|---|---|---|---|
| `UNIT-TC08-001` | 单元/服务 | 见 [traceability.md](traceability.md) 中的测试类 | 关键业务规则与异常分支均有断言 |
| `INT-TC08-001` | 集成/API | 见 [traceability.md](traceability.md) 中的测试类 | HTTP、数据库状态和权限边界一致 |
| `E2E-TC08-001` | Compose + MySQL + Playwright | `frontend/e2e/domain-b/uc08-shop.spec.ts` | 完整业务链路成功，失败为非零退出码并保留原始证据 |

## 执行入口

```powershell
$env:E2E_EVIDENCE_ROOT = '04_tests/UC08/evidence'
$env:E2E_OUTPUT_DIR = '04_tests/UC08/evidence/raw-reports/playwright'
.\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase -- e2e/domain-b/uc08-shop.spec.ts --workers=1
```

测试源码留在框架默认目录；本计划不复制 Java 或 Playwright 源码。只有 Surefire/Playwright 机器结果、日志、截图和真实退出码可以支撑 PASS。
