package com.fyp.resilientp2p.transport

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

enum class PacketType {
    PING,
    PONG,
    ACK,
    DATA,
    GOSSIP,
    IDENTITY
}

data class Hop(val peerId: String, val rssi: Int)

data class Packet(
        val id: String = UUID.randomUUID().toString(),
        val type: PacketType,
        val timestamp: Long = System.currentTimeMillis(),
        val sourceId: String,
        val destId: String,
        val payload: ByteArray = ByteArray(0),
        val ttl: Int = 3,
        val trace: List<Hop> = emptyList(),
        val sequenceNumber: Long = 0
) {
    fun toBytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

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

        return baos.toByteArray()
    }

    // Binary serialization only

    companion object {
        fun fromBytes(bytes: ByteArray): Packet {
            val bais = ByteArrayInputStream(bytes)
            val dis = DataInputStream(bais)

            // Helper to read large strings
            fun readString(): String {
                val len = dis.readInt()
                val b = ByteArray(len)
                dis.readFully(b)
                return String(b, StandardCharsets.UTF_8)
            }

            // Deserialize ID (String)
            val id = readString()

            val type = PacketType.valueOf(readString())
            val timestamp = dis.readLong()
            val sourceId = readString()
            val destId = readString()

            val payloadSize = dis.readInt()
            val payload = ByteArray(payloadSize)
            if (payloadSize > 0) {
                dis.readFully(payload)
            }

            val ttl = dis.readInt()
            val sequenceNumber = dis.readLong()

            val traceSize = dis.readInt()
            val trace = ArrayList<Hop>(traceSize)
            for (i in 0 until traceSize) {
                val peerId = readString()
                val rssi = dis.readInt()
                trace.add(Hop(peerId, rssi))
            }

            return Packet(
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
