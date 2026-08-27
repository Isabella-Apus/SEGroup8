# Domain B: Catalog and Shop

Domain B covers UC06-UC10 and owns catalog search, seller product lifecycle,
shop maintenance, product risk review, and user behavior history.

## Runtime modules

| Use case | Runtime module | Integration test tag |
| --- | --- | --- |
| UC06 | `catalog-service` | `UC06` |
| UC07 | `catalog-service` | `UC07` |
| UC08 | `shop-service` | `UC08` |
| UC09 | `risk-service` | `UC09` |
| UC10 | `behavior-service` | `UC10` |

All five use cases also carry the `DOMAIN_B` JUnit tag. `security-contract`
is a shared contract and is not counted as a Domain B use case runtime module.

The diagrams and traceability owned by each use case remain under
`02_docs/UCxx/`. This page is the shared index and does not duplicate those
sources of truth.

## Verification layers

- API Integration uses Spring Boot, MockMvc, and H2. It verifies controller,
  service, and persistence behavior without launching a browser.
- Browser E2E uses Playwright against the Compose frontend, backend, and MySQL.
  It must not use the Vite mock mode.

Operational commands and CI behavior are documented in
`03_devops/domains/domain-b.md`.
