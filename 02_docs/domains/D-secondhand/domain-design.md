# Domain D: secondhand trade design entry

## Scope

Domain D owns UC16-UC20:

| UC | Capability | Current monolith entry |
|---|---|---|
| UC16 | Publish and manage secondhand products | `SecondhandProductController` / `SecondhandProductServiceImpl` |
| UC17 | Direct purchase with self-purchase protection | `SecondhandProductController.buySecondhandProduct` |
| UC18 | Bargain request, accept, and reject | `SecondhandTradeController` / `SecondhandTradeServiceImpl` |
| UC19 | Auction creation, bidding, and settlement | `SecondhandTradeController` / auction scheduler |
| UC20 | Seller shipping and buyer receipt | `OrderController` / `OrderServiceImpl` |

The current baseline remains a Spring Boot monolith. The future service boundary is `secondhand-trade-service`; order creation and fulfillment remain explicit collaborations with the order domain. This document records the current boundary and does not claim that the microservice migration is complete.

## Design Sources

- Consolidated system, component, and object diagrams: `02_docs/UC16-UC20-用例说明与三层模型.md`
- Per-UC component diagrams: `02_docs/UC16-二手商品发布管理-组件级图.md` through `02_docs/UC20-二手订单履约-组件级图.md`
- Interfaces and table ownership: `02_docs/domains/D-secondhand/interfaces-and-data.md`
- Traceability and evidence status: `02_docs/domains/D-secondhand/traceability.md`

## State Invariants

1. A seller may mutate only their own secondhand product.
2. A buyer cannot directly buy, bargain for, or bid on their own product.
3. Direct purchase and accepted bargaining create a `PENDING_PAY` order; only payment moves it to `PENDING_SHIP`.
4. At most one active auction exists for a product; historical auctions do not prevent a new auction.
5. Shipping and receipt operations check actor ownership and legal order state.
6. A failed auxiliary notification must not silently change the core transaction result.

## Verification Boundary

| Layer | Meaning in this repository | Status term |
|---|---|---|
| Service unit / standalone MockMvc / Spring Boot H2 integration | Backend rule, route, and local persistence evidence | `API_PASS` after the tagged suite passes |
| Vite mock browser walkthrough and screenshots | UI navigation and presentation evidence only | `UI_WALKTHROUGH_PASS` |
| Compose Nginx + real backend + MySQL + Playwright | Full-stack business flow | `E2E_PENDING` until the per-UC specs pass |

The shared Playwright Compose scaffold is supplied by Platform Task #132. The five real Domain-D flows belong to Tasks #148-#152 and their independent PRs; they are intentionally not represented as completed by the current mock screenshots.
