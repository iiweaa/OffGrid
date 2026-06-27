package com.offgrid.app.link.packet

import com.offgrid.app.link.node.NodeId
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Binary serializer for [Packet].
 *
 * Layout (big-endian):
 * ```
 *  Offset | Size | Field
 * --------|------|------------------
 *       0 |    2 | Magic (0x4F47 "OG")
 *       2 |    1 | Version
 *       3 |    1 | Type
 *       4 |    1 | TTL
 *       5 |    1 | Reserved (hops/flags)
 *       6 |    8 | Source NodeId
 *      14 |    8 | Destination NodeId
 *      22 |    4 | Sequence number
 *      26 |    2 | Payload length
 *      28 |    N | Payload
 * ```
 */
object PacketSerializer {

    const val MAGIC: Short = 0x4F47
    const val NODE_ID_SIZE = NodeId.SIZE_BYTES
    const val HEADER_SIZE = 2 + 1 + 1 + 1 + 1 + NODE_ID_SIZE + NODE_ID_SIZE + 4 + 2
    const val MAX_PAYLOAD_SIZE = 65535

    fun serialize(packet: Packet): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_SIZE + packet.payload.size)
            .order(ByteOrder.BIG_ENDIAN)

        buffer.putShort(MAGIC)
        buffer.put(packet.version.toByte())
        buffer.put(packet.type.code.toByte())
        buffer.put(packet.ttl.toByte())
        buffer.put(0.toByte()) // reserved
        buffer.putLong(packet.source.value)
        buffer.putLong(packet.destination.value)
        buffer.putInt(packet.sequence)
        buffer.putShort(packet.payload.size.toShort())
        buffer.put(packet.payload)

        return buffer.array()
    }

    fun deserialize(data: ByteArray): Packet? {
        if (data.size < HEADER_SIZE) return null

        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

        val magic = buffer.short
        if (magic != MAGIC) return null

        val version = buffer.get().toInt() and 0xFF
        if (version != Packet.CURRENT_VERSION) return null

        val typeCode = buffer.get().toInt() and 0xFF
        val type = PacketType.fromCode(typeCode) ?: return null

        val ttl = buffer.get().toInt() and 0xFF
        buffer.get() // reserved

        val source = NodeId(buffer.long)
        val destination = NodeId(buffer.long)
        val sequence = buffer.int
        val payloadLength = buffer.short.toInt() and 0xFFFF

        if (data.size < HEADER_SIZE + payloadLength) return null

        val payload = ByteArray(payloadLength)
        buffer.get(payload)

        return Packet(
            version = version,
            type = type,
            ttl = ttl,
            source = source,
            destination = destination,
            sequence = sequence,
            payload = payload
        )
    }
}
