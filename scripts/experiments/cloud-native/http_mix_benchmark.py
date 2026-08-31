#!/usr/bin/env python3
"""Dependency-free weighted HTTP benchmark for the complete-system HPA experiment."""

import argparse
import concurrent.futures
import itertools
import json
import math
import statistics
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from collections import Counter, defaultdict
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


def metrics(records):
    successful = [latency for _, status, latency, error in records
                  if error is None and 200 <= status < 400]
    return {
        "requests": len(records),
        "successfulRequests": len(successful),
        "failedRequests": len(records) - len(successful),
        "errorRate": (len(records) - len(successful)) / len(records) if records else 1,
        "latencyMs": {
            "average": statistics.fmean(successful) if successful else None,
            "p50": percentile(successful, 0.50),
            "p95": percentile(successful, 0.95),
            "p99": percentile(successful, 0.99),
            "minimum": min(successful) if successful else None,
            "maximum": max(successful) if successful else None,
        },
        "statuses": dict(Counter(str(status) for _, status, _, _ in records)),
        "errors": dict(Counter(error for _, _, _, error in records if error)),
    }


def load_endpoints(path, base_url):
    weighted = []
    declared = []
    with open(path, encoding="utf-8") as stream:
        for number, raw in enumerate(stream, 1):
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split(maxsplit=1)
            if len(parts) != 2 or not parts[0].isdigit() or int(parts[0]) < 1:
                raise ValueError(f"invalid endpoint line {number}: {raw.rstrip()}")
            weight, endpoint = int(parts[0]), parts[1]
            url = urllib.parse.urljoin(base_url.rstrip("/") + "/", endpoint.lstrip("/"))
            declared.append({"path": endpoint, "url": url, "weight": weight})
            weighted.extend([(endpoint, url)] * weight)
    if not weighted:
        raise ValueError("endpoint file contains no usable endpoints")
    return declared, weighted


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--name", required=True)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--endpoint-file", required=True)
    parser.add_argument("--concurrency", type=int, default=20)
    parser.add_argument("--duration", type=float, default=30.0)
    parser.add_argument("--warmup", type=float, default=0.0)
    parser.add_argument("--timeout", type=float, default=10.0)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    if args.concurrency < 1 or args.duration <= 0 or args.warmup < 0:
        parser.error("concurrency and duration must be positive; warmup cannot be negative")

    declared, weighted = load_endpoints(args.endpoint_file, args.base_url)
    endpoint_cycle = itertools.cycle(weighted)
    cycle_lock = threading.Lock()

    def request_once():
        with cycle_lock:
            endpoint, url = next(endpoint_cycle)
        started = time.perf_counter()
        status = 0
        error = None
        try:
            request = urllib.request.Request(url, headers={"Accept": "application/json"})
            with urllib.request.urlopen(request, timeout=args.timeout) as response:
                status = response.status
                response.read()
        except urllib.error.HTTPError as exc:
            status = exc.code
            error = f"HTTPError:{exc.code}"
            exc.read()
        except Exception as exc:
            error = f"{type(exc).__name__}:{exc}"
        elapsed_ms = (time.perf_counter() - started) * 1000.0
        return endpoint, status, elapsed_ms, error

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

    aggregate = metrics(records)
    per_endpoint_records = defaultdict(list)
    for record in records:
        per_endpoint_records[record[0]].append(record)
    summary = {
        "schemaVersion": 1,
        "name": args.name,
        "baseUrl": args.base_url,
        "endpoints": declared,
        "concurrency": args.concurrency,
        "configuredDurationSeconds": args.duration,
        "warmupSeconds": args.warmup,
        "timeoutSeconds": args.timeout,
        "startedAt": started_at.isoformat(),
        "finishedAt": finished_at.isoformat(),
        "measuredDurationSeconds": elapsed,
        "throughputRequestsPerSecond": len(records) / elapsed if elapsed else 0,
        **aggregate,
        "perEndpoint": {endpoint: metrics(items) for endpoint, items in per_endpoint_records.items()},
        "raw": [
            {"endpoint": endpoint, "status": status, "latencyMs": round(latency, 4), "error": error}
            for endpoint, status, latency, error in records
        ],
    }
    with open(args.output, "w", encoding="utf-8") as output:
        json.dump(summary, output, ensure_ascii=False, indent=2)
        output.write("\n")
    compact = {key: value for key, value in summary.items() if key not in ("raw", "perEndpoint")}
    print(json.dumps(compact, ensure_ascii=False))


if __name__ == "__main__":
    main()
