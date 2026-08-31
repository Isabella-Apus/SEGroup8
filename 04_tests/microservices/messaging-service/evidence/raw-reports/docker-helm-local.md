# Local Docker and Helm evidence

Date: 2026-08-31

## Docker

- Command: `docker build --file microservices/messaging-service/Dockerfile --tag segroup8/messaging:sha-684aff9e87664ebb41d9844cacbfd8bdf2dc60b3 microservices`
- Result: PASS
- Image: `segroup8/messaging:sha-684aff9e87664ebb41d9844cacbfd8bdf2dc60b3`
- Image ID: `sha256:b0c0bf155725efdd0c6ceff3a98ce1bbddca9f288c3f478f34cd197ad27821d6`
- Candidate JAR SHA: `0467c321fc551b2024a6e18f112faf96912094a7f0b78609a579e0ca18d4c165`
- Final audit rerun: `docker run --rm alpine/helm:3.15.4` executed `helm lint` and `helm template` against the current tree; lint reported `1 chart(s) linted, 0 chart(s) failed`.
- Local `docker image ls --no-trunc` confirms the same immutable image ID and
  220 MB artifact; image history contains only the application JAR and runtime
  layers, with no runtime Secret values.
- Secrets were supplied only at runtime; no secret values were copied into the image.

## Helm

- Command: `helm lint deploy/helm/segroup8 --set-string messaging.image.repository=registry.example/segroup8/messaging --set-string messaging.image.tag=sha-test --set-string messaging.config.realtimeAllowedOriginPatterns=https://example.test`
- Result: PASS (`1 chart(s) linted, 0 chart(s) failed`)
- Command: `helm template segroup8 deploy/helm/segroup8` with the same image and explicit origin overrides
- Result: PASS (exit code 0)
- A Kubernetes cluster was not configured locally; rollout and production smoke remain manual actions.
