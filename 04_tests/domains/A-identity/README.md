# Domain-A evidence index

状态：共享基础设施与 Domain-A 定向入口已完成；真实 Compose/MySQL 浏览器验收
因本机 Docker Linux daemon 不可用而未运行。

Shared evidence and test-boundary documentation belongs here. Business
acceptance evidence belongs in `04_tests/UC01` through `04_tests/UC05` so each
Task can be reviewed independently.

The Compose runner writes logs and Playwright artifacts under the selected UC
evidence directory when `E2E_OUTPUT_DIR` is set. Platform smoke evidence
continues to live under `04_tests/platform-e2e/evidence`.

The shared JWT boundary is reported as `PLATFORM`, not duplicated into every
UC. The final Domain-A gate is:

```powershell
mvn -B -f backend/pom.xml -Dgroups=DOMAIN_A test
```

共享 CI：
`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`

该 workflow 在每个 Pull Request 上执行 Domain-A 定向测试、完整后端回归、真实
前端构建以及统一 Compose + MySQL + Playwright E2E。每个 UC 的业务报告和
Evidence 仍留在各自目录；`PLATFORM` JWT/interceptor 和 security-contract
放在共享层，不重复计入 UC 业务测试。

本地记录（2026-08-27）：Domain-A 65 tests PASS；后端 `clean verify` 127 tests
PASS；microservices JWT contract 5 tests PASS；前端 `npm ci` 和 `npm run
build:real` PASS；Compose 配置检查 PASS；Docker Compose 浏览器运行 NOT_RUN。
