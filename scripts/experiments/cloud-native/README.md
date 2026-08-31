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
Java base image and MySQL image. Put the identity-governance, order and
secondhand JARs under `<host-root>/jars/` before preparation. Generated credentials are kept only in
the Kubernetes Secret and the rendered runtime manifest; neither is evidence
that should be committed.

The dependency drill accepts an optional evidence directory name and never
overwrites a previous run:

```bash
bash scripts/experiments/cloud-native/run_dependency_fault_experiment.sh \
  <host-root>/state.env dependency-fault-<candidate>
```

It writes the commit and all three JAR SHA-256 values into the evidence
metadata. The command exits non-zero unless the controlled outage, automatic
recovery, no-duplicate-order check and receiver-address snapshot check all
pass. `summary.json` remains available on a failed exit for diagnosis.
