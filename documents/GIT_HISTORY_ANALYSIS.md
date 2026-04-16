# Git History Analysis Report — ResilientP2PTestbed

**Date:** 2026-04-16  
**Analyst:** Automated Git History Analysis  
**Repository:** `/home/hassaan/AndroidStudioProjects/ResilientP2PTestbed`

---

## 1. Executive Summary

The project underwent a **major simplification** for FYP1 presentation purposes, removing significant features including UWB support, security layer, Phase 4 features (trilateration, chat groups, DTN, file sharing, health dashboard), and test infrastructure. The full-featured code still exists in branches and can be restored by merging or cherry-picking from those branches.

**Key Finding:** The simplification occurred between commits `16a70de` (pre-simplification snapshot) and `6372d04` (fyp1-simplification HEAD). The `main` branch has since merged both `feature/security` and `feature/stage-3` back in, meaning the **full-featured code is already on `main`**.

---

## 2. Branch Structure

```
* fyp2-midterm (HEAD) — current working branch with FYP2 features
* main — has merged both feature branches (FULL-FEATURED)
* feature/stage-3 — Phase 4 features (trilateration, chat groups, DTN, file sharing, health dashboard)
* feature/security — Security layer (ECDH, AES-256-GCM, HMAC, rate limiting, peer blacklist)
* fyp1-simplification — simplified version for FYP1 presentation
* origin/aais — remote branch (unknown content)
* origin/fyp2-midterm — remote tracking branch
* origin/main — remote tracking branch
```

**Stashes:** None found (`git stash list` returned empty)

---

## 3. Full Commit History (47 commits, oldest to newest)

### Initial Setup Phase
| Commit | Message | Description |
|--------|---------|-------------|
| `776426b` | Initial Commit | Project bootstrap |
| `55cf1c3` | Initial project setup and P2P functionality | Core P2P networking |
| `880f92d` | Phase 0 Complete: UI Polish, Bug Fixes, and Feature Completion | UI refinements |
| `6d88f84` | Add detailed directory summaries for nearby-share documentation | Documentation |

### Major Refactoring Phase
| Commit | Message | Description |
|--------|---------|-------------|
| `350681b` | Refactor: Major resilience and feature enhancements | Core resilience improvements |
| `c4c4013` | refactor: Migrate to Jetpack Compose UI and enhance core logic | UI migration to Compose |
| `c86624b` | Refactor: Enhance PING logic and add graceful shutdown | Network protocol improvements |
| `e8ef498` | feat: Implement mesh routing with gossip protocol | Mesh routing protocol |

### Pre-Simplification Snapshot
| Commit | Message | Description |
|--------|---------|-------------|
| `079dcd5` | feat: Add automated class diagram generation script and updated diagram PDF | Documentation tooling |
| **`16a70de`** | **Snapshot of current state before FYP1 simplification** | **CRITICAL: Full-featured state preserved here** |

### Simplification Phase
| Commit | Message | Description |
|--------|---------|-------------|
| **`6372d04`** | **audit: final security hardening, architecture cleanup, and script improvements** | **CRITICAL: Simplification commit — removed features** |

### FYP1 Simplification Branch (HEAD)
| Commit | Message | Description |
|--------|---------|-------------|
| `589b251` | chore: restructure non-code assets (fyp1/fyp2) and update gitignore | Documentation restructure |
| `fed7177` | Feat: Standardize logging, add sender ID, and implement dynamic routing | Logging improvements |
| `d4a924f` | Final Polish: Cleanup extraneous comments and logs | Code cleanup |
| `774f3a6` | Docs: Update documentation and remove handoff scripts | Documentation |
| `1cc9c9f` | Docs: Extensive documentation update and script restoration | Documentation |
| `c9912de` | Docs: Update documentation to 'Technical Bible' standard | Documentation |
| `80f00d0` | Major Update (Jan 2026): Comprehensive Security, Stability, and Feature Polish | Security/stability |
| `ba7f834` | Fix: Suppress false positive AudioRecorder write error on stop | Bug fix |
| `e9f9f4b` | Config: Reduce audio sample rate to 16kHz to fix stutter | Audio config fix |

