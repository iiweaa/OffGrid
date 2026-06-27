package com.offgrid.app.link.packet

import com.offgrid.app.link.node.NodeId
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PacketSerializerTest {

    private val source = NodeId(0x1111_2222_3333_4444)
    private val destination = NodeId(0x5555_6666_7777_8888)

    @Test
    fun `serialize and deserialize voice packet`() {
        val payload = ByteArray(160) { it.toByte() }
        val packet = Packet(
            version = Packet.CURRENT_VERSION,
            type = PacketType.VOICE,
            ttl = 5,
            source = source,
            destination = destination,
            sequence = 42,
            payload = payload
        )

        val bytes = PacketSerializer.serialize(packet)
        val decoded = PacketSerializer.deserialize(bytes)

        assertNotNull(decoded)
        assertEquals(packet, decoded)
        assertArrayEquals(payload, decoded!!.payload)
    }

    @Test
    fun `serialize and deserialize empty payload`() {
        val packet = Packet(
            version = Packet.CURRENT_VERSION,
            type = PacketType.HELLO,
            ttl = 64,
            source = source,
            destination = destination,
            sequence = 0,
            payload = ByteArray(0)
        )

        val bytes = PacketSerializer.serialize(packet)
        val decoded = PacketSerializer.deserialize(bytes)

        assertNotNull(decoded)
        assertEquals(packet, decoded)
    }

    @Test
    fun `deserialize rejects wrong magic`() {
        val bytes = PacketSerializer.serialize(
            Packet(
                version = Packet.CURRENT_VERSION,
                type = PacketType.SIGNAL,
                ttl = 1,
                source = source,
                destination = destination,
                sequence = 1,
                payload = byteArrayOf(1, 2, 3)
            )
        )
        bytes[0] = 0x00
        bytes[1] = 0x00

        assertNull(PacketSerializer.deserialize(bytes))
    }

    @Test
    fun `deserialize rejects unsupported version`() {
        val bytes = PacketSerializer.serialize(
            Packet(
                version = Packet.CURRENT_VERSION,
                type = PacketType.LOCATION,
                ttl = 1,
                source = source,
                destination = destination,
                sequence = 1,
                payload = byteArrayOf(1)
            )
        )
        bytes[2] = 0xFF.toByte()

        assertNull(PacketSerializer.deserialize(bytes))
    }

    @Test
    fun `deserialize rejects truncated data`() {
        val bytes = PacketSerializer.serialize(
            Packet(
                version = Packet.CURRENT_VERSION,
                type = PacketType.VOICE,
                ttl = 1,
                source = source,
                destination = destination,
                sequence = 1,
                payload = byteArrayOf(1, 2, 3, 4)
            )
        )

        assertNull(PacketSerializer.deserialize(bytes.copyOfRange(0, PacketSerializer.HEADER_SIZE)))
        assertNull(PacketSerializer.deserialize(bytes.copyOfRange(0, bytes.size - 1)))
    }

    @Test
    fun `deserialize rejects unknown type code`() {
        val bytes = PacketSerializer.serialize(
            Packet(
                version = Packet.CURRENT_VERSION,
                type = PacketType.HELLO,
                ttl = 1,
                source = source,
                destination = destination,
                sequence = 1,
                payload = ByteArray(0)
            )
        )
        bytes[3] = 0xFF.toByte()

        assertNull(PacketSerializer.deserialize(bytes))
    }

    @Test
    fun `max payload size roundtrips`() {
        val payload = ByteArray(PacketSerializer.MAX_PAYLOAD_SIZE) { (it % 256).toByte() }
        val packet = Packet(
            version = Packet.CURRENT_VERSION,
            type = PacketType.VOICE,
            ttl = 1,
            source = source,
            destination = destination,
            sequence = Int.MAX_VALUE,
            payload = payload
        )

        val bytes = PacketSerializer.serialize(packet)
        val decoded = PacketSerializer.deserialize(bytes)

        assertNotNull(decoded)
        assertEquals(packet, decoded)
    }
}
