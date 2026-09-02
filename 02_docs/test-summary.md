# Kinda Goods 最终测试汇总

## 1. 已验证运行基线

功能与生产部署基线：`main` 提交 `b622e6bbb0447d6823b50e7789e4777f7131eb9b`。以下是该提交实际完成的 Actions 运行，不用文档或配置存在替代执行结果。

| 流水线 | 结果 | Run |
|---|---|---|
| 完整系统 CI/CD | PASS | [33526387696](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33526387696) |
| Identity Governance CI/CD | PASS | [33526387419](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33526387419) |
| Order Service CI/CD | PASS | [33526387441](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33526387441) |
| Secondhand Service CI/CD | PASS | [33526387403](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33526387403) |
| Catalog-Shop CI/CD | PASS | [33526387391](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33526387391) |
| Messaging CI/CD | PASS | [33526387412](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33526387412) |
| Benefits-Finance CI/CD | PASS | [33526387386](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33526387386) |

完整系统流水线包含后端测试、前端构建、Domain A–E、UC01–UC25 Playwright、候选镜像、Helm 校验和 K3s 部署。六个服务流水线各自包含 Maven/真实 MySQL、公开 API、独立 E2E、相关完整系统用例、候选镜像、Helm 和发布部署。完整 HTML、trace 与 video 由 Actions artifact 保存 14 天；Git 只保留 JSON/XML、摘要和关键证据。

## 2. 用例覆盖

| Domain | UC | Playwright 入口 | 当前结论 |
|---|---:|---:|---|
| A identity | UC01–UC05 | 5/5 | CI E2E PASS |
| B catalog-shop | UC06–UC10 | 5/5 | CI E2E PASS / Actions artifact |
| C order | UC11–UC15 | 5/5 | CI E2E PASS |
| D secondhand | UC16–UC20 | 5/5 | CI E2E PASS |
| E finance/messaging | UC21–UC25 | 5/5 | CI E2E PASS |

所有公开后端接口均由对应服务 API/契约测试覆盖；浏览器 E2E 从真实前端和路由进入服务。MockMvc、H2 或静态路由检查只作为较低层测试，不替代真实 MySQL 和 Compose/浏览器 E2E。

## 3. 云实验与性能

2026-09-02 在云服务器用同一功能基线重跑：

- 完整系统 HPA：`2 → 4（4 Ready）→ 2`，固定/HPA 共六个计量窗口错误率均为 0；
- Order 依赖故障：故障期 HTTP 202/`RETRY`，二手探针 UP；Order 恢复后自动 `CREATED`，重复请求后仍恰好一单；
- 故障窗口生产八个入口健康检查全部 HTTP 200，生产 Order endpoints 存在；
- 改造前后性能：3 个接口，单体与微服务各 3 轮，正式原始数据保留。

证据见：

- `04_tests/cloud-native-experiments/20260902-system-hpa-b622e6bb/`
- `04_tests/cloud-native-experiments/20260902-order-fault-b622e6bb/`
- `04_tests/cloud-native-experiments/20260830-2300-ef71f1fe/performance/`

## 4. 当前部署验证

实验退出后生产命名空间仅保留正式系统，无 HPA 和实验命名空间。前端、兼容后端、MySQL、identity、catalog-shop、order、secondhand、messaging、finance 均 Ready；六个微服务镜像和兼容系统镜像均为 `sha-b622e6...`。liveness、readiness、version 和公开 `/health` 均返回 200。

本次目录与脚本重构会在新的 `main` 提交再次触发相关流水线；该新提交的结果以 GitHub Actions 当前页面为准，不能用上述基线 run 冒充。