### Feature Addition Phase (on feature branches and main)
| Commit | Message | Description |
|--------|---------|-------------|
| `5e73a08` | feat: comprehensive automated test mode with 15 tests | Test infrastructure |
| `58a8232` | feat: full chat screen with message persistence, file transfer UI, image thumbnails | Chat UI |
| `7ecf698` | feat: cloud telemetry system with WorkManager periodic upload | Cloud telemetry |
| `fb9c8ee` | Comprehensive codebase audit: fix 50+ issues across all layers | Code quality |
| `21e365e` | feat: wire Firebase Firestore keys via BuildConfig (from local.properties) | Firebase integration |
| `cfebf74` | feat: Phase 3 — Internet Gateway, Emergency Broadcast, Audio Codec, KDoc, Design Decisions | Phase 3 features |
| `e1b4ec0` | feat: web dashboard for mesh network monitoring | Web dashboard |
| **`e270155`** | **feat: security layer — ECDH key exchange, AES-256-GCM encryption, HMAC integrity, rate limiting, peer blacklist** | **Security features** |
| **`5e43edf`** | **feat(phase-4): trilateration, chat groups, DTN encounters, file sharing, health dashboard** | **Phase 4 features** |

### Main Branch Merges and FYP2 Work
| Commit | Message | Description |
|--------|---------|-------------|
| `0012091` | merge: feature/security — ECDH, AES-256-GCM, HMAC, rate limiting, peer blacklist | Security merge |
| `ac3f16c` | merge: feature/stage-3 — Phase 4 (trilateration, chat groups, DTN, file sharing, health dashboard) | Phase 4 merge |
| `65b2278` | fix: resolve all lint errors and actionable warnings | Lint cleanup |
| `4ee3813` | fix: remove lazy @Suppress annotations, use proper modern APIs | API modernization |
| `5294066` | fix: comprehensive audit — fix race conditions, resource leaks, UI and logic bugs | Bug fixes |
| `b4b6a74` | Endurance testing infra: mAh tracking, advanced metrics, cloud upload, CSV/JSON export | Endurance testing |
| `5e267a4` | feat: add Detekt strict static analysis + 12-item security hardening | Static analysis |
| `9fa6e99` | chore: strategic Detekt cleanup — 450 → 0 issues | Detekt cleanup |
| `036dbd8` | fix: 3 test failures, back navigation, gateway toggle, emoji→icons | Bug fixes |
| `592731e` | fix: camera opens camera, hide idle transfer, msg notifications, filter endurance | Latest fixes on main |

### FYP2 Midterm Branch (Current HEAD)
| Commit | Message | Description |
|--------|---------|-------------|
| (10 commits ahead of main) | Cloud log sync, Firebase tools, HMAC fixes, P2PComposables overhaul, etc. | FYP2 feature work |

---

## 4. Simplification Analysis — What Was Removed

### 4.1 The Simplification Commit (`6372d04`)

**Diff from `16a70de` to `6372d04`:**

| File | Change | Impact |
|------|--------|--------|
| `P2PManager.kt` | **-375 lines** | Core P2P networking significantly reduced |
| `UwbManager.kt` | **-195 lines (DELETED)** | Ultra-Wideband support completely removed |
| `P2PComposables.kt` | **-343 lines** | UI components significantly reduced |
| `MainActivity.kt` | -56 lines | Main activity simplified |
| `AudioRecorder.kt` | -30 lines | Audio recording simplified |
| `VoiceManager.kt` | -65 lines | Voice management reduced |
| `ExampleInstrumentedTest.kt` | **-24 lines (DELETED)** | Instrumented tests removed |
| `ExampleUnitTest.kt` | **-17 lines (DELETED)** | Unit tests removed |

**What was ADDED during simplification:**
- 40+ documentation files (LaTeX presentations, reports, diagrams)
- FYP1 presentation materials
- Technical documentation

### 4.2 Features That Existed Before Simplification

Based on commit `16a70de` (pre-simplification snapshot) and subsequent feature branches:

