# V3 deployment test report

Static deployment checks completed:

- messaging Dockerfile exists and builds the independently verified JAR;
- Helm Deployment, Service, ConfigMap, Secret references, immutable SHA tag,
  resources, and liveness/readiness probes are present;
- existing frontend Nginx routes chat/notifications and `/ws/realtime` to the
  `messaging` Service while keeping other APIs on backend;
- deployment script now uses `--atomic --wait` and waits for Messaging rollout.

Execution status:

- **PASS** — Docker build completed locally with
  `segroup8/messaging:sha-0bafb9d6a82276f57e4fb6a66d5e50e4ce146ea5` and image
  ID `sha256:00da458d9e1622f6df8fcad691268b64c470e22e5ceea40d356f2db85299238a`.
  The candidate contains the tested JAR SHA
  `3a874f2fe2e7fe16370dd7b6836047f7abd2fea419d97592bd3648803aca910b`.
- **PASS** — `helm lint` and `helm template` completed locally with the
  messaging repository and immutable `sha-test` tag supplied.
- **MANUAL ACTION REQUIRED** — no Kubernetes cluster is configured on this
  workstation, so rollout status, production smoke, and deployment screenshots
  have not been claimed.
