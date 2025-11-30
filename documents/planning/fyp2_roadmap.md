# FYP 2 Implementation Roadmap: The Resilient Network

This document outlines the implementation steps for FYP 2 (Horizons 2 & 3), building upon the local mesh foundation to create a resilient, censorship-resistant network.

## 1. Phase 3: The Gateway Bridge (Horizon 1 Extension)
**Goal:** Connect the isolated mesh to the global internet.

*   **Internet Detection Module:**
    *   Implement a service to continuously monitor connectivity (Wi-Fi/Cellular).
    *   **Logic:** If `Internet == Available` -> Advertise capability as `GATEWAY`.
*   **Bridging Protocol:**
    *   Design a REST API or WebSocket interface to a simple external server (The "Relay").
    *   **Uplink:** Mesh Nodes -> Gateway Node -> Internet Relay.
    *   **Downlink:** Internet Relay -> Gateway Node -> Mesh Nodes.

## 2. Phase 4: Store-and-Forward Engine (Resilience)
**Goal:** No message left behind. Handle network partitions gracefully.

*   **Persistent Database:**
    *   Schema: `MessageTable` (`ID`, `Payload`, `Dest`, `Status` [PENDING, SENT, DELIVERED], `Timestamp`).
*   **Background Service (WorkManager):**
    *   **Queue Management:** Periodically check the database for `PENDING` messages.
    *   **Retry Logic:** If a neighbor appears or internet becomes available, attempt delivery.
    *   **TTL Management:** Delete messages older than $X$ hours to save storage.

## 3. Phase 5: Intelligent Routing (Horizon 2)
**Goal:** Optimize routing using data collected in FYP 1.

*   **Metric-Based Routing:**
    *   Replace simple flooding with a cost-based algorithm.
    *   **Cost Function:** $Cost = f(RSSI, Battery, Hops)$.
    *   **Logic:** Prefer paths with higher RSSI and sufficient battery.
*   **On-Device ML (Experimental):**
    *   Train a lightweight TFLite model using the logs from FYP 1.
    *   **Input:** Current Time, Battery, RSSI history.
    *   **Output:** Probability of successful delivery via Neighbor $X$.

## 4. Phase 6: Censorship Resistance (Horizon 2)
**Goal:** Hardening the network against blocking.

*   **Self-Hosted Relays:**
    *   Release a Docker container for the server-side component.
    *   Allow users to input their own Relay URL in the app settings.
*   **Traffic Obfuscation (Research):**
    *   Investigate wrapping traffic in standard HTTPS or WebSocket frames to look like normal web browsing.
