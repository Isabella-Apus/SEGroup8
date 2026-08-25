# UC16-UC20 k6 Performance Scripts

These scripts are the initial performance-test framework for member D's secondhand domain.
They are intentionally parameterized with environment variables so the same scripts can be
used against the monolith baseline and the later microservice deployment.

## Scripts

| Script | Purpose |
|---|---|
| `product-search.k6.js` | Compare product search and secondhand search latency/throughput |
| `new-order-create.k6.js` | Exercise new-product order creation, with optional payment |
| `secondhand-auction-bid.k6.js` | Exercise secondhand auction bidding |
| `secondhand-buy.k6.js` | Exercise direct secondhand purchase with prepared product ID dataset |

## Common Environment Variables

| Name | Default | Description |
|---|---|---|
| `BASE_URL` | `http://127.0.0.1:8080/api` | Backend API base URL |
| `VUS` | `10` | Virtual users |
| `DURATION` | `30s` | Test duration |
| `SLEEP` | `1` | Delay between iterations |
| `BUYER_USERNAME` | `user` | Seed buyer username |
| `BUYER_PASSWORD` | `user123` | Seed buyer password |
| `ADDRESS_ID` | `1` | Seed address ID |

## Example Commands

```powershell
k6 run -e BASE_URL=http://127.0.0.1:8080/api -e KEYWORD=耳机 .\04_tests\performance\k6\product-search.k6.js

k6 run -e BASE_URL=http://127.0.0.1:8080/api -e NEW_PRODUCT_ID=1 -e ADDRESS_ID=1 .\04_tests\performance\k6\new-order-create.k6.js

k6 run -e BASE_URL=http://127.0.0.1:8080/api -e AUCTION_ID=1 -e BID_BASE_AMOUNT=200 -e BID_STEP=10 .\04_tests\performance\k6\secondhand-auction-bid.k6.js

k6 run -e BASE_URL=http://127.0.0.1:8080/api -e SECONDHAND_PRODUCT_IDS=1,2,3 -e ADDRESS_ID=1 .\04_tests\performance\k6\secondhand-buy.k6.js
```

For final experiments, run each script at least three times on the monolith baseline and
microservice version, using the same machine, same data reset method, same VU count, and
same duration.