#### Security Features (from `feature/security` / commit `e270155`)
- **ECDH P-256 Key Exchange** — `SecurityManager.kt` (273 lines)
- **AES-256-GCM Encryption** — encrypt/decrypt for all P2P messages
- **HMAC-SHA256 Integrity** — message authentication codes
- **HKDF Key Derivation** — secure key derivation
- **RateLimiter** — per-peer sliding window (100 pkts/sec) — `RateLimiter.kt` (91 lines)
- **PeerBlacklist** — SharedPreferences-persistent blacklist, auto-ban after 10 violations — `PeerBlacklist.kt` (121 lines)
- **PeerTrustScorer** — peer trust scoring system

#### Phase 4 Features (from `feature/stage-3` / commit `5e43edf`)
- **LocationEstimator** — RTT-based 2D trilateration with EMA smoothing (α=0.3), least-squares linearised solver — `LocationEstimator.kt` (206 lines)
- **RadarView** — canvas composable for location visualization — `RadarView.kt` (218 lines)
- **Chat Groups** — Room entities (`ChatGroup`, `GroupMessage`) with broadcast GROUP_MESSAGE packets, dedup via unique packetId index — `ChatGroup.kt`, `GroupMessage.kt`, `ChatGroupDao.kt`, `GroupMessageDao.kt`
- **EncounterLogger** — DTN encounter lifecycle tracking, 2h/7-day TTL modes, 10-min flush cooldown — `EncounterLogger.kt` (141 lines), `EncounterLog.kt`, `EncounterDao.kt`
- **FileShareManager** — SHA-256 content-addressed chunked file sharing (32 KB), 3-packet announce/request/chunk protocol — `FileShareManager.kt` (307 lines), `SharedFile.kt`, `SharedFileDao.kt`
- **HealthDashboard** — topology graph, 8 stat cards, per-peer stats table, packet-loss heatmap with green→yellow→red interpolation — `HealthDashboard.kt` (399 lines)
- **GroupChatScreen** — full group chat UI — `GroupChatScreen.kt` (366 lines)
- **6 new PacketTypes** — extended packet protocol
- **4 new Room entities** — database schema expansion (DB version 7→8)

#### UWB Support (deleted in simplification)
- **UwbManager.kt** — Ultra-Wideband ranging and positioning (195 lines, completely removed)

#### Test Infrastructure (deleted in simplification)
- **ExampleInstrumentedTest.kt** — Android instrumented tests
- **ExampleUnitTest.kt** — JUnit unit tests

#### Phase 3 Features (added after simplification, on main)
- **Internet Gateway** — `InternetGatewayManager.kt`
- **Emergency Broadcast** — `EmergencyManager.kt`
- **Audio Codec** — `AudioCodecManager.kt` with Opus support
- **Cloud Telemetry** — `TelemetryManager.kt`, `TelemetryUploadWorker.kt`
- **Web Dashboard** — `dashboard/` directory with HTML/JS
- **Firebase Integration** — `CloudLogManager.kt` (431 lines)
- **Automated Test Mode** — `TestRunner.kt`, `EnduranceTestRunner.kt`, `TestModels.kt`
- **Full Chat Screen** — `ChatScreen.kt` with message persistence, file transfer UI, image thumbnails

---

## 5. Branch Comparison

### Current State by Branch

| Feature | `main` | `fyp2-midterm` (HEAD) | `fyp1-simplification` | `feature/security` | `feature/stage-3` |
|---------|--------|----------------------|----------------------|-------------------|-------------------|
| ECDH/AES-256-GCM/HMAC | ✅ | ✅ | ❌ | ✅ | ❌ |
| Rate Limiter | ✅ | ✅ | ❌ | ✅ | ❌ |
| Peer Blacklist | ✅ | ✅ | ❌ | ✅ | ❌ |
| Trilateration | ✅ | ✅ | ❌ | ❌ | ✅ |
| Chat Groups | ✅ | ✅ | ❌ | ❌ | ✅ |
| DTN Encounters | ✅ | ✅ | ❌ | ❌ | ✅ |
| File Sharing | ✅ | ✅ | ❌ | ❌ | ✅ |
| Health Dashboard | ✅ | ✅ | ❌ | ❌ | ✅ |
| UWB Support | ❌ | ❌ | ❌ | ❌ | ❌ |
| Cloud Telemetry | ✅ | ✅ | ❌ | ❌ | ❌ |
| Firebase Integration | ✅ | ✅ | ❌ | ❌ | ❌ |
| Web Dashboard | ✅ | ✅ | ❌ | ❌ | ❌ |
| Test Infrastructure | ✅ | ✅ | ❌ | ❌ | ❌ |
| Emergency Broadcast | ✅ | ✅ | ❌ | ❌ | ❌ |
| Internet Gateway | ✅ | ✅ | ❌ | ❌ | ❌ |
| Audio Codec (Opus) | ✅ | ✅ | ❌ | ❌ | ❌ |
| Endurance Testing | ✅ | ✅ | ❌ | ❌ | ❌ |
| Detekt Analysis | ✅ | ✅ | ❌ | ❌ | ❌ |

