package com.offgrid.app.link.signal

import com.offgrid.app.link.node.NodeId
import com.offgrid.app.link.packet.PacketSerializer
import com.offgrid.app.link.packet.PacketType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArrayList

class SignalingEngineTest {

    private val localNodeId = NodeId(0x1111_2222_3333_4444)
    private val remoteNodeId = NodeId(0x5555_6666_7777_8888)
    private val remoteAddress = InetAddress.getByName("192.168.49.100")

    @Test
    fun `start sends periodic hello packets`() {
        val engine = SignalingEngine(localNodeId, helloIntervalMs = 100)
        val sent = CopyOnWriteArrayList<ByteArray>()

        engine.setCallbacks(onSend = { bytes -> sent.add(bytes) })
        engine.start()

        Thread.sleep(250)
        engine.stop()

        // Should have received at least two HELLO packets.
        assertTrue("Expected at least 2 HELLOs, got ${sent.size}", sent.size >= 2)
        sent.forEach { bytes ->
            val packet = PacketSerializer.deserialize(bytes)
            assertNotNull(packet)
            assertEquals(PacketType.HELLO, packet!!.type)
            assertEquals(localNodeId, packet.source)
        }
    }

    @Test
    fun `handleHello connects new neighbor`() {
        val engine = SignalingEngine(localNodeId)
        val connected = mutableListOf<NodeId>()

        engine.setCallbacks(
            onNeighborConnected = { neighbor -> connected.add(neighbor.nodeId) }
        )
        engine.start()

        engine.handleHello(remoteNodeId, remoteAddress)

        assertEquals(1, connected.size)
        assertEquals(remoteNodeId, connected.first())
        assertEquals(1, engine.snapshot().size)

        engine.stop()
    }

    @Test
    fun `aging disconnects timed out neighbor`() {
        val engine = SignalingEngine(
            localNodeId,
            helloIntervalMs = 50,
            timeoutMs = 100
        )
        val disconnected = mutableListOf<NodeId>()

        engine.setCallbacks(
            onNeighborDisconnected = { nodeId -> disconnected.add(nodeId) }
        )
        engine.start()

        engine.handleHello(remoteNodeId, remoteAddress)
        assertEquals(1, engine.snapshot().size)

        Thread.sleep(250)

        assertTrue("Neighbor should have been removed", engine.snapshot().isEmpty())
        assertEquals(1, disconnected.size)
        assertEquals(remoteNodeId, disconnected.first())

        engine.stop()
    }

    @Test
    fun `reconnect after disconnect fires connected again`() {
        val engine = SignalingEngine(
            localNodeId,
            helloIntervalMs = 50,
            timeoutMs = 100
        )
        val connected = mutableListOf<NodeId>()
        val disconnected = mutableListOf<NodeId>()

        engine.setCallbacks(
            onNeighborConnected = { connected.add(it.nodeId) },
            onNeighborDisconnected = { disconnected.add(it) }
        )
        engine.start()

        engine.handleHello(remoteNodeId, remoteAddress)
        Thread.sleep(250) // wait for timeout
        engine.handleHello(remoteNodeId, remoteAddress)

        assertEquals(2, connected.size)
        assertEquals(1, disconnected.size)
        assertEquals(1, engine.snapshot().size)

        engine.stop()
    }
}
