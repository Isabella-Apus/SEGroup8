# Shared E2E fixtures

Import test and expect from frontend/e2e/fixtures in every future Domain
spec. The fixture resolves a role-aware test account from environment
variables and exposes loginAs for later role-specific scenarios.

Business actions belong in the Domain spec or that Domain's helper. Keep this
directory limited to browser context, authentication, test-account resolution,
and other platform concerns.
