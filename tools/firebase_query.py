#!/usr/bin/env python3
"""
firebase_query.py — Flexible CLI for querying Samizdat Firestore collections.

Reads credentials from local.properties automatically. Supports listing,
filtering, analyzing, exporting, and cleaning up all 11 Firestore collections.

Usage:
    python tools/firebase_query.py logs --device PhoneA --level ERROR --since 1h
    python tools/firebase_query.py stats --device PhoneA --format csv --output stats.csv
    python tools/firebase_query.py analyze --report hmac
    python tools/firebase_query.py list connections --limit 20
    python tools/firebase_query.py relay --dest PhoneB
    python tools/firebase_query.py export device_logs --format json --output dump.json
    python tools/firebase_query.py cleanup device_logs --older-than 3d --dry-run

Run with --help for full documentation.
"""

from __future__ import annotations

import argparse
import csv
import io
import json
import os
import re
import sys
import time
from collections import Counter, defaultdict
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any
from urllib.parse import quote as urlquote

try:
    import requests
except ImportError:
    print("ERROR: 'requests' package required. Install with: pip install requests", file=sys.stderr)
    sys.exit(1)

# ──────────────────────────────────────────────────────────────────────────────
# Constants
# ──────────────────────────────────────────────────────────────────────────────

FIRESTORE_BASE = "https://firestore.googleapis.com/v1"

ALL_COLLECTIONS = [
    "device_logs",
    "mesh_relay",
    "devices",
    "stats",
    "routing",
    "connections",
    "error_logs",
    "test_results",
    "store_forward",
    "endurance_reports",
    "endurance_snapshots",
]

# How many documents to fetch per page (Firestore max is 300)
DEFAULT_PAGE_SIZE = 100

# ──────────────────────────────────────────────────────────────────────────────
# Credential loading
# ──────────────────────────────────────────────────────────────────────────────

def find_local_properties() -> Path:
    """Walk up from this script's directory until we find local.properties."""
    cur = Path(__file__).resolve().parent
    for _ in range(10):
        candidate = cur / "local.properties"
        if candidate.is_file():
            return candidate
        cur = cur.parent
    raise FileNotFoundError(
        "Could not find local.properties. "
        "Set FIREBASE_PROJECT_ID and FIREBASE_API_KEY env vars instead."
    )


