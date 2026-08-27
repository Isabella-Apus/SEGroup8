# UC05 测试计划

## 分层

- MockMvc：验证举报、拉黑、信用和管理员接口的响应契约、参数校验和权限边界。
- H2 集成：从注册用户开始，验证真实 mapper、事务状态和跨表一致性。
- Compose E2E：连接 Docker Compose 的 frontend/backend/MySQL，验证页面刷新后举报记录与信用分仍然存在。

## 命令

```powershell
mvn -B -f backend/pom.xml -Dgroups=DOMAIN_A test
cd frontend
npm.cmd run e2e -- --list
```
