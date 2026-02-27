package com.fyp.resilientp2p.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A named chat group/channel on the mesh.
 *
 * Members are stored as a comma-separated list of peer names.
 * Group membership is disseminated via ROUTE_ANNOUNCE extensions and
 * GROUP_MESSAGE packets carry the [groupId] as their destId.
 *
 * @property groupId Unique group identifier (UUID or human-readable slug).
 * @property name Display name of the group.
 * @property createdBy Peer name of the group creator.
 * @property createdAt Epoch‐ms when the group was created.
 * @property members Comma-separated list of member peer names.
 */
@Entity(
    tableName = "chat_groups",
    indices = [Index(value = ["name"], unique = true)]
)
data class ChatGroup(
    @PrimaryKey val groupId: String,
    val name: String,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val members: String = ""  // comma-separated peer names
) {
    /** Returns the members list as a [Set]. */
    fun memberSet(): Set<String> =
        members.split(",").filter { it.isNotBlank() }.toSet()

    /** Returns a copy with the given peer added. */
    fun withMember(peer: String): ChatGroup =
        copy(members = (memberSet() + peer).joinToString(","))

    /** Returns a copy with the given peer removed. */
    fun withoutMember(peer: String): ChatGroup =
        copy(members = (memberSet() - peer).joinToString(","))
}
