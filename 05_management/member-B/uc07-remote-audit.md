# UC07 Remote Recovery Audit

Audit date: 2026-08-27

## Source of truth

- Remote: `origin` (`github.com:Isabella-Apus/SEGroup8`)
- Historical delivery commit: `4b99af197d247c948ea2b609d06e3dfda0367a89`
- Commit subject: `feat: deliver UC07 seller product lifecycle (#50)`
- Historical remote branch still available: `origin/codex/uc07-results`

## Confirmed historical assets

Commit `4b99af1` contains all of the following and must be used as the recovery
source instead of recreating UC07 from memory:

- `.github/workflows/uc07-tests.yml`
- `01_requirements/UC07-卖家商品生命周期.md`
- UC07 design and traceability documents under `02_docs/`
- `04_tests/UC07/UC07-demo.http`
- UC07 test report and evidence index
- Maven logs and Surefire XML/TXT raw reports
- `CatalogLifecycleApiAndE2ETest.java`
- catalog controller/service/schema changes for the seller lifecycle

## Current main assessment

The current `origin/main` contains the UC06 catalog-query baseline only. It does
not contain the historical UC07 seller lifecycle controller/service/schema,
requirements, design, report, or evidence paths listed above. The existing
`ProductServiceImplTest` in the monolith is only a unit-test baseline and is
not UC07 completion evidence.

## Recovery rule

UC07 recovery must be performed on `test/uc07-real-flow` created from the then
latest `main`, after PR-B0 is merged. Recover applicable files by comparing or
selectively restoring from `4b99af1`; do not restore its obsolete standalone
workflow because PR-B0 replaces per-UC workflows with the unified Domain B
workflow. Rename restored MockMvc tests to `*ApiIntegrationTest`, retain
`DOMAIN_B` and `UC07` tags, then add the real Compose/MySQL Playwright flow and
fresh evidence before closing the UC07 D3-D5 Task.

## GitHub access

GitHub CLI is not installed in the current environment. The available browser
session is also signed out and exposes only read-only repository pages, so
reopening parent issues, creating Task Issues, and creating PRs require an
authenticated GitHub session. No parent UC issue was closed or otherwise
modified during this audit.
