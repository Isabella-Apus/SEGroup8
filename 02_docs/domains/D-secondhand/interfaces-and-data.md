# Domain D interfaces and data ownership

The detailed source is `02_docs/UC16-UC20-接口与数据归属.md`. This page is the stable Domain-D entry used by CI evidence and later microservice work.

## Public API Groups

| API group | Main routes | UC |
|---|---|---|
| Product discovery and seller management | `GET /api/secondhand/list`, `GET /api/secondhand/detail/{productId}`, `/api/secondhand/seller/**` | UC16, UC17, UC19 |
| Direct purchase | `POST /api/secondhand/{productId}/buy` | UC17 |
| Bargaining | `/api/secondhand/trade/bargain/**` | UC18 |
| Auctions | `/api/secondhand/trade/auction/**` | UC19 |
| Order fulfillment | `/api/order/list`, `/api/order/seller/list`, `/api/order/{orderId}/pay`, `/ship`, `/confirm-receive` | UC17, UC18, UC20 |

Authentication is resolved by the existing user context. Route-level tests verify request mapping and delegation; service and H2 integration tests verify the current monolith behavior. They are not substitutes for a browser calling the real Compose stack.

## Current Table Ownership

| Data | Current owner | Future boundary |
|---|---|---|
| `secondhand_product` | secondhand product service | secondhand-trade-service |
| `product_negotiation` | secondhand trade service | secondhand-trade-service |
| `product_auction`, `auction_log` | secondhand trade service / scheduler | secondhand-trade-service |
| `order_info`, `order_item` | order service | order-service |
| `logistics_trace` | order/logistics services | order-service or logistics-service |
| notifications and chat messages | notification/chat services | asynchronous collaboration |

## Cross-Boundary Contract

- A secondhand transaction requests order creation; it does not own the order lifecycle.
- The created order records `productType=SECONDHAND` and starts in `PENDING_PAY`.
- Product availability and order creation must remain consistent under concurrency and failure.
- Later microservice migration must replace direct cross-table writes with versioned APIs/events plus idempotency and compensation evidence.
