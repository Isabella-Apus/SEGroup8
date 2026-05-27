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

## If a key was already committed

Delete the key from the repository, then rotate or revoke the exposed key in the provider console. Removing it from the latest commit is not enough if the key exists in Git history.
