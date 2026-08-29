# Issue #65 容器化运行验收

## 追溯关系

| 需求/用例 | 设计与配置 | 实现代码 | 测试 | 运行证据 |
|---|---|---|---|---|
| 前端装入容器并可访问 | `compose.yml`、Nginx 反向代理设计 | `frontend/Dockerfile`、`frontend/docker/nginx.conf` | TC-65-01、TC-65-04 | `evidence/logs/frontend-health.txt`、页面截图 |
| 后端装入容器并可连接数据库 | `compose.yml`、环境变量配置 | `backend/Dockerfile`、`application.yml` | TC-65-02、TC-65-05 | `evidence/logs/backend-health.txt`、`compose-logs.txt` |
| MySQL 装入容器并自动初始化 | Compose 卷与 initdb 挂载设计 | `schema.sql`、`data.sql`、`compose.yml` | TC-65-03、TC-65-06 | `evidence/logs/database-query.txt` |
| 三类容器可统一编排运行 | 服务依赖与健康检查设计 | `compose.yml` | TC-65-01～06 | `evidence/logs/compose-ps.txt`、`images.txt` |

## 测试用例

| 编号 | 测试内容 | 预期结果 |
|---|---|---|
| TC-65-01 | `docker compose config --quiet` | Compose 配置解析成功，退出码为 0 |
| TC-65-02 | 启动 database、backend、frontend | 三个容器均为 running/healthy |
| TC-65-03 | 容器内执行 MySQL 查询 | 可连接 `segroup8_platform`，业务表数量大于 0 |
| TC-65-04 | 请求前端 `/health` 和首页 | `/health` 返回 `ok`，首页返回 HTTP 200 |
| TC-65-05 | 请求后端 `/actuator/health` | HTTP 200，状态为 `UP` |
| TC-65-06 | 查询测试种子数据 | 用户表记录数大于 0 |

## 执行

先运行 `docker compose -f compose.yml up -d --build`，再运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\04_tests\platform-e2e\container-acceptance\collect-evidence.ps1
```

只有脚本打印 `ISSUE-65 ACCEPTANCE: PASS` 且退出码为 0，才能确认“代码、配置、测试、日志/原始报告完整，且容器化用例能够运行”。截图是辅助证据，原始日志和机器可判定的退出码是主要结论依据。

## 本次实测结果

- 执行时间：2026-08-25 15:46（Asia/Shanghai）
- 验收脚本：退出码 0，`ISSUE-65 ACCEPTANCE: PASS`
- 容器：database、backend、frontend 共 3 个，全部 `healthy`
- 数据库：31 张业务表，3 条用户种子数据
- 健康检查：前端 `ok`，后端 `UP`，后端数据库组件 `UP`
- 后端回归测试：42 个，失败 0、错误 0、跳过 0，`BUILD SUCCESS`
- 页面证据：`evidence/screenshots/issue-65-container-frontend.png`
- 机器可读汇总：`evidence/result-summary.json`
