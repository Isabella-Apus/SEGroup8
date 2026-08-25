---
name: UC06-UC10 微服务交付
about: 商品、店铺、风控与行为服务的实现和验收
title: "[UC06-UC10] 完成商品、店铺、风控和行为微服务"
labels: enhancement, testing, documentation
assignees: ""
---

## 目标

完成 UC06-UC10 的用例、三层模型、四个独立业务服务、独立 Schema、容器/Kubernetes、自动测试和交付证据。

## 验收条件

- [ ] UC06-UC10 用例说明完整，每个用例具有系统级、组件级、对象级图
- [ ] catalog/shop/risk/behavior 四个服务可独立构建
- [ ] 四个服务只能访问自己的 Schema
- [ ] 商品审核与回调失败写入 Outbox；店铺查询在目录故障时降级
- [ ] 关键规则单元测试通过
- [ ] 所有公开接口 API 测试通过
- [ ] UC06、UC07、UC08、UC09、UC10 五条 E2E 通过
- [ ] Dockerfile、Kubernetes 配置和健康探针完整
- [ ] OpenAPI、表归属、跨服务调用和失败处理文档完整
- [ ] CI 上传原始 Surefire 测试报告

## 证据

- 分支：`codex/uc06-uc10-microservices`
- 测试命令：`cd microservices && mvn -B clean verify`
- Kubernetes 校验：`kubectl kustomize microservices/k8s`
