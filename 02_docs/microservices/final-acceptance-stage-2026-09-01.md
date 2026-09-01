# Six-Service Final Acceptance: Pre-Review Stage

Date: 2026-09-01 (Asia/Kuala_Lumpur)

This is a staging record, not a final acceptance result. The authoritative
acceptance standard remains `02_docs/microservices/service-acceptance-checklist.md`.

## Repository state

- `origin/main`: `233ac46bc74cfac440ba004db1a5c9256cc60685`
- Domain D contract PR: #223, head
  `924f83927ca8496319ccbdba486157bb23d0faf9`
- Production prerequisite PR: #224, head
  `bfdc9d6f41cbfa06d133c1a45342a23618d60ecc`
- Local combined validation commit: `ac812b8f283f249d883c6d850ad5306613ede728`
  (PR #224 plus PR #223, local validation only)
- No cloud-native experiment branch was merged.
- No existing PR body was modified.

## GitHub Actions evidence

### PR #223

- Shared workflow run: `33472617625`
- Backend, frontend, Helm validation, UC manifest, Domains A-E and full
  Playwright smoke/all-UC E2E: PASS
- Image publication, release and production deployment: SKIPPED because this
  is a pull request
- Independent review: BLOCKED pending a non-author review

### PR #224

- Catalog Shop run `33472637292`: service gates and independent smoke PASS
- Identity Governance run `33472637244`: service gates and browser E2E PASS
- Benefits Finance run `33472637346`: service gates, API E2E and Compose
  regression PASS
- Secondhand run `33472637336`: service gates, independent E2E and Compose
  Playwright regression PASS
- Messaging run `33472637348`: service gates, MySQL, API/WebSocket E2E and
  full-system UC24/UC25 E2E PASS
- Order run `33472637422`: service gates, API E2E and Compose regression PASS
- Shared run `33472637423`: FAIL only in Domain D because PR #224 intentionally
  does not contain the one-assertion PR #223 fix
- All publication and deployment jobs: SKIPPED on the pull request
- Independent review: BLOCKED pending a non-author review

## Local combined validation

The two open PR heads were combined locally without changing either PR.

- Security contract: 5 tests PASS
- Identity Governance: 17 tests PASS
- Order: 17 tests PASS
- Secondhand: 24 tests PASS
- Catalog Shop: 15 tests, 0 failures, 0 errors, 1 skipped
- Messaging: 23 tests, 0 failures, 0 errors, 1 skipped
- Benefits Finance: 26 tests PASS
- Aggregate across the initial reactor run and resumed reactor run: 127 tests,
  0 final failures, 0 final errors, 2 skipped
- `git diff --check`: PASS
- Validation worktree was restored to a clean status after generated historical
  Surefire evidence timestamps were discarded

One Catalog Shop outbox retry test failed once because the application scheduler
and the manually invoked publisher both selected the same row. The same test
passed on an immediate isolated rerun and the resumed Catalog Shop reactor passed.
This is recorded as a residual concurrency/flakiness risk and is not represented
as a production fix in these PRs.

## Helm and deployment configuration

The combined six-service chart was linted and rendered with one replica per
service and Catalog Shop HPA disabled for the first resource-constrained rollout.

- Deployments: 8
- Services: 9
- Ingresses: 1
- HPAs: 0
- Startup probes: 9
- Readiness probes: 9
- Liveness probes: 9
- Private registry pull-secret references: 9
- Bash syntax, Actionlint and secret-literal scan for PR #224: PASS

## Production prerequisites completed

The production write operations were explicitly authorized before execution.
No plaintext credential was printed, written to Git or included in this record.

- Existing Helm revision 4 and rollback metadata were recorded before changes.
- Six service-owned schemas exist.
- Six application accounts and six migration accounts exist.
- Runtime accounts are limited to DML on their own schema.
- Migration accounts are limited to migration permissions on their own schema;
  Catalog Shop additionally has the routine permissions required by its migration.
- Six service Kubernetes Secrets exist with the required key names.
- The legacy backend Secret now includes the internal service token and Messaging
  service URL keys while retaining its previous keys.
- The old platform was not restarted, scaled or switched.
- No six-service Helm rollout has been performed.
- The existing ACR pull Secret has not yet been replaced.
- Repository variable `ENABLE_PRODUCTION_DEPLOY` is intentionally `false` to
  prevent an incomplete automatic production rollout.

## Current blockers and remaining work

- Non-author review and merge of PR #223: BLOCKED by the requested review pause.
- Rebase/update PR #224 after #223 merges, then obtain non-author review: BLOCKED.
- Current final `main` SHA and its seven workflow runs: NOT_RUN.
- Immutable ACR images and registry digests for the final SHA: NOT_RUN.
- Local candidate image build: BLOCKED because Docker Desktop rejects the local
  Scholar interception certificate chain while resolving Docker Hub. TLS
  verification was not disabled.
- Correct ACR pull Secret recreation from GitHub Secrets: NOT_RUN.
- Atomic six-service Helm rollout: NOT_RUN.
- Production migrations, cross-schema negative query, probes, `/actuator/info`,
  logs, events, routes and service-to-service checks: NOT_RUN.
- Six-service smoke/E2E and authenticated platform UC coverage against production:
  NOT_RUN.
- Deliberate failure and rollback drill against the final release: NOT_RUN.
- Final checklist matrix and independent sign-off: NOT_RUN/BLOCKED.

No final acceptance claim is made at this stage.