**Key Insight:** The `main` branch has ALL features merged. The `fyp2-midterm` branch (current HEAD) has all features from `main` plus additional FYP2 work.

---

## 6. Restoration Strategy

### Option A: Switch to `main` Branch (Recommended)
The `main` branch already has both `feature/security` and `feature/stage-3` merged:
```bash
git checkout main
```
This gives you the full-featured codebase with all security and Phase 4 features.

### Option B: Merge `main` into current branch
If you want to keep FYP2-midterm work but get full features:
```bash
git checkout fyp2-midterm
git merge main
```

### Option C: Cherry-pick specific feature commits
If you only want specific features:
```bash
# Security features
git cherry-pick e270155

# Phase 4 features
git cherry-pick 5e43edf
```

### Option D: Restore from pre-simplification snapshot
To get the exact state before simplification:
```bash
git checkout 16a70de
```
Note: This will lose all work done after the snapshot.

---

## 7. Files That Need Attention for Full Restoration

### Files That Were Reduced/Deleted During Simplification
1. **`app/src/main/java/com/fyp/resilientp2p/managers/P2PManager.kt`** — needs +375 lines restored
2. **`app/src/main/java/com/fyp/resilientp2p/managers/UwbManager.kt`** — needs full restoration (195 lines, file deleted)
3. **`app/src/main/java/com/fyp/resilientp2p/ui/P2PComposables.kt`** — needs +343 lines restored
4. **`app/src/main/java/com/fyp/resilientp2p/MainActivity.kt`** — needs +56 lines restored
5. **`app/src/main/java/com/fyp/resilientp2p/audio/AudioRecorder.kt`** — needs +30 lines restored
6. **`app/src/main/java/com/fyp/resilientp2p/audio/VoiceManager.kt`** — needs +65 lines restored
7. **`app/src/androidTest/java/.../ExampleInstrumentedTest.kt`** — needs restoration
8. **`app/src/test/java/.../ExampleUnitTest.kt`** — needs restoration

