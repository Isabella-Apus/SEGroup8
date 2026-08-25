## 变更摘要

- 完成 UC06-UC10 用例说明及 15 张三层模型图
- 新增 catalog、shop、risk、behavior 四个独立 Spring Boot 服务和独立 Schema
- 新增 Dockerfile、Kubernetes 部署、健康探针及 MySQL 初始化配置
- 新增关键规则单元测试、全部公开接口 API 测试和 5 个用例 E2E

Closes #请替换为实际Issue编号

## 验证

- [ ] `cd microservices && mvn -B clean verify`
- [ ] `kubectl kustomize microservices/k8s`
- [ ] CI 原始 Surefire 报告已上传
- [ ] 未提交真实密码、Token 或云平台密钥

## 评审重点

- 商品生命周期是否拒绝非法转换和越权操作
- 服务是否存在跨 Schema 查询
- 风控请求/回调失败是否保持可恢复
- 店铺读取在 catalog-service 故障时是否正确降级
- 公开接口是否全部具有自动化协议测试
