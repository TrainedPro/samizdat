package com.fyp.resilientp2p.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.fyp.resilientp2p.data.LogLevel
import com.fyp.resilientp2p.transport.Packet
import com.fyp.resilientp2p.transport.PacketType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.nio.charset.StandardCharsets

/**
 * FirestoreRelayManager — persistent store-and-forward for cross-mesh messages.
 *
 * Firestore schema  /relayed_messages/{docId}
 *   from        : String  — sender device name (human-readable)
 *   to          : String  — destination device name or "BROADCAST"
 *   message     : String  — the actual message text (plain, readable in Firebase Console)
 *   networkId   : String  — sender's mesh network ID
 *   packetType  : String  — e.g. "DATA", "GOSSIP"
 *   rawPayload  : String  — Base64 binary (for non-DATA packets / full reconstruction)
 *   timestamp   : Long    — unix ms
 *   expiresAt   : Long    — timestamp + 48h
 */
class FirestoreRelayManager(
    private val context: Context,
    private val localDeviceName: String,
    private val getNetworkId: () -> String,
    private val onPacketReceived: (Packet) -> Unit,
    private val log: (String, LogLevel) -> Unit
) {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("relayed_messages")
    private val prefs: SharedPreferences =
        context.getSharedPreferences("firestore_relay", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val PREF_LAST_FETCH = "last_fetch_ts"
        private const val TTL_MS = 48L * 60 * 60 * 1000 // 48 hours
        private const val TAG = "FirestoreRelay"
    }

    /**
     * Publish a packet to Firestore so the destination gateway can pick it up
     * even if it's offline. Shows actual message text in the Firebase Console.
     */
    fun publishPacket(packet: Packet) {
        val networkId = getNetworkId()
        if (networkId == "local") return

        val now = System.currentTimeMillis()

        // Decode human-readable message text for DATA packets
        val messageText: String = when (packet.type) {
            PacketType.DATA -> String(packet.payload, StandardCharsets.UTF_8)
            else -> "[${packet.type.name} packet]"
        }

        val data = hashMapOf(
            "from"        to packet.sourceId,
            "to"          to packet.destId,
            "message"     to messageText,                              // ← plain readable text
            "networkId"   to networkId,
            "packetType"  to packet.type.name,
            "rawPayload"  to Base64.encodeToString(packet.toBytes(), Base64.NO_WRAP),
            "timestamp"   to now,
            "expiresAt"   to (now + TTL_MS)
        )

        scope.launch {
            try {
                collection.add(data).await()
                log("Relayed to Firestore: \"$messageText\" → ${packet.destId}", LogLevel.DEBUG)
            } catch (e: Exception) {
                log("Firestore publish failed: ${e.message}", LogLevel.WARN)
            }
        }
    }

    /**
     * Fetch messages addressed to this device (or BROADCAST) that arrived after
     * the last successful fetch. Called when verified internet becomes available.
     */
    fun fetchPendingMessages() {
        val lastFetch = prefs.getLong(PREF_LAST_FETCH, 0L)
        val now = System.currentTimeMillis()
        log("Fetching pending relay messages (since ${(now - lastFetch) / 1000}s ago)", LogLevel.INFO)

        scope.launch {
            try {
                // Query 1: messages addressed directly to this device
                val directDocs = collection
                    .whereEqualTo("to", localDeviceName)
                    .whereGreaterThan("timestamp", lastFetch)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get().await().documents

                // Query 2: BROADCAST messages from other devices
                val broadcastDocs = collection
                    .whereEqualTo("to", "BROADCAST")
                    .whereGreaterThan("timestamp", lastFetch)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get().await().documents

                val allDocs = (directDocs + broadcastDocs)
                    .distinctBy { it.id }
                    .sortedBy { it.getLong("timestamp") ?: 0L }
                    .filter { (it.getLong("expiresAt") ?: 0L) > now } // Ignore expired

                log("Found ${allDocs.size} pending relay messages", LogLevel.INFO)

                allDocs.forEach { doc ->
                    try {
                        val payloadB64 = doc.getString("rawPayload") ?: return@forEach
                        val bytes = Base64.decode(payloadB64, Base64.NO_WRAP)
                        val packet = Packet.fromBytes(bytes)
                        val from = doc.getString("from") ?: packet.sourceId

                        // Self-echo prevention
                        if (packet.sourceId == localDeviceName) return@forEach

                        log("Delivering relayed message from $from: \"${doc.getString("message")}\"", LogLevel.DEBUG)
                        onPacketReceived(packet)
                    } catch (e: Exception) {
                        log("Error parsing relayed packet: ${e.message}", LogLevel.WARN)
                    }
                }

                // Save last fetch timestamp only on success
                prefs.edit().putLong(PREF_LAST_FETCH, now).apply()

            } catch (e: Exception) {
                log("Firestore fetch failed: ${e.message}", LogLevel.WARN)
                Log.e(TAG, "Firestore fetch error", e)
            }
        }
    }
}
