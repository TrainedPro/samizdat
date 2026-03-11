/**
 * Samizdat Mesh Network Dashboard — JavaScript
 *
 * Reads telemetry data from Firebase Firestore REST API and renders:
 * - Device list with live status
 * - Packet/bandwidth charts (Chart.js)
 * - RTT distribution histogram
 * - Mesh topology visualization (Canvas 2D)
 * - Event log with filtering
 * - Cloud relay queue monitor
 * - Test results viewer
 */

// ============================================================
// State
// ============================================================

let config = {
  projectId: "",
  apiKey: "",
  refreshInterval: 30,
};

let autoRefreshTimer = null;
let allDevices = [];
let allEvents = [];
let allRelayMessages = [];
let allDeviceLogs = [];
let charts = {};

// ============================================================
// Initialization
// ============================================================

document.addEventListener("DOMContentLoaded", () => {
  loadSettings();
  initCharts();
  if (config.projectId && config.apiKey) {
    refreshAll();
  }
});

function loadSettings() {
  try {
    const saved = localStorage.getItem("samizdat_dashboard_config");
    if (saved) {
      const parsed = JSON.parse(saved);
      config = { ...config, ...parsed };
      document.getElementById("input-project-id").value = config.projectId;
      document.getElementById("input-api-key").value = config.apiKey;
      document.getElementById("input-refresh-interval").value =
        config.refreshInterval;
    }
  } catch (e) {
    console.warn("Failed to load settings:", e);
  }
}

function saveSettings() {
  config.projectId = document.getElementById("input-project-id").value.trim();
  config.apiKey = document.getElementById("input-api-key").value.trim();
  config.refreshInterval =
    parseInt(document.getElementById("input-refresh-interval").value) || 30;
  localStorage.setItem("samizdat_dashboard_config", JSON.stringify(config));
  toggleSettings();
  refreshAll();
}

function toggleSettings() {
  const panel = document.getElementById("settings-panel");
  panel.classList.toggle("hidden");
}

function toggleAutoRefresh() {
  const btn = document.getElementById("btn-auto");
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer);
    autoRefreshTimer = null;
    btn.textContent = "Auto: OFF";
    btn.classList.remove("btn-primary");
    btn.classList.add("btn-outline");
  } else {
    autoRefreshTimer = setInterval(refreshAll, config.refreshInterval * 1000);
    btn.textContent = `Auto: ${config.refreshInterval}s`;
    btn.classList.remove("btn-outline");
    btn.classList.add("btn-primary");
  }
}

// ============================================================
// Firestore REST API Helpers
// ============================================================

function firestoreUrl(collection, query) {
  const base = `https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/(default)/documents`;
  if (query) {
    return `${base}:runQuery?key=${config.apiKey}`;
  }
  return `${base}/${collection}?key=${config.apiKey}&pageSize=500`;
}

async function firestoreGet(collection) {
  const url = firestoreUrl(collection);
  const resp = await fetch(url);
  if (!resp.ok) throw new Error(`HTTP ${resp.status}: ${resp.statusText}`);
  const data = await resp.json();
  return (data.documents || []).map(parseDocument);
}

function parseDocument(doc) {
  const fields = doc.fields || {};
  const result = { _id: doc.name };
  for (const [key, val] of Object.entries(fields)) {
    result[key] = parseFirestoreValue(val);
  }
  return result;
}

function parseFirestoreValue(val) {
  if (val.stringValue !== undefined) return val.stringValue;
  if (val.integerValue !== undefined) return parseInt(val.integerValue);
  if (val.doubleValue !== undefined) return val.doubleValue;
  if (val.booleanValue !== undefined) return val.booleanValue;
  if (val.timestampValue !== undefined)
    return new Date(val.timestampValue).getTime();
  if (val.mapValue) {
    const result = {};
    for (const [k, v] of Object.entries(val.mapValue.fields || {})) {
      result[k] = parseFirestoreValue(v);
    }
    return result;
  }
  if (val.arrayValue) {
    return (val.arrayValue.values || []).map(parseFirestoreValue);
  }
  return null;
}

// ============================================================
// Data Fetching
// ============================================================

