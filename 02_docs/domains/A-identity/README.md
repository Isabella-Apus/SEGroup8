# A-identity shared design index

Domain-A owns UC01-UC05 identity and governance behavior. Existing UC design
documents remain the source of component-level detail:

- [UC01 design](../../UC01-注册登录与角色鉴权-设计.md)
- [UC02 design](../../UC02-用户资料与地址-设计.md)
- [UC03 design](../../UC03-商家申请与审核-设计.md)
- [UC04 design](../../UC04-用户封禁解禁与审计-设计.md)
- [UC05 design](../../UC05-举报拉黑与信用治理-设计.md)

Per-UC README, system/component/object model, and traceability files are
maintained with the independently reviewable UC changes; this index does not
duplicate them.

The shared security boundary is covered by the tagged `PLATFORM` test
`JwtAuthInterceptorTest` and the reusable `microservices/security-contract`
module. The contract exposes `JwtPrincipal(uid, username, role)` and rejects
missing, malformed, tampered, expired, or weak-secret JWT configurations.
