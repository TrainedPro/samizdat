# Troubleshooting: Peer Discovery Issue

## Issue Report

**Symptom**: "No peers found yet. Waiting for mesh..." message showing even when devices should be connected  
**Reported By**: User  
**Last Working Commit**: Unknown (possibly before QoL commit)  
**Current Commit**: `2bb1fc7` - Implement quality of life features

---

## Analysis: What Changed in Last Commit

### Files Modified

1. ✅ `P2PManager.kt` - **NO ROUTING LOGIC CHANGED**
   - Only added `PongReceivedEvent` data class (lines 134-137)
   - Only added event emission in PONG handler (lines 1770-1772)
   - **Zero changes to discovery, advertising, connection, or routing logic**

2. ✅ `P2PComposables.kt` - **NO PEER LIST LOGIC CHANGED**
   - Changed title: "Mesh Contacts" → "Connected Peers" (cosmetic)
   - Changed labels: "Via Internet Relay" → "☁️ Cloud Relay (Internet)" (cosmetic)
   - Added PONG notification (new feature, doesn't affect display)
   - **Zero changes to how peers are fetched or displayed**

3. ✅ `ChatScreen.kt` - **UNRELATED TO PEER DISCOVERY**
   - Only added `imePadding()` for keyboard handling

4. ✅ `AndroidManifest.xml` - **UNRELATED TO PEER DISCOVERY**
   - Only added `windowSoftInputMode="adjustResize"` for keyboard

### Conclusion

**NO CODE CHANGES THAT COULD AFFECT PEER DISCOVERY OR DISPLAY**

---

## Possible Causes

### 1. Version Mismatch (MOST LIKELY)

**User mentioned**: "The other device isn't on the latest version of the app though"

**Why this matters**:

- If one device has the new code and another has old code, they might not see each other
- However, the changes were purely UI/notification - shouldn't affect protocol
- **BUT**: If the old device is from BEFORE the cloud relay fix commit, there could be issues

**Solution**: Update both devices to the same version

### 2. No Active Devices

**User mentioned**: "It could be a testing issue from my side bcz no devices may be active but im pretty sure that isn't the case"

**Check**:

- Are both devices running the app in foreground?
- Are both devices on the same WiFi network?
- Are both devices within Bluetooth range?
- Have both devices granted all permissions?

### 3. Permissions Issue

After app update, Android might require re-granting permissions:

- Location (required for Nearby Connections)
- Bluetooth
- WiFi
- Nearby Devices (Android 12+)

**Solution**:

- Uninstall app completely
- Reinstall
- Grant all permissions when prompted

### 4. Cache/Data Issue

Old app data might be interfering with new version

**Solution**:

- Clear app data: Settings → Apps → Resilient P2P → Storage → Clear Data
- Or uninstall/reinstall

### 5. Firebase/Internet Gateway Issue

If relying on internet gateway for discovery:

- Check if internet is available
- Check if Firebase is accessible
- Check if gateway is enabled on at least one device

### 6. Mesh Not Started

**Check**: Is the "MESH ON" button pressed?

- The app requires manually starting the mesh
- After app update, mesh might not auto-start

---

## Diagnostic Steps

### Step 1: Verify Mesh is Running

1. Open app on both devices
2. Press "MESH ON" button (should turn red and say "MESH OFF")
3. Check if "Advertising" and "Discovering" indicators appear below button

**Expected**: Both should show "Advertising" and "Discovering"

### Step 2: Check Permissions

1. Go to Android Settings → Apps → Resilient P2P → Permissions
2. Verify ALL permissions are granted:
   - ✅ Location (Allow all the time or While using app)
   - ✅ Nearby devices
   - ✅ Bluetooth (if separate permission)

### Step 3: Check Logs

1. In app, scroll down to "Logs" section
2. Look for these log entries:
   - `ADVERTISING_STARTED` - Confirms advertising is active
   - `DISCOVERY_STARTED` - Confirms discovery is active
   - `ENDPOINT_FOUND` - Confirms other device was discovered
   - `CONNECTION_INITIATED` - Confirms connection attempt
   - `CONNECTION_ACCEPTED` - Confirms connection established
   - `IDENTITY_RECEIVED` - Confirms peer identity exchange

**If missing any of these**, that's where the problem is.

### Step 4: Check Network Stats

1. Scroll to "Network Statistics" section
2. Check:
   - "Connections": Should be > 0 if peers are connected
   - "Packets Sent/Received": Should be increasing if mesh is active

### Step 5: Force Reconnection

1. Press "MESH OFF" on both devices
2. Wait 5 seconds
3. Press "MESH ON" on both devices
4. Wait 30 seconds for discovery

### Step 6: Check Device Names

1. Look at top of screen for "Device: [name]"
2. Make sure both devices have different names
3. If names are identical, that could cause self-connection issues

---

## Code Verification

### Peer List Display Logic (Unchanged)

```kotlin
// Line 1042: P2PComposables.kt
if (knownPeers.isEmpty() && connectedEndpoints.isEmpty() && internetPeers.isEmpty()) {
    Text("No peers found yet. Waiting for mesh...")
} else {
    // Display peers
    val allPeers = (connectedEndpoints + knownPeers.keys + internetPeers).toSet()
    allPeers.forEach { peerId -> /* render peer */ }
}
```

**This logic is UNCHANGED from previous version.**

### State Variables (Unchanged)

```kotlin
// Line 626-628: P2PComposables.kt
knownPeers = state.knownPeers,
connectedEndpoints = state.connectedEndpoints.toSet(),
internetPeers = state.internetPeers,
```

**These are pulled directly from P2PState, UNCHANGED.**

### P2PState Definition (Check if changed in previous commits)

The issue might be from the PREVIOUS commit (cloud relay fix), not this one.

---

## Recommended Actions

### Immediate Actions

1. **Update both devices to same version** (most important)
2. **Clear app data on both devices**
3. **Reinstall app on both devices**
4. **Grant all permissions**
5. **Restart both devices**
6. **Test again**

### If Still Not Working

1. **Check logs** for error messages
2. **Export logs** and review for:
   - Connection failures
   - Permission denials
   - Discovery failures
3. **Try with 3rd device** to isolate if it's device-specific

### Rollback Option

If the issue persists and is blocking submission:

```bash
# Rollback to previous commit (before QoL features)
git checkout f60e26d

# Rebuild
./gradlew clean assembleDebug

# Test if peers show up
```

If peers show up after rollback, then we know the issue is in the QoL commit (even though code review shows no logical changes).

---

## Technical Deep Dive

### How Peer Discovery Works

1. **Advertising**: Device broadcasts its presence via Nearby Connections
2. **Discovery**: Device scans for other advertising devices
3. **Connection**: When discovered, devices initiate connection
4. **Identity Exchange**: After connection, devices exchange IDENTITY packets
5. **State Update**: P2PManager updates `connectedEndpoints` set
6. **UI Update**: StateFlow triggers recomposition, UI shows peer

### Where It Could Break

- ❌ **Advertising** - No changes made
- ❌ **Discovery** - No changes made
- ❌ **Connection** - No changes made
- ❌ **Identity Exchange** - No changes made
- ❌ **State Update** - No changes made
- ❌ **UI Update** - Only cosmetic label changes

**Conclusion**: Code changes cannot explain the issue.

---

## Alternative Hypothesis

### Could it be the PREVIOUS commit?

The cloud relay fix commit (`f60e26d`) made significant changes to:

- `InternetGatewayManager.pollRelayMessages()`
- HMAC verification and decryption logic
- Rate limiting tiers

**Possibility**: Those changes might have inadvertently affected local mesh discovery?

**Test**: Rollback to commit BEFORE cloud relay fix:

```bash
git checkout 1815649  # Before cloud relay fix
./gradlew clean assembleDebug
```

---

## Summary

**Most Likely Cause**: Version mismatch between devices  
**Second Most Likely**: Permissions need to be re-granted  
**Third Most Likely**: App data cache issue  
**Least Likely**: Code changes (none that affect peer discovery)

**Recommended Solution**:

1. Update both devices to same version
2. Clear app data
3. Reinstall
4. Test

**If that doesn't work**:

1. Check logs for errors
2. Try rollback to previous commit
3. Test with 3rd device

---

**Status**: Awaiting user testing with recommended solutions  
**Date**: 2026-04-28