async function refreshAll() {
  if (!config.projectId || !config.apiKey) {
    setStatus("⚠ Configure Firebase credentials in Settings", "disconnected");
    return;
  }

  setStatus("⏳ Loading data...", "loading");
  const btn = document.getElementById("btn-refresh");
  btn.disabled = true;

  try {
    const [devices, events, relay, deviceLogs] = await Promise.all([
      firestoreGet("devices").catch(() => []),
      firestoreGet("telemetry").catch(() => []),
      firestoreGet("mesh_relay").catch(() => []),
      firestoreGet("device_logs").catch(() => []),
    ]);

    allDevices = devices;
    allEvents = events.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
    allRelayMessages = relay;
    allDeviceLogs = deviceLogs.sort(
      (a, b) => (b.batchTimestamp || 0) - (a.batchTimestamp || 0),
    );

    renderDevices();
    renderEvents();
    renderRelayQueue();
    renderDeviceLogs();
    updateSummaryCards();
    updateCharts();
    renderTestResults();

    const now = new Date().toLocaleTimeString();
    document.getElementById("last-refresh").textContent = `Last: ${now}`;
    setStatus(
      `✓ Connected — ${devices.length} devices, ${events.length} events`,
      "connected",
    );
  } catch (err) {
    console.error("Refresh failed:", err);
    setStatus(`✗ Error: ${err.message}`, "disconnected");
  } finally {
    btn.disabled = false;
  }
}

async function refreshRelay() {
  try {
    allRelayMessages = await firestoreGet("mesh_relay");
    renderRelayQueue();
  } catch (err) {
    console.error("Relay refresh failed:", err);
  }
}

function setStatus(text, type) {
  const bar = document.getElementById("status-bar");
  const span = document.getElementById("status-text");
  bar.className = `status-bar status-${type}`;
  span.textContent = text;
}

// ============================================================
// Rendering — Devices
// ============================================================

function renderDevices() {
  const tbody = document.getElementById("device-tbody");
  if (allDevices.length === 0) {
    tbody.innerHTML =
      '<tr><td colspan="7" class="empty-row">No devices registered</td></tr>';
    return;
  }

  const now = Date.now();
  const fifteenMin = 15 * 60 * 1000;

  tbody.innerHTML = allDevices
    .map((d) => {
      const lastSeen = d.lastSeen || d.timestamp || 0;
      const isActive = now - lastSeen < fifteenMin;
      const statusClass = isActive ? "device-active" : "device-inactive";
      const statusIcon = isActive ? "🟢" : "⚫";
      const battery = d.batteryLevel ?? d.battery ?? -1;
      const batteryHtml = renderBattery(battery);
      const uptime = d.uptimeMs ? formatDuration(d.uptimeMs) : "—";
      const neighbors = d.neighborCount ?? d.currentNeighborCount ?? 0;
      const sent = d.totalPacketsSent ?? 0;
      const recv = d.totalPacketsReceived ?? 0;

      return `<tr>
            <td><span class="${statusClass}">${statusIcon}</span> ${escHtml(d.deviceName || d.deviceId || "?")}</td>
            <td>${escHtml(d.model || d.device || "—")}</td>
            <td>${formatTimestamp(lastSeen)}</td>
            <td>${uptime}</td>
            <td>${batteryHtml}</td>
            <td>${neighbors}</td>
            <td>${sent} ↑ / ${recv} ↓</td>
        </tr>`;
    })
    .join("");
}

function filterDevices() {
  const q = document.getElementById("device-search").value.toLowerCase();
  const rows = document.querySelectorAll("#device-tbody tr");
  rows.forEach((row) => {
    row.style.display = row.textContent.toLowerCase().includes(q) ? "" : "none";
  });
}

function renderBattery(level) {
  if (level < 0) return '<span class="device-inactive">—</span>';
  const cls =
    level > 50 ? "battery-high" : level > 20 ? "battery-mid" : "battery-low";
  return `<div class="battery-bar"><div class="battery-fill ${cls}" style="width:${level}%"></div></div>${level}%`;
}

// ============================================================
// Rendering — Events
// ============================================================

function renderEvents() {
  const log = document.getElementById("event-log");
  if (allEvents.length === 0) {
    log.innerHTML = '<div class="empty-row">No telemetry events</div>';
    return;
  }

  log.innerHTML = allEvents
    .slice(0, 200)
    .map((e) => {
      const type = e.eventType || e.type || "UNKNOWN";
      const typeCls = getEventTypeClass(type);
      const device = e.deviceId || e.deviceName || "?";
      const time = formatTimestamp(e.timestamp || 0);
      const body = summarizeEvent(e);

      return `<div class="event-item" data-type="${type}" data-text="${escAttr(body + " " + device)}">
            <span class="event-time">${time}</span>
            <span class="event-type ${typeCls}">${type.replace(/_/g, " ")}</span>
            <span class="event-device">${escHtml(device)}</span>
            <span class="event-body">${escHtml(body)}</span>
        </div>`;
    })
    .join("");
}

