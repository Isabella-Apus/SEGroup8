# MS-04 secondhand-service 测试计划

## 基线与范围

- 改造前基线：tag `monolith-start`，commit `2d39751cbda8d4e6d6b4a10565a9f9f9e266f119`
- 实现基于的主干提交：`bb72290cff96c78ab189468b82db1f8ba3cd9323`
- 工作分支：`feature/ms-secondhand`
- 服务范围：UC16 二手发布、UC17 直接购买、UC18 议价、UC19 拍卖
- 协作回归：UC20 订单履约以及 `OrderStatusChanged.v1`
- 数据边界：只写 `secondhand_db`，不得直接写 `order_db.order_info`

## 测试分层

| 层级 | 目标 | 测试位置 | 通过条件 |
|---|---|---|---|
| Unit | 受控降级响应 | `src/test/java/**/unit/` | 状态和用户提示正确 |
| API | 公开接口、JWT、所有权、非法状态、内部 token | `src/test/java/**/api/` | HTTP 状态和响应数据符合契约 |
| Contract | order-service 请求契约、架构边界 | `src/test/java/**/contract/` | business key 幂等且无跨域 Mapper/接口 |
| Integration | 直购、议价、拍卖并发与恢复 | `src/test/java/**/integration/` | 单一成交、无重复订单、失败可恢复 |
| MySQL | Flyway、表归属、跨库权限 | `MySqlSchemaOwnershipIntegrationTest` | 7 张自有表存在，跨库插入被拒绝 |
| E2E | UC16-UC20 浏览器真实流程 | `frontend/e2e/domain-d/` | 5 个 spec 全部通过 |
| Delivery | Docker、Helm、CI 门禁 | Dockerfile、Helm、workflow | 镜像非 root，模板有效，失败阻断发布 |

## 执行命令

```powershell
mvn -B --no-transfer-progress -f microservices/pom.xml -pl secondhand-service -am clean verify
docker build -f microservices/secondhand-service/Dockerfile -t segroup8/secondhand:local microservices
docker run --rm --entrypoint sh segroup8/secondhand:local -c "id -u; test -r /app.jar"
docker run --rm -v "${PWD}/deploy/helm:/charts" alpine/helm:3.16.3 lint /charts/segroup8 --set secondhand.enabled=true
docker run --rm -v "${PWD}/deploy/helm:/charts" alpine/helm:3.16.3 template test /charts/segroup8 --set secondhand.enabled=true
cd frontend
npx playwright test e2e/domain-d
```

## 证据规则

- Maven 原始 XML/TXT 放在 `evidence/raw-reports/surefire/`。
- Git 长期只保留 Playwright JSON、JUnit XML、摘要和关键失败截图/上下文；完整 HTML、重复截图、trace、video 与 Compose 日志由 Actions artifact 保存。
- 只报告实际执行结果；GitHub Actions、ACR digest、真实 Kubernetes revision 和回滚截图必须在对应环境执行后补录。