### Files That Were Added After Simplification (on main/fyp2-midterm)
These files exist on `main` and `fyp2-midterm` but NOT on `fyp1-simplification`:
- `app/src/main/java/com/fyp/resilientp2p/security/SecurityManager.kt`
- `app/src/main/java/com/fyp/resilientp2p/security/RateLimiter.kt`
- `app/src/main/java/com/fyp/resilientp2p/security/PeerBlacklist.kt`
- `app/src/main/java/com/fyp/resilientp2p/security/PeerTrustScorer.kt`
- `app/src/main/java/com/fyp/resilientp2p/managers/LocationEstimator.kt`
- `app/src/main/java/com/fyp/resilientp2p/managers/EncounterLogger.kt`
- `app/src/main/java/com/fyp/resilientp2p/managers/FileShareManager.kt`
- `app/src/main/java/com/fyp/resilientp2p/managers/EmergencyManager.kt`
- `app/src/main/java/com/fyp/resilientp2p/managers/InternetGatewayManager.kt`
- `app/src/main/java/com/fyp/resilientp2p/managers/HeartbeatManager.kt`
- `app/src/main/java/com/fyp/resilientp2p/managers/CloudLogManager.kt`
- `app/src/main/java/com/fyp/resilientp2p/managers/TelemetryManager.kt`
- `app/src/main/java/com/fyp/resilientp2p/managers/TelemetryUploadWorker.kt`
- `app/src/main/java/com/fyp/resilientp2p/audio/MeshAudioManager.kt`
- `app/src/main/java/com/fyp/resilientp2p/audio/AudioCodecManager.kt`
- `app/src/main/java/com/fyp/resilientp2p/ui/HealthDashboard.kt`
- `app/src/main/java/com/fyp/resilientp2p/ui/GroupChatScreen.kt`
- `app/src/main/java/com/fyp/resilientp2p/ui/RadarView.kt`
- `app/src/main/java/com/fyp/resilientp2p/ui/TestModeScreen.kt`
- `app/src/main/java/com/fyp/resilientp2p/testing/TestRunner.kt`
- `app/src/main/java/com/fyp/resilientp2p/testing/EnduranceTestRunner.kt`
- `app/src/main/java/com/fyp/resilientp2p/testing/TestModels.kt`
- `app/src/main/java/com/fyp/resilientp2p/service/P2PService.kt`
- `app/src/main/java/com/fyp/resilientp2p/data/ChatGroup.kt`
- `app/src/main/java/com/fyp/resilientp2p/data/GroupMessage.kt`
- `app/src/main/java/com/fyp/resilientp2p/data/EncounterLog.kt`
- `app/src/main/java/com/fyp/resilientp2p/data/SharedFile.kt`
- `app/src/main/java/com/fyp/resilientp2p/data/TelemetryEvent.kt`
- `app/src/main/java/com/fyp/resilientp2p/data/EmergencyMessage.kt`
- `app/src/main/java/com/fyp/resilientp2p/data/DownloadedChunk.kt`
- `app/src/main/java/com/fyp/resilientp2p/data/NetworkStats.kt`
- `app/src/main/java/com/fyp/resilientp2p/data/LogDao.kt`
- `app/src/main/java/com/fyp/resilientp2p/data/ChatGroupDao.kt`
- `app/src/main/java/com/fyp/resilientp2p/data/GroupMessageDao.kt`
- `app/src/main/java/com/fyp/resilientp2p/data/SharedFileDao.kt`
- `app/src/main/java/com/fyp/resilientp2p/data/EncounterDao.kt`
- `app/src/main/java/com/fyp/resilientp2p/data/TelemetryDao.kt`
- `app/src/test/java/com/fyp/resilientp2p/AudioCodecTest.kt`
- `app/src/test/java/com/fyp/resilientp2p/FileShareTest.kt`
- `app/src/test/java/com/fyp/resilientp2p/MeshAudioTest.kt`
- `app/src/test/java/com/fyp/resilientp2p/MessageCacheTest.kt`
- `app/src/test/java/com/fyp/resilientp2p/PacketTest.kt`
- `app/src/test/java/com/fyp/resilientp2p/RoutingTest.kt`
- `dashboard/index.html`
- `dashboard/dashboard.js`
- `dashboard/style.css`
- `dashboard/README.md`
- `tools/firebase_query.py`
- `tools/README.md`
- `pull_logs.sh`
- `detekt.yml`

---

## 8. Summary Statistics

| Metric | Value |
|--------|-------|
| Total commits | 47 |
| Branches | 5 local + 3 remote |
| Stashes | 0 |
| Lines removed during simplification | ~1,000+ |
| Files deleted during simplification | 3+ |
| Documentation files added during simplification | 40+ |
| Features in full version | 15+ |
| Features in simplified version | ~5 |
| Code restoration complexity | LOW (merge `main` into current branch) |

---

## 9. Recommendations

1. **IMMEDIATE:** Switch to `main` branch or merge `main` into `fyp2-midterm` — this restores ALL features in one operation
2. **VERIFY:** After merge, run `./gradlew build` to ensure compilation succeeds
3. **TEST:** Run the automated test mode to verify all features work
4. **DOCUMENT:** Update `DESIGN_DECISIONS.md` with any new design decisions from FYP2 work
5. **UWB NOTE:** `UwbManager.kt` was deleted during simplification and is NOT on any current branch. If UWB support is needed, it must be restored from commit `16a70de`:
   ```bash
   git show 16a70de:app/src/main/java/com/fyp/resilientp2p/managers/UwbManager.kt > app/src/main/java/com/fyp/resilientp2p/managers/UwbManager.kt
   ```

---

*Report generated from git history analysis on 2026-04-16*