function filterEvents() {
  const typeFilter = document.getElementById("log-type-filter").value;
  const search = document.getElementById("log-search").value.toLowerCase();
  const items = document.querySelectorAll(".event-item");
  items.forEach((item) => {
    const matchType = !typeFilter || item.dataset.type === typeFilter;
    const matchSearch =
      !search || item.dataset.text.toLowerCase().includes(search);
    item.style.display = matchType && matchSearch ? "" : "none";
  });
}

function getEventTypeClass(type) {
  if (type.includes("STATS")) return "event-type-stats";
  if (type.includes("CONNECTION")) return "event-type-connection";
  if (type.includes("ERROR")) return "event-type-error";
  if (type.includes("ROUTING")) return "event-type-routing";
  if (type.includes("TEST")) return "event-type-test";
  return "event-type-stats";
}

function summarizeEvent(e) {
  const data = e.data || e.payload || "";
  if (typeof data === "string") {
    try {
      const parsed = JSON.parse(data);
      return summarizeJsonData(parsed);
    } catch {
      return data.substring(0, 200);
    }
  }
  if (typeof data === "object") {
    return summarizeJsonData(data);
  }
  return String(data).substring(0, 200);
}

function summarizeJsonData(obj) {
  const parts = [];
  if (obj.totalPacketsSent !== undefined)
    parts.push(`pkts↑${obj.totalPacketsSent}`);
  if (obj.totalPacketsReceived !== undefined)
    parts.push(`pkts↓${obj.totalPacketsReceived}`);
  if (obj.currentNeighborCount !== undefined)
    parts.push(`peers:${obj.currentNeighborCount}`);
  if (obj.avgRttMs !== undefined) parts.push(`rtt:${obj.avgRttMs}ms`);
  if (obj.batteryLevel !== undefined) parts.push(`bat:${obj.batteryLevel}%`);
  if (obj.message) parts.push(obj.message.substring(0, 100));
  if (obj.testName) parts.push(`${obj.testName}: ${obj.passed ? "✓" : "✗"}`);
  if (obj.peerName) parts.push(`peer:${obj.peerName}`);
  if (parts.length === 0) {
    // Generic summary of first few keys
    const keys = Object.keys(obj).slice(0, 5);
    return keys
      .map((k) => `${k}:${String(obj[k]).substring(0, 30)}`)
      .join(", ");
  }
  return parts.join(" | ");
}

// ============================================================
// Rendering — Relay Queue
// ============================================================

function renderRelayQueue() {
  const tbody = document.getElementById("relay-tbody");
  document.getElementById("relay-messages").textContent =
    allRelayMessages.length;

  if (allRelayMessages.length === 0) {
    tbody.innerHTML =
      '<tr><td colspan="5" class="empty-row">No relay messages in queue</td></tr>';
    return;
  }

  tbody.innerHTML = allRelayMessages
    .map((m) => {
      const age = formatDuration(Date.now() - (m.timestamp || 0));
      const payload = (m.payload || "").substring(0, 100);
      return `<tr>
            <td>${escHtml(m.sourceId || "?")}</td>
            <td>${escHtml(m.destId || "?")}</td>
            <td>${escHtml(m.gatewayId || "—")}</td>
            <td>${age}</td>
            <td style="color:var(--text-muted)">${escHtml(payload)}</td>
        </tr>`;
    })
    .join("");
}

// ============================================================
// Rendering — Test Results
// ============================================================

