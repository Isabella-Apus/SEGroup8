# Cloud-native experiment runner

These scripts create and operate only namespaces matching
`segroup8-cloud-exp-*`. They are intended for the course experiment server,
not for production deployment.

Execution order:

1. `prepare_environment.sh`
2. `run_performance_comparison.sh`
3. `run_hpa_experiment.sh`
4. `run_dependency_fault_experiment.sh`
5. `cleanup_environment.sh`

The runner requires Bash, Python 3, kubectl, a working Metrics API, a cached
Java base image and MySQL image. Put the order and secondhand JARs under
`<host-root>/jars/` before preparation. Generated credentials are kept only in
the Kubernetes Secret and the rendered runtime manifest; neither is evidence
that should be committed.
