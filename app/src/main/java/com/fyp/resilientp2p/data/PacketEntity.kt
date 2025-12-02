package com.fyp.resilientp2p.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "packet_queue")
data class PacketEntity(
        @PrimaryKey val id: String,
        val destId: String,
        val type: String,
        val payload: ByteArray,
        val timestamp: Long,
        val expiration: Long,
        val sourceId: String // Need source to reconstruct packet
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PacketEntity

        if (id != other.id) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

@Dao
interface PacketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPacket(packet: PacketEntity)

    @Query("SELECT * FROM packet_queue WHERE destId = :destId")
    suspend fun getPacketsForPeer(destId: String): List<PacketEntity>

    @Query("DELETE FROM packet_queue WHERE id = :packetId")
    suspend fun deletePacket(packetId: String)

    @Query("DELETE FROM packet_queue WHERE expiration < :now") suspend fun cleanupExpired(now: Long)
}