function renderTestResults() {
  const container = document.getElementById("test-results");
  const testEvents = allEvents.filter((e) =>
    (e.eventType || e.type || "").includes("TEST"),
  );

  if (testEvents.length === 0) {
    container.innerHTML =
      '<div class="empty-row">No test results available</div>';
    return;
  }

  // Group by device
  const byDevice = {};
  testEvents.forEach((e) => {
    const device = e.deviceId || e.deviceName || "Unknown";
    if (!byDevice[device]) byDevice[device] = [];
    try {
      const data =
        typeof e.data === "string" ? JSON.parse(e.data) : e.data || {};
      if (data.results && Array.isArray(data.results)) {
        byDevice[device].push(...data.results);
      } else if (data.testName) {
        byDevice[device].push(data);
      }
    } catch {
      // skip malformed
    }
  });

  let html = "";
  for (const [device, tests] of Object.entries(byDevice)) {
    html += `<div class="test-device-group">
            <div class="test-device-name">📱 ${escHtml(device)}</div>`;
    tests.forEach((t) => {
      const icon = t.passed ? "✅" : "❌";
      const dur = t.durationMs ? `${t.durationMs}ms` : "";
      html += `<div class="test-item">
                <span class="test-status">${icon}</span>
                <span class="test-name">${escHtml(t.testName || "?")}</span>
                <span class="test-duration">${dur}</span>
            </div>`;
    });
    html += "</div>";
  }

  container.innerHTML = html;
}

// ============================================================
// Summary Cards
// ============================================================

function updateSummaryCards() {
  const now = Date.now();
  const fifteenMin = 15 * 60 * 1000;

  document.getElementById("total-devices").textContent = allDevices.length;
  document.getElementById("active-devices").textContent = allDevices.filter(
    (d) => now - (d.lastSeen || d.timestamp || 0) < fifteenMin,
  ).length;
  document.getElementById("total-events").textContent = allEvents.length;

  // Sum total connections across all devices
  const totalConns = allDevices.reduce(
    (sum, d) => sum + (d.totalConnectionsEstablished || 0),
    0,
  );
  document.getElementById("total-connections").textContent = totalConns;
}

// ============================================================
// Charts
// ============================================================

