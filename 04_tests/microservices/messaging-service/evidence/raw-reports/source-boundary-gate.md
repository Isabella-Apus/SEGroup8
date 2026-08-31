# Messaging source boundary gate

Command: `node scripts/ci/verify-messaging-boundary.mjs`

Result: **PASS** — 38 Java source files scanned. No foreign Mapper,
Repository, Entity, cross-schema reference, foreign-table SQL, or trusted
identity header was found. The same gate is executed in the messaging CI job
before packaging.
