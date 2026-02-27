package com.fyp.resilientp2p.transport

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * All wire-level message types in the Samizdat mesh protocol.
 *
 * Each packet on the mesh carries exactly one [PacketType]. Routers use the type to
 * decide forwarding strategy (unicast, broadcast, flood) and priority.
 *
 * @see Packet
 */
enum class PacketType {
    PING,
    PONG,
    ACK,
    DATA,
    GOSSIP,
    IDENTITY,
    AUDIO_DATA,        // Chunked audio payload for multi-hop streaming
    AUDIO_CONTROL,     // Start/stop audio session signals
    ROUTE_ANNOUNCE,    // Periodic route table advertisement
    STORE_FORWARD,     // Store-and-forward delivery attempt
    FILE_META,         // File transfer metadata (filename, MIME, payloadId)
    EMERGENCY,         // Emergency broadcast — highest priority, floods entire mesh

    // Phase 4 additions
    GROUP_MESSAGE,     // Chat message destined for a named channel/group
    FILE_ANNOUNCE,     // Announce a shared file to the mesh (hash, name, size)
    FILE_REQUEST,      // Request a file chunk by SHA-256 hash + offset
    FILE_CHUNK,        // Response with a chunk of the requested file
    ENCOUNTER_LOG,     // DTN encounter record broadcast
    LOCATION_PING      // Trilateration RTT measurement ping
}

/**
 * A single hop recorded in a packet's route trace.
 *
 * @property peerId The device name of the node at this hop.
 * @property rssi The RSSI (signal strength) measured at this hop, or 0 if unknown.
 */
data class Hop(val peerId: String, val rssi: Int)

/**
 * The fundamental mesh protocol data unit. Every message on the Samizdat mesh is
 * serialized as a [Packet] using a compact binary format (no Protobuf/JSON on-wire).
 *
 * Binary format (big-endian via [DataOutputStream]):
 * ```
 * [id:lenPrefixStr][type:lenPrefixStr][timestamp:i64][sourceId:str][destId:str]
 * [payloadSize:i32][payload:bytes][ttl:i32][seqNo:i64][traceSize:i32][trace...]
 * ```
 *
 * @property id Unique packet identifier (UUID). Used for deduplication.
 * @property type The [PacketType] determining routing behavior.
 * @property timestamp Unix epoch millis when the packet was created.
 * @property sourceId The originating device name.
 * @property destId The target device name, or a broadcast address.
 * @property payload Opaque application-level data (max 1 MB).
 * @property ttl Time-to-live in hops (default 5, max 255). Decremented at each relay.
 * @property trace Ordered list of [Hop]s this packet has traversed.
 * @property sequenceNumber Monotonic counter from the source, used for ordering.
 * @see PacketType
 */
