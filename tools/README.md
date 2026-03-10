# Firebase Query Tool

CLI for querying and analyzing Samizdat Firestore collections.

## Quick Start

```bash
# Requires Python 3.10+ and requests
pip install requests

# Credentials auto-read from local.properties (FIREBASE_PROJECT_ID, FIREBASE_API_KEY)
# Or set env vars: export FIREBASE_PROJECT_ID=samizdat FIREBASE_API_KEY=...

# Summary of all collections
python tools/firebase_query.py summary

# Browse recent logs
python tools/firebase_query.py logs --since 1h

# HMAC analysis
python tools/firebase_query.py analyze hmac
```

## Commands

### `summary`
Document counts for all 11 collections.
```bash
python tools/firebase_query.py summary
```

### `logs`
Query `device_logs` with automatic expansion of embedded JSON log entries.
```bash
python tools/firebase_query.py logs --device PhoneA --level ERROR --since 1h
python tools/firebase_query.py logs --search "HMAC" --format json
python tools/firebase_query.py logs --peer PhoneB --limit 50
python tools/firebase_query.py logs --verbose   # show rssi, latency, size
```

### `list`
List documents from any collection with filters.
```bash
python tools/firebase_query.py list connections --device PhoneA --since 6h
python tools/firebase_query.py list routing --limit 5 --format csv
python tools/firebase_query.py list store_forward --fields packetId,sourceId,destId
```

### `stats`
Query network statistics snapshots.
```bash
python tools/firebase_query.py stats --device PhoneA --since 24h
python tools/firebase_query.py stats --format csv --output stats.csv
```

### `relay`
Query mesh relay queue.
```bash
python tools/firebase_query.py relay --dest PhoneB
python tools/firebase_query.py relay --since 1h --verbose  # includes payload
```

### `devices`
List registered devices.
```bash
python tools/firebase_query.py devices
```

### `get`
Fetch a single document by ID.
```bash
python tools/firebase_query.py get device_logs <document-id>
```

### `analyze`
Run analysis reports: `hmac`, `transfers`, `errors`, `connections`, `network`, `all`.
```bash
python tools/firebase_query.py analyze hmac
python tools/firebase_query.py analyze network --since 24h
python tools/firebase_query.py analyze all
```

### `export`
Export an entire collection to file.
```bash
python tools/firebase_query.py export device_logs --format json
python tools/firebase_query.py export device_logs --expand  # individual log entries
python tools/firebase_query.py export stats --format csv --output stats.csv
```

### `cleanup`
Delete expired documents.
```bash
python tools/firebase_query.py cleanup device_logs --older-than 3d --dry-run
python tools/firebase_query.py cleanup mesh_relay --older-than 24h --yes
```

## Firestore Collections

| Collection            | Description                                     | Key Fields                                        |
|-----------------------|-------------------------------------------------|---------------------------------------------------|
| `device_logs`         | Batched system/chat logs (JSON-embedded entries) | deviceId, deviceModel, logs (JSON), batchTimestamp |
| `mesh_relay`          | Cloud relay queue for cross-mesh messaging       | packetId, sourceId, destId, gateway, ttl, payload |
| `devices`             | Device registrations                             | deviceId, model, manufacturer, androidVersion     |
| `stats`               | Periodic network statistics snapshots            | deviceId, avgRttMs, neighbors, routes, battery    |
| `routing`             | Routing table snapshots                          | deviceId, routes, hopCounts                       |
| `connections`         | Connection/disconnection events                  | deviceId, peerId, event, timestamp                |
| `error_logs`          | ERROR and WARN level logs                        | deviceId, logCount, logs                          |
| `test_results`        | Automated test results                           | testName, status, duration, deviceId              |
| `store_forward`       | Store-and-forward delivery reports               | packetId, sourceId, destId, delivered, hops       |
| `endurance_reports`   | Endurance test final reports                     | testDuration, messagesTotal, deviceStats          |
| `endurance_snapshots` | Endurance test progress snapshots                | elapsedMs, messagesThisInterval                   |

## Log Entry Fields (expanded from device_logs)

| Field     | Type   | Description                        |
|-----------|--------|------------------------------------|
| id        | string | Log entry UUID                     |
| ts        | int    | Epoch milliseconds                 |
| lvl       | string | INFO, WARN, ERROR, DEBUG, TRACE    |
| type      | string | Event type (HMAC_INVALID, etc.)    |
| msg       | string | Human-readable message             |
| peer      | string | Peer device name (if applicable)   |
| rssi      | int    | Signal strength (optional)         |
| lat       | int    | Latency in ms (optional)           |
| sz        | int    | Payload size in bytes (optional)   |

## Common Options

| Flag            | Short | Description                              |
|-----------------|-------|------------------------------------------|
| `--device`      | `-d`  | Filter by device ID (substring match)    |
| `--since`       | `-s`  | Time window (e.g. `30m`, `1h`, `7d`)    |
| `--search`      | `-q`  | Full-text search across all fields       |
| `--limit`       | `-n`  | Max results to return                    |
| `--format`      | `-f`  | Output format: `table`, `json`, `csv`    |
| `--output`      | `-o`  | Write to file instead of stdout          |
| `--verbose`     | `-v`  | Show additional columns                  |

## Environment Variables

| Variable              | Description                           | Default                   |
|-----------------------|---------------------------------------|---------------------------|
| `FIREBASE_PROJECT_ID` | Google Cloud project ID               | Read from local.properties |
| `FIREBASE_API_KEY`    | Firebase Web API key                  | Read from local.properties |

## Access Limitations

Only `device_logs` is currently readable via API key authentication. The other 10 collections return 403 Forbidden due to Firestore security rules requiring Firebase Auth. The tool handles this gracefully with a clear error message.

To enable access to all collections, either:
1. Update Firestore security rules to allow read access with API key
2. Add Firebase Auth token support to this tool

## Troubleshooting

- **`requests` not found**: Run `pip install requests`
- **403 Forbidden**: Collection requires Firebase Auth (see above)
- **Could not find local.properties**: Set env vars or run from project root
- **Empty results**: Try broader filters or `summary` to check data availability