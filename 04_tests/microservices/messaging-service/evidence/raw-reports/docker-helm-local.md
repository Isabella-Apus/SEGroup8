# Local Docker and Helm evidence

Date: 2026-08-31

## Docker

- Command: `docker build --file microservices/messaging-service/Dockerfile --tag segroup8/messaging:sha-0bafb9d6a82276f57e4fb6a66d5e50e4ce146ea5 microservices`
- Result: PASS
- Image: `segroup8/messaging:sha-0bafb9d6a82276f57e4fb6a66d5e50e4ce146ea5`
- Image ID: `sha256:00da458d9e1622f6df8fcad691268b64c470e22e5ceea40d356f2db85299238a`
- Candidate JAR SHA: `3a874f2fe2e7fe16370dd7b6836047f7abd2fea419d97592bd3648803aca910b`
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
