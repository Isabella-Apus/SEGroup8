#!/usr/bin/env python3
"""Small dependency-free HTTP benchmark used by the cloud-native experiments.

The same runner is used for monolith and microservice targets. It emits all
latencies plus a compact summary so the reported aggregates remain auditable.
"""

import argparse
import concurrent.futures
import json
import math
import statistics
import threading
import time
import urllib.error
import urllib.request
from collections import Counter
from datetime import datetime, timezone


def percentile(values, quantile):
    if not values:
        return None
    ordered = sorted(values)
    position = (len(ordered) - 1) * quantile
    lower = math.floor(position)
    upper = math.ceil(position)
    if lower == upper:
        return ordered[lower]
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (position - lower)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--name", required=True)
    parser.add_argument("--url", required=True)
    parser.add_argument("--concurrency", type=int, default=20)
    parser.add_argument("--duration", type=float, default=30.0)
    parser.add_argument("--warmup", type=float, default=5.0)
    parser.add_argument("--timeout", type=float, default=5.0)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    if args.concurrency < 1 or args.duration <= 0 or args.warmup < 0:
        parser.error("concurrency and duration must be positive; warmup cannot be negative")

    def request_once():
        started = time.perf_counter()
        status = 0
        error = None
        try:
            request = urllib.request.Request(args.url, headers={"Accept": "application/json"})
            with urllib.request.urlopen(request, timeout=args.timeout) as response:
                status = response.status
                response.read()
        except urllib.error.HTTPError as exc:
            status = exc.code
            error = f"HTTPError:{exc.code}"
            exc.read()
        except Exception as exc:  # raw exception class is evidence, not a pass/fail rewrite
            error = f"{type(exc).__name__}:{exc}"
        elapsed_ms = (time.perf_counter() - started) * 1000.0
        return status, elapsed_ms, error

    warmup_deadline = time.monotonic() + args.warmup
    while time.monotonic() < warmup_deadline:
        request_once()

    stop = threading.Event()
    records = []
    record_lock = threading.Lock()

    def worker():
        local = []
        while not stop.is_set():
            local.append(request_once())
        with record_lock:
            records.extend(local)

    started_at = datetime.now(timezone.utc)
    monotonic_start = time.monotonic()
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        futures = [pool.submit(worker) for _ in range(args.concurrency)]
        time.sleep(args.duration)
        stop.set()
        for future in futures:
            future.result()
    elapsed = time.monotonic() - monotonic_start
    finished_at = datetime.now(timezone.utc)

    statuses = Counter(str(status) for status, _, _ in records)
    errors = Counter(error for _, _, error in records if error)
    successful_latencies = [latency for status, latency, error in records if error is None and 200 <= status < 400]
    failed = len(records) - len(successful_latencies)
    summary = {
        "schemaVersion": 1,
        "name": args.name,
        "url": args.url,
        "concurrency": args.concurrency,
        "configuredDurationSeconds": args.duration,
        "warmupSeconds": args.warmup,
        "timeoutSeconds": args.timeout,
        "startedAt": started_at.isoformat(),
        "finishedAt": finished_at.isoformat(),
        "measuredDurationSeconds": elapsed,
        "requests": len(records),
        "successfulRequests": len(successful_latencies),
        "failedRequests": failed,
        "throughputRequestsPerSecond": len(records) / elapsed if elapsed else 0,
        "errorRate": failed / len(records) if records else 1,
        "latencyMs": {
            "average": statistics.fmean(successful_latencies) if successful_latencies else None,
            "p50": percentile(successful_latencies, 0.50),
            "p95": percentile(successful_latencies, 0.95),
            "p99": percentile(successful_latencies, 0.99),
            "minimum": min(successful_latencies) if successful_latencies else None,
            "maximum": max(successful_latencies) if successful_latencies else None,
        },
        "statuses": dict(statuses),
        "errors": dict(errors),
        "rawLatencyMs": [round(latency, 4) for _, latency, _ in records],
    }
    with open(args.output, "w", encoding="utf-8") as output:
        json.dump(summary, output, ensure_ascii=False, indent=2)
        output.write("\n")
    print(json.dumps({key: value for key, value in summary.items() if key != "rawLatencyMs"}, ensure_ascii=False))


if __name__ == "__main__":
    main()
