# Messaging service operations

Build and verify from the repository root:

```powershell
$env:JAVA_HOME='D:\java\IntelliJ IDEA 2025.2.1\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -B -f microservices/pom.xml -pl messaging-service -am clean verify
```

The runtime requires Secret-injected `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
`JWT_SECRET`, `INTERNAL_SERVICE_TOKEN`, and (for replay)
`INTERNAL_OPERATIONS_TOKEN`. `REALTIME_ALLOWED_ORIGIN_PATTERNS` must be an
explicit allow-list.

Do not use `latest` for the messaging release. Use
`segroup8/messaging:sha-<full-git-sha>` and deploy through the existing
`deploy/helm/segroup8` chart.
