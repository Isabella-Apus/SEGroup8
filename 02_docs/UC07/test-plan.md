# UC07 Seller Product Lifecycle Test Plan

## Scope

Validate a seller product lifecycle against the real frontend, backend, and MySQL stack:

- create a product with an uploaded image;
- edit its name and verify the seller API returns the persisted record;
- increase stock and reject a negative resulting stock level;
- take the product off sale, delete it, and confirm it is no longer retrievable;
- deny a buyer access to the seller workbench.

## Test Layers

`ProductServiceImplTest` remains the fast service-level regression baseline. The acceptance flow is implemented in `frontend/e2e/domain-b/uc07-product-lifecycle.spec.ts`; it runs Playwright against the Compose services and does not use MockMvc as E2E evidence.

## Commands

```powershell
npm --prefix frontend run build:real
powershell -NoProfile -ExecutionPolicy Bypass -File 04_tests/domains/B-catalog-shop/run-domain-b.ps1 -Browser
```

The browser command rebuilds the Compose stack, refreshes the idempotent fixture, and runs the Domain B Playwright suite serially.
