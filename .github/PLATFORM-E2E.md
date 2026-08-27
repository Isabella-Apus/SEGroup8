# Shared platform E2E operating note

The shared real-browser E2E entry point is documented in
04_tests/platform-e2e/README.md. This note is intentionally short so the CI
pipeline has one platform reference and Domain teams do not create separate
Playwright or Compose frameworks.

The required sequence is:

1. frontend build and backend automated tests
2. Compose build
3. database health
4. backend health
5. frontend health
6. Playwright against the Compose frontend
7. upload report, browser artifacts, and service logs

The CI entry point is scripts/e2e/run-compose-e2e.sh. Domain teams add specs
under frontend/e2e/domain-a through frontend/e2e/domain-e and import the shared
fixture. A passing platform smoke is not evidence that any UC is complete.
