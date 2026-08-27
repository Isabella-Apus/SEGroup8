# Domain B Test Evidence

This directory is the shared index for UC06-UC10 test execution. Use-case
plans, reports, demos, and evidence remain under `04_tests/UC06/` through
`04_tests/UC10/`; this directory does not duplicate them.

Generated shared evidence is written to:

- `evidence/logs/` for command logs;
- `evidence/raw-reports/` for Surefire XML/TXT;
- `evidence/screenshots/` only for supplementary visual evidence;
- `evidence/result-summary.json` for the machine-readable aggregate.

The aggregate must identify API Integration and Browser E2E as separate test
layers. Never label a Spring Boot + MockMvc suite as E2E.
