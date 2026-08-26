# Security Notes

Do not commit real API keys, database passwords, JWT secrets, or other credentials.

## Local secrets

Use one of these approaches for local credentials:

- Put secrets in `backend/src/main/resources/application-local.yml`.
- Or set environment variables before starting the backend.

`application-local.yml` is ignored by Git and should stay untracked.

## LLM API key

The risk-audit LLM key can be provided with either environment variable:

```powershell
$env:RISK_AUDIT_LLM_API_KEY="your_real_key"
```

or:

```powershell
$env:OPENAI_API_KEY="your_real_key"
```

Never paste a real key into `application.yml` or `application-local.example.yml`.

## Docker and Kubernetes secrets

- Copy `deploy/docker/.env.secrets.example` to a file outside the repository, replace every placeholder, and restrict it to the deployment account.
- Create the Kubernetes Secret from the deployment environment or a secret manager. `deploy/k8s/secret.example.yaml` is a schema-only template and must never contain production values.
- Application configuration may reference environment-variable names, but must not contain a production password, JWT signing key, API key, private key, cookie, or access token.
- Do not print secret values in build logs, startup logs, exception messages, health responses, screenshots, or test evidence.
- Grant the application read access only to its own namespace and Secret; do not grant permission to list all cluster Secrets.

Before committing, verify staged changes with:

```bash
git diff --cached
```

If a scanner or reviewer reports a suspected secret, stop deployment and rotate the value before treating the Git cleanup as complete.

## If a key was already committed

Delete the key from the repository, then rotate or revoke the exposed key in the provider console. Removing it from the latest commit is not enough if the key exists in Git history.
