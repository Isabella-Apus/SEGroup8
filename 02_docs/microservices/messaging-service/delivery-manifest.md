# Messaging delivery manifest

The immutable application artifact is `segroup8/messaging:sha-<full-git-sha>`.
The image is built from `microservices/messaging-service/Dockerfile` only after
the Maven verification gate. Runtime values are injected by Kubernetes Secret
and ConfigMap; no JWT key, database password, or internal token is in the image.

The Helm release is `segroup8` in namespace `segroup8`. Its messaging workload
is `Deployment/messaging`, `Service/messaging`, and
`ConfigMap/segroup8-messaging-config`. `helm upgrade --install --atomic --wait`
is used by `.github/scripts/deploy-messaging-k3s.sh` so a failed rollout is
rolled back. The script uses `--reset-then-reuse-values`: chart defaults are
refreshed while existing environment-specific values are retained.

Production deployment requires the non-secret GitHub variable
`REALTIME_ALLOWED_ORIGIN_PATTERNS`; the deployment script rejects an empty
value or `*`. `IDENTITY_SERVICE_URL` may be supplied for the governance
fallback. The corresponding `IDENTITY_SERVICE_TOKEN` is a service-only
credential injected through the Messaging Secret; user Bearer tokens are
never forwarded.

This manifest is configuration evidence, not a claim that a production cluster
was reached from this workstation.

The service also has an independently named workflow at
`.github/workflows/messaging-service-ci-cd.yml` for boundary, Maven, real
MySQL, independent API/WebSocket E2E, full-system UC24/UC25 E2E,
candidate-image, and Helm-static gates. Production publishing and deployment
are performed by this dedicated workflow; the shared platform workflow keeps
Messaging disabled. The deployment job uses the shared
`segroup8-production-helm` concurrency lock.
