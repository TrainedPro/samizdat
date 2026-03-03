# Samizdat Web Dashboard

A standalone web dashboard for monitoring the Samizdat mesh network in real time.

## Quick Start

1. Open `index.html` in any modern browser (no build step needed).
2. Click **⚙ Settings** and enter your Firebase credentials:
   - **Project ID:** `samizdat` (or your Firebase project)
   - **API Key:** Your Firebase Web API key
3. Click **Save & Connect**.

## Features

| Feature           | Description                                                                 |
| ----------------- | --------------------------------------------------------------------------- |
| **Device List**   | All registered mesh nodes with live status, battery, uptime, peer count     |
| **Packets Chart** | Line chart of sent/received/forwarded packets over time                     |
| **Battery Chart** | Bar chart of current battery levels across all devices                      |
| **RTT Chart**     | Average round-trip time per device                                          |
| **Mesh Topology** | Canvas visualization of connected mesh nodes                                |
| **Event Log**     | Filterable telemetry event stream (stats, connection, error, routing, test) |
| **Cloud Relay**   | Monitor messages in the Firestore relay queue                               |
| **Test Results**  | View automated test suite results per device                                |

## Data Sources

The dashboard reads from three Firestore collections:

- `devices` — Device registration and status snapshots
- `telemetry` — Telemetry events (stats, connections, errors, routing, tests)
- `mesh_relay` — Cloud relay message queue

These are populated by the Android app's `TelemetryManager` / `TelemetryUploadWorker` and `InternetGatewayManager`.

## Auto-Refresh

Click the **Auto** button to enable periodic refresh (default: 30 seconds). The interval is configurable in Settings.

## Technology

- Pure HTML/CSS/JS — no build tools, no Node.js
- [Chart.js 4.x](https://www.chartjs.org/) via CDN for charts
- Firestore REST API (no Firebase SDK dependency)
- Credentials stored in `localStorage` (never sent to any server except Google's Firestore)

## Deployment

Since it's static HTML, you can serve it from anywhere:

```bash
# Local
python3 -m http.server 8080 -d dashboard/

# Or simply open index.html in your browser
open dashboard/index.html
```
