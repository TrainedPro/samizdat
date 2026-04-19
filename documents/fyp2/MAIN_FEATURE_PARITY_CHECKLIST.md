# Main Feature Parity Checklist

Source of truth: main branch implementation and current documentation.

## Legend
- IMPLEMENTED_VERIFIED: implemented with direct code evidence and at least baseline tests/evidence.
- IMPLEMENTED_NEEDS_ACCEPTANCE: implemented in code, but acceptance proof is weak/incomplete.
- PARTIAL: foundational pieces exist but full behavior needs closure.
- PLANNED: roadmap item not expected in current baseline.

## Core Networking
| Item | Status | Evidence | Action |
|---|---|---|---|
| Mesh discovery/connection lifecycle | IMPLEMENTED_VERIFIED | app/src/main/java/com/fyp/resilientp2p/managers/P2PManager.kt | Keep regression checks in TestRunner |
| Heartbeat/liveness + RTT tracking | IMPLEMENTED_VERIFIED | app/src/main/java/com/fyp/resilientp2p/managers/HeartbeatManager.kt | Add failure-injection scenario |
| Store-and-forward queueing | IMPLEMENTED_NEEDS_ACCEPTANCE | app/src/main/java/com/fyp/resilientp2p/managers/P2PManager.kt | Add reconnect/expiry acceptance test |

## Routing
| Item | Status | Evidence | Action |
|---|---|---|---|
| Multi-hop route announcements + forwarding | IMPLEMENTED_VERIFIED | app/src/main/java/com/fyp/resilientp2p/managers/P2PManager.kt | Add 3-device deterministic path test |
| Route pruning and dedup cache | IMPLEMENTED_VERIFIED | app/src/main/java/com/fyp/resilientp2p/transport/MessageCache.kt | Keep as is |

## Gateway and Cloud
| Item | Status | Evidence | Action |
|---|---|---|---|
| Internet gateway detection/flag advertise | IMPLEMENTED_VERIFIED | app/src/main/java/com/fyp/resilientp2p/managers/InternetGatewayManager.kt | Keep as is |
| Firestore relay send/poll/inject | IMPLEMENTED_NEEDS_ACCEPTANCE | app/src/main/java/com/fyp/resilientp2p/managers/InternetGatewayManager.kt | Add scripted integration scenario |
| Telemetry upload worker | IMPLEMENTED_NEEDS_ACCEPTANCE | app/src/main/java/com/fyp/resilientp2p/managers/TelemetryUploadWorker.kt | Add retry/failure-path acceptance tests |
| Cloud log sync | IMPLEMENTED_NEEDS_ACCEPTANCE | app/src/main/java/com/fyp/resilientp2p/managers/CloudLogManager.kt | Add relay/upload acceptance script |

## Security
| Item | Status | Evidence | Action |
|---|---|---|---|
| ECDH + AES-GCM + HMAC | IMPLEMENTED_VERIFIED | app/src/main/java/com/fyp/resilientp2p/security/SecurityManager.kt | Add negative-path test matrix |
| Rate limiter + blacklist + trust scorer | IMPLEMENTED_VERIFIED | app/src/main/java/com/fyp/resilientp2p/security/*.kt | Keep as is |

## Messaging, Media, and Files
| Item | Status | Evidence | Action |
|---|---|---|---|
| Direct chat persistence | IMPLEMENTED_VERIFIED | app/src/main/java/com/fyp/resilientp2p/data/ChatMessage.kt | Keep as is |
| Group chat data + packet handling | IMPLEMENTED_NEEDS_ACCEPTANCE | app/src/main/java/com/fyp/resilientp2p/ui/GroupChatScreen.kt, app/src/main/java/com/fyp/resilientp2p/P2PApplication.kt | Add multi-device group acceptance run |
| Push-to-talk / mesh audio | IMPLEMENTED_NEEDS_ACCEPTANCE | app/src/main/java/com/fyp/resilientp2p/audio/*.kt | Add jitter/loss acceptance profile |
| File share protocol and chunking | IMPLEMENTED_VERIFIED | app/src/main/java/com/fyp/resilientp2p/managers/FileShareManager.kt | Add large-file resume acceptance test |

## Phase 4 Research Features
| Item | Status | Evidence | Action |
|---|---|---|---|
| RTT trilateration engine | IMPLEMENTED_NEEDS_ACCEPTANCE | app/src/main/java/com/fyp/resilientp2p/managers/LocationEstimator.kt | Added unit tests; now add runtime acceptance proof |
| Radar/health dashboard UI | IMPLEMENTED_NEEDS_ACCEPTANCE | app/src/main/java/com/fyp/resilientp2p/ui/RadarView.kt, app/src/main/java/com/fyp/resilientp2p/ui/HealthDashboard.kt | Validate with 3+ anchors scenario |
| DTN encounter logging | IMPLEMENTED_VERIFIED | app/src/main/java/com/fyp/resilientp2p/managers/EncounterLogger.kt | Keep as is |

## Lifecycle and Reliability
| Item | Status | Evidence | Action |
|---|---|---|---|
| Ordered manager teardown in service | IMPLEMENTED_VERIFIED | app/src/main/java/com/fyp/resilientp2p/service/P2PService.kt | Updated to include cloud log manager cleanup |
| stopAll cleanup consistency | IMPLEMENTED_VERIFIED | app/src/main/java/com/fyp/resilientp2p/managers/P2PManager.kt | Updated to stop voice I/O and reset location estimator |

## Planned (not baseline blockers)
| Item | Status | Evidence | Action |
|---|---|---|---|
| UWB ranging | PLANNED | documents/fyp2/feature_roadmap.md | Track separately |
| CI/CD pipeline | PLANNED | documents/fyp2/FYP2_MASTER_PLAN.md | Track separately |
| Non-destructive Room migrations | PLANNED | app/src/main/java/com/fyp/resilientp2p/data/AppDatabase.kt | Schedule as tech debt |

## Immediate implementation queue
1. Gateway relay and group messaging acceptance checks: IMPLEMENTED in TestRunner (control plane + GROUP_MESSAGE persistence path).
2. Store-and-forward acceptance checks: IMPLEMENTED baseline queue/delivery-window assertions in TestRunner (reconnect+expiry long-window still requires multi-device endurance run).
3. Large-file transfer acceptance checks: IMPLEMENTED dual-size transfer initiation checks (small + 1 MiB) in TestRunner.
4. Capture reproducible evidence pack tied to main commit IDs: PENDING runtime execution on physical device mesh.