function initCharts() {
  Chart.defaults.color = "#94a3b8";
  Chart.defaults.borderColor = "#334155";

  // Packets over time
  charts.packets = new Chart(document.getElementById("chart-packets"), {
    type: "line",
    data: {
      labels: [],
      datasets: [
        {
          label: "Sent",
          data: [],
          borderColor: "#3b82f6",
          backgroundColor: "rgba(59,130,246,0.1)",
          fill: true,
          tension: 0.3,
        },
        {
          label: "Received",
          data: [],
          borderColor: "#22c55e",
          backgroundColor: "rgba(34,197,94,0.1)",
          fill: true,
          tension: 0.3,
        },
        {
          label: "Forwarded",
          data: [],
          borderColor: "#a855f7",
          backgroundColor: "rgba(168,85,247,0.1)",
          fill: true,
          tension: 0.3,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { position: "bottom" } },
      scales: { y: { beginAtZero: true } },
    },
  });

  // Battery levels (bar)
  charts.battery = new Chart(document.getElementById("chart-battery"), {
    type: "bar",
    data: {
      labels: [],
      datasets: [
        {
          label: "Battery %",
          data: [],
          backgroundColor: [],
          borderRadius: 4,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true, max: 100 } },
    },
  });

  // RTT (bar histogram)
  charts.rtt = new Chart(document.getElementById("chart-rtt"), {
    type: "bar",
    data: {
      labels: [],
      datasets: [
        {
          label: "Avg RTT (ms)",
          data: [],
          backgroundColor: "#06b6d4",
          borderRadius: 4,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true } },
    },
  });
}

function updateCharts() {
  updatePacketChart();
  updateBatteryChart();
  updateRttChart();
  drawTopology();
}

function updatePacketChart() {
  // Extract stats snapshots sorted by time
  const stats = allEvents
    .filter((e) => (e.eventType || e.type || "").includes("STATS"))
    .sort((a, b) => (a.timestamp || 0) - (b.timestamp || 0))
    .slice(-50); // Last 50 snapshots

  const labels = stats.map((s) =>
    new Date(s.timestamp || 0).toLocaleTimeString(),
  );
  const sent = [];
  const recv = [];
  const fwd = [];

  stats.forEach((s) => {
    let d = s.data || s;
    if (typeof d === "string")
      try {
        d = JSON.parse(d);
      } catch {
        d = {};
      }
    sent.push(d.totalPacketsSent || 0);
    recv.push(d.totalPacketsReceived || 0);
    fwd.push(d.totalPacketsForwarded || 0);
  });

  charts.packets.data.labels = labels;
  charts.packets.data.datasets[0].data = sent;
  charts.packets.data.datasets[1].data = recv;
  charts.packets.data.datasets[2].data = fwd;
  charts.packets.update("none");
}

function updateBatteryChart() {
  const labels = [];
  const data = [];
  const colors = [];

  allDevices.forEach((d) => {
    const name = (d.deviceName || d.deviceId || "?").substring(0, 15);
    const bat = d.batteryLevel ?? d.battery ?? -1;
    if (bat >= 0) {
      labels.push(name);
      data.push(bat);
      colors.push(bat > 50 ? "#22c55e" : bat > 20 ? "#f97316" : "#ef4444");
    }
  });

  charts.battery.data.labels = labels;
  charts.battery.data.datasets[0].data = data;
  charts.battery.data.datasets[0].backgroundColor = colors;
  charts.battery.update("none");
}

function updateRttChart() {
  const labels = [];
  const data = [];

  allDevices.forEach((d) => {
    const name = (d.deviceName || d.deviceId || "?").substring(0, 15);
    const rtt = d.avgRttMs ?? -1;
    if (rtt >= 0) {
      labels.push(name);
      data.push(rtt);
    }
  });

  charts.rtt.data.labels = labels;
  charts.rtt.data.datasets[0].data = data;
  charts.rtt.update("none");
}

// ============================================================
// Topology Visualization (Canvas 2D)
// ============================================================

function drawTopology() {
  const canvas = document.getElementById("chart-topology");
  const ctx = canvas.getContext("2d");
  const W = (canvas.width = canvas.parentElement.clientWidth - 40);
  const H = (canvas.height = 280);

  ctx.clearRect(0, 0, W, H);

  if (allDevices.length === 0) {
    ctx.fillStyle = "#64748b";
    ctx.font = "14px sans-serif";
    ctx.textAlign = "center";
    ctx.fillText("No topology data", W / 2, H / 2);
    return;
  }

  // Place devices in a circle
  const cx = W / 2;
  const cy = H / 2;
  const radius = Math.min(W, H) * 0.35;
  const nodes = allDevices.map((d, i) => {
    const angle = (2 * Math.PI * i) / allDevices.length - Math.PI / 2;
    return {
      x: cx + radius * Math.cos(angle),
      y: cy + radius * Math.sin(angle),
      name: (d.deviceName || d.deviceId || `Device ${i}`).substring(0, 12),
      neighbors: d.neighbors || d.connectedEndpoints || [],
      isActive: Date.now() - (d.lastSeen || d.timestamp || 0) < 15 * 60 * 1000,
    };
  });

  // Draw edges
  ctx.strokeStyle = "rgba(59, 130, 246, 0.3)";
  ctx.lineWidth = 1.5;
  const nameToIndex = {};
  nodes.forEach((n, i) => {
    nameToIndex[n.name] = i;
  });

  nodes.forEach((node, i) => {
    if (Array.isArray(node.neighbors)) {
      node.neighbors.forEach((neighbor) => {
        const nName =
          typeof neighbor === "string" ? neighbor.substring(0, 12) : "";
        const j = nameToIndex[nName];
        if (j !== undefined && j > i) {
          ctx.beginPath();
          ctx.moveTo(node.x, node.y);
          ctx.lineTo(nodes[j].x, nodes[j].y);
          ctx.stroke();
        }
      });
    }
  });

  // Draw nodes
  nodes.forEach((node) => {
    // Circle
    ctx.beginPath();
    ctx.arc(node.x, node.y, 16, 0, 2 * Math.PI);
    ctx.fillStyle = node.isActive
      ? "rgba(34, 197, 94, 0.3)"
      : "rgba(100, 116, 139, 0.3)";
    ctx.fill();
    ctx.strokeStyle = node.isActive ? "#22c55e" : "#64748b";
    ctx.lineWidth = 2;
    ctx.stroke();

    // Icon
    ctx.fillStyle = node.isActive ? "#22c55e" : "#94a3b8";
    ctx.font = "14px sans-serif";
    ctx.textAlign = "center";
    ctx.fillText("📱", node.x, node.y + 5);

    // Label
    ctx.fillStyle = "#f1f5f9";
    ctx.font = "11px sans-serif";
    ctx.fillText(node.name, node.x, node.y + 30);
  });
}

// ============================================================
// Utility Functions
// ============================================================

function escHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

function escAttr(str) {
  return str.replace(/"/g, "&quot;").replace(/'/g, "&#39;");
}

function formatTimestamp(ms) {
  if (!ms || ms <= 0) return "—";
  const d = new Date(ms);
  const now = new Date();
  const diff = now - d;

  if (diff < 60000) return `${Math.floor(diff / 1000)}s ago`;
  if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;

  return d.toLocaleDateString() + " " + d.toLocaleTimeString();
}

function formatDuration(ms) {
  if (!ms || ms <= 0) return "—";
  const s = Math.floor(ms / 1000);
  if (s < 60) return `${s}s`;
  const m = Math.floor(s / 60);
  if (m < 60) return `${m}m ${s % 60}s`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ${m % 60}m`;
  const d = Math.floor(h / 24);
  return `${d}d ${h % 24}h`;
}

// ============================================================
// Device Logs (Cloud-Synced)
// ============================================================

async function refreshDeviceLogs() {
  try {
    allDeviceLogs = (await firestoreGet("device_logs")).sort(
      (a, b) => (b.batchTimestamp || 0) - (a.batchTimestamp || 0),
    );
    renderDeviceLogs();
  } catch (err) {
    console.error("Device logs refresh failed:", err);
  }
}

function renderDeviceLogs() {
  const container = document.getElementById("device-log-container");
  const deviceFilter = document.getElementById("device-log-device-filter");

  // Populate device filter dropdown
  const deviceIds = [
    ...new Set(allDeviceLogs.map((b) => b.deviceId).filter(Boolean)),
  ];
  const currentFilter = deviceFilter.value;
  deviceFilter.innerHTML = '<option value="">All Devices</option>';
  deviceIds.forEach((id) => {
    const opt = document.createElement("option");
    opt.value = id;
    opt.textContent = id;
    deviceFilter.appendChild(opt);
  });
  deviceFilter.value = currentFilter;

  filterDeviceLogs();
}

function filterDeviceLogs() {
  const container = document.getElementById("device-log-container");
  const deviceFilter = document.getElementById(
    "device-log-device-filter",
  ).value;
  const levelFilter = document.getElementById("device-log-level-filter").value;
  const searchText = document
    .getElementById("device-log-search")
    .value.toLowerCase();

  // Flatten all log batches into individual entries
  let entries = [];
  for (const batch of allDeviceLogs) {
    if (deviceFilter && batch.deviceId !== deviceFilter) continue;
    let logs;
    try {
      logs = typeof batch.logs === "string" ? JSON.parse(batch.logs) : [];
    } catch {
      continue;
    }
    for (const log of logs) {
      entries.push({
        device: batch.deviceId || "unknown",
        uploadedBy: batch.uploadedBy || batch.deviceId || "unknown",
        ts: log.ts || 0,
        lvl: log.lvl || "INFO",
        type: log.type || "SYSTEM",
        msg: log.msg || "",
        peer: log.peer || "",
        rssi: log.rssi,
        lat: log.lat,
      });
    }
  }

  // Apply level filter
  if (levelFilter) {
    entries = entries.filter((e) => e.lvl === levelFilter);
  }

  // Apply search filter
  if (searchText) {
    entries = entries.filter(
      (e) =>
        e.msg.toLowerCase().includes(searchText) ||
        e.device.toLowerCase().includes(searchText) ||
        e.peer.toLowerCase().includes(searchText),
    );
  }

  // Sort by timestamp descending
  entries.sort((a, b) => b.ts - a.ts);

  // Limit display
  const maxDisplay = 500;
  const displayed = entries.slice(0, maxDisplay);

  if (displayed.length === 0) {
    container.innerHTML = '<div class="empty-row">No logs match filters</div>';
    return;
  }

  const levelColors = {
    ERROR: "#ef5350",
    WARN: "#ffa726",
    METRIC: "#7e57c2",
    INFO: "#66bb6a",
    DEBUG: "#78909c",
    TRACE: "#90a4ae",
  };

  container.innerHTML = displayed
    .map((e) => {
      const time = new Date(e.ts).toLocaleTimeString();
      const color = levelColors[e.lvl] || "#ccc";
      const relayed = e.device !== e.uploadedBy ? " ☁" : "";
      const peerTag = e.peer ? ` [${escapeHtml(e.peer)}]` : "";
      return `<div class="event-entry">
        <span class="event-time">${time}</span>
        <span class="event-badge" style="background:${color}">${e.lvl}</span>
        <span class="event-device">${escapeHtml(e.device)}${relayed}</span>
        <span class="event-msg">${peerTag} ${escapeHtml(e.msg)}</span>
      </div>`;
    })
    .join("");

  if (entries.length > maxDisplay) {
    container.innerHTML += `<div class="empty-row">Showing ${maxDisplay} of ${entries.length} logs</div>`;
  }
}
