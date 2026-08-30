# Evidence index

- `playwright-final/`: final 5/5 JSON, JUnit XML and self-contained HTML report bundle.
- `playwright-uc18-rerun/`: targeted UC18 regression rerun after the frontend fix.
- `playwright/`: initial 4/5 run retained as failure-to-fix evidence.
- `screenshots/`: final named screenshots copied to short paths for reliable Windows checkout.
- `raw-reports/playwright-failure/`: initial UC18 failure screenshots, context, trace and video.
- `raw-reports/surefire/`: Maven Surefire XML and text reports.
- `logs/`: local validation summary.

The generated JSON/XML reports retain their original execution-time artifact paths. Exact artifact copies are stored in the short-path directories above; duplicate generated `test-results` trees are intentionally omitted so the repository does not exceed the Windows path limit.