def load_credentials() -> tuple[str, str]:
    """Return (project_id, api_key) from env vars or local.properties."""
    project_id = os.environ.get("FIREBASE_PROJECT_ID")
    api_key = os.environ.get("FIREBASE_API_KEY")
    if project_id and api_key:
        return project_id, api_key

    props_path = find_local_properties()
    props: dict[str, str] = {}
    with open(props_path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if "=" in line and not line.startswith("#"):
                k, _, v = line.partition("=")
                props[k.strip()] = v.strip()

    project_id = project_id or props.get("FIREBASE_PROJECT_ID")
    api_key = api_key or props.get("FIREBASE_API_KEY")
    if not project_id or not api_key:
        raise ValueError(
            "Missing FIREBASE_PROJECT_ID or FIREBASE_API_KEY in "
            f"{props_path} and environment."
        )
    return project_id, api_key


# ──────────────────────────────────────────────────────────────────────────────
# Firestore value conversions
# ──────────────────────────────────────────────────────────────────────────────

def _fs_to_python(val: dict) -> Any:
    """Convert a single Firestore value wrapper to a Python object."""
    if "stringValue" in val:
        return val["stringValue"]
    if "integerValue" in val:
        return int(val["integerValue"])
    if "doubleValue" in val:
        return float(val["doubleValue"])
    if "booleanValue" in val:
        return val["booleanValue"]
    if "nullValue" in val:
        return None
    if "timestampValue" in val:
        return val["timestampValue"]
    if "arrayValue" in val:
        return [_fs_to_python(v) for v in val["arrayValue"].get("values", [])]
    if "mapValue" in val:
        return {
            k: _fs_to_python(v) for k, v in val["mapValue"].get("fields", {}).items()
        }
    # geoPointValue, referenceValue, bytesValue — return raw
    return val


def parse_document(doc: dict) -> dict[str, Any]:
    """Parse a Firestore REST document into a flat Python dict.

    Adds a synthetic '_id' field extracted from the document name path.
    """
    fields = doc.get("fields", {})
    result: dict[str, Any] = {}
    for key, wrapper in fields.items():
        result[key] = _fs_to_python(wrapper)
    # Extract document ID from name path
    name = doc.get("name", "")
    result["_id"] = name.rsplit("/", 1)[-1] if name else ""
    result["_path"] = name
    return result


# ──────────────────────────────────────────────────────────────────────────────
# FirestoreClient
# ──────────────────────────────────────────────────────────────────────────────

class FirestoreClient:
    """Thin wrapper around the Firestore REST API with pagination support."""

    def __init__(self, project_id: str, api_key: str):
        self.project_id = project_id
        self.api_key = api_key
        self.base_url = (
            f"{FIRESTORE_BASE}/projects/{urlquote(project_id, safe='')}"
            f"/databases/(default)/documents"
        )
        self.session = requests.Session()
        self.session.headers["Accept"] = "application/json"

    # ── Core HTTP helpers ───────────────────────────────────────────────

    def _params(self, extra: dict | None = None) -> dict:
        p = {"key": self.api_key}
        if extra:
            p.update(extra)
        return p

    def _get(self, url: str, params: dict | None = None) -> dict:
        resp = self.session.get(url, params=self._params(params), timeout=30)
        if resp.status_code == 403:
            raise PermissionError(
                "403 Forbidden — Firestore security rules block reads on this "
                "collection with API-key auth. Only device_logs is readable."
            )
        resp.raise_for_status()
        return resp.json()

    def _delete(self, url: str) -> bool:
        resp = self.session.delete(url, params=self._params(), timeout=30)
        return resp.status_code in (200, 204)

    # ── Public methods ──────────────────────────────────────────────────

    def list_documents(
        self,
        collection: str,
        *,
        page_size: int = DEFAULT_PAGE_SIZE,
        max_docs: int | None = None,
    ) -> list[dict[str, Any]]:
        """Fetch all documents from a collection with automatic pagination.

        Args:
            collection: Firestore collection name.
            page_size: Documents per request (max 300).
            max_docs: Stop after this many documents (None = all).

        Returns:
            List of parsed document dicts.
        """
        url = f"{self.base_url}/{urlquote(collection, safe='')}"
        docs: list[dict[str, Any]] = []
        page_token: str | None = None

        while True:
            params: dict[str, Any] = {"pageSize": min(page_size, 300)}
            if page_token:
                params["pageToken"] = page_token

            data = self._get(url, params)
            for raw_doc in data.get("documents", []):
                docs.append(parse_document(raw_doc))
                if max_docs and len(docs) >= max_docs:
                    return docs

            page_token = data.get("nextPageToken")
            if not page_token:
                break

        return docs

    def get_document(self, collection: str, doc_id: str) -> dict[str, Any]:
        """Fetch a single document by collection and ID."""
        url = (
            f"{self.base_url}/{urlquote(collection, safe='')}"
            f"/{urlquote(doc_id, safe='')}"
        )
        raw = self._get(url)
        return parse_document(raw)

    def delete_document(self, collection: str, doc_id: str) -> bool:
        """Delete a single document. Returns True on success."""
        url = (
            f"{self.base_url}/{urlquote(collection, safe='')}"
            f"/{urlquote(doc_id, safe='')}"
        )
        return self._delete(url)

    def count_collection(self, collection: str) -> int:
        """Count documents in a collection (paginated, minimal fields)."""
        url = f"{self.base_url}/{urlquote(collection, safe='')}"
        count = 0
        page_token: str | None = None
        while True:
            params: dict[str, Any] = {"pageSize": 300}
            if page_token:
                params["pageToken"] = page_token
            data = self._get(url, params)
            count += len(data.get("documents", []))
            page_token = data.get("nextPageToken")
            if not page_token:
                break
        return count


# ──────────────────────────────────────────────────────────────────────────────
# Filtering helpers
# ──────────────────────────────────────────────────────────────────────────────

def parse_duration(s: str) -> timedelta:
    """Parse a human duration string like '1h', '30m', '7d', '2w'."""
    m = re.fullmatch(r"(\d+)\s*([smhdw])", s.strip().lower())
    if not m:
        raise argparse.ArgumentTypeError(
            f"Invalid duration '{s}'. Use e.g. 30s, 5m, 1h, 7d, 2w"
        )
    n = int(m.group(1))
    unit = m.group(2)
    mapping = {"s": "seconds", "m": "minutes", "h": "hours", "d": "days", "w": "weeks"}
    return timedelta(**{mapping[unit]: n})


def epoch_ms_now() -> int:
    return int(time.time() * 1000)


def filter_docs(
    docs: list[dict],
    *,
    device: str | None = None,
    level: str | None = None,
    event: str | None = None,
    since: timedelta | None = None,
    search: str | None = None,
    peer: str | None = None,
    fields: list[str] | None = None,
) -> list[dict]:
    """Apply client-side filters to a list of parsed documents."""
    result = []
    cutoff_ms = epoch_ms_now() - int(since.total_seconds() * 1000) if since else 0

    for doc in docs:
        # Device filter — check deviceId, sourceId, uploadedBy
        if device:
            doc_device = doc.get("deviceId") or doc.get("sourceId") or doc.get("uploadedBy") or ""
            if device.lower() not in str(doc_device).lower():
                continue

        # Event type filter
        if event:
            doc_event = doc.get("eventType", "")
            if event.lower() not in str(doc_event).lower():
                continue

        # Timestamp filter
        if since:
            ts = doc.get("timestamp") or doc.get("batchTimestamp") or 0
            if isinstance(ts, (int, float)) and ts < cutoff_ms:
                continue

        # Peer filter
        if peer:
            doc_peer = (
                doc.get("peerId")
                or doc.get("destId")
                or doc.get("peer")
                or ""
            )
            if peer.lower() not in str(doc_peer).lower():
                continue

        # Search (substring match across all string values)
        if search:
            haystack = " ".join(str(v) for v in doc.values())
            if search.lower() not in haystack.lower():
                continue

        # Level filter (for device_logs, requires expanding the logs JSON)
        # Handled separately in logs command — skip here for raw docs
        if level and "logs" not in doc:
            doc_level = doc.get("level", "")
            if level.upper() != str(doc_level).upper():
                continue

        # Field projection
        if fields:
            doc = {k: v for k, v in doc.items() if k in fields or k == "_id"}

        result.append(doc)

    return result


# ──────────────────────────────────────────────────────────────────────────────
# Log expansion (device_logs collection stores logs as JSON string)
# ──────────────────────────────────────────────────────────────────────────────

def expand_device_logs(
    docs: list[dict],
    *,
    level: str | None = None,
    search: str | None = None,
    since: timedelta | None = None,
) -> list[dict]:
    """Expand the 'logs' JSON field from device_logs documents into individual
    log entries, each inheriting the parent's deviceId/deviceModel.

    Returns a flat list of individual log entries.
    """
    cutoff_ms = epoch_ms_now() - int(since.total_seconds() * 1000) if since else 0
    entries: list[dict] = []

    for doc in docs:
        device_id = doc.get("deviceId", "?")
        device_model = doc.get("deviceModel", "?")
        logs_raw = doc.get("logs", "[]")

        try:
            log_array = json.loads(logs_raw) if isinstance(logs_raw, str) else logs_raw
        except (json.JSONDecodeError, TypeError):
            continue

        if not isinstance(log_array, list):
            continue

        for entry in log_array:
            if not isinstance(entry, dict):
                continue

            # Time filter
            if since and entry.get("ts", 0) < cutoff_ms:
                continue

            # Level filter
            if level and entry.get("lvl", "").upper() != level.upper():
                continue

            # Search filter
            if search and search.lower() not in entry.get("msg", "").lower():
                continue

            entries.append(
                {
                    "deviceId": device_id,
                    "model": device_model,
                    "id": entry.get("id", ""),
                    "timestamp": entry.get("ts", 0),
                    "time": _fmt_ts(entry.get("ts", 0)),
                    "level": entry.get("lvl", "?"),
                    "type": entry.get("type", "?"),
                    "message": entry.get("msg", ""),
                    "peer": entry.get("peer", ""),
                    "rssi": entry.get("rssi", ""),
                    "latency": entry.get("lat", ""),
                    "size": entry.get("sz", ""),
                }
            )

    # Sort by timestamp descending (newest first)
    entries.sort(key=lambda e: e.get("timestamp", 0), reverse=True)
    return entries


# ──────────────────────────────────────────────────────────────────────────────
# Formatting helpers
# ──────────────────────────────────────────────────────────────────────────────

def _fmt_ts(ms: int | float | str) -> str:
    """Format millisecond epoch timestamp to human-readable UTC string."""
    try:
        ms_int = int(ms)
        if ms_int <= 0:
            return "—"
        dt = datetime.fromtimestamp(ms_int / 1000, tz=timezone.utc)
        return dt.strftime("%Y-%m-%d %H:%M:%S")
    except (ValueError, TypeError, OSError):
        return str(ms)


def _truncate(s: str, max_len: int = 80) -> str:
    return s if len(s) <= max_len else s[: max_len - 1] + "…"


def format_table(rows: list[dict], columns: list[str] | None = None) -> str:
    """Format a list of dicts as a fixed-width text table."""
    if not rows:
        return "(no results)"

    if columns is None:
        # Discover columns from first rows, excluding internal _path
        seen: dict[str, None] = {}
        for row in rows[:20]:
            for k in row:
                if k != "_path":
                    seen[k] = None
        columns = list(seen.keys())

    # Calculate column widths
    widths = {c: len(c) for c in columns}
    str_rows = []
    for row in rows:
        sr = {}
        for c in columns:
            val = row.get(c, "")
            s = _truncate(str(val), 100) if isinstance(val, str) else str(val)
            sr[c] = s
            widths[c] = max(widths[c], len(s))
        str_rows.append(sr)

    # Cap column widths
    for c in widths:
        widths[c] = min(widths[c], 100)

    # Build table
    header = "  ".join(c.ljust(widths[c]) for c in columns)
    sep = "  ".join("─" * widths[c] for c in columns)
    lines = [header, sep]
    for sr in str_rows:
        lines.append("  ".join(sr.get(c, "").ljust(widths[c])[:widths[c]] for c in columns))
    return "\n".join(lines)


def format_json(rows: list[dict]) -> str:
    """Format rows as indented JSON."""
    # Remove internal fields
    clean = [{k: v for k, v in r.items() if not k.startswith("_")} for r in rows]
    return json.dumps(clean, indent=2, default=str)


def format_csv_str(rows: list[dict], columns: list[str] | None = None) -> str:
    """Format rows as CSV."""
    if not rows:
        return ""
    if columns is None:
        seen: dict[str, None] = {}
        for row in rows[:20]:
            for k in row:
                if not k.startswith("_"):
                    seen[k] = None
        columns = list(seen.keys())

    buf = io.StringIO()
    writer = csv.DictWriter(buf, fieldnames=columns, extrasaction="ignore")
    writer.writeheader()
    for row in rows:
        writer.writerow({k: row.get(k, "") for k in columns})
    return buf.getvalue()


def output_results(
    rows: list[dict],
    fmt: str = "table",
    output_file: str | None = None,
    columns: list[str] | None = None,
) -> None:
    """Format and print or write results."""
    if fmt == "json":
        text = format_json(rows)
    elif fmt == "csv":
        text = format_csv_str(rows, columns)
    else:
        text = format_table(rows, columns)

    if output_file:
        Path(output_file).write_text(text, encoding="utf-8")
        print(f"Wrote {len(rows)} rows to {output_file}")
    else:
        print(text)

    if not output_file:
        print(f"\n({len(rows)} results)")


# ──────────────────────────────────────────────────────────────────────────────
# Analysis reports
# ──────────────────────────────────────────────────────────────────────────────

def analyze_hmac(client: FirestoreClient, since: timedelta | None = None) -> None:
    """Analyze HMAC_INVALID events from device_logs."""
    print("Fetching device_logs…")
    docs = client.list_documents("device_logs")
    entries = expand_device_logs(docs, since=since)

    hmac_entries = [e for e in entries if "HMAC" in e.get("message", "").upper()]
    total = len(entries)
    hmac_count = len(hmac_entries)

    print(f"\n{'═' * 60}")
    print("HMAC ANALYSIS REPORT")
    print(f"{'═' * 60}")
    print(f"Total log entries:      {total:,}")
    print(f"HMAC-related entries:   {hmac_count:,} ({hmac_count/max(total,1)*100:.1f}%)")

    if not hmac_entries:
        print("No HMAC issues found.")
        return

    # By device
    by_device: Counter[str] = Counter()
    by_level: Counter[str] = Counter()
    by_peer: Counter[str] = Counter()
    for e in hmac_entries:
        by_device[e["deviceId"]] += 1
        by_level[e["level"]] += 1
        # Try to extract peer from message
        peer = e.get("peer", "")
        if not peer:
            # Parse from message like "HMAC_INVALID from PeerX"
            m = re.search(r"(?:from|peer|src)\s*[=:]?\s*(\S+)", e["message"], re.IGNORECASE)
            if m:
                peer = m.group(1)
        if peer:
            by_peer[peer] += 1

    print(f"\n  By device:")
    for dev, cnt in by_device.most_common():
        print(f"    {dev:30s}  {cnt:,}")

    print(f"\n  By level:")
    for lvl, cnt in by_level.most_common():
        print(f"    {lvl:30s}  {cnt:,}")

    if by_peer:
        print(f"\n  By peer (source of invalid HMAC):")
        for p, cnt in by_peer.most_common(10):
            print(f"    {p:30s}  {cnt:,}")

    # Timeline — group by hour
    print(f"\n  Timeline (hourly):")
    by_hour: Counter[str] = Counter()
    for e in hmac_entries:
        ts = e.get("timestamp", 0)
        if ts:
            by_hour[_fmt_ts(ts)[:13]] += 1
    for hour, cnt in sorted(by_hour.items()):
        bar = "█" * min(cnt // 2, 50)
        print(f"    {hour}  {cnt:4d}  {bar}")


def analyze_transfers(client: FirestoreClient, since: timedelta | None = None) -> None:
    """Analyze TRANSFER_FAILED events from device_logs."""
    print("Fetching device_logs…")
    docs = client.list_documents("device_logs")
    entries = expand_device_logs(docs, search="TRANSFER_FAILED", since=since)

    print(f"\n{'═' * 60}")
    print("TRANSFER FAILURE REPORT")
    print(f"{'═' * 60}")
    print(f"Total TRANSFER_FAILED entries: {len(entries):,}")

    if not entries:
        print("No transfer failures found.")
        return

    by_device: Counter[str] = Counter()
    for e in entries:
        by_device[e["deviceId"]] += 1

    print(f"\n  By device:")
    for dev, cnt in by_device.most_common():
        print(f"    {dev:30s}  {cnt:,}")


def analyze_errors(client: FirestoreClient, since: timedelta | None = None) -> None:
    """Analyze error_logs collection."""
    print("Fetching error_logs…")
    docs = client.list_documents("error_logs")

    if since:
        cutoff = epoch_ms_now() - int(since.total_seconds() * 1000)
        docs = [d for d in docs if d.get("timestamp", 0) >= cutoff]

    print(f"\n{'═' * 60}")
    print("ERROR LOG ANALYSIS")
    print(f"{'═' * 60}")
    print(f"Total batches: {len(docs):,}")

    by_device: Counter[str] = Counter()
    total_logs = 0
    for doc in docs:
        device = doc.get("deviceId", "?")
        count = doc.get("logCount", 0)
        by_device[device] += count
        total_logs += count

    print(f"Total error/warn entries: {total_logs:,}")
    print(f"\n  By device:")
    for dev, cnt in by_device.most_common():
        print(f"    {dev:30s}  {cnt:,}")


def analyze_connections(client: FirestoreClient, since: timedelta | None = None) -> None:
    """Analyze connections collection — connection/disconnection patterns."""
    print("Fetching connections…")
    docs = client.list_documents("connections")

    if since:
        cutoff = epoch_ms_now() - int(since.total_seconds() * 1000)
        docs = [d for d in docs if d.get("timestamp", 0) >= cutoff]

    print(f"\n{'═' * 60}")
    print("CONNECTION ANALYSIS")
    print(f"{'═' * 60}")
    print(f"Total events: {len(docs):,}")

    connected = [d for d in docs if d.get("event") == "CONNECTED"]
    disconnected = [d for d in docs if d.get("event") == "DISCONNECTED"]
    print(f"  CONNECTED:     {len(connected):,}")
    print(f"  DISCONNECTED:  {len(disconnected):,}")

    by_pair: Counter[str] = Counter()
    for doc in docs:
        pair = f"{doc.get('deviceId','?')} ↔ {doc.get('peerId','?')}"
        by_pair[pair] += 1

    print(f"\n  Connection frequency by pair:")
    for pair, cnt in by_pair.most_common(10):
        print(f"    {pair:50s}  {cnt:,}")


def analyze_network(client: FirestoreClient, since: timedelta | None = None) -> None:
    """Full network health analysis: stats, routing, connections."""
    print("Fetching stats…")
    stats = client.list_documents("stats")

    if since:
        cutoff = epoch_ms_now() - int(since.total_seconds() * 1000)
        stats = [d for d in stats if d.get("timestamp", 0) >= cutoff]

    print(f"\n{'═' * 60}")
    print("NETWORK HEALTH REPORT")
    print(f"{'═' * 60}")
    print(f"Total stat snapshots: {len(stats):,}")

    if not stats:
        print("No stats data available.")
        return

    by_device: dict[str, list] = defaultdict(list)
    for s in stats:
        by_device[s.get("deviceId", "?")].append(s)

    for device, snapshots in sorted(by_device.items()):
        snapshots.sort(key=lambda s: s.get("timestamp", 0))
        latest = snapshots[-1]
        print(f"\n  {device}:")
        print(f"    Snapshots:      {len(snapshots)}")
        print(f"    Neighbors:      {latest.get('currentNeighborCount', '?')}")
        print(f"    Routes:         {latest.get('currentRouteCount', '?')}")
        print(f"    Avg RTT:        {latest.get('avgRttMs', '?')} ms")
        print(f"    Bytes sent:     {latest.get('totalBytesSent', 0):,}")
        print(f"    Bytes recv:     {latest.get('totalBytesReceived', 0):,}")
        print(f"    Packets sent:   {latest.get('totalPacketsSent', 0):,}")
        print(f"    Packets recv:   {latest.get('totalPacketsReceived', 0):,}")
        print(f"    Packets fwd:    {latest.get('totalPacketsForwarded', 0):,}")
        print(f"    Packets drop:   {latest.get('totalPacketsDropped', 0):,}")
        print(f"    S&F queued:     {latest.get('storeForwardQueued', 0):,}")
        print(f"    S&F delivered:  {latest.get('storeForwardDelivered', 0):,}")
        print(f"    Battery:        {latest.get('batteryLevel', '?')}%")
        print(f"    Advertising:    {latest.get('isAdvertising', '?')}")
        print(f"    Discovering:    {latest.get('isDiscovering', '?')}")

        # RTT anomaly detection
        rtts = [s.get("avgRttMs", 0) for s in snapshots if s.get("avgRttMs", 0) > 0]
        if rtts:
            avg_rtt = sum(rtts) / len(rtts)
            max_rtt = max(rtts)
            anomalies = [r for r in rtts if r > 10000]
            print(f"    RTT avg/max:    {avg_rtt:.0f}/{max_rtt:,} ms")
            if anomalies:
                print(f"    ⚠ RTT anomalies (>10s): {len(anomalies)} readings")


def run_analyze(args: argparse.Namespace, client: FirestoreClient) -> None:
    """Dispatch to the appropriate analysis report."""
    since = parse_duration(args.since) if args.since else None

    report = args.report.lower()
    if report == "hmac":
        analyze_hmac(client, since)
    elif report == "transfers":
        analyze_transfers(client, since)
    elif report == "errors":
        analyze_errors(client, since)
    elif report == "connections":
        analyze_connections(client, since)
    elif report == "network":
        analyze_network(client, since)
    elif report == "all":
        analyze_hmac(client, since)
        analyze_transfers(client, since)
        analyze_errors(client, since)
        analyze_connections(client, since)
        analyze_network(client, since)
    else:
        print(f"Unknown report: {report}. Choose from: hmac, transfers, errors, connections, network, all")
        sys.exit(1)


# ──────────────────────────────────────────────────────────────────────────────
# CLI subcommands
# ──────────────────────────────────────────────────────────────────────────────

def cmd_list(args: argparse.Namespace, client: FirestoreClient) -> None:
    """List documents from any collection with optional filters."""
    collection = args.collection
    if collection not in ALL_COLLECTIONS:
        print(f"Unknown collection '{collection}'. Available: {', '.join(ALL_COLLECTIONS)}")
        sys.exit(1)

    since = parse_duration(args.since) if args.since else None
    flds = args.fields.split(",") if args.fields else None

    print(f"Fetching {collection}…")
    docs = client.list_documents(collection, max_docs=args.limit or None)
    docs = filter_docs(
        docs,
        device=args.device,
        event=args.event,
        since=since,
        search=args.search,
        peer=args.peer,
        fields=flds,
    )

    if args.limit:
        docs = docs[: args.limit]

    output_results(docs, fmt=args.format, output_file=args.output, columns=flds)


def cmd_logs(args: argparse.Namespace, client: FirestoreClient) -> None:
    """Query device_logs with per-entry filtering (expands embedded JSON)."""
    since = parse_duration(args.since) if args.since else None

    print("Fetching device_logs…")
    docs = client.list_documents("device_logs")

    # Pre-filter batches by device before expanding
    if args.device:
        docs = [d for d in docs if args.device.lower() in str(d.get("deviceId", "")).lower()]

    entries = expand_device_logs(docs, level=args.level, search=args.search, since=since)

    if args.peer:
        entries = [e for e in entries if args.peer.lower() in str(e.get("peer", "")).lower()]

    if args.limit:
        entries = entries[: args.limit]

    columns = ["time", "deviceId", "level", "type", "message", "peer"]
    if args.verbose:
        columns.extend(["rssi", "latency", "size", "model"])

    output_results(entries, fmt=args.format, output_file=args.output, columns=columns)


def cmd_stats(args: argparse.Namespace, client: FirestoreClient) -> None:
    """Query stats snapshots."""
    since = parse_duration(args.since) if args.since else None

    print("Fetching stats…")
    docs = client.list_documents("stats")
    docs = filter_docs(docs, device=args.device, since=since)

    # Sort by timestamp descending
    docs.sort(key=lambda d: d.get("timestamp", 0), reverse=True)

    if args.limit:
        docs = docs[: args.limit]

    # Add formatted time
    for d in docs:
        d["time"] = _fmt_ts(d.get("timestamp", 0))

    columns = [
        "time", "deviceId", "currentNeighborCount", "currentRouteCount",
        "avgRttMs", "totalPacketsSent", "totalPacketsReceived",
        "totalPacketsForwarded", "totalPacketsDropped", "batteryLevel",
    ]

    output_results(docs, fmt=args.format, output_file=args.output, columns=columns)


def cmd_relay(args: argparse.Namespace, client: FirestoreClient) -> None:
    """Query mesh_relay messages."""
    since = parse_duration(args.since) if args.since else None

    print("Fetching mesh_relay…")
    docs = client.list_documents("mesh_relay")
    docs = filter_docs(docs, device=args.device, since=since, search=args.search, peer=args.dest)

    docs.sort(key=lambda d: d.get("timestamp", 0), reverse=True)

    if args.limit:
        docs = docs[: args.limit]

    for d in docs:
        d["time"] = _fmt_ts(d.get("timestamp", 0))

    columns = ["time", "packetId", "sourceId", "destId", "gatewayId", "ttl"]
    if args.verbose:
        columns.append("payload")

    output_results(docs, fmt=args.format, output_file=args.output, columns=columns)


def cmd_devices(args: argparse.Namespace, client: FirestoreClient) -> None:
    """List registered devices."""
    print("Fetching devices…")
    docs = client.list_documents("devices")

    if args.device:
        docs = [d for d in docs if args.device.lower() in str(d.get("deviceId", "")).lower()]

    for d in docs:
        d["registered"] = _fmt_ts(d.get("timestamp", 0))

    columns = ["deviceId", "model", "manufacturer", "androidVersion", "appVersion", "registered"]
    output_results(docs, fmt=args.format, output_file=args.output, columns=columns)


def cmd_export(args: argparse.Namespace, client: FirestoreClient) -> None:
    """Export an entire collection to file."""
    collection = args.collection
    if collection not in ALL_COLLECTIONS:
        print(f"Unknown collection '{collection}'. Available: {', '.join(ALL_COLLECTIONS)}")
        sys.exit(1)

    print(f"Fetching all documents from {collection}…")
    docs = client.list_documents(collection)

    fmt = args.format or "json"
    output_file = args.output or f"{collection}_export.{fmt}"

    # For device_logs, optionally expand entries
    if collection == "device_logs" and args.expand:
        docs = expand_device_logs(docs)
        print(f"Expanded to {len(docs)} individual log entries")

    output_results(docs, fmt=fmt, output_file=output_file)


def cmd_cleanup(args: argparse.Namespace, client: FirestoreClient) -> None:
    """Delete expired documents from a collection."""
    collection = args.collection
    if collection not in ALL_COLLECTIONS:
        print(f"Unknown collection '{collection}'. Available: {', '.join(ALL_COLLECTIONS)}")
        sys.exit(1)

    older_than = parse_duration(args.older_than)
    cutoff_ms = epoch_ms_now() - int(older_than.total_seconds() * 1000)

    print(f"Fetching {collection} for cleanup…")
    docs = client.list_documents(collection)

    expired = []
    for doc in docs:
        ts = doc.get("expiresAt") or doc.get("timestamp") or doc.get("batchTimestamp") or 0
        if isinstance(ts, (int, float)) and ts < cutoff_ms:
            expired.append(doc)

    print(f"Found {len(expired)} documents older than {args.older_than}")

    if not expired:
        print("Nothing to clean up.")
        return

    if args.dry_run:
        print("[DRY RUN] Would delete:")
        for doc in expired[:20]:
            print(f"  {doc['_id']}  (ts: {_fmt_ts(doc.get('timestamp', 0))})")
        if len(expired) > 20:
            print(f"  … and {len(expired) - 20} more")
        return

    # Confirm before deleting
    if not args.yes:
        answer = input(f"Delete {len(expired)} documents? [y/N] ").strip().lower()
        if answer != "y":
            print("Aborted.")
            return

    deleted = 0
    for doc in expired:
        if client.delete_document(collection, doc["_id"]):
            deleted += 1
            if deleted % 10 == 0:
                print(f"  Deleted {deleted}/{len(expired)}…")

    print(f"Deleted {deleted}/{len(expired)} documents.")


def cmd_summary(args: argparse.Namespace, client: FirestoreClient) -> None:
    """Show a quick summary of all collections."""
    print(f"{'═' * 50}")
    print("FIRESTORE COLLECTION SUMMARY")
    print(f"{'═' * 50}")
    print(f"{'Collection':<25s}  {'Documents':>10s}")
    print(f"{'─' * 25}  {'─' * 10}")

    total = 0
    for coll in ALL_COLLECTIONS:
        try:
            count = client.count_collection(coll)
            total += count
            print(f"{coll:<25s}  {count:>10,}")
        except Exception as e:
            print(f"{coll:<25s}  {'error':>10s}  ({e})")

    print(f"{'─' * 25}  {'─' * 10}")
    print(f"{'TOTAL':<25s}  {total:>10,}")


def cmd_get(args: argparse.Namespace, client: FirestoreClient) -> None:
    """Get a single document by collection and ID."""
    doc = client.get_document(args.collection, args.doc_id)
    print(json.dumps({k: v for k, v in doc.items() if not k.startswith("_")}, indent=2, default=str))


# ──────────────────────────────────────────────────────────────────────────────
# Argument parser
# ──────────────────────────────────────────────────────────────────────────────

def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="firebase_query",
        description="Query and analyze Samizdat Firestore collections.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
EXAMPLES:
  # Quick summary of all collections
  %(prog)s summary

  # Browse recent logs
  %(prog)s logs --since 1h
  %(prog)s logs --device PhoneA --level ERROR
  %(prog)s logs --search "HMAC" --format json

  # View stats snapshots
  %(prog)s stats --device PhoneA --since 24h

  # List any collection with filters
  %(prog)s list connections --device PhoneA --since 6h
  %(prog)s list routing --limit 5

  # Relay messages
  %(prog)s relay --dest PhoneB

  # Registered devices
  %(prog)s devices

  # Get a single document
  %(prog)s get device_logs <document-id>

  # Analysis reports
  %(prog)s analyze hmac
  %(prog)s analyze network --since 24h
  %(prog)s analyze all

  # Export entire collection
  %(prog)s export device_logs --format json
  %(prog)s export device_logs --expand  # expand per-entry
  %(prog)s export stats --format csv --output stats.csv

  # Cleanup expired data
  %(prog)s cleanup device_logs --older-than 3d --dry-run
  %(prog)s cleanup mesh_relay --older-than 24h --yes

COLLECTIONS:
  device_logs          System/chat logs (batched, JSON-embedded entries)
  mesh_relay           Cloud relay queue for cross-mesh messaging
  devices              Device registrations (one per device)
  stats                Periodic network statistics snapshots
  routing              Routing table snapshots
  connections          Connection/disconnection events
  error_logs           ERROR and WARN level logs
  test_results         Automated test results
  store_forward        Store-and-forward delivery reports
  endurance_reports    Endurance test completion reports
  endurance_snapshots  Endurance test progress snapshots
""",
    )

    # Common arguments
    common = argparse.ArgumentParser(add_help=False)
    common.add_argument("--device", "-d", help="Filter by device ID (substring match)")
    common.add_argument("--since", "-s", help="Only show data newer than duration (e.g. 1h, 7d)")
    common.add_argument("--search", "-q", help="Full-text search across all fields")
    common.add_argument("--limit", "-n", type=int, help="Max results to return")
    common.add_argument("--format", "-f", choices=["table", "json", "csv"], default="table")
    common.add_argument("--output", "-o", help="Write output to file instead of stdout")
    common.add_argument("--verbose", "-v", action="store_true", help="Show additional columns")

    sub = parser.add_subparsers(dest="command", required=True)

    # summary
    sub.add_parser("summary", help="Show document counts for all collections")

    # list
    p_list = sub.add_parser("list", parents=[common], help="List docs from any collection")
    p_list.add_argument("collection", choices=ALL_COLLECTIONS, help="Collection name")
    p_list.add_argument("--event", "-e", help="Filter by eventType")
    p_list.add_argument("--peer", "-p", help="Filter by peer/dest ID")
    p_list.add_argument("--fields", help="Comma-separated field names to show")

    # logs
    p_logs = sub.add_parser("logs", parents=[common], help="Query device_logs (expanded entries)")
    p_logs.add_argument("--level", "-l", choices=["ERROR", "WARN", "INFO", "DEBUG", "TRACE"])
    p_logs.add_argument("--peer", "-p", help="Filter by peer name")

    # stats
    sub.add_parser("stats", parents=[common], help="Query stats snapshots")

    # relay
    p_relay = sub.add_parser("relay", parents=[common], help="Query mesh_relay messages")
    p_relay.add_argument("--dest", help="Filter by destination device ID")

    # devices
    sub.add_parser("devices", parents=[common], help="List registered devices")

    # get
    p_get = sub.add_parser("get", help="Get a single document by ID")
    p_get.add_argument("collection", choices=ALL_COLLECTIONS)
    p_get.add_argument("doc_id", help="Document ID")

    # analyze
    p_analyze = sub.add_parser("analyze", parents=[common], help="Run analysis reports")
    p_analyze.add_argument(
        "report",
        choices=["hmac", "transfers", "errors", "connections", "network", "all"],
        help="Report type",
    )

    # export
    p_export = sub.add_parser("export", parents=[common], help="Export full collection to file")
    p_export.add_argument("collection", choices=ALL_COLLECTIONS)
    p_export.add_argument("--expand", action="store_true", help="Expand embedded JSON (device_logs)")

    # cleanup
    p_cleanup = sub.add_parser("cleanup", help="Delete expired documents")
    p_cleanup.add_argument("collection", choices=ALL_COLLECTIONS)
    p_cleanup.add_argument("--older-than", required=True, help="Delete docs older than (e.g. 3d)")
    p_cleanup.add_argument("--dry-run", action="store_true", help="Preview without deleting")
    p_cleanup.add_argument("--yes", "-y", action="store_true", help="Skip confirmation prompt")

    return parser


# ──────────────────────────────────────────────────────────────────────────────
# Main
# ──────────────────────────────────────────────────────────────────────────────

def main() -> None:
    parser = build_parser()
    args = parser.parse_args()

    try:
        project_id, api_key = load_credentials()
    except (FileNotFoundError, ValueError) as e:
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(1)

    client = FirestoreClient(project_id, api_key)

    dispatch = {
        "summary": lambda a, c: cmd_summary(a, c),
        "list": cmd_list,
        "logs": cmd_logs,
        "stats": cmd_stats,
        "relay": cmd_relay,
        "devices": cmd_devices,
        "get": cmd_get,
        "analyze": run_analyze,
        "export": cmd_export,
        "cleanup": cmd_cleanup,
    }

    handler = dispatch.get(args.command)
    if handler:
        try:
            handler(args, client)
        except PermissionError as e:
            print(f"ACCESS DENIED: {e}", file=sys.stderr)
            print(
                "\nHint: Only 'device_logs' is readable via API key. "
                "Other collections require Firebase Auth or updated security rules.",
                file=sys.stderr,
            )
            sys.exit(1)
        except requests.HTTPError as e:
            print(f"HTTP ERROR: {e}", file=sys.stderr)
            sys.exit(1)
    else:
        parser.print_help()


if __name__ == "__main__":
    main()