#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
export RUN_ID="${RUN_ID:-defense-$(date +%Y%m%d-%H%M%S)}"
export CONCURRENCIES="${CONCURRENCIES:-10 20 40}"
export STAGE_DURATION="${STAGE_DURATION:-30}"
export WARMUP_DURATION="${WARMUP_DURATION:-20}"
export SCALE_TRIGGER_DURATION="${SCALE_TRIGGER_DURATION:-120}"
export REQUEST_TIMEOUT="${REQUEST_TIMEOUT:-15}"
export MIN_REPLICAS="${MIN_REPLICAS:-2}"
export MAX_REPLICAS="${MAX_REPLICAS:-4}"
export TARGET_CPU="${TARGET_CPU:-60}"
export KEEP_OPTIMIZED_HPA="${KEEP_OPTIMIZED_HPA:-true}"

exec bash "$SCRIPT_DIR/run_system_hpa_experiment.sh" \
  "${1:-/root/segroup8-experiments/system-hpa-$RUN_ID}"
