# SEGroup8 K3s deployment

This chart targets the project's single-node K3s production environment. The
GitHub Actions pipeline supplies immutable ACR image tags and the current
`backend/src/main/resources/schema.sql`; real credentials are never stored in
the chart.

## One-time cluster prerequisites

Create the namespace and the platform secrets before enabling CD. The
Messaging secret is required when the Messaging service pipeline enables that
component:

```bash
kubectl create namespace segroup8
kubectl -n segroup8 get secret \
  acr-pull-secret segroup8-backend-secret segroup8-mysql-secret
kubectl -n segroup8 get secret identity-governance-secret
```

The secrets must contain these keys:

- `acr-pull-secret`: Kubernetes `docker-registry` credentials for ACR.
- `segroup8-mysql-secret`: `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`,
  and `MYSQL_ROOT_PASSWORD`.
- `segroup8-backend-secret`: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
  `JWT_SECRET`, and `REALTIME_ALLOWED_ORIGIN_PATTERNS`. Optional LLM settings
  may be added to the same secret.
- `identity-governance-secret`: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
  `JWT_SECRET`, `INTERNAL_SERVICE_TOKEN`, and `BOOTSTRAP_ADMIN_PASSWORD`.
  Its database account must be restricted to `identity_governance_db.*`.
- `segroup8-messaging-secret`: required by the Messaging service pipeline;
  `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and
  `INTERNAL_SERVICE_TOKEN`; add `INTERNAL_OPERATIONS_TOKEN` for the replay
  endpoint and `IDENTITY_SERVICE_TOKEN` for the governance fallback. This is a
  service-to-service token, never an end-user Bearer token.

`REALTIME_ALLOWED_ORIGIN_PATTERNS` must be supplied as an explicit
non-wildcard production allow-list. The deployment script rejects an empty
value or `*`. `IDENTITY_SERVICE_URL` is an optional non-secret variable for the
governance fallback endpoint. The ConfigMap also carries bounded identity
timeout/retry settings.

Use `mysql:3306` as the database host in `DB_URL`. Do not expose MySQL with a
NodePort or cloud security-group rule.

Before the first identity deployment, create `identity_governance_db` and its
restricted account on the existing MySQL server. The service JAR then applies
versioned Flyway migrations automatically. Do not grant the service account
access to `segroup8_platform` or another service Schema.

## Pipeline configuration

The `production` GitHub Environment must provide:

- Secrets: `ACR_USERNAME`, `ACR_PASSWORD`, `DEPLOY_HOST`, `DEPLOY_USER`,
  `DEPLOY_SSH_KEY`, and `DEPLOY_KNOWN_HOSTS`.
- Variables: `ACR_REGISTRY`, `ACR_NAMESPACE`, `ENABLE_PRODUCTION_DEPLOY=true`,
  and `TEST=true`.
- Optional variables: `K8S_NAMESPACE` (defaults to `segroup8`) and
  `PRODUCTION_URL` (enables an external `/health` check),
  `REALTIME_ALLOWED_ORIGIN_PATTERNS`, and `IDENTITY_SERVICE_URL`.

The SSH user needs `helm`, `kubectl`, `curl`, and a working
`$HOME/.kube/config` for the local K3s cluster.

## Storage and database lifecycle

The chart uses K3s `local-path` volumes for MySQL and `/app/uploads`. They are
single-node volumes and must be included in ECS disk snapshots or another
backup process.

The schema is processed by the MySQL image only when its data directory is
empty. Later schema evolution requires an explicit migration mechanism such as
Flyway; deleting the MySQL PVC is destructive and is not an upgrade strategy.
The demo seed file is intentionally excluded from production.

## Deployment behavior

The platform workflow installs the base `segroup8` release. The identity
and individual service pipelines then use
`helm upgrade --install --reset-then-reuse-values --atomic --wait`.
This loads keys newly introduced by the current chart, then preserves explicit
values owned by the existing release and upgrades only the selected component.
All service deployment jobs must share the
`segroup8-production-helm` concurrency group. A failed upgrade is rolled back
automatically.
Images use `sha-<full Git SHA>` tags so every release is traceable and
rollback-safe.

The identity HPA template is present but disabled by default. Enabling it and
recording scale-out/scale-in metrics belongs to the later autoscaling
experiment; the normal Kubernetes deployment does not claim that experiment.
