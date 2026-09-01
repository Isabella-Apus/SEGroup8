# Kubernetes environment check

Date: 2026-08-31

`kubectl` is installed, but no current context is configured. `kubectl
cluster-info` therefore cannot connect (local API endpoint refused). Helm lint
and template validation were run locally; deployment rollout, production smoke,
and the wrong-origin rollback drill remain manual actions requiring a configured
cluster.
