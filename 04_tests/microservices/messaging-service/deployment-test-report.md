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
  `segroup8/messaging:sha-684aff9e87664ebb41d9844cacbfd8bdf2dc60b3` and image
  ID `sha256:b0c0bf155725efdd0c6ceff3a98ce1bbddca9f288c3f478f34cd197ad27821d6`.
  The candidate contains the tested JAR SHA
  `0467c321fc551b2024a6e18f112faf96912094a7f0b78609a579e0ca18d4c165`.
- **PASS** — `helm lint` and `helm template` completed locally with the
  messaging repository and immutable `sha-test` tag supplied.
- **MANUAL ACTION REQUIRED** — no Kubernetes cluster is configured on this
  workstation, so rollout status, production smoke, and deployment screenshots
  have not been claimed.
