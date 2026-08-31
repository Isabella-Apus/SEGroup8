# Candidate image inspection

- Image tag: `segroup8/messaging:sha-0bafb9d6a82276f57e4fb6a66d5e50e4ce146ea5`
- Image ID: `sha256:00da458d9e1622f6df8fcad691268b64c470e22e5ceea40d356f2db85299238a`
- Candidate JAR: `microservices/messaging-service/target/messaging-service-1.0.0.jar`
- Candidate JAR SHA-256: `3a874f2fe2e7fe16370dd7b6836047f7abd2fea419d97592bd3648803aca910b`
- OCI revision label: `0bafb9d6a82276f57e4fb6a66d5e50e4ce146ea5`
- OCI source label: `https://github.com/segroup8/kinda-goods`
- OCI JAR SHA label: `3a874f2fe2e7fe16370dd7b6836047f7abd2fea419d97592bd3648803aca910b`
- Runtime user: `10001:10001`
- Base image: `eclipse-temurin:17-jre-alpine@sha256:27cc0849148c0fd32ee8e95988917becf9bc96a3182a24f99d9763aa8e90f8cb`
- Dockerfile copies the already-built Boot JAR and does not execute Maven.
