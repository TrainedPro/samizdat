# Handoff Review & To-Do List

## Current Project Status
The project is in the middle of a stability refactor for the [ResilientP2PTestbed](file:///home/hassaan/AndroidStudioProjects/ResilientP2PTestbed) app.
We have shifted from an "Automated Pulse" (Duty Cycle) strategy to an **"Always On" Mesh strategy** at the user's request.

## Recent Changes (Completed)
- **Self-Discovery Fix**: [P2PManager.kt](file:///home/hassaan/AndroidStudioProjects/ResilientP2PTestbed/app/src/main/java/com/fyp/resilientp2p/managers/P2PManager.kt) now ignores its own `endpointID`/`name` in [onEndpointFound](file:///home/hassaan/AndroidStudioProjects/ResilientP2PTestbed/samples/android-platform-samples-new/connectivity/UwbRanging/src/main/java/com/google/uwb/uwbranging/impl/NearbyConnections.kt#85-91) and [onConnectionInitiated](file:///home/hassaan/AndroidStudioProjects/ResilientP2PTestbed/app/src/main/java/com/fyp/resilientp2p/managers/P2PManager.kt#213-224).
- **UI State Fix**: Added `init` block in [P2PManager.kt](file:///home/hassaan/AndroidStudioProjects/ResilientP2PTestbed/app/src/main/java/com/fyp/resilientp2p/managers/P2PManager.kt) to correctly start mesh tasks if `isMeshMaintenanceActive` is true on app launch.
- **Always-On Implementation**:
    - Removed `MeshMaintenanceJob` (the "Pulse" loop).
    - Updated [startMeshMaintenance()](file:///home/hassaan/AndroidStudioProjects/ResilientP2PTestbed/app/src/main/java/com/fyp/resilientp2p/managers/P2PManager.kt#120-126) to start both **Advertising** and **Discovery** immediately.
    - Removed [stopRadioActivity()](file:///home/hassaan/AndroidStudioProjects/ResilientP2PTestbed/app/src/main/java/com/fyp/resilientp2p/managers/P2PManager.kt#131-136) from [onConnectionResult](file:///home/hassaan/AndroidStudioProjects/ResilientP2PTestbed/samples/android-connectivity-samples-archived/NearbyConnectionsWalkieTalkie/app/src/main/java/com/google/location/nearby/apps/walkietalkie/ConnectionsActivity.java#141-158) so radios remain active after connection.

## Immediate Next Steps (To-Do)
The code for "Always On" is written but **unverified**.

1.  **Build Verification**:
    - Run `./gradlew assembleDebug` to ensure [P2PManager.kt](file:///home/hassaan/AndroidStudioProjects/ResilientP2PTestbed/app/src/main/java/com/fyp/resilientp2p/managers/P2PManager.kt) changes compile cleanly.
2.  **Runtime Verification**:
    - Install on 2+ devices.
    - Enable "Start Mesh Network".
    - **Verify Radios**: Ensure both devices continue Advertising *and* Discovering even after they connect.
    - **Verify Stability**: Check if connections drop or if `VirtualOutputStream closed` errors appear in Logcat (a known previous issue).
3.  **Feature Implementation**:
    - **File Transfer**: Currently a stub in [P2PManager.kt](file:///home/hassaan/AndroidStudioProjects/ResilientP2PTestbed/app/src/main/java/com/fyp/resilientp2p/managers/P2PManager.kt). Needs implementation.
    - **Audio Streaming**: Currently a stub. Needs integration (likely using `AudioRecord`/`AudioTrack`).
    - **Gossip Routing**: [HeartbeatManager.kt](file:///home/hassaan/AndroidStudioProjects/ResilientP2PTestbed/app/src/main/java/com/fyp/resilientp2p/managers/HeartbeatManager.kt) has commented-out gossip logic. Needs to be revitalized for multi-hop routing if direct connections aren't sufficient.

## Known Issues / Context
- **VirtualOutputStream Closed**: We saw these errors previously. The "Always On" strategy might exacerbate this if the radios interrupt the localized connection streams. Watch for this.
- **Battery Usage**: "Always On" high-power usage is expected.

## Key Files
- [app/src/main/java/com/fyp/resilientp2p/managers/P2PManager.kt](file:///home/hassaan/AndroidStudioProjects/ResilientP2PTestbed/app/src/main/java/com/fyp/resilientp2p/managers/P2PManager.kt): Core mesh logic.
- [app/src/main/java/com/fyp/resilientp2p/managers/HeartbeatManager.kt](file:///home/hassaan/AndroidStudioProjects/ResilientP2PTestbed/app/src/main/java/com/fyp/resilientp2p/managers/HeartbeatManager.kt): Routing/Updates (currently partial).
