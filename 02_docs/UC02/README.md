# UC02 用户资料与地址归属

- 需求：`REQ02 / UC02`
- 集成测试：`ProfileAddressUc02IntegrationTest`
- 浏览器测试：`frontend/e2e/domain-a/uc02-profile-address.spec.ts`
- 证据：`04_tests/UC02/evidence/`

测试同时校验资料回读、地址 CRUD、默认地址唯一性、删除后不可见和跨用户
越权保护。浏览器用例通过 Compose 的真实前端页面回读 API 持久化结果。
