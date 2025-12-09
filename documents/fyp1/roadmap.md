# FYP 1 Implementation Roadmap: Local Mesh & Calibration

This document outlines the implementation steps for FYP 1, prioritizing signal strength analysis, configurable performance metrics, and reliable packet delivery over user-facing features. It also lays the architectural foundation for long-term resilience.

## 1. Phase 0: Calibration & Instrumentation (The "Lab Bench")
**Goal:** Build the tools to measure the network's physical reality.

*   **Signal Strength Monitoring (RSSI):**
    *   Implement real-time monitoring of Received Signal Strength Indication (RSSI) for all connected peers.
    *   **Visual Indicator:** Display raw RSSI values (dBm) and a visual "quality" meter for each peer.
*   **Configurable "Heartbeat" Engine:**
    *   Implement a background ping mechanism.
    *   **User Controls:** Add a "Developer Settings" panel to configure:
        *   **Ping Interval:** (e.g., Slider: 100ms to 60s).
        *   **Discovery Interval:** How often to scan for new neighbors.
        *   **Payload Size:** (e.g., 64B, 512B, 1KB).
*   **Structured Logging (Database):**
    *   **Investment:** Instead of simple text files, use a database for logging metrics. This sets up the database infrastructure needed for the future Store-and-Forward engine.
    *   **Schema:** `LogEntry(id, timestamp, type, peerId, rssi, batteryLevel, payloadSize, latency)`.
    *   **Export:** Feature to export these logs to CSV for analysis.

## 2. Phase 1: Robust Transport Layer & Background Execution
**Goal:** Ensure the node stays alive and reliable, even when the screen is off.

*   **Android Foreground Service:**
    *   **Core Requirement:** Implement a persistent Foreground Service with a notification to keep the Nearby Connections API alive when the app is minimized or the screen is locked.
    *   **Permissions:** Handle `FOREGROUND_SERVICE` and battery optimization exemptions (if possible/necessary for the testbed).
*   **Battery-Aware Scheduling:**
    *   Use `WorkManager` for non-critical tasks (like log export or cleanup) to respect system resources.
    *   Monitor battery impact of the Foreground Service to establish a baseline.
*   **Reliability Protocol:**
    *   Implement an Application-Level ACK (Acknowledgement) system.
    *   **Retransmission Logic:** If ACK not received within $T$ ms, retry $N$ times.
*   **Packet Structure:**
    *   `Header`: `Sequence_Number`, `Timestamp`, `Type` (PING, ACK, DATA), `Source_ID`, `Dest_ID`.
    *   `Payload`: Variable padding for stress testing.

## 3. Phase 2: Local Mesh (The Network Layer)
**Goal:** Multi-hop delivery with a focus on metric collection.

*   **Neighbor Table 2.0:**
    *   Store not just the Peer ID, but their current **Link Quality (RSSI)** and **Last Seen** timestamp.
*   **Flooding with Telemetry:**
    *   Implement basic Epidemic Routing.
    *   **Trace Header:** Append each hop's ID and RSSI to the packet header as it travels. This allows us to reconstruct the full path and link quality of every hop during analysis.
*   **De-duplication:**
    *   Strict `Message_ID` caching to prevent broadcast storms.

## 4. Verification & Testing
*   **Background Survival Test:** Lock the phone and walk away; verify that pings continue to be sent/received and logged.
*   **Range Test:** Walk away until RSSI drops below threshold; verify packet loss correlates with RSSI.
*   **Saturation Test:** Set Ping Interval to minimum (e.g., 100ms) and observe latency spikes and queue backups.
*   **Battery Test:** Run the "Heartbeat" for 1 hour at different intervals and measure battery drain using `batterystats`.
