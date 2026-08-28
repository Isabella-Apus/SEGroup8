# UC21–UC25 E2E 证据

本目录保存 2026-08-28 定向测试结果。仓库不收录 Surefire XML，避免提交其中的本机用户名、路径和内网地址。

| 文件 | 内容 |
|---|---|
| `raw-reports/com.segroup8.platform.integration.EngagementFinanceApiAndE2ETest.txt` | 五条 E2E 的文本汇总 |
| `raw-reports/com.segroup8.platform.service.VoucherServiceTest.txt` | 优惠券补充单元测试文本汇总 |
| `result-summary.json` | 本次运行命令、环境和统计 |

复现命令：

```powershell
cd backend
mvn -B "-Dtest=VoucherServiceTest,EngagementFinanceApiAndE2ETest" test
```
