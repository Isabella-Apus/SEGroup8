# Messaging E2E evidence

The repository keeps only compact acceptance summaries and deliberately selected failure screenshots here.
The complete Playwright HTML report, JSON/JUnit output, traces, videos, and test-results are generated in the
CI runner's temporary directory and uploaded by `.github/workflows/messaging-service-ci-cd.yml` as Actions artifacts.

This prevents generated browser output from becoming part of the source tree while preserving reproducible
evidence links in the workflow run.
