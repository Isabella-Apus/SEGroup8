# Messaging delivery manifest

The immutable application artifact is `segroup8/messaging:sha-<full-git-sha>`.
The image is built from `microservices/messaging-service/Dockerfile` only after
the Maven verification gate. Runtime values are injected by Kubernetes Secret
and ConfigMap; no JWT key, database password, or internal token is in the image.

The Helm release is `segroup8` in namespace `segroup8`. Its messaging workload
is `Deployment/messaging`, `Service/messaging`, and
`ConfigMap/segroup8-messaging-config`. `helm upgrade --install --atomic --wait`
is used by `.github/scripts/deploy-k3s.sh` so a failed rollout is rolled back.

Production deployment requires the non-secret GitHub variable
`REALTIME_ALLOWED_ORIGIN_PATTERNS`; the deployment script rejects an empty
value or `*`. `IDENTITY_SERVICE_URL` may be supplied for the governance
fallback.

This manifest is configuration evidence, not a claim that a production cluster
was reached from this workstation.

The service also has an independently named workflow at
`.github/workflows/messaging-service-ci-cd.yml` for boundary, Maven, real
MySQL, candidate-image, and Helm-static gates. Existing production publishing
and deployment remains in the repository's unified workflow to avoid two
competing deployment pipelines; its production deployment job uses the shared
`segroup8-production-helm` concurrency lock.
