# A-identity shared design index

Domain-A owns UC01-UC05 identity and governance behavior. Existing UC design
documents remain the source of component-level detail:

- [UC01 标准材料](../../UC01/README.md)
- [UC02 标准材料](../../UC02/README.md)
- [UC03 标准材料](../../UC03/README.md)
- [UC04 标准材料](../../UC04/README.md)
- [UC05 标准材料](../../UC05/README.md)

Per-UC README, system/component/object model, and traceability files are
maintained with the independently reviewable UC changes; this index does not
duplicate them.

The shared security boundary is covered by the tagged `PLATFORM` test
`JwtAuthInterceptorTest` and the reusable `microservices/security-contract`
module. The contract exposes `JwtPrincipal(uid, username, role)` and rejects
missing, malformed, tampered, expired, or weak-secret JWT configurations.

The A-E labels are delivery/test domains, not a requirement to deploy exactly
five services. The reviewed runtime boundary, offline JWT dependency model,
complete API ownership, and table ownership are maintained in:

- [Microservice boundaries](../../architecture/microservice-boundaries.md)
- [Service API catalog](../../architecture/service-api-catalog.md)
- [Database ownership](../../architecture/database-ownership.md)

Login is a business prerequisite for protected use cases, but other services
must validate a signed token locally instead of calling Domain A on every
request. Ban/role changes propagate through short-lived tokens and versioned
access-change events as described in the architecture source above.
