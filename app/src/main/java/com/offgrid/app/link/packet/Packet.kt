package com.offgrid.app.link.packet

import com.offgrid.app.link.node.NodeId

/**
 * Mesh packet.
 *
 * All multi-hop payloads are carried in this uniform envelope. The header
 * contains enough metadata for forwarding, deduplication and payload routing.
 */
data class Packet(
    val version: Int,
    val type: PacketType,
    val ttl: Int,
    val source: NodeId,
    val destination: NodeId,
    val sequence: Int,
    val payload: ByteArray
) {
    init {
        require(version in 0..255) { "version must fit in one byte" }
        require(ttl in 0..255) { "ttl must fit in one byte" }
        require(payload.size <= PacketSerializer.MAX_PAYLOAD_SIZE) {
            "payload must be <= ${PacketSerializer.MAX_PAYLOAD_SIZE} bytes"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Packet) return false
        return version == other.version &&
            type == other.type &&
            ttl == other.ttl &&
            source == other.source &&
            destination == other.destination &&
            sequence == other.sequence &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + type.hashCode()
        result = 31 * result + ttl
        result = 31 * result + source.hashCode()
        result = 31 * result + destination.hashCode()
        result = 31 * result + sequence
        result = 31 * result + payload.contentHashCode()
        return result
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}