data class Packet(
        val id: String = UUID.randomUUID().toString(),
        val type: PacketType,
        val timestamp: Long = System.currentTimeMillis(),
        val sourceId: String,
        val destId: String,
        val payload: ByteArray = ByteArray(0),
        val ttl: Int = DEFAULT_TTL,
        val trace: List<Hop> = emptyList(),
        val sequenceNumber: Long = 0
) {
    fun toBytes(): ByteArray {
        require(ttl in 0..255) { "Invalid TTL: $ttl" }
        require(sourceId.toByteArray(Charsets.UTF_8).size <= MAX_STRING_LENGTH) { "sourceId too long" }
        require(destId.toByteArray(Charsets.UTF_8).size <= MAX_STRING_LENGTH) { "destId too long" }
        require(payload.size <= MAX_PAYLOAD_SIZE) { "Payload too large: ${payload.size}" }
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { dos ->
            // Helper to write large strings
            fun writeString(str: String) {
                val bytes = str.toByteArray(StandardCharsets.UTF_8)
                dos.writeInt(bytes.size)
                dos.write(bytes)
            }

            // Optimize ID (UUID) -> String (UTF) for safety
            writeString(id)

            writeString(type.name)
            dos.writeLong(timestamp)
            writeString(sourceId)
            writeString(destId)

            dos.writeInt(payload.size)
            if (payload.isNotEmpty()) {
                dos.write(payload)
            }

            dos.writeInt(ttl)
            dos.writeLong(sequenceNumber)

            dos.writeInt(trace.size)
            trace.forEach { hop ->
                writeString(hop.peerId)
                dos.writeInt(hop.rssi)
            }
        }
        return baos.toByteArray()
    }

    // Binary serialization only

    companion object {
        /** Default hop limit. 5 hops covers most BLE/WiFi-Direct mesh topologies. */
        const val DEFAULT_TTL = 5
        /** Maximum byte length for any single string field (device name, UUID). */
        private const val MAX_STRING_LENGTH = 256
        /** Maximum payload size in bytes (1 MB). */
        private const val MAX_PAYLOAD_SIZE = 1 * 1024 * 1024
        /** Maximum number of hops recorded in the trace list. */
        private const val MAX_TRACE_SIZE = 256
        /** Aggregate packet size limit to prevent OOM from malformed data (2 MB). */
        private const val MAX_TOTAL_PACKET_SIZE = 2 * 1024 * 1024

        fun fromBytes(bytes: ByteArray): Packet {
            // Aggregate size check to prevent OOM DoS
            if (bytes.size > MAX_TOTAL_PACKET_SIZE) {
                throw IllegalArgumentException("Packet too large: ${bytes.size} bytes")
            }
            return DataInputStream(ByteArrayInputStream(bytes)).use { dis ->
            // Helper to read large strings with validation
            fun readString(): String {
                val len = dis.readInt()
                if (len < 0 || len > MAX_STRING_LENGTH || len > dis.available()) {
                    throw IllegalArgumentException("Invalid string length: $len (available: ${dis.available()})")
                }
                val b = ByteArray(len)
                dis.readFully(b)
                return String(b, StandardCharsets.UTF_8)
            }

            // Deserialize ID (String)
            val id = readString()
            if (id.isBlank()) {
                throw IllegalArgumentException("Packet ID cannot be blank")
            }

            val typeStr = readString()
            val type =
                    try {
                        PacketType.valueOf(typeStr)
                    } catch (e: IllegalArgumentException) {
                        throw IllegalArgumentException("Unknown packet type: $typeStr")
                    }
            val timestamp = dis.readLong()
            val sourceId = readString()
            val destId = readString()
            if (sourceId.isBlank() || destId.isBlank()) {
                throw IllegalArgumentException("Empty or blank sourceId or destId")
            }
            val payloadSize = dis.readInt()
            if (payloadSize < 0 || payloadSize > MAX_PAYLOAD_SIZE) {
                throw IllegalArgumentException("Invalid payload size: $payloadSize")
            }
            val payload = ByteArray(payloadSize)
            if (payloadSize > 0) {
                dis.readFully(payload)
            }

            val ttl = dis.readInt()
            if (ttl < 0 || ttl > 255) {
                throw IllegalArgumentException("Invalid TTL: $ttl")
            }
            val sequenceNumber = dis.readLong()

            val traceSize = dis.readInt()
            if (traceSize < 0 || traceSize > MAX_TRACE_SIZE) {
                throw IllegalArgumentException("Invalid trace size: $traceSize")
            }
            val trace = ArrayList<Hop>(traceSize)
            for (i in 0 until traceSize) {
                val peerId = readString()
                val rssi = dis.readInt()
                trace.add(Hop(peerId, rssi))
            }

            Packet(
                    id = id,
                    type = type,
                    timestamp = timestamp,
                    sourceId = sourceId,
                    destId = destId,
                    payload = payload,
                    ttl = ttl,
                    trace = trace,
                    sequenceNumber = sequenceNumber
            )
            } // use
        }

        // Binary serialization only
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Packet

        if (id != other.id) return false
        if (type != other.type) return false
        if (timestamp != other.timestamp) return false
        if (sourceId != other.sourceId) return false
        if (destId != other.destId) return false
        if (!payload.contentEquals(other.payload)) return false
        if (ttl != other.ttl) return false
        if (trace != other.trace) return false
        if (sequenceNumber != other.sequenceNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + sourceId.hashCode()
        result = 31 * result + destId.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + ttl
        result = 31 * result + trace.hashCode()
        result = 31 * result + sequenceNumber.hashCode()
        return result
    }

    override fun toString(): String {
        return "Packet(id=${id.take(8)}, type=$type, src=$sourceId, dest=$destId, ts=$timestamp)"
    }
}
